package moustapha.nfsgroup.tender.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository JPA des appels d'offres.
 *
 * <p><b>OWASP A03 — Injection (4.5)</b> : toutes les requêtes utilisent des
 * <i>paramètres liés</i> ({@code :tenantId}). On ne concatène jamais de chaîne
 * utilisateur dans une requête JPQL/SQL (cf. exemple vulnérable du §4.5.2).
 */
public interface TenderRepository extends JpaRepository<Tender, Long> {

    /** Isolation multi-tenant : un locataire ne voit que ses propres offres (BOLA / A01 — 4.2). */
    List<Tender> findByTenantId(String tenantId);

    @Query("select t from Tender t where t.tenantId = :tenantId and t.department = :department")
    List<Tender> findByTenantAndDepartment(@Param("tenantId") String tenantId,
                                           @Param("department") String department);
}