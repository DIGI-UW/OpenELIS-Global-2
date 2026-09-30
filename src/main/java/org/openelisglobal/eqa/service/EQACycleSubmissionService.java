package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.eqa.valueholder.EQACycle;

/**
 * Automatic submission of a participant cycle's results, and the two fallbacks
 * around it (OGC-610).
 *
 * <p>
 * This is also the only production path that bridges the standard result
 * pipeline into {@code eqa_participant_result} for an external scheme. Before
 * it, the table had exactly two writers: the participant-result REST controller
 * and the in-house blinding service — so a lab that ordered an EQA sample,
 * entered its result and validated it produced no EQA-side row at all, and its
 * cycle stayed in the state its panel receipt left it in.
 */
public interface EQACycleSubmissionService {

    /**
     * Cycles worth a look this tick: participant-side states in which results can
     * still arrive or a submission can still be owed.
     */
    List<Long> findAdvanceCandidates();

    /**
     * Bridge this cycle's validated analyses into participant results, advance the
     * participant state machine as far as the evidence allows, and submit when The
     * submission preconditions are met. Idempotent: a re-run over an unchanged
     * cycle writes nothing.
     *
     * @return true when anything changed, for the scheduler's log line
     */
    boolean advanceCycle(Long cycleId);

    /**
     * Submits a cycle a reviewer has just released, on a scheme that asks a human
     * to look at each cycle before it goes. The send runs on the same path the
     * unattended sweep uses, so the result rows carry the channel and timestamp of
     * a real submission rather than only the cycle carrying a new state.
     *
     * <p>
     * The sweep never revisits a SUBMITTED cycle, so a release that advanced the
     * state without sending left nothing behind to recover it: the screen reported
     * a submission the provider never received.
     *
     * @throws IllegalArgumentException when no such cycle exists
     * @throws IllegalStateException    when the cycle is not ready to submit, the
     *                                  laboratory has no automatic channel, no
     *                                  validated result is waiting, or the provider
     *                                  cannot be reached
     */
    EQACycle submitAfterReview(Long cycleId, String sysUserId);

    /**
     * Manual fallback: record that this lab submitted outside OpenELIS. The
     * provider's reference is mandatory — an unreferenced manual submission is
     * indistinguishable from a lab claiming it submitted.
     *
     * @throws IllegalArgumentException when the reference is missing
     */
    EQACycle submitManually(Long cycleId, Long labEnrollmentId, String reference, String sysUserId);

    /**
     * Export bundle: this lab's submittable results as CSV, for a provider portal
     * upload or an email attachment.
     */
    String exportBundleCsv(Long cycleId, Long labEnrollmentId);

    /**
     * Score intake: the provider's verdicts coming back. Each entry carries
     * {@code resultId} or {@code analyteId}, {@code performance}, and an optional
     * {@code zScore}. The cycle moves to SCORED once every submitted result carries
     * a verdict.
     *
     * @return how many results were scored
     */
    /**
     * Records who ran this analysis on its EQA participant result, creating the
     * draft row if the scheduler has not bridged it yet.
     *
     * <p>
     * The analyst is captured at result entry but the participant result is
     * normally written later, when the cycle sweep sees a finalized analysis — so
     * this writes the row early rather than parking the analyst somewhere it would
     * have to be found again. A later sweep updates the value and leaves the
     * analyst alone.
     *
     * @return true when an analyst was recorded; false when the analysis is not
     *         EQA, its scheme does not capture analysts, or the sample names no
     *         cycle
     */
    boolean assignAnalyst(Analysis analysis, Long analystId, String sysUserId);

    int intakeScores(Long cycleId, Long labEnrollmentId, List<Map<String, Object>> scores, String sysUserId);

    /**
     * Scores by file: the provider's score CSV (columns analyte_name,
     * performance_status, z_score; the rest is ignored) applied through
     * {@link #intakeScores}. Analytes are matched by name — the provider's ids are
     * its own. Answers {@code scored} and the {@code unmapped} analyte names this
     * laboratory does not know.
     */
    Map<String, Object> intakeScoresCsv(Long cycleId, Long labEnrollmentId, String csv, String sysUserId);
}
