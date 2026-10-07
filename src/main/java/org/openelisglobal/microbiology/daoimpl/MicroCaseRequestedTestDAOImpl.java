package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCaseRequestedTestDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseRequestedTest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCaseRequestedTestDAOImpl extends BaseDAOImpl<MicroCaseRequestedTest, String>
        implements MicroCaseRequestedTestDAO {
    public MicroCaseRequestedTestDAOImpl() {
        super(MicroCaseRequestedTest.class);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseRequestedTest getByRequestAndTest(Integer requestId, String testId) {
        return entityManager.createQuery(
                "from MicroCaseRequestedTest r where r.requestId = :requestId and r.testId = :testId and r.cancelledAt is null",
                MicroCaseRequestedTest.class).setParameter("requestId", requestId).setParameter("testId", testId)
                .getResultList().stream().findFirst().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseRequestedTest> getByCaseId(String caseId) {
        return entityManager
                .createQuery("from MicroCaseRequestedTest r where r.caseId = :caseId order by r.createdAt, r.id",
                        MicroCaseRequestedTest.class)
                .setParameter("caseId", caseId).getResultList();
    }
}
