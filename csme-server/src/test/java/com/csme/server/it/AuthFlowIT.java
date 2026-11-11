package com.csme.server.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.csme.shared.api.ApiError;
import com.csme.shared.api.ApiRoutes;
import com.csme.shared.api.PageResponse;
import com.csme.shared.dto.AuditDtos.AuditLogDto;
import com.csme.shared.dto.AuthDtos.ChangePasswordRequest;
import com.csme.shared.dto.AuthDtos.CurrentUser;
import com.csme.shared.dto.AuthDtos.LoginRequest;
import com.csme.shared.dto.AuthDtos.RefreshRequest;
import com.csme.shared.dto.AuthDtos.TokenResponse;
import com.csme.shared.dto.UserDtos.CreateUserRequest;
import com.csme.shared.dto.UserDtos.RoleDto;
import com.csme.shared.dto.UserDtos.UserDto;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthFlowIT extends IntegrationTestBase {

    @Test
    void companyAdminLogsInAndSeesOwnProfile() throws Exception {
        Tenant t = newTenant("AUTH");
        assertThat(t.admin().tokenType()).isEqualTo("Bearer");
        assertThat(t.admin().user().companyId()).isEqualTo(t.company().id());
        assertThat(t.admin().user().permissions()).contains("USERS:CREATE", "DASHBOARD:VIEW");

        CurrentUser me = call(GET(ApiRoutes.AUTH_ME), t.token(), null, 200, CurrentUser.class);
        assertThat(me.email()).isEqualTo(t.adminEmail());
        assertThat(me.superAdmin()).isFalse();
    }

    @Test
    void wrongPasswordIsRejectedGenericallyAndAudited() throws Exception {
        Tenant t = newTenant("BADPW");
        ApiError error = call(POST(ApiRoutes.AUTH_LOGIN), null, new LoginRequest(t.adminEmail(), "Wrong-Passw0rd"),
                401, ApiError.class);
        assertThat(error.message()).doesNotContain("mot de passe incorrect");

        ApiError unknown = call(POST(ApiRoutes.AUTH_LOGIN), null, new LoginRequest("nobody@example.test", "Wrong-Passw0rd"),
                401, ApiError.class);
        assertThat(unknown.message()).isEqualTo(error.message());

        PageResponse<AuditLogDto> audit = call(GET(ApiRoutes.AUDIT + "?action=LOGIN_FAILURE"), t.token(), null, 200,
                new TypeReference<>() { });
        assertThat(audit.items()).anySatisfy(a -> assertThat(a.actorEmail()).isEqualTo(t.adminEmail()));
    }

    @Test
    void accountLocksAfterFiveFailures() throws Exception {
        Tenant t = newTenant("LOCK");
        List<RoleDto> roles = call(GET(ApiRoutes.ROLES), t.token(), null, 200, new TypeReference<>() { });
        RoleDto cashier = roles.stream().filter(r -> r.name().equals("Caissier")).findFirst().orElseThrow();
        String email = "caisse." + t.company().code().toLowerCase() + "@example.test";
        UserDto user = call(POST(ApiRoutes.USERS), t.token(),
                new CreateUserRequest(email, "Caissier Test", "Caissier", "Caisse-Passw0rd", Set.of(cashier.id()), Set.of()),
                201, UserDto.class);

        for (int i = 0; i < 5; i++) {
            perform(POST(ApiRoutes.AUTH_LOGIN), null, new LoginRequest(email, "Mauvais-0000"), 401);
        }
        // Même le bon mot de passe est refusé pendant le verrouillage.
        perform(POST(ApiRoutes.AUTH_LOGIN), null, new LoginRequest(email, "Caisse-Passw0rd"), 401);

        PageResponse<UserDto> users = call(GET(ApiRoutes.USERS + "?q=caissier"), t.token(), null, 200,
                new TypeReference<>() { });
        assertThat(users.items()).anySatisfy(u -> {
            assertThat(u.id()).isEqualTo(user.id());
            assertThat(u.locked()).isTrue();
        });

        call(POST(ApiRoutes.USERS + "/" + user.id() + "/unlock"), t.token(), null, 200, UserDto.class);
        login(email, "Caisse-Passw0rd");
    }

    @Test
    void refreshTokensRotateAndReuseRevokesTheWholeFamily() throws Exception {
        Tenant t = newTenant("ROT");
        String first = t.admin().refreshToken();

        TokenResponse second = call(POST(ApiRoutes.AUTH_REFRESH), null, new RefreshRequest(first), 200, TokenResponse.class);
        assertThat(second.refreshToken()).isNotEqualTo(first);

        // Rejouer l'ancien jeton : refusé, et la famille entière est révoquée.
        perform(POST(ApiRoutes.AUTH_REFRESH), null, new RefreshRequest(first), 401);
        perform(POST(ApiRoutes.AUTH_REFRESH), null, new RefreshRequest(second.refreshToken()), 401);
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        Tenant t = newTenant("OUT");
        perform(POST(ApiRoutes.AUTH_LOGOUT), null, new RefreshRequest(t.admin().refreshToken()), 204);
        perform(POST(ApiRoutes.AUTH_REFRESH), null, new RefreshRequest(t.admin().refreshToken()), 401);
    }

    @Test
    void changingPasswordClosesOtherSessions() throws Exception {
        Tenant t = newTenant("CHPW");
        TokenResponse other = login(t.adminEmail(), ADMIN_PASSWORD);
        perform(POST(ApiRoutes.AUTH_CHANGE_PASSWORD), t.token(),
                new ChangePasswordRequest(ADMIN_PASSWORD, "Nouveau-Passw0rd-2026"), 204);
        perform(POST(ApiRoutes.AUTH_REFRESH), null, new RefreshRequest(other.refreshToken()), 401);
        perform(POST(ApiRoutes.AUTH_LOGIN), null, new LoginRequest(t.adminEmail(), ADMIN_PASSWORD), 401);
        login(t.adminEmail(), "Nouveau-Passw0rd-2026");
    }

    @Test
    void requestsWithoutOrWithForgedTokenAreRejected() throws Exception {
        ApiError missing = call(GET(ApiRoutes.AUTH_ME), null, null, 401, ApiError.class);
        assertThat(missing.code()).isEqualTo("UNAUTHENTICATED");
        perform(GET(ApiRoutes.AUTH_ME), "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.forged", null, 401);
    }

    @Test
    void deactivatedCompanyCannotLogIn() throws Exception {
        Tenant t = newTenant("OFF");
        perform(PATCH(ApiRoutes.PLATFORM_COMPANIES + "/" + t.company().id() + "/active"), superAdminToken(),
                new com.csme.shared.dto.CompanyDtos.SetActiveRequest(false), 200);
        perform(POST(ApiRoutes.AUTH_LOGIN), null, new LoginRequest(t.adminEmail(), ADMIN_PASSWORD), 401);
        perform(POST(ApiRoutes.AUTH_REFRESH), null, new RefreshRequest(t.admin().refreshToken()), 401);
    }
}
