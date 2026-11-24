package com.csme.server.infrastructure.persistence;

import com.csme.server.domain.model.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findAllByCompanyIdOrderByNameAsc(UUID companyId);

    List<Role> findAllByIdInAndCompanyId(Collection<UUID> ids, UUID companyId);

    Optional<Role> findByCompanyIdAndName(UUID companyId, String name);

    Optional<Role> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(UUID companyId, String name);

    long countByCompanyId(UUID companyId);

    @Query("select count(u) from AppUser u join u.roles r where r.id = :roleId")
    long countUsers(@Param("roleId") UUID roleId);
}
