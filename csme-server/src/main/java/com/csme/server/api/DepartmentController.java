package com.csme.server.api;

import com.csme.server.application.DepartmentService;
import com.csme.server.infrastructure.security.RequiresPermission;
import com.csme.shared.api.ApiRoutes;
import com.csme.shared.dto.CompanyDtos.SetActiveRequest;
import com.csme.shared.dto.DepartmentDtos.DepartmentDto;
import com.csme.shared.dto.DepartmentDtos.SaveDepartmentRequest;
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
public class DepartmentController {

    private final DepartmentService departments;

    public DepartmentController(DepartmentService departments) {
        this.departments = departments;
    }

    @RequiresPermission(module = ErpModule.DEPARTMENTS, action = Action.VIEW)
    @GetMapping(ApiRoutes.DEPARTMENTS)
    public List<DepartmentDto> list() {
        return departments.list();
    }

    @RequiresPermission(module = ErpModule.DEPARTMENTS, action = Action.CREATE)
    @PostMapping(ApiRoutes.DEPARTMENTS)
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentDto create(@RequestBody SaveDepartmentRequest request) {
        return departments.create(request);
    }

    @RequiresPermission(module = ErpModule.DEPARTMENTS, action = Action.UPDATE)
    @PutMapping(ApiRoutes.DEPARTMENTS + "/{id}")
    public DepartmentDto update(@PathVariable UUID id, @RequestBody SaveDepartmentRequest request) {
        return departments.update(id, request);
    }

    @RequiresPermission(module = ErpModule.DEPARTMENTS, action = Action.UPDATE)
    @PatchMapping(ApiRoutes.DEPARTMENTS + "/{id}/active")
    public DepartmentDto setActive(@PathVariable UUID id, @RequestBody SetActiveRequest request) {
        return departments.setActive(id, request != null && request.active());
    }
}
