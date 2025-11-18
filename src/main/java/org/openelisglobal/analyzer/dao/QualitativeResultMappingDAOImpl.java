package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.analyzer.valueholder.QualitativeResultMapping;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
            String hql = "FROM QualitativeResultMapping WHERE analyzerField.id = :analyzerFieldId";
            Query<QualitativeResultMapping> query = entityManager.unwrap(Session.class).createQuery(hql,
                    QualitativeResultMapping.class);
            query.setParameter("analyzerFieldId", analyzerFieldId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding QualitativeResultMapping by analyzer field ID", e);
        }
    }
}

