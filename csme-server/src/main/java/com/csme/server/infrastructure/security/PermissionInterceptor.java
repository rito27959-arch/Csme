package com.csme.server.infrastructure.security;

import com.csme.server.application.AuditService;
import com.csme.server.application.SystemTransactions;
import com.csme.server.tenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Contrôle des permissions au niveau de chaque point d'entrée de l'API.
 *
 * <p>Refus par défaut : un contrôleur sans annotation de sécurité ({@link RequiresPermission},
 * {@link SuperAdminOnly}, {@link AuthenticatedOnly} ou {@link PublicEndpoint}) est inaccessible.
 * Un test vérifie en plus qu'aucun point d'entrée n'en est dépourvu.</p>
 */
public class PermissionInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PermissionInterceptor.class);

    private final AuditService audit;
    private final SystemTransactions system;

    public PermissionInterceptor(AuditService audit, SystemTransactions system) {
        this.audit = audit;
        this.system = system;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        if (method.hasMethodAnnotation(PublicEndpoint.class)) {
            return true;
        }
        CurrentActor actor = CurrentActor.current().orElse(null);
        if (actor == null) {
            throw new AccessDeniedException("Authentification requise");
        }
        if (method.hasMethodAnnotation(AuthenticatedOnly.class)) {
            return true;
        }
        SuperAdminOnly superAdminOnly = method.getMethodAnnotation(SuperAdminOnly.class);
        if (superAdminOnly != null) {
            return allowOrDeny(actor.superAdmin() && actor.has(CurrentActor.SUPER_ADMIN_AUTHORITY), request,
                    CurrentActor.SUPER_ADMIN_AUTHORITY);
        }
        RequiresPermission required = method.getMethodAnnotation(RequiresPermission.class);
        if (required != null) {
            String authority = required.module().name() + ":" + required.action().name();
            boolean inCompany = TenantContext.companyId().isPresent();
            return allowOrDeny(inCompany && actor.has(authority), request, authority);
        }
        log.error("Point d'entrée sans annotation de sécurité : {}", method);
        throw new AccessDeniedException("Point d'entrée non configuré");
    }

    private boolean allowOrDeny(boolean allowed, HttpServletRequest request, String authority) {
        if (allowed) {
            return true;
        }
        String endpoint = request.getMethod() + " " + request.getRequestURI();
        try {
            if (TenantContext.companyId().isPresent()) {
                audit.denied(endpoint, authority);
            } else {
                system.run(() -> audit.denied(endpoint, authority));
            }
        } catch (RuntimeException e) {
            log.warn("Impossible de journaliser un refus d'accès : {}", e.getMessage());
        }
        throw new AccessDeniedException("Permission requise : " + authority);
    }
}
