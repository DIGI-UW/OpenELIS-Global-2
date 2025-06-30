package org.openelisglobal.security.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.apache.commons.validator.GenericValidator;
import org.jasypt.util.text.TextEncryptor;
import org.openelisglobal.spring.util.SpringContext;

@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (GenericValidator.isBlankOrNull(attribute)) {
            return attribute;
        }
        return SpringContext.getBean(TextEncryptor.class).encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (GenericValidator.isBlankOrNull(dbData)) {
            return dbData;
        }
        return SpringContext.getBean(TextEncryptor.class).decrypt(dbData);
    }
}
