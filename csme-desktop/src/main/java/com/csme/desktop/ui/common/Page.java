package com.csme.desktop.ui.common;

import javafx.scene.Node;

/** Une page de la zone centrale. */
public interface Page {

    Node view();

    /** Appelée à chaque affichage (rechargement des données). */
    default void onShow() {
    }
}
