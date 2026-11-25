package com.csme.desktop.ui.companies;

import java.util.Optional;
import atlantafx.base.theme.Styles;
import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Formats;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Page;
import com.csme.desktop.ui.common.Tables;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.shared.dto.CompanyDtos.CompanyDto;
import com.csme.shared.dto.CompanyDtos.CreateCompanyRequest;
import java.util.List;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Entreprises hébergées (super administrateur) : liste, création, activation. */
public final class CompaniesPage extends ViewModel implements Page {

    private final AppContext ctx;
    private final Overlay overlay;
    private final ObservableList<CompanyDto> items = FXCollections.observableArrayList();
    private final TextField search;
    private final VBox root;

    public CompaniesPage(AppContext ctx, Overlay overlay) {
        this.ctx = ctx;
        this.overlay = overlay;

        search = Ui.searchField("Rechercher une entreprise", 260);
        FilteredList<CompanyDto> filtered = new FilteredList<>(items);
        search.textProperty().addListener((obs, o, n) -> {
            String q = n == null ? "" : n.strip().toLowerCase(Locale.ROOT);
            filtered.setPredicate(c -> q.isEmpty() || c.name().toLowerCase(Locale.ROOT).contains(q)
                    || c.code().toLowerCase(Locale.ROOT).contains(q));
        });

        Button create = Ui.primary("Nouvelle entreprise", "plus");
        create.setOnAction(e -> openCreate());

        Button emptyCreate = Ui.primary("Nouvelle entreprise", "plus");
        emptyCreate.setOnAction(e -> openCreate());
        TableView<CompanyDto> table = Tables.table(Ui.emptyState("building", "Aucune entreprise",
                "Chaque entreprise cliente dispose de ses propres données, strictement isolées des autres.", emptyCreate));
        table.getColumns().addAll(List.of(
                Tables.text("Code", CompanyDto::code),
                Tables.text("Nom", CompanyDto::name),
                Tables.text("Devise", CompanyDto::currency),
                Tables.text("Pays", CompanyDto::country),
                Tables.text("Utilisateurs", c -> Formats.number(c.userCount())),
                Tables.node("Statut", c -> c.active() ? Ui.badge("Active", "success") : Ui.badge("Désactivée", "neutral")),
                Tables.text("Créée le", c -> Formats.date(c.createdAt())),
                Tables.menuColumn(this::rowActions)));
        table.setItems(filtered);
        VBox.setVgrow(table, Priority.ALWAYS);

        root = new VBox(Ui.pageHeader("Entreprises", "Chaque entreprise dispose de ses propres données, isolées des autres",
                Ui.spinner(busyProperty()), search, create), Ui.errorBanner(errorProperty()), table);
        root.getStyleClass().add("csme-page");
    }

    @Override
    public Node view() {
        return root;
    }

    @Override
    public void onShow() {
        execute(() -> ctx.api().companies(), items::setAll);
    }

    @Override
    public Optional<PrimaryAction> primaryAction() {
        return Optional.of(new PrimaryAction("Nouvelle entreprise", "plus", this::openCreate));
    }

    @Override
    public void focusSearch() {
        search.requestFocus();
    }

    private Node rowActions(CompanyDto c) {
        return Ui.rowMenu(Ui.menuItem(c.active() ? "Désactiver l'entreprise…" : "Activer l'entreprise…",
                c.active() ? "power" : "check", () -> overlay.confirm(
                        c.active() ? "Désactiver " + c.name() + " ?" : "Activer " + c.name() + " ?",
                        c.active() ? "Plus aucun utilisateur de cette entreprise ne pourra se connecter. Les données sont conservées."
                                : "Les utilisateurs actifs de cette entreprise pourront de nouveau se connecter.",
                        c.active() ? "Désactiver" : "Activer", c.active(),
                        () -> execute(() -> ctx.api().setCompanyActive(c.id(), !c.active()), updated -> {
                            items.replaceAll(x -> x.id().equals(updated.id()) ? updated : x);
                            overlay.success("Entreprise " + (updated.active() ? "activée" : "désactivée") + ".");
                        }, err -> overlay.error(err.getMessage())))));
    }

    private void openCreate() {
        CreateCompanyForm form = new CreateCompanyForm();
        Button cancel = new Button("Annuler");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> overlay.close());
        Button save = Ui.primary("Créer l'entreprise", null);
        save.setDefaultButton(true);
        save.disableProperty().bind(form.vm.busyProperty());
        save.setOnAction(e -> form.vm.submit(ctx, created -> {
            overlay.close();
            items.add(created);
            overlay.success("Entreprise « " + created.name() + " » créée avec ses rôles et son directeur général.");
        }));
        overlay.sheet("Nouvelle entreprise", form.node(), cancel, save);
    }

    /** Formulaire de création, avec son propre ViewModel pour les erreurs par champ. */
    private static final class CreateCompanyForm {
        final FormVm vm = new FormVm();
        final TextField code = new TextField();
        final TextField name = new TextField();
        final TextField legalName = new TextField();
        final TextField currency = new TextField();
        final TextField country = new TextField();
        final TextField adminName = new TextField();
        final TextField adminEmail = new TextField();
        final PasswordField adminPassword = new PasswordField();
        private final VBox node;

        CreateCompanyForm() {
            code.setPromptText("ex. ACME");
            currency.setPromptText("ex. XAF");
            country.setPromptText("ex. CM");
            Ui.Form company = new Ui.Form()
                    .field("code", "Code", code)
                    .field("name", "Nom commercial", name)
                    .field("legalName", "Raison sociale (facultatif)", legalName);
            HBox currencyCountry = new HBox(12);
            Ui.Form cur = new Ui.Form().field("currency", "Devise (ISO 4217)", currency);
            Ui.Form cty = new Ui.Form().field("country", "Pays (ISO 3166)", country);
            currencyCountry.getChildren().addAll(cur.node(), cty.node());
            HBox.setHgrow(cur.node(), Priority.ALWAYS);
            HBox.setHgrow(cty.node(), Priority.ALWAYS);
            Ui.Form admin = new Ui.Form()
                    .field("adminFullName", "Nom du directeur général", adminName)
                    .field("adminEmail", "Adresse électronique", adminEmail)
                    .field("adminPassword", "Mot de passe initial (10 caractères min., lettres et chiffres)", adminPassword);
            company.bindErrors(vm.fieldErrorsProperty());
            cur.bindErrors(vm.fieldErrorsProperty());
            cty.bindErrors(vm.fieldErrorsProperty());
            admin.bindErrors(vm.fieldErrorsProperty());
            node = new VBox(6, Ui.errorBanner(vm.errorProperty()), company.node(), currencyCountry,
                    Ui.caption("Premier compte de l'entreprise"), admin.node());
        }

        Node node() {
            return node;
        }

        final class FormVm extends ViewModel {
            void submit(AppContext ctx, java.util.function.Consumer<CompanyDto> onCreated) {
                CreateCompanyRequest request = new CreateCompanyRequest(code.getText(), name.getText(), legalName.getText(),
                        currency.getText(), country.getText(), adminName.getText(), adminEmail.getText(), adminPassword.getText());
                execute(() -> ctx.api().createCompany(request), onCreated);
            }
        }
    }
}
