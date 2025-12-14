package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.valueholder.ChainOfCustodyEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ChainOfCustodyEventDAOImpl extends BaseDAOImpl<ChainOfCustodyEvent, Integer>
        implements ChainOfCustodyEventDAO {

    public ChainOfCustodyEventDAOImpl() {
        super(ChainOfCustodyEvent.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyEvent> findBySampleId(Integer sampleId) {
        try {
            String hql = "FROM ChainOfCustodyEvent WHERE sampleId = :sampleId ORDER BY eventTimestamp DESC";
            Query<ChainOfCustodyEvent> query = entityManager.unwrap(Session.class)
                    .createQuery(hql, ChainOfCustodyEvent.class);
            query.setParameter("sampleId", sampleId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding ChainOfCustodyEvents by sampleId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyEvent> findByAliquotId(Integer aliquotId) {
        try {
            String hql = "FROM ChainOfCustodyEvent WHERE aliquotId = :aliquotId ORDER BY eventTimestamp DESC";
            Query<ChainOfCustodyEvent> query = entityManager.unwrap(Session.class)
                    .createQuery(hql, ChainOfCustodyEvent.class);
            query.setParameter("aliquotId", aliquotId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding ChainOfCustodyEvents by aliquotId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyEvent> findByAction(ChainOfCustodyEvent.CustodyAction action) {
        try {
            String hql = "FROM ChainOfCustodyEvent WHERE action = :action ORDER BY eventTimestamp DESC";
            Query<ChainOfCustodyEvent> query = entityManager.unwrap(Session.class)
                    .createQuery(hql, ChainOfCustodyEvent.class);
            query.setParameter("action", action);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding ChainOfCustodyEvents by action", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyEvent> findPendingApprovals() {
        try {
            String hql = "FROM ChainOfCustodyEvent WHERE approvalStatus = 'PENDING' ORDER BY eventTimestamp ASC";
            Query<ChainOfCustodyEvent> query = entityManager.unwrap(Session.class)
                    .createQuery(hql, ChainOfCustodyEvent.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding pending approval ChainOfCustodyEvents", e);
        }
    }
}
