package org.openelisglobal.microbiology.daoimpl;

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
        Query<MicroCaseOrderDetail> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCaseOrderDetail d where d.sampleId = :sampleId and d.caseId is null",
                MicroCaseOrderDetail.class);
        query.setParameter("sampleId", sampleId);
        return query.uniqueResultOptional().orElse(null);
    }
}
