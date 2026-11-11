package com.csme.server.domain.rules;

import com.csme.server.domain.error.ValidationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validation côté serveur, explicite et lisible. Toutes les entrées de l'API passent par ici :
 * la validation du client n'est qu'un confort.
 */
public final class Checks {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]{1,64}@[^@\\s]+\\.[^@\\s]{2,}$");

    private final Map<String, String> errors = new LinkedHashMap<>();

    private Checks() {
    }

    public static Checks start() {
        return new Checks();
    }

    public Checks required(String field, String value, String label) {
        if (value == null || value.isBlank()) {
            errors.putIfAbsent(field, label + " est obligatoire.");
        }
        return this;
    }

    public Checks maxLength(String field, String value, int max, String label) {
        if (value != null && value.strip().length() > max) {
            errors.putIfAbsent(field, label + " ne doit pas dépasser " + max + " caractères.");
        }
        return this;
    }

    public Checks matches(String field, String value, Pattern pattern, String message) {
        if (value != null && !value.isBlank() && !pattern.matcher(value.strip()).matches()) {
            errors.putIfAbsent(field, message);
        }
        return this;
    }

    public Checks email(String field, String value) {
        return matches(field, value, EMAIL, "Adresse électronique invalide.");
    }

    public Checks password(String field, String password, String email) {
        PasswordPolicy.violation(password, email).ifPresent(msg -> errors.putIfAbsent(field, msg));
        return this;
    }

    public Checks rule(boolean ok, String field, String message) {
        if (!ok) {
            errors.putIfAbsent(field, message);
        }
        return this;
    }

    public void throwIfInvalid() {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public static String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(java.util.Locale.ROOT);
    }
}
