package org.openelisglobal.qc.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
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

    @Override
    public List<QCControlLot> getByTestAndInstrument(Integer testId, Integer instrumentId) throws LIMSRuntimeException {
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
    public List<QCControlLot> getActiveByTestAndInstrument(Integer testId, Integer instrumentId)
            throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCControlLot> cq = cb.createQuery(QCControlLot.class);
            Root<QCControlLot> root = cq.from(QCControlLot.class);
            cq.where(cb.equal(root.get("testId"), testId), cb.equal(root.get("instrumentId"), instrumentId),
                    cb.equal(root.get("status"), "ACTIVE"));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving active control lots", e);
        }
    }

    @Override
    public List<QCControlLot> getActiveByInstrument(Integer instrumentId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCControlLot> cq = cb.createQuery(QCControlLot.class);
            Root<QCControlLot> root = cq.from(QCControlLot.class);
            cq.where(cb.equal(root.get("instrumentId"), instrumentId), cb.equal(root.get("status"), "ACTIVE"));
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
    public long countActiveByInstrument(Integer instrumentId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<QCControlLot> root = cq.from(QCControlLot.class);
            cq.select(cb.count(root));
            cq.where(cb.equal(root.get("instrumentId"), instrumentId), cb.equal(root.get("status"), "ACTIVE"));
            return entityManager.createQuery(cq).getSingleResult();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error counting active control lots by instrument", e);
        }
    }

    @Override
    public long countActiveByTestAndInstrument(Integer testId, Integer instrumentId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<QCControlLot> root = cq.from(QCControlLot.class);
            cq.select(cb.count(root));
            cq.where(cb.equal(root.get("testId"), testId), cb.equal(root.get("instrumentId"), instrumentId),
                    cb.equal(root.get("status"), "ACTIVE"));
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
                    .map(row -> new TestInstrumentPair((Integer) row[0], (Integer) row[1]))
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving distinct test-instrument pairs from control lots", e);
        }
    }
}
