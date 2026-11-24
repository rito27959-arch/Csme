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

    /** Informations générales de l'entreprise, consultées et modifiées par l'entreprise elle-même. */
    public record CompanyProfileDto(
            UUID id,
            String code,
            String name,
            String legalName,
            String currency,
            String country,
            String address,
            String city,
            String phone,
            String email,
            String website,
            String taxId,
            String tradeRegister) {
    }

    /** Le code, la devise et le pays ne sont pas modifiables : ils fondent les données déjà enregistrées. */
    public record UpdateCompanyProfileRequest(
            String name,
            String legalName,
            String address,
            String city,
            String phone,
            String email,
            String website,
            String taxId,
            String tradeRegister) {
    }
}
