package com.csme.desktop.ui.dashboard;

import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Page;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.shared.dto.DashboardDtos.CompanySummary;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Tableau de bord de l'entreprise. Les indicateurs affichés sont ceux que les modules livrés permettent
 * de calculer réellement ; les indicateurs commerciaux et financiers arriveront avec leurs modules.
 */
public final class CompanyDashboardPage extends ViewModel implements Page {

    private final AppContext ctx;
    private final VBox body = new VBox(18);
    private final ScrollPane root;

    public CompanyDashboardPage(AppContext ctx, Overlay overlay) {
        this.ctx = ctx;
        Button refresh = Ui.iconButton("refresh", "Actualiser");
        refresh.setOnAction(e -> onShow());
        refresh.disableProperty().bind(busyProperty());
        VBox page = new VBox(Ui.pageHeader("Bonjour, " + firstName(), "Situation de l'entreprise en temps réel",
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
        execute(() -> ctx.api().companySummary(), this::render);
    }

    private void render(CompanySummary s) {
        HBox lower = new HBox(18, DashboardWidgets.loginChart("Activité des comptes", s.loginsLast14Days()),
                DashboardWidgets.recentActivity(s.recentActivity()));
        HBox.setHgrow(lower.getChildren().getFirst(), Priority.ALWAYS);
        body.getChildren().setAll(
                DashboardWidgets.kpiRow(
                        DashboardWidgets.kpi("Utilisateurs actifs", s.activeUsers(), "users", null),
                        DashboardWidgets.kpi("Comptes désactivés", s.inactiveUsers(), "power", null),
                        DashboardWidgets.kpi("Comptes verrouillés", s.lockedUsers(), "lock", s.lockedUsers() > 0 ? "danger" : null),
                        DashboardWidgets.kpi("Agences", s.agencies(), "pin", null),
                        DashboardWidgets.kpi("Connexions aujourd'hui", s.successfulLoginsToday(), "activity", "success"),
                        DashboardWidgets.kpi("Échecs aujourd'hui", s.failedLoginsToday(), "shield",
                                s.failedLoginsToday() > 0 ? "danger" : null)),
                lower);
    }

    private String firstName() {
        String full = ctx.session().user().fullName();
        return full == null ? "" : full.strip().split("\\s+")[0];
    }
}
