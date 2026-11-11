package com.csme.desktop.ui.dashboard;

import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Page;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.shared.dto.DashboardDtos.PlatformSummary;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Vue d'ensemble de la plateforme pour le super administrateur. */
public final class PlatformDashboardPage extends ViewModel implements Page {

    private final AppContext ctx;
    private final VBox body = new VBox(18);
    private final ScrollPane root;

    public PlatformDashboardPage(AppContext ctx, Overlay overlay) {
        this.ctx = ctx;
        Button refresh = Ui.iconButton("refresh", "Actualiser");
        refresh.setOnAction(e -> onShow());
        VBox page = new VBox(Ui.pageHeader("Plateforme CSME", "Entreprises hébergées et activité globale",
                Ui.spinner(busyProperty()), refresh), Ui.errorBanner(errorProperty()), body);
        page.getStyleClass().add("csme-page");
        root = new ScrollPane(page);
        root.setFitToWidth(true);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        page.setMinWidth(0);
    }

    @Override
    public Node view() {
        return root;
    }

    @Override
    public void onShow() {
        execute(() -> ctx.api().platformSummary(), this::render);
    }

    private void render(PlatformSummary s) {
        HBox lower = new HBox(18, DashboardWidgets.loginChart("Activité de la plateforme", s.loginsLast14Days()),
                DashboardWidgets.recentActivity(s.recentActivity()));
        HBox.setHgrow(lower.getChildren().getFirst(), Priority.ALWAYS);
        body.getChildren().setAll(
                DashboardWidgets.kpiRow(
                        DashboardWidgets.kpi("Entreprises", s.companies(), "building", null),
                        DashboardWidgets.kpi("Entreprises actives", s.activeCompanies(), "check", "success"),
                        DashboardWidgets.kpi("Utilisateurs", s.users(), "users", null),
                        DashboardWidgets.kpi("Connexions aujourd'hui", s.successfulLoginsToday(), "activity", "success"),
                        DashboardWidgets.kpi("Échecs aujourd'hui", s.failedLoginsToday(), "shield",
                                s.failedLoginsToday() > 0 ? "danger" : null)),
                lower);
    }
}
