package com.csme.server.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.csme.server.domain.model.Agency;
import com.csme.server.infrastructure.persistence.AgencyRepository;
import com.csme.server.infrastructure.persistence.UserRepository;
import com.csme.server.tenancy.TenantContext;
import com.csme.shared.api.ApiRoutes;
import com.csme.shared.api.PageResponse;
import com.csme.shared.dto.AgencyDtos.AgencyDto;
import com.csme.shared.dto.AgencyDtos.SaveAgencyRequest;
import com.csme.shared.dto.UserDtos.CreateUserRequest;
import com.csme.shared.dto.UserDtos.RoleDto;
import com.csme.shared.dto.UserDtos.UpdateUserRequest;
import com.csme.shared.dto.UserDtos.UserDto;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Critère de validation n°12 du cahier des charges : une entreprise ne peut jamais accéder aux données d'une autre.
 * Vérifié au niveau de l'API puis directement au niveau de la base (RLS), sans filtre applicatif.
 */
class TenantIsolationIT extends IntegrationTestBase {

    @Autowired
    AgencyRepository agencyRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void companyCannotSeeOrModifyAnotherCompanysData() throws Exception {
        Tenant a = newTenant("ISOA");
        Tenant b = newTenant("ISOB");

        AgencyDto agencyOfA = call(POST(ApiRoutes.AGENCIES), a.token(),
                new SaveAgencyRequest("DLA", "Douala Centre", "Douala", null), 201, AgencyDto.class);

        List<AgencyDto> seenByB = call(GET(ApiRoutes.AGENCIES), b.token(), null, 200, new TypeReference<>() { });
        assertThat(seenByB).extracting(AgencyDto::id).doesNotContain(agencyOfA.id());

        // Modifier / désactiver l'agence de A depuis B : introuvable (aucune fuite d'existence).
        perform(PUT(ApiRoutes.AGENCIES + "/" + agencyOfA.id()), b.token(),
                new SaveAgencyRequest("HACK", "Piratée", null, null), 404);
        perform(PATCH(ApiRoutes.AGENCIES + "/" + agencyOfA.id() + "/active"), b.token(),
                new com.csme.shared.dto.CompanyDtos.SetActiveRequest(false), 404);

        PageResponse<UserDto> usersSeenByB = call(GET(ApiRoutes.USERS), b.token(), null, 200, new TypeReference<>() { });
        assertThat(usersSeenByB.items()).extracting(UserDto::email).doesNotContain(a.adminEmail());

        UserDto adminOfA = call(GET(ApiRoutes.USERS), a.token(), null, 200, new TypeReference<PageResponse<UserDto>>() { })
                .items().getFirst();
        List<RoleDto> rolesOfB = call(GET(ApiRoutes.ROLES), b.token(), null, 200, new TypeReference<>() { });
        perform(PUT(ApiRoutes.USERS + "/" + adminOfA.id()), b.token(),
                new UpdateUserRequest("Pirate", null, Set.of(rolesOfB.getFirst().id()), Set.of(), null), 404);

        // Utiliser un rôle de A pour créer un compte dans B : refusé.
        List<RoleDto> rolesOfA = call(GET(ApiRoutes.ROLES), a.token(), null, 200, new TypeReference<>() { });
        perform(POST(ApiRoutes.USERS), b.token(), new CreateUserRequest("intrus@example.test", "Intrus", null,
                "Intrus-Passw0rd", Set.of(rolesOfA.getFirst().id()), Set.of(), null), 400);
    }

    @Test
    void databaseRowLevelSecurityHidesOtherTenantsEvenWithoutApplicationFilter() throws Exception {
        Tenant a = newTenant("RLSA");
        Tenant b = newTenant("RLSB");
        AgencyDto agencyOfA = call(POST(ApiRoutes.AGENCIES), a.token(),
                new SaveAgencyRequest("YDE", "Yaoundé", "Yaoundé", null), 201, AgencyDto.class);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // findById et findAll n'ont AUCUN filtre par entreprise : seule la base protège.
        TenantContext.asCompany(b.company().id(), () -> tx.execute(s -> {
            assertThat(agencyRepository.findById(agencyOfA.id())).isEmpty();
            assertThat(agencyRepository.findAll()).extracting(Agency::getCompanyId)
                    .allMatch(id -> id.equals(b.company().id()));
            assertThat(userRepository.findAll()).allMatch(u -> b.company().id().equals(u.getCompanyId()));
            return null;
        }));

        TenantContext.asCompany(a.company().id(), () -> tx.execute(s -> {
            assertThat(agencyRepository.findById(agencyOfA.id())).isPresent();
            return null;
        }));

        // Sans portée du tout : aucune ligne métier visible.
        Long visible = tx.execute(s -> agencyRepository.count() + userRepository.count());
        assertThat(visible).isZero();
    }
}
