package com.csme.server.application;

import com.csme.server.domain.model.AuditLog;
import com.csme.server.domain.model.AuditLog.Outcome;
import com.csme.server.infrastructure.persistence.AuditLogRepository;
import com.csme.server.infrastructure.security.CurrentActor;
import com.csme.server.infrastructure.web.RequestInfo;
import com.csme.server.tenancy.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Journal d'audit : qui, quoi, sur quel élément, quand, depuis quelle entreprise, valeurs avant/après, résultat.
 * L'écriture se fait dans la transaction de l'opération : pas d'opération sans trace, pas de trace sans opération.
 */
@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /** Opération réussie d'un utilisateur authentifié, dans sa portée courante. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void success(String action, String entityType, Object entityId, Map<String, ?> before, Map<String, ?> after) {
        record(action, entityType, entityId, Outcome.SUCCESS, before, after, null, null);
    }

    /** Opération plateforme (portée système) concernant une entreprise donnée. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void successForCompany(UUID companyId, String action, String entityType, Object entityId, Map<String, ?> after) {
        record(action, entityType, entityId, Outcome.SUCCESS, null, after, null, companyId);
    }

    /** Événement d'authentification : l'acteur est fourni explicitement (pas encore de jeton). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void authentication(String action, Outcome outcome, UUID companyId, UUID userId, String email, String note) {
        RequestInfo request = RequestInfo.current();
        repository.save(new AuditLog(companyId, userId, email, action, "User",
                userId == null ? null : userId.toString(), outcome, toJson(null, null, note), request.ipAddress()));
    }

    /** Accès refusé par le contrôle des permissions (journalisé dans sa propre transaction). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void denied(String endpoint, String requiredPermission) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("endpoint", endpoint);
        details.put("required", requiredPermission);
        record("ACCESS_DENIED", null, null, Outcome.DENIED, null, details, null, null);
    }

    private void record(String action, String entityType, Object entityId, Outcome outcome,
                        Map<String, ?> before, Map<String, ?> after, String note, UUID explicitCompany) {
        CurrentActor actor = CurrentActor.current().orElse(null);
        UUID companyId = explicitCompany != null ? explicitCompany : TenantContext.companyId().orElse(null);
        RequestInfo request = RequestInfo.current();
        repository.save(new AuditLog(
                companyId,
                actor == null ? null : actor.userId(),
                actor == null ? null : actor.email(),
                action,
                entityType,
                entityId == null ? null : entityId.toString(),
                outcome,
                toJson(before, after, note),
                request.ipAddress()));
    }

    private String toJson(Map<String, ?> before, Map<String, ?> after, String note) {
        if (before == null && after == null && note == null) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        if (before != null) {
            payload.put("before", before);
        }
        if (after != null) {
            payload.put("after", after);
        }
        if (note != null) {
            payload.put("note", note);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{\"note\":\"détails non sérialisables\"}";
        }
    }
}
