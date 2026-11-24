package com.csme.desktop.ui.common;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/** Mise en forme française des dates et libellés. */
public final class Formats {

    private static final Locale FR = Locale.FRANCE;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", FR);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", FR);

    private static final Map<String, String> ACTIONS = Map.ofEntries(
            Map.entry("LOGIN_SUCCESS", "Connexion"),
            Map.entry("LOGIN_FAILURE", "Échec de connexion"),
            Map.entry("LOGOUT", "Déconnexion"),
            Map.entry("REFRESH_TOKEN_REUSE", "Réutilisation de jeton"),
            Map.entry("REFRESH_DENIED", "Session refusée"),
            Map.entry("PASSWORD_CHANGE", "Changement de mot de passe"),
            Map.entry("ACCESS_DENIED", "Accès refusé"),
            Map.entry("COMPANY_CREATED", "Entreprise créée"),
            Map.entry("COMPANY_ACTIVATED", "Entreprise activée"),
            Map.entry("COMPANY_DEACTIVATED", "Entreprise désactivée"),
            Map.entry("AGENCY_CREATED", "Agence créée"),
            Map.entry("AGENCY_UPDATED", "Agence modifiée"),
            Map.entry("AGENCY_ACTIVATED", "Agence activée"),
            Map.entry("AGENCY_DEACTIVATED", "Agence désactivée"),
            Map.entry("USER_CREATED", "Utilisateur créé"),
            Map.entry("USER_UPDATED", "Utilisateur modifié"),
            Map.entry("USER_ACTIVATED", "Utilisateur activé"),
            Map.entry("USER_DEACTIVATED", "Utilisateur désactivé"),
            Map.entry("USER_UNLOCKED", "Utilisateur déverrouillé"),
            Map.entry("USER_PASSWORD_RESET", "Mot de passe réinitialisé"),
            Map.entry("DEPARTMENT_CREATED", "Département créé"),
            Map.entry("DEPARTMENT_UPDATED", "Département modifié"),
            Map.entry("DEPARTMENT_ACTIVATED", "Département activé"),
            Map.entry("DEPARTMENT_DEACTIVATED", "Département désactivé"),
            Map.entry("ROLE_CREATED", "Rôle créé"),
            Map.entry("ROLE_UPDATED", "Rôle modifié"),
            Map.entry("ROLE_DELETED", "Rôle supprimé"),
            Map.entry("COMPANY_PROFILE_UPDATED", "Informations de l'entreprise modifiées"));

    private Formats() {
    }

    public static String dateTime(Instant instant) {
        return instant == null ? "—" : DATE_TIME.format(instant.atZone(ZoneId.systemDefault()));
    }

    public static String date(Instant instant) {
        return instant == null ? "—" : DATE.format(instant.atZone(ZoneId.systemDefault()));
    }

    public static String action(String code) {
        return code == null ? "" : ACTIONS.getOrDefault(code, code);
    }

    public static java.util.Set<String> knownActions() {
        return new java.util.TreeSet<>(ACTIONS.keySet());
    }

    public static String number(long value) {
        return String.format(FR, "%,d", value).replace(' ', ' ').replace(' ', ' ');
    }

    public static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] parts = fullName.strip().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(FR);
    }
}
