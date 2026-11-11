package com.csme.server.infrastructure.persistence;

import com.csme.server.domain.model.AppUser;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsBySuperAdminTrue();

    Optional<AppUser> findByIdAndCompanyId(UUID id, UUID companyId);

    @Query("""
            select u from AppUser u
            where u.companyId = :companyId
              and (lower(u.fullName) like :pattern or u.email like :pattern)
            """)
    /** {@code pattern} est un motif LIKE en minuscules ; « % » pour tout lister. */
    Page<AppUser> search(@Param("companyId") UUID companyId, @Param("pattern") String pattern, Pageable pageable);

    long countByCompanyIdAndActive(UUID companyId, boolean active);

    long countByCompanyIdAndLockedUntilAfter(UUID companyId, Instant now);

    long countByCompanyId(UUID companyId);

    long countBySuperAdminFalse();
}
