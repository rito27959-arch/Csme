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
import atlantafx.base.controls.CustomTextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Composants réutilisables au style Apple (AtlantaFX Cupertino + feuille csme.css). */
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

    /** Menu « … » d'une ligne de tableau, comme les menus contextuels d'iOS. */
    public static javafx.scene.control.MenuButton rowMenu(javafx.scene.control.MenuItem... items) {
        javafx.scene.control.MenuButton menu = new javafx.scene.control.MenuButton(null, Icons.of("more", 16));
        menu.getStyleClass().addAll(Styles.FLAT, Styles.BUTTON_ICON, atlantafx.base.theme.Tweaks.NO_ARROW);
        menu.setAccessibleText("Actions");
        menu.getItems().addAll(items);
        return menu;
    }

    public static javafx.scene.control.MenuItem menuItem(String text, String icon, Runnable action) {
        javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(text, icon == null ? null : Icons.of(icon, 14));
        item.setOnAction(e -> action.run());
        return item;
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

    /** En-tête de page à la manière d'iOS : grand titre, sous-titre discret, actions à droite. */
    public static HBox pageHeader(String title, String subtitle, Node... actions) {
        Label t = new Label(title);
        t.getStyleClass().add("csme-large-title");
        VBox texts = new VBox(3, t);
        if (subtitle != null) {
            Label sub = new Label(subtitle);
            sub.getStyleClass().add("csme-subtitle");
            texts.getChildren().add(sub);
        }
        HBox header = new HBox(10, texts, spacer());
        header.getChildren().addAll(actions);
        header.setAlignment(Pos.BOTTOM_LEFT);
        return header;
    }

    public static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("csme-section-title");
        return l;
    }

    /** Tuile d'icône colorée, comme dans l'application Réglages d'iOS. */
    public static StackPane tile(String icon, String tint, double size) {
        StackPane t = new StackPane(Icons.of(icon, size * 0.62));
        t.getStyleClass().addAll("csme-tile", "csme-tint-" + tint);
        t.setMinSize(size, size);
        t.setPrefSize(size, size);
        t.setMaxSize(size, size);
        t.setStyle("-fx-background-radius: " + Math.round(size * 0.27) + "px;");
        return t;
    }

    private static final String[] AVATAR_TINTS = {"blue", "indigo", "purple", "pink", "orange", "teal", "green", "brown"};

    /** Pastille ronde aux initiales ; la couleur dépend du nom, donc reste stable d'un écran à l'autre. */
    public static StackPane avatar(String fullName, double size) {
        Label initials = new Label(Formats.initials(fullName));
        initials.setStyle("-fx-font-size: " + Math.round(size * 0.4) + "px;");
        StackPane a = new StackPane(initials);
        String tint = AVATAR_TINTS[Math.floorMod(fullName == null ? 0 : fullName.hashCode(), AVATAR_TINTS.length)];
        a.getStyleClass().addAll("csme-avatar", "csme-tint-" + tint);
        a.setMinSize(size, size);
        a.setPrefSize(size, size);
        a.setMaxSize(size, size);
        return a;
    }

    /** Champ de recherche arrondi avec loupe. */
    public static CustomTextField searchField(String prompt, double width) {
        CustomTextField field = new CustomTextField();
        field.setPromptText(prompt);
        field.setPrefWidth(width);
        field.setLeft(Icons.of("search", 14));
        return field;
    }

    /** État vide explicatif (icône, titre, texte, action), à la manière de ContentUnavailableView. */
    public static VBox emptyState(String icon, String title, String text, Button action) {
        StackPane circle = new StackPane(Icons.of(icon, 26));
        circle.getStyleClass().add("csme-empty-icon");
        Label t = new Label(title);
        t.getStyleClass().add("csme-empty-title");
        Label m = new Label(text);
        m.getStyleClass().add("csme-empty-text");
        m.setWrapText(true);
        m.setMaxWidth(380);
        VBox box = new VBox(circle, t, m);
        box.getStyleClass().add("csme-empty-state");
        if (action != null) {
            VBox.setMargin(action, new Insets(6, 0, 0, 0));
            box.getChildren().add(action);
        }
        return box;
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
