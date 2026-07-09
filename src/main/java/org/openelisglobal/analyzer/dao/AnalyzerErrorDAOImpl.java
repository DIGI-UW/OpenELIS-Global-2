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
    public int nullifyAnalyzerId(String analyzerId) {
        try {
            // Hibernate 5.6-jakarta rejects all HQL/JPQL/CriteriaUpdate for UPDATE
            // statements ("query must begin with SELECT or FROM"). Load + nullify + flush.
            List<AnalyzerError> errors = findByAnalyzerId(analyzerId);
            for (AnalyzerError error : errors) {
                error.setAnalyzer(null);
            }
            if (!errors.isEmpty()) {
                entityManager.flush();
            }
            return errors.size();
        } catch (LIMSRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error nullifying analyzer ID on AnalyzerError rows", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerError> findByAnalyzerId(String analyzerId) {
        try {
            // Analyzer.id is String in Java (LIMSStringNumberUserType), so pass String
            // Eagerly fetch analyzer to avoid LazyInitializationException
            String hql = "SELECT ae FROM AnalyzerError ae LEFT JOIN FETCH ae.analyzer WHERE ae.analyzer.id = :analyzerId ORDER BY ae.lastupdated DESC";
            Query<AnalyzerError> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerError.class);
            query.setParameter("analyzerId", analyzerId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding AnalyzerError by analyzer ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerError> findByStatus(AnalyzerError.ErrorStatus status) {
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
    public List<AnalyzerError> findByErrorType(AnalyzerError.ErrorType errorType) {
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
    public List<AnalyzerError> findBySeverity(AnalyzerError.Severity severity) {
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
                query.setParameter("analyzerId", analyzerId);
            }
            if (errorType != null) {
                query.setParameter("errorType", errorType);
            }
            if (severity != null) {
                query.setParameter("severity", severity);
            }
            if (status != null) {
                query.setParameter("status", status);
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
                    .setParameter("s", AnalyzerError.ErrorStatus.UNACKNOWLEDGED).uniqueResult();

            Long critical = (Long) session.createQuery("SELECT COUNT(ae) FROM AnalyzerError ae WHERE ae.severity = :s")
                    .setParameter("s", AnalyzerError.Severity.CRITICAL).uniqueResult();

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
