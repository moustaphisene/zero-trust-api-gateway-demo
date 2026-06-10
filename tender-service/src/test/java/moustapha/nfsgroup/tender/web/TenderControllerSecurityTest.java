package moustapha.nfsgroup.tender.web;

import moustapha.nfsgroup.tender.config.MethodSecurityConfig;
import moustapha.nfsgroup.tender.domain.Tender;
import moustapha.nfsgroup.tender.domain.TenderRepository;
import moustapha.nfsgroup.tender.observability.SecurityEventLogger;
import moustapha.nfsgroup.tender.observability.SecurityMetrics;
import moustapha.nfsgroup.tender.security.AbacPermissionEvaluator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de sécurité du contrôleur (RBAC + ABAC) via slice MVC + spring-security-test.
 * L'authentification est simulée par le post-processor {@code jwt()} : aucun Keycloak requis.
 */
@WebMvcTest(TenderController.class)
@Import(MethodSecurityConfig.class)
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.config.import=",
        "spring.cloud.vault.enabled=false"
})
class TenderControllerSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    TenderRepository repository;
    @MockBean
    SecurityEventLogger audit;
    @MockBean
    SecurityMetrics metrics;
    @MockBean
    AbacPermissionEvaluator abac;

    private static final String BODY = """
            {"title":"Srv","amount":150000,"department":"ENGINEERING"}""";

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asRole(String role, String... extra) {
        java.util.List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        for (String e : extra) authorities.add(new SimpleGrantedAuthority(e));
        return jwt()
                .jwt(j -> j.subject("user-1").claim("tenant_id", "tenant-alpha"))
                .authorities(authorities);
    }

    private Tender tenderAlpha() {
        Tender t = new Tender();
        t.setId(1L);
        t.setDepartment("ENGINEERING");
        t.setAmount(new BigDecimal("100000"));
        t.setTenantId("tenant-alpha");
        return t;
    }

    // ---------- RBAC ----------

    @Test
    void lecture_sans_token_renvoie_401() throws Exception {
        mvc.perform(get("/api/tenders")).andExpect(status().isUnauthorized());
    }

    @Test
    void lecture_avec_role_USER_renvoie_200() throws Exception {
        when(repository.findByTenantId("tenant-alpha")).thenReturn(List.of());
        mvc.perform(get("/api/tenders").with(asRole("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void creation_par_USER_est_interdite_403() throws Exception {
        mvc.perform(post("/api/tenders").with(asRole("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void creation_par_MANAGER_avec_permission_renvoie_201() throws Exception {
        when(repository.save(any(Tender.class))).thenAnswer(inv -> {
            Tender t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });
        mvc.perform(post("/api/tenders").with(asRole("MANAGER", "tender:write")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void suppression_par_MANAGER_est_interdite_403() throws Exception {
        mvc.perform(delete("/api/tenders/1").with(asRole("MANAGER", "tender:write")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void suppression_par_ADMIN_avec_permission_renvoie_204() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(tenderAlpha()));
        mvc.perform(delete("/api/tenders/1").with(asRole("ADMIN", "tender:delete")).with(csrf()))
                .andExpect(status().isNoContent());
    }

    // ---------- ABAC ----------

    @Test
    void modification_autorisee_quand_ABAC_accorde() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(tenderAlpha()));
        when(abac.hasPermission(any(), any(), eq("Tender"), eq("modify"))).thenReturn(true);
        when(repository.save(any(Tender.class))).thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(put("/api/tenders/1").with(asRole("MANAGER", "tender:write")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    void modification_refusee_quand_ABAC_refuse_403() throws Exception {
        when(abac.hasPermission(any(), any(), eq("Tender"), eq("modify"))).thenReturn(false);

        mvc.perform(put("/api/tenders/1").with(asRole("MANAGER", "tender:write")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    // ---------- BOLA / multi-tenant ----------

    @Test
    void acces_cross_tenant_renvoie_404() throws Exception {
        Tender other = tenderAlpha();
        other.setTenantId("tenant-beta");
        when(repository.findById(1L)).thenReturn(Optional.of(other));

        mvc.perform(get("/api/tenders/1").with(asRole("USER")))
                .andExpect(status().isNotFound());
    }
}