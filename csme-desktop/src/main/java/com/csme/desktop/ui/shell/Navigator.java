package com.csme.desktop.ui.shell;

/** Permet à une page d'en ouvrir une autre (ex. le tableau de bord qui mène à « Nouvelle agence »). */
public interface Navigator {

    void go(Destination destination);

    /** Ouvre la rubrique puis lance son action principale (création). */
    void goAndCreate(Destination destination);

    /** Vrai si la rubrique existe et que l'utilisateur peut la consulter. */
    boolean canGo(Destination destination);
}
