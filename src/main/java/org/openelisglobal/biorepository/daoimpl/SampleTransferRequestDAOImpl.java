package org.openelisglobal.biorepository.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.biorepository.dao.SampleTransferRequestDAO;
import org.openelisglobal.biorepository.valueholder.SampleTransferItem.ItemStatus;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest.TransferStatus;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;

/**
 * DAO implementation for SampleTransferRequest entity operations.
 */
@Component
public class SampleTransferRequestDAOImpl extends BaseDAOImpl<SampleTransferRequest, Integer>
        implements SampleTransferRequestDAO {

    public SampleTransferRequestDAOImpl() {
        super(SampleTransferRequest.class);
    }

    @Override
    public List<SampleTransferRequest> getByStatus(TransferStatus status) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleTransferRequest r WHERE r.status = :status ORDER BY r.requestedTimestamp DESC";
        return session.createQuery(hql, SampleTransferRequest.class).setParameter("status", status.name())
                .getResultList();
    }

    @Override
    public List<SampleTransferRequest> getPendingRequests(int limit) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleTransferRequest r WHERE r.status = :status ORDER BY r.requestedTimestamp ASC";
        return session.createQuery(hql, SampleTransferRequest.class)
                .setParameter("status", TransferStatus.PENDING.name()).setMaxResults(limit).getResultList();
    }

    @Override
    public List<SampleTransferRequest> getBySourceLab(String sourceLab) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleTransferRequest r WHERE r.sourceLab = :sourceLab ORDER BY r.requestedTimestamp DESC";
        return session.createQuery(hql, SampleTransferRequest.class).setParameter("sourceLab", sourceLab)
                .getResultList();
    }

    @Override
    public List<SampleTransferRequest> getByRequestedByUserId(Integer requestedByUserId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleTransferRequest r WHERE r.requestedBy.id = :requestedByUserId "
                + "ORDER BY r.requestedTimestamp DESC";
        return session.createQuery(hql, SampleTransferRequest.class)
                .setParameter("requestedByUserId", requestedByUserId).getResultList();
    }

    @Override
    public List<SampleTransferRequest> getBySampleItemId(Integer sampleItemId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT DISTINCT r FROM SampleTransferRequest r JOIN r.items i "
                + "WHERE i.sampleItem.id = :sampleItemId ORDER BY r.requestedTimestamp DESC";
        return session.createQuery(hql, SampleTransferRequest.class).setParameter("sampleItemId", sampleItemId)
                .getResultList();
    }

    @Override
    public boolean hasPendingTransferForSampleItem(Integer sampleItemId) {
        Session session = entityManager.unwrap(Session.class);
        String sql = "SELECT COUNT(*) FROM clinlims.sample_transfer_item "
                + "WHERE sample_item_id = :sampleItemId AND status = :status";
        Long count = ((Number) session.createNativeQuery(sql).setParameter("sampleItemId", sampleItemId)
                .setParameter("status", ItemStatus.PENDING.name()).getSingleResult()).longValue();
        return count > 0;
    }

    @Override
    public long countByStatus(TransferStatus status) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(r) FROM SampleTransferRequest r WHERE r.status = :status";
        return session.createQuery(hql, Long.class).setParameter("status", status.name()).getSingleResult();
    }

    @Override
    public long countBySourceLab(String sourceLab) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(r) FROM SampleTransferRequest r WHERE r.sourceLab = :sourceLab";
        return session.createQuery(hql, Long.class).setParameter("sourceLab", sourceLab).getSingleResult();
    }

    @Override
    public List<String> getDistinctSourceLabs() {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT DISTINCT r.sourceLab FROM SampleTransferRequest r ORDER BY r.sourceLab";
        return session.createQuery(hql, String.class).getResultList();
    }
}
