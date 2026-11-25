package com.csme.shared.dto;

import java.util.List;

/**
 * Indicateurs du tableau de bord. Seuls les indicateurs calculables à partir de données
 * réellement enregistrées sont exposés ; les indicateurs commerciaux et financiers
 * arriveront avec leurs modules.
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DailyCount(String day, long value) {
    }

    public record CompanySummary(
            String companyName,
            long activeUsers,
            long inactiveUsers,
            long lockedUsers,
            long agencies,
            long departments,
            long roles,
            boolean profileComplete,
            long successfulLoginsToday,
            long failedLoginsToday,
            List<DailyCount> loginsLast14Days,
            List<AuditDtos.AuditLogDto> recentActivity) {
    }

    public record PlatformSummary(
            long companies,
            long activeCompanies,
            long users,
            long successfulLoginsToday,
            long failedLoginsToday,
            List<DailyCount> loginsLast14Days,
            List<AuditDtos.AuditLogDto> recentActivity) {
    }
}
