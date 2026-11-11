package com.csme.server.api;

import com.csme.server.application.DashboardService;
import com.csme.server.infrastructure.security.RequiresPermission;
import com.csme.server.infrastructure.security.SuperAdminOnly;
import com.csme.shared.api.ApiRoutes;
import com.csme.shared.dto.DashboardDtos.CompanySummary;
import com.csme.shared.dto.DashboardDtos.PlatformSummary;
import com.csme.shared.security.Action;
import com.csme.shared.security.ErpModule;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @RequiresPermission(module = ErpModule.DASHBOARD, action = Action.VIEW)
    @GetMapping(ApiRoutes.DASHBOARD_SUMMARY)
    public CompanySummary companySummary() {
        return dashboard.companySummary();
    }

    @SuperAdminOnly
    @GetMapping(ApiRoutes.PLATFORM_SUMMARY)
    public PlatformSummary platformSummary() {
        return dashboard.platformSummary();
    }
}
