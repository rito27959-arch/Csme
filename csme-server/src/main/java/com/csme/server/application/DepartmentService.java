package com.csme.server.application;

import com.csme.server.domain.error.ConflictException;
import com.csme.server.domain.error.NotFoundException;
import com.csme.server.domain.error.ValidationException;
import com.csme.server.domain.model.Agency;
import com.csme.server.domain.model.Department;
import com.csme.server.domain.rules.Checks;
import com.csme.server.infrastructure.persistence.AgencyRepository;
import com.csme.server.infrastructure.persistence.DepartmentRepository;
import com.csme.server.tenancy.TenantContext;
import com.csme.shared.dto.DepartmentDtos.DepartmentDto;
import com.csme.shared.dto.DepartmentDtos.SaveDepartmentRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Départements de l'entreprise courante, chacun rattaché à une de ses agences. */
@Service
@Transactional
public class DepartmentService {

    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{0,31}$");

    private final DepartmentRepository departments;
    private final AgencyRepository agencies;
    private final AuditService audit;

    public DepartmentService(DepartmentRepository departments, AgencyRepository agencies, AuditService audit) {
        this.departments = departments;
        this.agencies = agencies;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> list() {
        return departments.findAllByCompanyIdOrderByNameAsc(TenantContext.requireCompanyId()).stream()
                .map(this::toDto).toList();
    }

    public DepartmentDto create(SaveDepartmentRequest request) {
        UUID companyId = TenantContext.requireCompanyId();
        String code = validate(request);
        if (departments.existsByCompanyIdAndCodeIgnoreCase(companyId, code)) {
            throw new ConflictException("Un département utilise déjà le code " + code + ".");
        }
        Agency agency = agency(companyId, request.agencyId());
        Department department = departments.save(new Department(companyId, agency, code, request.name().strip(),
                Checks.clean(request.description())));
        audit.success("DEPARTMENT_CREATED", "Department", department.getId(), null, snapshot(department));
        return toDto(department);
    }

    public DepartmentDto update(UUID id, SaveDepartmentRequest request) {
        UUID companyId = TenantContext.requireCompanyId();
        Department department = load(id, companyId);
        String code = validate(request);
        if (!department.getCode().equalsIgnoreCase(code) && departments.existsByCompanyIdAndCodeIgnoreCase(companyId, code)) {
            throw new ConflictException("Un département utilise déjà le code " + code + ".");
        }
        Map<String, Object> before = snapshot(department);
        department.update(agency(companyId, request.agencyId()), code, request.name().strip(),
                Checks.clean(request.description()));
        audit.success("DEPARTMENT_UPDATED", "Department", department.getId(), before, snapshot(department));
        return toDto(department);
    }

    public DepartmentDto setActive(UUID id, boolean active) {
        Department department = load(id, TenantContext.requireCompanyId());
        Map<String, Object> before = snapshot(department);
        department.setActive(active);
        audit.success(active ? "DEPARTMENT_ACTIVATED" : "DEPARTMENT_DEACTIVATED", "Department", department.getId(),
                before, snapshot(department));
        return toDto(department);
    }

    private Department load(UUID id, UUID companyId) {
        return departments.findByIdAndCompanyId(id, companyId).orElseThrow(() -> new NotFoundException("Département"));
    }

    private Agency agency(UUID companyId, UUID agencyId) {
        if (agencyId == null) {
            throw new ValidationException(Map.of("agencyId", "Choisissez l'agence du département."));
        }
        return agencies.findByIdAndCompanyId(agencyId, companyId)
                .orElseThrow(() -> new ValidationException(Map.of("agencyId", "Cette agence n'existe pas.")));
    }

    private static String validate(SaveDepartmentRequest request) {
        String code = request == null || Checks.clean(request.code()) == null ? null
                : request.code().strip().toUpperCase(Locale.ROOT);
        Checks.start()
                .required("code", code, "Le code")
                .matches("code", code, CODE, "Code : jusqu'à 32 caractères, lettres majuscules, chiffres, « - » ou « _ ».")
                .required("name", request == null ? null : request.name(), "Le nom")
                .maxLength("name", request == null ? null : request.name(), 160, "Le nom")
                .maxLength("description", request == null ? null : request.description(), 255, "La description")
                .rule(request != null && request.agencyId() != null, "agencyId", "Choisissez l'agence du département.")
                .throwIfInvalid();
        return code;
    }

    private DepartmentDto toDto(Department d) {
        return new DepartmentDto(d.getId(), d.getCode(), d.getName(), d.getDescription(), Dtos.agencyRef(d.getAgency()),
                d.isActive(), departments.countUsers(d.getId()), d.getCreatedAt());
    }

    private static Map<String, Object> snapshot(Department d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", d.getCode());
        map.put("name", d.getName());
        map.put("agency", d.getAgency().getCode());
        map.put("description", d.getDescription());
        map.put("active", d.isActive());
        return map;
    }
}
