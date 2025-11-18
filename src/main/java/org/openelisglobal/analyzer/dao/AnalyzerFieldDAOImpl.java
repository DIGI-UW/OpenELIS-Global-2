package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerFieldDAOImpl extends BaseDAOImpl<AnalyzerField, String> implements AnalyzerFieldDAO {

    public AnalyzerFieldDAOImpl() {
        super(AnalyzerField.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerField> findByAnalyzerId(String analyzerId) {
        try {
            // Analyzer entity uses XML mappings with NUMERIC id, so we use native SQL
            // to query by the analyzer_id foreign key column directly
            Integer analyzerIdInt;
            try {
                analyzerIdInt = Integer.parseInt(analyzerId);
            } catch (NumberFormatException e) {
                throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
            }
            
            // Use native SQL to get analyzer field IDs, then fetch entities with HQL
            String sql = "SELECT id FROM analyzer_field WHERE analyzer_id = :analyzerId";
            @SuppressWarnings("unchecked")
            List<Object> rawResults = entityManager.createNativeQuery(sql)
                    .setParameter("analyzerId", analyzerIdInt)
                    .getResultList();
            
            // Convert Object results to String IDs
            List<String> fieldIds = new java.util.ArrayList<>();
            for (Object result : rawResults) {
                if (result != null) {
                    fieldIds.add(result.toString());
                }
            }
            
            if (fieldIds.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            
            // Fetch entities by IDs using HQL (works with annotation-based entities)
            String hql = "FROM AnalyzerField WHERE id IN :fieldIds";
            Query<AnalyzerField> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerField.class);
            query.setParameterList("fieldIds", fieldIds);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerField by analyzer ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<AnalyzerField> findByIdWithAnalyzer(String analyzerFieldId) {
        try {
            // Use JOIN FETCH to eagerly load analyzer relationship within transaction
            String hql = "SELECT af FROM AnalyzerField af " +
                    "LEFT JOIN FETCH af.analyzer a " +
                    "WHERE af.id = :analyzerFieldId";
            Query<AnalyzerField> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerField.class);
            query.setParameter("analyzerFieldId", analyzerFieldId);
            AnalyzerField result = query.uniqueResult();
            return java.util.Optional.ofNullable(result);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerField by ID with analyzer", e);
        }
    }
}

