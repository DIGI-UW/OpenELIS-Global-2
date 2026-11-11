package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCaseActivityDAOImpl extends BaseDAOImpl<MicroCaseActivity, String> implements MicroCaseActivityDAO {

    public MicroCaseActivityDAOImpl() {
        super(MicroCaseActivity.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseActivity> getByCaseId(String caseId) {
        Query<MicroCaseActivity> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCaseActivity a where a.caseId = :caseId" + " order by a.occurredAt, a.id",
                MicroCaseActivity.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }
}
