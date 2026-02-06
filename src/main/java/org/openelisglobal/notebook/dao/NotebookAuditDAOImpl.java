package org.openelisglobal.notebook.dao;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.notebook.valueholder.NotebookAuditLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for NotebookAuditLog operations.
 *
 * <p>
 * Uses HQL queries with pagination and filtering support. Follows the pattern
 * from HistoryDAOImpl.
 */
@Component
@Transactional
public class NotebookAuditDAOImpl extends BaseDAOImpl<NotebookAuditLog, Long> implements NotebookAuditDAO {

    public NotebookAuditDAOImpl() {
        super(NotebookAuditLog.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookAuditLog> getAuditLogsByEntityId(Long referenceId, String entityType) {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<NotebookAuditLog> cq = cb.createQuery(NotebookAuditLog.class);
            Root<NotebookAuditLog> root = cq.from(NotebookAuditLog.class);

            Predicate refIdPredicate = cb.equal(root.get("referenceId"), cb.parameter(Long.class, "refId"));
            Predicate entityTypePredicate = cb.equal(root.get("entityType"), cb.parameter(String.class, "entityType"));

            cq.where(cb.and(refIdPredicate, entityTypePredicate)).orderBy(cb.desc(root.get("timestamp")));

            TypedQuery<NotebookAuditLog> query = entityManager.createQuery(cq);
            query.setParameter("refId", referenceId);
            query.setParameter("entityType", entityType);
            return query.getResultList();
        } catch (RuntimeException e) {
            handleException(e, "getAuditLogsByEntityId");
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookAuditLog> getAuditLogsByReference(Long referenceId, Long referenceTable) {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<NotebookAuditLog> cq = cb.createQuery(NotebookAuditLog.class);
            Root<NotebookAuditLog> root = cq.from(NotebookAuditLog.class);

            Predicate refIdPredicate = cb.equal(root.get("referenceId"), cb.parameter(Long.class, "refId"));
            Predicate refTablePredicate = cb.equal(root.get("referenceTable"), cb.parameter(Long.class, "refTable"));

            cq.where(cb.and(refIdPredicate, refTablePredicate)).orderBy(cb.desc(root.get("timestamp")));

            TypedQuery<NotebookAuditLog> query = entityManager.createQuery(cq);
            query.setParameter("refId", referenceId);
            query.setParameter("refTable", referenceTable);
            return query.getResultList();
        } catch (RuntimeException e) {
            handleException(e, "getAuditLogsByReference");
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookAuditLog> searchAuditLogs(Map<String, Object> filters, int offset, int limit) {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<NotebookAuditLog> cq = cb.createQuery(NotebookAuditLog.class);
            Root<NotebookAuditLog> root = cq.from(NotebookAuditLog.class);

            List<Predicate> predicates = new ArrayList<>();

            if (filters.containsKey("entityType")) {
                predicates.add(cb.equal(root.get("entityType"), filters.get("entityType")));
            }

            if (filters.containsKey("activity")) {
                predicates.add(cb.equal(root.get("activity"), filters.get("activity")));
            }

            if (filters.containsKey("sysUserId")) {
                predicates.add(cb.equal(root.get("sysUserId"), filters.get("sysUserId")));
            }

            if (filters.containsKey("startDate")) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), (Timestamp) filters.get("startDate")));
            }

            if (filters.containsKey("endDate")) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), (Timestamp) filters.get("endDate")));
            }

            if (filters.containsKey("statusNew")) {
                predicates.add(cb.equal(root.get("statusNew"), filters.get("statusNew")));
            }

            if (filters.containsKey("referenceId")) {
                Object refIdValue = filters.get("referenceId");
                if (refIdValue instanceof String) {
                    predicates.add(cb.equal(root.get("referenceId"), Long.parseLong((String) refIdValue)));
                } else {
                    predicates.add(cb.equal(root.get("referenceId"), refIdValue));
                }
            }

            cq.where(predicates.toArray(new Predicate[0])).orderBy(cb.desc(root.get("timestamp")));

            TypedQuery<NotebookAuditLog> query = entityManager.createQuery(cq);
            query.setFirstResult(offset);
            query.setMaxResults(limit);

            return query.getResultList();
        } catch (RuntimeException e) {
            handleException(e, "searchAuditLogs");
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countAuditLogs(Map<String, Object> filters) {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<NotebookAuditLog> root = cq.from(NotebookAuditLog.class);

            List<Predicate> predicates = new ArrayList<>();

            // Build dynamic query (same filters as searchAuditLogs)
            if (filters.containsKey("entityType")) {
                predicates.add(cb.equal(root.get("entityType"), filters.get("entityType")));
            }

            if (filters.containsKey("activity")) {
                predicates.add(cb.equal(root.get("activity"), filters.get("activity")));
            }

            if (filters.containsKey("sysUserId")) {
                predicates.add(cb.equal(root.get("sysUserId"), filters.get("sysUserId")));
            }

            if (filters.containsKey("startDate")) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), (Timestamp) filters.get("startDate")));
            }

            if (filters.containsKey("endDate")) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), (Timestamp) filters.get("endDate")));
            }

            if (filters.containsKey("statusNew")) {
                predicates.add(cb.equal(root.get("statusNew"), filters.get("statusNew")));
            }

            if (filters.containsKey("referenceId")) {
                Object refIdValue = filters.get("referenceId");
                if (refIdValue instanceof String) {
                    predicates.add(cb.equal(root.get("referenceId"), Long.parseLong((String) refIdValue)));
                } else {
                    predicates.add(cb.equal(root.get("referenceId"), refIdValue));
                }
            }

            cq.select(cb.count(root)).where(predicates.toArray(new Predicate[0]));

            TypedQuery<Long> query = entityManager.createQuery(cq);
            return query.getSingleResult();
        } catch (RuntimeException e) {
            handleException(e, "countAuditLogs");
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookAuditLog> getAuditLogsByUser(String sysUserId, int offset, int limit) {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<NotebookAuditLog> cq = cb.createQuery(NotebookAuditLog.class);
            Root<NotebookAuditLog> root = cq.from(NotebookAuditLog.class);

            cq.where(cb.equal(root.get("sysUserId"), sysUserId)).orderBy(cb.desc(root.get("timestamp")));

            TypedQuery<NotebookAuditLog> query = entityManager.createQuery(cq);
            query.setFirstResult(offset);
            query.setMaxResults(limit);

            return query.getResultList();
        } catch (RuntimeException e) {
            handleException(e, "getAuditLogsByUser");
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookAuditLog> getRecentAuditLogs(int limit) {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<NotebookAuditLog> cq = cb.createQuery(NotebookAuditLog.class);
            Root<NotebookAuditLog> root = cq.from(NotebookAuditLog.class);

            cq.orderBy(cb.desc(root.get("timestamp")));

            TypedQuery<NotebookAuditLog> query = entityManager.createQuery(cq);
            query.setMaxResults(limit);

            return query.getResultList();
        } catch (RuntimeException e) {
            handleException(e, "getRecentAuditLogs");
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getAuditLogStatistics() {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
            Root<NotebookAuditLog> root = cq.from(NotebookAuditLog.class);

            cq.multiselect(root.get("entityType"), cb.count(root)).groupBy(root.get("entityType"));

            TypedQuery<Object[]> query = entityManager.createQuery(cq);
            List<Object[]> results = query.getResultList();

            Map<String, Long> statistics = new HashMap<>();
            for (Object[] row : results) {
                statistics.put((String) row[0], (Long) row[1]);
            }

            return statistics;
        } catch (RuntimeException e) {
            handleException(e, "getAuditLogStatistics");
            return new HashMap<>();
        }
    }

    /**
     * Handle exceptions and log errors.
     */
    private void handleException(RuntimeException e, String methodName) {
        throw new LIMSRuntimeException("Error in " + methodName + ": " + e.getMessage(), e);
    }
}
