package org.openelisglobal.qc.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for QCControlLot entity.
 */
@Component
@Transactional
public class QCControlLotDAOImpl extends BaseDAOImpl<QCControlLot, String> implements QCControlLotDAO {

    public QCControlLotDAOImpl() {
        super(QCControlLot.class);
    }

    @Override
    public List<QCControlLot> getByTestAndInstrument(Integer testId, Integer instrumentId) throws LIMSRuntimeException {
        String hql = "FROM QCControlLot WHERE testId = :testId AND instrumentId = :instrumentId";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCControlLot> query = session.createQuery(hql, QCControlLot.class);
            query.setParameter("testId", testId);
            query.setParameter("instrumentId", instrumentId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving control lots by test and instrument", e);
        }
    }

    @Override
    public List<QCControlLot> getActiveByTestAndInstrument(Integer testId, Integer instrumentId)
            throws LIMSRuntimeException {
        String hql = "FROM QCControlLot WHERE testId = :testId AND instrumentId = :instrumentId AND status = 'ACTIVE'";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCControlLot> query = session.createQuery(hql, QCControlLot.class);
            query.setParameter("testId", testId);
            query.setParameter("instrumentId", instrumentId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving active control lots", e);
        }
    }

    @Override
    public QCControlLot getByLotNumber(String lotNumber) throws LIMSRuntimeException {
        String hql = "FROM QCControlLot WHERE lotNumber = :lotNumber";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCControlLot> query = session.createQuery(hql, QCControlLot.class);
            query.setParameter("lotNumber", lotNumber);
            return query.uniqueResult();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving control lot by lot number", e);
        }
    }
}
