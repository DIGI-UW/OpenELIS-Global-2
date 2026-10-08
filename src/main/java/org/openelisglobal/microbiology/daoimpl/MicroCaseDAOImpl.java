package org.openelisglobal.microbiology.daoimpl;

import java.sql.Timestamp;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCaseDAOImpl extends BaseDAOImpl<MicroCase, String> implements MicroCaseDAO {

    static final String FINALIZED_BACTERIOLOGY_BY_COLLECTION_DATE_HQL = "select c from MicroCase c"
            + " join SampleItem sampleItem on sampleItem.id = c.sampleItemId"
            + " where c.workflowType = :workflowType and c.finalReleaseState = :finalReleaseState"
            + " and sampleItem.collectionDate >= :fromInclusive"
            + " and sampleItem.collectionDate < :toExclusive order by sampleItem.collectionDate, c.id";

    public MicroCaseDAOImpl() {
        super(MicroCase.class);
    }

    @Override
    public MicroCase getForUpdate(String caseId) {
        return entityManager.find(MicroCase.class, caseId, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public void lockOrder(String sampleId) {
        if (entityManager.find(org.openelisglobal.sample.valueholder.Sample.class, sampleId,
                jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new IllegalArgumentException("Order not found");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getByOrder(String sampleId) {
        return entityManager.unwrap(Session.class)
                .createQuery("from MicroCase c where c.sampleId = :sampleId order by c.createdAt, c.id", MicroCase.class)
                .setParameter("sampleId", sampleId).list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getBySampleItem(String sampleItemId) {
        Query<MicroCase> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCase c where c.sampleItemId = :sampleItemId" + " order by c.createdAt, c.id",
                MicroCase.class);
        query.setParameter("sampleItemId", sampleItemId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getOpenCases() {
        Query<MicroCase> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCase c where c.closedAt is null order by c.createdAt", MicroCase.class);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getFinalizedBacteriologyByCollectionDateRange(Timestamp fromInclusive,
            Timestamp toExclusive) {
        Query<MicroCase> query = entityManager.unwrap(Session.class)
                .createQuery(FINALIZED_BACTERIOLOGY_BY_COLLECTION_DATE_HQL, MicroCase.class);
        query.setParameter("workflowType", "BACTERIOLOGY");
        query.setParameter("finalReleaseState", "FINAL_RELEASED");
        query.setParameter("fromInclusive", fromInclusive);
        query.setParameter("toExclusive", toExclusive);
        return query.list();
    }

}
