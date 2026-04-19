package org.openelisglobal.qc.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for QCStatistics entity.
 *
 * Uses JPA Criteria API instead of HQL because the project's
 * ClassicQueryTranslatorFactory does not resolve JPA @Column annotations in HQL
 * queries.
 */
@Component
@Transactional
public class QCStatisticsDAOImpl extends BaseDAOImpl<QCStatistics, String> implements QCStatisticsDAO {

    public QCStatisticsDAOImpl() {
        super(QCStatistics.class);
    }

    @Override
    public QCStatistics findLatestByControlLot(String controlLotId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCStatistics> cq = cb.createQuery(QCStatistics.class);
            Root<QCStatistics> root = cq.from(QCStatistics.class);
            cq.where(cb.equal(root.get("controlLotId"), controlLotId));
            cq.orderBy(cb.desc(root.get("calculationDate")));
            List<QCStatistics> results = entityManager.createQuery(cq).setMaxResults(1).getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving latest QC statistics", e);
        }
    }

    @Override
    public List<QCStatistics> findByCalculationMethod(String controlLotId, String calculationMethod)
            throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCStatistics> cq = cb.createQuery(QCStatistics.class);
            Root<QCStatistics> root = cq.from(QCStatistics.class);
            cq.where(cb.and(cb.equal(root.get("controlLotId"), controlLotId),
                    cb.equal(root.get("calculationMethod"), calculationMethod)));
            cq.orderBy(cb.desc(root.get("calculationDate")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC statistics by calculation method", e);
        }
    }

    @Override
    public List<QCStatistics> findAllByControlLot(String controlLotId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCStatistics> cq = cb.createQuery(QCStatistics.class);
            Root<QCStatistics> root = cq.from(QCStatistics.class);
            cq.where(cb.equal(root.get("controlLotId"), controlLotId));
            cq.orderBy(cb.desc(root.get("calculationDate")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving all QC statistics for control lot", e);
        }
    }
}
