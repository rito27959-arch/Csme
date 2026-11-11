package com.csme.shared.security;

/** Actions contrôlées par le système de permissions (cahier des charges §6). */
public enum Action {
    VIEW("Consulter"),
    CREATE("Créer"),
    UPDATE("Modifier"),
    DELETE("Supprimer"),
    VALIDATE("Valider"),
    CANCEL("Annuler"),
    EXPORT("Exporter"),
    PRINT("Imprimer");

    private final String label;

    Action(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
