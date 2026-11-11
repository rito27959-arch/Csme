package com.csme.server.infrastructure.persistence;

import com.csme.server.domain.model.AuditLog;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByCompanyId(UUID companyId, Pageable pageable);

    Page<AuditLog> findByCompanyIdAndAction(UUID companyId, String action, Pageable pageable);

    List<AuditLog> findTop10ByCompanyIdOrderByOccurredAtDesc(UUID companyId);

    List<AuditLog> findTop10ByOrderByOccurredAtDesc();

    long countByCompanyIdAndActionAndOccurredAtGreaterThanEqual(UUID companyId, String action, Instant from);

    long countByActionAndOccurredAtGreaterThanEqual(String action, Instant from);

    @Query("""
            select a.occurredAt from AuditLog a
            where a.companyId = :companyId and a.action = :action and a.occurredAt >= :from
            """)
    List<Instant> occurrencesForCompany(@Param("companyId") UUID companyId, @Param("action") String action, @Param("from") Instant from);

    @Query("""
            select a.occurredAt from AuditLog a
            where a.action = :action and a.occurredAt >= :from
            """)
    List<Instant> occurrencesForPlatform(@Param("action") String action, @Param("from") Instant from);
}
