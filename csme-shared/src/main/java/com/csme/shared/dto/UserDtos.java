package com.csme.shared.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Utilisateurs et rôles d'une entreprise. */
public final class UserDtos {

    private UserDtos() {
    }

    public record RoleRef(UUID id, String name) {
    }

    public record DepartmentRef(UUID id, String code, String name) {
    }

    public record UserDto(
            UUID id,
            String email,
            String fullName,
            String jobTitle,
            boolean active,
            boolean locked,
            List<RoleRef> roles,
            List<AuthDtos.AgencyRef> agencies,
            DepartmentRef department,
            Instant lastLoginAt,
            Instant createdAt) {

        public UserDto {
            roles = roles == null ? List.of() : List.copyOf(roles);
            agencies = agencies == null ? List.of() : List.copyOf(agencies);
        }
    }

    public record CreateUserRequest(
            String email,
            String fullName,
            String jobTitle,
            String initialPassword,
            Set<UUID> roleIds,
            Set<UUID> agencyIds,
            UUID departmentId) {
        @Override
        public String toString() {
            return "CreateUserRequest[email=" + email + ", fullName=" + fullName + "]";
        }
    }

    public record UpdateUserRequest(String fullName, String jobTitle, Set<UUID> roleIds, Set<UUID> agencyIds,
                                    UUID departmentId) {
    }

    public record ResetPasswordRequest(String newPassword) {
        @Override
        public String toString() {
            return "ResetPasswordRequest[***]";
        }
    }

    /** Création ou modification d'un rôle : permissions au format MODULE:ACTION. */
    public record SaveRoleRequest(String name, String description, Set<String> permissions) {
        public SaveRoleRequest {
            permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        }
    }

    public record RoleDto(UUID id, String name, String description, boolean systemRole, Set<String> permissions, long userCount) {
        public RoleDto {
            permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        }
    }
}
