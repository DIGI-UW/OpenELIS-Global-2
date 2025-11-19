package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerErrorDAOImpl extends BaseDAOImpl<AnalyzerError, String> implements AnalyzerErrorDAO {

    public AnalyzerErrorDAOImpl() {
        super(AnalyzerError.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerError> findByAnalyzerId(String analyzerId) {
        try {
            // Convert String analyzerId to Integer for HQL parameter binding
            // Legacy Analyzer entity uses LIMSStringNumberUserType: Java String, DB INTEGER
            // Reference: ID_TYPE_ANALYSIS.md
            Integer analyzerIdInt;
            try {
                analyzerIdInt = Integer.parseInt(analyzerId);
            } catch (NumberFormatException e) {
                throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
            }

            // Eagerly fetch analyzer to avoid LazyInitializationException
            String hql = "SELECT ae FROM AnalyzerError ae LEFT JOIN FETCH ae.analyzer WHERE ae.analyzer.id = :analyzerId ORDER BY ae.lastupdated DESC";
            Query<AnalyzerError> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerError.class);
            query.setParameter("analyzerId", analyzerIdInt); // Pass Integer, not String
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerError by analyzer ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerError> findByStatus(String status) {
        try {
            // Eagerly fetch analyzer to avoid LazyInitializationException
            String hql = "SELECT DISTINCT ae FROM AnalyzerError ae LEFT JOIN FETCH ae.analyzer WHERE ae.status = :status ORDER BY ae.lastupdated DESC";
            Query<AnalyzerError> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerError.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerError by status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerError> findByErrorType(String errorType) {
        try {
            // Eagerly fetch analyzer to avoid LazyInitializationException
            String hql = "SELECT DISTINCT ae FROM AnalyzerError ae LEFT JOIN FETCH ae.analyzer WHERE ae.errorType = :errorType ORDER BY ae.lastupdated DESC";
            Query<AnalyzerError> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerError.class);
            query.setParameter("errorType", errorType);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerError by error type", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerError> findBySeverity(String severity) {
        try {
            // Eagerly fetch analyzer to avoid LazyInitializationException
            String hql = "SELECT DISTINCT ae FROM AnalyzerError ae LEFT JOIN FETCH ae.analyzer WHERE ae.severity = :severity ORDER BY ae.lastupdated DESC";
            Query<AnalyzerError> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerError.class);
            query.setParameter("severity", severity);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerError by severity", e);
        }
    }
}
