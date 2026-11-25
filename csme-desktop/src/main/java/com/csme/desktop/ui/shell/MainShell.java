package com.csme.desktop.ui.shell;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import com.csme.desktop.AppContext;
import com.csme.desktop.ui.account.ChangePasswordSheet;
import com.csme.desktop.ui.agencies.AgenciesPage;
import com.csme.desktop.ui.audit.AuditPage;
import com.csme.desktop.ui.common.Async;
import com.csme.desktop.ui.common.Icons;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Page;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.companies.CompaniesPage;
import com.csme.desktop.ui.dashboard.CompanyDashboardPage;
import com.csme.desktop.ui.dashboard.PlatformDashboardPage;
import com.csme.desktop.ui.departments.DepartmentsPage;
import com.csme.desktop.ui.roles.RolesPage;
import com.csme.desktop.ui.settings.SettingsPage;
import com.csme.desktop.ui.users.UsersPage;
import com.csme.shared.dto.AuthDtos.CurrentUser;
import com.csme.shared.security.Action;
import com.csme.shared.security.ErpModule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Fenêtre principale : barre latérale (navigation à tuiles colorées), barre supérieure (recherche universelle,
 * état du serveur, apparence, profil) et zone centrale.
 *
 * <p>Raccourcis : Ctrl+K recherche universelle · Ctrl+N action principale de la page · Ctrl+F recherche
 * dans la page · Ctrl+R ou F5 actualiser · Ctrl+1…9 aller aux rubriques · Échap fermer.</p>
 */
public final class MainShell implements Navigator {

    private static final String PREF_UPCOMING_OPEN = "sidebar.upcoming.open";

    private final AppContext ctx;
    private final Runnable onLogout;
    private final StackPane root = new StackPane();
    private final BorderPane layout = new BorderPane();
    private final StackPane content = new StackPane();
    private final ToggleGroup navGroup = new ToggleGroup();
    private final Map<Destination, ToggleButton> navButtons = new EnumMap<>(Destination.class);
    private final Map<Destination, Page> pages = new EnumMap<>(Destination.class);
    private final Preferences prefs = Preferences.userNodeForPackage(MainShell.class);
    private final Overlay overlay;
    private final CommandPalette palette;
    private final ConnectionMonitor monitor;
    private final EventHandler<KeyEvent> shortcuts = this::onKey;
    private Destination current;
    private Scene scene;

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
        this.palette = new CommandPalette(overlay);
        root.sceneProperty().addListener((obs, old, s) -> attachShortcuts(s));
        monitor.start();
        go(defaultDestination());
    }

    public Node view() {
        return root;
    }

    public void dispose() {
        monitor.stop();
        attachShortcuts(null);
    }

    // ------------------------------------------------------------------ navigation

    @Override
    public void go(Destination destination) {
        Page page = pages.computeIfAbsent(destination, this::createPage);
        if (page == null) {
            return;
        }
        current = destination;
        ToggleButton button = navButtons.get(destination);
        if (button != null) {
            button.setSelected(true);
        }
        Node view = page.view();
        content.getChildren().setAll(view);
        view.setOpacity(0);
        view.setTranslateY(6);
        FadeTransition fade = new FadeTransition(Duration.millis(170), view);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), view);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
        page.onShow();
    }

    @Override
    public void goAndCreate(Destination destination) {
        go(destination);
        Page page = pages.get(destination);
        if (page != null) {
            page.primaryAction().ifPresent(a -> a.run().run());
        }
    }

    @Override
    public boolean canGo(Destination destination) {
        return visibleDestinations().contains(destination) && destination.available();
    }

    private Destination defaultDestination() {
        if (ctx.session().isSuperAdmin()) {
            return Destination.PLATFORM_OVERVIEW;
        }
        return visibleDestinations().stream().filter(Destination::available).findFirst().orElse(Destination.DASHBOARD);
    }

    private List<Destination> visibleDestinations() {
        boolean sa = ctx.session().isSuperAdmin();
        return Arrays.stream(Destination.values())
                .filter(d -> sa ? d.platform() : !d.platform())
                .filter(d -> !d.available() || d.platform() || ctx.session().can(d.module(), Action.VIEW))
                .toList();
    }

    private Page createPage(Destination d) {
        return switch (d) {
            case PLATFORM_OVERVIEW -> new PlatformDashboardPage(ctx, overlay, this);
            case PLATFORM_COMPANIES -> new CompaniesPage(ctx, overlay);
            case PLATFORM_AUDIT -> new AuditPage(ctx, overlay, true);
            case DASHBOARD -> new CompanyDashboardPage(ctx, overlay, this);
            case AGENCIES -> new AgenciesPage(ctx, overlay);
            case DEPARTMENTS -> new DepartmentsPage(ctx, overlay);
            case USERS -> new UsersPage(ctx, overlay);
            case ROLES -> new RolesPage(ctx, overlay);
            case AUDIT -> new AuditPage(ctx, overlay, false);
            case SETTINGS -> new SettingsPage(ctx, overlay);
            default -> null;
        };
    }

    // ------------------------------------------------------------------ raccourcis clavier

    private void attachShortcuts(Scene newScene) {
        if (scene != null) {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, shortcuts);
        }
        scene = newScene;
        if (scene != null) {
            scene.addEventFilter(KeyEvent.KEY_PRESSED, shortcuts);
        }
    }

    private void onKey(KeyEvent e) {
        boolean ctrl = e.isShortcutDown();
        if (ctrl && e.getCode() == KeyCode.K) {
            if (!overlay.isOpen()) {
                openPalette();
            }
            e.consume();
            return;
        }
        if (overlay.isOpen()) {
            return; // une feuille est ouverte : ses propres touches s'appliquent (Entrée, Échap)
        }
        Page page = current == null ? null : pages.get(current);
        if (ctrl && e.getCode() == KeyCode.N && page != null) {
            page.primaryAction().ifPresent(a -> a.run().run());
            e.consume();
        } else if (ctrl && e.getCode() == KeyCode.F && page != null) {
            page.focusSearch();
            e.consume();
        } else if (((ctrl && e.getCode() == KeyCode.R) || e.getCode() == KeyCode.F5) && page != null) {
            page.onShow();
            e.consume();
        } else if (ctrl && e.getCode().isDigitKey() && e.getCode() != KeyCode.DIGIT0) {
            int index = Integer.parseInt(e.getCode().getChar()) - 1;
            List<Destination> available = visibleDestinations().stream().filter(Destination::available).toList();
            if (index < available.size()) {
                go(available.get(index));
                e.consume();
            }
        }
    }

    private void openPalette() {
        List<CommandPalette.Command> commands = new ArrayList<>();
        List<Destination> available = visibleDestinations().stream().filter(Destination::available).toList();
        for (int i = 0; i < available.size(); i++) {
            Destination d = available.get(i);
            commands.add(new CommandPalette.Command(d.label(), "Aller à", d.icon(), d.tint(),
                    i < 9 ? "Ctrl " + (i + 1) : null, () -> go(d)));
        }
        addCreate(commands, Destination.PLATFORM_COMPANIES, null, "Nouvelle entreprise");
        addCreate(commands, Destination.USERS, ErpModule.USERS, "Nouvel utilisateur");
        addCreate(commands, Destination.AGENCIES, ErpModule.AGENCIES, "Nouvelle agence");
        addCreate(commands, Destination.DEPARTMENTS, ErpModule.DEPARTMENTS, "Nouveau département");
        addCreate(commands, Destination.ROLES, ErpModule.ROLES, "Nouveau rôle");
        commands.add(new CommandPalette.Command(ctx.theme().darkProperty().get() ? "Passer en mode clair" : "Passer en mode sombre",
                "Apparence", ctx.theme().darkProperty().get() ? "sun" : "moon", "graphite", null, this::toggleTheme));
        commands.add(new CommandPalette.Command("Changer mon mot de passe", "Compte", "key", "gray", null,
                () -> new ChangePasswordSheet(ctx, overlay, this::logout).show()));
        commands.add(new CommandPalette.Command("Se déconnecter", "Compte", "logout", "red", null, this::logout));
        palette.open(commands);
    }

    private void addCreate(List<CommandPalette.Command> commands, Destination d, ErpModule module, String title) {
        boolean allowed = module == null ? ctx.session().isSuperAdmin() && canGo(d)
                : canGo(d) && ctx.session().can(module, Action.CREATE);
        if (allowed) {
            commands.add(new CommandPalette.Command(title, "Créer", "plus", d.tint(), null, () -> goAndCreate(d)));
        }
    }

    // ------------------------------------------------------------------ barre latérale

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
        List<Destination> ready = visible.stream().filter(Destination::available).toList();
        if (sa) {
            addSection(nav, "Plateforme", ready);
        } else {
            addSection(nav, null, ready.stream().filter(d -> d == Destination.DASHBOARD).toList());
            addSection(nav, "Organisation", ready.stream()
                    .filter(d -> d == Destination.AGENCIES || d == Destination.DEPARTMENTS).toList());
            addSection(nav, "Accès et sécurité", ready.stream()
                    .filter(d -> d == Destination.USERS || d == Destination.ROLES || d == Destination.AUDIT).toList());
            addSection(nav, "Entreprise", ready.stream().filter(d -> d == Destination.SETTINGS).toList());
        }
        List<Destination> upcoming = visible.stream().filter(d -> !d.available()).toList();
        if (!upcoming.isEmpty()) {
            addUpcoming(nav, upcoming);
        }
        ScrollPane scroll = new ScrollPane(nav);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Label userName = new Label(user.fullName());
        userName.getStyleClass().add("csme-medium");
        Label userRole = Ui.caption(user.jobTitle() == null ? user.email() : user.jobTitle());
        VBox who = new VBox(1, userName, userRole);
        HBox.setHgrow(who, Priority.ALWAYS);
        HBox userCard = new HBox(Ui.avatar(user.fullName(), 34), who, Icons.of("more", 16));
        userCard.getStyleClass().add("csme-user-card");
        ContextMenu menu = profileMenu();
        userCard.setOnMouseClicked(e -> menu.show(userCard, Side.TOP, 0, -4));

        VBox sidebar = new VBox(brand, scroll, userCard);
        sidebar.getStyleClass().add("csme-sidebar");
        return sidebar;
    }

    private void addSection(VBox nav, String header, List<Destination> items) {
        if (items.isEmpty()) {
            return;
        }
        if (header != null) {
            Label section = new Label(header);
            section.getStyleClass().add("csme-nav-section");
            nav.getChildren().add(section);
        } else {
            javafx.scene.layout.Region gap = new javafx.scene.layout.Region();
            gap.setMinHeight(8);
            nav.getChildren().add(gap);
        }
        for (Destination d : items) {
            ToggleButton b = navItem(d);
            b.setToggleGroup(navGroup);
            b.setOnAction(e -> {
                if (!b.isSelected()) {
                    b.setSelected(true); // une rubrique reste toujours sélectionnée
                }
                go(d);
            });
            navButtons.put(d, b);
            nav.getChildren().add(b);
        }
    }

    /** Les modules pas encore livrés, repliés par défaut pour ne pas encombrer la navigation. */
    private void addUpcoming(VBox nav, List<Destination> upcoming) {
        VBox list = new VBox(2);
        for (Destination d : upcoming) {
            ToggleButton b = navItem(d);
            b.setDisable(true);
            Label phase = new Label("Phase " + d.module().phase());
            phase.getStyleClass().add("csme-nav-phase");
            phase.setMinWidth(Label.USE_PREF_SIZE);
            HBox row = new HBox(b, phase);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 0 8 0 0;");
            HBox.setHgrow(b, Priority.ALWAYS);
            Tooltip.install(row, new Tooltip(d.label() + " : prévu en phase " + d.module().phase() + " du plan"));
            list.getChildren().add(row);
        }
        boolean open = prefs.getBoolean(PREF_UPCOMING_OPEN, false);
        list.setVisible(open);
        list.managedProperty().bind(list.visibleProperty());

        Button disclosure = new Button("Modules à venir (" + upcoming.size() + ")");
        disclosure.getStyleClass().setAll("csme-nav-disclosure");
        disclosure.setGraphic(Icons.of(open ? "chevron-down" : "arrow-right", 12));
        disclosure.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        disclosure.setMaxWidth(Double.MAX_VALUE);
        disclosure.setOnAction(e -> {
            boolean now = !list.isVisible();
            list.setVisible(now);
            prefs.putBoolean(PREF_UPCOMING_OPEN, now);
            disclosure.setGraphic(Icons.of(now ? "chevron-down" : "arrow-right", 12));
        });
        nav.getChildren().addAll(disclosure, list);
    }

    private static ToggleButton navItem(Destination d) {
        ToggleButton b = new ToggleButton(d.label(), Ui.tile(d.icon(), d.tint(), 24));
        b.getStyleClass().setAll("csme-nav-item");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setMinWidth(0);
        return b;
    }

    // ------------------------------------------------------------------ barre supérieure

    private Node topBar() {
        Label searchText = new Label("Rechercher ou aller à…");
        Label kbd = new Label("Ctrl K");
        kbd.getStyleClass().add("csme-kbd");
        HBox search = new HBox(Icons.of("search", 15), searchText, Ui.spacer(), kbd);
        search.getStyleClass().add("csme-search-pill");
        search.setOnMouseClicked(e -> openPalette());
        search.setAccessibleText("Recherche universelle (Ctrl+K)");

        Label statusText = new Label();
        StackPane dot = new StackPane();
        dot.getStyleClass().add("csme-status-dot");
        HBox status = new HBox(dot, statusText);
        status.getStyleClass().add("csme-status");
        monitor.stateProperty().addListener((obs, o, n) -> renderStatus(status, statusText, n));
        renderStatus(status, statusText, monitor.stateProperty().get());
        status.setOnMouseClicked(e -> monitor.checkNow());
        Tooltip.install(status, new Tooltip("Serveur : " + ctx.config().serverUrl() + "\nCliquer pour vérifier maintenant"));

        Button theme = Ui.iconButton(ctx.theme().darkProperty().get() ? "sun" : "moon", "Mode clair / sombre");
        theme.setOnAction(e -> toggleTheme());
        ctx.theme().darkProperty().addListener((obs, o, dark) -> theme.setGraphic(Icons.of(dark ? "sun" : "moon", 16)));

        Button profile = new Button(null, Ui.avatar(ctx.session().user().fullName(), 30));
        profile.getStyleClass().add("csme-profile-button");
        profile.setTooltip(new Tooltip(ctx.session().user().fullName()));
        ContextMenu menu = profileMenu();
        profile.setOnAction(e -> menu.show(profile, Side.BOTTOM, -180, 6));

        HBox bar = new HBox(search, Ui.spacer(), status, theme, profile);
        bar.getStyleClass().add("csme-topbar");
        return bar;
    }

    private ContextMenu profileMenu() {
        CurrentUser user = ctx.session().user();
        MenuItem who = new MenuItem(user.fullName() + " — " + user.email());
        who.setDisable(true);
        MenuItem password = new MenuItem("Changer le mot de passe…", Icons.of("key", 14));
        password.setOnAction(e -> new ChangePasswordSheet(ctx, overlay, this::logout).show());
        MenuItem search = new MenuItem("Recherche universelle        Ctrl K", Icons.of("search", 14));
        search.setOnAction(e -> openPalette());
        MenuItem logout = new MenuItem("Se déconnecter", Icons.of("logout", 14));
        logout.setOnAction(e -> logout());
        ContextMenu menu = new ContextMenu(who, new SeparatorMenuItem(), password, search, new SeparatorMenuItem(), logout);
        menu.getStyleClass().add(Styles.ELEVATED_2);
        return menu;
    }

    private void toggleTheme() {
        ctx.theme().toggle();
    }

    private static void renderStatus(HBox status, Label text, ConnectionMonitor.State state) {
        status.getStyleClass().removeAll("csme-status-online", "csme-status-offline", "csme-status-checking");
        switch (state) {
            case ONLINE -> {
                status.getStyleClass().add("csme-status-online");
                text.setText("En ligne");
            }
            case OFFLINE -> {
                status.getStyleClass().add("csme-status-offline");
                text.setText("Serveur injoignable");
            }
            default -> {
                status.getStyleClass().add("csme-status-checking");
                text.setText("Connexion…");
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
