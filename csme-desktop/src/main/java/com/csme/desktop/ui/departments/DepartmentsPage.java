package com.csme.desktop.ui.departments;

import atlantafx.base.theme.Styles;
import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Formats;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Page;
import com.csme.desktop.ui.common.Tables;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.shared.dto.AgencyDtos.AgencyDto;
import com.csme.shared.dto.DepartmentDtos.DepartmentDto;
import com.csme.shared.dto.DepartmentDtos.SaveDepartmentRequest;
import com.csme.shared.security.Action;
import com.csme.shared.security.ErpModule;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Départements de l'entreprise, chacun rattaché à une agence. */
public final class DepartmentsPage extends ViewModel implements Page {

    private final AppContext ctx;
    private final Overlay overlay;
    private final ObservableList<DepartmentDto> items = FXCollections.observableArrayList();
    private final VBox root;
    private final boolean canUpdate;

    public DepartmentsPage(AppContext ctx, Overlay overlay) {
        this.ctx = ctx;
        this.overlay = overlay;
        boolean canCreate = ctx.session().can(ErpModule.DEPARTMENTS, Action.CREATE);
        this.canUpdate = ctx.session().can(ErpModule.DEPARTMENTS, Action.UPDATE);

        TableView<DepartmentDto> table = Tables.table("Aucun département. Créez d'abord une agence, puis ses départements.");
        table.getColumns().addAll(List.of(
                Tables.text("Code", DepartmentDto::code),
                Tables.text("Nom", DepartmentDto::name),
                Tables.text("Agence", d -> d.agency().name()),
                Tables.text("Utilisateurs", d -> Formats.number(d.userCount())),
                Tables.node("Statut", d -> d.active() ? Ui.badge("Actif", "success") : Ui.badge("Inactif", "neutral")),
                Tables.text("Description", d -> d.description() == null ? "—" : d.description())));
        if (canUpdate) {
            table.getColumns().add(Tables.node("", this::rowActions));
        }
        table.setItems(items);
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox header = Ui.pageHeader("Départements", "Organisation interne de chaque agence", Ui.spinner(busyProperty()));
        if (canCreate) {
            Button create = Ui.primary("Nouveau département", "plus");
            create.setOnAction(e -> openEditor(null));
            header.getChildren().add(create);
        }
        root = new VBox(header, Ui.errorBanner(errorProperty()), table);
        root.getStyleClass().add("csme-page");
    }

    @Override
    public Node view() {
        return root;
    }

    @Override
    public void onShow() {
        execute(() -> ctx.api().departments(), items::setAll);
    }

    private Node rowActions(DepartmentDto d) {
        Button edit = Ui.iconButton("edit", "Modifier");
        edit.setOnAction(e -> openEditor(d));
        Button toggle = new Button(d.active() ? "Désactiver" : "Réactiver");
        toggle.getStyleClass().addAll(Styles.SMALL, Styles.FLAT, d.active() ? Styles.DANGER : Styles.SUCCESS);
        toggle.setOnAction(e -> execute(() -> ctx.api().setDepartmentActive(d.id(), !d.active()), updated -> {
            replace(updated);
            overlay.success("Département " + (updated.active() ? "réactivé" : "désactivé") + ".");
        }, err -> overlay.error(err.getMessage())));
        HBox box = new HBox(4, edit, toggle);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return box;
    }

    private void replace(DepartmentDto updated) {
        items.replaceAll(x -> x.id().equals(updated.id()) ? updated : x);
    }

    private void openEditor(DepartmentDto existing) {
        execute(() -> ctx.api().agencies(), agencies -> showEditor(existing, agencies), err -> overlay.error(err.getMessage()));
    }

    private void showEditor(DepartmentDto existing, List<AgencyDto> agencies) {
        EditorVm vm = new EditorVm();
        TextField code = new TextField(existing == null ? "" : existing.code());
        TextField name = new TextField(existing == null ? "" : existing.name());
        TextField description = new TextField(existing == null || existing.description() == null ? "" : existing.description());
        ComboBox<AgencyDto> agency = new ComboBox<>(FXCollections.observableArrayList(
                agencies.stream().filter(a -> a.active() || (existing != null && a.id().equals(existing.agency().id()))).toList()));
        agency.setMaxWidth(Double.MAX_VALUE);
        agency.setPromptText("Choisir une agence");
        agency.setCellFactory(l -> agencyCell());
        agency.setButtonCell(agencyCell());
        if (existing != null) {
            agency.getItems().stream().filter(a -> a.id().equals(existing.agency().id())).findFirst().ifPresent(agency::setValue);
        } else if (agency.getItems().size() == 1) {
            agency.setValue(agency.getItems().getFirst());
        }
        Ui.Form form = new Ui.Form()
                .field("agencyId", "Agence", agency)
                .field("code", "Code", code)
                .field("name", "Nom", name)
                .field("description", "Description (facultatif)", description);
        form.bindErrors(vm.fieldErrorsProperty());

        Button cancel = new Button("Annuler");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> overlay.close());
        Button save = Ui.primary(existing == null ? "Créer" : "Enregistrer", null);
        save.setDefaultButton(true);
        save.disableProperty().bind(vm.busyProperty());
        save.setOnAction(e -> {
            SaveDepartmentRequest request = new SaveDepartmentRequest(code.getText(), name.getText(), description.getText(),
                    agency.getValue() == null ? null : agency.getValue().id());
            vm.save(() -> existing == null ? ctx.api().createDepartment(request)
                    : ctx.api().updateDepartment(existing.id(), request), saved -> {
                overlay.close();
                if (existing == null) {
                    items.add(saved);
                } else {
                    replace(saved);
                }
                overlay.success("Département enregistré.");
            });
        });
        VBox content = new VBox(6, Ui.errorBanner(vm.errorProperty()), form.node());
        if (agencies.isEmpty()) {
            content.getChildren().addFirst(Ui.caption("Aucune agence : créez d'abord une agence dans « Agences »."));
        }
        overlay.sheet(existing == null ? "Nouveau département" : "Modifier le département", content, cancel, save);
    }

    private static ListCell<AgencyDto> agencyCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(AgencyDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name() + " (" + item.code() + ")");
            }
        };
    }

    private static final class EditorVm extends ViewModel {
        void save(Callable<DepartmentDto> work, Consumer<DepartmentDto> done) {
            execute(work, done);
        }
    }
}
