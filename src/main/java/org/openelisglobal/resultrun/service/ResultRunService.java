package org.openelisglobal.resultrun.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.resultrun.form.ResultRunHeader;
import org.openelisglobal.resultrun.form.ResultRunListResponse;
import org.openelisglobal.resultrun.form.ResultRunRowsPage;
import org.openelisglobal.resultrun.form.RunViewer;
import org.openelisglobal.resultrun.form.WaitingRun;
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
     * A value of the run left staging (accepted, rejected or ignored). An analyzer
     * run whose last value has left is complete (FR-E5).
     */
    void rowLeftStaging(Long runId);

    ResultRunListResponse listActiveRuns(RunViewer viewer, String testSectionId);

    Optional<ResultRunHeader> getHeader(Long runId, RunViewer viewer);

    Optional<ResultRunRowsPage> getRows(Long runId, RunViewer viewer, int page, int pageSize);

    /** The message the run was opened from, as it was accepted. */
    Optional<String> getRawMessage(Long runId, RunViewer viewer);

    /**
     * The run each lab number and test has a value waiting in, keyed by
     * {@link #waitingKey(String, String)}.
     */
    Map<String, WaitingRun> findWaiting(Collection<String> accessionNumbers);

    Optional<WaitingRun> findWaiting(String accessionNumber, String testId);

    /** Names, on each pending worklist row, the run its analyzer value waits in. */
    void markWaiting(List<TestResultItem> items);

    /**
     * The run an analysis with no stored result has a value waiting in. Such an
     * analysis is resulted from its run, not entered around it (FR-D3).
     */
    Optional<WaitingRun> waitingFor(Analysis analysis);

    static String waitingKey(String accessionNumber, String testId) {
        return accessionNumber + "|" + testId;
    }
}
