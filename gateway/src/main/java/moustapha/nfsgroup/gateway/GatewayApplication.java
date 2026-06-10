package moustapha.nfsgroup.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway — point d'entrée unique et frontière de confiance du système.
 *
 * <p>Responsabilités sécuritaires (réf. 2.6.2 de l'ouvrage) :
 * <ul>
 *   <li>Terminaison TLS / HTTPS (1.3) ;</li>
 *   <li>Validation des JWT émis par Keycloak (2.6.4) ;</li>
 *   <li>Rate limiting et protection DDoS via Redis (2.6.5) ;</li>
 *   <li>Application des en-têtes HTTP de sécurité (1.2.3) ;</li>
 *   <li>Propagation contrôlée de l'identité vers les microservices (Zero Trust, 9.5).</li>
 * </ul>
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}