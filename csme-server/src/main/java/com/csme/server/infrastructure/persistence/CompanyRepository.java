package com.csme.server.infrastructure.persistence;

import com.csme.server.domain.model.Company;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByCode(String code);

    long countByActiveTrue();
}
