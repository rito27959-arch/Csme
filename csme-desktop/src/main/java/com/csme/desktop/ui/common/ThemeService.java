package com.csme.desktop.ui.common;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import java.util.prefs.Preferences;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/** Thème Cupertino (AtlantaFX) clair ou sombre, mémorisé dans les préférences de l'utilisateur Windows. */
public final class ThemeService {

    private static final String KEY = "theme.dark";
    private final Preferences prefs = Preferences.userNodeForPackage(ThemeService.class);
    private final BooleanProperty dark = new SimpleBooleanProperty();

    public ThemeService() {
        dark.set(prefs.getBoolean(KEY, false));
        apply(dark.get());
        dark.addListener((obs, o, n) -> {
            apply(n);
            prefs.putBoolean(KEY, n);
        });
    }

    public BooleanProperty darkProperty() {
        return dark;
    }

    public void toggle() {
        dark.set(!dark.get());
    }

    private static void apply(boolean isDark) {
        Application.setUserAgentStylesheet(isDark
                ? new CupertinoDark().getUserAgentStylesheet()
                : new CupertinoLight().getUserAgentStylesheet());
    }
}
