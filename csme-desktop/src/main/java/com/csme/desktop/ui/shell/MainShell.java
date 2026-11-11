package com.csme.desktop.ui.shell;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import com.csme.desktop.AppContext;
import com.csme.desktop.ui.account.ChangePasswordSheet;
import com.csme.desktop.ui.agencies.AgenciesPage;
import com.csme.desktop.ui.audit.AuditPage;
import com.csme.desktop.ui.common.Async;
import com.csme.desktop.ui.common.Formats;
import com.csme.desktop.ui.common.Icons;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Page;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.companies.CompaniesPage;
import com.csme.desktop.ui.dashboard.CompanyDashboardPage;
import com.csme.desktop.ui.dashboard.PlatformDashboardPage;
import com.csme.desktop.ui.users.UsersPage;
import com.csme.shared.dto.AuthDtos.CurrentUser;
import com.csme.shared.security.Action;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Fenêtre principale : barre latérale (navigation), barre supérieure (titre, état du serveur, thème,
 * profil) et zone centrale. La navigation change la zone centrale sans ouvrir de nouvelle fenêtre.
 */
public final class MainShell {

    private final AppContext ctx;
    private final Runnable onLogout;
    private final StackPane root = new StackPane();
    private final BorderPane layout = new BorderPane();
    private final StackPane content = new StackPane();
    private final Label title = new Label();
    private final ToggleGroup navGroup = new ToggleGroup();
    private final Map<Destination, ToggleButton> navButtons = new EnumMap<>(Destination.class);
    private final Map<Destination, Page> pages = new EnumMap<>(Destination.class);
    private final Overlay overlay;
    private final ConnectionMonitor monitor;

    public MainShell(AppContext ctx, Runnable onLogout) {
        this.ctx = ctx;
        this.onLogout = onLogout;
        this.monitor = new ConnectionMonitor(ctx.api().client());
        root.getStyleClass().add("csme-window");
        content.getStyleClass().add("csme-content");
        VBox center = new VBox(topBar(), content);
        VBox.setVgrow(content, Priority.ALWAYS);
        layout.setLeft(sidebar());
        layout.setCenter(center);
        root.getChildren().add(layout);
        this.overlay = new Overlay(root);
        monitor.start();
        navigate(defaultDestination());
    }

    public Node view() {
        return root;
    }

    public void dispose() {
        monitor.stop();
    }

    // ------------------------------------------------------------------ navigation

    private Destination defaultDestination() {
        if (ctx.session().isSuperAdmin()) {
            return Destination.PLATFORM_OVERVIEW;
        }
        return visibleDestinations().stream().filter(Destination::available).findFirst().orElse(Destination.DASHBOARD);
    }

    private List<Destination> visibleDestinations() {
        boolean sa = ctx.session().isSuperAdmin();
        return java.util.Arrays.stream(Destination.values())
                .filter(d -> sa ? d.platform() : !d.platform())
                .filter(d -> !d.available() || d.platform() || ctx.session().can(d.module(), Action.VIEW))
                .toList();
    }

    private void navigate(Destination destination) {
        Page page = pages.computeIfAbsent(destination, this::createPage);
        if (page == null) {
            return;
        }
        title.setText(destination.label());
        ToggleButton button = navButtons.get(destination);
        if (button != null) {
            button.setSelected(true);
        }
        content.getChildren().setAll(page.view());
        page.onShow();
    }

    private Page createPage(Destination d) {
        Function<Destination, Page> factory = dest -> switch (dest) {
            case PLATFORM_OVERVIEW -> new PlatformDashboardPage(ctx, overlay);
            case PLATFORM_COMPANIES -> new CompaniesPage(ctx, overlay);
            case PLATFORM_AUDIT -> new AuditPage(ctx, overlay, true);
            case DASHBOARD -> new CompanyDashboardPage(ctx, overlay);
            case AGENCIES -> new AgenciesPage(ctx, overlay);
            case USERS -> new UsersPage(ctx, overlay);
            case AUDIT -> new AuditPage(ctx, overlay, false);
            default -> null;
        };
        return factory.apply(d);
    }

    // ------------------------------------------------------------------ sidebar

    private Node sidebar() {
        CurrentUser user = ctx.session().user();
        StackPane logo = new StackPane(new Label("C"));
        logo.getStyleClass().add("csme-logo");
        Label name = new Label("CSME ERP");
        name.getStyleClass().add("csme-brand-name");
        Label company = Ui.caption(user.companyName());
        HBox brand = new HBox(logo, new VBox(1, name, company));
        brand.getStyleClass().add("csme-brand");

        VBox nav = new VBox(2);
        nav.setFillWidth(true);
        nav.setStyle("-fx-padding: 0 10 10 10;");
        List<Destination> visible = visibleDestinations();
        boolean sa = ctx.session().isSuperAdmin();
        addSection(nav, sa ? "PLATEFORME" : "ORGANISATION", visible.stream().filter(Destination::available).toList());
        List<Destination> upcoming = visible.stream().filter(d -> !d.available()).toList();
        if (!upcoming.isEmpty()) {
            addSection(nav, "MODULES À VENIR", upcoming);
        }
        ScrollPane scroll = new ScrollPane(nav);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        StackPane avatar = new StackPane(new Label(Formats.initials(user.fullName())));
        avatar.getStyleClass().add("csme-avatar");
        Label userName = new Label(user.fullName());
        userName.getStyleClass().add(Styles.TEXT_BOLD);
        Label userMail = Ui.caption(user.email());
        VBox who = new VBox(1, userName, userMail);
        HBox.setHgrow(who, Priority.ALWAYS);
        HBox userCard = new HBox(avatar, who);
        userCard.getStyleClass().add("csme-user-card");

        VBox sidebar = new VBox(brand, scroll, userCard);
        sidebar.getStyleClass().add("csme-sidebar");
        return sidebar;
    }

    private void addSection(VBox nav, String header, List<Destination> items) {
        if (items.isEmpty()) {
            return;
        }
        Label section = new Label(header);
        section.getStyleClass().add("csme-nav-section");
        section.setStyle("-fx-padding: 14 0 4 12;");
        nav.getChildren().add(section);
        for (Destination d : items) {
            ToggleButton b = new ToggleButton(d.label(), Icons.of(d.icon(), 16));
            b.getStyleClass().setAll("csme-nav-item");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setToggleGroup(navGroup);
            if (d.available()) {
                b.setOnAction(e -> {
                    if (!b.isSelected()) {
                        b.setSelected(true); // une pastille reste toujours sélectionnée
                    }
                    navigate(d);
                });
                navButtons.put(d, b);
                nav.getChildren().add(b);
            } else {
                b.setDisable(true);
                Label phase = new Label("Phase " + d.module().phase());
                phase.getStyleClass().add("csme-nav-phase");
                phase.setMinWidth(Label.USE_PREF_SIZE);
                b.setText(d.label());
                b.setMinWidth(0);
                HBox row = new HBox(b, phase);
                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(b, Priority.ALWAYS);
                Tooltip.install(row, new Tooltip(d.label() + " : prévu en phase " + d.module().phase()));
                nav.getChildren().add(row);
            }
        }
    }

    // ------------------------------------------------------------------ top bar

    private Node topBar() {
        title.getStyleClass().add("csme-topbar-title");

        Label statusText = new Label();
        statusText.getStyleClass().add(Styles.TEXT_SMALL);
        StackPane dot = new StackPane();
        dot.getStyleClass().add("csme-status-dot");
        HBox status = new HBox(dot, statusText);
        status.getStyleClass().add("csme-status");
        monitor.stateProperty().addListener((obs, o, n) -> renderStatus(status, statusText, n));
        renderStatus(status, statusText, monitor.stateProperty().get());
        status.setOnMouseClicked(e -> monitor.checkNow());
        Tooltip.install(status, new Tooltip("Serveur : " + ctx.config().serverUrl() + " — cliquer pour vérifier"));

        javafx.scene.control.Button theme = Ui.iconButton(ctx.theme().darkProperty().get() ? "sun" : "moon", "Mode clair / sombre");
        theme.setOnAction(e -> {
            ctx.theme().toggle();
            theme.setGraphic(Icons.of(ctx.theme().darkProperty().get() ? "sun" : "moon", 16));
        });

        MenuButton profile = new MenuButton(ctx.session().user().fullName());
        profile.getStyleClass().addAll(Styles.FLAT);
        profile.setGraphic(Icons.of("users", 15));
        MenuItem password = new MenuItem("Changer le mot de passe…", Icons.of("key", 14));
        password.setOnAction(e -> new ChangePasswordSheet(ctx, overlay, this::logout).show());
        MenuItem logout = new MenuItem("Se déconnecter", Icons.of("logout", 14));
        logout.setOnAction(e -> logout());
        profile.getItems().addAll(password, new SeparatorMenuItem(), logout);

        HBox bar = new HBox(title, Ui.spacer(), status, theme, profile);
        bar.getStyleClass().add("csme-topbar");
        return bar;
    }

    private static void renderStatus(HBox status, Label text, ConnectionMonitor.State state) {
        status.getStyleClass().removeAll("csme-status-online", "csme-status-offline", "csme-status-checking");
        switch (state) {
            case ONLINE -> {
                status.getStyleClass().add("csme-status-online");
                text.setText("Connecté");
            }
            case OFFLINE -> {
                status.getStyleClass().add("csme-status-offline");
                text.setText("Serveur injoignable");
            }
            default -> {
                status.getStyleClass().add("csme-status-checking");
                text.setText("Vérification…");
            }
        }
    }

    private void logout() {
        String refresh = ctx.session().refreshToken().orElse(null);
        dispose();
        ctx.session().clear();
        if (refresh != null) {
            Async.run(() -> {
                ctx.api().logout(refresh);
                return null;
            }, r -> { }, e -> { });
        }
        onLogout.run();
    }
}
