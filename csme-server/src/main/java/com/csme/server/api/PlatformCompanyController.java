package com.csme.server.api;

import com.csme.server.application.CompanyService;
import com.csme.server.infrastructure.security.SuperAdminOnly;
import com.csme.shared.api.ApiRoutes;
import com.csme.shared.dto.CompanyDtos.CompanyDto;
import com.csme.shared.dto.CompanyDtos.CreateCompanyRequest;
import com.csme.shared.dto.CompanyDtos.SetActiveRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlatformCompanyController {

    private final CompanyService companies;

    public PlatformCompanyController(CompanyService companies) {
        this.companies = companies;
    }

    @SuperAdminOnly
    @GetMapping(ApiRoutes.PLATFORM_COMPANIES)
    public List<CompanyDto> list() {
        return companies.list();
    }

    @SuperAdminOnly
    @PostMapping(ApiRoutes.PLATFORM_COMPANIES)
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyDto create(@RequestBody CreateCompanyRequest request) {
        return companies.create(request);
    }

    @SuperAdminOnly
    @PatchMapping(ApiRoutes.PLATFORM_COMPANIES + "/{id}/active")
    public CompanyDto setActive(@PathVariable UUID id, @RequestBody SetActiveRequest request) {
        return companies.setActive(id, request != null && request.active());
    }
}
