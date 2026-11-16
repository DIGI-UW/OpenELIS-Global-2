package org.openelisglobal.resultrun.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.resultrun.form.RunTestVerdict;

/**
 * OGC-1200 slice 4: the per-test QC gate of a run. A control recorded on a run
 * gives each test it covers a verdict; a failed test holds that test's rows in
 * the run, and only those (FR-C4).
 */
public interface RunQcService {

    /**
     * The run's verdict for every test it covers: tests with values still staged,
     * tests of the results already accepted from it, and tests a control was
     * recorded for.
     */
    Map<String, RunTestVerdict> testVerdicts(Long runId);

    /** The tests whose rows the run holds. */
    Set<String> heldTestIds(Long runId);

    /**
     * Of these analyses, those whose result was stored from a run that now holds
     * the analysis's test (FR-C4: not released from any surface).
     */
    default Set<String> heldAnalysisIds(Collection<String> analysisIds) {
        return holdingRuns(analysisIds).keySet();
    }

    /** As above, each held analysis with the run that holds it. */
    Map<String, Long> holdingRuns(Collection<String> analysisIds);

    /**
     * Names the run on a control the analyzer reported in that run's message and
     * gives it its verdict: the qualitative outcome, or the value against the QC
     * target's expected value and uncertainty (FR-C2). A control with no target has
     * no verdict and fails nothing.
     */
    void attachAnalyzerControl(QCResult control, Long runId, String componentId);

    /**
     * Names the run on a control the technician recorded on it, with the
     * technician's own verdict (FR-E2 Record control).
     */
    void attachRecordedControl(QCResult control, Long runId, String sysUserId);

    /** The run's controls, oldest first. */
    java.util.List<QCResult> controlsOf(Long runId);

    /**
     * Sets the run Held while a test it still has rows of is held, and Open again
     * when none is, and writes testHeld and testReleased to the audit trail (FR-A3,
     * FR-A6).
     */
    void refreshState(Long runId, String sysUserId);

    /** The run header's QC block (FR-E2). */
    org.openelisglobal.resultrun.form.RunQcBlock qcBlock(Long runId);

    /**
     * Records a control the technician ran for this run (FR-E2 Record control,
     * FR-C7: a manual control may be recorded against an analyzer run), with the
     * technician's own verdict.
     */
    QCResult recordControl(Long runId, org.openelisglobal.qc.form.BenchQCCaptureForm capture, int sysUserId);
}
