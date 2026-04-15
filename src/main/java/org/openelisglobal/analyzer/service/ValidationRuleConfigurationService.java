package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service interface for ValidationRuleConfiguration
 * 
 */
public interface ValidationRuleConfigurationService extends BaseObjectService<ValidationRuleConfiguration, String> {
    List<ValidationRuleConfiguration> findByCustomFieldTypeId(String customFieldTypeId);

    List<ValidationRuleConfiguration> findActiveRulesByCustomFieldTypeId(String customFieldTypeId);

    ValidationRuleConfiguration createValidationRule(ValidationRuleConfiguration rule);

    ValidationRuleConfiguration updateValidationRule(ValidationRuleConfiguration rule);

    void deleteValidationRule(ValidationRuleConfiguration rule);
}
