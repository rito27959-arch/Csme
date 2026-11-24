package com.csme.shared.dto;

import java.time.Instant;
import java.util.UUID;

/** Départements : chaque département est rattaché à une agence de l'entreprise. */
public final class DepartmentDtos {

    private DepartmentDtos() {
    }

    public record DepartmentDto(
            UUID id,
            String code,
            String name,
            String description,
            AuthDtos.AgencyRef agency,
            boolean active,
            long userCount,
            Instant createdAt) {
    }

    public record SaveDepartmentRequest(String code, String name, String description, UUID agencyId) {
    }
}
