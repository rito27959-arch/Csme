package com.csme.desktop.ui.shell;

import com.csme.shared.security.ErpModule;

/**
 * Entrées de navigation. {@code module == null} : entrée réservée au super administrateur.
 * Les modules sans écran dans cette version apparaissent désactivés avec leur phase prévue.
 */
public enum Destination {
    // Plateforme (super administrateur)
    PLATFORM_OVERVIEW("Vue d'ensemble", "globe", null, true),
    PLATFORM_COMPANIES("Entreprises", "building", null, true),
    PLATFORM_AUDIT("Journal plateforme", "shield", null, true),

    // Entreprise
    DASHBOARD("Tableau de bord", "dashboard", ErpModule.DASHBOARD, true),
    AGENCIES("Agences", "pin", ErpModule.AGENCIES, true),
    USERS("Utilisateurs", "users", ErpModule.USERS, true),
    AUDIT("Journal d'audit", "shield", ErpModule.AUDIT, true),

    // À venir
    HR("Ressources humaines", "briefcase", ErpModule.HR, false),
    CUSTOMERS("Clients", "users", ErpModule.CUSTOMERS, false),
    SUPPLIERS("Fournisseurs", "truck", ErpModule.SUPPLIERS, false),
    PRODUCTS("Produits", "box", ErpModule.PRODUCTS, false),
    STOCK("Stocks", "box", ErpModule.STOCK, false),
    PURCHASES("Achats", "cart", ErpModule.PURCHASES, false),
    SALES("Ventes", "cart", ErpModule.SALES, false),
    CASH("Caisse", "wallet", ErpModule.CASH, false),
    FINANCE("Finances", "wallet", ErpModule.FINANCE, false),
    ACCOUNTING("Comptabilité", "file", ErpModule.ACCOUNTING, false),
    ASSETS("Patrimoine", "factory", ErpModule.ASSETS, false),
    MARKETING("Marketing", "megaphone", ErpModule.MARKETING, false),
    PRODUCTIVITY("Productivité", "tasks", ErpModule.PRODUCTIVITY, false),
    REPORTS("Rapports", "chart", ErpModule.REPORTS, false);

    private final String label;
    private final String icon;
    private final ErpModule module;
    private final boolean available;

    Destination(String label, String icon, ErpModule module, boolean available) {
        this.label = label;
        this.icon = icon;
        this.module = module;
        this.available = available;
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
