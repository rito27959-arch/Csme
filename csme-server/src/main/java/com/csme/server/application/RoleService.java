package com.csme.server.application;

import com.csme.server.domain.error.BusinessRuleException;
import com.csme.server.domain.error.ConflictException;
import com.csme.server.domain.error.NotFoundException;
import com.csme.server.domain.error.ValidationException;
import com.csme.server.domain.model.AppUser;
import com.csme.server.domain.model.Role;
import com.csme.server.domain.rules.Checks;
import com.csme.server.infrastructure.persistence.RoleRepository;
import com.csme.server.infrastructure.persistence.UserRepository;
import com.csme.server.infrastructure.security.CurrentActor;
import com.csme.server.tenancy.TenantContext;
import com.csme.shared.dto.UserDtos.RoleDto;
import com.csme.shared.dto.UserDtos.SaveRoleRequest;
import com.csme.shared.security.Action;
import com.csme.shared.security.DefaultRoles;
import com.csme.shared.security.ErpModule;
import com.csme.shared.security.Permission;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rôles de l'entreprise courante. Les rôles prédéfinis sont modifiables, sauf « Directeur général »
 * qui garantit qu'il existe toujours un accès complet à l'entreprise.
 *
 * <p>Les permissions modifiées s'appliquent aux utilisateurs concernés au prochain renouvellement
 * de leur jeton d'accès (15 minutes au plus).</p>
 */
@Service
@Transactional
public class RoleService {

    private final RoleRepository roles;
    private final UserRepository users;
    private final AuditService audit;

    public RoleService(RoleRepository roles, UserRepository users, AuditService audit) {
        this.roles = roles;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<RoleDto> list() {
        return roles.findAllByCompanyIdOrderByNameAsc(TenantContext.requireCompanyId()).stream().map(this::toDto).toList();
    }

    public RoleDto create(SaveRoleRequest request) {
        UUID companyId = TenantContext.requireCompanyId();
        Set<Permission> permissions = validate(request);
        String name = request.name().strip();
        if (roles.existsByCompanyIdAndNameIgnoreCase(companyId, name)) {
            throw new ConflictException("Un rôle porte déjà le nom « " + name + " ».");
        }
        PermissionGuard.requireHeldByActor(permissions);
        Role role = roles.save(new Role(companyId, name, Checks.clean(request.description()), false, permissions));
        audit.success("ROLE_CREATED", "Role", role.getId(), null, snapshot(role));
        return toDto(role);
    }

    public RoleDto update(UUID id, SaveRoleRequest request) {
        UUID companyId = TenantContext.requireCompanyId();
        Role role = load(id, companyId);
        requireEditable(role);
        Set<Permission> permissions = validate(request);
        String name = request.name().strip();
        if (!role.getName().equalsIgnoreCase(name) && roles.existsByCompanyIdAndNameIgnoreCase(companyId, name)) {
            throw new ConflictException("Un rôle porte déjà le nom « " + name + " ».");
        }
        Set<Permission> added = new HashSet<>(permissions);
        added.removeAll(role.getPermissions());
        PermissionGuard.requireHeldByActor(added);
        preventSelfLockout(role, permissions);

        Map<String, Object> before = snapshot(role);
        role.rename(name, Checks.clean(request.description()));
        role.replacePermissions(permissions);
        audit.success("ROLE_UPDATED", "Role", role.getId(), before, snapshot(role));
        return toDto(role);
    }

    public void delete(UUID id) {
        Role role = load(id, TenantContext.requireCompanyId());
        if (role.isSystemRole()) {
            throw new BusinessRuleException("SYSTEM_ROLE", "Les rôles prédéfinis ne peuvent pas être supprimés, seulement modifiés.");
        }
        long users = roles.countUsers(role.getId());
        if (users > 0) {
            throw new ConflictException("Ce rôle est attribué à " + users + " utilisateur(s). Retirez-le d'abord de leurs comptes.");
        }
        Map<String, Object> before = snapshot(role);
        roles.delete(role);
        audit.success("ROLE_DELETED", "Role", role.getId(), before, null);
    }

    // ------------------------------------------------------------------------------------

    private Role load(UUID id, UUID companyId) {
        return roles.findByIdAndCompanyId(id, companyId).orElseThrow(() -> new NotFoundException("Rôle"));
    }

    private static void requireEditable(Role role) {
        if (role.isSystemRole() && DefaultRoles.GENERAL_MANAGER.equals(role.getName())) {
            throw new BusinessRuleException("LOCKED_ROLE",
                    "Le rôle « Directeur général » n'est pas modifiable : il garantit un accès complet à l'entreprise.");
        }
    }

    /** Refuse qu'un utilisateur retire de son propre rôle le droit de gérer les rôles. */
    private void preventSelfLockout(Role role, Set<Permission> newPermissions) {
        Permission manageRoles = Permission.of(ErpModule.ROLES, Action.UPDATE);
        if (newPermissions.contains(manageRoles) || !role.getPermissions().contains(manageRoles)) {
            return;
        }
        AppUser actor = users.findByIdAndCompanyId(CurrentActor.require().userId(), role.getCompanyId()).orElse(null);
        if (actor == null || actor.getRoles().stream().noneMatch(r -> r.getId().equals(role.getId()))) {
            return;
        }
        boolean keptElsewhere = actor.getRoles().stream()
                .filter(r -> !r.getId().equals(role.getId()))
                .anyMatch(r -> r.getPermissions().contains(manageRoles));
        if (!keptElsewhere) {
            throw new BusinessRuleException("SELF_LOCKOUT",
                    "Vous perdriez votre droit de gérer les rôles. Faites cette modification depuis un autre compte.");
        }
    }

    private static Set<Permission> validate(SaveRoleRequest request) {
        Checks.start()
                .required("name", request == null ? null : request.name(), "Le nom")
                .maxLength("name", request == null ? null : request.name(), 80, "Le nom")
                .maxLength("description", request == null ? null : request.description(), 255, "La description")
                .throwIfInvalid();
        Set<Permission> parsed = new HashSet<>();
        for (String raw : request.permissions()) {
            try {
                parsed.add(Permission.parse(raw));
            } catch (IllegalArgumentException e) {
                throw new ValidationException(Map.of("permissions", "Permission inconnue : " + raw));
            }
        }
        return parsed;
    }

    private RoleDto toDto(Role r) {
        return new RoleDto(r.getId(), r.getName(), r.getDescription(), r.isSystemRole(),
                Dtos.authorities(r.getPermissions()), roles.countUsers(r.getId()));
    }

    private static Map<String, Object> snapshot(Role r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", r.getName());
        map.put("description", r.getDescription());
        map.put("permissions", new TreeSet<>(Dtos.authorities(r.getPermissions())));
        return map;
    }
}
