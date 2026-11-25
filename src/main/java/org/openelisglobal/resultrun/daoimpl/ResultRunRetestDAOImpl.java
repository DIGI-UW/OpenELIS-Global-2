package org.openelisglobal.resultrun.daoimpl;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.resultrun.dao.ResultRunRetestDAO;
import org.openelisglobal.resultrun.valueholder.ResultRunRetest;
import org.openelisglobal.resultrun.valueholder.RunTestAction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ResultRunRetestDAOImpl extends BaseDAOImpl<ResultRunRetest, Long> implements ResultRunRetestDAO {

    private static final String SAMPLES = "SELECT d.runId, d.testId, r.dispositionId, r.analysisId, r.retestedInRunId"
            + " FROM ResultRunRetest r, ResultRunTestDisposition d WHERE d.id = r.dispositionId AND d.action = :retest";

    public ResultRunRetestDAOImpl() {
        super(ResultRunRetest.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetestedSample> findUnlinked(String testId, Collection<String> analysisIds, Long beforeRunId) {
        if (analysisIds == null || analysisIds.isEmpty()) {
            return List.of();
        }
        try {
            return samples(session().createQuery(SAMPLES + " AND d.testId = :testId AND d.runId < :before"
                    + " AND r.retestedInRunId IS NULL AND r.analysisId IN (:analysisIds) ORDER BY d.runId, r.id",
                    Object[].class).setParameter("retest", RunTestAction.RETEST).setParameter("testId", testId)
                    .setParameter("before", beforeRunId).setParameterList("analysisIds", analysisIds).list());
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error finding the samples a later run may retest", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetestedSample> findSetAsideBy(Long runId) {
        try {
            return samples(session().createQuery(SAMPLES + " AND d.runId = :runId ORDER BY r.id", Object[].class)
                    .setParameter("retest", RunTestAction.RETEST).setParameter("runId", runId).list());
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error finding the samples a run set aside for retest", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetestedSample> findRetestedIn(Long runId) {
        try {
            return samples(session()
                    .createQuery(SAMPLES + " AND r.retestedInRunId = :runId ORDER BY d.runId, r.id", Object[].class)
                    .setParameter("retest", RunTestAction.RETEST).setParameter("runId", runId).list());
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error finding the samples a run retested", e);
        }
    }

    @Override
    public int link(Long dispositionId, Collection<String> analysisIds, Long retestRunId, String sysUserId,
            Timestamp linkedAt) {
        if (analysisIds == null || analysisIds.isEmpty()) {
            return 0;
        }
        try {
            return session()
                    .createQuery("UPDATE ResultRunRetest r SET r.retestedInRunId = :run, r.linkedBy = :user,"
                            + " r.linkedAt = :at WHERE r.dispositionId = :disposition"
                            + " AND r.analysisId IN (:analysisIds) AND r.retestedInRunId IS NULL")
                    .setParameter("run", retestRunId).setParameter("user", sysUserId).setParameter("at", linkedAt)
                    .setParameter("disposition", dispositionId).setParameterList("analysisIds", analysisIds)
                    .executeUpdate();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error linking retested samples to their retest run", e);
        }
    }

    private Session session() {
        return entityManager.unwrap(Session.class);
    }

    private static List<RetestedSample> samples(List<Object[]> rows) {
        return rows.stream().map(row -> new RetestedSample((Long) row[0], (String) row[1], (Long) row[2],
                (String) row[3], (Long) row[4])).toList();
    }
}
