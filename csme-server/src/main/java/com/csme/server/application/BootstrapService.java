package com.csme.server.application;

import com.csme.server.domain.model.AppUser;
import com.csme.server.domain.rules.Checks;
import com.csme.server.domain.rules.PasswordPolicy;
import com.csme.server.infrastructure.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Création du premier super administrateur au démarrage, à partir des variables
 * {@code CSME_BOOTSTRAP_ADMIN_EMAIL} / {@code CSME_BOOTSTRAP_ADMIN_PASSWORD}. Sans effet si un
 * super administrateur existe déjà. Aucun compte par défaut n'est codé en dur.
 */
@Component
public class BootstrapService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapService.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SystemTransactions system;
    private final String email;
    private final String password;
    private final String fullName;

    public BootstrapService(UserRepository users, PasswordEncoder passwordEncoder, SystemTransactions system,
                            @Value("${csme.bootstrap.super-admin-email:}") String email,
                            @Value("${csme.bootstrap.super-admin-password:}") String password,
                            @Value("${csme.bootstrap.super-admin-name:Super administrateur}") String fullName) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.system = system;
        this.email = Checks.normalizeEmail(email);
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean exists = system.call(users::existsBySuperAdminTrue);
        if (exists) {
            return;
        }
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("Aucun super administrateur. Définissez CSME_BOOTSTRAP_ADMIN_EMAIL et CSME_BOOTSTRAP_ADMIN_PASSWORD puis redémarrez.");
            return;
        }
        PasswordPolicy.violation(password, email).ifPresent(v -> {
            throw new IllegalStateException("Mot de passe du super administrateur refusé : " + v);
        });
        system.run(() -> users.save(AppUser.superAdmin(email, fullName, passwordEncoder.encode(password))));
        log.info("Super administrateur créé : {}", email);
    }
}
