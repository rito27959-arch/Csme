package com.csme.shared.api;

/** Routes de l'API REST, partagées pour éviter toute divergence entre client et serveur. */
public final class ApiRoutes {

    public static final String BASE = "/api/v1";

    public static final String AUTH = BASE + "/auth";
    public static final String AUTH_LOGIN = AUTH + "/login";
    public static final String AUTH_REFRESH = AUTH + "/refresh";
    public static final String AUTH_LOGOUT = AUTH + "/logout";
    public static final String AUTH_ME = AUTH + "/me";
    public static final String AUTH_CHANGE_PASSWORD = AUTH + "/change-password";

    public static final String DASHBOARD_SUMMARY = BASE + "/dashboard/summary";

    public static final String PLATFORM = BASE + "/platform";
    public static final String PLATFORM_SUMMARY = PLATFORM + "/summary";
    public static final String PLATFORM_COMPANIES = PLATFORM + "/companies";

    public static final String AGENCIES = BASE + "/agencies";
    public static final String DEPARTMENTS = BASE + "/departments";
    public static final String COMPANY_PROFILE = BASE + "/company";
    public static final String USERS = BASE + "/users";
    public static final String ROLES = BASE + "/roles";
    public static final String AUDIT = BASE + "/audit";

    public static final String HEALTH = "/actuator/health";

    private ApiRoutes() {
    }
}
