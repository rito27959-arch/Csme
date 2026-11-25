package com.csme.desktop.ui.common;

import java.util.Optional;
import javafx.scene.Node;

/** Une page de la zone centrale. */
public interface Page {

    Node view();

    /** Appelée à chaque affichage et par Ctrl+R / F5 (rechargement des données). */
    default void onShow() {
    }

    /**
     * Action principale de la page (« Nouvel utilisateur », « Nouvelle agence »…), déclenchée par Ctrl+N
     * et par la palette de commandes. Vide si l'utilisateur n'en a pas le droit.
     */
    default Optional<PrimaryAction> primaryAction() {
        return Optional.empty();
    }

    /** Ctrl+F : place le curseur dans la recherche de la page, s'il y en a une. */
    default void focusSearch() {
    }

    record PrimaryAction(String label, String icon, Runnable run) {
    }
}
