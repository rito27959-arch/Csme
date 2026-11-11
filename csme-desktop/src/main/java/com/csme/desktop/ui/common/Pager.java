package com.csme.desktop.ui.common;

import com.csme.shared.api.PageResponse;
import java.util.function.IntConsumer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** Navigation entre pages de résultats. */
public final class Pager extends HBox {

    private final Label info = new Label();
    private final Button previous = Ui.iconButton("arrow-left", "Page précédente");
    private final Button next = Ui.iconButton("arrow-right", "Page suivante");
    private int page;

    public Pager(IntConsumer goTo) {
        super(6);
        setAlignment(Pos.CENTER_RIGHT);
        info.getStyleClass().add("text-muted");
        previous.setOnAction(e -> goTo.accept(page - 1));
        next.setOnAction(e -> goTo.accept(page + 1));
        getChildren().addAll(info, previous, next);
        update(null);
    }

    public void update(PageResponse<?> response) {
        if (response == null || response.totalItems() == 0) {
            page = 0;
            info.setText("Aucun résultat");
            previous.setDisable(true);
            next.setDisable(true);
            return;
        }
        page = response.page();
        long from = (long) response.page() * response.size() + 1;
        long to = from + response.items().size() - 1;
        info.setText(from + "–" + to + " sur " + Formats.number(response.totalItems()));
        previous.setDisable(page == 0);
        next.setDisable(!response.hasNext());
    }
}
