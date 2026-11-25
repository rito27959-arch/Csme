package com.csme.desktop.ui.common;

import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/** Fabrique de colonnes de tableau typées. */
public final class Tables {

    private Tables() {
    }

    public static <S> TableView<S> table(String emptyText) {
        TableView<S> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        Label empty = new Label(emptyText);
        empty.getStyleClass().add("csme-empty");
        table.setPlaceholder(empty);
        return table;
    }

    /** Tableau dont l'état vide est une vue explicative avec action (et non une simple phrase). */
    public static <S> TableView<S> table(Node emptyState) {
        TableView<S> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(emptyState);
        return table;
    }

    /** Double-clic sur une ligne : ouvre l'élément (modification). */
    public static <S> void onDoubleClick(TableView<S> table, java.util.function.Consumer<S> action) {
        table.setRowFactory(t -> {
            javafx.scene.control.TableRow<S> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    action.accept(row.getItem());
                }
            });
            return row;
        });
    }

    /** Colonne d'actions étroite (menu « … »). */
    public static <S> TableColumn<S, S> menuColumn(java.util.function.Function<S, Node> render) {
        TableColumn<S, S> col = node("", render);
        col.setMinWidth(60);
        col.setPrefWidth(60);
        col.setMaxWidth(60);
        return col;
    }

    public static <S> TableColumn<S, String> text(String title, Function<S, String> value) {
        TableColumn<S, String> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(value.apply(c.getValue())));
        return col;
    }

    public static <S> TableColumn<S, S> node(String title, Function<S, Node> render) {
        TableColumn<S, S> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(S item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : render.apply(item));
            }
        });
        col.setSortable(false);
        if (title.isEmpty()) {
            // Colonne d'actions : largeur fixe, elle ne prend pas l'espace des colonnes utiles.
            col.setMinWidth(64);
            col.setPrefWidth(110);
            col.setMaxWidth(150);
            col.setResizable(false);
        }
        return col;
    }
}
