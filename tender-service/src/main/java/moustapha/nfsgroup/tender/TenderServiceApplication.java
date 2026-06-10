package moustapha.nfsgroup.tender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * tender-service — microservice « appels d'offres ».
 *
 * <p>Bien qu'il soit situé derrière le Gateway, ce service applique le principe
 * <b>Zero Trust (9.5)</b> : il re-valide systématiquement le JWT (issuer, signature,
 * expiration) et applique le contrôle d'accès <b>RBAC (2.4.1)</b> et <b>ABAC (2.4.2)</b>.
 */
@SpringBootApplication
public class TenderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenderServiceApplication.class, args);
    }
}