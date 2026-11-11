package com.csme.server.api;

import com.csme.server.application.AuditQueryService;
import com.csme.server.infrastructure.security.RequiresPermission;
import com.csme.server.infrastructure.security.SuperAdminOnly;
import com.csme.shared.api.ApiRoutes;
import com.csme.shared.api.PageResponse;
import com.csme.shared.dto.AuditDtos.AuditLogDto;
import com.csme.shared.security.Action;
import com.csme.shared.security.ErpModule;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditController {

    private final AuditQueryService audit;

    public AuditController(AuditQueryService audit) {
        this.audit = audit;
    }

    @RequiresPermission(module = ErpModule.AUDIT, action = Action.VIEW)
    @GetMapping(ApiRoutes.AUDIT)
    public PageResponse<AuditLogDto> companyAudit(@RequestParam(name = "action", required = false) String action,
                                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                                  @RequestParam(name = "size", defaultValue = "50") int size) {
        return audit.forCompany(action, page, size);
    }

    @SuperAdminOnly
    @GetMapping(ApiRoutes.PLATFORM + "/audit")
    public PageResponse<AuditLogDto> platformAudit(@RequestParam(name = "page", defaultValue = "0") int page,
                                                   @RequestParam(name = "size", defaultValue = "50") int size) {
        return audit.forPlatform(page, size);
    }
}
