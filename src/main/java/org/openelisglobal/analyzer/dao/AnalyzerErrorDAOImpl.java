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

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerError> findAll() {
        try {
            // Eagerly fetch analyzer to avoid LazyInitializationException
            String hql = "SELECT DISTINCT ae FROM AnalyzerError ae LEFT JOIN FETCH ae.analyzer ORDER BY ae.lastupdated DESC";
            Query<AnalyzerError> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerError.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding all AnalyzerError", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerError> findByFilters(String analyzerId, AnalyzerError.ErrorType errorType,
            AnalyzerError.Severity severity, AnalyzerError.ErrorStatus status, java.util.Date startDate,
            java.util.Date endDate) {
        try {
            StringBuilder hql = new StringBuilder(
                    "SELECT DISTINCT ae FROM AnalyzerError ae LEFT JOIN FETCH ae.analyzer WHERE 1=1");

            if (analyzerId != null) {
                hql.append(" AND ae.analyzer.id = :analyzerId");
            }
            if (errorType != null) {
                hql.append(" AND ae.errorType = :errorType");
            }
            if (severity != null) {
                hql.append(" AND ae.severity = :severity");
            }
            if (status != null) {
                hql.append(" AND ae.status = :status");
            }
            if (startDate != null) {
                hql.append(" AND ae.lastupdated >= :startDate");
            }
            if (endDate != null) {
                hql.append(" AND ae.lastupdated <= :endDate");
            }
            hql.append(" ORDER BY ae.lastupdated DESC");

            Query<AnalyzerError> query = entityManager.unwrap(Session.class).createQuery(hql.toString(),
                    AnalyzerError.class);

            if (analyzerId != null) {
                // Legacy Analyzer uses LIMSStringNumberUserType: Java String, DB INTEGER
                try {
                    query.setParameter("analyzerId", Integer.parseInt(analyzerId));
                } catch (NumberFormatException e) {
                    throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
                }
            }
            if (errorType != null) {
                // Use .name() to avoid PostgreSQL varchar/bytea type mismatch
                query.setParameter("errorType", errorType.name());
            }
            if (severity != null) {
                query.setParameter("severity", severity.name());
            }
            if (status != null) {
                query.setParameter("status", status.name());
            }
            if (startDate != null) {
                query.setParameter("startDate", new java.sql.Timestamp(startDate.getTime()));
            }
            if (endDate != null) {
                query.setParameter("endDate", new java.sql.Timestamp(endDate.getTime()));
            }

            return query.list();
        } catch (LIMSRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerError by filters", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getGlobalStatistics() {
        try {
            // Single query for all statistics
            String hql = "SELECT COUNT(ae)," + " SUM(CASE WHEN ae.status = :unack THEN 1L ELSE 0L END),"
                    + " SUM(CASE WHEN ae.severity = :crit THEN 1L ELSE 0L END),"
                    + " SUM(CASE WHEN ae.lastupdated >= :since24h THEN 1L ELSE 0L END)" + " FROM AnalyzerError ae";

            Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(hql, Object[].class);
            // Use .name() to avoid PostgreSQL varchar/bytea type mismatch
            query.setParameter("unack", AnalyzerError.ErrorStatus.UNACKNOWLEDGED.name());
            query.setParameter("crit", AnalyzerError.Severity.CRITICAL.name());
            query.setParameter("since24h",
                    new java.sql.Timestamp(System.currentTimeMillis() - (24L * 60L * 60L * 1000L)));

            Object[] row = query.uniqueResult();
            java.util.Map<String, Long> stats = new java.util.LinkedHashMap<>();
            stats.put("totalErrors", row[0] != null ? (Long) row[0] : 0L);
            stats.put("unacknowledged", row[1] != null ? (Long) row[1] : 0L);
            stats.put("critical", row[2] != null ? (Long) row[2] : 0L);
            stats.put("last24Hours", row[3] != null ? (Long) row[3] : 0L);
            return stats;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting global AnalyzerError statistics", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<AnalyzerError> getWithAnalyzer(String errorId) {
        try {
            // Eagerly fetch analyzer to avoid LazyInitializationException
            String hql = "SELECT ae FROM AnalyzerError ae LEFT JOIN FETCH ae.analyzer WHERE ae.id = :errorId";
            Query<AnalyzerError> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerError.class);
            query.setParameter("errorId", errorId);
            AnalyzerError result = query.uniqueResult();
            return java.util.Optional.ofNullable(result);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerError by ID with analyzer", e);
        }
    }
}
