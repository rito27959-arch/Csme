package com.csme.desktop.ui.users;

import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Formats;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Pager;
import com.csme.desktop.ui.common.Tables;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.shared.api.PageResponse;
import com.csme.shared.dto.AuditDtos.AuditLogDto;
import com.csme.shared.dto.UserDtos.UserDto;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

/** Historique de connexion d'un utilisateur (connexions, échecs, déconnexions, changements de mot de passe). */
final class LoginHistorySheet extends ViewModel {

    private static final int PAGE_SIZE = 15;

    private final AppContext ctx;
    private final Overlay overlay;
    private final UserDto user;
    private final ObservableList<AuditLogDto> items = FXCollections.observableArrayList();
    private final Pager pager = new Pager(this::load);

    LoginHistorySheet(AppContext ctx, Overlay overlay, UserDto user) {
        this.ctx = ctx;
        this.overlay = overlay;
        this.user = user;
    }

    void show() {
        TableView<AuditLogDto> table = Tables.table("Aucune connexion enregistrée.");
        table.getColumns().addAll(List.of(
                Tables.text("Date", a -> Formats.dateTime(a.occurredAt())),
                Tables.text("Événement", a -> Formats.action(a.action())),
                Tables.node("Résultat", a -> "SUCCESS".equals(a.outcome()) ? Ui.badge("Réussi", "success") : Ui.badge("Échec", "danger")),
                Tables.text("Adresse IP", a -> a.ipAddress() == null ? "—" : a.ipAddress())));
        table.setItems(items);
        table.setPrefHeight(380);
        Button close = new Button("Fermer");
        close.setDefaultButton(true);
        close.setOnAction(e -> overlay.close());
        overlay.sheet("Historique de connexion — " + user.fullName(),
                new VBox(8, Ui.errorBanner(errorProperty()), table, pager), close);
        load(0);
    }

    private void load(int page) {
        execute(() -> ctx.api().loginHistory(user.id(), Math.max(page, 0), PAGE_SIZE), (PageResponse<AuditLogDto> r) -> {
            items.setAll(r.items());
            pager.update(r);
        });
    }
}
