package org.openelisglobal.microbiology.daoimpl;

import java.util.Collection;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstQcResultDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstQcResult;
import org.openelisglobal.qaevent.daoimpl.NceSpecimenDAOImpl;
import org.openelisglobal.qaevent.service.QcViolationNceServiceImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAstQcResultDAOImpl extends BaseDAOImpl<MicroAstQcResult, String> implements MicroAstQcResultDAO {

    public MicroAstQcResultDAOImpl() {
        super(MicroAstQcResult.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstQcResult> getByRunId(String runId) {
        Query<MicroAstQcResult> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroAstQcResult r where r.astRunId = :runId order by r.id", MicroAstQcResult.class);
        query.setParameter("runId", runId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUnresolvedRejection(Collection<String> controlIds) {
        return !entityManager.createQuery("select r.id from MicroAstQcResult r, QCResult q"
                + " where r.controlId in :controlIds and q.id = r.qcResultId and q.resultStatus = 'REJECTED'"
                + " and not exists (select v.id from QCRuleViolation v, NcEvent n where v.triggeringResultId = q.id"
                + " and n.triggerSourceType = :source and n.triggerSourceId = v.id and n.status in :closed)",
                String.class).setParameter("controlIds", controlIds)
                .setParameter("source", QcViolationNceServiceImpl.TRIGGER_SOURCE_QC_VIOLATION)
                .setParameter("closed", NceSpecimenDAOImpl.CLOSED_STATUSES).setMaxResults(1).getResultList().isEmpty();
    }
}
