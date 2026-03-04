package org.openelisglobal.qaevent.daoimpl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qaevent.dao.DeltaCheckAlertDAO;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert.AlertStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DeltaCheckAlertDAOImpl extends BaseDAOImpl<DeltaCheckAlert, Integer> implements DeltaCheckAlertDAO {

    public DeltaCheckAlertDAOImpl() {
        super(DeltaCheckAlert.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getActiveAlertsForResult(String resultId) {
        return queryAlertsForResult(resultId, AlertStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getAllAlertsForResult(String resultId) {
        return queryAlertsForResult(resultId, null);
    }

    private List<DeltaCheckAlert> queryAlertsForResult(String resultId, AlertStatus status) {
        if (resultId == null) {
            throw new LIMSRuntimeException("ResultId cannot be null");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<DeltaCheckAlert> cq = cb.createQuery(DeltaCheckAlert.class);
            Root<DeltaCheckAlert> root = cq.from(DeltaCheckAlert.class);

            Predicate where = cb.equal(root.get("resultId"), resultId);
            if (status != null) {
                where = cb.and(where, cb.equal(root.get("status"), status.name()));
            }

            cq.select(root).where(where).orderBy(cb.desc(root.get("createdDate")));

            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving alerts for result: " + resultId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getAlertsByStatus(AlertStatus status) {
        if (status == null) {
            throw new LIMSRuntimeException("Status cannot be null");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<DeltaCheckAlert> cq = cb.createQuery(DeltaCheckAlert.class);
            Root<DeltaCheckAlert> root = cq.from(DeltaCheckAlert.class);

            cq.select(root).where(cb.equal(root.get("status"), status.name()))
                    .orderBy(cb.desc(root.get("createdDate")));

            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving alerts by status: " + status, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getAlertsDismissedBy(String dismissedBy) {
        if (dismissedBy == null) {
            throw new LIMSRuntimeException("DismissedBy cannot be null");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<DeltaCheckAlert> cq = cb.createQuery(DeltaCheckAlert.class);
            Root<DeltaCheckAlert> root = cq.from(DeltaCheckAlert.class);

            cq.select(root).where(cb.equal(root.get("dismissedBy"), dismissedBy))
                    .orderBy(cb.desc(root.get("dismissedDate")));

            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving alerts dismissed by: " + dismissedBy, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getEscalatedAlerts() {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<DeltaCheckAlert> cq = cb.createQuery(DeltaCheckAlert.class);
            Root<DeltaCheckAlert> root = cq.from(DeltaCheckAlert.class);

            cq.select(root).where(cb.and(cb.equal(root.get("status"), AlertStatus.ESCALATED_NCE.name()),
                    cb.isNotNull(root.get("escalatedNcEvent")))).orderBy(cb.desc(root.get("createdDate")));

            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving escalated alerts", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getRecentAlerts(int daysSince) {
        if (daysSince < 0) {
            throw new LIMSRuntimeException("Days since must be non-negative");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<DeltaCheckAlert> cq = cb.createQuery(DeltaCheckAlert.class);
            Root<DeltaCheckAlert> root = cq.from(DeltaCheckAlert.class);

            Timestamp cutoffDate = Timestamp.from(Instant.now().minus(daysSince, ChronoUnit.DAYS));

            cq.select(root).where(cb.greaterThanOrEqualTo(root.get("createdDate"), cutoffDate))
                    .orderBy(cb.desc(root.get("createdDate")));

            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving recent alerts for " + daysSince + " days", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getAlertsAboveThreshold(double thresholdPercent) {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<DeltaCheckAlert> cq = cb.createQuery(DeltaCheckAlert.class);
            Root<DeltaCheckAlert> root = cq.from(DeltaCheckAlert.class);
            jakarta.persistence.criteria.ParameterExpression<BigDecimal> param = cb.parameter(BigDecimal.class);

            cq.select(root).where(cb.greaterThanOrEqualTo(root.get("changePercent"), param))
                    .orderBy(cb.desc(root.get("changePercent")));

            return entityManager.createQuery(cq).setParameter(param, BigDecimal.valueOf(thresholdPercent))
                    .getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving alerts above threshold: " + thresholdPercent, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countActiveAlertsForResult(String resultId) {
        if (resultId == null) {
            return 0;
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<DeltaCheckAlert> root = cq.from(DeltaCheckAlert.class);

            cq.select(cb.count(root)).where(cb.and(cb.equal(root.get("resultId"), resultId),
                    cb.equal(root.get("status"), AlertStatus.ACTIVE.name())));

            Long count = entityManager.createQuery(cq).getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error counting active alerts for result: " + resultId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countAlertsByStatus(AlertStatus status) {
        if (status == null) {
            return 0;
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<DeltaCheckAlert> root = cq.from(DeltaCheckAlert.class);

            cq.select(cb.count(root)).where(cb.equal(root.get("status"), status.name()));

            Long count = entityManager.createQuery(cq).getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error counting alerts by status: " + status, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean alertExistsForResultComparison(String resultId, String previousResultId) {
        if (resultId == null || previousResultId == null) {
            return false;
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<DeltaCheckAlert> root = cq.from(DeltaCheckAlert.class);

            cq.select(cb.count(root)).where(cb.and(cb.equal(root.get("resultId"), resultId),
                    cb.equal(root.get("previousResultId"), previousResultId)));

            Long count = entityManager.createQuery(cq).getSingleResult();
            return count != null && count > 0;
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException(
                    "Error checking alert existence for result comparison: " + resultId + ", " + previousResultId, e);
        }
    }

    // Two-step approach to avoid the cross-type IN subquery type mismatch
    // (same reasoning as getAlertsForAnalyses).
    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getAlertsForAnalysis(String analysisId, AlertStatus status) {
        if (analysisId == null) {
            throw new LIMSRuntimeException("AnalysisId cannot be null");
        }

        try {
            int intAnalysisId = Integer.parseInt(analysisId);

            Query<String> resultIdQuery = entityManager.unwrap(Session.class)
                    .createQuery("SELECT r.id FROM Result r WHERE r.analysis.id = :analysisId", String.class);
            resultIdQuery.setParameter("analysisId", intAnalysisId);
            List<String> resultIds = resultIdQuery.getResultList();

            if (resultIds.isEmpty()) {
                return new ArrayList<>();
            }

            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<DeltaCheckAlert> cq = cb.createQuery(DeltaCheckAlert.class);
            Root<DeltaCheckAlert> root = cq.from(DeltaCheckAlert.class);

            Predicate where = root.get("resultId").in(resultIds);
            if (status != null) {
                where = cb.and(where, cb.equal(root.get("status"), status.name()));
            }
            cq.select(root).where(where).orderBy(cb.desc(root.get("createdDate")));
            return entityManager.createQuery(cq).getResultList();
        } catch (NumberFormatException e) {
            throw new LIMSRuntimeException("Invalid analysis ID format: " + analysisId, e);
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving alerts for analysis: " + analysisId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getAlertsNeedingAttention(int hoursOld) {
        if (hoursOld < 0) {
            throw new LIMSRuntimeException("Hours old must be non-negative");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<DeltaCheckAlert> cq = cb.createQuery(DeltaCheckAlert.class);
            Root<DeltaCheckAlert> root = cq.from(DeltaCheckAlert.class);

            Timestamp cutoffDate = Timestamp.from(Instant.now().minus(hoursOld, ChronoUnit.HOURS));

            cq.select(root)
                    .where(cb.and(cb.equal(root.get("status"), AlertStatus.ACTIVE.name()),
                            cb.lessThanOrEqualTo(root.get("createdDate"), cutoffDate)))
                    .orderBy(cb.asc(root.get("createdDate")));

            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException(
                    "Error retrieving alerts needing attention older than " + hoursOld + " hours", e);
        }
    }

    @Override
    public int deleteOldResolvedAlerts(int daysSinceResolution) {
        if (daysSinceResolution < 0) {
            throw new LIMSRuntimeException("Days since resolution must be non-negative");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaDelete<DeltaCheckAlert> cd = cb.createCriteriaDelete(DeltaCheckAlert.class);
            Root<DeltaCheckAlert> root = cd.from(DeltaCheckAlert.class);

            Timestamp cutoffDate = Timestamp.from(Instant.now().minus(daysSinceResolution, ChronoUnit.DAYS));

            cd.where(cb.or(
                    cb.and(cb.equal(root.get("status"), AlertStatus.DISMISSED.name()),
                            cb.lessThanOrEqualTo(root.get("dismissedDate"), cutoffDate)),
                    cb.and(cb.equal(root.get("status"), AlertStatus.ESCALATED_NCE.name()),
                            cb.lessThanOrEqualTo(root.get("createdDate"), cutoffDate))));

            return entityManager.createQuery(cd).executeUpdate();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error deleting old resolved alerts", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countAlertsSince(Timestamp cutoffDate, AlertStatus status) {
        if (cutoffDate == null) {
            throw new LIMSRuntimeException("Cutoff date cannot be null");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<DeltaCheckAlert> root = cq.from(DeltaCheckAlert.class);

            Predicate where = cb.greaterThanOrEqualTo(root.get("createdDate"), cutoffDate);
            if (status != null) {
                where = cb.and(where, cb.equal(root.get("status"), status.name()));
            }

            cq.select(cb.count(root)).where(where);

            Long count = entityManager.createQuery(cq).getSingleResult();
            return count != null ? count : 0;
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error counting alerts since " + cutoffDate, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public double getAverageChangePercentSince(Timestamp cutoffDate) {
        if (cutoffDate == null) {
            throw new LIMSRuntimeException("Cutoff date cannot be null");
        }

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Double> cq = cb.createQuery(Double.class);
            Root<DeltaCheckAlert> root = cq.from(DeltaCheckAlert.class);

            cq.select(cb.avg(root.get("changePercent")))
                    .where(cb.greaterThanOrEqualTo(root.get("createdDate"), cutoffDate));

            Double avg = entityManager.createQuery(cq).getSingleResult();
            return avg != null ? avg : 0.0;
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error getting average change percent since " + cutoffDate, e);
        }
    }

    // Two-step approach: first resolve result IDs for the given analyses, then
    // query
    // alerts. This avoids a cross-type IN subquery (delta_check_alert.result_id is
    // VARCHAR while result.id is NUMERIC in the DB) that Hibernate 5.6's classic
    // query translator cannot cast in HQL.
    @Override
    @Transactional(readOnly = true)
    public List<DeltaCheckAlert> getAlertsForAnalyses(List<String> analysisIds, AlertStatus status) {
        if (analysisIds == null || analysisIds.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Parse String IDs to integers so Hibernate binds them as NUMERIC, matching
            // the analysis.id column type and avoiding a PostgreSQL type-mismatch error.
            List<Integer> intAnalysisIds = new ArrayList<>();
            for (String id : analysisIds) {
                intAnalysisIds.add(Integer.parseInt(id));
            }

            // Step 1: resolve result IDs (returned as String by Hibernate's StringType)
            Query<String> resultIdQuery = entityManager.unwrap(Session.class)
                    .createQuery("SELECT r.id FROM Result r WHERE r.analysis.id IN (:analysisIds)", String.class);
            resultIdQuery.setParameterList("analysisIds", intAnalysisIds);
            List<String> resultIds = resultIdQuery.getResultList();

            if (resultIds.isEmpty()) {
                return new ArrayList<>();
            }

            // Step 2: query alerts by the resolved String result IDs (both sides VARCHAR)
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<DeltaCheckAlert> cq = cb.createQuery(DeltaCheckAlert.class);
            Root<DeltaCheckAlert> root = cq.from(DeltaCheckAlert.class);

            Predicate where = root.get("resultId").in(resultIds);
            if (status != null) {
                where = cb.and(where, cb.equal(root.get("status"), status.name()));
            }
            cq.select(root).where(where).orderBy(cb.desc(root.get("createdDate")));
            return entityManager.createQuery(cq).getResultList();
        } catch (NumberFormatException e) {
            throw new LIMSRuntimeException("Invalid analysis ID in list: " + analysisIds, e);
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving alerts for analyses: " + analysisIds, e);
        }
    }
}
