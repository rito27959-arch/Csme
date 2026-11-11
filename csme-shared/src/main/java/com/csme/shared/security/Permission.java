package com.csme.shared.security;

import java.util.Objects;

/**
 * Permission atomique : un module et une action. Sa forme textuelle {@code MODULE:ACTION}
 * (ex. {@code USERS:CREATE}) est celle transportée dans le jeton d'accès et vérifiée côté serveur.
 */
public record Permission(ErpModule module, Action action) {

    private static final char SEPARATOR = ':';

    public Permission {
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(action, "action");
    }

    public static Permission of(ErpModule module, Action action) {
        return new Permission(module, action);
    }

    /** Analyse {@code MODULE:ACTION}. Lève {@link IllegalArgumentException} si le format est invalide. */
    public static Permission parse(String authority) {
        if (authority == null) {
            throw new IllegalArgumentException("Permission nulle");
        }
        int idx = authority.indexOf(SEPARATOR);
        if (idx <= 0 || idx != authority.lastIndexOf(SEPARATOR) || idx == authority.length() - 1) {
            throw new IllegalArgumentException("Permission invalide : " + authority);
        }
        return new Permission(
                ErpModule.valueOf(authority.substring(0, idx)),
                Action.valueOf(authority.substring(idx + 1)));
    }

    public String authority() {
        return module.name() + SEPARATOR + action.name();
    }

    @Override
    public String toString() {
        return authority();
    }
}
