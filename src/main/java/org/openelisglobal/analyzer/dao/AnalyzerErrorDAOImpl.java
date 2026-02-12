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
            query.setParameter("analyzerId", analyzerIdInt);
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
            // Use separate COUNT queries — Hibernate's classic HQL parser cannot
            // detect named parameters inside SUM(CASE WHEN ...) aggregate expressions.
            Session session = entityManager.unwrap(Session.class);

            Long total = (Long) session.createQuery("SELECT COUNT(ae) FROM AnalyzerError ae").uniqueResult();

            Long unacknowledged = (Long) session
                    .createQuery("SELECT COUNT(ae) FROM AnalyzerError ae WHERE ae.status = :s")
                    .setParameter("s", AnalyzerError.ErrorStatus.UNACKNOWLEDGED.name()).uniqueResult();

            Long critical = (Long) session.createQuery("SELECT COUNT(ae) FROM AnalyzerError ae WHERE ae.severity = :s")
                    .setParameter("s", AnalyzerError.Severity.CRITICAL.name()).uniqueResult();

            Long last24h = (Long) session
                    .createQuery("SELECT COUNT(ae) FROM AnalyzerError ae WHERE ae.lastupdated >= :since")
                    .setParameter("since",
                            new java.sql.Timestamp(System.currentTimeMillis() - (24L * 60L * 60L * 1000L)))
                    .uniqueResult();

            java.util.Map<String, Long> stats = new java.util.LinkedHashMap<>();
            stats.put("totalErrors", total != null ? total : 0L);
            stats.put("unacknowledged", unacknowledged != null ? unacknowledged : 0L);
            stats.put("critical", critical != null ? critical : 0L);
            stats.put("last24Hours", last24h != null ? last24h : 0L);
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
