package com.csme.desktop.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Configuration du poste client. L'adresse du serveur est lue, dans l'ordre :
 * <ol>
 *   <li>propriété système {@code -Dcsme.server.url=…}</li>
 *   <li>variable d'environnement {@code CSME_SERVER_URL}</li>
 *   <li>fichier {@code %APPDATA%\CSME ERP\config.properties} (clé {@code server.url})</li>
 *   <li>{@code http://localhost:8080} (développement)</li>
 * </ol>
 * HTTPS est exigé pour toute adresse autre que la machine locale.
 */
public record AppConfig(URI serverUrl, String version) {

    public static final String DEFAULT_URL = "http://localhost:8080";

    public static AppConfig load() {
        String url = firstNonBlank(
                System.getProperty("csme.server.url"),
                System.getenv("CSME_SERVER_URL"),
                fromFile());
        URI uri = URI.create(url == null ? DEFAULT_URL : url.strip().replaceAll("/+$", ""));
        requireSecure(uri);
        String version = AppConfig.class.getPackage().getImplementationVersion();
        return new AppConfig(uri, version == null ? "dev" : version);
    }

    static void requireSecure(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean local = host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1") || host.equals("[::1]");
        if (!scheme.equals("https") && !(scheme.equals("http") && local)) {
            throw new IllegalStateException("L'adresse du serveur doit utiliser HTTPS : " + uri);
        }
    }

    public static Path configDirectory() {
        String appData = System.getenv("APPDATA");
        Path base = appData != null ? Path.of(appData) : Path.of(System.getProperty("user.home"), ".config");
        return base.resolve("CSME ERP");
    }

    private static String fromFile() {
        Path file = configDirectory().resolve("config.properties");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
            return properties.getProperty("server.url");
        } catch (IOException e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
