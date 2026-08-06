package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCaseInoculationDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseInoculation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCaseInoculationDAOImpl extends BaseDAOImpl<MicroCaseInoculation, String>
        implements MicroCaseInoculationDAO {

    public MicroCaseInoculationDAOImpl() {
        super(MicroCaseInoculation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseInoculation> getByCaseId(String caseId) {
        Query<MicroCaseInoculation> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCaseInoculation i where i.caseId = :caseId order by i.occurredAt, i.id",
                MicroCaseInoculation.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }
}
