package com.csme.desktop.api;

import com.csme.desktop.session.Session;
import com.csme.shared.api.ApiError;
import com.csme.shared.api.ApiRoutes;
import com.csme.shared.dto.AuthDtos.RefreshRequest;
import com.csme.shared.dto.AuthDtos.TokenResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;

/**
 * Client HTTP de l'API. Joint le jeton d'accès, le renouvelle automatiquement une fois en cas de 401,
 * et traduit toutes les erreurs en {@link ApiException} au message compréhensible.
 * Toutes les méthodes sont bloquantes : elles sont appelées hors du fil JavaFX.
 */
public final class ApiClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final URI baseUrl;
    private final Session session;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String userAgent;
    private final Object refreshLock = new Object();

    public ApiClient(URI baseUrl, Session session, String version) {
        this.baseUrl = baseUrl;
        this.session = session;
        this.userAgent = "CSME-ERP-Desktop/" + version + " (" + System.getProperty("os.name") + ")";
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.mapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public URI baseUrl() {
        return baseUrl;
    }

    // --------------------------------------------------------------------------- verbs

    public <T> T get(String path, TypeReference<T> type) {
        return send("GET", path, null, type, true);
    }

    public <T> T post(String path, Object body, TypeReference<T> type) {
        return send("POST", path, body, type, true);
    }

    public <T> T put(String path, Object body, TypeReference<T> type) {
        return send("PUT", path, body, type, true);
    }

    public <T> T patch(String path, Object body, TypeReference<T> type) {
        return send("PATCH", path, body, type, true);
    }

    public void postNoContent(String path, Object body) {
        send("POST", path, body, null, true);
    }

    /** Appel sans jeton (connexion, déconnexion). */
    public <T> T postAnonymous(String path, Object body, TypeReference<T> type) {
        return send("POST", path, body, type, false);
    }

    /** Vérifie que le serveur répond. Ne lève jamais d'exception. */
    public boolean isReachable() {
        try {
            HttpRequest request = HttpRequest.newBuilder(baseUrl.resolve(ApiRoutes.HEALTH))
                    .timeout(Duration.ofSeconds(5)).header("User-Agent", userAgent).GET().build();
            return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // ------------------------------------------------------------------------ internals

    private <T> T send(String method, String path, Object body, TypeReference<T> type, boolean authenticated) {
        HttpResponse<byte[]> response = execute(method, path, body, authenticated);
        if (response.statusCode() == 401 && authenticated && tryRefresh()) {
            response = execute(method, path, body, true);
        }
        if (response.statusCode() == 401 && authenticated) {
            session.expire("Votre session a expiré. Veuillez vous reconnecter.");
        }
        if (response.statusCode() >= 400) {
            throw toException(response);
        }
        if (type == null || response.body().length == 0) {
            return null;
        }
        try {
            return mapper.readValue(response.body(), type);
        } catch (IOException e) {
            throw new ApiException(response.statusCode(), "BAD_RESPONSE",
                    "Réponse du serveur inattendue. Vérifiez que le client est à jour.", Map.of());
        }
    }

    private HttpResponse<byte[]> execute(String method, String path, Object body, boolean authenticated) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(baseUrl.resolve(path))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .header("User-Agent", userAgent);
            if (authenticated) {
                session.accessToken().ifPresent(token -> builder.header("Authorization", "Bearer " + token));
            }
            if (body != null) {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(body)));
            } else if (method.equals("GET")) {
                builder.GET();
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            return http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (HttpTimeoutException e) {
            throw new ApiException(0, "TIMEOUT", "Le serveur met trop de temps à répondre. Réessayez.", Map.of());
        } catch (ConnectException e) {
            throw new ApiException(0, "OFFLINE", "Serveur injoignable (" + baseUrl.getHost() + "). Vérifiez la connexion.", Map.of());
        } catch (IOException e) {
            throw new ApiException(0, "NETWORK", "Erreur réseau : " + e.getMessage(), Map.of());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "INTERRUPTED", "Opération interrompue.", Map.of());
        }
    }

    /** Renouvelle le jeton d'accès (un seul renouvellement à la fois). */
    private boolean tryRefresh() {
        synchronized (refreshLock) {
            String refresh = session.refreshToken().orElse(null);
            if (refresh == null) {
                return false;
            }
            try {
                HttpResponse<byte[]> response = execute("POST", ApiRoutes.AUTH_REFRESH, new RefreshRequest(refresh), false);
                if (response.statusCode() != 200) {
                    return false;
                }
                session.updateTokens(mapper.readValue(response.body(), TokenResponse.class));
                return true;
            } catch (IOException | ApiException e) {
                return false;
            }
        }
    }

    private ApiException toException(HttpResponse<byte[]> response) {
        int status = response.statusCode();
        try {
            ApiError error = mapper.readValue(response.body(), ApiError.class);
            if (error != null && error.message() != null) {
                return new ApiException(status, error.code(), error.message(), error.fieldErrors());
            }
        } catch (IOException ignored) {
            // corps non JSON : message générique ci-dessous
        }
        String message = switch (status) {
            case 403 -> "Vous n'avez pas l'autorisation d'effectuer cette opération.";
            case 404 -> "Élément introuvable.";
            case 401 -> "Session expirée. Veuillez vous reconnecter.";
            default -> "Le serveur a renvoyé une erreur (" + status + ").";
        };
        return new ApiException(status, "HTTP_" + status, message, Map.of());
    }
}
