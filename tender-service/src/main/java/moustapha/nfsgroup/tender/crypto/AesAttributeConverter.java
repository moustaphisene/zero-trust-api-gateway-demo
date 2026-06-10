package moustapha.nfsgroup.tender.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Converter JPA appliquant de façon transparente le chiffrement AES-256-GCM (5.3)
 * aux attributs annotés {@code @Convert(converter = AesAttributeConverter.class)}.
 *
 * <p>Les converters JPA ne sont pas gérés par Spring : on relie le bean
 * {@link AesEncryptionService} via un pont statique alimenté au démarrage.
 */
@Component
@Converter
public class AesAttributeConverter implements AttributeConverter<String, String> {

    private static AesEncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(AesEncryptionService service) {
        AesAttributeConverter.encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService == null ? attribute : encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService == null ? dbData : encryptionService.decrypt(dbData);
    }
}