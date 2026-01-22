package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for AnalyzerFieldMapping.
 * 
 * Uses HQL queries only (no native SQL) to query by ID fields. Relationships
 * are not loaded here - they are hydrated manually in the service layer to
 * avoid Hibernate's table name resolution issues when XML-mapped entities
 * reference annotation-based entities.
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
            String hql = "FROM AnalyzerFieldMapping afm WHERE afm.analyzerFieldId = :analyzerFieldId";
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
            // Legacy Analyzer entity uses LIMSStringNumberUserType: Java String, DB INTEGER
            Integer analyzerIdInt;
            try {
                analyzerIdInt = Integer.parseInt(analyzerId);
            } catch (NumberFormatException e) {
                throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
            }

            String hql = "FROM AnalyzerFieldMapping afm WHERE afm.analyzerId = :analyzerId AND afm.isActive = true";
            Query<AnalyzerFieldMapping> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerFieldMapping.class);
            query.setParameter("analyzerId", analyzerIdInt); // Pass Integer, not String
            return query.list();
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
            // Legacy Analyzer entity uses LIMSStringNumberUserType: Java String, DB INTEGER
            Integer analyzerIdInt;
            try {
                analyzerIdInt = Integer.parseInt(analyzerId);
            } catch (NumberFormatException e) {
                throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
            }

            String hql = "FROM AnalyzerFieldMapping afm WHERE afm.analyzerId = :analyzerId";
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
