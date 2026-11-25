package com.csme.desktop.ui.login;

import atlantafx.base.controls.PasswordTextField;
import atlantafx.base.theme.Styles;
import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Icons;
import com.csme.desktop.ui.common.Ui;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Écran de connexion : panneau de marque à gauche, formulaire à droite.
 * L'adresse de la dernière connexion réussie est proposée ; le mot de passe peut être affiché.
 */
public final class LoginView {

    private static final String PREF_LAST_EMAIL = "login.lastEmail";

    private final HBox root;
    private final Preferences prefs = Preferences.userNodeForPackage(LoginView.class);

    public LoginView(AppContext ctx, LoginViewModel vm, Runnable onLoggedIn, String notice) {
        root = new HBox(brandPanel(), formPanel(ctx, vm, onLoggedIn, notice));
        root.getStyleClass().add("csme-window");
    }

    public Node view() {
        return root;
    }

    private static VBox brandPanel() {
        StackPane logo = new StackPane(new Label("C"));
        logo.getStyleClass().add("csme-logo");
        logo.setStyle("-fx-background-color: rgba(255,255,255,0.22); -fx-effect: none;");
        Label name = new Label("CSME ERP");
        name.getStyleClass().add("csme-semibold");
        name.setStyle("-fx-font-size: 15px;");
        HBox brand = new HBox(11, logo, name);
        brand.setAlignment(Pos.CENTER_LEFT);

        Label headline = new Label("Toute votre entreprise,\nau même endroit.");
        headline.getStyleClass().add("csme-login-headline");
        headline.setWrapText(true);
        VBox features = new VBox(12,
                feature("users", "Personnel, rôles et accès maîtrisés"),
                feature("layers", "Agences et départements synchronisés"),
                feature("shield", "Données isolées par entreprise, chaque action tracée"));
        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        VBox panel = new VBox(brand, grow, headline, features);
        VBox.setMargin(features, new Insets(22, 0, 0, 0));
        panel.getStyleClass().add("csme-login-brand");
        panel.setPrefWidth(470);
        panel.setMinWidth(380);
        return panel;
    }

    private static HBox feature(String icon, String text) {
        StackPane circle = new StackPane(Icons.of(icon, 15));
        circle.setStyle("-fx-background-color: rgba(255,255,255,0.18); -fx-background-radius: 50%;");
        circle.setMinSize(30, 30);
        circle.setMaxSize(30, 30);
        HBox row = new HBox(circle, new Label(text));
        row.getStyleClass().add("csme-login-feature");
        return row;
    }

    private Node formPanel(AppContext ctx, LoginViewModel vm, Runnable onLoggedIn, String notice) {
        Label title = new Label("Connexion");
        title.getStyleClass().add("csme-login-title");
        Label subtitle = Ui.caption("Utilisez le compte fourni par votre administrateur.");

        TextField email = new TextField();
        email.setPromptText("prenom.nom@entreprise.com");
        email.textProperty().bindBidirectional(vm.emailProperty());
        String last = prefs.get(PREF_LAST_EMAIL, "");
        if (vm.emailProperty().get().isBlank() && !last.isBlank()) {
            vm.emailProperty().set(last);
        }

        // Mot de passe avec bouton « œil » pour l'afficher ou le masquer.
        PasswordTextField password = new PasswordTextField();
        password.setPromptText("Mot de passe");
        password.passwordProperty().addListener((obs, o, n) -> vm.passwordProperty().set(n));
        vm.passwordProperty().addListener((obs, o, n) -> {
            if (n.isEmpty() && !password.getPassword().isEmpty()) {
                password.clear();
            }
        });
        StackPane eye = eye("eye", "Afficher le mot de passe");
        eye.setOnMouseClicked(e -> {
            boolean reveal = !password.getRevealPassword();
            password.setRevealPassword(reveal);
            eye.getChildren().setAll(Icons.of(reveal ? "eye-off" : "eye", 16));
            javafx.scene.control.Tooltip.install(eye, new javafx.scene.control.Tooltip(
                    reveal ? "Masquer le mot de passe" : "Afficher le mot de passe"));
        });
        password.setRight(eye);

        Ui.Form form = new Ui.Form()
                .field("email", "Adresse électronique", email)
                .field("password", "Mot de passe", password);
        form.bindErrors(vm.fieldErrorsProperty());

        Button submit = new Button("Se connecter");
        submit.getStyleClass().addAll(Styles.ACCENT, Styles.LARGE);
        submit.setDefaultButton(true);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.disableProperty().bind(vm.canSubmit().not());
        submit.setOnAction(e -> vm.login(t -> {
            prefs.put(PREF_LAST_EMAIL, t.user().email());
            onLoggedIn.run();
        }));
        HBox submitRow = new HBox(8, submit, Ui.spinner(vm.busyProperty()));
        submitRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(submit, Priority.ALWAYS);

        Label server = Ui.caption("Serveur : " + ctx.config().serverUrl() + "  ·  version " + ctx.config().version());

        VBox formBox = new VBox(title, subtitle, Ui.errorBanner(vm.errorProperty()), form.node(), submitRow, server);
        formBox.getStyleClass().add("csme-login-form");
        formBox.setFillWidth(true);
        VBox.setMargin(server, new Insets(18, 0, 0, 0));
        VBox.setMargin(title, new Insets(0, 0, -6, 0));
        if (notice != null) {
            vm.errorProperty().set(notice);
        }

        StackPane holder = new StackPane(formBox);
        holder.setAlignment(Pos.CENTER);
        HBox.setHgrow(holder, Priority.ALWAYS);
        Platform.runLater(() -> {
            if (email.getText().isBlank()) {
                email.requestFocus();
            } else {
                password.requestFocus();
            }
        });
        return holder;
    }

    private static StackPane eye(String icon, String tooltip) {
        StackPane p = new StackPane(Icons.of(icon, 16));
        p.setCursor(Cursor.HAND);
        p.setMaxSize(StackPane.USE_PREF_SIZE, StackPane.USE_PREF_SIZE);
        javafx.scene.control.Tooltip.install(p, new javafx.scene.control.Tooltip(tooltip));
        p.setOpacity(0.6);
        return p;
    }
}
