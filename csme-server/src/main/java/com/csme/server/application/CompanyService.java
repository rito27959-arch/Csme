package com.csme.server.application;

import com.csme.server.domain.error.ConflictException;
import com.csme.server.domain.error.NotFoundException;
import com.csme.server.domain.model.AppUser;
import com.csme.server.domain.model.Company;
import com.csme.server.domain.model.Role;
import com.csme.server.domain.rules.Checks;
import com.csme.server.infrastructure.persistence.CompanyRepository;
import com.csme.server.infrastructure.persistence.RoleRepository;
import com.csme.server.infrastructure.persistence.UserRepository;
import com.csme.shared.dto.CompanyDtos.CompanyDto;
import com.csme.shared.dto.CompanyDtos.CreateCompanyRequest;
import com.csme.shared.security.DefaultRoles;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Gestion des entreprises par le super administrateur (portée système).
 * Créer une entreprise crée aussi ses rôles prédéfinis et son premier compte « Directeur général ».
 */
@Service
public class CompanyService {

    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{1,31}$");
    private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern COUNTRY = Pattern.compile("^[A-Z]{2}$");

    private final CompanyRepository companies;
    private final RoleRepository roles;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final SystemTransactions system;

    public CompanyService(CompanyRepository companies, RoleRepository roles, UserRepository users,
                          PasswordEncoder passwordEncoder, AuditService audit, SystemTransactions system) {
        this.companies = companies;
        this.roles = roles;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.system = system;
    }

    public List<CompanyDto> list() {
        return system.call(() -> companies.findAll().stream()
                .sorted(Comparator.comparing(Company::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList());
    }

    public CompanyDto create(CreateCompanyRequest request) {
        if (request == null) {
            Checks.start().required("name", null, "Le nom").throwIfInvalid();
        }
        String code = upper(request.code());
        String currency = upper(request.currency());
        String country = upper(request.country());
        String adminEmail = Checks.normalizeEmail(request.adminEmail());
        Checks.start()
                .required("code", code, "Le code")
                .matches("code", code, CODE, "Code : 2 à 32 caractères, lettres majuscules, chiffres, « - » ou « _ ».")
                .required("name", request.name(), "Le nom")
                .maxLength("name", request.name(), 160, "Le nom")
                .maxLength("legalName", request.legalName(), 200, "La raison sociale")
                .required("currency", currency, "La devise")
                .matches("currency", currency, CURRENCY, "Devise : code ISO 4217 à 3 lettres (ex. XAF, EUR).")
                .required("country", country, "Le pays")
                .matches("country", country, COUNTRY, "Pays : code ISO 3166 à 2 lettres (ex. CM, FR).")
                .required("adminFullName", request.adminFullName(), "Le nom du directeur général")
                .maxLength("adminFullName", request.adminFullName(), 160, "Le nom")
                .required("adminEmail", adminEmail, "L'adresse électronique du directeur général")
                .email("adminEmail", adminEmail)
                .password("adminPassword", request.adminPassword(), adminEmail)
                .throwIfInvalid();

        return system.call(() -> {
            if (companies.existsByCode(code)) {
                throw new ConflictException("Une entreprise utilise déjà le code " + code + ".");
            }
            if (users.existsByEmail(adminEmail)) {
                throw new ConflictException("Cette adresse électronique est déjà utilisée.");
            }
            Company company = companies.save(new Company(code, request.name().strip(),
                    Checks.clean(request.legalName()), currency, country));
            Role generalManager = null;
            for (DefaultRoles.Template template : DefaultRoles.all()) {
                Role role = roles.save(new Role(company.getId(), template.name(), template.description(), true,
                        template.permissions()));
                if (DefaultRoles.GENERAL_MANAGER.equals(template.name())) {
                    generalManager = role;
                }
            }
            AppUser admin = AppUser.companyUser(company.getId(), adminEmail, request.adminFullName().strip(),
                    DefaultRoles.GENERAL_MANAGER, passwordEncoder.encode(request.adminPassword()));
            admin.replaceRoles(List.of(generalManager));
            users.save(admin);
            audit.successForCompany(company.getId(), "COMPANY_CREATED", "Company", company.getId(),
                    Map.of("code", code, "name", company.getName(), "currency", currency, "country", country,
                            "adminEmail", adminEmail));
            return toDto(company);
        });
    }

    public CompanyDto setActive(UUID id, boolean active) {
        return system.call(() -> {
            Company company = companies.findById(id).orElseThrow(() -> new NotFoundException("Entreprise"));
            boolean before = company.isActive();
            company.setActive(active);
            audit.successForCompany(company.getId(), active ? "COMPANY_ACTIVATED" : "COMPANY_DEACTIVATED",
                    "Company", company.getId(), Map.of("activeBefore", before, "activeAfter", active));
            return toDto(company);
        });
    }

    private CompanyDto toDto(Company c) {
        return new CompanyDto(c.getId(), c.getCode(), c.getName(), c.getLegalName(), c.getCurrency(), c.getCountry(),
                c.isActive(), users.countByCompanyId(c.getId()), c.getCreatedAt());
    }

    private static String upper(String value) {
        String clean = Checks.clean(value);
        return clean == null ? null : clean.toUpperCase(Locale.ROOT);
    }
}
