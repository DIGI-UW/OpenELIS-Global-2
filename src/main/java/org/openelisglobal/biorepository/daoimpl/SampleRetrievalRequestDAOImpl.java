package org.openelisglobal.biorepository.daoimpl;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.biorepository.dao.SampleRetrievalRequestDAO;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem.ItemStatus;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.RequestStatus;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;

/**
 * DAO implementation for SampleRetrievalRequest entity operations.
 */
@Component
public class SampleRetrievalRequestDAOImpl extends BaseDAOImpl<SampleRetrievalRequest, Integer>
        implements SampleRetrievalRequestDAO {

    public SampleRetrievalRequestDAOImpl() {
        super(SampleRetrievalRequest.class);
    }

    @Override
    public List<SampleRetrievalRequest> getByStatus(RequestStatus status) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRetrievalRequest r WHERE r.status = :status ORDER BY r.requestedTimestamp DESC";
        return session.createQuery(hql, SampleRetrievalRequest.class).setParameter("status", status.name())
                .getResultList();
    }

    @Override
    public List<SampleRetrievalRequest> getPendingApproval(int limit) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRetrievalRequest r WHERE r.status = :status ORDER BY r.requestedTimestamp ASC";
        return session.createQuery(hql, SampleRetrievalRequest.class)
                .setParameter("status", RequestStatus.PENDING_APPROVAL.name()).setMaxResults(limit).getResultList();
    }

    @Override
    public List<SampleRetrievalRequest> getByRequestedByUserId(Integer requestedByUserId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRetrievalRequest r WHERE r.requestedBy.id = :requestedByUserId "
                + "ORDER BY r.requestedTimestamp DESC";
        return session.createQuery(hql, SampleRetrievalRequest.class)
                .setParameter("requestedByUserId", requestedByUserId).getResultList();
    }

    @Override
    public List<SampleRetrievalRequest> getByProjectId(Integer projectId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRetrievalRequest r WHERE r.project.id = :projectId "
                + "ORDER BY r.requestedTimestamp DESC";
        return session.createQuery(hql, SampleRetrievalRequest.class).setParameter("projectId", projectId)
                .getResultList();
    }

    @Override
    public List<SampleRetrievalRequest> getByBioSampleId(Integer bioSampleId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT DISTINCT r FROM SampleRetrievalRequest r JOIN r.items i "
                + "WHERE i.bioSample.id = :bioSampleId ORDER BY r.requestedTimestamp DESC";
        return session.createQuery(hql, SampleRetrievalRequest.class).setParameter("bioSampleId", bioSampleId)
                .getResultList();
    }

    @Override
    public boolean hasActiveRetrievalForBioSample(Integer bioSampleId) {
        Session session = entityManager.unwrap(Session.class);
        String sql = "SELECT COUNT(*) FROM clinlims.sample_retrieval_item "
                + "WHERE bio_sample_id = :bioSampleId AND item_status IN (:statuses)";
        Long count = ((Number) session.createNativeQuery(sql).setParameter("bioSampleId", bioSampleId)
                .setParameterList("statuses",
                        List.of(ItemStatus.PENDING.name(), ItemStatus.RETRIEVED.name(), ItemStatus.IN_ANALYSIS.name()))
                .getSingleResult()).longValue();
        return count > 0;
    }

    @Override
    public long countByStatus(RequestStatus status) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(r) FROM SampleRetrievalRequest r WHERE r.status = :status";
        return session.createQuery(hql, Long.class).setParameter("status", status.name()).getSingleResult();
    }

    @Override
    public List<SampleRetrievalRequest> getWithCheckedOutItems() {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT DISTINCT r FROM SampleRetrievalRequest r JOIN r.items i "
                + "WHERE i.status IN (:statuses) ORDER BY r.requestedTimestamp DESC";
        return session.createQuery(hql, SampleRetrievalRequest.class)
                .setParameterList("statuses", List.of(ItemStatus.RETRIEVED.name(), ItemStatus.IN_ANALYSIS.name()))
                .getResultList();
    }

    @Override
    public List<SampleRetrievalRequest> getWithOverdueReturns() {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT DISTINCT r FROM SampleRetrievalRequest r JOIN r.items i "
                + "WHERE i.status IN (:statuses) AND i.returnExpected = true "
                + "AND i.expectedReturnDate < :today ORDER BY i.expectedReturnDate ASC";
        return session.createQuery(hql, SampleRetrievalRequest.class)
                .setParameterList("statuses", List.of(ItemStatus.RETRIEVED.name(), ItemStatus.IN_ANALYSIS.name()))
                .setParameter("today", LocalDate.now()).getResultList();
    }

    @Override
    public SampleRetrievalRequest getByRequestNumber(String requestNumber) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRetrievalRequest r WHERE r.requestNumber = :requestNumber";
        List<SampleRetrievalRequest> results = session.createQuery(hql, SampleRetrievalRequest.class)
                .setParameter("requestNumber", requestNumber).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public int getNextRequestNumberSequence() {
        Session session = entityManager.unwrap(Session.class);
        String sql = "SELECT nextval('clinlims.sample_retrieval_request_seq')";
        BigInteger result = (BigInteger) session.createNativeQuery(sql).getSingleResult();
        return result.intValue();
    }
}
