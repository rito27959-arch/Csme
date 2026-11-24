package com.csme.server.application;

import com.csme.server.domain.error.BusinessRuleException;
import com.csme.server.domain.model.Role;
import com.csme.server.infrastructure.security.CurrentActor;
import com.csme.shared.security.Permission;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Empêche l'élévation de privilèges : un utilisateur ne peut accorder (à un rôle ou à un compte)
 * que des permissions qu'il détient lui-même. Sans cette règle, quiconque peut modifier les rôles
 * pourrait s'attribuer n'importe quel droit.
 */
final class PermissionGuard {

    private PermissionGuard() {
    }

    static void requireHeldByActor(Collection<Permission> granted) {
        CurrentActor actor = CurrentActor.require();
        Set<String> missing = granted.stream()
                .map(Permission::authority)
                .filter(a -> !actor.has(a))
                .collect(Collectors.toCollection(TreeSet::new));
        if (!missing.isEmpty()) {
            throw new BusinessRuleException("PRIVILEGE_ESCALATION",
                    "Vous ne pouvez pas accorder des permissions que vous ne possédez pas : " + String.join(", ", missing));
        }
    }

    static void requireRolesHeldByActor(Collection<Role> roles) {
        requireHeldByActor(roles.stream().flatMap(r -> r.getPermissions().stream()).collect(Collectors.toSet()));
    }
}
