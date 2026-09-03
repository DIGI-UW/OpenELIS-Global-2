package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;

/**
 * Provider-side scoring and score return for a V2 cycle (T-26, FR-V2.5-03 /
 * FR-V2.5-04).
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
     * unacceptable is entered in the provider follow-up register (FR-V2.5-05).
     *
     * @throws IllegalStateException when the cycle is not open for scoring, or too
     *                               few results have arrived for the statistics to
     *                               mean anything
     */
    Map<String, Object> scoreCycle(Long cycleId, String sysUserId);

    /** One participant's scores as CSV (FR-V2.5-04 manual return channel). */
    String buildScoreCsv(Long cycleId, Long organizationId);

    /**
     * The intake grid for one participant: the scheme's tests with the value
     * already on file for each, so phoned and emailed results can be keyed on the
     * provider side.
     */
    Map<String, Object> intakeGrid(Long cycleId, Long organizationId);

    /**
     * Provider-side intake of a participant's reported values, keyed by test id.
     * Numbers and qualitative words alike; the cycle's distribution is opened on
     * demand. Answers the refreshed grid.
     */
    Map<String, Object> takeIn(Long cycleId, Long organizationId, Map<Long, String> reportedByTest,
            EQASubmissionMethod method, String sysUserId);

    /**
     * Import a participant's export bundle CSV (columns analyte_name and
     * result_value; the rest is ignored). Analytes are matched by name to the
     * scheme's tests, because the two instances do not share ids. Rows naming an
     * analyte this scheme does not run are reported back, not dropped silently.
     */
    Map<String, Object> importReportedCsv(Long cycleId, Long organizationId, String csv, String sysUserId);

    /**
     * Take in values keyed by analyte <i>name</i> — the identity another instance
     * shares — resolving each to the scheme's test. The answer is the grid plus an
     * {@code unmapped} list naming analytes this scheme does not run.
     */
    Map<String, Object> takeInByAnalyteName(Long cycleId, Long organizationId,
            Map<String, String> reportedByAnalyteName, EQASubmissionMethod method, String sysUserId);

    /** One participant's scores returned over FHIR (FR-V2.5-04). */
    Map<String, Object> distributeScores(Long cycleId, Long organizationId);
}
