package com.csme.server.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Après vérification du jeton, fixe la portée entreprise du thread à partir de la revendication
 * {@code cid} signée par le serveur. Le super administrateur n'a pas de portée par défaut : ses
 * opérations plateforme s'élèvent explicitement en portée système dans le code.
 */
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String COMPANY_CLAIM = "cid";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        TenantContext.clear();
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                String cid = jwt.getClaimAsString(COMPANY_CLAIM);
                if (cid != null && !cid.isBlank()) {
                    TenantContext.set(new TenantScope.Company(UUID.fromString(cid)));
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
