package org.openelisglobal.qc.dao;

import java.sql.Timestamp;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for QCResult entity.
 */
@Component
@Transactional
public class QCResultDAOImpl extends BaseDAOImpl<QCResult, String> implements QCResultDAO {

    public QCResultDAOImpl() {
        super(QCResult.class);
    }

    @Override
    public List<QCResult> findByControlLot(String controlLotId) throws LIMSRuntimeException {
        String hql = "FROM QCResult WHERE controlLotId = :controlLotId ORDER BY runDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCResult> query = session.createQuery(hql, QCResult.class);
            query.setParameter("controlLotId", controlLotId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC results by control lot", e);
        }
    }

    @Override
    public List<QCResult> findHistoricalForRule(String controlLotId, int limit) throws LIMSRuntimeException {
        String hql = "FROM QCResult WHERE controlLotId = :controlLotId ORDER BY runDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCResult> query = session.createQuery(hql, QCResult.class);
            query.setParameter("controlLotId", controlLotId);
            query.setMaxResults(limit);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving historical QC results", e);
        }
    }

    @Override
    public List<QCResult> findByInstrumentAndDateRange(Integer instrumentId, Timestamp startDate, Timestamp endDate)
            throws LIMSRuntimeException {
        String hql = "FROM QCResult WHERE instrumentId = :instrumentId AND runDateTime >= :startDate AND runDateTime <= :endDate ORDER BY runDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCResult> query = session.createQuery(hql, QCResult.class);
            query.setParameter("instrumentId", instrumentId);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC results by instrument and date range", e);
        }
    }

    @Override
    public List<QCResult> findLatestByControlLot(String controlLotId, int limit) throws LIMSRuntimeException {
        return findHistoricalForRule(controlLotId, limit);
    }
}
