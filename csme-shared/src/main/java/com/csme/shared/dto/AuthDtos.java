package com.csme.shared.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Contrats d'authentification. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(String email, String password) {
        @Override
        public String toString() {
            return "LoginRequest[email=" + email + ", password=***]";
        }
    }

    public record RefreshRequest(String refreshToken) {
        @Override
        public String toString() {
            return "RefreshRequest[***]";
        }
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {
        @Override
        public String toString() {
            return "ChangePasswordRequest[***]";
        }
    }

    public record AgencyRef(UUID id, String code, String name) {
    }

    /** Utilisateur connecté tel que le client doit le connaître. */
    public record CurrentUser(
            UUID id,
            String email,
            String fullName,
            String jobTitle,
            UUID companyId,
            String companyName,
            boolean superAdmin,
            Set<String> permissions,
            List<AgencyRef> agencies,
            Instant lastLoginAt) {

        public CurrentUser {
            permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
            agencies = agencies == null ? List.of() : List.copyOf(agencies);
        }

        public boolean can(String authority) {
            return permissions.contains(authority);
        }
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInSeconds,
            CurrentUser user) {
        @Override
        public String toString() {
            return "TokenResponse[user=" + user.email() + ", expiresIn=" + expiresInSeconds + "]";
        }
    }
}
