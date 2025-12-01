package org.openelisglobal.qc.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for QCStatistics entity.
 */
@Component
@Transactional
public class QCStatisticsDAOImpl extends BaseDAOImpl<QCStatistics, String> implements QCStatisticsDAO {

    public QCStatisticsDAOImpl() {
        super(QCStatistics.class);
    }

    @Override
    public QCStatistics findLatestByControlLot(String controlLotId) throws LIMSRuntimeException {
        String hql = "FROM QCStatistics WHERE controlLotId = :controlLotId ORDER BY calculationDate DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCStatistics> query = session.createQuery(hql, QCStatistics.class);
            query.setParameter("controlLotId", controlLotId);
            query.setMaxResults(1);
            return query.uniqueResult();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving latest QC statistics", e);
        }
    }

    @Override
    public List<QCStatistics> findByCalculationMethod(String controlLotId, String calculationMethod)
            throws LIMSRuntimeException {
        String hql = "FROM QCStatistics WHERE controlLotId = :controlLotId AND calculationMethod = :calculationMethod ORDER BY calculationDate DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCStatistics> query = session.createQuery(hql, QCStatistics.class);
            query.setParameter("controlLotId", controlLotId);
            query.setParameter("calculationMethod", calculationMethod);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC statistics by calculation method", e);
        }
    }

    @Override
    public List<QCStatistics> findAllByControlLot(String controlLotId) throws LIMSRuntimeException {
        String hql = "FROM QCStatistics WHERE controlLotId = :controlLotId ORDER BY calculationDate DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCStatistics> query = session.createQuery(hql, QCStatistics.class);
            query.setParameter("controlLotId", controlLotId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving all QC statistics for control lot", e);
        }
    }
}
