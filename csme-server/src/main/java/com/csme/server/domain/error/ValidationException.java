package com.csme.server.domain.error;

import java.util.Map;

/** Données invalides, avec un message par champ. */
public class ValidationException extends DomainException {

    private final Map<String, String> fieldErrors;

    public ValidationException(Map<String, String> fieldErrors) {
        super("VALIDATION_FAILED", "Certaines informations sont invalides.");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
