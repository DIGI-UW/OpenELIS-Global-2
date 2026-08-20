package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.eqa.valueholder.EQACycle;

/**
 * Automatic submission of a participant cycle's results, and the two fallbacks
 * around it (OGC-610, FR-V2.2-05 / FR-V2.2-06 / FR-V2.2-08).
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
     * participant state machine as far as the evidence allows, and submit when
     * FR-V2.2-05's preconditions are met. Idempotent: a re-run over an unchanged
     * cycle writes nothing.
     *
     * @return true when anything changed, for the scheduler's log line
     */
    boolean advanceCycle(Long cycleId);

    /**
     * FR-V2.2-06 manual fallback: record that this lab submitted outside OpenELIS.
     * The provider's reference is mandatory — an unreferenced manual submission is
     * indistinguishable from a lab claiming it submitted.
     *
     * @throws IllegalArgumentException when the reference is missing
     */
    EQACycle submitManually(Long cycleId, Long labEnrollmentId, String reference, String sysUserId);

    /**
     * FR-V2.2-06 export bundle: this lab's submittable results as CSV, for a
     * provider portal upload or an email attachment.
     */
    String exportBundleCsv(Long cycleId, Long labEnrollmentId);

    /**
     * FR-V2.2-08 score intake: the provider's verdicts coming back. Each entry
     * carries {@code resultId} or {@code analyteId}, {@code performance}, and an
     * optional {@code zScore}. The cycle moves to SCORED once every submitted
     * result carries a verdict.
     *
     * @return how many results were scored
     */
    int intakeScores(Long cycleId, Long labEnrollmentId, List<Map<String, Object>> scores, String sysUserId);
}
