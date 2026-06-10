package moustapha.nfsgroup.tender.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Métriques de sécurité personnalisées.
 * Exposées sur {@code /actuator/prometheus} pour Prometheus / Grafana.
 */
@Component
public class SecurityMetrics {

    private final Counter accessDeniedCounter;
    private final Counter dataAccessCounter;

    public SecurityMetrics(MeterRegistry registry) {
        this.accessDeniedCounter = Counter.builder("security.access.denied")
                .description("Acces refuses (RBAC/ABAC)")
                .register(registry);
        this.dataAccessCounter = Counter.builder("security.data.access")
                .description("Acces aux donnees metier")
                .register(registry);
    }

    public void recordAccessDenied() {
        accessDeniedCounter.increment();
    }

    public void recordDataAccess() {
        dataAccessCounter.increment();
    }
}