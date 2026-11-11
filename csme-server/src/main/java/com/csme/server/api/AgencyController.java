package com.csme.server.api;

import com.csme.server.application.AgencyService;
import com.csme.server.infrastructure.security.RequiresPermission;
import com.csme.shared.api.ApiRoutes;
import com.csme.shared.dto.AgencyDtos.AgencyDto;
import com.csme.shared.dto.AgencyDtos.SaveAgencyRequest;
import com.csme.shared.dto.CompanyDtos.SetActiveRequest;
import com.csme.shared.security.Action;
import com.csme.shared.security.ErpModule;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgencyController {

    private final AgencyService agencies;

    public AgencyController(AgencyService agencies) {
        this.agencies = agencies;
    }

    @RequiresPermission(module = ErpModule.AGENCIES, action = Action.VIEW)
    @GetMapping(ApiRoutes.AGENCIES)
    public List<AgencyDto> list() {
        return agencies.list();
    }

    @RequiresPermission(module = ErpModule.AGENCIES, action = Action.CREATE)
    @PostMapping(ApiRoutes.AGENCIES)
    @ResponseStatus(HttpStatus.CREATED)
    public AgencyDto create(@RequestBody SaveAgencyRequest request) {
        return agencies.create(request);
    }

    @RequiresPermission(module = ErpModule.AGENCIES, action = Action.UPDATE)
    @PutMapping(ApiRoutes.AGENCIES + "/{id}")
    public AgencyDto update(@PathVariable UUID id, @RequestBody SaveAgencyRequest request) {
        return agencies.update(id, request);
    }

    @RequiresPermission(module = ErpModule.AGENCIES, action = Action.UPDATE)
    @PatchMapping(ApiRoutes.AGENCIES + "/{id}/active")
    public AgencyDto setActive(@PathVariable UUID id, @RequestBody SetActiveRequest request) {
        return agencies.setActive(id, request != null && request.active());
    }
}
