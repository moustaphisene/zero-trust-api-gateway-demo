package moustapha.nfsgroup.user.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * API « utilisateurs » (annuaire de démonstration).
 *
 * <p>Démontre la séparation des privilèges (RBAC 2.4.1) :
 * <ul>
 *   <li>{@code GET /api/users/me} — accessible à tout utilisateur authentifié (renvoie ses propres claims) ;</li>
 *   <li>{@code GET /api/users} — réservé à la permission {@code user:manage} (ADMIN).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger audit = LoggerFactory.getLogger("SECURITY_AUDIT");

    /** Profil de l'appelant — démontre la lecture des claims (sub, roles, attributs ABAC). */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        audit.info("event=DATA_ACCESS user={} data_type=Profile operation=READ_SELF timestamp={}",
                jwt.getSubject(), Instant.now());
        return Map.of(
                "sub", jwt.getSubject(),
                "preferred_username", jwt.getClaimAsString("preferred_username"),
                "department", String.valueOf(jwt.getClaimAsString("department")),
                "clearance_level", String.valueOf(jwt.getClaimAsString("clearance_level")),
                "tenant_id", String.valueOf(jwt.getClaimAsString("tenant_id")),
                "roles", realmRoles(jwt));
    }

    /** Administration : nécessite la permission fine user:manage (ADMIN). */
    @GetMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public List<Map<String, String>> listUsers(@AuthenticationPrincipal Jwt jwt) {
        audit.info("event=DATA_ACCESS user={} data_type=UserDirectory operation=LIST timestamp={}",
                jwt.getSubject(), Instant.now());
        // Annuaire de démonstration (en production : Keycloak Admin API).
        return List.of(
                Map.of("username", "ahmed.alami", "department", "ENGINEERING", "role", "MANAGER"),
                Map.of("username", "fatima.zahra", "department", "FINANCE", "role", "USER"),
                Map.of("username", "admin", "department", "IT", "role", "ADMIN"),
                Map.of("username", "auditor", "department", "COMPLIANCE", "role", "AUDITOR"));
    }

    @SuppressWarnings("unchecked")
    private Object realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        return realmAccess != null ? realmAccess.getOrDefault("roles", List.of()) : List.of();
    }
}