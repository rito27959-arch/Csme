package com.csme.server.config;

import com.csme.server.infrastructure.security.ApiAuthenticationEntryPoint;
import com.csme.server.infrastructure.security.CurrentActor;
import com.csme.server.infrastructure.security.JwtProperties;
import com.csme.server.infrastructure.security.JwtService;
import com.csme.server.tenancy.TenantContextFilter;
import com.csme.shared.api.ApiRoutes;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API sans état : jeton d'accès JWT dans l'en-tête Authorization, aucune session HTTP, pas de cookie
 * (donc pas de CSRF). Les permissions fines sont contrôlées par {@code PermissionInterceptor}.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(objectMapper);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, ApiRoutes.AUTH_LOGIN, ApiRoutes.AUTH_REFRESH, ApiRoutes.AUTH_LOGOUT).permitAll()
                        .requestMatchers(ApiRoutes.HEALTH, ApiRoutes.HEALTH + "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(entryPoint)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new ClaimsAuthenticationConverter())))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
                .addFilterAfter(new TenantContextFilter(), BearerTokenAuthenticationFilter.class)
                .headers(h -> h
                        .contentTypeOptions(c -> { })
                        .frameOptions(f -> f.deny())
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000)));
        return http.build();
    }

    @Bean
    JwtService jwtService(JwtProperties properties) {
        return new JwtService(properties);
    }

    @Bean
    JwtDecoder jwtDecoder(JwtService jwtService) {
        return jwtService.decoder();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // BCrypt par défaut, avec préfixe d'algorithme pour permettre une migration future (Argon2…).
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** Transforme les revendications signées du jeton en autorités Spring Security. */
    static final class ClaimsAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
        @Override
        public AbstractAuthenticationToken convert(Jwt jwt) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            List<String> permissions = jwt.getClaimAsStringList(JwtService.CLAIM_PERMISSIONS);
            if (permissions != null) {
                permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
            }
            if (Boolean.TRUE.equals(jwt.getClaimAsBoolean(JwtService.CLAIM_SUPER_ADMIN))) {
                authorities.add(new SimpleGrantedAuthority(CurrentActor.SUPER_ADMIN_AUTHORITY));
            }
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        }
    }
}
