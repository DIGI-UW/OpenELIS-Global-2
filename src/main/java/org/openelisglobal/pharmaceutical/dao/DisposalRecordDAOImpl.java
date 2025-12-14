package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.valueholder.DisposalRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DisposalRecordDAOImpl extends BaseDAOImpl<DisposalRecord, Integer> implements DisposalRecordDAO {

    public DisposalRecordDAOImpl() {
        super(DisposalRecord.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisposalRecord> findBySampleId(Integer sampleId) {
        try {
            String hql = "FROM DisposalRecord WHERE sampleId = :sampleId ORDER BY requestedAt DESC";
            Query<DisposalRecord> query = entityManager.unwrap(Session.class).createQuery(hql, DisposalRecord.class);
            query.setParameter("sampleId", sampleId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding DisposalRecords by sampleId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisposalRecord> findByStatus(DisposalRecord.DisposalStatus status) {
        try {
            String hql = "FROM DisposalRecord WHERE status = :status ORDER BY requestedAt DESC";
            Query<DisposalRecord> query = entityManager.unwrap(Session.class).createQuery(hql, DisposalRecord.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding DisposalRecords by status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisposalRecord> findPendingApprovals() {
        try {
            String hql = "FROM DisposalRecord WHERE status = 'PENDING_APPROVAL' ORDER BY requestedAt ASC";
            Query<DisposalRecord> query = entityManager.unwrap(Session.class).createQuery(hql, DisposalRecord.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding pending approval DisposalRecords", e);
        }
    }
}
