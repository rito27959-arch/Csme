package com.csme.desktop.ui.common;

import java.io.IOException;
import java.io.InputStream;
import javafx.scene.text.Font;

/**
 * Charge la police Inter (licence SIL OFL, voir fonts/OFL-LICENSE.txt), dessinée pour les écrans et très proche
 * de la police système d'Apple. Les feuilles de style l'utilisent ensuite via {@code -fx-font-family: "Inter"}.
 */
public final class Fonts {

    private static final String[] FILES = {
        "Inter-Regular", "Inter-Medium", "Inter-SemiBold", "Inter-Bold", "InterDisplay-SemiBold", "InterDisplay-Bold"
    };

    private Fonts() {
    }

    public static void load() {
        for (String name : FILES) {
            try (InputStream in = Fonts.class.getResourceAsStream("/com/csme/desktop/fonts/" + name + ".ttf")) {
                if (in != null) {
                    Font.loadFont(in, 13);
                }
            } catch (IOException ignored) {
                // Police de secours : celle du système. L'application reste utilisable.
            }
        }
    }
}
