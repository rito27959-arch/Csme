package com.csme.server.application;

import com.csme.server.infrastructure.persistence.AgencyRepository;
import com.csme.server.infrastructure.persistence.AuditLogRepository;
import com.csme.server.infrastructure.persistence.CompanyRepository;
import com.csme.server.infrastructure.persistence.RoleRepository;
import com.csme.server.infrastructure.persistence.UserRepository;
import com.csme.server.tenancy.TenantContext;
import com.csme.shared.dto.DashboardDtos.CompanySummary;
import com.csme.shared.dto.DashboardDtos.DailyCount;
import com.csme.shared.dto.DashboardDtos.PlatformSummary;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Indicateurs calculés à partir des opérations réellement enregistrées. */
@Service
public class DashboardService {

    private static final int HISTORY_DAYS = 14;

    private final CompanyRepository companies;
    private final UserRepository users;
    private final AgencyRepository agencies;
    private final RoleRepository roles;
    private final AuditLogRepository auditLogs;
    private final SystemTransactions system;
    private final Clock clock;
    private final ZoneId zone;

    public DashboardService(CompanyRepository companies, UserRepository users, AgencyRepository agencies,
                            RoleRepository roles, AuditLogRepository auditLogs, SystemTransactions system, Clock clock,
                            @Value("${csme.reporting.time-zone:UTC}") String zone) {
        this.companies = companies;
        this.users = users;
        this.agencies = agencies;
        this.roles = roles;
        this.auditLogs = auditLogs;
        this.system = system;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
    }

    @Transactional(readOnly = true)
    public CompanySummary companySummary() {
        UUID companyId = TenantContext.requireCompanyId();
        Instant now = clock.instant();
        Instant startOfToday = LocalDate.now(clock.withZone(zone)).atStartOfDay(zone).toInstant();
        Instant historyStart = startOfToday.minus(java.time.Duration.ofDays(HISTORY_DAYS - 1L));
        String name = companies.findById(companyId).map(c -> c.getName()).orElse("");
        return new CompanySummary(
                name,
                users.countByCompanyIdAndActive(companyId, true),
                users.countByCompanyIdAndActive(companyId, false),
                users.countByCompanyIdAndLockedUntilAfter(companyId, now),
                agencies.countByCompanyId(companyId),
                roles.countByCompanyId(companyId),
                auditLogs.countByCompanyIdAndActionAndOccurredAtGreaterThanEqual(companyId, "LOGIN_SUCCESS", startOfToday),
                auditLogs.countByCompanyIdAndActionAndOccurredAtGreaterThanEqual(companyId, "LOGIN_FAILURE", startOfToday),
                perDay(auditLogs.occurrencesForCompany(companyId, "LOGIN_SUCCESS", historyStart), historyStart),
                auditLogs.findTop10ByCompanyIdOrderByOccurredAtDesc(companyId).stream().map(Dtos::audit).toList());
    }

    public PlatformSummary platformSummary() {
        return system.call(() -> {
            Instant startOfToday = LocalDate.now(clock.withZone(zone)).atStartOfDay(zone).toInstant();
            Instant historyStart = startOfToday.minus(java.time.Duration.ofDays(HISTORY_DAYS - 1L));
            return new PlatformSummary(
                    companies.count(),
                    companies.countByActiveTrue(),
                    users.countBySuperAdminFalse(),
                    auditLogs.countByActionAndOccurredAtGreaterThanEqual("LOGIN_SUCCESS", startOfToday),
                    auditLogs.countByActionAndOccurredAtGreaterThanEqual("LOGIN_FAILURE", startOfToday),
                    perDay(auditLogs.occurrencesForPlatform("LOGIN_SUCCESS", historyStart), historyStart),
                    auditLogs.findTop10ByOrderByOccurredAtDesc().stream().map(Dtos::audit).toList());
        });
    }

    /** Regroupe les instants par jour (fuseau de reporting), en incluant les jours sans activité. */
    private List<DailyCount> perDay(List<Instant> instants, Instant from) {
        Map<LocalDate, Long> counts = instants.stream()
                .collect(Collectors.groupingBy(i -> i.atZone(zone).toLocalDate(), Collectors.counting()));
        LocalDate first = from.atZone(zone).toLocalDate();
        List<DailyCount> result = new ArrayList<>(HISTORY_DAYS);
        for (int d = 0; d < HISTORY_DAYS; d++) {
            LocalDate day = first.plusDays(d);
            result.add(new DailyCount(day.toString(), counts.getOrDefault(day, 0L)));
        }
        return result;
    }

}
