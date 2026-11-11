package com.csme.server.domain.rules;

import java.util.Locale;
import java.util.Optional;

/** Politique de mot de passe minimale. Les règles plus strictes pourront devenir configurables. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    public static Optional<String> violation(String password, String email) {
        if (password == null || password.isEmpty()) {
            return Optional.of("Le mot de passe est obligatoire.");
        }
        if (password.length() < MIN_LENGTH) {
            return Optional.of("Le mot de passe doit contenir au moins " + MIN_LENGTH + " caractères.");
        }
        if (password.length() > MAX_LENGTH) {
            return Optional.of("Le mot de passe ne doit pas dépasser " + MAX_LENGTH + " caractères.");
        }
        boolean letter = password.chars().anyMatch(Character::isLetter);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        if (!letter || !digit) {
            return Optional.of("Le mot de passe doit contenir au moins une lettre et un chiffre.");
        }
        if (email != null && !email.isBlank()) {
            String local = email.toLowerCase(Locale.ROOT).split("@", 2)[0];
            if (local.length() >= 4 && password.toLowerCase(Locale.ROOT).contains(local)) {
                return Optional.of("Le mot de passe ne doit pas contenir l'identifiant de l'adresse électronique.");
            }
        }
        return Optional.empty();
    }
}
