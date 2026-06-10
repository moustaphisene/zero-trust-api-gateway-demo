package moustapha.nfsgroup.tender.config;

import moustapha.nfsgroup.tender.security.KeycloakRealmRoleConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;

/**
 * Sécurité du tender-service (Resource Server JWT, stateless).
 *
 * <p>Réf. : 1.2.3 (en-têtes), 2.6.4 (validation JWT côté service — Zero Trust 9.5),
 * 2.4.1 (RBAC via authorities), 4.x (durcissement OWASP).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.resource-client-id:api-gateway}")
    private String resourceClientId;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);

        http
            // API stateless : pas de session, CSRF non pertinent (4.7.2).
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // En-têtes HTTP de sécurité (1.2.3 — Listing 1.1).
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(ref -> ref.policy(
                    org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
            // Autorisations (Zero Trust : deny-by-default ; le détail RBAC/ABAC est sur les méthodes).
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(mvc.pattern("/actuator/health/**"), mvc.pattern("/actuator/prometheus"),
                                 mvc.pattern("/actuator/info")).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated())
            // Re-validation du JWT (issuer/signature/expiration) + conversion des rôles Keycloak → authorities.
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter(resourceClientId));
        return converter;
    }
}