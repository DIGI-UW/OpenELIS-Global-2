package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service interface for ValidationRuleConfiguration
 * 
 */
@PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
public interface ValidationRuleConfigurationService extends BaseObjectService<ValidationRuleConfiguration, String> {
    List<ValidationRuleConfiguration> findByCustomFieldTypeId(String customFieldTypeId);

    List<ValidationRuleConfiguration> findActiveRulesByCustomFieldTypeId(String customFieldTypeId);

    ValidationRuleConfiguration createValidationRule(ValidationRuleConfiguration rule);

    ValidationRuleConfiguration updateValidationRule(ValidationRuleConfiguration rule);

    void deleteValidationRule(ValidationRuleConfiguration rule);
}
