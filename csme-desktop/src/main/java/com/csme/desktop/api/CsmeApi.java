package com.csme.desktop.api;

import com.csme.shared.api.ApiRoutes;
import com.csme.shared.api.PageResponse;
import com.csme.shared.dto.AgencyDtos.AgencyDto;
import com.csme.shared.dto.AgencyDtos.SaveAgencyRequest;
import com.csme.shared.dto.AuditDtos.AuditLogDto;
import com.csme.shared.dto.AuthDtos.ChangePasswordRequest;
import com.csme.shared.dto.AuthDtos.LoginRequest;
import com.csme.shared.dto.AuthDtos.RefreshRequest;
import com.csme.shared.dto.AuthDtos.TokenResponse;
import com.csme.shared.dto.CompanyDtos.CompanyDto;
import com.csme.shared.dto.CompanyDtos.CreateCompanyRequest;
import com.csme.shared.dto.CompanyDtos.SetActiveRequest;
import com.csme.shared.dto.DashboardDtos.CompanySummary;
import com.csme.shared.dto.DashboardDtos.PlatformSummary;
import com.csme.shared.dto.UserDtos.CreateUserRequest;
import com.csme.shared.dto.UserDtos.ResetPasswordRequest;
import com.csme.shared.dto.UserDtos.RoleDto;
import com.csme.shared.dto.UserDtos.UpdateUserRequest;
import com.csme.shared.dto.UserDtos.UserDto;
import com.fasterxml.jackson.core.type.TypeReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/** Façade typée de l'API, utilisée par les ViewModels. */
public final class CsmeApi {

    private final ApiClient client;

    public CsmeApi(ApiClient client) {
        this.client = client;
    }

    public ApiClient client() {
        return client;
    }

    // Authentification ---------------------------------------------------------------
    public TokenResponse login(String email, String password) {
        return client.postAnonymous(ApiRoutes.AUTH_LOGIN, new LoginRequest(email, password), new TypeReference<>() { });
    }

    public void logout(String refreshToken) {
        client.postAnonymous(ApiRoutes.AUTH_LOGOUT, new RefreshRequest(refreshToken), null);
    }

    public void changePassword(String current, String next) {
        client.postNoContent(ApiRoutes.AUTH_CHANGE_PASSWORD, new ChangePasswordRequest(current, next));
    }

    // Tableaux de bord ---------------------------------------------------------------
    public CompanySummary companySummary() {
        return client.get(ApiRoutes.DASHBOARD_SUMMARY, new TypeReference<>() { });
    }

    public PlatformSummary platformSummary() {
        return client.get(ApiRoutes.PLATFORM_SUMMARY, new TypeReference<>() { });
    }

    // Plateforme ---------------------------------------------------------------------
    public List<CompanyDto> companies() {
        return client.get(ApiRoutes.PLATFORM_COMPANIES, new TypeReference<>() { });
    }

    public CompanyDto createCompany(CreateCompanyRequest request) {
        return client.post(ApiRoutes.PLATFORM_COMPANIES, request, new TypeReference<>() { });
    }

    public CompanyDto setCompanyActive(UUID id, boolean active) {
        return client.patch(ApiRoutes.PLATFORM_COMPANIES + "/" + id + "/active", new SetActiveRequest(active), new TypeReference<>() { });
    }

    public PageResponse<AuditLogDto> platformAudit(int page, int size) {
        return client.get(ApiRoutes.PLATFORM + "/audit?page=" + page + "&size=" + size, new TypeReference<>() { });
    }

    // Agences ------------------------------------------------------------------------
    public List<AgencyDto> agencies() {
        return client.get(ApiRoutes.AGENCIES, new TypeReference<>() { });
    }

    public AgencyDto createAgency(SaveAgencyRequest request) {
        return client.post(ApiRoutes.AGENCIES, request, new TypeReference<>() { });
    }

    public AgencyDto updateAgency(UUID id, SaveAgencyRequest request) {
        return client.put(ApiRoutes.AGENCIES + "/" + id, request, new TypeReference<>() { });
    }

    public AgencyDto setAgencyActive(UUID id, boolean active) {
        return client.patch(ApiRoutes.AGENCIES + "/" + id + "/active", new SetActiveRequest(active), new TypeReference<>() { });
    }

    // Utilisateurs & rôles -----------------------------------------------------------
    public PageResponse<UserDto> users(String query, int page, int size) {
        String q = query == null || query.isBlank() ? "" : "&q=" + URLEncoder.encode(query.strip(), StandardCharsets.UTF_8);
        return client.get(ApiRoutes.USERS + "?page=" + page + "&size=" + size + q, new TypeReference<>() { });
    }

    public UserDto createUser(CreateUserRequest request) {
        return client.post(ApiRoutes.USERS, request, new TypeReference<>() { });
    }

    public UserDto updateUser(UUID id, UpdateUserRequest request) {
        return client.put(ApiRoutes.USERS + "/" + id, request, new TypeReference<>() { });
    }

    public UserDto setUserActive(UUID id, boolean active) {
        return client.patch(ApiRoutes.USERS + "/" + id + "/active", new SetActiveRequest(active), new TypeReference<>() { });
    }

    public UserDto unlockUser(UUID id) {
        return client.post(ApiRoutes.USERS + "/" + id + "/unlock", null, new TypeReference<>() { });
    }

    public void resetPassword(UUID id, String newPassword) {
        client.postNoContent(ApiRoutes.USERS + "/" + id + "/reset-password", new ResetPasswordRequest(newPassword));
    }

    public List<RoleDto> roles() {
        return client.get(ApiRoutes.ROLES, new TypeReference<>() { });
    }

    // Audit --------------------------------------------------------------------------
    public PageResponse<AuditLogDto> audit(String action, int page, int size) {
        String a = action == null || action.isBlank() ? "" : "&action=" + URLEncoder.encode(action, StandardCharsets.UTF_8);
        return client.get(ApiRoutes.AUDIT + "?page=" + page + "&size=" + size + a, new TypeReference<>() { });
    }
}
