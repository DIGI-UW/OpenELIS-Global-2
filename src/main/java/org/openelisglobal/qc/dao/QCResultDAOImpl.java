package org.openelisglobal.qc.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for QCResult entity.
 *
 * Uses JPA Criteria API instead of HQL to avoid ClassicQueryTranslatorFactory
 * column resolution issues (field names like controlLotId not resolving to
 * control_lot_id).
 */
@Component
@Transactional
public class QCResultDAOImpl extends BaseDAOImpl<QCResult, String> implements QCResultDAO {

    public QCResultDAOImpl() {
        super(QCResult.class);
    }

    @Override
    public List<QCResult> findByControlLot(String controlLotId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCResult> cq = cb.createQuery(QCResult.class);
            Root<QCResult> root = cq.from(QCResult.class);
            cq.where(cb.equal(root.get("controlLotId"), controlLotId));
            cq.orderBy(cb.desc(root.get("runDateTime")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC results by control lot", e);
        }
    }

    @Override
    public List<QCResult> findHistoricalForRule(String controlLotId, int limit) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCResult> cq = cb.createQuery(QCResult.class);
            Root<QCResult> root = cq.from(QCResult.class);
            cq.where(cb.equal(root.get("controlLotId"), controlLotId));
            cq.orderBy(cb.desc(root.get("runDateTime")));
            return entityManager.createQuery(cq).setMaxResults(limit).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving historical QC results", e);
        }
    }

    @Override
    public List<QCResult> findByInstrumentAndDateRange(String instrumentId, Timestamp startDate, Timestamp endDate)
            throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCResult> cq = cb.createQuery(QCResult.class);
            Root<QCResult> root = cq.from(QCResult.class);
            cq.where(cb.equal(root.get("instrumentId"), instrumentId),
                    cb.greaterThanOrEqualTo(root.get("runDateTime"), startDate),
                    cb.lessThanOrEqualTo(root.get("runDateTime"), endDate));
            cq.orderBy(cb.desc(root.get("runDateTime")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC results by instrument and date range", e);
        }
    }

    @Override
    public List<QCResult> findLatestByControlLot(String controlLotId, int limit) throws LIMSRuntimeException {
        return findHistoricalForRule(controlLotId, limit);
    }

    @Override
    public List<QCResult> findByControlLotIdOrderByRunDateTime(String controlLotId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCResult> cq = cb.createQuery(QCResult.class);
            Root<QCResult> root = cq.from(QCResult.class);
            cq.where(cb.equal(root.get("controlLotId"), controlLotId));
            cq.orderBy(cb.asc(root.get("runDateTime")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC results by control lot ordered by date", e);
        }
    }

    @Override
    public List<QCResult> findByControlLotAndDateRange(String controlLotId, Timestamp startDate, Timestamp endDate)
            throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCResult> cq = cb.createQuery(QCResult.class);
            Root<QCResult> root = cq.from(QCResult.class);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("controlLotId"), controlLotId));
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("runDateTime"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("runDateTime"), endDate));
            }

            cq.where(predicates.toArray(new Predicate[0]));
            cq.orderBy(cb.asc(root.get("runDateTime")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC results by control lot and date range", e);
        }
    }

    @Override
    public List<QCResult> findLatestByInstrumentAndTest(String instrumentId, String testId, int limit)
            throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCResult> cq = cb.createQuery(QCResult.class);
            Root<QCResult> root = cq.from(QCResult.class);
            cq.where(cb.equal(root.get("instrumentId"), instrumentId), cb.equal(root.get("testId"), testId));
            cq.orderBy(cb.desc(root.get("runDateTime")));
            return entityManager.createQuery(cq).setMaxResults(limit).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving latest QC results by instrument and test", e);
        }
    }

    @Override
    public List<QCResult> findLatestAcceptedBefore(String instrumentId, String testId, Timestamp before)
            throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCResult> cq = cb.createQuery(QCResult.class);
            Root<QCResult> root = cq.from(QCResult.class);
            cq.where(cb.equal(root.get("instrumentId"), instrumentId), cb.equal(root.get("testId"), testId),
                    cb.equal(root.get("resultStatus"), "ACCEPTED"), cb.lessThan(root.get("runDateTime"), before));
            cq.orderBy(cb.desc(root.get("runDateTime")));
            return entityManager.createQuery(cq).setMaxResults(1).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving latest accepted QC result before timestamp", e);
        }
    }

    @Override
    public List<QCResult> findLatestAcceptedBenchResultBefore(String testSectionId, String testId, Timestamp before)
            throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCResult> cq = cb.createQuery(QCResult.class);
            Root<QCResult> root = cq.from(QCResult.class);
            cq.where(cb.equal(root.get("testSectionId"), testSectionId), cb.equal(root.get("testId"), testId),
                    root.get("source").in(QCSource.MANUAL, QCSource.RDT),
                    cb.equal(root.get("resultStatus"), "ACCEPTED"), cb.lessThan(root.get("runDateTime"), before));
            cq.orderBy(cb.desc(root.get("runDateTime")));
            return entityManager.createQuery(cq).setMaxResults(1).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving latest accepted bench QC result before timestamp", e);
        }
    }

    /**
     * Bench QC activity for a window, grouped the way a bench actually works: by
     * lab unit and test, not by analyzer. The QC dashboard's existing aggregation
     * is keyed on instrument (see QCDashboardServiceImpl), which a manual or RDT
     * control can never occupy — hence a separate roll-up rather than a filter over
     * that one.
     *
     * <p>
     * Returns {testSectionId, testId, source, totalRuns, failedRuns, lastRun}.
     * Aggregated in SQL: a busy lab unit runs controls all day and this must not
     * pull every row into memory to count them.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Object[]> summariseBenchQc(Timestamp startDate, Timestamp endDate, QCSource source)
            throws LIMSRuntimeException {
        String hql = "SELECT r.testSectionId, r.testId, r.source, COUNT(r.id),"
                + " SUM(CASE WHEN r.qualitativeOutcome IN (:failing) THEN 1 ELSE 0 END), MAX(r.runDateTime)"
                + " FROM QCResult r WHERE r.source IN (:sources)"
                + " AND r.runDateTime >= :startDate AND r.runDateTime < :endDate"
                + " GROUP BY r.testSectionId, r.testId, r.source ORDER BY MAX(r.runDateTime) DESC";
        try {
            var query = entityManager.createQuery(hql, Object[].class);
            query.setParameter("sources", source == null ? List.of(QCSource.MANUAL, QCSource.RDT) : List.of(source));
            query.setParameter("failing", List.of(QCQualitativeOutcome.FAIL, QCQualitativeOutcome.INVALID));
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error summarising bench QC activity", e);
        }
    }

    /**
     * Flat list of bench control runs in a window, newest first — the accreditation
     * register an assessor asks for (OGC-1147).
     *
     * <p>
     * Deliberately flat rather than folded into the chart export's lot sections:
     * that document is a Westgard review, sectioned per control lot and carrying
     * statistics and sigma. An RDT control has no lot and no statistics, so it has
     * no section to live in.
     */
    @Override
    @Transactional(readOnly = true)
    public List<QCResult> findBenchResults(Timestamp startDate, Timestamp endDate, QCSource source, int maxRows)
            throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCResult> cq = cb.createQuery(QCResult.class);
            Root<QCResult> root = cq.from(QCResult.class);
            cq.where(root.get("source").in(source == null ? List.of(QCSource.MANUAL, QCSource.RDT) : List.of(source)),
                    cb.greaterThanOrEqualTo(root.get("runDateTime"), startDate),
                    cb.lessThan(root.get("runDateTime"), endDate));
            cq.orderBy(cb.desc(root.get("runDateTime")));
            return entityManager.createQuery(cq).setMaxResults(maxRows).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving bench QC results for export", e);
        }
    }

    @Override
    public List<String> findDistinctInstrumentIds() throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<String> cq = cb.createQuery(String.class);
            Root<QCResult> root = cq.from(QCResult.class);
            cq.select(root.get("instrumentId")).distinct(true);
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving distinct instrument IDs from QC results", e);
        }
    }
}
