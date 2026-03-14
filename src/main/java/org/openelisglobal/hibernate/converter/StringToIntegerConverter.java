package org.openelisglobal.hibernate.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class StringToIntegerConverter implements AttributeConverter<String, Integer> {

    @Override
    public Integer convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return Integer.valueOf(attribute);
    }

    @Override
    public String convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return dbData.toString();
    }
}
