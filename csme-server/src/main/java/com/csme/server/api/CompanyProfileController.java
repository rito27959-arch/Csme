package com.csme.server.api;

import com.csme.server.application.CompanyProfileService;
import com.csme.server.infrastructure.security.RequiresPermission;
import com.csme.shared.api.ApiRoutes;
import com.csme.shared.dto.CompanyDtos.CompanyProfileDto;
import com.csme.shared.dto.CompanyDtos.UpdateCompanyProfileRequest;
import com.csme.shared.security.Action;
import com.csme.shared.security.ErpModule;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CompanyProfileController {

    private final CompanyProfileService profile;

    public CompanyProfileController(CompanyProfileService profile) {
        this.profile = profile;
    }

    @RequiresPermission(module = ErpModule.SETTINGS, action = Action.VIEW)
    @GetMapping(ApiRoutes.COMPANY_PROFILE)
    public CompanyProfileDto get() {
        return profile.get();
    }

    @RequiresPermission(module = ErpModule.SETTINGS, action = Action.UPDATE)
    @PutMapping(ApiRoutes.COMPANY_PROFILE)
    public CompanyProfileDto update(@RequestBody UpdateCompanyProfileRequest request) {
        return profile.update(request);
    }
}
