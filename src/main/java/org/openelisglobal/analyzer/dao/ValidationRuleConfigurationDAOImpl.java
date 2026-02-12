package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for ValidationRuleConfiguration
 * 
 */
@Component
@Transactional
public class ValidationRuleConfigurationDAOImpl extends BaseDAOImpl<ValidationRuleConfiguration, String>
        implements ValidationRuleConfigurationDAO {

    public ValidationRuleConfigurationDAOImpl() {
        super(ValidationRuleConfiguration.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidationRuleConfiguration> findByCustomFieldTypeId(String customFieldTypeId) {
        try {
            String sql = "FROM ValidationRuleConfiguration vrc WHERE vrc.customFieldType.id = :customFieldTypeId";
            Query<ValidationRuleConfiguration> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ValidationRuleConfiguration.class);
            query.setParameter("customFieldTypeId", customFieldTypeId);
            return query.list();
        } catch (Exception e) {
            handleException(e, "findByCustomFieldTypeId");
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidationRuleConfiguration> findActiveRulesByCustomFieldTypeId(String customFieldTypeId) {
        try {
            String sql = "FROM ValidationRuleConfiguration vrc WHERE vrc.customFieldType.id = :customFieldTypeId AND vrc.isActive = true";
            Query<ValidationRuleConfiguration> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ValidationRuleConfiguration.class);
            query.setParameter("customFieldTypeId", customFieldTypeId);
            return query.list();
        } catch (Exception e) {
            handleException(e, "findActiveRulesByCustomFieldTypeId");
            return null;
        }
    }
}
