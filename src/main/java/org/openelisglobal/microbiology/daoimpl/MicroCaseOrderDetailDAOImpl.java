package org.openelisglobal.microbiology.daoimpl;

import java.sql.Timestamp;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCaseOrderDetailDAOImpl extends BaseDAOImpl<MicroCaseOrderDetail, String>
        implements MicroCaseOrderDetailDAO {

    public MicroCaseOrderDetailDAOImpl() {
        super(MicroCaseOrderDetail.class);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseOrderDetail getByCaseId(String caseId) {
        Query<MicroCaseOrderDetail> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCaseOrderDetail d where d.caseId = :caseId", MicroCaseOrderDetail.class);
        query.setParameter("caseId", caseId);
        return query.uniqueResultOptional().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseOrderDetail getDraftBySampleId(String sampleId) {
        Query<MicroCaseOrderDetail> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCaseOrderDetail d where d.sampleId = :sampleId and d.caseId is null"
                        + " and d.discardedAt is null", MicroCaseOrderDetail.class);
        query.setParameter("sampleId", sampleId);
        return query.uniqueResultOptional().orElse(null);
    }

    /**
     * Includes a retired draft, because the sample owns at most one draft row and
     * re-qualifying reuses it rather than inserting a second one.
     */
    @Override
    @Transactional(readOnly = true)
    public MicroCaseOrderDetail getAnyDraftBySampleId(String sampleId) {
        Query<MicroCaseOrderDetail> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCaseOrderDetail d where d.sampleId = :sampleId and d.caseId is null",
                MicroCaseOrderDetail.class);
        query.setParameter("sampleId", sampleId);
        return query.uniqueResultOptional().orElse(null);
    }

    @Override
    public void discardDraftBySampleId(String sampleId, Timestamp discardedAt, String discardedBy) {
        MicroCaseOrderDetail draft = getDraftBySampleId(sampleId);
        if (draft != null) {
            draft.setDiscardedAt(discardedAt);
            draft.setDiscardedBy(discardedBy);
            update(draft);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseOrderDetail> getByCaseIds(List<String> caseIds) {
        if (caseIds == null || caseIds.isEmpty()) {
            return List.of();
        }
        Query<MicroCaseOrderDetail> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCaseOrderDetail d where d.caseId in (:caseIds)", MicroCaseOrderDetail.class);
        query.setParameterList("caseIds", caseIds);
        return query.list();
    }
}
