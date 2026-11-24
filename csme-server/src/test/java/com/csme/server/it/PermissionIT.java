package com.csme.server.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.csme.shared.api.ApiRoutes;
import com.csme.shared.api.PageResponse;
import com.csme.shared.dto.AgencyDtos.SaveAgencyRequest;
import com.csme.shared.dto.AuditDtos.AuditLogDto;
import com.csme.shared.dto.CompanyDtos.SetActiveRequest;
import com.csme.shared.dto.UserDtos.CreateUserRequest;
import com.csme.shared.dto.UserDtos.RoleDto;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Les permissions sont vérifiées côté serveur, pas seulement masquées dans l'interface. */
class PermissionIT extends IntegrationTestBase {

    @Test
    void userWithoutPermissionIsDeniedAndDenialIsAudited() throws Exception {
        Tenant t = newTenant("PERM");
        List<RoleDto> roles = call(GET(ApiRoutes.ROLES), t.token(), null, 200, new TypeReference<>() { });
        RoleDto storekeeper = roles.stream().filter(r -> r.name().equals("Magasinier")).findFirst().orElseThrow();
        String email = "mag." + t.company().code().toLowerCase() + "@example.test";
        perform(POST(ApiRoutes.USERS), t.token(),
                new CreateUserRequest(email, "Magasinier Test", null, "Magasin-Passw0rd", Set.of(storekeeper.id()), Set.of(), null), 201);
        String token = login(email, "Magasin-Passw0rd").accessToken();

        perform(GET(ApiRoutes.USERS), token, null, 403);
        perform(POST(ApiRoutes.AGENCIES), token, new SaveAgencyRequest("X1", "Interdite", null, null), 403);
        perform(GET(ApiRoutes.DASHBOARD_SUMMARY), token, null, 403);
        perform(GET(ApiRoutes.PLATFORM_COMPANIES), token, null, 403);

        PageResponse<AuditLogDto> denied = call(GET(ApiRoutes.AUDIT + "?action=ACCESS_DENIED"), t.token(), null, 200,
                new TypeReference<>() { });
        assertThat(denied.items()).anySatisfy(a -> assertThat(a.actorEmail()).isEqualTo(email));
    }

    @Test
    void superAdminHasNoAccessToCompanyBusinessData() throws Exception {
        String root = superAdminToken();
        perform(GET(ApiRoutes.USERS), root, null, 403);
        perform(GET(ApiRoutes.AGENCIES), root, null, 403);
        perform(GET(ApiRoutes.PLATFORM_SUMMARY), root, null, 200);
    }

    @Test
    void companyAdminCannotUsePlatformEndpoints() throws Exception {
        Tenant t = newTenant("NOPLAT");
        perform(GET(ApiRoutes.PLATFORM_COMPANIES), t.token(), null, 403);
        perform(PATCH(ApiRoutes.PLATFORM_COMPANIES + "/" + t.company().id() + "/active"), t.token(),
                new SetActiveRequest(false), 403);
    }

    @Test
    void adminCannotDeactivateThemselves() throws Exception {
        Tenant t = newTenant("SELF");
        var me = t.admin().user();
        perform(PATCH(ApiRoutes.USERS + "/" + me.id() + "/active"), t.token(), new SetActiveRequest(false), 422);
    }

    @Test
    void validationErrorsAreReportedPerField() throws Exception {
        Tenant t = newTenant("VAL");
        var error = call(POST(ApiRoutes.USERS), t.token(),
                new CreateUserRequest("pas-une-adresse", "", null, "court", Set.of(), Set.of(), null), 400,
                com.csme.shared.api.ApiError.class);
        assertThat(error.fieldErrors()).containsKeys("email", "fullName", "initialPassword", "roleIds");
    }
}
