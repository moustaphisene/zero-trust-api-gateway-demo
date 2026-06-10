package moustapha.nfsgroup.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway — point d'entrée unique et frontière de confiance du système.
 *
 * <p>Responsabilités sécuritaires :
 * <ul>
 *   <li>Terminaison TLS / HTTPS ;</li>
 *   <li>Validation des JWT émis par Keycloak ;</li>
 *   <li>Rate limiting et protection DDoS via Redis ;</li>
 *   <li>Application des en-têtes HTTP de sécurité ;</li>
 *   <li>Propagation contrôlée de l'identité vers les microservices (Zero Trust, 9.5).</li>
 * </ul>
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}