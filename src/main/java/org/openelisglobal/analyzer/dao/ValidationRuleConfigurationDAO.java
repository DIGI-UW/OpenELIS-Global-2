package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for ValidationRuleConfiguration
 * 
 */
public interface ValidationRuleConfigurationDAO extends BaseDAO<ValidationRuleConfiguration, String> {
    List<ValidationRuleConfiguration> findByCustomFieldTypeId(String customFieldTypeId);

    List<ValidationRuleConfiguration> findActiveRulesByCustomFieldTypeId(String customFieldTypeId);
}
