package moustapha.nfsgroup.tender.config;

import moustapha.nfsgroup.tender.security.AbacPermissionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Active la sécurité au niveau méthode (@PreAuthorize) et branche le PDP ABAC.
 *
 * <p>Le {@link AbacPermissionEvaluator} joue le rôle de
 * <b>PDP (Policy Decision Point)</b>, invoqué depuis les annotations
 * {@code @PreAuthorize("hasPermission(...)")} qui font office de
 * <b>PEP (Policy Enforcement Point)</b>.
 */
@Configuration
@EnableMethodSecurity   // active @PreAuthorize / @PostAuthorize
public class MethodSecurityConfig {

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(AbacPermissionEvaluator abac) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(abac);
        return handler;
    }
}