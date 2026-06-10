package moustapha.nfsgroup.tender.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Chiffrement au repos AES-256-GCM.
 *
 * <p>GCM (Galois/Counter Mode) fournit à la fois la confidentialité et l'authenticité
 * (tag d'intégrité). Un IV aléatoire de 12 octets est généré par opération et préfixé
 * au texte chiffré : {@code base64(IV || ciphertext || tag)}.
 *
 * <p>La clé maîtresse provient des secrets (Vault — 10.2) via la propriété
 * {@code app.encryption.master-key} (clé de 32 octets encodée en Base64).
 */
@Service
public class AesEncryptionService {

    private static final int IV_LENGTH = 12;          // 96 bits recommandés pour GCM
    private static final int TAG_LENGTH_BITS = 128;   // tag d'authentification
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesEncryptionService(@Value("${app.encryption.master-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("La clé AES doit faire 256 bits (32 octets) ; reçu : " + keyBytes.length);
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Échec du chiffrement AES-GCM", e);
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            byte[] ciphertext = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Échec du déchiffrement AES-GCM (intégrité compromise ?)", e);
        }
    }
}