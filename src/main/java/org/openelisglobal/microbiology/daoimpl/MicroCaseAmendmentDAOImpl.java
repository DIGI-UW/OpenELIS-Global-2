package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCaseAmendmentDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendmentStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCaseAmendmentDAOImpl extends BaseDAOImpl<MicroCaseAmendment, String>
        implements MicroCaseAmendmentDAO {

    public MicroCaseAmendmentDAOImpl() {
        super(MicroCaseAmendment.class);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseAmendment getOpenByCaseId(String caseId) {
        Query<MicroCaseAmendment> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCaseAmendment a where a.caseId = :caseId and a.status = :status", MicroCaseAmendment.class);
        query.setParameter("caseId", caseId);
        query.setParameter("status", MicroCaseAmendmentStatus.OPEN.name());
        return query.uniqueResultOptional().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseAmendment> getByCaseId(String caseId) {
        Query<MicroCaseAmendment> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCaseAmendment a where a.caseId = :caseId order by a.sequenceNumber",
                MicroCaseAmendment.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public int getNextSequence(String caseId) {
        Query<Number> query = entityManager.unwrap(Session.class).createQuery(
                "select max(a.sequenceNumber) from MicroCaseAmendment a where a.caseId = :caseId", Number.class);
        query.setParameter("caseId", caseId);
        Number current = query.uniqueResult();
        return current == null ? 1 : current.intValue() + 1;
    }
}
