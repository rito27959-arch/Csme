package com.csme.shared.security;

/**
 * Modules fonctionnels de CSME ERP. Une permission = un module × une {@link Action}.
 *
 * <p>Le champ {@code phase} indique la phase du plan de développement prévue pour le module.
 * Le client s'en sert pour afficher honnêtement les modules qui n'ont pas encore d'écran
 * (désactivés, avec leur phase) au lieu de boutons sans action.</p>
 */
public enum ErpModule {
    DASHBOARD("Tableau de bord", 2),
    AGENCIES("Agences", 4),
    USERS("Utilisateurs", 5),
    ROLES("Rôles et permissions", 5),
    AUDIT("Journal d'audit", 3),
    HR("Ressources humaines", 6),
    ADMINISTRATION("Administration", 14),
    CUSTOMERS("Clients", 7),
    SUPPLIERS("Fournisseurs", 7),
    PRODUCTS("Produits", 8),
    STOCK("Stocks", 9),
    PURCHASES("Achats", 10),
    SALES("Ventes", 11),
    CASH("Caisse", 12),
    FINANCE("Finances", 13),
    ACCOUNTING("Comptabilité", 13),
    ASSETS("Patrimoine", 14),
    MARKETING("Marketing", 15),
    PRODUCTIVITY("Productivité", 15),
    REPORTS("Rapports", 16),
    SETTINGS("Paramètres", 4);

    private final String label;
    private final int phase;

    ErpModule(String label, int phase) {
        this.label = label;
        this.phase = phase;
    }

    public String label() {
        return label;
    }

    public int phase() {
        return phase;
    }
}
