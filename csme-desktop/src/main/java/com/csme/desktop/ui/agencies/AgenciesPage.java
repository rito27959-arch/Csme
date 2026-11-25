package com.csme.desktop.ui.agencies;

import java.util.Optional;
import atlantafx.base.theme.Styles;
import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Formats;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Page;
import com.csme.desktop.ui.common.Tables;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.shared.dto.AgencyDtos.AgencyDto;
import com.csme.shared.dto.AgencyDtos.SaveAgencyRequest;
import com.csme.shared.security.Action;
import com.csme.shared.security.ErpModule;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Agences de l'entreprise. Les actions affichées dépendent des permissions (vérifiées aussi par le serveur). */
public final class AgenciesPage extends ViewModel implements Page {

    private final AppContext ctx;
    private final Overlay overlay;
    private final ObservableList<AgencyDto> items = FXCollections.observableArrayList();
    private final VBox root;
    private final boolean canCreate;
    private final boolean canUpdate;

    public AgenciesPage(AppContext ctx, Overlay overlay) {
        this.ctx = ctx;
        this.overlay = overlay;
        this.canCreate = ctx.session().can(ErpModule.AGENCIES, Action.CREATE);
        this.canUpdate = ctx.session().can(ErpModule.AGENCIES, Action.UPDATE);

        TableView<AgencyDto> table = Tables.table(Ui.emptyState("pin", "Aucune agence",
                "Les agences sont vos points de vente, bureaux ou entrepôts. Les départements et les accès des "
                        + "utilisateurs s'y rattachent.", canCreate ? createButton() : null));
        table.getColumns().addAll(List.of(
                Tables.text("Code", AgencyDto::code),
                Tables.text("Nom", AgencyDto::name),
                Tables.text("Ville", a -> a.city() == null ? "—" : a.city()),
                Tables.text("Téléphone", a -> a.phone() == null ? "—" : a.phone()),
                Tables.node("Statut", a -> a.active() ? Ui.badge("Active", "success") : Ui.badge("Fermée", "neutral")),
                Tables.text("Créée le", a -> Formats.date(a.createdAt()))));
        if (canUpdate) {
            table.getColumns().add(Tables.menuColumn(this::rowActions));
            Tables.onDoubleClick(table, this::openEditor);
        }
        table.setItems(items);
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox header = canCreate
                ? Ui.pageHeader("Agences", "Implantations de l'entreprise", Ui.spinner(busyProperty()), createButton())
                : Ui.pageHeader("Agences", "Implantations de l'entreprise", Ui.spinner(busyProperty()));
        root = new VBox(header, Ui.errorBanner(errorProperty()), table);
        root.getStyleClass().add("csme-page");
    }

    @Override
    public Node view() {
        return root;
    }

    @Override
    public void onShow() {
        execute(() -> ctx.api().agencies(), items::setAll);
    }

    @Override
    public Optional<PrimaryAction> primaryAction() {
        return canCreate ? Optional.of(new PrimaryAction("Nouvelle agence", "plus", () -> openEditor(null))) : Optional.empty();
    }

    private Button createButton() {
        Button b = Ui.primary("Nouvelle agence", "plus");
        b.setOnAction(e -> openEditor(null));
        return b;
    }

    private Node rowActions(AgencyDto a) {
        return Ui.rowMenu(
                Ui.menuItem("Modifier…", "edit", () -> openEditor(a)),
                new javafx.scene.control.SeparatorMenuItem(),
                Ui.menuItem(a.active() ? "Fermer l'agence" : "Rouvrir l'agence", a.active() ? "power" : "check",
                        () -> execute(() -> ctx.api().setAgencyActive(a.id(), !a.active()), updated -> {
                            replace(updated);
                            overlay.success("Agence " + (updated.active() ? "rouverte" : "fermée") + ".");
                        }, err -> overlay.error(err.getMessage()))));
    }

    private void replace(AgencyDto updated) {
        items.replaceAll(x -> x.id().equals(updated.id()) ? updated : x);
    }

    private void openEditor(AgencyDto existing) {
        EditorVm vm = new EditorVm();
        TextField code = new TextField(existing == null ? "" : existing.code());
        TextField name = new TextField(existing == null ? "" : existing.name());
        TextField city = new TextField(existing == null || existing.city() == null ? "" : existing.city());
        TextField phone = new TextField(existing == null || existing.phone() == null ? "" : existing.phone());
        Ui.Form form = new Ui.Form()
                .field("code", "Code", code)
                .field("name", "Nom", name)
                .field("city", "Ville", city)
                .field("phone", "Téléphone", phone);
        form.bindErrors(vm.fieldErrorsProperty());

        Button cancel = new Button("Annuler");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> overlay.close());
        Button save = Ui.primary(existing == null ? "Créer" : "Enregistrer", null);
        save.setDefaultButton(true);
        save.disableProperty().bind(vm.busyProperty());
        save.setOnAction(e -> {
            SaveAgencyRequest request = new SaveAgencyRequest(code.getText(), name.getText(), city.getText(), phone.getText());
            vm.save(() -> existing == null ? ctx.api().createAgency(request) : ctx.api().updateAgency(existing.id(), request),
                    saved -> {
                        overlay.close();
                        if (existing == null) {
                            items.add(saved);
                        } else {
                            replace(saved);
                        }
                        overlay.success("Agence enregistrée.");
                    });
        });
        overlay.sheet(existing == null ? "Nouvelle agence" : "Modifier l'agence",
                new VBox(6, Ui.errorBanner(vm.errorProperty()), form.node()), cancel, save);
    }

    private static final class EditorVm extends ViewModel {
        void save(java.util.concurrent.Callable<AgencyDto> work, java.util.function.Consumer<AgencyDto> done) {
            execute(work, done);
        }
    }
}
