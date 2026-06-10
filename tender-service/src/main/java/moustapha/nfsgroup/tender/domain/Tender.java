package moustapha.nfsgroup.tender.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Appel d'offres (tender).
 *
 * <p>Le champ {@code sealedBudget} illustre le <b>chiffrement au repos AES-256-GCM (5.3)</b> :
 * il est converti de façon transparente par {@code AesAttributeConverter} avant persistance.
 */
@Entity
@Table(name = "tenders")
public class Tender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    /** Montant public de l'offre (sert à l'ABAC : plafond d'approbation). */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    /** Département propriétaire (sert à l'ABAC : same-department). */
    @Column(nullable = false)
    private String department;

    /** Cloisonnement multi-tenant (10.x) : isolation par locataire. */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** Sujet (sub) Keycloak du créateur. */
    @Column(name = "created_by")
    private String createdBy;

    /** Donnée sensible chiffrée au repos (AES-256-GCM via converter). */
    @Convert(converter = moustapha.nfsgroup.tender.crypto.AesAttributeConverter.class)
    @Column(name = "sealed_budget", length = 1024)
    private String sealedBudget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenderStatus status = TenderStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum TenderStatus { DRAFT, PUBLISHED, APPROVED, CLOSED }

    // --- Getters / setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getSealedBudget() { return sealedBudget; }
    public void setSealedBudget(String sealedBudget) { this.sealedBudget = sealedBudget; }
    public TenderStatus getStatus() { return status; }
    public void setStatus(TenderStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}