package moustapha.nfsgroup.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

/**
 * Configuration de sécurité du Gateway (WebFlux réactif).
 *
 * <p>Couvre :
 * <ul>
 *   <li><b>1.2.3</b> — En-têtes HTTP de sécurité (CSP, HSTS, X-Frame-Options, nosniff, Referrer-Policy) ;</li>
 *   <li><b>1.4 / 2.6.4</b> — Validation centralisée du JWT (Resource Server OAuth2) ;</li>
 *   <li><b>9.5</b> — Zero Trust : aucune route n'est ouverte sans authentification,
 *       hormis les sondes de santé et le point de scrutation Prometheus.</li>
 * </ul>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            // API stateless : CSRF désactivé (pas de cookie de session ; cf. 4.7.2).
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            // Le Gateway ne gère pas de session HTTP — chaque requête est ré-authentifiée.
            .requestCache(ServerHttpSecurity.RequestCacheSpec::disable)
            // En-têtes de sécurité.
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                .frameOptions(frame -> frame.mode(
                    org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                .hsts(hsts -> hsts
                    .includeSubdomains(true)
                    .maxAge(java.time.Duration.ofDays(365))
                    .preload(true))
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(ref -> ref.policy(
                    org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.NO_REFERRER)))
            // Autorisations (Zero Trust : deny-by-default).
            .authorizeExchange(ex -> ex
                .pathMatchers("/actuator/health/**", "/actuator/prometheus", "/actuator/info").permitAll()
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                // Tout le reste exige un JWT valide.
                .anyExchange().authenticated())
            // Validation du JWT Keycloak (signature RS256 via JWKS — cf. 3.3.1).
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * Durcissement supplémentaire des en-têtes de réponse non couverts nativement
     * par le HeaderSpec (Permissions-Policy, Cache-Control des données sensibles).
     */
    @Bean
    public WebFilter extraSecurityHeadersFilter() {
        return (ServerWebExchange exchange, org.springframework.web.server.WebFilterChain chain) -> {
            exchange.getResponse().beforeCommit(() -> {
                try {
                    var h = exchange.getResponse().getHeaders();
                    h.set("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
                    h.set("X-XSS-Protection", "1; mode=block");
                    // Désactive la mise en cache des réponses d'API sensibles (1.2.3, Cache-Control).
                    if (exchange.getRequest().getPath().value().startsWith("/api/")) {
                        h.set("Cache-Control", "no-store");
                        h.set("Pragma", "no-cache");
                    }
                } catch (UnsupportedOperationException ignored) {
                    // En-têtes déjà figés (ex. court-circuit 429 du rate limiter) : on ignore.
                }
                return Mono.empty();
            });
            return chain.filter(exchange);
        };
    }
}