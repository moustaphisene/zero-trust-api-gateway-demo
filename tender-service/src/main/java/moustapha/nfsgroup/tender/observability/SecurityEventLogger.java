package moustapha.nfsgroup.tender.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Journalisation structurée des événements de sécurité.
 *
 * <p>Émet des lignes clé=valeur sous le logger {@code SECURITY_AUDIT}, directement
 * exploitables par un SIEM (Security Information and Event Management).
 */
@Component
public class SecurityEventLogger {

    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void logAuthenticationSuccess(String userId, String clientIp) {
        log.info("event=AUTH_SUCCESS user={} ip={} timestamp={}", userId, clientIp, Instant.now());
    }

    public void logAuthenticationFailure(String attemptedUser, String clientIp, String reason) {
        log.warn("event=AUTH_FAILURE user={} ip={} reason={} timestamp={}",
                attemptedUser, clientIp, reason, Instant.now());
    }

    public void logAccessDenied(String userId, String resource, String clientIp) {
        log.warn("event=ACCESS_DENIED user={} resource={} ip={} timestamp={}",
                userId, resource, clientIp, Instant.now());
    }

    public void logSuspiciousActivity(String userId, String description, String clientIp) {
        log.error("event=SUSPICIOUS_ACTIVITY user={} description={} ip={} timestamp={}",
                userId, description, clientIp, Instant.now());
    }

    public void logDataAccess(String userId, String dataType, String operation) {
        log.info("event=DATA_ACCESS user={} data_type={} operation={} timestamp={}",
                userId, dataType, operation, Instant.now());
    }
}