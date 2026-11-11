package com.csme.server.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Entrée du journal d'audit. Immuable : la base n'accorde ni UPDATE ni DELETE à l'API. */
@Entity
@Immutable
@Table(name = "audit_logs")
public class AuditLog {

    public enum Outcome { SUCCESS, FAILURE, DENIED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", updatable = false)
    private UUID companyId;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "actor_email", length = 254, updatable = false)
    private String actorEmail;

    @Column(nullable = false, length = 64, updatable = false)
    private String action;

    @Column(name = "entity_type", length = 64, updatable = false)
    private String entityType;

    @Column(name = "entity_id", length = 64, updatable = false)
    private String entityId;

    @Column(nullable = false, length = 16, updatable = false)
    private String outcome;

    @Column(columnDefinition = "text", updatable = false)
    private String details;

    @Column(name = "ip_address", length = 64, updatable = false)
    private String ipAddress;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    protected AuditLog() {
    }

    public AuditLog(UUID companyId, UUID actorId, String actorEmail, String action, String entityType,
                    String entityId, Outcome outcome, String details, String ipAddress) {
        this.companyId = companyId;
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.outcome = outcome.name();
        this.details = details;
        this.ipAddress = ipAddress;
    }

    public Long getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getDetails() {
        return details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
