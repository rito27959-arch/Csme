package com.csme.desktop.ui.roles;

import atlantafx.base.theme.Styles;
import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Page;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.shared.dto.UserDtos.RoleDto;
import com.csme.shared.dto.UserDtos.SaveRoleRequest;
import com.csme.shared.security.Action;
import com.csme.shared.security.DefaultRoles;
import com.csme.shared.security.ErpModule;
import com.csme.shared.security.Permission;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Rôles et permissions : liste des rôles à gauche, matrice module × action à droite.
 * Les cases des permissions que l'utilisateur ne possède pas sont désactivées (le serveur le refuserait).
 */
public final class RolesPage extends ViewModel implements Page {

    private final AppContext ctx;
    private final Overlay overlay;
    private final ObservableList<RoleDto> roles = FXCollections.observableArrayList();
    private final ListView<RoleDto> list = new ListView<>(roles);
    private final TextField name = new TextField();
    private final TextField description = new TextField();
    private final Map<ErpModule, Map<Action, CheckBox>> matrix = new EnumMap<>(ErpModule.class);
    private final Label notice = new Label();
    private final Button save = Ui.primary("Enregistrer", "check");
    private final Button delete = new Button("Supprimer");
    private final boolean canCreate;
    private final boolean canUpdate;
    private final boolean canDelete;
    private final HBox root;
    private RoleDto editing;
    private boolean creating;

    public RolesPage(AppContext ctx, Overlay overlay) {
        this.ctx = ctx;
        this.overlay = overlay;
        this.canCreate = ctx.session().can(ErpModule.ROLES, Action.CREATE);
        this.canUpdate = ctx.session().can(ErpModule.ROLES, Action.UPDATE);
        this.canDelete = ctx.session().can(ErpModule.ROLES, Action.DELETE);

        // Colonne de gauche : liste des rôles
        list.setCellFactory(l -> new RoleCell());
        list.getStyleClass().add(atlantafx.base.theme.Tweaks.EDGE_TO_EDGE);
        list.setStyle("-fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: -color-border-muted;");
        list.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                edit(n);
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);
        Label rolesTitle = new Label("Rôles");
        rolesTitle.getStyleClass().add(Styles.TITLE_4);
        VBox left = new VBox(10, rolesTitle, list);
        if (canCreate) {
            Button create = Ui.secondary("Nouveau rôle", "plus");
            create.setMaxWidth(Double.MAX_VALUE);
            create.setOnAction(e -> startCreate());
            left.getChildren().add(create);
        }
        left.setPrefWidth(270);
        left.setMinWidth(240);

        // Colonne de droite : éditeur
        Ui.Form form = new Ui.Form().field("name", "Nom du rôle", name).field("description", "Description", description);
        form.bindErrors(fieldErrorsProperty());
        notice.setWrapText(true);
        notice.getStyleClass().add(Styles.TEXT_MUTED);
        notice.managedProperty().bind(notice.visibleProperty());

        save.setOnAction(e -> save());
        save.disableProperty().bind(busyProperty());
        delete.getStyleClass().addAll(Styles.DANGER, Styles.FLAT);
        delete.setOnAction(e -> confirmDelete());
        HBox actions = new HBox(10, delete, Ui.spacer(), save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        ScrollPane matrixScroll = new ScrollPane(buildMatrix());
        matrixScroll.setFitToWidth(true);
        VBox.setVgrow(matrixScroll, Priority.ALWAYS);
        Label perms = new Label("Permissions");
        perms.getStyleClass().add(Styles.TITLE_4);
        VBox editor = Ui.card(form.node(), notice, perms, matrixScroll, actions);
        VBox.setVgrow(editor, Priority.ALWAYS);

        VBox right = new VBox(Ui.pageHeader("Rôles et permissions",
                "Chaque rôle regroupe des droits par module et par action", Ui.spinner(busyProperty())),
                Ui.errorBanner(errorProperty()), editor);
        right.setSpacing(18);
        HBox.setHgrow(right, Priority.ALWAYS);

        left.setStyle("-fx-padding: 24 0 28 28;");
        right.setStyle("-fx-padding: 24 28 28 0;");
        root = new HBox(18, left, right);
        setEditorDisabled(true);
    }

    @Override
    public Node view() {
        return root;
    }

    @Override
    public void onShow() {
        reload(null);
    }

    // ------------------------------------------------------------------ matrix

    private GridPane buildMatrix() {
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(4);
        grid.getStyleClass().add("csme-permission-matrix");
        int col = 1;
        for (Action action : Action.values()) {
            Label head = new Label(action.label());
            head.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_BOLD);
            GridPane.setHalignment(head, HPos.CENTER);
            grid.add(head, col++, 0);
        }
        int row = 1;
        for (ErpModule module : ErpModule.values()) {
            Label label = new Label(module.label());
            label.setMinWidth(170);
            grid.add(label, 0, row);
            Map<Action, CheckBox> cells = new EnumMap<>(Action.class);
            col = 1;
            for (Action action : Action.values()) {
                CheckBox box = new CheckBox();
                box.setAccessibleText(module.label() + " — " + action.label());
                GridPane.setHalignment(box, HPos.CENTER);
                cells.put(action, box);
                grid.add(box, col++, row);
            }
            matrix.put(module, cells);
            row++;
        }
        return grid;
    }

    private void fillMatrix(Set<String> granted, boolean readOnly) {
        for (var moduleEntry : matrix.entrySet()) {
            for (var cell : moduleEntry.getValue().entrySet()) {
                String authority = Permission.of(moduleEntry.getKey(), cell.getKey()).authority();
                CheckBox box = cell.getValue();
                boolean has = granted.contains(authority);
                box.setSelected(has);
                boolean actorHolds = ctx.session().user().can(authority);
                // On peut toujours retirer un droit ; on ne peut accorder que ceux qu'on détient.
                box.setDisable(readOnly || (!has && !actorHolds));
                box.setTooltip(!readOnly && !has && !actorHolds
                        ? new Tooltip("Vous ne possédez pas cette permission : vous ne pouvez pas l'accorder.")
                        : null);
            }
        }
    }

    private Set<String> selectedPermissions() {
        Set<String> result = new HashSet<>();
        matrix.forEach((module, cells) -> cells.forEach((action, box) -> {
            if (box.isSelected()) {
                result.add(Permission.of(module, action).authority());
            }
        }));
        return result;
    }

    // ------------------------------------------------------------------ editing

    private void edit(RoleDto role) {
        editing = role;
        creating = false;
        name.setText(role.name());
        description.setText(role.description() == null ? "" : role.description());
        boolean locked = role.systemRole() && DefaultRoles.GENERAL_MANAGER.equals(role.name());
        boolean readOnly = locked || !canUpdate;
        fillMatrix(role.permissions(), readOnly);
        name.setEditable(!readOnly);
        description.setEditable(!readOnly);
        save.setVisible(!readOnly);
        delete.setVisible(canDelete && !role.systemRole());
        delete.setDisable(role.userCount() > 0);
        delete.setTooltip(role.userCount() > 0 ? new Tooltip("Rôle attribué à " + role.userCount() + " utilisateur(s)") : null);
        if (locked) {
            showNotice("Rôle verrouillé : il garantit qu'un accès complet à l'entreprise existe toujours.");
        } else if (role.systemRole()) {
            showNotice("Rôle prédéfini : vous pouvez adapter ses permissions, mais pas le supprimer. "
                    + role.userCount() + " utilisateur(s).");
        } else {
            showNotice(role.userCount() + " utilisateur(s). Les changements s'appliquent à leur prochaine reconnexion "
                    + "ou dans 15 minutes au plus.");
        }
        setEditorDisabled(false);
    }

    private void startCreate() {
        list.getSelectionModel().clearSelection();
        editing = null;
        creating = true;
        name.setText("");
        description.setText("");
        name.setEditable(true);
        description.setEditable(true);
        fillMatrix(Set.of(), false);
        save.setVisible(true);
        delete.setVisible(false);
        showNotice("Nouveau rôle : cochez les permissions à accorder.");
        setEditorDisabled(false);
        name.requestFocus();
    }

    private void save() {
        SaveRoleRequest request = new SaveRoleRequest(name.getText(), description.getText(), selectedPermissions());
        if (creating) {
            execute(() -> ctx.api().createRole(request), saved -> {
                overlay.success("Rôle « " + saved.name() + " » créé.");
                reload(saved);
            });
        } else if (editing != null) {
            var id = editing.id();
            execute(() -> ctx.api().updateRole(id, request), saved -> {
                overlay.success("Rôle « " + saved.name() + " » enregistré.");
                reload(saved);
            });
        }
    }

    private void confirmDelete() {
        if (editing == null) {
            return;
        }
        RoleDto role = editing;
        overlay.confirm("Supprimer le rôle « " + role.name() + " » ?", "Cette action est définitive.", "Supprimer", true,
                () -> execute(() -> {
                    ctx.api().deleteRole(role.id());
                    return Boolean.TRUE;
                }, ok -> {
                    overlay.success("Rôle supprimé.");
                    reload(null);
                }, err -> overlay.error(err.getMessage())));
    }

    private void reload(RoleDto select) {
        execute(() -> ctx.api().roles(), loaded -> {
            roles.setAll(loaded);
            RoleDto target = select == null ? null
                    : loaded.stream().filter(r -> r.id().equals(select.id())).findFirst().orElse(null);
            if (target == null && !loaded.isEmpty() && !creating) {
                target = loaded.getFirst();
            }
            if (target != null) {
                list.getSelectionModel().select(target);
                edit(target);
            }
        });
    }

    private void showNotice(String text) {
        notice.setText(text);
        notice.setVisible(true);
    }

    private void setEditorDisabled(boolean disabled) {
        name.setDisable(disabled);
        description.setDisable(disabled);
    }

    /** Ligne de la liste : nom, nombre d'utilisateurs, pastille « Prédéfini ». */
    private static final class RoleCell extends ListCell<RoleDto> {
        @Override
        protected void updateItem(RoleDto role, boolean empty) {
            super.updateItem(role, empty);
            if (empty || role == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label title = new Label(role.name());
            title.getStyleClass().add(Styles.TEXT_BOLD);
            Label meta = Ui.caption((role.systemRole() ? "Prédéfini · " : "Personnalisé · ")
                    + role.userCount() + " utilisateur(s) · " + role.permissions().size() + " droits");
            VBox text = new VBox(1, title, meta);
            text.setMinWidth(0);
            setText(null);
            setGraphic(text);
        }
    }
}
