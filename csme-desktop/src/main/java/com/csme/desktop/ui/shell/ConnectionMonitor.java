package com.csme.desktop.ui.shell;

import com.csme.desktop.api.ApiClient;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/** Surveille l'état de la connexion au serveur central (affiché en permanence dans la barre supérieure). */
public final class ConnectionMonitor {

    public enum State { CHECKING, ONLINE, OFFLINE }

    private final ApiClient client;
    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.CHECKING);
    private ScheduledExecutorService scheduler;

    public ConnectionMonitor(ApiClient client) {
        this.client = client;
    }

    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }

    public synchronized void start() {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "csme-connection-monitor");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::check, 0, 15, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public void checkNow() {
        Thread.ofVirtual().start(this::check);
    }

    private void check() {
        boolean ok = client.isReachable();
        Platform.runLater(() -> state.set(ok ? State.ONLINE : State.OFFLINE));
    }
}
