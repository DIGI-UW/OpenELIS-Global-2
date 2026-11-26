package org.openelisglobal.resultrun.dao;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.resultrun.valueholder.ResultRunRetest;

public interface ResultRunRetestDAO extends BaseDAO<ResultRunRetest, Long> {

    /**
     * A retested sample with the run and test whose Retest set it aside, and the
     * run confirmed as its retest, if any. Read as values, so a link made earlier
     * in the same transaction shows.
     */
    record RetestedSample(Long runId, String testId, Long dispositionId, String analysisId, Long retestedInRunId) {
    }

    /**
     * The not yet linked samples that Retest decisions on the test, in runs opened
     * before the given run, set aside, among the given analyses.
     */
    List<RetestedSample> findUnlinked(String testId, Collection<String> analysisIds, Long beforeRunId);

    /** Every sample a Retest on the run set aside. */
    List<RetestedSample> findSetAsideBy(Long runId);

    /** Every sample confirmed as retested in the run. */
    List<RetestedSample> findRetestedIn(Long runId);

    /**
     * Links the decision's still unlinked samples among the analyses to the retest
     * run; returns how many it linked, none when another user won.
     */
    int link(Long dispositionId, Collection<String> analysisIds, Long retestRunId, String sysUserId,
            Timestamp linkedAt);
}
