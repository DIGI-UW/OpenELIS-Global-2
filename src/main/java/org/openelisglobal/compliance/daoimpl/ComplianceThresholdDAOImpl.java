package org.openelisglobal.compliance.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliance.dao.ComplianceThresholdDAO;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ThresholdType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ComplianceThresholdDAO following OpenELIS patterns.
 *
 * Constitutional compliance: @Transactional, exception-handling wrappers with
 * LogEvent, HQL only (no native SQL).
 */
@Component
@Transactional
public class ComplianceThresholdDAOImpl extends BaseDAOImpl<ComplianceThreshold, String>
        implements ComplianceThresholdDAO {

    public ComplianceThresholdDAOImpl() {
        super(ComplianceThreshold.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceThreshold> getThresholdsByGroupId(String groupId) throws LIMSRuntimeException {
        try {
            String hql = "SELECT DISTINCT ct FROM ComplianceThreshold ct "
                    + "LEFT JOIN FETCH ct.group g LEFT JOIN FETCH g.standard "
                    + "LEFT JOIN FETCH ct.valueMappings WHERE ct.group.id = :groupId ORDER BY ct.sortOrder";
            TypedQuery<ComplianceThreshold> query = entityManager.createQuery(hql, ComplianceThreshold.class);
            query.setParameter("groupId", groupId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold getThresholdsByGroupId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean parameterExistsInGroupForType(String groupId, String parameterCode, ThresholdType thresholdType)
            throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(ct) FROM ComplianceThreshold ct" + " WHERE ct.group.id = :groupId"
                    + " AND ct.parameterCode = :parameterCode" + " AND ct.thresholdType = :thresholdType";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("groupId", groupId);
            query.setParameter("parameterCode", parameterCode);
            query.setParameter("thresholdType", thresholdType);
            return query.getSingleResult() > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold parameterExistsInGroupForType()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceThreshold> getThresholdsByTestId(String testId) throws LIMSRuntimeException {
        try {
            String hql = "SELECT DISTINCT ct FROM ComplianceThreshold ct "
                    + "LEFT JOIN FETCH ct.group g LEFT JOIN FETCH g.standard "
                    + "LEFT JOIN FETCH ct.valueMappings WHERE ct.test.id = :testId ORDER BY ct.sortOrder";
            TypedQuery<ComplianceThreshold> query = entityManager.createQuery(hql, ComplianceThreshold.class);
            query.setParameter("testId", testId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold getThresholdsByTestId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceThreshold> getThresholdsByTestAndStandard(String testId, String standardId)
            throws LIMSRuntimeException {
        try {
            // Match thresholds either by direct test FK (set when the threshold
            // CSV resolved the testName at seed time) OR by parameterCode
            // case-insensitively matching the test's name (fallback for
            // template-level thresholds where test_id was left null because the
            // test didn't exist in the catalog when the seed ran).
            // Primary: match by direct test FK.
            // Fallback: match by parameterCode = test name (case-insensitive) for
            // template-level thresholds where test_id was left null at seed time.
            String hql = "SELECT DISTINCT ct FROM ComplianceThreshold ct "
                    + "JOIN FETCH ct.group pg LEFT JOIN FETCH ct.valueMappings "
                    + "WHERE pg.standard.id = :standardId "
                    + "AND (ct.test.id = :testId "
                    + "     OR (ct.test IS NULL AND LOWER(ct.parameterCode) = ("
                    + "         SELECT LOWER(t.name) FROM org.openelisglobal.test.valueholder.Test t"
                    + "         WHERE t.id = :testId))) "
                    + "ORDER BY pg.sortOrder, ct.sortOrder";
            TypedQuery<ComplianceThreshold> query = entityManager.createQuery(hql, ComplianceThreshold.class);
            query.setParameter("testId", testId);
            query.setParameter("standardId", standardId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold getThresholdsByTestAndStandard()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean groupHasThresholds(String groupId) throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(ct) FROM ComplianceThreshold ct WHERE ct.group.id = :groupId";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("groupId", groupId);
            return query.getSingleResult() > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold groupHasThresholds()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean standardHasThresholds(String standardId) throws LIMSRuntimeException {
        try {
            String hql = "SELECT COUNT(ct) FROM ComplianceThreshold ct WHERE ct.group.standard.id = :standardId";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("standardId", standardId);
            return query.getSingleResult() > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold standardHasThresholds()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> countLinkedTestsByStandardIds(Collection<String> standardIds)
            throws LIMSRuntimeException {
        if (standardIds == null || standardIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            // COUNT(DISTINCT ct.test.id) per standard so a single test linked
            // to multiple groups in the same standard is only counted once.
            // INNER JOIN on ct.test filters out template-level thresholds with
            // null test_id, matching what the linked-tests panel actually shows.
            String hql = "SELECT pg.standard.id, COUNT(DISTINCT ct.test.id) " + "FROM ComplianceThreshold ct "
                    + "JOIN ct.group pg " + "JOIN ct.test t " + "WHERE pg.standard.id IN :ids "
                    + "GROUP BY pg.standard.id";
            TypedQuery<Object[]> query = entityManager.createQuery(hql, Object[].class);
            query.setParameter("ids", standardIds);
            List<Object[]> rows = query.getResultList();
            Map<String, Integer> counts = new HashMap<>(rows.size());
            for (Object[] row : rows) {
                String standardId = String.valueOf(row[0]);
                int count = ((Number) row[1]).intValue();
                counts.put(standardId, count);
            }
            return counts;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold countLinkedTestsByStandardIds()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getTestThresholdSummary() throws LIMSRuntimeException {
        try {
            // Explicit INNER JOINs (instead of dotted-path SELECT on optional
            // associations) so the generated SQL filters out seeded thresholds
            // that have NULL test_id without relying on Hibernate's optional-
            // FK heuristics.
            String hql = "SELECT t.id, COUNT(DISTINCT ct.id), COUNT(DISTINCT s.id) " + "FROM ComplianceThreshold ct "
                    + "JOIN ct.test t " + "JOIN ct.group pg " + "JOIN pg.standard s " + "GROUP BY t.id";
            TypedQuery<Object[]> query = entityManager.createQuery(hql, Object[].class);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ComplianceThreshold getTestThresholdSummary()", e);
        }
    }
}
