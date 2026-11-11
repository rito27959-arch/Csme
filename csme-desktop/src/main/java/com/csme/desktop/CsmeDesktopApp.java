package com.csme.desktop;

import com.csme.desktop.api.ApiClient;
import com.csme.desktop.api.CsmeApi;
import com.csme.desktop.config.AppConfig;
import com.csme.desktop.session.Session;
import com.csme.desktop.ui.common.Async;
import com.csme.desktop.ui.common.ThemeService;
import com.csme.desktop.ui.login.LoginView;
import com.csme.desktop.ui.login.LoginViewModel;
import com.csme.desktop.ui.shell.MainShell;
import java.util.Objects;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Application JavaFX : bascule entre l'écran de connexion et la fenêtre principale. */
public class CsmeDesktopApp extends Application {

    private final StackPane root = new StackPane();
    private AppContext ctx;
    private MainShell shell;

    @Override
    public void start(Stage stage) {
        ThemeService theme = new ThemeService();
        AppConfig config;
        try {
            config = AppConfig.load();
        } catch (RuntimeException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            return;
        }
        Session session = new Session();
        ctx = new AppContext(config, session, new CsmeApi(new ApiClient(config.serverUrl(), session, config.version())), theme);
        session.setOnExpired(this::showLogin);

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(
                CsmeDesktopApp.class.getResource("/com/csme/desktop/csme.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("CSME ERP");
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        showLogin(null);
        stage.show();
    }

    private void showLogin(String notice) {
        if (shell != null) {
            shell.dispose();
            shell = null;
        }
        LoginView login = new LoginView(ctx, new LoginViewModel(ctx), this::showShell, notice);
        root.getChildren().setAll(login.view());
    }

    private void showShell() {
        shell = new MainShell(ctx, () -> showLogin(null));
        root.getChildren().setAll(shell.view());
    }

    @Override
    public void stop() {
        if (shell != null) {
            shell.dispose();
        }
        Async.shutdown();
    }
}
