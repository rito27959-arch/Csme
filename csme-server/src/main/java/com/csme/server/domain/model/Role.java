package com.csme.server.domain.model;

import com.csme.shared.security.Action;
import com.csme.shared.security.ErpModule;
import com.csme.shared.security.Permission;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Rôle d'entreprise : un nom et un ensemble de permissions MODULE:ACTION. */
@Entity
@Table(name = "roles")
public class Role extends TenantOwnedEntity {

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "system_role", nullable = false)
    private boolean systemRole;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    private Set<Grant> grants = new HashSet<>();

    protected Role() {
    }

    public Role(UUID companyId, String name, String description, boolean systemRole, Collection<Permission> permissions) {
        super(companyId);
        this.name = name;
        this.description = description;
        this.systemRole = systemRole;
        replacePermissions(permissions);
    }

    public void rename(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public final void replacePermissions(Collection<Permission> permissions) {
        grants.clear();
        permissions.forEach(p -> grants.add(new Grant(p.module(), p.action())));
    }

    public Set<Permission> getPermissions() {
        return grants.stream().map(g -> Permission.of(g.module, g.action)).collect(Collectors.toUnmodifiableSet());
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSystemRole() {
        return systemRole;
    }

    /** Ligne de role_permissions. */
    @Embeddable
    public static class Grant {
        @Enumerated(EnumType.STRING)
        @Column(name = "module", nullable = false, length = 40)
        private ErpModule module;

        @Enumerated(EnumType.STRING)
        @Column(name = "action", nullable = false, length = 20)
        private Action action;

        protected Grant() {
        }

        Grant(ErpModule module, Action action) {
            this.module = module;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Grant g && g.module == module && g.action == action;
        }

        @Override
        public int hashCode() {
            return 31 * module.hashCode() + action.hashCode();
        }
    }
}
