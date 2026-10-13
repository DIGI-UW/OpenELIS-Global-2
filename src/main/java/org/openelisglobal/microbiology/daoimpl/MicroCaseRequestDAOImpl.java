package org.openelisglobal.microbiology.daoimpl;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCaseRequestDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseRequest;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCaseRequestDAOImpl extends BaseDAOImpl<MicroCaseRequest, String> implements MicroCaseRequestDAO {
    public MicroCaseRequestDAOImpl() {
        super(MicroCaseRequest.class);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseRequest getActiveByRequestAndTest(Integer requestId, String testId) {
        Query<MicroCaseRequest> query = entityManager.unwrap(org.hibernate.Session.class).createQuery(
                "from MicroCaseRequest r where r.sampleTypeRequestId = :requestId and r.testId = :testId and r.cancelledAt is null",
                MicroCaseRequest.class);
        query.setParameter("requestId", requestId);
        query.setParameter("testId", testId);
        return query.uniqueResultOptional().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseRequest> getByCaseId(String caseId) {
        Query<MicroCaseRequest> query = entityManager.unwrap(org.hibernate.Session.class).createQuery(
                "from MicroCaseRequest r where r.caseId = :caseId order by r.requestedAt, r.id",
                MicroCaseRequest.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseRequest> getBySampleItemId(String sampleItemId) {
        Query<MicroCaseRequest> query = entityManager.unwrap(org.hibernate.Session.class)
                .createQuery("from MicroCaseRequest r where r.sampleItemId = :sampleItemId", MicroCaseRequest.class);
        query.setParameter("sampleItemId", sampleItemId);
        return query.list();
    }

    @Override
    public SampleTypeRequest lockSampleTypeRequest(Integer requestId) {
        return entityManager.find(SampleTypeRequest.class, requestId, LockModeType.PESSIMISTIC_WRITE);
    }
}
