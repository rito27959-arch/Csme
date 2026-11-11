package com.csme.shared.dto;

import java.time.Instant;
import java.util.UUID;

/** Journal d'audit (lecture seule). */
public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditLogDto(
            long id,
            Instant occurredAt,
            UUID actorId,
            String actorEmail,
            String action,
            String entityType,
            String entityId,
            String outcome,
            String details,
            String ipAddress) {
    }
}
