package com.csme.desktop.ui.settings;

import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Page;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.shared.dto.CompanyDtos.CompanyProfileDto;
import com.csme.shared.dto.CompanyDtos.UpdateCompanyProfileRequest;
import com.csme.shared.security.Action;
import com.csme.shared.security.ErpModule;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Informations générales de l'entreprise (en-têtes des documents, coordonnées, identifiants légaux). */
public final class SettingsPage extends ViewModel implements Page {

    private final AppContext ctx;
    private final Overlay overlay;
    private final boolean canUpdate;
    private final TextField name = new TextField();
    private final TextField legalName = new TextField();
    private final TextField address = new TextField();
    private final TextField city = new TextField();
    private final TextField phone = new TextField();
    private final TextField email = new TextField();
    private final TextField website = new TextField();
    private final TextField taxId = new TextField();
    private final TextField tradeRegister = new TextField();
    private final Label code = new Label("—");
    private final Label currency = new Label("—");
    private final Label country = new Label("—");
    private final ScrollPane root;

    public SettingsPage(AppContext ctx, Overlay overlay) {
        this.ctx = ctx;
        this.overlay = overlay;
        this.canUpdate = ctx.session().can(ErpModule.SETTINGS, Action.UPDATE);

        Ui.Form identity = new Ui.Form()
                .field("name", "Nom commercial", name)
                .field("legalName", "Raison sociale", legalName)
                .field("taxId", "Numéro d'identification fiscale", taxId)
                .field("tradeRegister", "Registre du commerce", tradeRegister);
        Ui.Form contact = new Ui.Form()
                .field("address", "Adresse", address)
                .field("city", "Ville", city)
                .field("phone", "Téléphone", phone)
                .field("email", "Adresse électronique", email)
                .field("website", "Site web", website);
        identity.bindErrors(fieldErrorsProperty());
        contact.bindErrors(fieldErrorsProperty());
        for (TextField f : new TextField[] {name, legalName, address, city, phone, email, website, taxId, tradeRegister}) {
            f.setEditable(canUpdate);
        }

        GridPane fixed = new GridPane();
        fixed.setHgap(24);
        fixed.setVgap(8);
        fixed.addRow(0, Ui.caption("Code"), Ui.caption("Devise"), Ui.caption("Pays"));
        fixed.addRow(1, code, currency, country);

        VBox identityCard = Ui.card(section("Identité"), identity.node());
        VBox contactCard = Ui.card(section("Coordonnées"), contact.node());
        VBox fixedCard = Ui.card(section("Fixé à la création"), fixed,
                Ui.caption("Le code, la devise et le pays fondent les données déjà enregistrées : ils ne sont pas modifiables."));
        HBox columns = new HBox(18, identityCard, contactCard);
        HBox.setHgrow(identityCard, Priority.ALWAYS);
        HBox.setHgrow(contactCard, Priority.ALWAYS);
        identityCard.setPrefWidth(0);
        contactCard.setPrefWidth(0);

        HBox header = Ui.pageHeader("Paramètres", "Informations générales de l'entreprise", Ui.spinner(busyProperty()));
        if (canUpdate) {
            Button save = Ui.primary("Enregistrer", "check");
            save.disableProperty().bind(busyProperty());
            save.setOnAction(e -> save());
            header.getChildren().add(save);
        }
        VBox page = new VBox(header, Ui.errorBanner(errorProperty()), columns, fixedCard);
        page.getStyleClass().add("csme-page");
        root = new ScrollPane(page);
        root.setFitToWidth(true);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    @Override
    public Node view() {
        return root;
    }

    @Override
    public void onShow() {
        execute(() -> ctx.api().companyProfile(), this::render);
    }

    private void save() {
        UpdateCompanyProfileRequest request = new UpdateCompanyProfileRequest(name.getText(), legalName.getText(),
                address.getText(), city.getText(), phone.getText(), email.getText(), website.getText(), taxId.getText(),
                tradeRegister.getText());
        execute(() -> ctx.api().updateCompanyProfile(request), saved -> {
            render(saved);
            overlay.success("Informations de l'entreprise enregistrées.");
        });
    }

    private void render(CompanyProfileDto p) {
        name.setText(nz(p.name()));
        legalName.setText(nz(p.legalName()));
        address.setText(nz(p.address()));
        city.setText(nz(p.city()));
        phone.setText(nz(p.phone()));
        email.setText(nz(p.email()));
        website.setText(nz(p.website()));
        taxId.setText(nz(p.taxId()));
        tradeRegister.setText(nz(p.tradeRegister()));
        code.setText(p.code());
        currency.setText(p.currency());
        country.setText(p.country());
    }

    private static Label section(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("csme-section-title");
        return l;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
