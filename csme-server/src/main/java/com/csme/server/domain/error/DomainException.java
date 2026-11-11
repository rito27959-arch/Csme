package com.csme.server.domain.error;

/** Erreur métier : message destiné à l'utilisateur, code stable destiné au client. */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
