package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerFieldMappingDAOImpl extends BaseDAOImpl<AnalyzerFieldMapping, String>
        implements AnalyzerFieldMappingDAO {

    public AnalyzerFieldMappingDAOImpl() {
        super(AnalyzerFieldMapping.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerFieldMapping> findByAnalyzerFieldId(String analyzerFieldId) {
        try {
            String hql = "FROM AnalyzerFieldMapping WHERE analyzerField.id = :analyzerFieldId";
            Query<AnalyzerFieldMapping> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerFieldMapping.class);
            query.setParameter("analyzerFieldId", analyzerFieldId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerFieldMapping by analyzer field ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerFieldMapping> findActiveMappingsByAnalyzerId(String analyzerId) {
        try {
            String hql = "FROM AnalyzerFieldMapping afm " + "WHERE afm.analyzerField.analyzer.id = :analyzerId "
                    + "AND afm.isActive = true";
            Query<AnalyzerFieldMapping> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerFieldMapping.class);
            query.setParameter("analyzerId", analyzerId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding active AnalyzerFieldMapping by analyzer ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerFieldMapping> findByAnalyzerIdWithFields(String analyzerId) {
        try {
            Integer analyzerIdInt;
            try {
                analyzerIdInt = Integer.parseInt(analyzerId);
            } catch (NumberFormatException e) {
                throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
            }

            // HQL join via relationship path; eagerly load analyzerField
            String hql = "SELECT DISTINCT afm FROM AnalyzerFieldMapping afm " + "LEFT JOIN FETCH afm.analyzerField af "
                    + "JOIN af.analyzer a " + "WHERE a.id = :analyzerId";
            Query<AnalyzerFieldMapping> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerFieldMapping.class);
            query.setParameter("analyzerId", analyzerIdInt);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerFieldMapping by analyzer ID with fields", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerFieldMapping findByIdWithField(String mappingId) {
        try {
            // Use JOIN FETCH to eagerly load analyzerField relationship within transaction
            // Note: Cannot JOIN FETCH analyzer (uses XML mappings), but analyzerField is
            // enough
            // The analyzer relationship can be accessed via analyzerField.getAnalyzer() if
            // needed
            String hql = "SELECT afm FROM AnalyzerFieldMapping afm " + "LEFT JOIN FETCH afm.analyzerField af "
                    + "WHERE afm.id = :mappingId";
            Query<AnalyzerFieldMapping> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerFieldMapping.class);
            query.setParameter("mappingId", mappingId);
            return query.uniqueResult();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerFieldMapping by ID with field", e);
        }
    }
}
