package moustapha.nfsgroup.tender.web;

import jakarta.validation.Valid;
import moustapha.nfsgroup.tender.domain.Tender;
import moustapha.nfsgroup.tender.domain.TenderRepository;
import moustapha.nfsgroup.tender.observability.SecurityEventLogger;
import moustapha.nfsgroup.tender.observability.SecurityMetrics;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

import static moustapha.nfsgroup.tender.web.TenderDtos.*;

/**
 * API « appels d'offres ».
 *
 * <p>Démontre la matrice RBAC (Table 2.4) et les politiques ABAC (2.4.2) :
 * <pre>
 *   Action / Rôle        ADMIN  MANAGER  USER  AUDITOR
 *   Lire les offres       Oui     Oui    Oui    Oui
 *   Créer une offre       Oui     Oui    Non    Non
 *   Modifier une offre    Oui     Oui*   Non    Non      (* + ABAC same-department)
 *   Approuver             Oui     Oui*   Non    Non      (* + ABAC limit & horaires)
 *   Supprimer             Oui     Non    Non    Non
 * </pre>
 *
 * <p><b>BOLA / OWASP A01 (4.2)</b> : chaque accès par identifiant vérifie que l'offre
 * appartient bien au tenant de l'appelant — on ne se fie jamais au seul ID fourni.
 */
@RestController
@RequestMapping("/api/tenders")
public class TenderController {

    private final TenderRepository repository;
    private final SecurityEventLogger audit;
    private final SecurityMetrics metrics;

    public TenderController(TenderRepository repository, SecurityEventLogger audit, SecurityMetrics metrics) {
        this.repository = repository;
        this.audit = audit;
        this.metrics = metrics;
    }

    /** Lecture : tous les rôles authentifiés. Résultat borné au tenant (multi-tenancy). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER','AUDITOR')")
    public List<TenderResponse> list(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = tenant(jwt);
        audit.logDataAccess(jwt.getSubject(), "Tender", "LIST");
        metrics.recordDataAccess();
        return repository.findByTenantId(tenantId).stream().map(TenderResponse::from).toList();
    }

    /** Lecture d'une offre — avec contrôle BOLA (isolation tenant). */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER','AUDITOR')")
    public TenderResponse getOne(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Tender tender = loadWithTenantCheck(id, jwt);
        audit.logDataAccess(jwt.getSubject(), "Tender#" + id, "READ");
        return TenderResponse.from(tender);
    }

    /** Création : ADMIN ou MANAGER (permission fine tender:write). */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('tender:write')")
    public ResponseEntity<TenderResponse> create(@Valid @RequestBody CreateTenderRequest req,
                                                 @AuthenticationPrincipal Jwt jwt) {
        Tender tender = new Tender();
        tender.setTitle(req.title());
        tender.setDescription(req.description());
        tender.setAmount(req.amount());
        tender.setDepartment(req.department());
        tender.setSealedBudget(req.sealedBudget());
        // Champs sensibles imposés par le serveur (jamais par le client) — anti mass-assignment.
        tender.setTenantId(tenant(jwt));
        tender.setCreatedBy(jwt.getSubject());

        Tender saved = repository.save(tender);
        audit.logDataAccess(jwt.getSubject(), "Tender#" + saved.getId(), "CREATE");
        return ResponseEntity.created(URI.create("/api/tenders/" + saved.getId()))
                .body(TenderResponse.from(saved));
    }

    /** Modification : RBAC (ADMIN/MANAGER) + ABAC (même département que l'offre). */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasPermission(#id, 'Tender', 'modify')")
    public TenderResponse update(@PathVariable Long id,
                                 @Valid @RequestBody CreateTenderRequest req,
                                 @AuthenticationPrincipal Jwt jwt) {
        Tender tender = loadWithTenantCheck(id, jwt);
        tender.setTitle(req.title());
        tender.setDescription(req.description());
        tender.setAmount(req.amount());
        tender.setSealedBudget(req.sealedBudget());
        Tender saved = repository.save(tender);
        audit.logDataAccess(jwt.getSubject(), "Tender#" + id, "UPDATE");
        return TenderResponse.from(saved);
    }

    /** Approbation : ABAC complet (même département + montant ≤ plafond + heures ouvrables). */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasPermission(#id, 'Tender', 'approve')")
    public TenderResponse approve(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Tender tender = loadWithTenantCheck(id, jwt);
        tender.setStatus(Tender.TenderStatus.APPROVED);
        Tender saved = repository.save(tender);
        audit.logDataAccess(jwt.getSubject(), "Tender#" + id, "APPROVE");
        return TenderResponse.from(saved);
    }

    /** Suppression : ADMIN uniquement (permission fine tender:delete). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('tender:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Tender tender = loadWithTenantCheck(id, jwt);
        repository.delete(tender);
        audit.logDataAccess(jwt.getSubject(), "Tender#" + id, "DELETE");
        return ResponseEntity.noContent().build();
    }

    // --- Helpers ---

    /** Charge l'offre en garantissant l'isolation tenant (protection BOLA / A01). */
    private Tender loadWithTenantCheck(Long id, Jwt jwt) {
        Tender tender = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));
        if (!tender.getTenantId().equals(tenant(jwt))) {
            // Tentative d'accès cross-tenant : on journalise et on répond 404 (pas 403 — anti-énumération).
            metrics.recordAccessDenied();
            audit.logSuspiciousActivity(jwt.getSubject(),
                    "Tentative d'acces cross-tenant sur Tender#" + id, jwt.getClaimAsString("location"));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable");
        }
        return tender;
    }

    private static String tenant(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        return tenantId != null ? tenantId : "default";
    }
}