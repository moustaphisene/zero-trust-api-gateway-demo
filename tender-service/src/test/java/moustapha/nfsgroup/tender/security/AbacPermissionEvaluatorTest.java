package moustapha.nfsgroup.tender.security;

import moustapha.nfsgroup.tender.domain.Tender;
import moustapha.nfsgroup.tender.domain.TenderRepository;
import moustapha.nfsgroup.tender.observability.SecurityEventLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du PDP ABAC : politiques same-department et within-approval-limit.
 */
class AbacPermissionEvaluatorTest {

    private TenderRepository repository;
    private AbacPermissionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        repository = mock(TenderRepository.class);
        evaluator = new AbacPermissionEvaluator(repository, mock(SecurityEventLogger.class));
    }

    private Jwt jwt(String department, String approvalLimit) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .claim("department", department)
                .claim("approval_limit", approvalLimit)
                .claim("location", "DAKAR")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    private Tender tender(String department, BigDecimal amount) {
        Tender t = new Tender();
        t.setId(1L);
        t.setDepartment(department);
        t.setAmount(amount);
        t.setTenantId("tenant-alpha");
        return t;
    }

    private Authentication auth(Jwt jwt) {
        return new TestingAuthenticationToken(jwt, null);
    }

    @Test
    void sameDepartment_vrai_quand_departements_identiques() {
        when(repository.findById(1L)).thenReturn(Optional.of(tender("ENGINEERING", new BigDecimal("100"))));
        assertThat(evaluator.sameDepartment(1L, jwt("ENGINEERING", "500000"))).isTrue();
    }

    @Test
    void sameDepartment_faux_quand_departements_differents() {
        when(repository.findById(1L)).thenReturn(Optional.of(tender("FINANCE", new BigDecimal("100"))));
        assertThat(evaluator.sameDepartment(1L, jwt("ENGINEERING", "500000"))).isFalse();
    }

    @Test
    void withinApprovalLimit_vrai_quand_montant_sous_le_plafond() {
        when(repository.findById(1L)).thenReturn(Optional.of(tender("ENGINEERING", new BigDecimal("250000"))));
        assertThat(evaluator.withinApprovalLimit(1L, jwt("ENGINEERING", "500000"))).isTrue();
    }

    @Test
    void withinApprovalLimit_faux_quand_montant_depasse_le_plafond() {
        when(repository.findById(1L)).thenReturn(Optional.of(tender("ENGINEERING", new BigDecimal("750000"))));
        assertThat(evaluator.withinApprovalLimit(1L, jwt("ENGINEERING", "500000"))).isFalse();
    }

    @Test
    void hasPermission_modify_respecte_la_politique_same_department() {
        when(repository.findById(1L)).thenReturn(Optional.of(tender("ENGINEERING", new BigDecimal("100"))));

        assertThat(evaluator.hasPermission(auth(jwt("ENGINEERING", "0")), 1L, "Tender", "modify")).isTrue();
        assertThat(evaluator.hasPermission(auth(jwt("FINANCE", "0")), 1L, "Tender", "modify")).isFalse();
    }

    @Test
    void hasPermission_refuse_type_cible_inconnu() {
        assertThat(evaluator.hasPermission(auth(jwt("ENGINEERING", "0")), 1L, "Autre", "modify")).isFalse();
    }
}