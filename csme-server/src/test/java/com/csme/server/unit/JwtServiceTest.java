package com.csme.server.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.csme.server.infrastructure.security.JwtProperties;
import com.csme.server.infrastructure.security.JwtService;
import com.csme.server.infrastructure.security.TokenHasher;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdefghijklmnop";

    private final JwtService service = new JwtService(
            new JwtProperties(SECRET, "csme-erp", Duration.ofMinutes(5), Duration.ofHours(1)));

    @Test
    void issuedTokenCarriesTenantAndPermissions() {
        UUID user = UUID.randomUUID();
        UUID company = UUID.randomUUID();
        String token = service.issueAccessToken(user, company, "a@b.cm", "A B", false, List.of("USERS:VIEW")).value();

        Jwt jwt = service.decoder().decode(token);

        assertThat(jwt.getSubject()).isEqualTo(user.toString());
        assertThat(jwt.getClaimAsString("cid")).isEqualTo(company.toString());
        assertThat(jwt.getClaimAsStringList(JwtService.CLAIM_PERMISSIONS)).containsExactly("USERS:VIEW");
        assertThat(jwt.getClaimAsBoolean(JwtService.CLAIM_SUPER_ADMIN)).isFalse();
    }

    @Test
    void rejectsTamperedToken() {
        String token = service.issueAccessToken(UUID.randomUUID(), UUID.randomUUID(), "a@b.cm", "A", false, List.of()).value();
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "x." + parts[2];
        assertThatThrownBy(() -> service.decoder().decode(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService other = new JwtService(new JwtProperties("another-secret-0123456789abcdefghijklmnopq", "csme-erp",
                Duration.ofMinutes(5), Duration.ofHours(1)));
        String foreign = other.issueAccessToken(UUID.randomUUID(), UUID.randomUUID(), "a@b.cm", "A", true, List.of()).value();
        assertThatThrownBy(() -> service.decoder().decode(foreign)).isInstanceOf(JwtException.class);
    }

    @Test
    void refusesShortSecret() {
        assertThatThrownBy(() -> new JwtProperties("too-short", null, null, null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void opaqueTokensAreUniqueAndHashedDeterministically() {
        String a = TokenHasher.newOpaqueToken();
        String b = TokenHasher.newOpaqueToken();
        assertThat(a).isNotEqualTo(b);
        assertThat(TokenHasher.sha256Hex(a)).hasSize(64).isEqualTo(TokenHasher.sha256Hex(a));
    }
}
