package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.openelisglobal.analyzer.dao.CustomFieldTypeDAO;
import org.openelisglobal.analyzer.valueholder.CustomFieldType;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CustomFieldTypeService implementation - Manages custom field types with
 * validation rules.
 * 
 * Per FR-018: Custom field types MUST include validation rules (e.g., format
 * patterns, value ranges, allowed characters) and MUST be available for use in
 * field mapping configuration.
 */
@Service
@Transactional
public class CustomFieldTypeServiceImpl extends BaseObjectServiceImpl<CustomFieldType, String>
        implements CustomFieldTypeService {

    @Autowired
    private CustomFieldTypeDAO customFieldTypeDAO;

    public CustomFieldTypeServiceImpl() {
        super(CustomFieldType.class);
    }

    @Override
    protected CustomFieldTypeDAO getBaseObjectDAO() {
        return customFieldTypeDAO;
    }

    @Override
    @Transactional
    public CustomFieldType createCustomFieldType(CustomFieldType customFieldType) {
        if (customFieldType.getValidationPattern() != null && !customFieldType.getValidationPattern().isEmpty()) {
            validateRegexPattern(customFieldType.getValidationPattern());
        }

        if (customFieldType.getValueRangeMin() != null && customFieldType.getValueRangeMax() != null) {
            if (customFieldType.getValueRangeMin().compareTo(customFieldType.getValueRangeMax()) > 0) {
                throw new LIMSRuntimeException("Value range minimum cannot be greater than maximum");
            }
        }

        CustomFieldType existing = customFieldTypeDAO.findByTypeName(customFieldType.getTypeName());
        if (existing != null) {
            throw new LIMSRuntimeException(
                    "Custom field type with name '" + customFieldType.getTypeName() + "' already exists");
        }

        String id = customFieldTypeDAO.insert(customFieldType);
        return customFieldTypeDAO.get(id)
                .orElseThrow(() -> new LIMSRuntimeException("Failed to retrieve created CustomFieldType"));
    }

    @Override
    @Transactional
    public CustomFieldType updateCustomFieldType(CustomFieldType customFieldType) {
        if (customFieldType.getValidationPattern() != null && !customFieldType.getValidationPattern().isEmpty()) {
            validateRegexPattern(customFieldType.getValidationPattern());
        }

        if (customFieldType.getValueRangeMin() != null && customFieldType.getValueRangeMax() != null) {
            if (customFieldType.getValueRangeMin().compareTo(customFieldType.getValueRangeMax()) > 0) {
                throw new LIMSRuntimeException("Value range minimum cannot be greater than maximum");
            }
        }

        CustomFieldType existing = customFieldTypeDAO.findByTypeName(customFieldType.getTypeName());
        if (existing != null && !existing.getId().equals(customFieldType.getId())) {
            throw new LIMSRuntimeException(
                    "Custom field type with name '" + customFieldType.getTypeName() + "' already exists");
        }

        return customFieldTypeDAO.update(customFieldType);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateFieldValue(String value, CustomFieldType customFieldType) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        if (customFieldType.getValidationPattern() != null && !customFieldType.getValidationPattern().isEmpty()) {
            try {
                Pattern pattern = Pattern.compile(customFieldType.getValidationPattern());
                if (!pattern.matcher(value).matches()) {
                    return false;
                }
            } catch (PatternSyntaxException e) {
                throw new LIMSRuntimeException("Invalid regex pattern in custom field type", e);
            }
        }

        // Validate value range if provided (for numeric values)
        if (customFieldType.getValueRangeMin() != null || customFieldType.getValueRangeMax() != null) {
            try {
                java.math.BigDecimal numericValue = new java.math.BigDecimal(value);
                if (customFieldType.getValueRangeMin() != null
                        && numericValue.compareTo(customFieldType.getValueRangeMin()) < 0) {
                    return false;
                }
                if (customFieldType.getValueRangeMax() != null
                        && numericValue.compareTo(customFieldType.getValueRangeMax()) > 0) {
                    return false;
                }
            } catch (NumberFormatException e) {
                // Not a numeric value, skip range validation
            }
        }

        if (customFieldType.getAllowedCharacters() != null && !customFieldType.getAllowedCharacters().isEmpty()) {
            for (char c : value.toCharArray()) {
                if (customFieldType.getAllowedCharacters().indexOf(c) == -1) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomFieldType> getAllActiveTypes() {
        return customFieldTypeDAO.findAllActive();
    }

    private void validateRegexPattern(String pattern) {
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw new LIMSRuntimeException("Invalid regex pattern: " + e.getMessage(), e);
        }
    }
}
