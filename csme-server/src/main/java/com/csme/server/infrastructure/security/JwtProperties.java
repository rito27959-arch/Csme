package com.csme.server.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres des jetons. Le secret vient de l'environnement ({@code CSME_JWT_SECRET}) et n'a
 * aucune valeur par défaut : le serveur refuse de démarrer sans secret suffisamment long.
 */
@ConfigurationProperties(prefix = "csme.security.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {

    public static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "csme.security.jwt.secret (CSME_JWT_SECRET) doit contenir au moins " + MIN_SECRET_BYTES + " octets");
        }
        issuer = issuer == null || issuer.isBlank() ? "csme-erp" : issuer;
        accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(15) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofHours(12) : refreshTokenTtl;
    }

    @Override
    public String toString() {
        return "JwtProperties[issuer=" + issuer + ", accessTokenTtl=" + accessTokenTtl + ", refreshTokenTtl=" + refreshTokenTtl + "]";
    }
}
