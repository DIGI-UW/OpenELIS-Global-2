package org.openelisglobal.resultrun.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.resultrun.valueholder.ResultRun;

public interface ResultRunDAO extends BaseDAO<ResultRun, Long> {

    /** The run one accepted analyzer message created. */
    Optional<ResultRun> findAnalyzerRun(String connectionId, String messageId);

    /**
     * The run a workplan batch is, if it was released (FR-B2): its latest, since a
     * re-release after a QC decision opens a fresh one.
     */
    Optional<ResultRun> findBatchRun(Long batchWorkplanId);

    /** The analyses of a workplan run: its batch's items (FR-G4). */
    List<String> getBatchAnalysisIds(Long runId);

    /**
     * How many of a workplan run's analyses still wait for a result: not cancelled,
     * and with no result value.
     */
    long countBatchAnalysesAwaiting(Long runId, String cancelledStatusId);

    /**
     * The analyses a workplan run waits for: its batch's analyses, of the test when
     * one is given, that are not cancelled and have no recorded value, in the
     * batch's order. Only the batch's current open run waits for any; an earlier or
     * closed run has none.
     */
    List<String> getBatchAnalysisIdsAwaiting(Long runId, String cancelledStatusId, String testId);

    /**
     * The open and held runs of one of the analyses (FR-B3), as analysis id, run
     * id, test id.
     */
    List<Object[]> getActiveRunsOfOne(Collection<String> analysisIds);

    /**
     * The open or held run of one a control recorded on the analysis's row opened.
     */
    Optional<ResultRun> findOpenRunOfOne(String analysisId);

    /**
     * Per analysis, {analysisId, runId, testId, state} of its latest run of one
     * that later controls and values still belong to: not superseded, and not set
     * aside by a Retest or Reject decision.
     */
    List<Object[]> getReusableRunsOfOne(Collection<String> analysisIds);

    /** The analysis's run of one that later controls and values belong to. */
    Optional<ResultRun> findReusableRunOfOne(String analysisId);

    /**
     * Serialises the work that opens or completes an analysis's run of one, until
     * the transaction ends, so two requests at once cannot open two runs.
     */
    void lockRunOfOne(String analysisId);

    /** The run each of the results was recorded in, by result id. */
    java.util.Map<String, Long> getRunIdsOfResults(Collection<String> resultIds);

    /** How many analyses have a value recorded in the run. */
    int countResultedAnalyses(Long runId);

    /**
     * Of the given reagent lots, the one most recently recorded on a run other than
     * this one (FR-E3 "Use last-used lot").
     */
    Optional<Long> findLastUsedLot(java.util.Collection<Long> lotIds, Long excludeRunId);

    /** The tests of a workplan run's batched analyses. */
    List<String> getBatchTestIds(Long runId);

    /**
     * For each analysis batched in an open or held workplan run: analysis id, run
     * id, test id (FR-D5).
     */
    List<Object[]> getActiveBatchRunsOfAnalyses(Collection<String> analysisIds);

    /** Runs with rows still to work (Open or Held), newest first. */
    List<ResultRun> getActiveRuns();

    /** The values of the given runs still waiting in staging, in arrival order. */
    List<AnalyzerResults> getStagedRows(Collection<Long> runIds);

    long countStagedRows(Long runId);

    /**
     * Patient values waiting in an active run for any of these lab numbers: the
     * rows a worklist must not let anyone result around.
     */
    List<AnalyzerResults> getWaitingRowsForAccessions(Collection<String> accessionNumbers);

    /**
     * For each of these analyses that has a result stored from a run: the analysis
     * id, the run id and the analysis's test id.
     */
    List<Object[]> getRunsOfAnalyses(Collection<String> analysisIds);

    /** The tests of the results stored from a run. */
    List<String> getTestIdsOfResults(Long runId);

    /**
     * The analyses of one test in a run: those with a result stored from it and
     * those its staged values were reported for.
     */
    List<String> getAnalysisIdsOfRunTest(Long runId, String testId);
}
