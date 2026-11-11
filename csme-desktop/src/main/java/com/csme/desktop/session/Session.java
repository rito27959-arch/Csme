package com.csme.desktop.session;

import com.csme.shared.dto.AuthDtos.CurrentUser;
import com.csme.shared.dto.AuthDtos.TokenResponse;
import com.csme.shared.security.Action;
import com.csme.shared.security.ErpModule;
import com.csme.shared.security.Permission;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * Session de l'utilisateur connecté. Les jetons restent en mémoire uniquement : rien n'est écrit sur disque.
 * Les permissions servent à adapter l'interface ; le serveur les vérifie de toute façon.
 */
public final class Session {

    private final ReadOnlyObjectWrapper<CurrentUser> user = new ReadOnlyObjectWrapper<>();
    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile Consumer<String> onExpired = reason -> { };

    public synchronized void open(TokenResponse tokens) {
        this.accessToken = tokens.accessToken();
        this.refreshToken = tokens.refreshToken();
        CurrentUser current = tokens.user();
        runOnFx(() -> user.set(current));
    }

    public synchronized void updateTokens(TokenResponse tokens) {
        this.accessToken = tokens.accessToken();
        this.refreshToken = tokens.refreshToken();
        CurrentUser current = tokens.user();
        runOnFx(() -> user.set(current));
    }

    public synchronized void clear() {
        accessToken = null;
        refreshToken = null;
        runOnFx(() -> user.set(null));
    }

    /** Appelé quand le serveur refuse définitivement la session (jeton révoqué ou expiré). */
    public void expire(String reason) {
        clear();
        runOnFx(() -> onExpired.accept(reason));
    }

    public void setOnExpired(Consumer<String> handler) {
        this.onExpired = handler == null ? r -> { } : handler;
    }

    public Optional<String> accessToken() {
        return Optional.ofNullable(accessToken);
    }

    public Optional<String> refreshToken() {
        return Optional.ofNullable(refreshToken);
    }

    public ReadOnlyObjectProperty<CurrentUser> userProperty() {
        return user.getReadOnlyProperty();
    }

    public CurrentUser user() {
        return user.get();
    }

    public boolean isSuperAdmin() {
        CurrentUser u = user.get();
        return u != null && u.superAdmin();
    }

    public boolean can(ErpModule module, Action action) {
        CurrentUser u = user.get();
        return u != null && u.can(Permission.of(module, action).authority());
    }

    private static void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
