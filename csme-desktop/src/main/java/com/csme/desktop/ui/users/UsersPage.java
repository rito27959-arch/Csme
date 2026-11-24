package com.csme.desktop.ui.users;

import atlantafx.base.theme.Styles;
import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Formats;
import com.csme.desktop.ui.common.Icons;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Page;
import com.csme.desktop.ui.common.Pager;
import com.csme.desktop.ui.common.Tables;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.shared.api.PageResponse;
import com.csme.shared.dto.AgencyDtos.AgencyDto;
import com.csme.shared.dto.UserDtos.RoleRef;
import com.csme.shared.dto.UserDtos.UserDto;
import com.csme.shared.security.Action;
import com.csme.shared.security.ErpModule;
import java.util.List;
import java.util.stream.Collectors;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Comptes utilisateurs de l'entreprise : recherche côté serveur, pagination, création et gestion. */
public final class UsersPage extends ViewModel implements Page {

    private static final int PAGE_SIZE = 25;

    private final AppContext ctx;
    private final Overlay overlay;
    private final ObservableList<UserDto> items = FXCollections.observableArrayList();
    private final TextField search = new TextField();
    private final Pager pager;
    private final VBox root;
    private final boolean canCreate;
    private final boolean canUpdate;
    private int page;

    public UsersPage(AppContext ctx, Overlay overlay) {
        this.ctx = ctx;
        this.overlay = overlay;
        this.canCreate = ctx.session().can(ErpModule.USERS, Action.CREATE);
        this.canUpdate = ctx.session().can(ErpModule.USERS, Action.UPDATE);
        this.pager = new Pager(this::load);

        search.setPromptText("Nom ou adresse électronique");
        search.setPrefWidth(280);
        PauseTransition debounce = new PauseTransition(Duration.millis(350));
        debounce.setOnFinished(e -> load(0));
        search.textProperty().addListener((obs, o, n) -> debounce.playFromStart());

        TableView<UserDto> table = Tables.table("Aucun utilisateur ne correspond à la recherche.");
        table.getColumns().addAll(List.of(
                Tables.node("Utilisateur", this::identity),
                Tables.text("Poste", u -> u.jobTitle() == null ? "—" : u.jobTitle()),
                Tables.text("Rôles", u -> u.roles().stream().map(RoleRef::name).collect(Collectors.joining(", "))),
                Tables.text("Département", u -> u.department() == null ? "—" : u.department().name()),
                Tables.node("Statut", this::status),
                Tables.text("Dernière connexion", u -> Formats.dateTime(u.lastLoginAt()))));
        if (canUpdate) {
            table.getColumns().add(Tables.node("", this::rowActions));
        }
        table.setItems(items);
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox header = Ui.pageHeader("Utilisateurs", "Comptes, rôles et accès aux agences", Ui.spinner(busyProperty()), search);
        if (canCreate) {
            Button create = Ui.primary("Nouvel utilisateur", "plus");
            create.setOnAction(e -> openEditor(null));
            header.getChildren().add(create);
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
        String q = search.getText();
        execute(() -> ctx.api().users(q, Math.max(requested, 0), PAGE_SIZE), (PageResponse<UserDto> r) -> {
            page = r.page();
            items.setAll(r.items());
            pager.update(r);
        });
    }

    private Node identity(UserDto u) {
        Label name = new Label(u.fullName());
        name.getStyleClass().add(Styles.TEXT_BOLD);
        return new VBox(1, name, Ui.caption(u.email()));
    }

    private Node status(UserDto u) {
        if (!u.active()) {
            return Ui.badge("Désactivé", "neutral");
        }
        if (u.locked()) {
            return Ui.badge("Verrouillé", "danger");
        }
        return Ui.badge("Actif", "success");
    }

    private Node rowActions(UserDto u) {
        MenuButton menu = new MenuButton(null, Icons.of("more", 16));
        menu.getStyleClass().addAll(Styles.FLAT, Styles.BUTTON_ICON, atlantafx.base.theme.Tweaks.NO_ARROW);
        menu.setAccessibleText("Actions");
        MenuItem edit = new MenuItem("Modifier…");
        edit.setOnAction(e -> openEditor(u));
        MenuItem reset = new MenuItem("Réinitialiser le mot de passe…");
        reset.setOnAction(e -> openReset(u));
        MenuItem history = new MenuItem("Historique de connexion…");
        history.setOnAction(e -> new LoginHistorySheet(ctx, overlay, u).show());
        menu.getItems().addAll(edit, history, reset);
        if (u.locked()) {
            MenuItem unlock = new MenuItem("Déverrouiller");
            unlock.setOnAction(e -> execute(() -> ctx.api().unlockUser(u.id()), updated -> {
                replace(updated);
                overlay.success("Compte déverrouillé.");
            }, err -> overlay.error(err.getMessage())));
            menu.getItems().add(unlock);
        }
        MenuItem toggle = new MenuItem(u.active() ? "Désactiver" : "Réactiver");
        toggle.setOnAction(e -> overlay.confirm(
                (u.active() ? "Désactiver " : "Réactiver ") + u.fullName() + " ?",
                u.active() ? "Ses sessions sont fermées immédiatement et il ne pourra plus se connecter. L'historique est conservé."
                        : "Il pourra de nouveau se connecter avec son mot de passe actuel.",
                u.active() ? "Désactiver" : "Réactiver", u.active(),
                () -> execute(() -> ctx.api().setUserActive(u.id(), !u.active()), updated -> {
                    replace(updated);
                    overlay.success("Compte " + (updated.active() ? "réactivé" : "désactivé") + ".");
                }, err -> overlay.error(err.getMessage()))));
        menu.getItems().add(toggle);
        return menu;
    }

    private void replace(UserDto updated) {
        items.replaceAll(x -> x.id().equals(updated.id()) ? updated : x);
    }

    private void openEditor(UserDto existing) {
        // Les rôles et agences sont chargés avant d'ouvrir la feuille.
        execute(() -> new UserEditor.Choices(ctx.api().roles(),
                        ctx.session().can(ErpModule.AGENCIES, Action.VIEW) ? ctx.api().agencies() : List.<AgencyDto>of(),
                        ctx.session().can(ErpModule.DEPARTMENTS, Action.VIEW) ? ctx.api().departments()
                                : List.<com.csme.shared.dto.DepartmentDtos.DepartmentDto>of()),
                choices -> new UserEditor(ctx, overlay, existing, choices, saved -> {
                    if (existing == null) {
                        load(0);
                    } else {
                        replace(saved);
                    }
                }).show(),
                err -> overlay.error(err.getMessage()));
    }

    private void openReset(UserDto u) {
        ResetVm vm = new ResetVm();
        PasswordField password = new PasswordField();
        Ui.Form form = new Ui.Form().field("newPassword", "Nouveau mot de passe", password);
        form.bindErrors(vm.fieldErrorsProperty());
        Button cancel = new Button("Annuler");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> overlay.close());
        Button save = Ui.primary("Réinitialiser", null);
        save.setDefaultButton(true);
        save.disableProperty().bind(vm.busyProperty());
        save.setOnAction(e -> vm.reset(ctx, u, password.getText(), () -> {
            overlay.close();
            overlay.success("Mot de passe réinitialisé. Communiquez-le à " + u.fullName() + " par un canal sûr.");
        }));
        overlay.sheet("Réinitialiser le mot de passe de " + u.fullName(), new VBox(6, Ui.errorBanner(vm.errorProperty()),
                Ui.caption("Toutes ses sessions seront fermées."), form.node()), cancel, save);
    }

    private static final class ResetVm extends ViewModel {
        void reset(AppContext ctx, UserDto u, String password, Runnable done) {
            execute(() -> {
                ctx.api().resetPassword(u.id(), password);
                return Boolean.TRUE;
            }, ok -> done.run());
        }
    }

}
