package com.csme.server.application;

import com.csme.server.domain.model.Agency;
import com.csme.server.domain.model.AppUser;
import com.csme.server.domain.model.AuditLog;
import com.csme.server.domain.model.Role;
import com.csme.shared.api.PageResponse;
import com.csme.shared.dto.AgencyDtos.AgencyDto;
import com.csme.shared.dto.AuditDtos.AuditLogDto;
import com.csme.shared.dto.AuthDtos.AgencyRef;
import com.csme.shared.dto.UserDtos.RoleRef;
import com.csme.shared.dto.UserDtos.UserDto;
import com.csme.shared.security.Permission;
import java.time.Instant;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;

/** Conversion entités → contrats partagés. Les entités ne sortent jamais de la couche application. */
public final class Dtos {

    private Dtos() {
    }

    public static AgencyDto agency(Agency a) {
        return new AgencyDto(a.getId(), a.getCode(), a.getName(), a.getCity(), a.getPhone(), a.isActive(), a.getCreatedAt());
    }

    public static AgencyRef agencyRef(Agency a) {
        return new AgencyRef(a.getId(), a.getCode(), a.getName());
    }

    public static UserDto user(AppUser u, Instant now) {
        return new UserDto(
                u.getId(), u.getEmail(), u.getFullName(), u.getJobTitle(), u.isActive(), u.isLocked(now),
                u.getRoles().stream().sorted(Comparator.comparing(Role::getName))
                        .map(r -> new RoleRef(r.getId(), r.getName())).toList(),
                u.getAgencies().stream().sorted(Comparator.comparing(Agency::getName)).map(Dtos::agencyRef).toList(),
                u.getDepartment() == null ? null : new com.csme.shared.dto.UserDtos.DepartmentRef(
                        u.getDepartment().getId(), u.getDepartment().getCode(), u.getDepartment().getName()),
                u.getLastLoginAt(), u.getCreatedAt());
    }

    public static AuditLogDto audit(AuditLog a) {
        return new AuditLogDto(a.getId(), a.getOccurredAt(), a.getActorId(), a.getActorEmail(), a.getAction(),
                a.getEntityType(), a.getEntityId(), a.getOutcome(), a.getDetails(), a.getIpAddress());
    }

    public static java.util.Set<String> authorities(java.util.Set<Permission> permissions) {
        return permissions.stream().map(Permission::authority).collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    public static <E, D> PageResponse<D> page(Page<E> page, Function<E, D> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
