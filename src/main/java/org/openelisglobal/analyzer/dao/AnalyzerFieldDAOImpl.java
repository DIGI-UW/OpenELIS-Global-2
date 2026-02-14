package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerField;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
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
            // Validate analyzerId is not null or empty
            if (analyzerId == null || analyzerId.trim().isEmpty() || "null".equalsIgnoreCase(analyzerId)) {
                throw new LIMSRuntimeException("Analyzer ID cannot be null or empty");
            }

            Integer analyzerIdInt;
            try {
                analyzerIdInt = Integer.parseInt(analyzerId);
            } catch (NumberFormatException e) {
                throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
            }

            // HQL join on legacy Analyzer (XML-mapped) via relationship path
            String hql = "SELECT af FROM AnalyzerField af " + "JOIN af.analyzer a " + "WHERE a.id = :analyzerId";
            Query<AnalyzerField> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerField.class);
            query.setParameter("analyzerId", analyzerIdInt);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerField by analyzer ID", e);
        }
    }

    @Override
    public String insert(AnalyzerField field) {
        if (field.getId() == null || field.getId().trim().isEmpty()) {
            field.setId(UUID.randomUUID().toString());
        }
        if (field.getLastupdated() == null) {
            field.setLastupdatedFields();
        }
        return super.insert(field);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<AnalyzerField> findByIdWithAnalyzer(String analyzerFieldId) {
        try {
            // Use JOIN FETCH to eagerly load analyzer and customFieldType relationships
            // within transaction
            String hql = "SELECT af FROM AnalyzerField af " + "LEFT JOIN FETCH af.analyzer a "
                    + "LEFT JOIN FETCH af.customFieldType cft " + "WHERE af.id = :analyzerFieldId";
            Query<AnalyzerField> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerField.class);
            query.setParameter("analyzerFieldId", analyzerFieldId);
            AnalyzerField result = query.uniqueResult();
            return java.util.Optional.ofNullable(result);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerField by ID with analyzer", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<AnalyzerField> findByAnalyzerIdAndFieldName(String analyzerId, String fieldName) {
        try {
            // Validate analyzerId is not null or empty
            if (analyzerId == null || analyzerId.trim().isEmpty() || "null".equalsIgnoreCase(analyzerId)) {
                throw new LIMSRuntimeException("Analyzer ID cannot be null or empty");
            }

            Integer analyzerIdInt;
            try {
                analyzerIdInt = Integer.parseInt(analyzerId);
            } catch (NumberFormatException e) {
                throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
            }

            String hql = "SELECT af FROM AnalyzerField af " + "JOIN af.analyzer a "
                    + "WHERE a.id = :analyzerId AND af.fieldName = :fieldName";
            Query<AnalyzerField> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerField.class);
            query.setParameter("analyzerId", analyzerIdInt);
            query.setParameter("fieldName", fieldName);
            AnalyzerField result = query.uniqueResult();
            return java.util.Optional.ofNullable(result);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerField by analyzer ID and field name", e);
        }
    }
}
