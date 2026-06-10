package moustapha.nfsgroup.gateway.filter;

import moustapha.nfsgroup.gateway.observability.SecurityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Filtre Gateway personnalisé de propagation d'identité — réf. <b>2.6.4</b> (Listing 2.16).
 *
 * <p>La validation cryptographique du JWT (signature RS256 + expiration via JWKS Keycloak)
 * est assurée en amont par le Resource Server OAuth2 (chaîne de sécurité WebFlux). Ce filtre,
 * exécuté ensuite sur la route, exploite le principal <b>déjà authentifié</b> pour :
 * <ol>
 *   <li>propager l'identité vérifiée vers le microservice aval
 *       ({@code X-User-Id}, {@code X-User-Roles}, {@code X-Tenant-Id}, {@code X-Department}) ;</li>
 *   <li><b>supprimer</b> au préalable tout en-tête d'identité fourni par le client
 *       (durcissement Zero Trust 9.5 — anti header-spoofing).</li>
 * </ol>
 *
 * <p>On évite ainsi un second décodage redondant du token dans le même processus.
 */
@Component
public class JwtValidationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtValidationGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationGatewayFilterFactory.class);

    private final SecurityMetrics metrics;

    public JwtValidationGatewayFilterFactory(SecurityMetrics metrics) {
        super(Config.class);
        this.metrics = metrics;
    }

    @Override
    public GatewayFilter apply(Config config) {
        // L'authentification (signature RS256 + expiration) est déjà imposée par le
        // Resource Server (deny-by-default). Ce filtre lit le principal dans le contexte
        // de sécurité réactif et propage l'identité vérifiée vers le microservice aval.
        return (exchange, chain) -> ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(jwt -> {
                    // Zero Trust : on reconstruit l'identité depuis le token validé dans une
                    // HttpHeaders fraîche et writable. set() écrase tout en-tête X-* injecté
                    // par le client (anti header-spoofing). Décorateur car le builder de
                    // Spring 6.1 expose des en-têtes en lecture seule.
                    HttpHeaders headers = new HttpHeaders();
                    headers.addAll(exchange.getRequest().getHeaders());
                    headers.set("X-User-Id", nullSafe(jwt.getSubject()));
                    headers.set("X-User-Roles", String.join(",", extractRoles(jwt)));
                    headers.set("X-Tenant-Id", nullSafe(jwt.getClaimAsString("tenant_id")));
                    headers.set("X-Department", nullSafe(jwt.getClaimAsString("department")));

                    metrics.recordAuthSuccess();
                    ServerHttpRequest request = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public HttpHeaders getHeaders() {
                            return headers;
                        }
                    };
                    return exchange.mutate().request(request).build();
                })
                // Si le principal n'est pas (encore) disponible, on poursuit sans enrichir :
                // le microservice aval re-valide le JWT de toute façon (Zero Trust).
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    /** Keycloak place les rôles realm dans {@code realm_access.roles}. */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof List<?> roles) {
            return roles.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /** Aucune option n'est requise pour ce filtre. */
    public static class Config {
    }
}