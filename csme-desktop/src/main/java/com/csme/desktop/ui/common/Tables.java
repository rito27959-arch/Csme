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
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Label empty = new Label(emptyText);
        empty.getStyleClass().add("csme-empty");
        table.setPlaceholder(empty);
        return table;
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
        return col;
    }
}
