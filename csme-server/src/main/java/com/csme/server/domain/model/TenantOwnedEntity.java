package com.csme.server.domain.model;

import com.csme.server.tenancy.TenantContext;
import com.csme.server.tenancy.TenantScope;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.Objects;
import java.util.UUID;

/**
 * Donnée métier appartenant à une entreprise. Première ligne de défense applicative :
 * impossible d'écrire une ligne pour une autre entreprise que celle de la portée courante.
 * La seconde ligne de défense est la politique RLS de PostgreSQL.
 */
@MappedSuperclass
public abstract class TenantOwnedEntity extends BaseEntity {

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    protected TenantOwnedEntity() {
    }

    protected TenantOwnedEntity(UUID companyId) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
    }

    public UUID getCompanyId() {
        return companyId;
    }

    @PrePersist
    @PreUpdate
    protected void guardTenant() {
        TenantScope scope = TenantContext.current()
                .orElseThrow(() -> new IllegalStateException("Écriture sans portée entreprise"));
        if (scope instanceof TenantScope.Company c && !c.companyId().equals(companyId)) {
            throw new IllegalStateException("Écriture refusée : la donnée appartient à une autre entreprise");
        }
    }
}
