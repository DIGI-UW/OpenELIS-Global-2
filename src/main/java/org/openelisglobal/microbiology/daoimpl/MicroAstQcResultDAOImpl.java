package org.openelisglobal.microbiology.daoimpl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstQcResultDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstQcResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
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
    public List<MicroAstQcResult> getByControlIds(Collection<String> controlIds) {
        if (controlIds == null || controlIds.isEmpty()) {
            return List.of();
        }
        Query<MicroAstQcResult> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstQcResult r where r.controlId in :controlIds order by r.id", MicroAstQcResult.class);
        query.setParameter("controlIds", controlIds);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QCRuleViolation> getViolationByTriggeringResultId(String qcResultId) {
        Query<QCRuleViolation> query = entityManager.unwrap(Session.class).createQuery(
                "from QCRuleViolation v where v.triggeringResultId = :qcResultId order by v.id", QCRuleViolation.class);
        query.setParameter("qcResultId", qcResultId);
        query.setMaxResults(1);
        return query.uniqueResultOptional();
    }
}
