package com.csme.desktop.api;

import java.util.Map;

/** Erreur renvoyée par l'API (ou réseau indisponible, statut 0), avec un message lisible par l'utilisateur. */
public class ApiException extends RuntimeException {

    private final int status;
    private final String code;
    private final Map<String, String> fieldErrors;

    public ApiException(int status, String code, String message, Map<String, String> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }

    public boolean isNetworkError() {
        return status == 0;
    }
}
