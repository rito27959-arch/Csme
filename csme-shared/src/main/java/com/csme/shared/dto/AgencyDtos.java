package com.csme.shared.dto;

import java.time.Instant;
import java.util.UUID;

/** Agences d'une entreprise. */
public final class AgencyDtos {

    private AgencyDtos() {
    }

    public record AgencyDto(UUID id, String code, String name, String city, String phone, boolean active, Instant createdAt) {
    }

    public record SaveAgencyRequest(String code, String name, String city, String phone) {
    }
}
