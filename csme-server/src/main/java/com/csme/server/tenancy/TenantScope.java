package com.csme.server.tenancy;

import java.util.Objects;
import java.util.UUID;

/**
 * Portée d'exécution des accès aux données.
 * <ul>
 *   <li>{@link Company} : requête d'un utilisateur d'entreprise — la base ne montre que ses lignes.</li>
 *   <li>{@link System} : opération plateforme explicitement élevée (connexion, super administrateur).</li>
 * </ul>
 * L'absence de portée ne donne accès à aucune ligne métier.
 */
public sealed interface TenantScope permits TenantScope.Company, TenantScope.System {

    record Company(UUID companyId) implements TenantScope {
        public Company {
            Objects.requireNonNull(companyId, "companyId");
        }
    }

    record System() implements TenantScope {
    }
}
