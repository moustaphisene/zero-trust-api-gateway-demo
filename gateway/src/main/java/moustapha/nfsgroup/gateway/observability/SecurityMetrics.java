package moustapha.nfsgroup.gateway.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Métriques de sécurité du Gateway exposées à Prometheus.
 *
 * <p>Compteurs publiés sur {@code /actuator/prometheus} :
 * <ul>
 *   <li>{@code security_authentication_success_total} ;</li>
 *   <li>{@code security_authentication_failure_total} ;</li>
 *   <li>{@code security_ratelimit_exceeded_total}.</li>
 * </ul>
 */
@Component
public class SecurityMetrics {

    private final Counter authSuccessCounter;
    private final Counter authFailureCounter;
    private final Counter rateLimitCounter;

    public SecurityMetrics(MeterRegistry registry) {
        this.authSuccessCounter = Counter.builder("security.authentication.success")
                .description("Authentifications reussies au niveau du Gateway")
                .register(registry);
        this.authFailureCounter = Counter.builder("security.authentication.failure")
                .description("Authentifications echouees au niveau du Gateway")
                .register(registry);
        this.rateLimitCounter = Counter.builder("security.ratelimit.exceeded")
                .description("Limites de debit depassees")
                .register(registry);
    }

    public void recordAuthSuccess() {
        authSuccessCounter.increment();
    }

    public void recordAuthFailure() {
        authFailureCounter.increment();
    }

    public void recordRateLimitExceeded() {
        rateLimitCounter.increment();
    }
}