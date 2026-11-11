package com.csme.server.domain.error;

/** Échec d'authentification. Le message reste volontairement générique. */
public class AuthenticationFailedException extends DomainException {
    public AuthenticationFailedException(String code) {
        super(code, "Identifiants invalides ou session expirée.");
    }
}
