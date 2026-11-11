package com.csme.server.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.csme.server.infrastructure.security.AuthenticatedOnly;
import com.csme.server.infrastructure.security.PublicEndpoint;
import com.csme.server.infrastructure.security.RequiresPermission;
import com.csme.server.infrastructure.security.SuperAdminOnly;
import com.csme.shared.api.ApiRoutes;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Aucun point d'entrée de l'API ne doit exister sans déclaration explicite de sécurité. */
class EndpointSecurityIT extends IntegrationTestBase {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mapping;

    @Test
    void everyApiEndpointDeclaresItsSecurity() {
        List<String> unprotected = mapping.getHandlerMethods().entrySet().stream()
                .filter(e -> e.getKey().getPatternValues().stream().anyMatch(p -> p.startsWith(ApiRoutes.BASE)))
                .filter(e -> {
                    var m = e.getValue();
                    return !m.hasMethodAnnotation(RequiresPermission.class)
                            && !m.hasMethodAnnotation(SuperAdminOnly.class)
                            && !m.hasMethodAnnotation(AuthenticatedOnly.class)
                            && !m.hasMethodAnnotation(PublicEndpoint.class);
                })
                .map(e -> e.getKey().toString())
                .toList();
        assertThat(unprotected).isEmpty();
        assertThat(mapping.getHandlerMethods()).isNotEmpty();
    }
}
