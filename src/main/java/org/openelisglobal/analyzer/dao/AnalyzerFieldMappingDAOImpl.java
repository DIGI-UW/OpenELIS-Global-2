package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for AnalyzerFieldMapping.
 *
 * Uses HQL queries with relationship navigation (e.g., afm.analyzer.id).
 * Relationships use JPA @ManyToOne annotations. Analyzer.id uses
 * LIMSStringNumberUserType (String in Java, INTEGER in DB), so we convert
 * String to Integer for parameter binding.
 */
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
            String hql = "FROM AnalyzerFieldMapping afm WHERE afm.analyzerField.id = :analyzerFieldId";
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
            // Convert String analyzerId to Integer for HQL parameter binding
            // Analyzer.id uses LIMSStringNumberUserType: Java String, DB INTEGER
            Integer analyzerIdInt;
            try {
                analyzerIdInt = Integer.parseInt(analyzerId);
            } catch (NumberFormatException e) {
                throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
            }

            LogEvent.logDebug(this.getClass().getSimpleName(), "findActiveMappingsByAnalyzerId",
                    "Querying with analyzerId=" + analyzerId + ", analyzerIdInt=" + analyzerIdInt);

            String hql = "SELECT afm FROM AnalyzerFieldMapping afm LEFT JOIN FETCH afm.analyzerField WHERE afm.analyzer.id = :analyzerId AND afm.isActive = true";
            Query<AnalyzerFieldMapping> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerFieldMapping.class);
            query.setParameter("analyzerId", analyzerIdInt); // Pass Integer, not String
            List<AnalyzerFieldMapping> results = query.list();

            LogEvent.logDebug(this.getClass().getSimpleName(), "findActiveMappingsByAnalyzerId",
                    "Query returned " + results.size() + " results");

            return results;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding active AnalyzerFieldMapping by analyzer ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerFieldMapping> findByAnalyzerId(String analyzerId) {
        try {
            // Validate analyzerId is not null or empty
            if (analyzerId == null || analyzerId.trim().isEmpty() || "null".equalsIgnoreCase(analyzerId)) {
                throw new LIMSRuntimeException("Analyzer ID cannot be null or empty");
            }

            // Convert String analyzerId to Integer for HQL parameter binding
            // Analyzer.id uses LIMSStringNumberUserType: Java String, DB INTEGER
            Integer analyzerIdInt;
            try {
                analyzerIdInt = Integer.parseInt(analyzerId);
            } catch (NumberFormatException e) {
                throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
            }

            String hql = "SELECT afm FROM AnalyzerFieldMapping afm LEFT JOIN FETCH afm.analyzerField WHERE afm.analyzer.id = :analyzerId";
            Query<AnalyzerFieldMapping> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerFieldMapping.class);
            query.setParameter("analyzerId", analyzerIdInt); // Pass Integer, not String
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerFieldMapping by analyzer ID", e);
        }
    }

    @Override
    public String insert(AnalyzerFieldMapping mapping) {
        if (mapping.getId() == null || mapping.getId().trim().isEmpty()) {
            mapping.setId(UUID.randomUUID().toString());
        }
        if (mapping.getLastupdated() == null) {
            mapping.setLastupdatedFields();
        }
        return super.insert(mapping);
    }
}
