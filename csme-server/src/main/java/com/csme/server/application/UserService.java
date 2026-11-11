package com.csme.server.application;

import com.csme.server.domain.error.BusinessRuleException;
import com.csme.server.domain.error.ConflictException;
import com.csme.server.domain.error.NotFoundException;
import com.csme.server.domain.error.ValidationException;
import com.csme.server.domain.model.Agency;
import com.csme.server.domain.model.AppUser;
import com.csme.server.domain.model.Role;
import com.csme.server.domain.rules.Checks;
import com.csme.server.infrastructure.persistence.AgencyRepository;
import com.csme.server.infrastructure.persistence.RefreshTokenRepository;
import com.csme.server.infrastructure.persistence.RoleRepository;
import com.csme.server.infrastructure.persistence.UserRepository;
import com.csme.server.infrastructure.security.CurrentActor;
import com.csme.server.tenancy.TenantContext;
import com.csme.shared.api.PageResponse;
import com.csme.shared.dto.UserDtos.CreateUserRequest;
import com.csme.shared.dto.UserDtos.ResetPasswordRequest;
import com.csme.shared.dto.UserDtos.UpdateUserRequest;
import com.csme.shared.dto.UserDtos.UserDto;
import java.time.Clock;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Comptes utilisateurs de l'entreprise courante. */
@Service
@Transactional
public class UserService {

    public static final int MAX_PAGE_SIZE = 100;

    private final UserRepository users;
    private final RoleRepository roles;
    private final AgencyRepository agencies;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final Clock clock;

    public UserService(UserRepository users, RoleRepository roles, AgencyRepository agencies,
                       RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder,
                       AuditService audit, Clock clock) {
        this.users = users;
        this.roles = roles;
        this.agencies = agencies;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> search(String query, int page, int size) {
        UUID companyId = TenantContext.requireCompanyId();
        String q = Checks.clean(query);
        String pattern = q == null ? "%" : "%" + escapeLike(q.toLowerCase(Locale.ROOT)) + "%";
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("fullName").ascending());
        var now = clock.instant();
        return Dtos.page(users.search(companyId, pattern, pageable), u -> Dtos.user(u, now));
    }

    public UserDto create(CreateUserRequest request) {
        UUID companyId = TenantContext.requireCompanyId();
        if (request == null) {
            throw new ValidationException(Map.of("email", "Les informations du compte sont obligatoires."));
        }
        String email = Checks.normalizeEmail(request.email());
        Checks.start()
                .required("email", email, "L'adresse électronique")
                .email("email", email)
                .maxLength("email", email, 254, "L'adresse électronique")
                .required("fullName", request.fullName(), "Le nom complet")
                .maxLength("fullName", request.fullName(), 160, "Le nom complet")
                .maxLength("jobTitle", request.jobTitle(), 120, "Le poste")
                .password("initialPassword", request.initialPassword(), email)
                .rule(request.roleIds() != null && !request.roleIds().isEmpty(), "roleIds", "Attribuez au moins un rôle.")
                .throwIfInvalid();
        if (users.existsByEmail(email)) {
            throw new ConflictException("Cette adresse électronique est déjà utilisée.");
        }
        AppUser user = AppUser.companyUser(companyId, email, request.fullName().strip(),
                Checks.clean(request.jobTitle()), passwordEncoder.encode(request.initialPassword()));
        user.replaceRoles(resolveRoles(companyId, request.roleIds()));
        user.replaceAgencies(resolveAgencies(companyId, request.agencyIds()));
        users.save(user);
        audit.success("USER_CREATED", "User", user.getId(), null, snapshot(user));
        return Dtos.user(user, clock.instant());
    }

    public UserDto update(UUID id, UpdateUserRequest request) {
        UUID companyId = TenantContext.requireCompanyId();
        AppUser user = load(id, companyId);
        Checks.start()
                .required("fullName", request == null ? null : request.fullName(), "Le nom complet")
                .maxLength("fullName", request == null ? null : request.fullName(), 160, "Le nom complet")
                .maxLength("jobTitle", request == null ? null : request.jobTitle(), 120, "Le poste")
                .rule(request != null && request.roleIds() != null && !request.roleIds().isEmpty(), "roleIds",
                        "Attribuez au moins un rôle.")
                .throwIfInvalid();
        Map<String, Object> before = snapshot(user);
        List<Role> newRoles = resolveRoles(companyId, request.roleIds());
        if (isSelf(user) && !grantsUserAdministration(newRoles)) {
            throw new BusinessRuleException("SELF_LOCKOUT",
                    "Vous ne pouvez pas retirer vos propres droits d'administration des utilisateurs.");
        }
        user.updateProfile(request.fullName().strip(), Checks.clean(request.jobTitle()));
        user.replaceRoles(newRoles);
        user.replaceAgencies(resolveAgencies(companyId, request.agencyIds()));
        audit.success("USER_UPDATED", "User", user.getId(), before, snapshot(user));
        return Dtos.user(user, clock.instant());
    }

    public UserDto setActive(UUID id, boolean active) {
        AppUser user = load(id, TenantContext.requireCompanyId());
        if (!active && isSelf(user)) {
            throw new BusinessRuleException("SELF_DEACTIVATION", "Vous ne pouvez pas désactiver votre propre compte.");
        }
        Map<String, Object> before = snapshot(user);
        user.setActive(active);
        if (!active) {
            refreshTokens.revokeAllForUser(user.getId(), clock.instant());
        }
        audit.success(active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "User", user.getId(), before, snapshot(user));
        return Dtos.user(user, clock.instant());
    }

    public UserDto unlock(UUID id) {
        AppUser user = load(id, TenantContext.requireCompanyId());
        user.unlock();
        audit.success("USER_UNLOCKED", "User", user.getId(), null, null);
        return Dtos.user(user, clock.instant());
    }

    public void resetPassword(UUID id, ResetPasswordRequest request) {
        AppUser user = load(id, TenantContext.requireCompanyId());
        Checks.start().password("newPassword", request == null ? null : request.newPassword(), user.getEmail())
                .throwIfInvalid();
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokens.revokeAllForUser(user.getId(), clock.instant());
        audit.success("USER_PASSWORD_RESET", "User", user.getId(), null, Map.of("sessionsRevoked", true));
    }

    // ------------------------------------------------------------------------------------

    private AppUser load(UUID id, UUID companyId) {
        return users.findByIdAndCompanyId(id, companyId).orElseThrow(() -> new NotFoundException("Utilisateur"));
    }

    private List<Role> resolveRoles(UUID companyId, Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Role> found = roles.findAllByIdInAndCompanyId(Set.copyOf(ids), companyId);
        if (found.size() != Set.copyOf(ids).size()) {
            // Un identifiant inconnu ou appartenant à une autre entreprise : même réponse.
            throw new ValidationException(Map.of("roleIds", "Un des rôles sélectionnés n'existe pas."));
        }
        return found;
    }

    private List<Agency> resolveAgencies(UUID companyId, Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Agency> found = agencies.findAllByIdInAndCompanyId(Set.copyOf(ids), companyId);
        if (found.size() != Set.copyOf(ids).size()) {
            throw new ValidationException(Map.of("agencyIds", "Une des agences sélectionnées n'existe pas."));
        }
        return found;
    }

    private static boolean isSelf(AppUser user) {
        return CurrentActor.current().map(a -> a.userId().equals(user.getId())).orElse(false);
    }

    private static boolean grantsUserAdministration(List<Role> roles) {
        return roles.stream().flatMap(r -> r.getPermissions().stream())
                .anyMatch(p -> p.module() == com.csme.shared.security.ErpModule.USERS
                        && p.action() == com.csme.shared.security.Action.UPDATE);
    }

    private static Map<String, Object> snapshot(AppUser u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("email", u.getEmail());
        map.put("fullName", u.getFullName());
        map.put("jobTitle", u.getJobTitle());
        map.put("active", u.isActive());
        map.put("roles", new TreeSet<>(u.getRoles().stream().map(Role::getName).toList()));
        map.put("agencies", new TreeSet<>(u.getAgencies().stream().map(Agency::getCode).toList()));
        return map;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
