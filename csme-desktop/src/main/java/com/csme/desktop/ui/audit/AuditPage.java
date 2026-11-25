package com.csme.desktop.ui.audit;

import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Formats;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Page;
import com.csme.desktop.ui.common.Pager;
import com.csme.desktop.ui.common.Tables;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.shared.api.PageResponse;
import com.csme.shared.dto.AuditDtos.AuditLogDto;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Journal d'audit en lecture seule (entreprise ou plateforme). */
public final class AuditPage extends ViewModel implements Page {

    private static final int PAGE_SIZE = 50;
    private static final String ALL = "";

    private final AppContext ctx;
    private final boolean platform;
    private final ObservableList<AuditLogDto> items = FXCollections.observableArrayList();
    private final ComboBox<String> actionFilter = new ComboBox<>();
    private final Pager pager;
    private final VBox root;
    private int page;

    public AuditPage(AppContext ctx, Overlay overlay, boolean platform) {
        this.ctx = ctx;
        this.platform = platform;
        this.pager = new Pager(this::load);

        TableView<AuditLogDto> table = Tables.table(Ui.emptyState("shield", "Aucune opération",
                "Chaque connexion, création ou modification sera tracée ici : qui, quoi, quand et depuis où.", null));
        table.getColumns().addAll(List.of(
                Tables.text("Date", a -> Formats.dateTime(a.occurredAt())),
                Tables.text("Utilisateur", a -> a.actorEmail() == null ? "Système" : a.actorEmail()),
                Tables.text("Opération", a -> Formats.action(a.action())),
                Tables.text("Élément", a -> a.entityType() == null ? "—" : a.entityType()),
                Tables.node("Résultat", a -> switch (a.outcome()) {
                    case "SUCCESS" -> Ui.badge("Réussi", "success");
                    case "DENIED" -> Ui.badge("Refusé", "warning");
                    default -> Ui.badge("Échec", "danger");
                }),
                Tables.text("Adresse IP", a -> a.ipAddress() == null ? "—" : a.ipAddress())));
        table.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(AuditLogDto item, boolean empty) {
                super.updateItem(item, empty);
                setTooltip(empty || item == null || item.details() == null ? null : new Tooltip(item.details()));
            }
        });
        table.setItems(items);
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox header;
        if (platform) {
            header = Ui.pageHeader("Journal de la plateforme", "Toutes les opérations, toutes entreprises confondues",
                    Ui.spinner(busyProperty()));
        } else {
            actionFilter.getItems().add(ALL);
            actionFilter.getItems().addAll(Formats.knownActions());
            actionFilter.setValue(ALL);
            actionFilter.setButtonCell(actionCell());
            actionFilter.setCellFactory(l -> actionCell());
            actionFilter.valueProperty().addListener((obs, o, n) -> load(0));
            header = Ui.pageHeader("Journal d'audit", "Qui a fait quoi, quand et depuis où — lecture seule",
                    Ui.spinner(busyProperty()), new Label("Filtrer :"), actionFilter);
        }
        root = new VBox(header, Ui.errorBanner(errorProperty()), table, pager);
        root.getStyleClass().add("csme-page");
    }

    @Override
    public Node view() {
        return root;
    }

    @Override
    public void onShow() {
        load(page);
    }

    private void load(int requested) {
        int p = Math.max(requested, 0);
        String action = actionFilter.getValue();
        execute(() -> platform ? ctx.api().platformAudit(p, PAGE_SIZE) : ctx.api().audit(action, p, PAGE_SIZE),
                (PageResponse<AuditLogDto> r) -> {
                    page = r.page();
                    items.setAll(r.items());
                    pager.update(r);
                });
    }

    private static ListCell<String> actionCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.isEmpty() ? "Toutes les opérations" : Formats.action(item));
            }
        };
    }
}
