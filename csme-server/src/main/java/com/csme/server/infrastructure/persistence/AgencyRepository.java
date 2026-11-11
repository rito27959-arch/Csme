package com.csme.server.infrastructure.persistence;

import com.csme.server.domain.model.Agency;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Toutes les méthodes filtrent explicitement par entreprise, en plus de la politique RLS. */
public interface AgencyRepository extends JpaRepository<Agency, UUID> {

    List<Agency> findAllByCompanyIdOrderByNameAsc(UUID companyId);

    Optional<Agency> findByIdAndCompanyId(UUID id, UUID companyId);

    List<Agency> findAllByIdInAndCompanyId(Collection<UUID> ids, UUID companyId);

    boolean existsByCompanyIdAndCodeIgnoreCase(UUID companyId, String code);

    long countByCompanyId(UUID companyId);
}
