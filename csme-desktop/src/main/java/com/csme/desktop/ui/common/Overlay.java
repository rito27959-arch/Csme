package com.csme.desktop.ui.common;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Couche de superposition de la fenêtre principale : feuilles modales (à la manière des « sheets » macOS)
 * et notifications de succès / erreur. Aucune nouvelle fenêtre n'est ouverte pour une opération courante.
 */
public final class Overlay {

    private final ModalPane modal = new ModalPane();
    private final VBox toasts = new VBox(10);

    public Overlay(StackPane root) {
        modal.setAlignment(Pos.TOP_CENTER);
        modal.usePredefinedTransitionFactories(javafx.geometry.Side.TOP);
        toasts.setAlignment(Pos.BOTTOM_RIGHT);
        toasts.setPadding(new Insets(20));
        toasts.setPickOnBounds(false);
        toasts.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        StackPane.setAlignment(toasts, Pos.BOTTOM_RIGHT);
        root.getChildren().addAll(modal, toasts);
    }

    /** Affiche une feuille modale avec titre, contenu et boutons. */
    public void sheet(String title, Node content, Button... buttons) {
        Label heading = new Label(title);
        heading.getStyleClass().add(Styles.TITLE_3);
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getChildren().add(Ui.spacer());
        actions.getChildren().addAll(buttons);
        VBox sheet = new VBox(16, heading, content, actions);
        sheet.getStyleClass().add("csme-sheet");
        sheet.setPadding(new Insets(22, 24, 20, 24));
        sheet.setMaxWidth(520);
        sheet.setMaxHeight(VBox.USE_PREF_SIZE);
        StackPane.setMargin(sheet, new Insets(36, 0, 0, 0));
        modal.show(sheet);
    }

    public void close() {
        modal.hide(true);
    }

    /** Fenêtre de confirmation. */
    public void confirm(String title, String message, String confirmText, boolean danger, Runnable onConfirm) {
        Label text = new Label(message);
        text.setWrapText(true);
        Button cancel = new Button("Annuler");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> close());
        Button ok = new Button(confirmText);
        ok.getStyleClass().add(danger ? Styles.DANGER : Styles.ACCENT);
        ok.setDefaultButton(true);
        ok.setOnAction(e -> {
            close();
            onConfirm.run();
        });
        sheet(title, text, cancel, ok);
    }

    public void success(String message) {
        toast(message, Styles.SUCCESS, Icons.of("check", 16));
    }

    public void error(String message) {
        toast(message, Styles.DANGER, Icons.of("shield", 16));
    }

    private void toast(String message, String tone, Node icon) {
        Notification n = new Notification(message, icon);
        n.getStyleClass().addAll(tone, Styles.ELEVATED_2);
        n.setPrefWidth(360);
        n.setMaxWidth(360);
        n.setMaxHeight(Notification.USE_PREF_SIZE);
        n.setOnClose(e -> toasts.getChildren().remove(n));
        toasts.getChildren().add(n);
        Animations.slideInUp(n, Duration.millis(220)).playFromStart();
        PauseTransition delay = new PauseTransition(Duration.seconds(tone.equals(Styles.DANGER) ? 7 : 4));
        delay.setOnFinished(e -> toasts.getChildren().remove(n));
        delay.play();
    }
}
