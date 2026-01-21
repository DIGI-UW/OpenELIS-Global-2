package org.openelisglobal.biorepository.valueholder;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for BiosafetyLevel enum. Stores the display value (e.g.,
 * "BSL-1") in the database instead of the enum name (e.g., "BSL_1").
 */
@Converter(autoApply = false)
public class BiosafetyLevelConverter implements AttributeConverter<BioSample.BiosafetyLevel, String> {

    @Override
    public String convertToDatabaseColumn(BioSample.BiosafetyLevel attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDisplayValue();
    }

    @Override
    public BioSample.BiosafetyLevel convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        return BioSample.BiosafetyLevel.fromString(dbData);
    }
}
