package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.service.EQAAnalystCompetencyService;
import org.openelisglobal.eqa.valueholder.EQAAnalystCompetencyEvent;
import org.openelisglobal.eqa.valueholder.EQACompetencyEventType;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQADismissalCategory;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.qaevent.service.EqaScoreNceService;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-611 — the Analyst Competency rollup against the real schema: which events
 * count against an analyst, which are excused, and the band each combination
 * asserts.
 *
 * <p>
 * Every case here seeds through the shipped writer
 * ({@link EQAAnalystCompetencyService#record}) rather than raw SQL, so a change
 * to what scoring writes fails these tests instead of silently drifting from
 * them.
 */
public class EQAAnalystCompetencyIntegrationTest extends EQASpineTestBase {

    private static final long ANALYTE = 9820L;
    private static final String ANALYTE_NAME = "EQA Competency Analyte";
    private static final long SECOND_ANALYTE = 9821L;
    private static final String SECOND_ANALYTE_NAME = "EQA Competency Analyte 2";
    private static final long ENROLLMENT = 9920L;

    private static final String COMPETENT = "COMPETENT";
    private static final String UNDER_REVIEW = "UNDER_REVIEW";
    private static final String NOT_COMPETENT = "NOT_COMPETENT";

    private static final String MEETS_EVIDENCE = "MEETS_EVIDENCE";
    private static final String REPEATED_FAILURE = "REPEATED_FAILURE";
    private static final String INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE";
    private static final String OPEN_ESCALATION = "OPEN_ESCALATION";

    @Autowired
    private EQAAnalystCompetencyService competencyService;

    @Autowired
    private NCEventService ncEventService;

    private EQAProgram scheme;
    private EQACycle cycle;

    /**
     * eqa_participant_result is unique on (round, lab, analyte), so an analyst's
     * four samples for one analyte are four rounds — which is what a year of
     * quarterly PT actually looks like.
     */
    private int rounds;

    @Before
    public void seedCatalogAndCycle() {
        seedAnalyte(ANALYTE, ANALYTE_NAME);
        seedAnalyte(SECOND_ANALYTE, SECOND_ANALYTE_NAME);
        seedEnrollment(ENROLLMENT, "EQA Competency Programme");
        clearNceTables();

        scheme = insertScheme("Competency Scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        cycle = readBack(insertCycle(scheme, 1));
        // The base fixture runs with transactions NOT_SUPPORTED, so a read-back
        // entity's lazy scheme has no session behind it. The writer under test
        // reads cycle.getScheme(), which in production runs inside the scoring
        // transaction; here the association is re-attached by hand.
        cycle.setScheme(scheme);
        rounds = 0;
    }

    @After
    public void cleanUpCompetencyTables() {
        jdbc.update("DELETE FROM clinlims.eqa_analyst_competency_event");
        clearNceTables();
    }

    // ---- the counted / excused split ----

    @Test
    public void fourAcceptableScoredResultsAssertCompetent() {
        for (int i = 0; i < 4; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }

        Map<String, Object> analyst = onlyAnalyst();
        assertEquals(COMPETENT, analyst.get("status"));
        assertEquals(4, analyst.get("evaluableCount"));
        assertEquals(0, analyst.get("failureCount"));
        assertEquals(4, analyst.get("sampleCount"));
        assertEquals("acceptable", analyst.get("mostRecentPerformance"));

        List<Map<String, Object>> analytes = analytes(analyst);
        assertEquals(1, analytes.size());
        assertEquals(ANALYTE_NAME, analytes.get(0).get("analyteName"));
        assertEquals(COMPETENT, analytes.get(0).get("status"));
        assertEquals(MEETS_EVIDENCE, analytes.get(0).get("reason"));
        assertEquals(4, analytes.get(0).get("evaluableCount"));
    }

    @Test
    public void threeAcceptableResultsAreInsufficientEvidence() {
        for (int i = 0; i < 3; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }

        Map<String, Object> analyst = onlyAnalyst();
        assertEquals("below the evidence floor an analyst is not competent by default", UNDER_REVIEW,
                analyst.get("status"));
        assertEquals(3, analyst.get("evaluableCount"));
        assertEquals(0, analyst.get("failureCount"));
        // The half of the pair that is not a performance finding: same band as
        // twoFailuresAmongFourSamplesAssertUnderReview, opposite rule.
        assertEquals(INSUFFICIENT_EVIDENCE, analytes(analyst).get(0).get("reason"));
        assertEquals(INSUFFICIENT_EVIDENCE, headlineReasons(analyst).get(0).get("reason"));
    }

    @Test
    public void twoFailuresAmongFourSamplesAssertUnderReview() {
        scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.UNACCEPTABLE_SCORE, ANALYTE);
        recordOn(EQAPerformanceStatus.QUESTIONABLE, EQACompetencyEventType.QUESTIONABLE_SCORE, ANALYTE);

        Map<String, Object> analyst = onlyAnalyst();
        assertEquals(UNDER_REVIEW, analyst.get("status"));
        assertEquals(4, analyst.get("evaluableCount"));
        assertEquals(2, analyst.get("failureCount"));
        // Same band as threeAcceptableResultsAreInsufficientEvidence, and the
        // reason is what tells the two apart.
        assertEquals(REPEATED_FAILURE, analytes(analyst).get(0).get("reason"));
        assertEquals(REPEATED_FAILURE, headlineReasons(analyst).get(0).get("reason"));
        assertEquals("the worst recent verdict is what the page shows", "unacceptable",
                analyst.get("mostRecentPerformance"));
    }

    @Test
    public void oneFailureAmongFourSamplesStillAssertsCompetent() {
        for (int i = 0; i < 3; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }
        recordOn(EQAPerformanceStatus.QUESTIONABLE, EQACompetencyEventType.QUESTIONABLE_SCORE, ANALYTE);

        Map<String, Object> analyst = onlyAnalyst();
        assertEquals(COMPETENT, analyst.get("status"));
        assertEquals(4, analyst.get("evaluableCount"));
        assertEquals(1, analyst.get("failureCount"));
    }

    @Test
    public void equipmentDismissalLeavesBothTotals() {
        for (int i = 0; i < 4; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }
        recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.DISMISSED_EQUIPMENT, ANALYTE);
        recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.DISMISSED_ACCEPTABLE_ON_REVIEW, ANALYTE);

        Map<String, Object> analyst = onlyAnalyst();
        assertEquals("equipment fault is not the analyst's failure", COMPETENT, analyst.get("status"));
        assertEquals("an excused sample leaves the denominator too", 4, analyst.get("evaluableCount"));
        assertEquals(0, analyst.get("failureCount"));
        assertEquals("excused samples are not assessed samples", 4, analyst.get("sampleCount"));
    }

    /**
     * The specification's own worked example, seeded the way production writes it:
     * the score event lands first and triage answers it. OR-ing the two let the
     * score event's flags survive whatever triage decided, so an equipment fault
     * was never lifted off the analyst who happened to run the sample -- the
     * excusing categories could only ever excuse a sample that had not failed.
     */
    @Test
    public void anEquipmentFaultIsLiftedOffTheAnalystWhoRanTheSample() {
        for (int i = 0; i < 2; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }
        recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.UNACCEPTABLE_SCORE, ANALYTE);
        for (int i = 0; i < 3; i++) {
            scoredThenTriaged(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.DISMISSED_EQUIPMENT, ANALYTE);
        }

        Map<String, Object> analyst = onlyAnalyst();
        // Two acceptables plus the one unanswered failure. The three equipment
        // faults leave both totals, which pulls the denominator below the
        // evidence floor -- a broken analyser means less evidence about the
        // person, not more.
        assertEquals("the excused samples leave the denominator", 3, analyst.get("evaluableCount"));
        assertEquals("and the numerator", 1, analyst.get("failureCount"));
        assertEquals(UNDER_REVIEW, analyst.get("status"));
    }

    /** Acceptable-on-review is the other excusing verdict, and behaves the same. */
    @Test
    public void acceptableOnReviewExcusesTheSampleItReviewed() {
        for (int i = 0; i < 4; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }
        scoredThenTriaged(EQAPerformanceStatus.QUESTIONABLE, EQACompetencyEventType.DISMISSED_ACCEPTABLE_ON_REVIEW,
                ANALYTE);

        Map<String, Object> analyst = onlyAnalyst();
        assertEquals(4, analyst.get("evaluableCount"));
        assertEquals("triage found nothing to answer for", 0, analyst.get("failureCount"));
        assertEquals(COMPETENT, analyst.get("status"));
    }

    /**
     * The counting categories are unaffected either way -- the point is that they
     * count once, not twice, for a sample that carries both events.
     */
    @Test
    public void aTranscriptionDismissalCountsOnceForASampleThatAlsoHasItsScore() {
        for (int i = 0; i < 3; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }
        scoredThenTriaged(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.DISMISSED_TRANSCRIPTION, ANALYTE);

        Map<String, Object> analyst = onlyAnalyst();
        assertEquals("one sample, one denominator entry", 4, analyst.get("evaluableCount"));
        assertEquals("one sample, one failure", 1, analyst.get("failureCount"));
        assertEquals(COMPETENT, analyst.get("status"));
    }

    /**
     * An escalation is not a counting decision: the score it escalates is already
     * the evaluable row, so it must not take the sample out of the denominator the
     * way a triage verdict does.
     */
    @Test
    public void anEscalationLeavesTheSampleInTheDenominator() {
        for (int i = 0; i < 3; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }
        Long failed = recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.UNACCEPTABLE_SCORE, ANALYTE);
        escalate(failed, insertNce("Closed"));

        Map<String, Object> analyst = onlyAnalyst();
        assertEquals("the escalated sample is still an assessed sample", 4, analyst.get("evaluableCount"));
        assertEquals("and fails once, not twice", 1, analyst.get("failureCount"));
    }

    @Test
    public void transcriptionAndOtherDismissalsDoCountAgainstTheAnalyst() {
        for (int i = 0; i < 2; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }
        recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.DISMISSED_TRANSCRIPTION, ANALYTE);
        recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.DISMISSED_OTHER, ANALYTE);

        Map<String, Object> analyst = onlyAnalyst();
        assertEquals("transcription is within analyst scope", UNDER_REVIEW, analyst.get("status"));
        assertEquals(4, analyst.get("evaluableCount"));
        assertEquals(2, analyst.get("failureCount"));
    }

    @Test
    public void missedDeadlineCountsAsAFailedSample() {
        for (int i = 0; i < 3; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }
        recordOn(null, EQACompetencyEventType.EXTERNAL_MISSED_DEADLINE, ANALYTE);

        Map<String, Object> analyst = onlyAnalyst();
        assertEquals(4, analyst.get("evaluableCount"));
        assertEquals("an absence of a result is still an assessable fact", 1, analyst.get("failureCount"));
        assertEquals(COMPETENT, analyst.get("status"));
    }

    // ---- escalation and the open-NCE band ----

    @Test
    public void openEscalatedNceAssertsNotCompetent() {
        for (int i = 0; i < 4; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }
        Long resultId = recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.UNACCEPTABLE_SCORE, ANALYTE);
        int nceId = insertNce("Pending");
        escalate(resultId, nceId);

        Map<String, Object> analyst = onlyAnalyst();
        assertEquals(NOT_COMPETENT, analyst.get("status"));
        assertEquals("an escalated score is one failed sample, not two", 1, analyst.get("failureCount"));
        assertEquals(5, analyst.get("evaluableCount"));
        assertEquals(Boolean.TRUE, analytes(analyst).get(0).get("openEscalation"));
        assertEquals(OPEN_ESCALATION, analytes(analyst).get(0).get("reason"));
    }

    @Test
    public void closedNceReleasesTheNotCompetentBand() {
        for (int i = 0; i < 4; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }
        Long resultId = recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.UNACCEPTABLE_SCORE, ANALYTE);
        int nceId = insertNce("Closed");
        escalate(resultId, nceId);

        Map<String, Object> analyst = onlyAnalyst();
        assertEquals("a closed non-conformity is an answered failure", COMPETENT, analyst.get("status"));
        assertEquals(1, analyst.get("failureCount"));
        assertEquals(Boolean.FALSE, analytes(analyst).get(0).get("openEscalation"));
    }

    // ---- grain ----

    @Test
    public void theAnalystBandIsTheWorstOfTheirAnalytes() {
        for (int i = 0; i < 4; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }
        recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.UNACCEPTABLE_SCORE, SECOND_ANALYTE);
        recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.UNACCEPTABLE_SCORE, SECOND_ANALYTE);

        Map<String, Object> analyst = onlyAnalyst();
        assertEquals("competence is claimed per analyte, so one bad analyte bands the analyst", UNDER_REVIEW,
                analyst.get("status"));

        List<Map<String, Object>> analytes = analytes(analyst);
        assertEquals(2, analytes.size());
        assertEquals(ANALYTE_NAME, analytes.get(0).get("analyteName"));
        assertEquals(COMPETENT, analytes.get(0).get("status"));
        assertEquals(SECOND_ANALYTE_NAME, analytes.get(1).get("analyteName"));
        assertEquals(UNDER_REVIEW, analytes.get(1).get("status"));
        assertEquals(2, analytes.get(1).get("failureCount"));

        // The headline reason names the analyte that produced it and carries that
        // analyte's own counts, not the analyst's. The competent analyte
        // contributes nothing, which is the control: a reason list that simply
        // collected every analyte would hold two entries here.
        List<Map<String, Object>> why = headlineReasons(analyst);
        assertEquals(1, why.size());
        assertEquals(REPEATED_FAILURE, why.get(0).get("reason"));
        assertEquals(SECOND_ANALYTE_NAME, why.get(0).get("analyteName"));
        assertEquals("the rule read two samples, not the analyst's six", 2, why.get(0).get("evaluableCount"));
        assertEquals(2, why.get(0).get("failureCount"));
        assertEquals("the analyst's own totals span both analytes", 6, analyst.get("evaluableCount"));
    }

    @Test
    public void theRollupPublishesTheWindowAndFloorItJudgedAgainst() {
        scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);

        Map<String, Object> page = competencyService.getCompetencyRollup();
        assertEquals(12, page.get("windowMonths"));
        assertEquals(4, page.get("evidenceFloor"));
        assertEquals(LocalDate.now().toString(), page.get("windowEnd"));
        assertEquals("the window is the twelve months the rollup actually read",
                LocalDate.now().minusMonths(12).toString(), page.get("windowStart"));
    }

    @Test
    public void resultsWithNoAssignedAnalystAreNotAssessed() {
        result(EQAPerformanceStatus.ACCEPTABLE, ANALYTE, null);

        assertEquals("no analyst, no competency claim", 0, analysts().size());
        assertEquals(0, kpis().get("analystCount"));
    }

    @Test
    public void eventsOlderThanTwelveMonthsFallOutOfTheWindow() {
        for (int i = 0; i < 4; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }
        Long resultId = recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.UNACCEPTABLE_SCORE, ANALYTE);
        Long secondId = recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.UNACCEPTABLE_SCORE, ANALYTE);
        ageEvent(resultId, LocalDate.now().minusMonths(13));
        ageEvent(secondId, LocalDate.now().minusMonths(13));

        Map<String, Object> analyst = onlyAnalyst();
        assertEquals("competency is a rolling twelve months, not a career", COMPETENT, analyst.get("status"));
        assertEquals(4, analyst.get("evaluableCount"));
        assertEquals(0, analyst.get("failureCount"));
    }

    // ---- page shape ----

    @Test
    public void kpisCountAnalystsByBand() {
        for (int i = 0; i < 4; i++) {
            scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        }

        Map<String, Object> kpis = kpis();
        assertEquals(1, kpis.get("analystCount"));
        assertEquals(1, kpis.get("competentCount"));
        assertEquals(0, kpis.get("underReviewCount"));
        assertEquals(0, kpis.get("notCompetentCount"));
        assertEquals(4, kpis.get("assessedSampleCount"));
    }

    @Test
    public void historyCarriesEveryEventBehindTheBand() {
        scoredResult(EQAPerformanceStatus.ACCEPTABLE, ANALYTE);
        recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.UNACCEPTABLE_SCORE, ANALYTE);
        recordOn(EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyEventType.DISMISSED_EQUIPMENT, ANALYTE);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) onlyAnalyst().get("history");
        assertEquals(3, history.size());
        assertTrue("the derived acceptable result is in the evidence too",
                history.stream().anyMatch(row -> row.get("eventType") == null && "acceptable".equals(row.get("outcome"))
                        && Boolean.TRUE.equals(row.get("counted"))));
        Map<String, Object> dismissal = history.stream()
                .filter(row -> "DISMISSED_EQUIPMENT".equals(row.get("eventType"))).findFirst()
                .orElseThrow(AssertionError::new);
        assertEquals("an excused event is shown but not counted", Boolean.FALSE, dismissal.get("counted"));
        assertEquals(Boolean.FALSE, dismissal.get("failure"));
        assertEquals(ANALYTE_NAME, dismissal.get("analyteName"));
        assertNull(dismissal.get("nceId"));
    }

    @Test
    public void emptyLogRendersAnEmptyPageRatherThanFailing() {
        Map<String, Object> page = competencyService.getCompetencyRollup();
        assertEquals(0, analysts().size());
        assertEquals(0, ((Map<?, ?>) page.get("kpis")).get("analystCount"));
    }

    // ---- fixtures ----

    private EQAParticipantResult result(EQAPerformanceStatus performance, long analyteId) {
        return result(performance, analyteId, ADMIN_USER_ID);
    }

    private EQAParticipantResult result(EQAPerformanceStatus performance, long analyteId, Long analystId) {
        EQARound round = eqaRoundDAO.get(insertRound(cycle, ++rounds, "OPEN")).orElseThrow(AssertionError::new);
        Long id = insertParticipantResult(cycle, round, ENROLLMENT, analyteId, EQASubmissionStatus.SCORED, "10");
        EQAParticipantResult result = eqaParticipantResultDAO.get(id).orElseThrow(AssertionError::new);
        result.setCycle(cycle);
        result.setAssignedAnalystId(analystId);
        result.setPerformanceStatus(performance);
        result.setScoreReceivedAt(new Timestamp(System.currentTimeMillis()));
        eqaParticipantResultDAO.update(result);
        return result;
    }

    /** A scored result the log does not speak for — the acceptable path. */
    private void scoredResult(EQAPerformanceStatus performance, long analyteId) {
        result(performance, analyteId);
    }

    /** A scored result plus the event scoring writes for it. */
    private Long recordOn(EQAPerformanceStatus performance, EQACompetencyEventType type, long analyteId) {
        EQAParticipantResult result = result(performance, analyteId);
        EQADismissalCategory category = switch (type) {
        case DISMISSED_EQUIPMENT -> EQADismissalCategory.KNOWN_EQUIPMENT_ISSUE;
        case DISMISSED_TRANSCRIPTION -> EQADismissalCategory.TRANSCRIPTION_ERROR;
        case DISMISSED_ACCEPTABLE_ON_REVIEW -> EQADismissalCategory.ACCEPTABLE_ON_REVIEW;
        case DISMISSED_OTHER -> EQADismissalCategory.OTHER;
        default -> null;
        };
        EQAAnalystCompetencyEvent event = competencyService.record(result, type, null, category, null, USER);
        assertTrue("the writer must produce an event for an assigned analyst", event != null);
        return result.getId();
    }

    /**
     * The shape that actually arises in production: a bad score writes its own
     * event, and triage answers that same result afterwards. Two events, one sample
     * -- which is the case no acceptance criterion seeds, because they all seed
     * bare events.
     */
    private Long scoredThenTriaged(EQAPerformanceStatus performance, EQACompetencyEventType triageType,
            long analyteId) {
        EQAParticipantResult result = result(performance, analyteId);
        EQACompetencyEventType scoreType = performance == EQAPerformanceStatus.UNACCEPTABLE
                ? EQACompetencyEventType.UNACCEPTABLE_SCORE
                : EQACompetencyEventType.QUESTIONABLE_SCORE;
        competencyService.record(result, scoreType, null, null, null, USER);
        EQADismissalCategory category = switch (triageType) {
        case DISMISSED_EQUIPMENT -> EQADismissalCategory.KNOWN_EQUIPMENT_ISSUE;
        case DISMISSED_TRANSCRIPTION -> EQADismissalCategory.TRANSCRIPTION_ERROR;
        case DISMISSED_ACCEPTABLE_ON_REVIEW -> EQADismissalCategory.ACCEPTABLE_ON_REVIEW;
        case DISMISSED_OTHER -> EQADismissalCategory.OTHER;
        default -> null;
        };
        competencyService.record(result, triageType, null, category, null, USER);
        return result.getId();
    }

    private void escalate(Long resultId, int nceId) {
        EQAParticipantResult result = eqaParticipantResultDAO.get(resultId).orElseThrow(AssertionError::new);
        result.setCycle(cycle);
        competencyService.record(result, EQACompetencyEventType.ESCALATED_TO_NCE, nceId, null, null, USER);
    }

    /**
     * Written through the register's own service, so the row is one it would
     * produce.
     */
    private int insertNce(String status) {
        NcEvent nce = new NcEvent();
        nce.setName("Competency test NCE");
        nce.setTitle("Competency test NCE");
        nce.setNceNumber("EQA-COMP-" + System.nanoTime());
        nce.setStatus(status);
        nce.setTriggerSourceType(EqaScoreNceService.TRIGGER_SOURCE_EQA_UNACCEPTABLE);
        nce.setReportDate(new java.sql.Date(System.currentTimeMillis()));
        nce.setDateOfEvent(new java.sql.Date(System.currentTimeMillis()));
        nce.setSysUserId(USER);
        return ncEventService.save(nce).getId();
    }

    private void ageEvent(Long participantResultId, LocalDate date) {
        jdbc.update("UPDATE clinlims.eqa_analyst_competency_event SET event_date = ? WHERE participant_result_id = ?",
                java.sql.Date.valueOf(date), participantResultId);
        jdbc.update("UPDATE clinlims.eqa_participant_result SET score_received_at = ? WHERE id = ?",
                Timestamp.valueOf(date.atStartOfDay()), participantResultId);
    }

    private void seedAnalyte(long id, String name) {
        jdbc.update(
                "INSERT INTO clinlims.analyte (id, name, is_active, lastupdated)"
                        + " SELECT ?, ?, 'Y', now() WHERE NOT EXISTS (SELECT 1 FROM clinlims.analyte WHERE id = ?)",
                id, name, id);
    }

    private void clearNceTables() {
        jdbc.update("DELETE FROM clinlims.nce_history WHERE nce_id IN"
                + " (SELECT id FROM clinlims.nc_event WHERE trigger_source_type LIKE 'EQA%')");
        jdbc.update("DELETE FROM clinlims.nc_event WHERE trigger_source_type LIKE 'EQA%'");
    }

    // ---- reads ----

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> analysts() {
        return (List<Map<String, Object>>) competencyService.getCompetencyRollup().get("analysts");
    }

    private Map<String, Object> onlyAnalyst() {
        List<Map<String, Object>> analysts = analysts();
        assertEquals("the seeded analyst is the only one assessed", 1, analysts.size());
        return analysts.get(0);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> analytes(Map<String, Object> analyst) {
        return (List<Map<String, Object>>) analyst.get("analytes");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> headlineReasons(Map<String, Object> analyst) {
        return (List<Map<String, Object>>) analyst.get("statusReasons");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> kpis() {
        return (Map<String, Object>) competencyService.getCompetencyRollup().get("kpis");
    }
}
