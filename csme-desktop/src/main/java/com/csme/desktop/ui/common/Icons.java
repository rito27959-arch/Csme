package com.csme.desktop.ui.common;

import java.util.Map;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Icônes vectorielles au trait (grille 24×24), dessinées pour CSME ERP. La couleur suit le texte
 * environnant via la classe CSS {@code csme-icon}.
 */
public final class Icons {

    private static final Map<String, String> PATHS = Map.ofEntries(
            Map.entry("dashboard", "M4 4h7v7H4z M13 4h7v4h-7z M13 10h7v10h-7z M4 13h7v7H4z"),
            Map.entry("building", "M4 21V4h10v17 M14 9h6v12 M2 21h20 M7.5 8h3 M7.5 12h3 M7.5 16h3 M17 13h0.01 M17 17h0.01"),
            Map.entry("users", "M9 11a4 4 0 1 0 0-8a4 4 0 0 0 0 8z M2 21v-1a6 6 0 0 1 6-6h2a6 6 0 0 1 6 6v1 M16 3.5a4 4 0 0 1 0 7.5 M22 21v-1a6 6 0 0 0-4-5.6"),
            Map.entry("pin", "M12 21c0 0-7-6.2-7-11a7 7 0 0 1 14 0c0 4.8-7 11-7 11z M12 12.5a2.5 2.5 0 1 0 0-5a2.5 2.5 0 0 0 0 5z"),
            Map.entry("shield", "M12 3l8 3v6c0 5-3.5 8-8 9c-4.5-1-8-4-8-9V6z M9 12l2 2l4-4"),
            Map.entry("logout", "M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4 M10 17l5-5l-5-5 M15 12H3"),
            Map.entry("sun", "M12 16a4 4 0 1 0 0-8a4 4 0 0 0 0 8z M12 2v2 M12 20v2 M4.9 4.9l1.4 1.4 M17.7 17.7l1.4 1.4 M2 12h2 M20 12h2 M4.9 19.1l1.4-1.4 M17.7 6.3l1.4-1.4"),
            Map.entry("moon", "M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"),
            Map.entry("plus", "M12 5v14 M5 12h14"),
            Map.entry("search", "M11 18a7 7 0 1 0 0-14a7 7 0 0 0 0 14z M21 21l-4.3-4.3"),
            Map.entry("refresh", "M20 12a8 8 0 1 1-2.3-5.7 M20 4v5h-5"),
            Map.entry("lock", "M6 11h12v10H6z M8 11V7a4 4 0 0 1 8 0v4"),
            Map.entry("key", "M15 9a3 3 0 1 0 0-0.01 M12.5 11.5L3 21 M6 18l2 2 M8 16l2 2"),
            Map.entry("globe", "M12 21a9 9 0 1 0 0-18a9 9 0 0 0 0 18z M3 12h18 M12 3c2.5 2.5 3.5 5.5 3.5 9s-1 6.5-3.5 9 M12 3c-2.5 2.5-3.5 5.5-3.5 9s1 6.5 3.5 9"),
            Map.entry("box", "M21 8l-9-5l-9 5l9 5z M3 8v8l9 5l9-5V8 M12 13v8"),
            Map.entry("cart", "M3 4h2l2.4 11h11l2-8H6.2 M9 20a1 1 0 1 0 0-0.01 M18 20a1 1 0 1 0 0-0.01"),
            Map.entry("wallet", "M3 7h18v13H3z M3 7l3-4h12l3 4 M16 13.5h0.01"),
            Map.entry("chart", "M4 20V10 M10 20V4 M16 20v-7 M22 20H2"),
            Map.entry("briefcase", "M3 7h18v13H3z M8 7V4h8v3 M3 13h18"),
            Map.entry("megaphone", "M3 10v4h4l6 4V6l-6 4z M17 9a4 4 0 0 1 0 6"),
            Map.entry("check", "M5 12l5 5L20 7"),
            Map.entry("tasks", "M9 6h11 M9 12h11 M9 18h11 M4 6l1 1l2-2 M4 12l1 1l2-2 M4 18l1 1l2-2"),
            Map.entry("settings", "M12 15a3 3 0 1 0 0-6a3 3 0 0 0 0 6z M12 2v3 M12 19v3 M2 12h3 M19 12h3 M4.9 4.9l2.1 2.1 M17 17l2.1 2.1 M4.9 19.1L7 17 M17 7l2.1-2.1"),
            Map.entry("truck", "M2 6h12v10H2z M14 10h4l3 3v3h-7 M6 19a2 2 0 1 0 0-0.01 M17 19a2 2 0 1 0 0-0.01"),
            Map.entry("factory", "M2 21V10l6 4V10l6 4V6h8v15z M2 21h20"),
            Map.entry("file", "M6 3h8l5 5v13H6z M14 3v5h5 M9 13h6 M9 17h6"),
            Map.entry("more", "M5 11h2v2H5z M11 11h2v2h-2z M17 11h2v2h-2z"),
            Map.entry("edit", "M4 20h4L19 9l-4-4L4 16z M13.5 6.5l4 4"),
            Map.entry("power", "M12 3v8 M6.3 6.3a8 8 0 1 0 11.4 0"),
            Map.entry("arrow-left", "M15 18l-6-6l6-6"),
            Map.entry("arrow-right", "M9 18l6-6l-6-6"),
            Map.entry("chevron-down", "M6 9l6 6l6-6"),
            Map.entry("eye", "M2 12s3.5-7 10-7s10 7 10 7s-3.5 7-10 7S2 12 2 12z M12 15a3 3 0 1 0 0-6a3 3 0 0 0 0 6z"),
            Map.entry("eye-off", "M3 3l18 18 M10.6 5.1A10.9 10.9 0 0 1 12 5c6.5 0 10 7 10 7a17.6 17.6 0 0 1-3.2 4.2 M6.6 6.6A17.4 17.4 0 0 0 2 12s3.5 7 10 7a10 10 0 0 0 5.4-1.6 M9.9 9.9a3 3 0 0 0 4.2 4.2"),
            Map.entry("sparkle", "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z M19 16l.7 2l2 .7l-2 .7l-.7 2l-.7-2l-2-.7l2-.7z"),
            Map.entry("calendar", "M4 6h16v15H4z M4 10h16 M8 3v4 M16 3v4"),
            Map.entry("layers", "M12 3l9 5l-9 5l-9-5z M3 13l9 5l9-5 M3 17.5l9 5l9-5"),
            Map.entry("trash", "M4 7h16 M9 7V4h6v3 M6 7l1 13h10l1-13 M10 11v6 M14 11v6"),
            Map.entry("clock", "M12 21a9 9 0 1 0 0-18a9 9 0 0 0 0 18z M12 7v5l3 2"),
            Map.entry("activity", "M3 12h4l3-8l4 16l3-8h4"));

    private Icons() {
    }

    public static Node of(String name, double size) {
        String content = PATHS.get(name);
        if (content == null) {
            throw new IllegalArgumentException("Icône inconnue : " + name);
        }
        SVGPath path = new SVGPath();
        path.setContent(content);
        path.getStyleClass().add("csme-icon");
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        double scale = size / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);
        StackPane box = new StackPane(new Group(path));
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        box.getStyleClass().add("csme-icon-box");
        return box;
    }
}
