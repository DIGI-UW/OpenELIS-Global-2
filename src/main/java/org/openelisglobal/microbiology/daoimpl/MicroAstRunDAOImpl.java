package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAstRunDAOImpl extends BaseDAOImpl<MicroAstRun, String> implements MicroAstRunDAO {

    public MicroAstRunDAOImpl() {
        super(MicroAstRun.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstRun> getByIsolateId(String isolateId) {
        Query<MicroAstRun> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstRun r where r.isolateId = :isolateId order by r.startedAt, r.id", MicroAstRun.class);
        query.setParameter("isolateId", isolateId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstRun> getByIsolateIds(List<String> isolateIds) {
        if (isolateIds == null || isolateIds.isEmpty()) {
            return List.of();
        }
        Query<MicroAstRun> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstRun r where r.isolateId in (:isolateIds) order by r.isolateId, r.startedAt, r.id",
                MicroAstRun.class);
        query.setParameterList("isolateIds", isolateIds);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstRun> getByAmendmentId(String amendmentId) {
        Query<MicroAstRun> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstRun r where r.amendmentId = :amendmentId order by r.startedAt", MicroAstRun.class);
        query.setParameter("amendmentId", amendmentId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroAstRun> getByAnalyzerAndCard(String analyzerId, String cardId) {
        Query<MicroAstRun> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstRun r where r.analyzerInstrumentId = :analyzerId and r.analyzerCardId = :cardId "
                        + "and r.status in ('AWAITING_RESULTS', 'RESULTS_IN', 'QC_FAILED') order by r.startedAt desc",
                MicroAstRun.class);
        query.setParameter("analyzerId", analyzerId);
        query.setParameter("cardId", cardId);
        query.setMaxResults(1);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnresolvedByBreakpointStandardId(String breakpointStandardId) {
        Query<Long> query = entityManager.unwrap(Session.class)
                .createQuery("select count(r.id) from MicroAstRun r where r.breakpointStandardId = :standardId"
                        + " and r.status = 'IN_PROGRESS'", Long.class);
        query.setParameter("standardId", breakpointStandardId);
        return query.uniqueResult();
    }
}
