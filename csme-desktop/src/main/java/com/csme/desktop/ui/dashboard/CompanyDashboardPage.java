package com.csme.desktop.ui.dashboard;

import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Page;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.desktop.ui.shell.Destination;
import com.csme.desktop.ui.shell.Navigator;
import com.csme.shared.dto.DashboardDtos.CompanySummary;
import com.csme.shared.security.Action;
import com.csme.shared.security.ErpModule;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Tableau de bord de l'entreprise : salutation, liste « Prise en main » tant que l'entreprise n'est pas configurée,
 * alertes, indicateurs, actions rapides et activité. Seuls des indicateurs réellement calculés sont affichés ;
 * le chiffre d'affaires, le stock ou les dépenses arriveront avec leurs modules.
 */
public final class CompanyDashboardPage extends ViewModel implements Page {

    private final AppContext ctx;
    private final Navigator nav;
    private final VBox body = new VBox(20);
    private final ScrollPane root;

    public CompanyDashboardPage(AppContext ctx, Overlay overlay, Navigator nav) {
        this.ctx = ctx;
        this.nav = nav;
        Button refresh = Ui.iconButton("refresh", "Actualiser (Ctrl+R)");
        refresh.setOnAction(e -> onShow());
        refresh.disableProperty().bind(busyProperty());
        VBox page = new VBox(DashboardWidgets.greeting(salutation() + ", " + firstName(), Ui.spinner(busyProperty()), refresh),
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
        execute(() -> ctx.api().companySummary(), this::render);
    }

    private void render(CompanySummary s) {
        body.getChildren().clear();

        List<DashboardWidgets.Step> steps = setupSteps(s);
        if (steps.stream().anyMatch(st -> !st.done())) {
            body.getChildren().add(DashboardWidgets.checklist(steps));
        }

        List<String> alerts = new ArrayList<>();
        if (s.lockedUsers() > 0) {
            alerts.add(s.lockedUsers() + (s.lockedUsers() > 1 ? " comptes verrouillés" : " compte verrouillé"));
        }
        if (s.failedLoginsToday() > 0) {
            alerts.add(s.failedLoginsToday() + (s.failedLoginsToday() > 1 ? " échecs de connexion" : " échec de connexion")
                    + " aujourd'hui");
        }
        if (!alerts.isEmpty()) {
            Destination target = s.lockedUsers() > 0 ? Destination.USERS : Destination.AUDIT;
            body.getChildren().add(DashboardWidgets.alert("À surveiller : " + String.join(" · ", alerts),
                    nav.canGo(target) ? () -> nav.go(target) : null));
        }

        body.getChildren().add(DashboardWidgets.equalRow(14, List.of(
                DashboardWidgets.kpi("Utilisateurs actifs", s.activeUsers(), "users", "indigo", link(Destination.USERS)),
                DashboardWidgets.kpi("Agences", s.agencies(), "pin", "red", link(Destination.AGENCIES)),
                DashboardWidgets.kpi("Départements", s.departments(), "layers", "orange", link(Destination.DEPARTMENTS)),
                DashboardWidgets.kpi("Connexions aujourd'hui", s.successfulLoginsToday(), "activity", "green",
                        link(Destination.AUDIT)))));

        List<Region> quick = new ArrayList<>();
        addQuick(quick, "Nouvel utilisateur", "plus", "indigo", Destination.USERS, ErpModule.USERS);
        addQuick(quick, "Nouvelle agence", "plus", "red", Destination.AGENCIES, ErpModule.AGENCIES);
        addQuick(quick, "Nouveau département", "plus", "orange", Destination.DEPARTMENTS, ErpModule.DEPARTMENTS);
        addQuick(quick, "Nouveau rôle", "plus", "yellow", Destination.ROLES, ErpModule.ROLES);
        if (!quick.isEmpty()) {
            body.getChildren().addAll(Ui.sectionTitle("Actions rapides"), DashboardWidgets.equalRow(12, quick));
        }

        HBox lower = new HBox(18, DashboardWidgets.loginChart("Activité des comptes", s.loginsLast14Days()),
                DashboardWidgets.recentActivity(s.recentActivity(),
                        nav.canGo(Destination.AUDIT) ? () -> nav.go(Destination.AUDIT) : null));
        HBox.setHgrow(lower.getChildren().getFirst(), Priority.ALWAYS);
        body.getChildren().add(lower);
    }

    private List<DashboardWidgets.Step> setupSteps(CompanySummary s) {
        List<DashboardWidgets.Step> steps = new ArrayList<>();
        if (ctx.session().can(ErpModule.SETTINGS, Action.UPDATE)) {
            steps.add(new DashboardWidgets.Step("Compléter les informations de l'entreprise",
                    "Raison sociale, adresse, téléphone et identifiant fiscal figureront sur vos documents.",
                    s.profileComplete(), "Compléter", () -> nav.go(Destination.SETTINGS)));
        }
        if (ctx.session().can(ErpModule.AGENCIES, Action.CREATE)) {
            steps.add(new DashboardWidgets.Step("Créer votre première agence",
                    "Chaque point de vente ou bureau est une agence.", s.agencies() > 0, "Créer",
                    () -> nav.goAndCreate(Destination.AGENCIES)));
        }
        if (ctx.session().can(ErpModule.DEPARTMENTS, Action.CREATE)) {
            steps.add(new DashboardWidgets.Step("Organiser les départements",
                    "Ventes, comptabilité, logistique… rattachés à une agence.", s.departments() > 0, "Créer",
                    () -> nav.goAndCreate(Destination.DEPARTMENTS)));
        }
        if (ctx.session().can(ErpModule.USERS, Action.CREATE)) {
            steps.add(new DashboardWidgets.Step("Ajouter vos collaborateurs",
                    "Créez leurs comptes et attribuez-leur un rôle.", s.activeUsers() > 1, "Ajouter",
                    () -> nav.goAndCreate(Destination.USERS)));
        }
        return steps;
    }

    private void addQuick(List<Region> quick, String label, String icon, String tint, Destination d, ErpModule module) {
        if (nav.canGo(d) && ctx.session().can(module, Action.CREATE)) {
            quick.add(DashboardWidgets.quickAction(label, icon, tint, () -> nav.goAndCreate(d)));
        }
    }

    private Runnable link(Destination d) {
        return nav.canGo(d) ? () -> nav.go(d) : null;
    }

    private static String salutation() {
        return LocalTime.now().getHour() >= 18 ? "Bonsoir" : "Bonjour";
    }

    private String firstName() {
        String full = ctx.session().user().fullName();
        return full == null ? "" : full.strip().split("\\s+")[0];
    }
}
