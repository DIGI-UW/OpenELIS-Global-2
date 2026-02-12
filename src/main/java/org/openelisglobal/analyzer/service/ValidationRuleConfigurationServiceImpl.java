package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.dao.ValidationRuleConfigurationDAO;
import org.openelisglobal.analyzer.valueholder.CustomFieldType;
import org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for ValidationRuleConfiguration
 * 
 */
@Service
@Transactional
public class ValidationRuleConfigurationServiceImpl extends BaseObjectServiceImpl<ValidationRuleConfiguration, String>
        implements ValidationRuleConfigurationService {

    @Autowired
    private ValidationRuleConfigurationDAO validationRuleConfigurationDAO;

    @Autowired
    private CustomFieldTypeService customFieldTypeService;

    public ValidationRuleConfigurationServiceImpl() {
        super(ValidationRuleConfiguration.class);
    }

    @Override
    protected ValidationRuleConfigurationDAO getBaseObjectDAO() {
        return validationRuleConfigurationDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidationRuleConfiguration> findByCustomFieldTypeId(String customFieldTypeId) {
        return validationRuleConfigurationDAO.findByCustomFieldTypeId(customFieldTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidationRuleConfiguration> findActiveRulesByCustomFieldTypeId(String customFieldTypeId) {
        return validationRuleConfigurationDAO.findActiveRulesByCustomFieldTypeId(customFieldTypeId);
    }

    @Override
    @Transactional
    public ValidationRuleConfiguration createValidationRule(ValidationRuleConfiguration rule) {
        // Validate custom field type exists
        CustomFieldType customFieldType = customFieldTypeService.get(rule.getCustomFieldType().getId());
        if (customFieldType == null) {
            throw new LIMSRuntimeException("Custom field type not found: " + rule.getCustomFieldType().getId());
        }

        // Validate rule expression format
        validateRuleExpression(rule);

        String id = validationRuleConfigurationDAO.insert(rule);
        return validationRuleConfigurationDAO.get(id)
                .orElseThrow(() -> new LIMSRuntimeException("Failed to retrieve created ValidationRuleConfiguration"));
    }

    @Override
    @Transactional
    public ValidationRuleConfiguration updateValidationRule(ValidationRuleConfiguration rule) {
        // Validate custom field type exists
        CustomFieldType customFieldType = customFieldTypeService.get(rule.getCustomFieldType().getId());
        if (customFieldType == null) {
            throw new LIMSRuntimeException("Custom field type not found: " + rule.getCustomFieldType().getId());
        }

        // Validate rule expression format
        validateRuleExpression(rule);

        return validationRuleConfigurationDAO.update(rule);
    }

    @Override
    @Transactional
    public void deleteValidationRule(ValidationRuleConfiguration rule) {
        validationRuleConfigurationDAO.delete(rule);
    }

    /**
     * Validate rule expression format based on rule type
     */
    private void validateRuleExpression(ValidationRuleConfiguration rule) {
        if (rule.getRuleExpression() == null || rule.getRuleExpression().trim().isEmpty()) {
            throw new LIMSRuntimeException("Rule expression is required");
        }

        try {
            switch (rule.getRuleType()) {
            case REGEX:
                // Validate regex pattern compiles
                java.util.regex.Pattern.compile(rule.getRuleExpression());
                break;
            case RANGE:
                // Validate JSON format: {"min": X, "max": Y}
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>> typeRef = new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                };
                java.util.Map<String, Object> rangeMap = mapper.readValue(rule.getRuleExpression(), typeRef);
                if (!rangeMap.containsKey("min") && !rangeMap.containsKey("max")) {
                    throw new LIMSRuntimeException("Range expression must contain 'min' or 'max'");
                }
                break;
            case ENUM:
                // Validate JSON array format: ["value1", "value2", ...]
                com.fasterxml.jackson.databind.ObjectMapper enumMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>> enumTypeRef = new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {
                };
                java.util.List<String> enumList = enumMapper.readValue(rule.getRuleExpression(), enumTypeRef);
                if (enumList == null || enumList.isEmpty()) {
                    throw new LIMSRuntimeException("Enum expression must contain at least one value");
                }
                break;
            case LENGTH:
                // Validate JSON format: {"minLength": X, "maxLength": Y}
                com.fasterxml.jackson.databind.ObjectMapper lengthMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>> lengthTypeRef = new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                };
                java.util.Map<String, Object> lengthMap = lengthMapper.readValue(rule.getRuleExpression(),
                        lengthTypeRef);
                if (!lengthMap.containsKey("minLength") && !lengthMap.containsKey("maxLength")) {
                    throw new LIMSRuntimeException("Length expression must contain 'minLength' or 'maxLength'");
                }
                break;
            default:
                throw new LIMSRuntimeException("Unknown rule type: " + rule.getRuleType());
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new LIMSRuntimeException("Invalid JSON format in rule expression: " + e.getMessage(), e);
        } catch (java.util.regex.PatternSyntaxException e) {
            throw new LIMSRuntimeException("Invalid regex pattern: " + e.getMessage(), e);
        }
    }
}
