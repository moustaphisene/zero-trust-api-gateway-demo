package moustapha.nfsgroup.tender.config;

import moustapha.nfsgroup.tender.domain.Tender;
import moustapha.nfsgroup.tender.domain.TenderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Jeu de données de démonstration, aligné sur les attributs ABAC des utilisateurs Keycloak.
 *
 * <p>Permet de tester immédiatement la collection Postman après {@code docker compose up}.
 * Tenant {@code tenant-alpha}, départements ENGINEERING / FINANCE.
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(TenderRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.save(build("Refonte du SI sinistres", "ENGINEERING", "tenant-alpha",
                    new BigDecimal("250000.00"), "manager-seed"));
            repository.save(build("Audit cybersécurité annuel", "ENGINEERING", "tenant-alpha",
                    new BigDecimal("80000.00"), "manager-seed"));
            repository.save(build("Migration data center", "FINANCE", "tenant-alpha",
                    new BigDecimal("750000.00"), "admin-seed"));
            // Offre d'un autre tenant : sert à démontrer la protection BOLA (A01).
            repository.save(build("Projet confidentiel beta", "ENGINEERING", "tenant-beta",
                    new BigDecimal("120000.00"), "beta-owner"));
        };
    }

    private Tender build(String title, String dept, String tenant, BigDecimal amount, String creator) {
        Tender t = new Tender();
        t.setTitle(title);
        t.setDescription("Donnée de démonstration");
        t.setDepartment(dept);
        t.setTenantId(tenant);
        t.setAmount(amount);
        t.setCreatedBy(creator);
        t.setSealedBudget("budget-confidentiel-" + amount);   // sera chiffré AES-256-GCM au repos
        t.setStatus(Tender.TenderStatus.PUBLISHED);
        return t;
    }
}