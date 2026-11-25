package com.csme.desktop.ui.shell;

import com.csme.desktop.ui.common.Icons;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Ui;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Palette de commandes (Ctrl+K), à la manière de Spotlight : on tape quelques lettres pour aller à une page
 * ou lancer une action. Flèches pour choisir, Entrée pour valider, Échap pour fermer.
 */
final class CommandPalette {

    /** Une entrée de la palette. {@code tint} et {@code icon} servent à dessiner la tuile. */
    record Command(String title, String section, String icon, String tint, String shortcut, Runnable run) {
    }

    private final Overlay overlay;

    CommandPalette(Overlay overlay) {
        this.overlay = overlay;
    }

    void open(List<Command> commands) {
        TextField input = new TextField();
        input.setPromptText("Rechercher une page ou une action…");
        input.getStyleClass().add("csme-palette-input");
        HBox header = new HBox(Icons.of("search", 18), input);
        header.getStyleClass().add("csme-palette-header");
        HBox.setHgrow(input, javafx.scene.layout.Priority.ALWAYS);

        FilteredList<Command> filtered = new FilteredList<>(FXCollections.observableArrayList(commands));
        ListView<Command> list = new ListView<>(filtered);
        list.getStyleClass().add("csme-palette-list");
        list.setCellFactory(l -> new CommandCell());
        list.setFixedCellSize(44);
        list.setPrefHeight(44 * 8 + 14);
        list.setFocusTraversable(false);
        list.getSelectionModel().selectFirst();

        input.textProperty().addListener((obs, o, n) -> {
            String q = normalize(n);
            filtered.setPredicate(c -> q.isEmpty() || normalize(c.title()).contains(q) || normalize(c.section()).contains(q));
            list.getSelectionModel().selectFirst();
            list.scrollTo(0);
        });
        input.setOnKeyPressed(e -> {
            int i = list.getSelectionModel().getSelectedIndex();
            if (e.getCode() == KeyCode.DOWN) {
                list.getSelectionModel().select(Math.min(i + 1, filtered.size() - 1));
                list.scrollTo(Math.max(list.getSelectionModel().getSelectedIndex() - 3, 0));
                e.consume();
            } else if (e.getCode() == KeyCode.UP) {
                list.getSelectionModel().select(Math.max(i - 1, 0));
                list.scrollTo(Math.max(list.getSelectionModel().getSelectedIndex() - 3, 0));
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                run(list.getSelectionModel().getSelectedItem());
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                overlay.close();
                e.consume();
            }
        });
        list.setOnMouseClicked(e -> run(list.getSelectionModel().getSelectedItem()));

        Label hint = Ui.caption("↑ ↓ pour choisir · Entrée pour ouvrir · Échap pour fermer");
        HBox footer = new HBox(hint);
        footer.setStyle("-fx-padding: 8 18 12 18;");

        VBox palette = new VBox(header, list, footer);
        palette.getStyleClass().add("csme-palette");
        palette.setMaxWidth(600);
        palette.setPrefWidth(600);
        palette.setMaxHeight(VBox.USE_PREF_SIZE);
        overlay.present(palette);
        Platform.runLater(input::requestFocus);
    }

    private void run(Command command) {
        if (command == null) {
            return;
        }
        overlay.close();
        Platform.runLater(command.run());
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).strip();
    }

    private static final class CommandCell extends ListCell<Command> {
        @Override
        protected void updateItem(Command c, boolean empty) {
            super.updateItem(c, empty);
            if (empty || c == null) {
                setGraphic(null);
                return;
            }
            StackPane tile = Ui.tile(c.icon(), c.tint(), 26);
            Label title = new Label(c.title());
            title.getStyleClass().add("csme-medium");
            Label section = new Label(c.section());
            section.getStyleClass().add("csme-palette-section");
            HBox row = new HBox(12, tile, title, Ui.spacer(), section);
            if (c.shortcut() != null) {
                Label kbd = new Label(c.shortcut());
                kbd.getStyleClass().add("csme-kbd");
                row.getChildren().add(kbd);
            }
            row.setAlignment(Pos.CENTER_LEFT);
            setText(null);
            setGraphic(row);
        }
    }

}
