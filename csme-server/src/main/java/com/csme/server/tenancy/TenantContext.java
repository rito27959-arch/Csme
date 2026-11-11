package com.csme.server.tenancy;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Portée multi-entreprise du thread courant. Positionnée par {@code TenantContextFilter}
 * à partir du jeton vérifié — jamais à partir d'un paramètre fourni par le client.
 *
 * <p>Elle est appliquée à chaque connexion PostgreSQL par {@link TenantAwareDataSource} au moment
 * où une transaction obtient sa connexion. Changer de portée au milieu d'une transaction n'aurait
 * donc aucun effet sur la connexion déjà ouverte : c'est interdit et vérifié.</p>
 */
public final class TenantContext {

    private static final ThreadLocal<TenantScope> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Optional<TenantScope> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static boolean isSystem() {
        return CURRENT.get() instanceof TenantScope.System;
    }

    /** Entreprise courante, ou exception si le thread n'est pas dans une portée entreprise. */
    public static UUID requireCompanyId() {
        if (CURRENT.get() instanceof TenantScope.Company company) {
            return company.companyId();
        }
        throw new IllegalStateException("Aucune entreprise dans le contexte courant");
    }

    public static Optional<UUID> companyId() {
        return CURRENT.get() instanceof TenantScope.Company company
                ? Optional.of(company.companyId())
                : Optional.empty();
    }

    /** Exécute {@code action} dans la portée donnée, puis restaure la portée précédente. */
    public static <T> T callAs(TenantScope scope, Supplier<T> action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "Changement de portée interdit à l'intérieur d'une transaction : la connexion est déjà configurée");
        }
        TenantScope previous = CURRENT.get();
        CURRENT.set(scope);
        try {
            return action.get();
        } finally {
            restore(previous);
        }
    }

    public static <T> T asSystem(Supplier<T> action) {
        return callAs(new TenantScope.System(), action);
    }

    public static <T> T asCompany(UUID companyId, Supplier<T> action) {
        return callAs(new TenantScope.Company(companyId), action);
    }

    static void set(TenantScope scope) {
        CURRENT.set(scope);
    }

    static void clear() {
        CURRENT.remove();
    }

    private static void restore(TenantScope previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}
