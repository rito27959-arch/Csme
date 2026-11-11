package com.csme.desktop.ui.dashboard;

import atlantafx.base.theme.Styles;
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
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Éléments visuels des tableaux de bord : tuiles d'indicateurs, graphique d'activité, activité récente. */
final class DashboardWidgets {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM", Locale.FRANCE);

    private DashboardWidgets() {
    }

    static VBox kpi(String label, long value, String icon, String tone) {
        StackPane iconBox = new StackPane(Icons.of(icon, 18));
        iconBox.getStyleClass().add("csme-kpi-icon");
        iconBox.setMaxSize(StackPane.USE_PREF_SIZE, StackPane.USE_PREF_SIZE);
        Label v = new Label(Formats.number(value));
        v.getStyleClass().add("csme-kpi-value");
        Label l = new Label(label);
        l.getStyleClass().add("csme-kpi-label");
        l.setWrapText(true);
        l.setMinWidth(0);
        v.setMinWidth(0);
        VBox tile = Ui.card(iconBox, v, l);
        tile.getStyleClass().add("csme-kpi");
        if (tone != null) {
            tile.getStyleClass().add("csme-kpi-" + tone);
        }
        tile.setSpacing(4);
        return tile;
    }

    /** Rangée de tuiles de largeur égale, qui se partagent l'espace disponible. */
    static HBox kpiRow(Node... tiles) {
        HBox row = new HBox(14, tiles);
        for (Node t : tiles) {
            HBox.setHgrow(t, javafx.scene.layout.Priority.ALWAYS);
            if (t instanceof VBox v) {
                v.setPrefWidth(0);
                v.setMinWidth(110);
                v.setMaxWidth(Double.MAX_VALUE);
            }
        }
        return row;
    }

    static VBox loginChart(String title, List<DailyCount> data) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setMinorTickVisible(false);
        y.setForceZeroInRange(true);
        y.setTickUnit(1);
        y.setTickLabelFormatter(new javafx.util.StringConverter<>() {
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
        chart.setCategoryGap(10);
        chart.setPrefHeight(240);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (DailyCount d : data) {
            series.getData().add(new XYChart.Data<>(DAY.format(LocalDate.parse(d.day())), d.value()));
        }
        chart.getData().add(series);
        Label heading = new Label(title);
        heading.getStyleClass().add(Styles.TITLE_4);
        chart.setMinWidth(0);
        VBox card = Ui.card(heading, Ui.caption("Connexions réussies par jour, 14 derniers jours"), chart);
        card.setMinWidth(0);
        card.setPrefWidth(0);
        return card;
    }

    static VBox recentActivity(List<AuditLogDto> items) {
        Label heading = new Label("Activité récente");
        heading.getStyleClass().add(Styles.TITLE_4);
        VBox list = new VBox();
        if (items.isEmpty()) {
            Label empty = new Label("Aucune activité enregistrée pour l'instant.");
            empty.getStyleClass().add("csme-empty");
            list.getChildren().add(empty);
        }
        for (AuditLogDto a : items) {
            String tone = switch (a.outcome()) {
                case "SUCCESS" -> "success";
                case "DENIED" -> "warning";
                default -> "danger";
            };
            Label action = new Label(Formats.action(a.action()));
            action.getStyleClass().add(Styles.TEXT_BOLD);
            Label who = Ui.caption(a.actorEmail() == null ? "Système" : a.actorEmail());
            VBox text = new VBox(1, action, who);
            HBox row = new HBox(Ui.badge(tone.equals("success") ? "OK" : tone.equals("warning") ? "Refusé" : "Échec", tone),
                    text, Ui.spacer(), Ui.caption(Formats.dateTime(a.occurredAt())));
            row.getStyleClass().add("csme-activity-row");
            row.setAlignment(Pos.CENTER_LEFT);
            list.getChildren().add(row);
        }
        VBox card = Ui.card(heading, list);
        card.setPrefWidth(380);
        card.setMinWidth(340);
        card.setMaxWidth(420);
        return card;
    }
}
