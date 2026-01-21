package org.openelisglobal.biorepository.daoimpl;

import java.time.LocalDate;
import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.biorepository.dao.SampleRetrievalItemDAO;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem.ItemStatus;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;

/**
 * DAO implementation for SampleRetrievalItem entity operations.
 */
@Component
public class SampleRetrievalItemDAOImpl extends BaseDAOImpl<SampleRetrievalItem, Integer>
        implements SampleRetrievalItemDAO {

    public SampleRetrievalItemDAOImpl() {
        super(SampleRetrievalItem.class);
    }

    @Override
    public List<SampleRetrievalItem> getByRetrievalRequestId(Integer retrievalRequestId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRetrievalItem i WHERE i.retrievalRequest.id = :requestId";
        return session.createQuery(hql, SampleRetrievalItem.class).setParameter("requestId", retrievalRequestId)
                .getResultList();
    }

    @Override
    public List<SampleRetrievalItem> getByBioSampleId(Integer bioSampleId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRetrievalItem i WHERE i.bioSample.id = :bioSampleId "
                + "ORDER BY i.retrievedTimestamp DESC NULLS LAST";
        return session.createQuery(hql, SampleRetrievalItem.class).setParameter("bioSampleId", bioSampleId)
                .getResultList();
    }

    @Override
    public List<SampleRetrievalItem> getByStatus(ItemStatus status) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRetrievalItem i WHERE i.status = :status";
        return session.createQuery(hql, SampleRetrievalItem.class).setParameter("status", status.name())
                .getResultList();
    }

    @Override
    public List<SampleRetrievalItem> getCheckedOutItems() {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRetrievalItem i WHERE i.status IN (:statuses) " + "ORDER BY i.retrievedTimestamp DESC";
        return session.createQuery(hql, SampleRetrievalItem.class)
                .setParameterList("statuses", List.of(ItemStatus.RETRIEVED.name(), ItemStatus.IN_ANALYSIS.name()))
                .getResultList();
    }

    @Override
    public List<SampleRetrievalItem> getOverdueItems() {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM SampleRetrievalItem i WHERE i.status IN (:statuses) "
                + "AND i.returnExpected = true AND i.expectedReturnDate < :today "
                + "ORDER BY i.expectedReturnDate ASC";
        return session.createQuery(hql, SampleRetrievalItem.class)
                .setParameterList("statuses", List.of(ItemStatus.RETRIEVED.name(), ItemStatus.IN_ANALYSIS.name()))
                .setParameter("today", LocalDate.now()).getResultList();
    }

    @Override
    public long countByStatus(ItemStatus status) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(i) FROM SampleRetrievalItem i WHERE i.status = :status";
        return session.createQuery(hql, Long.class).setParameter("status", status.name()).getSingleResult();
    }

    @Override
    public boolean hasActiveItemForBioSample(Integer bioSampleId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(i) FROM SampleRetrievalItem i WHERE i.bioSample.id = :bioSampleId "
                + "AND i.status IN (:statuses)";
        Long count = session.createQuery(hql, Long.class).setParameter("bioSampleId", bioSampleId)
                .setParameterList("statuses",
                        List.of(ItemStatus.PENDING.name(), ItemStatus.RETRIEVED.name(), ItemStatus.IN_ANALYSIS.name()))
                .getSingleResult();
        return count > 0;
    }
}
