package com.csme.desktop.ui.account;

import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import java.util.Map;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

/** Changement de mot de passe. Le serveur ferme alors toutes les sessions : l'utilisateur se reconnecte. */
public final class ChangePasswordSheet extends ViewModel {

    private final AppContext ctx;
    private final Overlay overlay;
    private final Runnable onDone;

    public ChangePasswordSheet(AppContext ctx, Overlay overlay, Runnable onDone) {
        this.ctx = ctx;
        this.overlay = overlay;
        this.onDone = onDone;
    }

    public void show() {
        PasswordField current = new PasswordField();
        PasswordField next = new PasswordField();
        PasswordField confirm = new PasswordField();
        Ui.Form form = new Ui.Form()
                .field("currentPassword", "Mot de passe actuel", current)
                .field("newPassword", "Nouveau mot de passe", next)
                .field("confirm", "Confirmation", confirm);
        form.bindErrors(fieldErrorsProperty());

        Button cancel = new Button("Annuler");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> overlay.close());
        Button save = Ui.primary("Modifier", null);
        save.setDefaultButton(true);
        save.disableProperty().bind(busyProperty());
        save.setOnAction(e -> {
            if (!next.getText().equals(confirm.getText())) {
                fieldErrorsProperty().set(Map.of("confirm", "Les deux mots de passe ne correspondent pas."));
                return;
            }
            String c = current.getText();
            String n = next.getText();
            execute(() -> {
                ctx.api().changePassword(c, n);
                return Boolean.TRUE;
            }, ok -> {
                overlay.close();
                onDone.run();
            });
        });
        overlay.sheet("Changer le mot de passe", new VBox(6, Ui.errorBanner(errorProperty()),
                Ui.caption("Toutes vos sessions seront fermées : vous devrez vous reconnecter."), form.node()), cancel, save);
    }
}
