package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
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
            String hql = "FROM AnalyzerError WHERE analyzer.id = :analyzerId ORDER BY lastupdated DESC";
            Query<AnalyzerError> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerError.class);
            query.setParameter("analyzerId", analyzerId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerError by analyzer ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerError> findByStatus(String status) {
        try {
            String hql = "FROM AnalyzerError WHERE status = :status ORDER BY lastupdated DESC";
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
            String hql = "FROM AnalyzerError WHERE errorType = :errorType ORDER BY lastupdated DESC";
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
            String hql = "FROM AnalyzerError WHERE severity = :severity ORDER BY lastupdated DESC";
            Query<AnalyzerError> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerError.class);
            query.setParameter("severity", severity);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerError by severity", e);
        }
    }
}

