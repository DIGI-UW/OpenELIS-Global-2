package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCaseSplitDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseSplit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCaseSplitDAOImpl extends BaseDAOImpl<MicroCaseSplit, String> implements MicroCaseSplitDAO {
    public MicroCaseSplitDAOImpl() {
        super(MicroCaseSplit.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseSplit> getByCaseId(String caseId) {
        Query<MicroCaseSplit> query = entityManager.unwrap(org.hibernate.Session.class).createQuery(
                "from MicroCaseSplit s where s.sourceCaseId = :caseId or s.resultCaseId = :caseId order by s.occurredAt, s.id",
                MicroCaseSplit.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }
}
