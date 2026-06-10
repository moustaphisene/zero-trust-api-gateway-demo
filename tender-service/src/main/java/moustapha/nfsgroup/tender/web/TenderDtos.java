package moustapha.nfsgroup.tender.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import moustapha.nfsgroup.tender.domain.Tender;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Objets de transfert.
 *
 * <p><b>OWASP A03 — Broken Object Property Level Authorization</b> : on n'expose
 * jamais l'entité JPA directement et on n'accepte jamais d'objet « brut » du client.
 * Le DTO d'entrée ne contient QUE les champs modifiables (pas de {@code tenantId},
 * {@code createdBy}, {@code status}) — protection contre le <i>mass assignment</i>.
 */
public final class TenderDtos {

    private TenderDtos() {
    }

    /** Entrée création/mise à jour — champs strictement validés. */
    public record CreateTenderRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
            @NotBlank @Size(max = 64) String department,
            @Size(max = 1024) String sealedBudget
    ) {
    }

    /** Sortie — vue maîtrisée de l'entité. */
    public record TenderResponse(
            Long id,
            String title,
            String description,
            BigDecimal amount,
            String department,
            String tenantId,
            String createdBy,
            String status,
            Instant createdAt
    ) {
        public static TenderResponse from(Tender t) {
            return new TenderResponse(
                    t.getId(), t.getTitle(), t.getDescription(), t.getAmount(),
                    t.getDepartment(), t.getTenantId(), t.getCreatedBy(),
                    t.getStatus().name(), t.getCreatedAt());
        }
    }
}