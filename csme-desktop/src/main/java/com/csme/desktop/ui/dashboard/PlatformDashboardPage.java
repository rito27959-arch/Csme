package com.csme.desktop.ui.dashboard;

import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Page;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.desktop.ui.shell.Destination;
import com.csme.desktop.ui.shell.Navigator;
import com.csme.shared.dto.DashboardDtos.PlatformSummary;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Vue d'ensemble de la plateforme pour le super administrateur. */
public final class PlatformDashboardPage extends ViewModel implements Page {

    private final AppContext ctx;
    private final Navigator nav;
    private final VBox body = new VBox(20);
    private final ScrollPane root;

    public PlatformDashboardPage(AppContext ctx, Overlay overlay, Navigator nav) {
        this.ctx = ctx;
        this.nav = nav;
        Button refresh = Ui.iconButton("refresh", "Actualiser (Ctrl+R)");
        refresh.setOnAction(e -> onShow());
        VBox page = new VBox(DashboardWidgets.greeting("Plateforme CSME", Ui.spinner(busyProperty()), refresh),
                Ui.errorBanner(errorProperty()), body);
        page.getStyleClass().add("csme-page");
        page.setMinWidth(0);
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
        execute(() -> ctx.api().platformSummary(), this::render);
    }

    private void render(PlatformSummary s) {
        body.getChildren().clear();
        if (s.companies() == 0) {
            Button create = Ui.primary("Créer la première entreprise", "plus");
            create.setOnAction(e -> nav.goAndCreate(Destination.PLATFORM_COMPANIES));
            body.getChildren().add(Ui.card(Ui.emptyState("building", "Aucune entreprise pour l'instant",
                    "Créez une entreprise : ses rôles prédéfinis et le compte de son directeur général seront préparés automatiquement.",
                    create)));
        }
        Runnable companies = () -> nav.go(Destination.PLATFORM_COMPANIES);
        List<javafx.scene.layout.Region> kpis = new ArrayList<>(List.of(
                DashboardWidgets.kpi("Entreprises", s.companies(), "building", "indigo", companies),
                DashboardWidgets.kpi("Entreprises actives", s.activeCompanies(), "check", "green", companies),
                DashboardWidgets.kpi("Utilisateurs", s.users(), "users", "blue", null),
                DashboardWidgets.kpi("Connexions aujourd'hui", s.successfulLoginsToday(), "activity", "teal",
                        () -> nav.go(Destination.PLATFORM_AUDIT))));
        if (s.failedLoginsToday() > 0) {
            body.getChildren().add(DashboardWidgets.alert("À surveiller : " + s.failedLoginsToday()
                    + " échec(s) de connexion aujourd'hui", () -> nav.go(Destination.PLATFORM_AUDIT)));
        }
        body.getChildren().add(DashboardWidgets.equalRow(14, kpis));
        HBox lower = new HBox(18, DashboardWidgets.loginChart("Activité de la plateforme", s.loginsLast14Days()),
                DashboardWidgets.recentActivity(s.recentActivity(), () -> nav.go(Destination.PLATFORM_AUDIT)));
        HBox.setHgrow(lower.getChildren().getFirst(), Priority.ALWAYS);
        body.getChildren().add(lower);
    }
}
