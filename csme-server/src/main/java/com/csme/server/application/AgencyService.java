package com.csme.server.application;

import com.csme.server.domain.error.ConflictException;
import com.csme.server.domain.error.NotFoundException;
import com.csme.server.domain.model.Agency;
import com.csme.server.domain.rules.Checks;
import com.csme.server.infrastructure.persistence.AgencyRepository;
import com.csme.server.tenancy.TenantContext;
import com.csme.shared.dto.AgencyDtos.AgencyDto;
import com.csme.shared.dto.AgencyDtos.SaveAgencyRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Agences de l'entreprise de l'utilisateur connecté. */
@Service
@Transactional
public class AgencyService {

    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{0,31}$");

    private final AgencyRepository agencies;
    private final AuditService audit;

    public AgencyService(AgencyRepository agencies, AuditService audit) {
        this.agencies = agencies;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AgencyDto> list() {
        return agencies.findAllByCompanyIdOrderByNameAsc(TenantContext.requireCompanyId()).stream()
                .map(Dtos::agency).toList();
    }

    public AgencyDto create(SaveAgencyRequest request) {
        UUID companyId = TenantContext.requireCompanyId();
        String code = validate(request);
        if (agencies.existsByCompanyIdAndCodeIgnoreCase(companyId, code)) {
            throw new ConflictException("Une agence utilise déjà le code " + code + ".");
        }
        Agency agency = agencies.save(new Agency(companyId, code, request.name().strip(),
                Checks.clean(request.city()), Checks.clean(request.phone())));
        audit.success("AGENCY_CREATED", "Agency", agency.getId(), null, snapshot(agency));
        return Dtos.agency(agency);
    }

    public AgencyDto update(UUID id, SaveAgencyRequest request) {
        UUID companyId = TenantContext.requireCompanyId();
        Agency agency = agencies.findByIdAndCompanyId(id, companyId).orElseThrow(() -> new NotFoundException("Agence"));
        String code = validate(request);
        if (!agency.getCode().equalsIgnoreCase(code) && agencies.existsByCompanyIdAndCodeIgnoreCase(companyId, code)) {
            throw new ConflictException("Une agence utilise déjà le code " + code + ".");
        }
        Map<String, Object> before = snapshot(agency);
        agency.update(code, request.name().strip(), Checks.clean(request.city()), Checks.clean(request.phone()));
        audit.success("AGENCY_UPDATED", "Agency", agency.getId(), before, snapshot(agency));
        return Dtos.agency(agency);
    }

    public AgencyDto setActive(UUID id, boolean active) {
        Agency agency = agencies.findByIdAndCompanyId(id, TenantContext.requireCompanyId())
                .orElseThrow(() -> new NotFoundException("Agence"));
        Map<String, Object> before = snapshot(agency);
        agency.setActive(active);
        audit.success(active ? "AGENCY_ACTIVATED" : "AGENCY_DEACTIVATED", "Agency", agency.getId(), before, snapshot(agency));
        return Dtos.agency(agency);
    }

    private static String validate(SaveAgencyRequest request) {
        String code = request == null || Checks.clean(request.code()) == null ? null
                : request.code().strip().toUpperCase(Locale.ROOT);
        Checks.start()
                .required("code", code, "Le code")
                .matches("code", code, CODE, "Code : jusqu'à 32 caractères, lettres majuscules, chiffres, « - » ou « _ ».")
                .required("name", request == null ? null : request.name(), "Le nom")
                .maxLength("name", request == null ? null : request.name(), 160, "Le nom")
                .maxLength("city", request == null ? null : request.city(), 120, "La ville")
                .maxLength("phone", request == null ? null : request.phone(), 40, "Le téléphone")
                .throwIfInvalid();
        return code;
    }

    private static Map<String, Object> snapshot(Agency a) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", a.getCode());
        map.put("name", a.getName());
        map.put("city", a.getCity());
        map.put("phone", a.getPhone());
        map.put("active", a.isActive());
        return map;
    }
}
