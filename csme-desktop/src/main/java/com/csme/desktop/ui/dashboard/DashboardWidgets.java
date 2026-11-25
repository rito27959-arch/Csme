package com.csme.desktop.ui.dashboard;

import com.csme.desktop.ui.common.Formats;
import com.csme.desktop.ui.common.Icons;
import com.csme.desktop.ui.common.Ui;
import com.csme.shared.dto.AuditDtos.AuditLogDto;
import com.csme.shared.dto.DashboardDtos.DailyCount;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Éléments visuels des tableaux de bord : en-tête, indicateurs, prise en main, actions rapides, activité. */
final class DashboardWidgets {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM", Locale.FRANCE);
    private static final DateTimeFormatter TODAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE);

    private DashboardWidgets() {
    }

    /** Date du jour en petites capitales au-dessus d'un grand titre, comme l'écran Aujourd'hui d'iOS. */
    static HBox greeting(String title, Node... actions) {
        Label date = new Label(TODAY.format(LocalDate.now()).toUpperCase(Locale.FRANCE));
        date.getStyleClass().add("csme-greeting-date");
        Label t = new Label(title);
        t.getStyleClass().add("csme-large-title");
        HBox header = new HBox(10, new VBox(2, date, t), Ui.spacer());
        header.getChildren().addAll(actions);
        header.setAlignment(Pos.BOTTOM_LEFT);
        return header;
    }

    /** Carte d'indicateur : tuile colorée, grande valeur, libellé ; cliquable si {@code onClick} est fourni. */
    static VBox kpi(String label, long value, String icon, String tint, Runnable onClick) {
        Label v = new Label(Formats.number(value));
        v.getStyleClass().add("csme-kpi-value");
        Label l = new Label(label);
        l.getStyleClass().add("csme-kpi-label");
        l.setWrapText(true);
        l.setMinWidth(0);
        HBox top = new HBox(Ui.tile(icon, tint, 30), Ui.spacer());
        if (onClick != null) {
            top.getChildren().add(Icons.of("arrow-right", 14));
        }
        VBox tile = Ui.card(top, v, l);
        tile.getStyleClass().add("csme-kpi");
        tile.setSpacing(4);
        if (onClick != null) {
            tile.setOnMouseClicked(e -> onClick.run());
            tile.setStyle("-fx-cursor: hand;");
        }
        return tile;
    }

    /** Rangée de cartes de largeur égale qui se partagent l'espace disponible. */
    static HBox equalRow(double gap, List<? extends Region> cells) {
        HBox row = new HBox(gap);
        for (Region c : cells) {
            HBox.setHgrow(c, Priority.ALWAYS);
            c.setPrefWidth(0);
            c.setMinWidth(110);
            c.setMaxWidth(Double.MAX_VALUE);
            row.getChildren().add(c);
        }
        return row;
    }

    /** Action rapide : tuile + libellé, comme un widget. */
    static HBox quickAction(String label, String icon, String tint, Runnable run) {
        Label l = new Label(label);
        l.setWrapText(true);
        l.setMinWidth(0);
        HBox box = new HBox(Ui.tile(icon, tint, 32), l);
        box.getStyleClass().add("csme-quick-action");
        box.setOnMouseClicked(e -> run.run());
        box.setAccessibleText(label);
        return box;
    }

    /** Une étape de la liste « Prise en main ». */
    record Step(String title, String detail, boolean done, String actionLabel, Runnable action) {
    }

    static VBox checklist(List<Step> steps) {
        long done = steps.stream().filter(Step::done).count();
        Label title = Ui.sectionTitle("Prise en main");
        Label progressText = Ui.caption(done + " sur " + steps.size() + " étapes terminées");

        StackPane track = new StackPane();
        track.getStyleClass().add("csme-progress-track");
        Region fill = new Region();
        fill.getStyleClass().add("csme-progress-fill");
        fill.maxWidthProperty().bind(track.widthProperty().multiply(steps.isEmpty() ? 0 : (double) done / steps.size()));
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);

        VBox rows = new VBox();
        for (Step s : steps) {
            StackPane check = new StackPane();
            check.getStyleClass().add("csme-check");
            if (s.done()) {
                check.getStyleClass().add("csme-check-done");
                check.getChildren().add(Icons.of("check", 13));
            }
            Label t = new Label(s.title());
            t.getStyleClass().add("csme-medium");
            if (s.done()) {
                t.setStyle("-fx-text-fill: -color-fg-muted;");
            }
            VBox text = new VBox(1, t, Ui.caption(s.detail()));
            HBox row = new HBox(check, text, Ui.spacer());
            row.getStyleClass().add("csme-checklist-row");
            if (!s.done() && s.action() != null) {
                Button go = new Button(s.actionLabel());
                go.getStyleClass().addAll(atlantafx.base.theme.Styles.ACCENT, atlantafx.base.theme.Styles.SMALL);
                go.setOnAction(e -> s.action().run());
                row.getChildren().add(go);
            }
            rows.getChildren().add(row);
        }
        HBox head = new HBox(10, title, Ui.spacer(), progressText);
        head.setAlignment(Pos.CENTER_LEFT);
        return Ui.card(head, track, rows);
    }

    /** Bandeau d'alerte cliquable (comptes verrouillés, échecs de connexion). */
    static HBox alert(String text, Runnable onClick) {
        Label l = new Label(text);
        l.getStyleClass().add("csme-medium");
        l.setStyle("-fx-text-fill: -color-warning-fg;");
        HBox box = new HBox(10, Ui.tile("shield", "orange", 24), l, Ui.spacer(), Icons.of("arrow-right", 14));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: -color-warning-subtle; -fx-background-radius: 12; -fx-padding: 10 14 10 12;"
                + (onClick != null ? " -fx-cursor: hand;" : ""));
        if (onClick != null) {
            box.setOnMouseClicked(e -> onClick.run());
        }
        return box;
    }

    static VBox loginChart(String title, List<DailyCount> data) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setMinorTickVisible(false);
        y.setForceZeroInRange(true);
        y.setTickUnit(1);
        y.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number n) {
                return n.doubleValue() == Math.rint(n.doubleValue()) ? String.valueOf(n.longValue()) : "";
            }

            @Override
            public Number fromString(String s) {
                return Double.valueOf(s);
            }
        });
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(12);
        chart.setPrefHeight(240);
        chart.setMinWidth(0);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        long total = 0;
        for (DailyCount d : data) {
            series.getData().add(new XYChart.Data<>(DAY.format(LocalDate.parse(d.day())), d.value()));
            total += d.value();
        }
        chart.getData().add(series);
        Label value = new Label(Formats.number(total));
        value.getStyleClass().add("csme-kpi-value");
        VBox head = new VBox(2, Ui.sectionTitle(title), Ui.caption("Connexions réussies sur 14 jours"));
        HBox top = new HBox(head, Ui.spacer(), value);
        top.setAlignment(Pos.CENTER_LEFT);
        VBox card = Ui.card(top, chart);
        card.setMinWidth(0);
        card.setPrefWidth(0);
        return card;
    }

    static VBox recentActivity(List<AuditLogDto> items, Runnable seeAll) {
        VBox list = new VBox();
        if (items.isEmpty()) {
            list.getChildren().add(Ui.emptyState("clock", "Aucune activité", "Les opérations apparaîtront ici dès qu'elles seront effectuées.", null));
        }
        for (AuditLogDto a : items.stream().limit(7).toList()) {
            String tone = switch (a.outcome()) {
                case "SUCCESS" -> "success";
                case "DENIED" -> "warning";
                default -> "danger";
            };
            StackPane dot = new StackPane();
            dot.getStyleClass().addAll("csme-activity-dot", "csme-activity-" + tone);
            Label action = new Label(Formats.action(a.action()));
            action.getStyleClass().add("csme-medium");
            Label who = Ui.caption(a.actorEmail() == null ? "Système" : a.actorEmail());
            VBox text = new VBox(1, action, who);
            text.setMinWidth(0);
            HBox row = new HBox(dot, text, Ui.spacer(), Ui.caption(Formats.relative(a.occurredAt())));
            row.getStyleClass().add("csme-activity-row");
            list.getChildren().add(row);
        }
        HBox head = new HBox(Ui.sectionTitle("Activité récente"), Ui.spacer());
        head.setAlignment(Pos.CENTER_LEFT);
        if (seeAll != null) {
            Button all = new Button("Tout voir");
            all.getStyleClass().addAll(atlantafx.base.theme.Styles.FLAT, atlantafx.base.theme.Styles.ACCENT, atlantafx.base.theme.Styles.SMALL);
            all.setOnAction(e -> seeAll.run());
            head.getChildren().add(all);
        }
        VBox card = Ui.card(head, list);
        card.setPrefWidth(380);
        card.setMinWidth(320);
        card.setMaxWidth(420);
        return card;
    }
}
