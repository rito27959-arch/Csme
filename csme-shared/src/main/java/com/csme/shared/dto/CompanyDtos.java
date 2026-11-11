package com.csme.shared.dto;

import java.time.Instant;
import java.util.UUID;

/** Entreprises (gérées par le super administrateur de la plateforme). */
public final class CompanyDtos {

    private CompanyDtos() {
    }

    public record CompanyDto(
            UUID id,
            String code,
            String name,
            String legalName,
            String currency,
            String country,
            boolean active,
            long userCount,
            Instant createdAt) {
    }

    /**
     * Création d'une entreprise et de son premier compte (rôle « Directeur général »).
     * La devise et le pays sont fournis explicitement : aucune valeur n'est imposée.
     */
    public record CreateCompanyRequest(
            String code,
            String name,
            String legalName,
            String currency,
            String country,
            String adminFullName,
            String adminEmail,
            String adminPassword) {
        @Override
        public String toString() {
            return "CreateCompanyRequest[code=" + code + ", name=" + name + ", adminEmail=" + adminEmail + "]";
        }
    }

    public record SetActiveRequest(boolean active) {
    }
}
