package com.csme.server.application;

import com.csme.server.infrastructure.persistence.AuditLogRepository;
import com.csme.server.tenancy.TenantContext;
import com.csme.shared.api.PageResponse;
import com.csme.shared.dto.AuditDtos.AuditLogDto;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultation du journal d'audit. */
@Service
public class AuditQueryService {

    private final AuditLogRepository repository;
    private final SystemTransactions system;

    public AuditQueryService(AuditLogRepository repository, SystemTransactions system) {
        this.repository = repository;
        this.system = system;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDto> forCompany(String action, int page, int size) {
        UUID companyId = TenantContext.requireCompanyId();
        PageRequest pageable = pageable(page, size);
        String a = normalize(action);
        return Dtos.page(a == null
                ? repository.findByCompanyId(companyId, pageable)
                : repository.findByCompanyIdAndAction(companyId, a, pageable), Dtos::audit);
    }

    public PageResponse<AuditLogDto> forPlatform(int page, int size) {
        return system.call(() -> Dtos.page(repository.findAll(pageable(page, size)), Dtos::audit));
    }

    private static PageRequest pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by("occurredAt").descending());
    }

    private static String normalize(String action) {
        return action == null || action.isBlank() ? null : action.strip().toUpperCase(Locale.ROOT);
    }
}
