package com.csme.server.application;

import com.csme.server.infrastructure.persistence.RoleRepository;
import com.csme.server.tenancy.TenantContext;
import com.csme.shared.dto.UserDtos.RoleDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Rôles de l'entreprise courante (lecture ; l'édition des permissions arrive en phase 5). */
@Service
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roles;

    public RoleService(RoleRepository roles) {
        this.roles = roles;
    }

    public List<RoleDto> list() {
        return roles.findAllByCompanyIdOrderByNameAsc(TenantContext.requireCompanyId()).stream()
                .map(r -> new RoleDto(r.getId(), r.getName(), r.getDescription(), r.isSystemRole(),
                        Dtos.authorities(r.getPermissions()), roles.countUsers(r.getId())))
                .toList();
    }
}
