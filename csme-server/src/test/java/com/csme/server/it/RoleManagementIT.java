package com.csme.server.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.csme.shared.api.ApiError;
import com.csme.shared.api.ApiRoutes;
import com.csme.shared.dto.UserDtos.CreateUserRequest;
import com.csme.shared.dto.UserDtos.RoleDto;
import com.csme.shared.dto.UserDtos.SaveRoleRequest;
import com.csme.shared.dto.UserDtos.UserDto;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Phase 5 : rôles personnalisables, sans élévation de privilèges ni blocage de l'entreprise. */
class RoleManagementIT extends IntegrationTestBase {

    @Test
    void customRoleLifecycle() throws Exception {
        Tenant t = newTenant("ROLE");
        RoleDto created = call(POST(ApiRoutes.ROLES), t.token(),
                new SaveRoleRequest("Auditeur interne", "Lecture seule", Set.of("AUDIT:VIEW", "DASHBOARD:VIEW")),
                201, RoleDto.class);
        assertThat(created.systemRole()).isFalse();
        assertThat(created.permissions()).containsExactlyInAnyOrder("AUDIT:VIEW", "DASHBOARD:VIEW");

        RoleDto updated = call(PUT(ApiRoutes.ROLES + "/" + created.id()), t.token(),
                new SaveRoleRequest("Auditeur", "Lecture", Set.of("AUDIT:VIEW", "AUDIT:EXPORT")), 200, RoleDto.class);
        assertThat(updated.name()).isEqualTo("Auditeur");
        assertThat(updated.permissions()).containsExactlyInAnyOrder("AUDIT:VIEW", "AUDIT:EXPORT");

        perform(POST(ApiRoutes.ROLES), t.token(), new SaveRoleRequest("auditeur", null, Set.of()), 409);
        perform(POST(ApiRoutes.ROLES), t.token(), new SaveRoleRequest("X", null, Set.of("NOPE:VIEW")), 400);

        perform(MockMvcRequestBuilders.delete(ApiRoutes.ROLES + "/" + created.id()), t.token(), null, 204);
    }

    @Test
    void predefinedRolesAreEditableExceptGeneralManagerAndCannotBeDeleted() throws Exception {
        Tenant t = newTenant("SYSR");
        List<RoleDto> roles = call(GET(ApiRoutes.ROLES), t.token(), null, 200, new TypeReference<>() { });
        RoleDto gm = roles.stream().filter(r -> r.name().equals("Directeur général")).findFirst().orElseThrow();
        RoleDto cashier = roles.stream().filter(r -> r.name().equals("Caissier")).findFirst().orElseThrow();

        ApiError locked = call(PUT(ApiRoutes.ROLES + "/" + gm.id()), t.token(),
                new SaveRoleRequest("Directeur général", null, Set.of("DASHBOARD:VIEW")), 422, ApiError.class);
        assertThat(locked.code()).isEqualTo("LOCKED_ROLE");

        RoleDto cashierUpdated = call(PUT(ApiRoutes.ROLES + "/" + cashier.id()), t.token(),
                new SaveRoleRequest("Caissier", "Caisse et ventes comptoir",
                        Set.of("CASH:VIEW", "CASH:CREATE", "SALES:VIEW", "SALES:CREATE")), 200, RoleDto.class);
        assertThat(cashierUpdated.permissions()).contains("SALES:CREATE");

        ApiError system = call(MockMvcRequestBuilders.delete(ApiRoutes.ROLES + "/" + cashier.id()), t.token(), null, 422,
                ApiError.class);
        assertThat(system.code()).isEqualTo("SYSTEM_ROLE");
    }

    @Test
    void roleInUseCannotBeDeleted() throws Exception {
        Tenant t = newTenant("INUSE");
        RoleDto role = call(POST(ApiRoutes.ROLES), t.token(), new SaveRoleRequest("Stagiaire", null, Set.of("DASHBOARD:VIEW")),
                201, RoleDto.class);
        perform(POST(ApiRoutes.USERS), t.token(), new CreateUserRequest("stagiaire." + t.company().code().toLowerCase()
                + "@example.test", "Stagiaire", null, "Stagiaire-2026", Set.of(role.id()), Set.of(), null), 201);
        perform(MockMvcRequestBuilders.delete(ApiRoutes.ROLES + "/" + role.id()), t.token(), null, 409);
    }

    @Test
    void cannotGrantPermissionsOneDoesNotHold() throws Exception {
        Tenant t = newTenant("ESC");
        // Un administrateur des accès : rôles et utilisateurs, mais rien en finance.
        RoleDto accessAdmin = call(POST(ApiRoutes.ROLES), t.token(), new SaveRoleRequest("Admin accès", null,
                Set.of("ROLES:VIEW", "ROLES:CREATE", "ROLES:UPDATE", "USERS:VIEW", "USERS:CREATE", "USERS:UPDATE")),
                201, RoleDto.class);
        String email = "acces." + t.company().code().toLowerCase() + "@example.test";
        perform(POST(ApiRoutes.USERS), t.token(),
                new CreateUserRequest(email, "Admin Accès", null, "AdminAcces-2026", Set.of(accessAdmin.id()), Set.of(), null), 201);
        String token = login(email, "AdminAcces-2026").accessToken();

        ApiError escalation = call(POST(ApiRoutes.ROLES), token,
                new SaveRoleRequest("Trésorier", null, Set.of("FINANCE:VIEW")), 422, ApiError.class);
        assertThat(escalation.code()).isEqualTo("PRIVILEGE_ESCALATION");

        // S'ajouter la finance sur son propre rôle : refusé aussi.
        perform(PUT(ApiRoutes.ROLES + "/" + accessAdmin.id()), token, new SaveRoleRequest("Admin accès", null,
                Set.of("ROLES:VIEW", "ROLES:CREATE", "ROLES:UPDATE", "USERS:VIEW", "USERS:CREATE", "USERS:UPDATE",
                        "FINANCE:VIEW")), 422);

        // Créer un compte « Directeur général » : refusé.
        List<RoleDto> roles = call(GET(ApiRoutes.ROLES), token, null, 200, new TypeReference<>() { });
        RoleDto gm = roles.stream().filter(r -> r.name().equals("Directeur général")).findFirst().orElseThrow();
        perform(POST(ApiRoutes.USERS), token, new CreateUserRequest("pirate." + t.company().code().toLowerCase()
                + "@example.test", "Pirate", null, "Pirate-Passw0rd", Set.of(gm.id()), Set.of(), null), 422);

        // Se retirer le droit de gérer les rôles : refusé (blocage).
        ApiError lockout = call(PUT(ApiRoutes.ROLES + "/" + accessAdmin.id()), token, new SaveRoleRequest("Admin accès", null,
                Set.of("ROLES:VIEW", "USERS:VIEW", "USERS:CREATE", "USERS:UPDATE")), 422, ApiError.class);
        assertThat(lockout.code()).isEqualTo("SELF_LOCKOUT");

        // Accorder ce qu'on possède : autorisé.
        UserDto created = call(POST(ApiRoutes.USERS), token, new CreateUserRequest("adj." + t.company().code().toLowerCase()
                + "@example.test", "Adjoint", null, "Adjoint-Passw0rd", Set.of(accessAdmin.id()), Set.of(), null), 201,
                UserDto.class);
        assertThat(created.roles()).hasSize(1);
    }

    @Test
    void permissionChangesApplyAtNextTokenRefresh() throws Exception {
        Tenant t = newTenant("REFR");
        RoleDto role = call(POST(ApiRoutes.ROLES), t.token(), new SaveRoleRequest("Lecteur", null, Set.of("DASHBOARD:VIEW")),
                201, RoleDto.class);
        String email = "lecteur." + t.company().code().toLowerCase() + "@example.test";
        perform(POST(ApiRoutes.USERS), t.token(),
                new CreateUserRequest(email, "Lecteur", null, "Lecteur-Passw0rd", Set.of(role.id()), Set.of(), null), 201);
        var session = login(email, "Lecteur-Passw0rd");
        perform(GET(ApiRoutes.AGENCIES), session.accessToken(), null, 403);

        perform(PUT(ApiRoutes.ROLES + "/" + role.id()), t.token(),
                new SaveRoleRequest("Lecteur", null, Set.of("DASHBOARD:VIEW", "AGENCIES:VIEW")), 200);
        var refreshed = call(POST(ApiRoutes.AUTH_REFRESH), null,
                new com.csme.shared.dto.AuthDtos.RefreshRequest(session.refreshToken()), 200,
                com.csme.shared.dto.AuthDtos.TokenResponse.class);
        perform(GET(ApiRoutes.AGENCIES), refreshed.accessToken(), null, 200);
    }
}
