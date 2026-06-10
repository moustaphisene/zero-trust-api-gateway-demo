package moustapha.nfsgroup.tender.security;

import moustapha.nfsgroup.tender.domain.Tender;
import moustapha.nfsgroup.tender.domain.TenderRepository;
import moustapha.nfsgroup.tender.observability.SecurityEventLogger;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * PDP (Policy Decision Point) ABAC — réf. <b>2.4.2</b>.
 *
 * <p>Évalue les décisions d'accès à partir d'attributs issus de plusieurs sources :
 * l'utilisateur (claims du JWT : {@code department}, {@code clearance_level},
 * {@code approval_limit}), la ressource (l'offre ciblée), l'action et le contexte
 * (heure courante). Invoqué via {@code @PreAuthorize("hasPermission(#id, 'Tender', 'approve')")}.
 *
 * <p>Politiques implémentées :
 * <ul>
 *   <li><b>same-department</b> : un utilisateur ne peut modifier une offre que s'il
 *       appartient au même département que celle-ci ;</li>
 *   <li><b>within-approval-limit</b> : un manager ne peut approuver une offre que si son
 *       montant est inférieur à son plafond d'autorisation ;</li>
 *   <li><b>business-hours</b> : certaines actions sensibles sont restreintes aux heures ouvrables.</li>
 * </ul>
 */
@Component
public class AbacPermissionEvaluator implements PermissionEvaluator {

    private final TenderRepository tenderRepository;
    private final SecurityEventLogger audit;

    public AbacPermissionEvaluator(TenderRepository tenderRepository, SecurityEventLogger audit) {
        this.tenderRepository = tenderRepository;
        this.audit = audit;
    }

    /**
     * Forme utilisée par {@code @PreAuthorize("hasPermission(#id, 'Tender', 'modify')")}.
     * (Signature exacte de {@link PermissionEvaluator}.)
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }
        if (!(targetId instanceof Number n) || !"Tender".equals(targetType)) {
            return false;
        }
        Long tenderId = n.longValue();
        String action = String.valueOf(permission);
        String userId = jwt.getSubject();

        boolean granted = switch (action) {
            case "modify" -> sameDepartment(tenderId, jwt);
            case "approve" -> sameDepartment(tenderId, jwt)
                    && withinApprovalLimit(tenderId, jwt)
                    && duringBusinessHours();
            default -> false;
        };

        if (!granted) {
            audit.logAccessDenied(userId, "Tender#" + tenderId + ":" + action, claim(jwt, "location"));
        }
        return granted;
    }

    /** Forme « objet domaine » de l'interface — non utilisée ici (on passe par l'id). */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return false;
    }

    /** Politique : même département que le créateur de l'offre. */
    public boolean sameDepartment(Long tenderId, Jwt jwt) {
        String userDept = jwt.getClaimAsString("department");
        return tenderRepository.findById(tenderId)
                .map(t -> t.getDepartment() != null && t.getDepartment().equalsIgnoreCase(userDept))
                .orElse(false);
    }

    /** Politique : montant de l'offre &le; plafond d'approbation de l'utilisateur. */
    public boolean withinApprovalLimit(Long tenderId, Jwt jwt) {
        BigDecimal limit = parseAmount(jwt.getClaimAsString("approval_limit"));
        if (limit == null) {
            return false;
        }
        return tenderRepository.findById(tenderId)
                .map(Tender::getAmount)
                .map(amount -> amount.compareTo(limit) <= 0)
                .orElse(false);
    }

    /** Politique contextuelle : heures ouvrables (08:00–18:00). */
    public boolean duringBusinessHours() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(18, 0));
    }

    private static BigDecimal parseAmount(String raw) {
        try {
            return raw == null ? null : new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String claim(Jwt jwt, String name) {
        String v = jwt.getClaimAsString(name);
        return v == null ? "unknown" : v;
    }
}