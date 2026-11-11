package com.csme.server.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.csme.shared.api.ApiRoutes;
import com.csme.shared.dto.AuthDtos.LoginRequest;
import com.csme.shared.dto.AuthDtos.TokenResponse;
import com.csme.shared.dto.CompanyDtos.CompanyDto;
import com.csme.shared.dto.CompanyDtos.CreateCompanyRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base des tests d'intégration : vraie base PostgreSQL (rôles csme_owner / csme_app), schéma recréé
 * par Flyway au démarrage du contexte, appels HTTP via MockMvc avec de vrais jetons.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestBase.CleanDatabase.class)
abstract class IntegrationTestBase {

    protected static final String SUPER_ADMIN_EMAIL = "root@csme.test";
    protected static final String SUPER_ADMIN_PASSWORD = "Plateforme-2026!";
    protected static final String ADMIN_PASSWORD = "Directeur-2026";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @TestConfiguration
    static class CleanDatabase {
        @Bean
        FlywayMigrationStrategy cleanThenMigrate() {
            return flyway -> {
                flyway.clean();
                flyway.migrate();
            };
        }
    }

    /** Entreprise créée pour un test, avec le jeton de son directeur général. */
    protected record Tenant(CompanyDto company, String adminEmail, TokenResponse admin) {
        String token() {
            return admin.accessToken();
        }
    }

    protected static String unique(String prefix) {
        return (prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8)).toUpperCase(Locale.ROOT);
    }

    protected TokenResponse login(String email, String password) throws Exception {
        return call(post(ApiRoutes.AUTH_LOGIN), null, new LoginRequest(email, password), 200, TokenResponse.class);
    }

    protected String superAdminToken() throws Exception {
        return login(SUPER_ADMIN_EMAIL, SUPER_ADMIN_PASSWORD).accessToken();
    }

    protected Tenant newTenant(String prefix) throws Exception {
        String code = unique(prefix);
        String email = "dg." + code.toLowerCase(Locale.ROOT) + "@example.test";
        CompanyDto company = call(post(ApiRoutes.PLATFORM_COMPANIES), superAdminToken(),
                new CreateCompanyRequest(code, "Société " + code, null, "XAF", "CM", "Directeur " + code, email, ADMIN_PASSWORD),
                201, CompanyDto.class);
        return new Tenant(company, email, login(email, ADMIN_PASSWORD));
    }

    // ------------------------------------------------------------------ HTTP helpers

    protected MvcResult perform(MockHttpServletRequestBuilder request, String token, Object body, int expectedStatus)
            throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        }
        MvcResult result = mvc.perform(request).andReturn();
        int status = result.getResponse().getStatus();
        if (status != expectedStatus) {
            throw new AssertionError("Statut HTTP " + status + " au lieu de " + expectedStatus + " pour "
                    + result.getRequest().getMethod() + " " + result.getRequest().getRequestURI()
                    + " — corps : " + result.getResponse().getContentAsString());
        }
        return result;
    }

    protected <T> T call(MockHttpServletRequestBuilder request, String token, Object body, int expectedStatus, Class<T> type)
            throws Exception {
        MvcResult result = perform(request, token, body, expectedStatus);
        return json.readValue(result.getResponse().getContentAsByteArray(), type);
    }

    protected <T> T call(MockHttpServletRequestBuilder request, String token, Object body, int expectedStatus,
                         TypeReference<T> type) throws Exception {
        MvcResult result = perform(request, token, body, expectedStatus);
        return json.readValue(result.getResponse().getContentAsByteArray(), type);
    }

    protected static MockHttpServletRequestBuilder GET(String url) {
        return get(url);
    }

    protected static MockHttpServletRequestBuilder POST(String url) {
        return post(url);
    }

    protected static MockHttpServletRequestBuilder PUT(String url) {
        return put(url);
    }

    protected static MockHttpServletRequestBuilder PATCH(String url) {
        return patch(url);
    }
}
