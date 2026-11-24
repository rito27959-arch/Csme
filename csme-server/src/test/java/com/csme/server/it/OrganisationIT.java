package com.csme.server.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.csme.shared.api.ApiError;
import com.csme.shared.api.ApiRoutes;
import com.csme.shared.api.PageResponse;
import com.csme.shared.dto.AgencyDtos.AgencyDto;
import com.csme.shared.dto.AgencyDtos.SaveAgencyRequest;
import com.csme.shared.dto.AuditDtos.AuditLogDto;
import com.csme.shared.dto.CompanyDtos.CompanyProfileDto;
import com.csme.shared.dto.CompanyDtos.UpdateCompanyProfileRequest;
import com.csme.shared.dto.DepartmentDtos.DepartmentDto;
import com.csme.shared.dto.DepartmentDtos.SaveDepartmentRequest;
import com.csme.shared.dto.UserDtos.CreateUserRequest;
import com.csme.shared.dto.UserDtos.RoleDto;
import com.csme.shared.dto.UserDtos.UserDto;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Phase 4 : informations de l'entreprise, départements, affectation des utilisateurs. */
class OrganisationIT extends IntegrationTestBase {

    @Test
    void companyUpdatesItsOwnProfileButNotCodeOrCurrency() throws Exception {
        Tenant a = newTenant("PRFA");
        Tenant b = newTenant("PRFB");

        CompanyProfileDto updated = call(PUT(ApiRoutes.COMPANY_PROFILE), a.token(),
                new UpdateCompanyProfileRequest("Acme Cameroun", "Acme Cameroun SA", "Rue de la Joie", "Douala",
                        "+237 233 00 00 00", "Contact@Acme.cm", "https://acme.cm", "M0123456789", "RC/DLA/2020/B/123"),
                200, CompanyProfileDto.class);
        assertThat(updated.name()).isEqualTo("Acme Cameroun");
        assertThat(updated.email()).isEqualTo("contact@acme.cm");
        assertThat(updated.currency()).isEqualTo("XAF");
        assertThat(updated.code()).isEqualTo(a.company().code());

        CompanyProfileDto otherCompany = call(GET(ApiRoutes.COMPANY_PROFILE), b.token(), null, 200, CompanyProfileDto.class);
        assertThat(otherCompany.id()).isEqualTo(b.company().id());
        assertThat(otherCompany.name()).isNotEqualTo("Acme Cameroun");

        ApiError invalid = call(PUT(ApiRoutes.COMPANY_PROFILE), a.token(),
                new UpdateCompanyProfileRequest("", null, null, null, null, "pas-une-adresse", null, null, null),
                400, ApiError.class);
        assertThat(invalid.fieldErrors()).containsKey("name");
    }

    @Test
    void departmentsBelongToAnAgencyOfTheSameCompany() throws Exception {
        Tenant a = newTenant("DEPA");
        Tenant b = newTenant("DEPB");
        AgencyDto agencyA = call(POST(ApiRoutes.AGENCIES), a.token(), new SaveAgencyRequest("DLA", "Douala", "Douala", null),
                201, AgencyDto.class);
        AgencyDto agencyB = call(POST(ApiRoutes.AGENCIES), b.token(), new SaveAgencyRequest("PAR", "Paris", "Paris", null),
                201, AgencyDto.class);

        DepartmentDto sales = call(POST(ApiRoutes.DEPARTMENTS), a.token(),
                new SaveDepartmentRequest("vte", "Ventes", "Équipe commerciale", agencyA.id()), 201, DepartmentDto.class);
        assertThat(sales.code()).isEqualTo("VTE");
        assertThat(sales.agency().id()).isEqualTo(agencyA.id());

        // Rattacher un département à l'agence d'une autre entreprise : refusé.
        ApiError foreign = call(POST(ApiRoutes.DEPARTMENTS), a.token(),
                new SaveDepartmentRequest("X", "Intrus", null, agencyB.id()), 400, ApiError.class);
        assertThat(foreign.fieldErrors()).containsKey("agencyId");

        // Code en double : conflit.
        perform(POST(ApiRoutes.DEPARTMENTS), a.token(), new SaveDepartmentRequest("VTE", "Doublon", null, agencyA.id()), 409);

        // L'autre entreprise ne voit pas et ne modifie pas le département.
        List<DepartmentDto> seenByB = call(GET(ApiRoutes.DEPARTMENTS), b.token(), null, 200, new TypeReference<>() { });
        assertThat(seenByB).isEmpty();
        perform(PUT(ApiRoutes.DEPARTMENTS + "/" + sales.id()), b.token(),
                new SaveDepartmentRequest("VTE", "Piraté", null, agencyB.id()), 404);

        // Affecter un utilisateur au département.
        List<RoleDto> roles = call(GET(ApiRoutes.ROLES), a.token(), null, 200, new TypeReference<>() { });
        RoleDto commercial = roles.stream().filter(r -> r.name().equals("Commercial")).findFirst().orElseThrow();
        UserDto user = call(POST(ApiRoutes.USERS), a.token(),
                new CreateUserRequest("vendeur." + a.company().code().toLowerCase() + "@example.test", "Vendeur Test",
                        "Commercial", "Vendeur-Passw0rd", Set.of(commercial.id()), Set.of(agencyA.id()), sales.id()),
                201, UserDto.class);
        assertThat(user.department().id()).isEqualTo(sales.id());

        List<DepartmentDto> afterAssign = call(GET(ApiRoutes.DEPARTMENTS), a.token(), null, 200, new TypeReference<>() { });
        assertThat(afterAssign.getFirst().userCount()).isEqualTo(1);
    }

    @Test
    void loginHistoryIsVisiblePerUser() throws Exception {
        Tenant t = newTenant("HIST");
        login(t.adminEmail(), ADMIN_PASSWORD);
        perform(POST(ApiRoutes.AUTH_LOGIN), null,
                new com.csme.shared.dto.AuthDtos.LoginRequest(t.adminEmail(), "Mauvais-0000"), 401);

        PageResponse<AuditLogDto> history = call(GET(ApiRoutes.USERS + "/" + t.admin().user().id() + "/logins"), t.token(),
                null, 200, new TypeReference<>() { });
        assertThat(history.items()).extracting(AuditLogDto::action).contains("LOGIN_SUCCESS", "LOGIN_FAILURE");
        assertThat(history.items()).allMatch(a -> t.admin().user().id().toString().equals(a.entityId()));
    }
}
