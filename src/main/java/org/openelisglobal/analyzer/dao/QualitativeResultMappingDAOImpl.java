package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.analyzer.valueholder.QualitativeResultMapping;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for QualitativeResultMapping. Entity uses JPA annotations.
 */
@Component
@Transactional
public class QualitativeResultMappingDAOImpl extends BaseDAOImpl<QualitativeResultMapping, String>
        implements QualitativeResultMappingDAO {

    public QualitativeResultMappingDAOImpl() {
        super(QualitativeResultMapping.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualitativeResultMapping> findByAnalyzerFieldId(String analyzerFieldId) {
        try {
            // Use native SQL to avoid HQL property name resolution issues with XML mappings
            // XML mappings define column as analyzer_field_id, but HQL resolves
            // analyzerFieldId to analyzerfieldid
            String sql = "SELECT * FROM qualitative_result_mapping WHERE analyzer_field_id = :analyzerFieldId";
            Session session = entityManager.unwrap(Session.class);
            @SuppressWarnings("unchecked")
            org.hibernate.query.NativeQuery<QualitativeResultMapping> query = (org.hibernate.query.NativeQuery<QualitativeResultMapping>) session
                    .createNativeQuery(sql).addEntity(QualitativeResultMapping.class);
            query.setParameter("analyzerFieldId", analyzerFieldId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding QualitativeResultMapping by analyzer field ID", e);
        }
    }

    @Override
    public String insert(QualitativeResultMapping mapping) {
        // @PrePersist in entity handles UUID and lastupdated
        return super.insert(mapping);
    }
}
