package moustapha.nfsgroup.tender.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du chiffrement AES-256-GCM au repos.
 */
class AesEncryptionServiceTest {

    // Clé de 32 octets (256 bits) encodée en Base64, dédiée aux tests.
    private static final String KEY = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private final AesEncryptionService service = new AesEncryptionService(KEY);

    @Test
    void chiffre_puis_dechiffre_restitue_le_clair() {
        String clear = "budget-confidentiel-250000";
        String cipher = service.encrypt(clear);

        assertThat(cipher).isNotNull().isNotEqualTo(clear);
        assertThat(service.decrypt(cipher)).isEqualTo(clear);
    }

    @Test
    void deux_chiffrements_du_meme_clair_different_grace_a_un_IV_aleatoire() {
        String clear = "donnée";
        assertThat(service.encrypt(clear)).isNotEqualTo(service.encrypt(clear));
    }

    @Test
    void null_reste_null() {
        assertThat(service.encrypt(null)).isNull();
        assertThat(service.decrypt(null)).isNull();
    }

    @Test
    void texte_altere_echoue_au_dechiffrement_integrite_GCM() {
        String cipher = service.encrypt("intègre");
        // Corruption d'un caractère du texte chiffré -> le tag GCM doit rejeter.
        String tampered = cipher.substring(0, cipher.length() - 2)
                + (cipher.endsWith("A") ? "B" : "A") + "=";
        assertThatThrownBy(() -> service.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cle_de_mauvaise_taille_est_rejetee() {
        String shortKey = Base64.getEncoder().encodeToString("trop-courte".getBytes());
        assertThatThrownBy(() -> new AesEncryptionService(shortKey))
                .isInstanceOf(IllegalStateException.class);
    }
}