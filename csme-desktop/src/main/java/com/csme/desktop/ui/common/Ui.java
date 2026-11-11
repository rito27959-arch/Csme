package com.csme.desktop.ui.common;

import atlantafx.base.theme.Styles;
import java.util.Map;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Petits composants réutilisables, au style macOS (AtlantaFX Cupertino + feuille csme.css). */
public final class Ui {

    private Ui() {
    }

    public static Button primary(String text, String icon) {
        Button b = new Button(text);
        if (icon != null) {
            b.setGraphic(Icons.of(icon, 15));
        }
        b.getStyleClass().add(Styles.ACCENT);
        b.setDefaultButton(false);
        return b;
    }

    public static Button secondary(String text, String icon) {
        Button b = new Button(text);
        if (icon != null) {
            b.setGraphic(Icons.of(icon, 15));
        }
        return b;
    }

    public static Button iconButton(String icon, String tooltip) {
        Button b = new Button(null, Icons.of(icon, 16));
        b.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        b.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        b.setAccessibleText(tooltip);
        return b;
    }

    public static Label badge(String text, String tone) {
        Label l = new Label(text);
        l.getStyleClass().addAll("csme-badge", "csme-badge-" + tone);
        return l;
    }

    public static Label caption(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);
        return l;
    }

    public static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Carte de contenu : coins arrondis, bordure fine, fond surélevé. */
    public static VBox card(Node... children) {
        VBox box = new VBox(12, children);
        box.getStyleClass().add("csme-card");
        box.setPadding(new Insets(18));
        return box;
    }

    /** En-tête de page : titre, sous-titre, actions à droite. */
    public static HBox pageHeader(String title, String subtitle, Node... actions) {
        Label t = new Label(title);
        t.getStyleClass().add(Styles.TITLE_2);
        VBox texts = new VBox(2, t);
        if (subtitle != null) {
            texts.getChildren().add(caption(subtitle));
        }
        HBox header = new HBox(10, texts, spacer());
        header.getChildren().addAll(actions);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    /** Bandeau d'erreur lié au ViewModel ; invisible lorsqu'il n'y a pas d'erreur. */
    public static Label errorBanner(StringProperty error) {
        Label l = new Label();
        l.textProperty().bind(error);
        l.getStyleClass().add("csme-error-banner");
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        l.managedProperty().bind(l.visibleProperty());
        l.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> error.get() != null && !error.get().isBlank(), error));
        return l;
    }

    public static ProgressIndicator spinner(javafx.beans.value.ObservableBooleanValue visible) {
        ProgressIndicator p = new ProgressIndicator();
        p.setPrefSize(18, 18);
        p.setMaxSize(18, 18);
        p.visibleProperty().bind(visible);
        p.managedProperty().bind(visible);
        return p;
    }

    /** Grille de formulaire « libellé au-dessus du champ », avec message d'erreur sous chaque champ. */
    public static final class Form {
        private final GridPane grid = new GridPane();
        private final Map<String, Label> errors = new java.util.HashMap<>();
        private int row;

        public Form() {
            grid.setHgap(14);
            grid.setVgap(4);
            javafx.scene.layout.ColumnConstraints c = new javafx.scene.layout.ColumnConstraints();
            c.setHgrow(Priority.ALWAYS);
            c.setFillWidth(true);
            grid.getColumnConstraints().add(c);
        }

        public Form field(String key, String label, Node input) {
            Label l = new Label(label);
            l.getStyleClass().add("csme-field-label");
            Label err = new Label();
            err.getStyleClass().add("csme-field-error");
            err.setWrapText(true);
            err.managedProperty().bind(err.visibleProperty());
            err.setVisible(false);
            errors.put(key, err);
            if (input instanceof Region r) {
                r.setMaxWidth(Double.MAX_VALUE);
            }
            VBox cell = new VBox(5, l, input, err);
            cell.setPadding(new Insets(0, 0, 8, 0));
            grid.add(cell, 0, row++);
            return this;
        }

        public Form full(Node node) {
            grid.add(node, 0, row++);
            return this;
        }

        public void bindErrors(ObjectProperty<Map<String, String>> fieldErrors) {
            Consumer<Map<String, String>> apply = map -> errors.forEach((key, label) -> {
                String msg = map == null ? null : map.get(key);
                label.setText(msg == null ? "" : msg);
                label.setVisible(msg != null);
            });
            fieldErrors.addListener((obs, o, n) -> apply.accept(n));
            apply.accept(fieldErrors.get());
        }

        public GridPane node() {
            return grid;
        }
    }
}
