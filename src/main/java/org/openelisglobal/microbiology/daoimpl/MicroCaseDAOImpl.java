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

    public MicroCaseDAOImpl() {
        super(MicroCase.class);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCase getBySampleItemAndWorkflow(String sampleItemId, String workflowType) {
        Query<MicroCase> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCase c where c.sampleItemId = :sampleItemId" + " and c.workflowType = :workflowType",
                MicroCase.class);
        query.setParameter("sampleItemId", sampleItemId);
        query.setParameter("workflowType", workflowType);
        return query.uniqueResultOptional().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getBySampleItem(String sampleItemId) {
        Query<MicroCase> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCase c where c.sampleItemId = :sampleItemId" + " order by c.workflowType", MicroCase.class);
        query.setParameter("sampleItemId", sampleItemId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getBySampleItemIds(List<String> sampleItemIds) {
        if (sampleItemIds == null || sampleItemIds.isEmpty()) {
            return List.of();
        }
        Query<MicroCase> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCase c where c.sampleItemId in (:sampleItemIds)"
                        + " order by c.sampleItemId, c.workflowType", MicroCase.class);
        query.setParameterList("sampleItemIds", sampleItemIds);
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
    public List<MicroCase> getFinalizedBacteriologyByClosedAtRange(Timestamp fromInclusive, Timestamp toExclusive) {
        Query<MicroCase> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCase c where c.workflowType = :workflowType"
                        + " and c.finalReleaseState = :finalReleaseState and c.closedAt >= :fromInclusive"
                        + " and c.closedAt < :toExclusive order by c.closedAt, c.id", MicroCase.class);
        query.setParameter("workflowType", "BACTERIOLOGY");
        query.setParameter("finalReleaseState", "FINAL_RELEASED");
        query.setParameter("fromInclusive", fromInclusive);
        query.setParameter("toExclusive", toExclusive);
        return query.list();
    }
}
