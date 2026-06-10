package moustapha.nfsgroup.tender.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'intégration : PostgreSQL réel (Testcontainers) + contexte Spring complet.
 *
 * <p>Vérifie la chaîne tender-service de bout en bout (hors Keycloak/Gateway) :
 * persistance JPA, RBAC via la vraie {@code SecurityConfig}, isolation multi-tenant,
 * et chiffrement AES-256-GCM au repos (la colonne {@code sealed_budget} contient du chiffré).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
// Pas de Vault en test : on neutralise l'import Vault dès la phase config-data
// (un @DynamicPropertySource serait évalué trop tard pour spring.config.import).
@TestPropertySource(properties = {
        "spring.config.import=",
        "spring.cloud.vault.enabled=false"
})
class TenderPersistenceIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("tender_db")
                    .withUsername("tender_user")
                    .withPassword("tender_pass");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Pas de Vault en test : clé AES fournie directement.
        registry.add("spring.cloud.vault.enabled", () -> "false");
        registry.add("app.encryption.master-key",
                () -> Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes()));
        // jwk-set-uri inutilisé (auth simulée par jwt()), mais doit être présent pour le contexte.
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:0/certs");
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor userAlpha() {
        return jwt().jwt(j -> j.subject("user-1").claim("tenant_id", "tenant-alpha"))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    void liste_bornee_au_tenant_de_lutilisateur() throws Exception {
        // Le DataSeeder a inséré 3 offres tenant-alpha + 1 tenant-beta.
        mvc.perform(get("/api/tenders").with(userAlpha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].tenantId", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("tenant-alpha"))));
    }

    @Test
    void sealedBudget_est_chiffre_au_repos_en_base() {
        List<String> raw = jdbc.queryForList(
                "select sealed_budget from tenders where sealed_budget is not null", String.class);
        assertThat(raw).isNotEmpty();
        // La valeur stockée ne doit jamais contenir le clair "budget-confidentiel".
        assertThat(raw).allSatisfy(v ->
                assertThat(v).doesNotContain("budget-confidentiel"));
    }
}