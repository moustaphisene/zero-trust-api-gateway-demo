package moustapha.nfsgroup.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Configuration du Rate Limiting et de la protection DDoS — réf. <b>2.6.5</b> (Listing 2.17).
 *
 * <p>Le {@code RequestRateLimiter} de Spring Cloud Gateway s'appuie sur Redis
 * (algorithme token-bucket distribué) et utilise un {@link KeyResolver} pour
 * déterminer la clé de comptage. Trois stratégies sont fournies :
 * <ul>
 *   <li><b>par IP</b> — anti-DDoS basique (clé par défaut) ;</li>
 *   <li><b>par utilisateur</b> — quota par identité authentifiée (API grand public) ;</li>
 *   <li><b>par clé d'API</b> — quota par partenaire (API B2B).</li>
 * </ul>
 */
@Configuration
public class RateLimiterConfig {

    /** Stratégie par défaut : limitation par adresse IP source (anti-DDoS basique). */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                        .getAddress()
                        .getHostAddress());
    }

    /** Limitation par identité authentifiée (en-tête X-User-Id posé par le filtre JWT). */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            return Mono.just(userId != null && !userId.isBlank() ? userId : "anonymous");
        };
    }

    /** Limitation par clé d'API (scénario B2B). */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            return Mono.just(apiKey != null && !apiKey.isBlank() ? apiKey : "no-key");
        };
    }
}