package org.openelisglobal.qc.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.dto.TestInstrumentPair;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for QCControlLot entity.
 *
 * Uses JPA Criteria API instead of HQL to avoid Hibernate 6 column name
 * resolution issues where camelCase field names (e.g. testId) are lowercased to
 * "testid" instead of using @Column(name = "test_id").
 */
@Component
@Transactional
public class QCControlLotDAOImpl extends BaseDAOImpl<QCControlLot, String> implements QCControlLotDAO {

    public QCControlLotDAOImpl() {
        super(QCControlLot.class);
    }

    /**
     * A lot is "active" only if its status='ACTIVE' AND its expiration_date is null
     * or in the future. Without the date filter, lots whose status was never
     * demoted but whose date has passed still surface — picked up via PR review.
     */
    private static Predicate activeAndUnexpired(CriteriaBuilder cb, Root<QCControlLot> root) {
        Timestamp now = Timestamp.from(Instant.now());
        return cb.and(cb.equal(root.get("status"), "ACTIVE"),
                cb.or(cb.isNull(root.get("expirationDate")), cb.greaterThanOrEqualTo(root.get("expirationDate"), now)));
    }

    @Override
    public List<QCControlLot> getByTestAndInstrument(String testId, String instrumentId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCControlLot> cq = cb.createQuery(QCControlLot.class);
            Root<QCControlLot> root = cq.from(QCControlLot.class);
            cq.where(cb.equal(root.get("testId"), testId), cb.equal(root.get("instrumentId"), instrumentId));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving control lots by test and instrument", e);
        }
    }

    @Override
    public List<QCControlLot> getActiveByTestAndInstrument(String testId, String instrumentId)
            throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCControlLot> cq = cb.createQuery(QCControlLot.class);
            Root<QCControlLot> root = cq.from(QCControlLot.class);
            cq.where(cb.equal(root.get("testId"), testId), cb.equal(root.get("instrumentId"), instrumentId),
                    activeAndUnexpired(cb, root));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving active control lots", e);
        }
    }

    @Override
    public List<QCControlLot> getActiveByInstrument(String instrumentId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCControlLot> cq = cb.createQuery(QCControlLot.class);
            Root<QCControlLot> root = cq.from(QCControlLot.class);
            cq.where(cb.equal(root.get("instrumentId"), instrumentId), activeAndUnexpired(cb, root));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving active control lots by instrument", e);
        }
    }

    @Override
    public QCControlLot getByLotNumber(String lotNumber) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCControlLot> cq = cb.createQuery(QCControlLot.class);
            Root<QCControlLot> root = cq.from(QCControlLot.class);
            cq.where(cb.equal(root.get("lotNumber"), lotNumber));
            List<QCControlLot> results = entityManager.createQuery(cq).getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving control lot by lot number", e);
        }
    }

    @Override
    public long countActiveByInstrument(String instrumentId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<QCControlLot> root = cq.from(QCControlLot.class);
            cq.select(cb.count(root));
            cq.where(cb.equal(root.get("instrumentId"), instrumentId), activeAndUnexpired(cb, root));
            return entityManager.createQuery(cq).getSingleResult();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error counting active control lots by instrument", e);
        }
    }

    @Override
    public long countActiveByTestAndInstrument(String testId, String instrumentId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<QCControlLot> root = cq.from(QCControlLot.class);
            cq.select(cb.count(root));
            cq.where(cb.equal(root.get("testId"), testId), cb.equal(root.get("instrumentId"), instrumentId),
                    activeAndUnexpired(cb, root));
            return entityManager.createQuery(cq).getSingleResult();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error counting active control lots by test and instrument", e);
        }
    }

    @Override
    public List<TestInstrumentPair> findDistinctTestInstrumentPairs() throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
            Root<QCControlLot> root = cq.from(QCControlLot.class);
            cq.multiselect(root.get("testId"), root.get("instrumentId")).distinct(true);
            return entityManager.createQuery(cq).getResultList().stream()
                    .map(row -> new TestInstrumentPair((String) row[0], (String) row[1])).collect(Collectors.toList());
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving distinct test-instrument pairs from control lots", e);
        }
    }
}
