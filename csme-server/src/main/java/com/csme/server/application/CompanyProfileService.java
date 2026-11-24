package com.csme.server.application;

import com.csme.server.domain.error.NotFoundException;
import com.csme.server.domain.model.Company;
import com.csme.server.domain.rules.Checks;
import com.csme.server.infrastructure.persistence.CompanyRepository;
import com.csme.server.tenancy.TenantContext;
import com.csme.shared.dto.CompanyDtos.CompanyProfileDto;
import com.csme.shared.dto.CompanyDtos.UpdateCompanyProfileRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Informations générales de l'entreprise de l'utilisateur connecté. */
@Service
@Transactional
public class CompanyProfileService {

    private static final Pattern WEBSITE = Pattern.compile("^(https?://)?[\\w.-]+\\.[a-zA-Z]{2,}(/\\S*)?$");

    private final CompanyRepository companies;
    private final AuditService audit;

    public CompanyProfileService(CompanyRepository companies, AuditService audit) {
        this.companies = companies;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public CompanyProfileDto get() {
        return toDto(load());
    }

    public CompanyProfileDto update(UpdateCompanyProfileRequest r) {
        Checks.start()
                .required("name", r == null ? null : r.name(), "Le nom")
                .throwIfInvalid();
        Checks.start()
                .maxLength("name", r.name(), 160, "Le nom")
                .maxLength("legalName", r.legalName(), 200, "La raison sociale")
                .maxLength("address", r.address(), 255, "L'adresse")
                .maxLength("city", r.city(), 120, "La ville")
                .maxLength("phone", r.phone(), 40, "Le téléphone")
                .maxLength("email", r.email(), 254, "L'adresse électronique")
                .email("email", r.email())
                .maxLength("website", r.website(), 255, "Le site web")
                .matches("website", r.website(), WEBSITE, "Adresse de site web invalide.")
                .maxLength("taxId", r.taxId(), 64, "Le numéro fiscal")
                .maxLength("tradeRegister", r.tradeRegister(), 64, "Le registre de commerce")
                .throwIfInvalid();
        Company company = load();
        Map<String, Object> before = snapshot(company);
        company.updateProfile(r.name().strip(), Checks.clean(r.legalName()), Checks.clean(r.address()),
                Checks.clean(r.city()), Checks.clean(r.phone()), Checks.normalizeEmail(Checks.clean(r.email())),
                Checks.clean(r.website()), Checks.clean(r.taxId()), Checks.clean(r.tradeRegister()));
        audit.success("COMPANY_PROFILE_UPDATED", "Company", company.getId(), before, snapshot(company));
        return toDto(company);
    }

    private Company load() {
        return companies.findById(TenantContext.requireCompanyId()).orElseThrow(() -> new NotFoundException("Entreprise"));
    }

    private static CompanyProfileDto toDto(Company c) {
        return new CompanyProfileDto(c.getId(), c.getCode(), c.getName(), c.getLegalName(), c.getCurrency(), c.getCountry(),
                c.getAddress(), c.getCity(), c.getPhone(), c.getEmail(), c.getWebsite(), c.getTaxId(), c.getTradeRegister());
    }

    private static Map<String, Object> snapshot(Company c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", c.getName());
        map.put("legalName", c.getLegalName());
        map.put("address", c.getAddress());
        map.put("city", c.getCity());
        map.put("phone", c.getPhone());
        map.put("email", c.getEmail());
        map.put("website", c.getWebsite());
        map.put("taxId", c.getTaxId());
        map.put("tradeRegister", c.getTradeRegister());
        return map;
    }
}
