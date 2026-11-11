package com.csme.server.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Adresse IP et agent client de la requête HTTP courante (si elle existe).
 * L'adresse tient compte des en-têtes du proxy inverse via {@code server.forward-headers-strategy}.
 */
public record RequestInfo(String ipAddress, String userAgent) {

    public static final RequestInfo NONE = new RequestInfo(null, null);

    public static RequestInfo current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return new RequestInfo(truncate(request.getRemoteAddr(), 64), truncate(request.getHeader("User-Agent"), 255));
        }
        return NONE;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
