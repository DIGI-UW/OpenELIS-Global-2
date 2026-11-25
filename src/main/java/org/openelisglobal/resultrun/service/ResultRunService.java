package org.openelisglobal.resultrun.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.resultrun.form.ResultRunChip;
import org.openelisglobal.resultrun.form.ResultRunHeader;
import org.openelisglobal.resultrun.form.ResultRunListResponse;
import org.openelisglobal.resultrun.form.ResultRunRowsPage;
import org.openelisglobal.resultrun.form.ResultRunSelection;
import org.openelisglobal.resultrun.form.ResultRunSelectionSummary;
import org.openelisglobal.resultrun.form.RunViewer;
import org.openelisglobal.resultrun.form.WaitingRun;
import org.openelisglobal.resultrun.service.ResultRunRowClassifier.ClassifiedRow;
import org.openelisglobal.resultrun.valueholder.ResultRun;
import org.openelisglobal.test.beanItems.TestResultItem;

/** OGC-1200: runs, the unit results are produced and reviewed in. */
public interface ResultRunService extends BaseObjectService<ResultRun, Long> {

    /**
     * Opens the run of one accepted analyzer message and names it on each of its
     * staged values, before they are stored. Values captured while the analyzer is
     * being set up are mapping work, not results, and open no run.
     */
    Optional<ResultRun> openAnalyzerRun(Analyzer analyzer, String connectionId, String messageId,
            List<AnalyzerResults> stagedRows);

    /** Keeps the accepted message's receipt on its run, for View raw. */
    void attachReceipt(Long runId, String receiptId);

    /** Values a run gains after it opened (a held record recovered as several). */
    void addRows(Long runId, int count);

    /**
     * A value of the run left staging (accepted, rejected or ignored) by the given
     * user. An analyzer run whose last value has left is complete (FR-E5), or
     * superseded when every test it covers was retested and linked (FR-A3).
     */
    void rowLeftStaging(Long runId, String sysUserId);

    ResultRunListResponse listActiveRuns(RunViewer viewer, String testSectionId);

    Optional<ResultRunHeader> getHeader(Long runId, RunViewer viewer);

    default Optional<ResultRunRowsPage> getRows(Long runId, RunViewer viewer, int page, int pageSize) {
        return getRows(runId, viewer, ResultRunChip.ALL, page, pageSize);
    }

    /**
     * One page of the run's rows the chip matches, in review order: exceptions and
     * held rows first, then critical, abnormal and normal values (FR-E1a, FR-E12).
     */
    Optional<ResultRunRowsPage> getRows(Long runId, RunViewer viewer, ResultRunChip chip, int page, int pageSize);

    /**
     * What a bulk action on the selection would cover, from the run, the chip and
     * the excluded rows, never from a list of every row (FR-E11, FR-E11a, FR-E12).
     */
    Optional<ResultRunSelectionSummary> summarizeSelection(Long runId, RunViewer viewer, ResultRunSelection selection);

    /**
     * Re-evaluates the run's QC holds and state once its values and controls are
     * in: a message with no control for a test that requires one under a Hold
     * policy holds that test at once (FR-C7).
     */
    void refreshQcState(Long runId, String sysUserId);

    /**
     * The run a released workplan batch is (FR-B2, FR-G4): created on its first
     * release, reopened and brought up to date when it is released again, and
     * opened afresh when a QC decision was taken on the earlier one, whose tests'
     * QC is closed. It carries the batch's lab unit, instrument, reagent lot and
     * operator; its rows are the analyses resulted in it and those it waits for,
     * and its holds follow the tests' QC policies at once.
     */
    ResultRun openWorkplanRun(Long batchWorkplanId, String testSectionId, String instrumentId, Long reagentLotId,
            String operatorName, String sysUserId);

    /** Completes the run of a batch that is marked complete (FR-B2). */
    void completeWorkplanRun(Long batchWorkplanId, String sysUserId);

    /**
     * A batch taken back to setup after a QC decision on its run: the run is done,
     * so its decisions stay with the results recorded in it, and the re-release
     * opens a fresh run whose controls cover the batch again.
     */
    void closeDecidedWorkplanRun(Long batchWorkplanId, String sysUserId);

    /** The run a workplan batch is, once released: its latest. */
    Optional<ResultRun> batchRun(Long batchWorkplanId);

    /**
     * For analyses batched in an open or held workplan run: analysis id to run id,
     * so a result entered for one records into its run (FR-D5).
     */
    Map<String, Long> activeBatchRunsOf(Collection<String> analysisIds);

    /**
     * After a result was saved into a workplan run: the run completes once every
     * analysis of its batch has a result or was cancelled (FR-B2).
     */
    void workplanResultSaved(Long runId, String sysUserId);

    /**
     * Records results of batched analyses into their open workplan run (FR-D5), and
     * returns the update that completes a run whose batch has no analysis left
     * waiting, run in the same save.
     */
    org.openelisglobal.common.services.registration.interfaces.IResultUpdate recordIntoWorkplanRuns(
            Collection<org.openelisglobal.result.valueholder.Result> results, String sysUserId);

    /** Whether the run exists and the viewer may see it. */
    boolean canSee(Long runId, RunViewer viewer);

    /**
     * The run's waiting values the viewer may see, each with its flag, category and
     * exception kind; empty when the viewer may not see the run.
     */
    Optional<List<ClassifiedRow>> classifiedRows(Long runId, RunViewer viewer);

    /** The message the run was opened from, as it was accepted. */
    Optional<String> getRawMessage(Long runId, RunViewer viewer);

    /**
     * The run each lab number and test has a value waiting in, keyed by
     * {@link #waitingKey(String, String)}.
     */
    Map<String, WaitingRun> findWaiting(Collection<String> accessionNumbers);

    Optional<WaitingRun> findWaiting(String accessionNumber, String testId);

    /**
     * Names, on each pending worklist row, the run its analyzer value waits in: a
     * value of that row's part, the main value counting as the primary part's.
     */
    void markWaiting(List<TestResultItem> items);

    /**
     * The run a value of this analysis's component waits in while that component
     * has no stored result. Such a component is resulted from its run, not entered
     * around it (FR-D3). A missing component is the test's primary one.
     */
    Optional<WaitingRun> waitingFor(Analysis analysis, String componentId);

    /**
     * The staged value a run row holds, when that row is still in an open or held
     * run (a held run holds only the tests a failed control covers) and names this
     * analysis's order, test and component, and is not a control, a duplicate or an
     * import exception. Accepting it is a Results Entry save of this analysis
     * (FR-E10, FR-E13).
     */
    Optional<AnalyzerResults> stagedValueFor(String runRowId, Analysis analysis, String componentId);

    /** The run that holds a row, as the worklist names it (FR-D4). */
    WaitingRun heldRun(Long runId, String testId);

    static String waitingKey(String accessionNumber, String testId) {
        return accessionNumber + "|" + testId;
    }
}
