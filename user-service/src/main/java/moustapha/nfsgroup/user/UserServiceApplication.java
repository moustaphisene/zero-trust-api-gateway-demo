package moustapha.nfsgroup.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * user-service — microservice « utilisateurs ».
 *
 * <p>Démontre le partage de la frontière Zero Trust (9.5) : même derrière le Gateway,
 * le service re-valide le JWT et restreint l'administration des utilisateurs au rôle
 * disposant de la permission {@code user:manage} (réservée à ADMIN dans la matrice RBAC).
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}