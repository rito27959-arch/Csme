package com.csme.desktop.ui.login;

import atlantafx.base.theme.Styles;
import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Écran de connexion : panneau de marque à gauche, formulaire à droite. */
public final class LoginView {

    private final HBox root;

    public LoginView(AppContext ctx, LoginViewModel vm, Runnable onLoggedIn, String notice) {
        // Panneau de marque
        StackPane logo = new StackPane(new Label("C"));
        logo.getStyleClass().add("csme-logo");
        logo.setStyle("-fx-background-color: rgba(255,255,255,0.22);");
        Label headline = new Label("Toute votre entreprise,\nau même endroit.");
        headline.getStyleClass().add("csme-login-headline");
        headline.setWrapText(true);
        Label tagline = new Label("Personnel, ventes, achats, stocks, caisse et comptabilité — reliés, sécurisés, synchronisés.");
        tagline.setWrapText(true);
        tagline.setOpacity(0.85);
        VBox brand = new VBox(logo, new javafx.scene.layout.Region(), headline, tagline);
        VBox.setVgrow(brand.getChildren().get(1), Priority.ALWAYS);
        brand.getStyleClass().add("csme-login-brand");
        brand.setPrefWidth(460);
        brand.setMinWidth(380);

        // Formulaire
        Label title = new Label("Connexion");
        title.getStyleClass().add(Styles.TITLE_1);
        Label subtitle = Ui.caption("Utilisez le compte fourni par votre administrateur.");

        TextField email = new TextField();
        email.setPromptText("prenom.nom@entreprise.com");
        email.textProperty().bindBidirectional(vm.emailProperty());
        PasswordField password = new PasswordField();
        password.setPromptText("Mot de passe");
        password.textProperty().bindBidirectional(vm.passwordProperty());

        Ui.Form form = new Ui.Form()
                .field("email", "Adresse électronique", email)
                .field("password", "Mot de passe", password);
        form.bindErrors(vm.fieldErrorsProperty());

        Button submit = new Button("Se connecter");
        submit.getStyleClass().addAll(Styles.ACCENT, Styles.LARGE);
        submit.setDefaultButton(true);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.disableProperty().bind(vm.canSubmit().not());
        submit.setOnAction(e -> vm.login(t -> onLoggedIn.run()));

        Label server = Ui.caption("Serveur : " + ctx.config().serverUrl() + "  ·  version " + ctx.config().version());

        VBox formBox = new VBox(title, subtitle, Ui.errorBanner(vm.errorProperty()), form.node(),
                new HBox(8, submit, Ui.spinner(vm.busyProperty())), server);
        HBox.setHgrow(submit, Priority.ALWAYS);
        ((HBox) formBox.getChildren().get(4)).setAlignment(Pos.CENTER_LEFT);
        formBox.getStyleClass().add("csme-login-form");
        formBox.setFillWidth(true);
        VBox.setMargin(server, new Insets(16, 0, 0, 0));
        if (notice != null) {
            vm.errorProperty().set(notice);
        }

        StackPane formHolder = new StackPane(formBox);
        formHolder.setAlignment(Pos.CENTER);
        HBox.setHgrow(formHolder, Priority.ALWAYS);
        root = new HBox(brand, formHolder);
        root.getStyleClass().add("csme-window");
        javafx.application.Platform.runLater(email::requestFocus);
    }

    public Node view() {
        return root;
    }
}
