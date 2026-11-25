package com.csme.desktop.ui.shell;

import com.csme.shared.security.ErpModule;

/**
 * Entrées de navigation. {@code module == null} : entrée réservée au super administrateur.
 * Les modules sans écran dans cette version apparaissent désactivés avec leur phase prévue.
 */
public enum Destination {
    // Plateforme (super administrateur)
    PLATFORM_OVERVIEW("Vue d'ensemble", "globe", null, true, "blue"),
    PLATFORM_COMPANIES("Entreprises", "building", null, true, "indigo"),
    PLATFORM_AUDIT("Journal plateforme", "shield", null, true, "gray"),

    // Entreprise
    DASHBOARD("Tableau de bord", "dashboard", ErpModule.DASHBOARD, true, "blue"),
    AGENCIES("Agences", "pin", ErpModule.AGENCIES, true, "red"),
    DEPARTMENTS("Départements", "layers", ErpModule.DEPARTMENTS, true, "orange"),
    USERS("Utilisateurs", "users", ErpModule.USERS, true, "indigo"),
    ROLES("Rôles et permissions", "key", ErpModule.ROLES, true, "yellow"),
    AUDIT("Journal d'audit", "shield", ErpModule.AUDIT, true, "gray"),
    SETTINGS("Paramètres", "settings", ErpModule.SETTINGS, true, "graphite"),

    // À venir
    HR("Ressources humaines", "briefcase", ErpModule.HR, false, "teal"),
    CUSTOMERS("Clients", "users", ErpModule.CUSTOMERS, false, "pink"),
    SUPPLIERS("Fournisseurs", "truck", ErpModule.SUPPLIERS, false, "brown"),
    PRODUCTS("Produits", "box", ErpModule.PRODUCTS, false, "orange"),
    STOCK("Stocks", "box", ErpModule.STOCK, false, "mint"),
    PURCHASES("Achats", "cart", ErpModule.PURCHASES, false, "purple"),
    SALES("Ventes", "cart", ErpModule.SALES, false, "green"),
    CASH("Caisse", "wallet", ErpModule.CASH, false, "green"),
    FINANCE("Finances", "wallet", ErpModule.FINANCE, false, "teal"),
    ACCOUNTING("Comptabilité", "file", ErpModule.ACCOUNTING, false, "blue"),
    ASSETS("Patrimoine", "factory", ErpModule.ASSETS, false, "brown"),
    MARKETING("Marketing", "megaphone", ErpModule.MARKETING, false, "pink"),
    PRODUCTIVITY("Productivité", "tasks", ErpModule.PRODUCTIVITY, false, "purple"),
    REPORTS("Rapports", "chart", ErpModule.REPORTS, false, "indigo");

    private final String label;
    private final String icon;
    private final ErpModule module;
    private final boolean available;
    private final String tint;

    Destination(String label, String icon, ErpModule module, boolean available, String tint) {
        this.label = label;
        this.icon = icon;
        this.module = module;
        this.available = available;
        this.tint = tint;
    }

    /** Couleur de la tuile d'icône (classe CSS {@code csme-tint-<couleur>}), à la manière de Réglages sur iOS. */
    public String tint() {
        return tint;
    }

    public String label() {
        return label;
    }

    public String icon() {
        return icon;
    }

    public ErpModule module() {
        return module;
    }

    public boolean available() {
        return available;
    }

    public boolean platform() {
        return module == null;
    }
}
