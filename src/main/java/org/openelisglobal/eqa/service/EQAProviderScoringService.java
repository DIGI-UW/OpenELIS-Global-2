package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;

/**
 * Provider-side scoring and score return for a V2 cycle.
 *
 * <p>
 * Participants' submissions live in {@code eqa_result}, at organization grain,
 * because {@code eqa_participant_result.lab_enrollment_id} references this
 * lab's own enrollments — a remote participant cannot have a row there. The
 * bridge between the two worlds is {@code eqa_distribution.cycle_id}: this
 * service finds (or opens) the cycle's distribution and then reuses the shipped
 * V1 machinery — {@link EQAStatisticsService} for z-score and classification,
 * {@link EQAFhirSubmissionService#submitResultsViaFhir} for the FHIR return —
 * rather than growing a second scoring implementation.
 */
public interface EQAProviderScoringService {

    /**
     * One row per participant organization with results in this cycle: how many
     * results, the verdict counts, and the worst Z. Empty until results are taken
     * in through the distribution endpoints.
     */
    List<Map<String, Object>> getScoreRows(Long cycleId);

    /**
     * Score every participant's results for this cycle against the peer group, then
     * walk the provider machine to scored. Each participant whose worst verdict is
     * unacceptable is entered in the provider follow-up register.
     *
     * @throws IllegalStateException when the cycle is not open for scoring, or too
     *                               few results have arrived for the statistics to
     *                               mean anything
     */
    Map<String, Object> scoreCycle(Long cycleId, String sysUserId);

    /** One participant's scores as CSV (the manual return channel). */
    String buildScoreCsv(Long cycleId, Long organizationId);

    /**
     * What one participating laboratory reported into this cycle, as scored. Shared
     * with the per-participant performance report so the PDF and the scores CSV
     * cannot disagree about which rows belong to a laboratory.
     */
    List<EQAResult> reportedResultsFor(Long cycleId, Long organizationId);

    /**
     * The sealed target each of the cycle's results was judged against, keyed by
     * {@code eqa_result} id, as a word or a number. {@code eqa_result.target_value}
     * is numeric, so a qualitative target lives only on the panel sample that
     * sealed it; the report reads it here rather than deriving the panel a second
     * time. A result is matched to its own panel sample, so two samples of one test
     * each carry their own target.
     */
    Map<Long, String> sealedTargetsByResult(Long cycleId);

    /**
     * One row per laboratory enrolled in the scheme, carrying its rolling pass rate
     * over its last four scored cycles, its most recent verdict and its open
     * follow-up count, plus the cycle history behind the rate so a row can be
     * drilled into without a second read.
     *
     * <p>
     * The provider could previously see one cycle at a time and never the trend,
     * which is the judgement this exists to support — which laboratory is drifting.
     */
    List<Map<String, Object>> getParticipantPerformance(Long schemeId);

    /**
     * The intake grid for one participant: one row per panel sample of each of the
     * scheme's tests (one row for a test the panel carries no sample of), with the
     * value already on file for each, so phoned and emailed results can be keyed on
     * the provider side.
     */
    Map<String, Object> intakeGrid(Long cycleId, Long organizationId);

    /**
     * Provider-side intake of a participant's reported values, one per intake row.
     * Numbers and qualitative words alike; the cycle's distribution is opened on
     * demand. Answers the refreshed grid.
     *
     * @throws IllegalArgumentException when a value names a test outside the
     *                                  scheme, a panel sample outside the cycle, or
     *                                  a test with several panel samples without
     *                                  saying which sample it answers
     */
    Map<String, Object> takeIn(Long cycleId, Long organizationId, List<EQAIntakeValue> reported,
            EQASubmissionMethod method, String sysUserId);

    /**
     * Import a participant's export bundle CSV (columns analyte_name and
     * result_value, and sample_code where the participant sends it; the rest is
     * ignored). A row naming a sample code is matched to that panel sample; any
     * other row is matched by analyte name to the scheme's tests, because the two
     * instances do not share ids. Rows naming an analyte this scheme does not run,
     * a sample this cycle did not send, or an analyte the panel carries several
     * samples of without a sample code are reported back, not dropped silently.
     */
    Map<String, Object> importReportedCsv(Long cycleId, Long organizationId, String csv, String sysUserId);

    /**
     * Take in values keyed by analyte <i>name</i>, or by panel sample code where
     * the sender has one (the identities another instance shares), resolving each
     * to the scheme's test and sample. The answer is the grid plus an
     * {@code unmapped} list naming analytes this scheme does not run, or runs as
     * several panel samples that a name alone cannot choose between.
     */
    Map<String, Object> takeInByAnalyteName(Long cycleId, Long organizationId,
            Map<String, String> reportedByAnalyteName, EQASubmissionMethod method, String sysUserId);

    /** One participant's scores returned over FHIR. */
    Map<String, Object> distributeScores(Long cycleId, Long organizationId);
}
