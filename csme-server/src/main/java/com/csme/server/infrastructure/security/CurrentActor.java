package com.csme.server.infrastructure.security;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.stream.Collectors;

/** Utilisateur à l'origine de la requête courante, tel qu'établi par le jeton vérifié. */
public record CurrentActor(UUID userId, UUID companyId, String email, boolean superAdmin, Set<String> authorities) {

    public static final String SUPER_ADMIN_AUTHORITY = "ROLE_SUPER_ADMIN";

    public static Optional<CurrentActor> current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        String cid = jwt.getClaimAsString(com.csme.server.tenancy.TenantContextFilter.COMPANY_CLAIM);
        Boolean sa = jwt.getClaimAsBoolean(JwtService.CLAIM_SUPER_ADMIN);
        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        return Optional.of(new CurrentActor(
                UUID.fromString(jwt.getSubject()),
                cid == null ? null : UUID.fromString(cid),
                jwt.getClaimAsString(JwtService.CLAIM_EMAIL),
                Boolean.TRUE.equals(sa),
                authorities));
    }

    public static CurrentActor require() {
        return current().orElseThrow(() -> new IllegalStateException("Aucun utilisateur authentifié"));
    }

    public boolean has(String authority) {
        return authorities.contains(authority);
    }
}
