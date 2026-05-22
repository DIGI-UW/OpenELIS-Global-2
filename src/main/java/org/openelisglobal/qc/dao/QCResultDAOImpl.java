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
import org.openelisglobal.qc.valueholder.QCResult;
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
