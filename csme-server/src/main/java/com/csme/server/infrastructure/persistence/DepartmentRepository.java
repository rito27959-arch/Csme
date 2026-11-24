package com.csme.server.infrastructure.persistence;

import com.csme.server.domain.model.Department;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    List<Department> findAllByCompanyIdOrderByNameAsc(UUID companyId);

    Optional<Department> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndCodeIgnoreCase(UUID companyId, String code);

    @Query("select count(u) from AppUser u where u.department.id = :departmentId")
    long countUsers(@Param("departmentId") UUID departmentId);
}
