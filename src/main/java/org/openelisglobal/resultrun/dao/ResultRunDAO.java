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

    /** The run a workplan batch is, if it was released (FR-B2). */
    Optional<ResultRun> findBatchRun(Long batchWorkplanId);

    /** The analyses of a workplan run: its batch's items (FR-G4). */
    List<String> getBatchAnalysisIds(Long runId);

    /**
     * How many of a workplan run's analyses still wait for a result: not cancelled,
     * and with no result value.
     */
    long countBatchAnalysesAwaiting(Long runId, String cancelledStatusId);

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
