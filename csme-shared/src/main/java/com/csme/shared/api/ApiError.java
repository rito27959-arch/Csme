package com.csme.shared.api;

import java.time.Instant;
import java.util.Map;

/**
 * Corps d'erreur unique renvoyé par l'API. Ne contient jamais de trace technique :
 * {@code traceId} permet de retrouver le détail dans les journaux du serveur.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        Map<String, String> fieldErrors,
        String traceId) {

    public ApiError {
        fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
    }
}
