package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
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
            String hql = "FROM AnalyzerFieldMapping afm " +
                    "WHERE afm.analyzerField.analyzer.id = :analyzerId " +
                    "AND afm.isActive = true";
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
            // Step 1: Use native SQL to get analyzer_field_ids for this analyzer
            // (Analyzer uses XML mappings, so we query by foreign key column directly)
            Integer analyzerIdInt;
            try {
                analyzerIdInt = Integer.parseInt(analyzerId);
            } catch (NumberFormatException e) {
                throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
            }
            
            String sql = "SELECT id FROM analyzer_field WHERE analyzer_id = :analyzerId";
            @SuppressWarnings("unchecked")
            List<Object> rawResults = entityManager.createNativeQuery(sql)
                    .setParameter("analyzerId", analyzerIdInt)
                    .getResultList();
            
            List<String> fieldIds = new java.util.ArrayList<>();
            for (Object result : rawResults) {
                if (result != null) {
                    fieldIds.add(result.toString());
                }
            }
            
            if (fieldIds.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            
            // Step 2: Fetch mappings for these fields with JOIN FETCH to eagerly load analyzerField
            // (No need to JOIN FETCH analyzer - we'll access it separately if needed)
            String hql = "SELECT DISTINCT afm FROM AnalyzerFieldMapping afm " +
                    "LEFT JOIN FETCH afm.analyzerField af " +
                    "WHERE af.id IN :fieldIds";
            Query<AnalyzerFieldMapping> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerFieldMapping.class);
            query.setParameterList("fieldIds", fieldIds);
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
            // Note: Cannot JOIN FETCH analyzer (uses XML mappings), but analyzerField is enough
            // The analyzer relationship can be accessed via analyzerField.getAnalyzer() if needed
            String hql = "SELECT afm FROM AnalyzerFieldMapping afm " +
                    "LEFT JOIN FETCH afm.analyzerField af " +
                    "WHERE afm.id = :mappingId";
            Query<AnalyzerFieldMapping> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerFieldMapping.class);
            query.setParameter("mappingId", mappingId);
            return query.uniqueResult();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerFieldMapping by ID with field", e);
        }
    }
}

