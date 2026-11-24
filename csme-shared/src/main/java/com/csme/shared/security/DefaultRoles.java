package com.csme.shared.security;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Rôles prédéfinis créés automatiquement pour chaque nouvelle entreprise (cahier des charges §6).
 * Ce sont des points de départ : l'entreprise peut ensuite les modifier.
 *
 * <p>Le super administrateur n'est pas ici : c'est un attribut de plateforme, pas un rôle d'entreprise.</p>
 */
public final class DefaultRoles {

    public static final String GENERAL_MANAGER = "Directeur général";

    public record Template(String name, String description, Set<Permission> permissions) {
    }

    private DefaultRoles() {
    }

    public static List<Template> all() {
        return List.of(
                new Template(GENERAL_MANAGER, "Supervision complète de l'entreprise", everything()),
                new Template("Directeur adjoint", "Gestion déléguée",
                        without(everything(), Action.DELETE)),
                new Template("Responsable RH", "Ressources humaines",
                        merge(full(ErpModule.HR), view(ErpModule.DASHBOARD, ErpModule.AGENCIES, ErpModule.DEPARTMENTS))),
                new Template("Comptable", "Gestion financière et comptable",
                        merge(full(ErpModule.FINANCE, ErpModule.ACCOUNTING, ErpModule.CASH),
                                view(ErpModule.DASHBOARD, ErpModule.SALES, ErpModule.PURCHASES, ErpModule.REPORTS))),
                new Template("Responsable commercial", "Ventes et commerciaux",
                        merge(full(ErpModule.SALES, ErpModule.CUSTOMERS, ErpModule.MARKETING),
                                view(ErpModule.DASHBOARD, ErpModule.PRODUCTS, ErpModule.STOCK, ErpModule.REPORTS))),
                new Template("Commercial", "Clients et ventes autorisées",
                        merge(actions(Set.of(ErpModule.SALES, ErpModule.CUSTOMERS), Action.VIEW, Action.CREATE, Action.UPDATE, Action.PRINT),
                                view(ErpModule.DASHBOARD, ErpModule.PRODUCTS))),
                new Template("Responsable des stocks", "Supervision des stocks",
                        merge(full(ErpModule.STOCK, ErpModule.PRODUCTS), view(ErpModule.DASHBOARD, ErpModule.PURCHASES))),
                new Template("Magasinier", "Entrées et sorties autorisées",
                        merge(actions(Set.of(ErpModule.STOCK), Action.VIEW, Action.CREATE), view(ErpModule.PRODUCTS))),
                new Template("Responsable des achats", "Achats et fournisseurs",
                        merge(full(ErpModule.PURCHASES, ErpModule.SUPPLIERS), view(ErpModule.DASHBOARD, ErpModule.STOCK, ErpModule.PRODUCTS))),
                new Template("Caissier", "Encaissements et clôtures",
                        merge(actions(Set.of(ErpModule.CASH), Action.VIEW, Action.CREATE, Action.VALIDATE, Action.PRINT),
                                view(ErpModule.SALES))),
                new Template("Secrétaire", "Administration et documents",
                        merge(actions(Set.of(ErpModule.ADMINISTRATION, ErpModule.PRODUCTIVITY), Action.VIEW, Action.CREATE, Action.UPDATE, Action.PRINT),
                                view(ErpModule.DASHBOARD))),
                new Template("Informaticien", "Assistance et administration technique",
                        merge(full(ErpModule.USERS, ErpModule.SETTINGS), view(ErpModule.ROLES, ErpModule.AUDIT, ErpModule.AGENCIES, ErpModule.DEPARTMENTS))));
    }

    private static Set<Permission> everything() {
        return actions(EnumSet.allOf(ErpModule.class), Action.values());
    }

    private static Set<Permission> full(ErpModule... modules) {
        return actions(Set.of(modules), Action.values());
    }

    private static Set<Permission> view(ErpModule... modules) {
        return actions(Set.of(modules), Action.VIEW);
    }

    private static Set<Permission> actions(Set<ErpModule> modules, Action... actions) {
        Set<Permission> result = new LinkedHashSet<>();
        for (ErpModule module : modules) {
            for (Action action : actions) {
                result.add(Permission.of(module, action));
            }
        }
        return Set.copyOf(result);
    }

    private static Set<Permission> without(Set<Permission> source, Action excluded) {
        Set<Permission> result = new LinkedHashSet<>(source);
        result.removeIf(p -> p.action() == excluded);
        return Set.copyOf(result);
    }

    @SafeVarargs
    private static Set<Permission> merge(Set<Permission>... sets) {
        Set<Permission> result = new LinkedHashSet<>();
        for (Set<Permission> set : sets) {
            result.addAll(set);
        }
        return Set.copyOf(result);
    }
}
