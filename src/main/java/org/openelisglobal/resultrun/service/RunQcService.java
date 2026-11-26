package org.openelisglobal.resultrun.service;

import java.util.Collection;
import java.util.List;
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

    /**
     * For each test whose verdict is No control, the kinds it requires per run that
     * the run lacks: CONTROL, BLANK, DUPLICATE (FR-H1).
     */
    Map<String, List<String>> missingQc(Long runId);

    /** The tests whose rows the run holds. */
    Set<String> heldTestIds(Long runId);

    /**
     * Why each test the run holds now is held: a failed control, or a required
     * control missing under a Hold policy (FR-A3, FR-D4); a decided test is not
     * held.
     */
    Map<String, org.openelisglobal.resultrun.form.RunHoldCause> holdCauses(Long runId);

    /**
     * Re-evaluates the open and held runs that cover the test, after its QC policy
     * changed (FR-H1): their holds and state follow the new policy at once.
     */
    void refreshActiveRunsOfTest(String testId, String sysUserId);

    /** Whether any test of the run was decided (FR-C5), closing its QC there. */
    boolean hasDecisions(Long runId);

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
     * Of these analyses, those whose stored result came from a run, each with what
     * that run says about the QC of the analysis's test (FR-G3). An analysis with
     * results from several runs reads the latest run.
     */
    Map<String, org.openelisglobal.resultrun.form.RunQcFact> qcOfAnalyses(Collection<String> analysisIds);

    /**
     * Of these analyses, those whose stored result came from a run that holds the
     * test undecided, each with that run: they take no new value until the test is
     * decided (FR-C4, FR-D4). A decided test's stored result is open to a new
     * value.
     */
    Map<String, Long> entryHoldingRuns(Collection<String> analysisIds);

    /**
     * A value entered on a result that came from a run whose test was decided
     * Retest or Reject is no longer that run's: the result stops naming the run and
     * the analyzer's reading, so the run no longer keeps it from release (FR-C5).
     */
    void detachFromDecidedRuns(Collection<org.openelisglobal.result.valueholder.Result> results);

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

    /**
     * FR-C10, FR-C11: records a lab blank, or a lab duplicate of a sample in the
     * run, for one of the run's tests, evaluates it and refreshes the run's holds.
     * Refused for a test not in the run or already decided, without a value, or for
     * a duplicate whose sample is not one of the run's.
     */
    org.openelisglobal.resultrun.valueholder.ResultRunQcItem recordQcItem(Long runId,
            org.openelisglobal.resultrun.form.RunQcItemForm form, String sysUserId);

    /**
     * The run's duplicates are evaluated again against their samples' results as
     * they stand: a duplicate recorded before its sample's result is evaluated once
     * that result is saved, and one whose sample's result was corrected follows the
     * correction (FR-C11).
     */
    void reevaluateDuplicates(Long runId, String sysUserId);

    /**
     * Results saved on these analyses, wherever they were saved (the run, Results
     * Entry, Validation): their duplicates on any run are evaluated again (FR-C11).
     */
    void parentResultsSaved(Collection<String> analysisIds, String sysUserId);

    /**
     * The run's samples of a test a duplicate may repeat: {analysisId, accession
     * number}.
     */
    java.util.List<org.openelisglobal.common.util.IdValuePair> duplicateCandidates(Long runId, String testId);

    /**
     * Decides a held test of a run (FR-C5): Retest and Reject take its values out
     * of review (Reject may file a non-conformity); Accept despite the QC failure
     * needs a reason and files a non-conformity linked to the test's analyses, and
     * lets its values be accepted and its stored results released. A test is
     * decided once.
     */
    org.openelisglobal.resultrun.valueholder.ResultRunTestDisposition decide(Long runId, String testId,
            org.openelisglobal.resultrun.valueholder.RunTestAction action, String reason, boolean fileNce,
            String sysUserId);

    /**
     * Confirms this run as the retest of an earlier run's test (FR-C5): the earlier
     * run's samples it set aside for that test and this run covers are linked, the
     * earlier test reads superseded, and the earlier run is superseded once every
     * test it covered was retested and linked (FR-A3). Nothing is accepted or
     * released by the link. Refused when nothing is left to link.
     */
    void confirmRetest(Long runId, String testId, Long retestOfRunId, String sysUserId);

    /**
     * Supersedes the run when nothing is left in it to review and every test it
     * covers was retested and all its retested samples linked (FR-A3). Called on a
     * confirmed link, and when a run's last row leaves review after its retests
     * were confirmed.
     */
    void supersedeIfRetested(Long runId, String sysUserId);
}
