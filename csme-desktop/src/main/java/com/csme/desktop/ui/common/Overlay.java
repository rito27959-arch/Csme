package com.csme.desktop.ui.common;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.theme.Styles;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
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
 * Couche de superposition de la fenêtre principale : feuilles modales (comme les « sheets » d'Apple),
 * palette de commandes et bannières de notification en haut de l'écran. Aucune nouvelle fenêtre
 * n'est ouverte pour une opération courante ; Échap ferme toujours la feuille ouverte.
 */
public final class Overlay {

    private final ModalPane modal = new ModalPane();
    private final VBox banners = new VBox(8);

    public Overlay(StackPane root) {
        modal.setAlignment(Pos.TOP_CENTER);
        modal.usePredefinedTransitionFactories(javafx.geometry.Side.TOP);
        modal.setPersistent(false);
        banners.setAlignment(Pos.TOP_CENTER);
        banners.setPadding(new Insets(16));
        banners.setPickOnBounds(false);
        banners.setMouseTransparent(false);
        banners.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        StackPane.setAlignment(banners, Pos.TOP_CENTER);
        root.getChildren().addAll(modal, banners);
    }

    public boolean isOpen() {
        return modal.isDisplay();
    }

    /** Affiche une feuille modale avec titre, contenu et boutons. */
    public void sheet(String title, Node content, Button... buttons) {
        Label heading = new Label(title);
        heading.getStyleClass().add("csme-sheet-title");
        heading.setWrapText(true);
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getChildren().add(Ui.spacer());
        actions.getChildren().addAll(buttons);
        VBox sheet = new VBox(18, heading, content, actions);
        sheet.getStyleClass().add("csme-sheet");
        sheet.setPadding(new Insets(24, 26, 22, 26));
        sheet.setMaxWidth(540);
        sheet.setMaxHeight(VBox.USE_PREF_SIZE);
        StackPane.setMargin(sheet, new Insets(48, 0, 0, 0));
        modal.show(sheet);
    }

    /** Affiche un contenu libre au centre haut (utilisé par la palette de commandes). */
    public void present(Node node) {
        StackPane.setMargin(node, new Insets(90, 0, 0, 0));
        modal.show(node);
    }

    public void close() {
        modal.hide(true);
    }

    /** Fenêtre de confirmation. */
    public void confirm(String title, String message, String confirmText, boolean danger, Runnable onConfirm) {
        Label text = new Label(message);
        text.setWrapText(true);
        text.getStyleClass().add(Styles.TEXT_MUTED);
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
        banner(message, "success", "check");
    }

    public void error(String message) {
        banner(message, "error", "shield");
    }

    /** Bannière en capsule qui glisse depuis le haut, puis disparaît d'elle-même. */
    private void banner(String message, String tone, String icon) {
        StackPane iconCircle = new StackPane(Icons.of(icon, 14));
        iconCircle.getStyleClass().add("csme-banner-icon");
        Label text = new Label(message);
        text.setWrapText(true);
        text.setMaxWidth(460);
        HBox banner = new HBox(iconCircle, text);
        banner.getStyleClass().addAll("csme-banner", "csme-banner-" + tone);
        banner.setMaxWidth(HBox.USE_PREF_SIZE);
        banner.setOnMouseClicked(e -> dismiss(banner));
        banners.getChildren().add(banner);

        banner.setOpacity(0);
        banner.setTranslateY(-16);
        FadeTransition fade = new FadeTransition(Duration.millis(180), banner);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(260), banner);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();

        PauseTransition delay = new PauseTransition(Duration.seconds("error".equals(tone) ? 7 : 3.5));
        delay.setOnFinished(e -> dismiss(banner));
        delay.play();
    }

    private void dismiss(Node banner) {
        if (!banners.getChildren().contains(banner)) {
            return;
        }
        FadeTransition fade = new FadeTransition(Duration.millis(200), banner);
        fade.setToValue(0);
        fade.setOnFinished(e -> banners.getChildren().remove(banner));
        fade.play();
    }
}
