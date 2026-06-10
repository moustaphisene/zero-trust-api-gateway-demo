package moustapha.nfsgroup.user.web;

import moustapha.nfsgroup.user.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de sécurité du user-service (RBAC user:manage) via slice MVC + la vraie SecurityConfig.
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerSecurityTest {

    @Autowired
    MockMvc mvc;

    // Le Resource Server exige un JwtDecoder ; l'auth réelle est simulée par jwt().
    @MockBean
    JwtDecoder jwtDecoder;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor as(String... authorities) {
        List<GrantedAuthority> list = java.util.Arrays.stream(authorities)
                .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a)).toList();
        return jwt().jwt(j -> j.subject("user-1").claim("preferred_username", "user")).authorities(list);
    }

    @Test
    void me_accessible_a_tout_utilisateur_authentifie() throws Exception {
        mvc.perform(get("/api/users/me").with(as("ROLE_USER")))
                .andExpect(status().isOk());
    }

    @Test
    void me_sans_token_renvoie_401() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void liste_users_autorisee_avec_permission_user_manage() throws Exception {
        mvc.perform(get("/api/users").with(as("ROLE_ADMIN", "user:manage")))
                .andExpect(status().isOk());
    }

    @Test
    void liste_users_interdite_sans_user_manage_403() throws Exception {
        mvc.perform(get("/api/users").with(as("ROLE_USER")))
                .andExpect(status().isForbidden());
    }
}