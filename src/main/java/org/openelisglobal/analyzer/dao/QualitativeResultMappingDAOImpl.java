package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
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
            String hql = "FROM QualitativeResultMapping q WHERE q.analyzerFieldId = :analyzerFieldId";
            Query<QualitativeResultMapping> query = entityManager.unwrap(Session.class).createQuery(hql,
                    QualitativeResultMapping.class);
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
