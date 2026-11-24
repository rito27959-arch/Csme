package com.csme.server.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Département d'une agence. La base vérifie que l'agence appartient à la même entreprise. */
@Entity
@Table(name = "departments")
public class Department extends TenantOwnedEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    protected Department() {
    }

    public Department(UUID companyId, Agency agency, String code, String name, String description) {
        super(companyId);
        update(agency, code, name, description);
    }

    public final void update(Agency agency, String code, String name, String description) {
        this.agency = agency;
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public Agency getAgency() {
        return agency;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
