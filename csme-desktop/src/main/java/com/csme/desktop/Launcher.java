package com.csme.desktop;

/**
 * Point d'entrée de l'exécutable. Classe distincte de {@link CsmeDesktopApp} pour que JavaFX
 * démarre correctement depuis le classpath (installateur jpackage, jar exécutable).
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        javafx.application.Application.launch(CsmeDesktopApp.class, args);
    }
}
