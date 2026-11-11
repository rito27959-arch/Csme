package com.csme.server.application;

import com.csme.server.domain.error.AuthenticationFailedException;
import com.csme.server.domain.error.BusinessRuleException;
import com.csme.server.domain.model.Agency;
import com.csme.server.domain.model.AppUser;
import com.csme.server.domain.model.AuditLog.Outcome;
import com.csme.server.domain.model.Company;
import com.csme.server.domain.model.RefreshToken;
import com.csme.server.domain.rules.Checks;
import com.csme.server.infrastructure.persistence.CompanyRepository;
import com.csme.server.infrastructure.persistence.RefreshTokenRepository;
import com.csme.server.infrastructure.persistence.UserRepository;
import com.csme.server.infrastructure.security.CurrentActor;
import com.csme.server.infrastructure.security.JwtService;
import com.csme.server.infrastructure.security.TokenHasher;
import com.csme.server.infrastructure.web.RequestInfo;
import com.csme.shared.dto.AuthDtos.ChangePasswordRequest;
import com.csme.shared.dto.AuthDtos.CurrentUser;
import com.csme.shared.dto.AuthDtos.LoginRequest;
import com.csme.shared.dto.AuthDtos.TokenResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Connexion, rotation des jetons, déconnexion, changement de mot de passe.
 *
 * <p>Ces opérations précèdent l'identification de l'entreprise : elles s'exécutent en portée système.
 * Les échecs sont d'abord enregistrés (compteur, verrouillage, audit) puis signalés après validation de
 * la transaction, afin qu'une exception ne fasse pas disparaître la trace de l'échec.</p>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final CompanyRepository companies;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService audit;
    private final SystemTransactions system;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(UserRepository users, CompanyRepository companies, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder, JwtService jwtService, AuditService audit,
                       SystemTransactions system, Clock clock) {
        this.users = users;
        this.companies = companies;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.audit = audit;
        this.system = system;
        this.clock = clock;
        // Égalise le temps de réponse entre « compte inconnu » et « mauvais mot de passe ».
        this.dummyHash = passwordEncoder.encode(TokenHasher.newOpaqueToken());
    }

    private sealed interface Attempt permits Ok, Fail {
    }

    private record Ok(TokenResponse tokens) implements Attempt {
    }

    private record Fail(String code) implements Attempt {
    }

    // ---------------------------------------------------------------------------------- login

    public TokenResponse login(LoginRequest request) {
        Checks.start()
                .required("email", request == null ? null : request.email(), "L'adresse électronique")
                .required("password", request == null ? null : request.password(), "Le mot de passe")
                .throwIfInvalid();
        String email = Checks.normalizeEmail(request.email());
        Attempt outcome = system.call(() -> attemptLogin(email, request.password()));
        return unwrap(outcome);
    }

    private Attempt attemptLogin(String email, String password) {
        Instant now = clock.instant();
        Optional<AppUser> found = users.findByEmail(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(password, dummyHash);
            audit.authentication("LOGIN_FAILURE", Outcome.FAILURE, null, null, email, "compte inconnu");
            return new Fail("INVALID_CREDENTIALS");
        }
        AppUser user = found.get();
        if (user.isLocked(now)) {
            audit.authentication("LOGIN_FAILURE", Outcome.FAILURE, user.getCompanyId(), user.getId(), email, "compte verrouillé");
            return new Fail("ACCOUNT_LOCKED");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            boolean lockedNow = user.registerFailedLogin(now);
            audit.authentication("LOGIN_FAILURE", Outcome.FAILURE, user.getCompanyId(), user.getId(), email,
                    lockedNow ? "mot de passe incorrect — compte verrouillé" : "mot de passe incorrect");
            return new Fail("INVALID_CREDENTIALS");
        }
        Optional<String> inactive = inactivityReason(user);
        if (inactive.isPresent()) {
            audit.authentication("LOGIN_FAILURE", Outcome.FAILURE, user.getCompanyId(), user.getId(), email, inactive.get());
            return new Fail("ACCOUNT_DISABLED");
        }
        user.registerSuccessfulLogin(now);
        TokenResponse tokens = issueTokens(user, UUID.randomUUID(), now).response();
        audit.authentication("LOGIN_SUCCESS", Outcome.SUCCESS, user.getCompanyId(), user.getId(), email, null);
        return new Ok(tokens);
    }

    // -------------------------------------------------------------------------------- refresh

    public TokenResponse refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthenticationFailedException("INVALID_REFRESH_TOKEN");
        }
        String hash = TokenHasher.sha256Hex(rawToken);
        return unwrap(system.call(() -> attemptRefresh(hash)));
    }

    private Attempt attemptRefresh(String hash) {
        Instant now = clock.instant();
        Optional<RefreshToken> found = refreshTokens.findByTokenHash(hash);
        if (found.isEmpty()) {
            return new Fail("INVALID_REFRESH_TOKEN");
        }
        RefreshToken token = found.get();
        AppUser user = users.findById(token.getUserId()).orElse(null);
        if (token.getRevokedAt() != null) {
            // Un jeton déjà remplacé est rejoué : vol probable. Toute la famille est révoquée.
            refreshTokens.revokeFamily(token.getFamilyId(), now);
            audit.authentication("REFRESH_TOKEN_REUSE", Outcome.DENIED,
                    user == null ? null : user.getCompanyId(), token.getUserId(),
                    user == null ? null : user.getEmail(), "famille de jetons révoquée");
            return new Fail("INVALID_REFRESH_TOKEN");
        }
        if (!token.isActive(now) || user == null) {
            return new Fail("INVALID_REFRESH_TOKEN");
        }
        Optional<String> inactive = inactivityReason(user);
        if (inactive.isPresent()) {
            refreshTokens.revokeFamily(token.getFamilyId(), now);
            audit.authentication("REFRESH_DENIED", Outcome.DENIED, user.getCompanyId(), user.getId(), user.getEmail(), inactive.get());
            return new Fail("ACCOUNT_DISABLED");
        }
        Issued issued = issueTokens(user, token.getFamilyId(), now);
        token.revoke(now, issued.refreshTokenId());
        return new Ok(issued.response());
    }

    // --------------------------------------------------------------------------------- logout

    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String hash = TokenHasher.sha256Hex(rawToken);
        system.run(() -> refreshTokens.findByTokenHash(hash).ifPresent(token -> {
            refreshTokens.revokeFamily(token.getFamilyId(), clock.instant());
            users.findById(token.getUserId()).ifPresent(u ->
                    audit.authentication("LOGOUT", Outcome.SUCCESS, u.getCompanyId(), u.getId(), u.getEmail(), null));
        }));
    }

    // ----------------------------------------------------------------------- profil / password

    public CurrentUser me() {
        CurrentActor actor = CurrentActor.require();
        return system.call(() -> users.findById(actor.userId())
                .map(this::currentUser)
                .orElseThrow(() -> new AuthenticationFailedException("UNKNOWN_USER")));
    }

    public void changePassword(ChangePasswordRequest request) {
        CurrentActor actor = CurrentActor.require();
        Checks.start()
                .required("currentPassword", request == null ? null : request.currentPassword(), "Le mot de passe actuel")
                .throwIfInvalid();
        Attempt outcome = system.call(() -> {
            AppUser user = users.findById(actor.userId())
                    .orElseThrow(() -> new AuthenticationFailedException("UNKNOWN_USER"));
            if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                audit.authentication("PASSWORD_CHANGE", Outcome.FAILURE, user.getCompanyId(), user.getId(), user.getEmail(),
                        "mot de passe actuel incorrect");
                return new Fail("WRONG_CURRENT_PASSWORD");
            }
            Checks.start().password("newPassword", request.newPassword(), user.getEmail())
                    .rule(!request.currentPassword().equals(request.newPassword()), "newPassword",
                            "Le nouveau mot de passe doit être différent de l'actuel.")
                    .throwIfInvalid();
            user.changePassword(passwordEncoder.encode(request.newPassword()));
            refreshTokens.revokeAllForUser(user.getId(), clock.instant());
            audit.authentication("PASSWORD_CHANGE", Outcome.SUCCESS, user.getCompanyId(), user.getId(), user.getEmail(),
                    "toutes les sessions ont été fermées");
            return new Ok(null);
        });
        if (outcome instanceof Fail) {
            throw new BusinessRuleException("WRONG_CURRENT_PASSWORD", "Le mot de passe actuel est incorrect.");
        }
    }

    // -------------------------------------------------------------------------------- helpers

    private Optional<String> inactivityReason(AppUser user) {
        if (!user.isActive()) {
            return Optional.of("compte désactivé");
        }
        if (user.getCompanyId() != null) {
            Company company = companies.findById(user.getCompanyId()).orElse(null);
            if (company == null || !company.isActive()) {
                return Optional.of("entreprise désactivée");
            }
        }
        return Optional.empty();
    }

    private record Issued(TokenResponse response, UUID refreshTokenId) {
    }

    private Issued issueTokens(AppUser user, UUID familyId, Instant now) {
        CurrentUser current = currentUser(user);
        JwtService.IssuedToken access = jwtService.issueAccessToken(
                user.getId(), user.getCompanyId(), user.getEmail(), user.getFullName(),
                user.isSuperAdmin(), current.permissions());
        String refresh = TokenHasher.newOpaqueToken();
        RequestInfo request = RequestInfo.current();
        RefreshToken stored = refreshTokens.save(new RefreshToken(user.getId(), user.getCompanyId(), familyId,
                TokenHasher.sha256Hex(refresh), now.plus(jwtService.refreshTokenTtl()),
                request.userAgent(), request.ipAddress()));
        return new Issued(
                new TokenResponse(access.value(), refresh, "Bearer", jwtService.accessTokenTtlSeconds(), current),
                stored.getId());
    }

    private CurrentUser currentUser(AppUser user) {
        String companyName = user.getCompanyId() == null ? "Plateforme CSME"
                : companies.findById(user.getCompanyId()).map(Company::getName).orElse("");
        return new CurrentUser(
                user.getId(), user.getEmail(), user.getFullName(), user.getJobTitle(), user.getCompanyId(), companyName,
                user.isSuperAdmin(), Dtos.authorities(user.effectivePermissions()),
                user.getAgencies().stream().filter(Agency::isActive)
                        .sorted(Comparator.comparing(Agency::getName)).map(Dtos::agencyRef).toList(),
                user.getLastLoginAt());
    }

    private static TokenResponse unwrap(Attempt outcome) {
        return switch (outcome) {
            case Ok ok -> ok.tokens();
            case Fail fail -> {
                log.debug("Authentification refusée : {}", fail.code());
                throw new AuthenticationFailedException(fail.code());
            }
        };
    }
}
