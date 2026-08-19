package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.service.EQAParticipantFollowupService;
import org.openelisglobal.eqa.service.EQAParticipantResultService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQADismissalCategory;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.qaevent.service.EqaScoreNceService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-611 (FR-V2.3-01/02) — the tiered EQA to NCE adapter against the real
 * schema: which scored results become non-conformities, which land in the
 * Follow-Up Queue, and what escalate and dismiss write.
 */
public class EQAScoreTriageIntegrationTest extends EQASpineTestBase {

    private static final long ANALYTE = 9810L;
    private static final String ANALYTE_NAME = "EQA Triage Analyte";
    private static final long SECOND_ANALYTE = 9811L;
    private static final long ENROLLMENT = 9910L;
    private static final long ANALYST = ADMIN_USER_ID;

    @Autowired
    private EQAParticipantResultService participantResultService;
    @Autowired
    private EQAParticipantFollowupService followupService;
    @Autowired
    private EqaScoreNceService eqaScoreNceService;

    @Before
    public void seedCatalogAndEnrollment() {
        // Self-ensured seeds: other fixtures truncate the shared catalog in
        // full-suite runs, so everything this class reads is re-inserted
        // idempotently.
        seedAnalyte(ANALYTE, ANALYTE_NAME);
        seedAnalyte(SECOND_ANALYTE, "EQA Triage Analyte 2");
        seedEnrollment(ENROLLMENT, "EQA Triage Programme");
        clearNceTables();
    }

    /**
     * A subclass @After runs before the base class's, so everything holding an FK
     * into nc_event or organization is cleared here rather than left to
     * {@code cleanEqaTables()}.
     */
    @After
    public void cleanUpTriageTables() {
        jdbc.update("DELETE FROM clinlims.eqa_analyst_competency_event");
        jdbc.update("DELETE FROM clinlims.eqa_participant_followup");
        clearNceTables();
        jdbc.update("DELETE FROM clinlims.organization WHERE name = 'This laboratory'");
    }

    private void seedAnalyte(long id, String name) {
        jdbc.update("INSERT INTO clinlims.analyte (id, name, is_active, lastupdated)"
                + " SELECT ?, ?, 'Y', now() WHERE NOT EXISTS" + " (SELECT 1 FROM clinlims.analyte WHERE id = ?)", id,
                name, id);
    }

    private void clearNceTables() {
        jdbc.update("DELETE FROM clinlims.nce_history WHERE nce_id IN"
                + " (SELECT id FROM clinlims.nc_event WHERE trigger_source_type LIKE 'EQA%')");
        jdbc.update("DELETE FROM clinlims.nc_event WHERE trigger_source_type LIKE 'EQA%'");
    }

    // ---- tiers ----

    @Test
    public void numericUnacceptableBeyondThreeSigmaCreatesOneNce() {
        Scored scored = score(EQASchemeType.INTERNATIONAL_PT, EQAPerformanceStatus.UNACCEPTABLE, new BigDecimal("3.5"),
                "120");

        List<Map<String, Object>> nces = eqaNces();
        assertEquals("|Z|>3 unacceptable creates exactly one NCE", 1, nces.size());
        Map<String, Object> nce = nces.get(0);
        assertEquals(EqaScoreNceService.TRIGGER_SOURCE_EQA_UNACCEPTABLE, nce.get("trigger_source_type"));
        assertEquals(String.valueOf(scored.resultId), nce.get("trigger_source_id"));
        assertEquals("CRITICAL", nce.get("severity"));
        assertEquals("Pending", nce.get("status"));
        assertEquals(Boolean.TRUE, nce.get("auto_generated"));
        assertEquals("Unacceptable EQA score: " + ANALYTE_NAME + " Z=3.5 in " + scored.schemeName + " cycle 1",
                nce.get("title"));
        assertTrue("description carries the reported value",
                String.valueOf(nce.get("description")).contains("Reported: 120"));

        assertEquals("an NCE tier never also queues", 0, followupService.getQueueRows().size());
        assertEquals("the scoring competency event is stamped with the NCE", nce.get("id"),
                competencyNceId(scored.resultId));
    }

    @Test
    public void categoricalUnacceptableWithoutZScoreAlsoCreatesNce() {
        Scored scored = score(EQASchemeType.INTERNATIONAL_PT, EQAPerformanceStatus.UNACCEPTABLE, null, "Reactive");

        List<Map<String, Object>> nces = eqaNces();
        assertEquals("an external provider's unacceptable verdict stands in for a Z-score", 1, nces.size());
        String title = String.valueOf(nces.get(0).get("title"));
        assertEquals(
                "Unacceptable EQA score: " + ANALYTE_NAME + " (reported Reactive) in " + scored.schemeName + " cycle 1",
                title);
        assertFalse("a categorical NCE carries no Z token", title.contains("Z="));
        assertEquals(0, followupService.getQueueRows().size());
    }

    @Test
    public void questionableBandQueuesAndNeverCreatesAnNce() {
        Scored scored = score(EQASchemeType.INTERNATIONAL_PT, EQAPerformanceStatus.UNACCEPTABLE, new BigDecimal("2.4"),
                "108");

        assertEquals("2<|Z|<=3 is a triage case, not a non-conformity", 0, eqaNces().size());
        List<Map<String, Object>> queue = followupService.getQueueRows();
        assertEquals(1, queue.size());
        assertEquals("External provider", queue.get(0).get("source"));
        assertEquals("NOTIFIED", queue.get(0).get("followupStatus"));
        assertEquals(scored.schemeName, queue.get(0).get("schemeName"));

        List<Map<String, Object>> rows = queueResults(queue.get(0));
        assertEquals(1, rows.size());
        assertEquals(scored.resultId.intValue(), ((Number) rows.get(0).get("participantResultId")).intValue());
        assertEquals(2.4d, ((Number) rows.get(0).get("zScore")).doubleValue(), 0.0001d);
        assertEquals("UNACCEPTABLE", rows.get(0).get("performanceStatus"));
        assertEquals("108", rows.get(0).get("reported"));
    }

    @Test
    public void questionableVerdictQueues() {
        score(EQASchemeType.INTERNATIONAL_PT, EQAPerformanceStatus.QUESTIONABLE, new BigDecimal("2.1"), "104");

        assertEquals(0, eqaNces().size());
        assertEquals(1, followupService.getQueueRows().size());
    }

    @Test
    public void inHouseUnacceptableQueuesAndNeverCreatesAnNce() {
        score(EQASchemeType.IN_HOUSE, EQAPerformanceStatus.UNACCEPTABLE, null, "Negative");

        assertEquals("in-house scoring is exploratory: triage first, never auto-NCE", 0, eqaNces().size());
        List<Map<String, Object>> queue = followupService.getQueueRows();
        assertEquals(1, queue.size());
        assertEquals("In-house", queue.get(0).get("source"));
    }

    @Test
    public void acceptableWithinTwoSigmaDoesNothing() {
        score(EQASchemeType.INTERNATIONAL_PT, EQAPerformanceStatus.ACCEPTABLE, new BigDecimal("0.5"), "100");

        assertEquals(0, eqaNces().size());
        assertEquals(0, followupService.getQueueRows().size());
        assertEquals("no competency event for a clean score", 0, competencyEventCount());
    }

    @Test
    public void acceptableVerdictOutsideTwoSigmaStillQueues() {
        score(EQASchemeType.INTERNATIONAL_PT, EQAPerformanceStatus.ACCEPTABLE, new BigDecimal("2.6"), "106");

        assertEquals(0, eqaNces().size());
        assertEquals("a Z past two sigma needs a human even when the verdict says acceptable", 1,
                followupService.getQueueRows().size());
    }

    // ---- triage actions ----

    @Test
    public void escalateCreatesAnNceClosesTheRowAndIsIdempotent() {
        Scored scored = score(EQASchemeType.INTERNATIONAL_PT, EQAPerformanceStatus.QUESTIONABLE, new BigDecimal("2.2"),
                "105");
        Long followupId = Long.valueOf(String.valueOf(followupService.getQueueRows().get(0).get("id")));

        NcEvent nce = eqaScoreNceService.escalateFollowup(followupId, USER);

        assertNotNull(nce.getId());
        assertEquals(EqaScoreNceService.TRIGGER_SOURCE_EQA_FOLLOWUP, nce.getTriggerSourceType());
        assertEquals(String.valueOf(followupId), nce.getTriggerSourceId());
        assertEquals("CRITICAL", nce.getSeverity());
        assertEquals(Boolean.FALSE, nce.getAutoGenerated());
        assertTrue("the escalated NCE names the analyte", nce.getTitle().contains(ANALYTE_NAME));

        assertEquals("ESCALATED",
                jdbc.queryForObject("SELECT followup_status FROM clinlims.eqa_participant_followup WHERE id = ?",
                        String.class, followupId));
        assertEquals("escalation leaves the queue", 0, followupService.getQueueRows().size());
        assertEquals(1,
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.eqa_analyst_competency_event"
                                + " WHERE participant_result_id = ? AND event_type = 'ESCALATED_TO_NCE' AND nce_id = ?",
                        Integer.class, scored.resultId, nce.getId()).intValue());

        NcEvent again = eqaScoreNceService.escalateFollowup(followupId, USER);
        assertEquals("a repeated escalation returns the same NCE", nce.getId(), again.getId());
        assertEquals("and writes no second competency event", 1,
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.eqa_analyst_competency_event"
                                + " WHERE participant_result_id = ? AND event_type = 'ESCALATED_TO_NCE'",
                        Integer.class, scored.resultId).intValue());
    }

    @Test
    public void dismissMapsEveryCategoryToItsCompetencyEvent() {
        Map<EQADismissalCategory, String> expected = Map.of(EQADismissalCategory.KNOWN_EQUIPMENT_ISSUE,
                "DISMISSED_EQUIPMENT", EQADismissalCategory.PENDING_RE_TEST, "DISMISSED_EQUIPMENT",
                EQADismissalCategory.TRANSCRIPTION_ERROR, "DISMISSED_TRANSCRIPTION",
                EQADismissalCategory.ACCEPTABLE_ON_REVIEW, "DISMISSED_ACCEPTABLE_ON_REVIEW", EQADismissalCategory.OTHER,
                "DISMISSED_OTHER");

        int cycleNumber = 1;
        for (Map.Entry<EQADismissalCategory, String> entry : expected.entrySet()) {
            Scored scored = score(EQASchemeType.INTERNATIONAL_PT, EQAPerformanceStatus.QUESTIONABLE,
                    new BigDecimal("2.3"), "103", cycleNumber++);
            Long followupId = openFollowupFor(scored.cycleId);

            followupService.dismiss(followupId, entry.getKey(), "triaged by " + entry.getKey(), USER);

            assertEquals("RESOLVED",
                    jdbc.queryForObject("SELECT followup_status FROM clinlims.eqa_participant_followup WHERE id = ?",
                            String.class, followupId));
            assertEquals(entry.getKey() + " maps to " + entry.getValue(), entry.getValue(),
                    jdbc.queryForObject(
                            "SELECT event_type FROM clinlims.eqa_analyst_competency_event"
                                    + " WHERE participant_result_id = ? AND dismissal_category = ?",
                            String.class, scored.resultId, entry.getKey().name()));
        }
        assertEquals("dismissal never creates a non-conformity", 0, eqaNces().size());
        assertEquals("every dismissed row leaves the queue", 0, followupService.getQueueRows().size());
    }

    @Test
    public void dismissRefusesAnAlreadyEscalatedRow() {
        score(EQASchemeType.INTERNATIONAL_PT, EQAPerformanceStatus.QUESTIONABLE, new BigDecimal("2.2"), "105");
        Long followupId = Long.valueOf(String.valueOf(followupService.getQueueRows().get(0).get("id")));
        eqaScoreNceService.escalateFollowup(followupId, USER);

        try {
            followupService.dismiss(followupId, EQADismissalCategory.OTHER, "too late", USER);
            org.junit.Assert.fail("expected an escalated row to refuse dismissal");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("ESCALATED"));
        }
    }

    @Test
    public void severalFailingAnalytesShareOneQueueRowForTheCycle() {
        Scored first = score(EQASchemeType.INTERNATIONAL_PT, EQAPerformanceStatus.QUESTIONABLE, new BigDecimal("2.2"),
                "105");
        EQACycle cycle = readBack(first.cycleId);
        EQARound round = eqaRoundDAO.get(first.roundId).orElseThrow(AssertionError::new);
        Long secondResultId = insertParticipantResult(cycle, round, ENROLLMENT, SECOND_ANALYTE,
                EQASubmissionStatus.SUBMITTED, "107");
        participantResultService.recordScore(secondResultId, EQAPerformanceStatus.QUESTIONABLE, new BigDecimal("2.7"),
                null, USER);

        List<Map<String, Object>> queue = followupService.getQueueRows();
        assertEquals("the register is unique on cycle and organization", 1, queue.size());
        List<Map<String, Object>> rows = queueResults(queue.get(0));
        assertEquals("both failures survive the merge", 2, rows.size());
        assertEquals(first.resultId.intValue(), ((Number) rows.get(0).get("participantResultId")).intValue());
        assertEquals(secondResultId.intValue(), ((Number) rows.get(1).get("participantResultId")).intValue());
    }

    @Test
    public void aResultWithNoAnalystStillReachesTheQueue() {
        EQAProgram scheme = insertScheme("Unstaffed Scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        EQARound round = eqaRoundDAO.get(insertRound(cycle, 1, "OPEN")).orElseThrow(AssertionError::new);
        Long resultId = insertParticipantResult(cycle, round, ENROLLMENT, ANALYTE, EQASubmissionStatus.SUBMITTED,
                "109");

        participantResultService.recordScore(resultId, EQAPerformanceStatus.QUESTIONABLE, new BigDecimal("2.5"), null,
                USER);

        assertEquals("triage does not depend on analyst attribution", 1, followupService.getQueueRows().size());
        assertEquals("but competency stays an analyst log", 0, competencyEventCount());
    }

    // ---- helpers ----

    private record Scored(Long resultId, Long cycleId, Long roundId, String schemeName) {
    }

    private Scored score(EQASchemeType type, EQAPerformanceStatus performance, BigDecimal zScore, String value) {
        return score(type, performance, zScore, value, 1);
    }

    private Scored score(EQASchemeType type, EQAPerformanceStatus performance, BigDecimal zScore, String value,
            int cycleNumber) {
        String schemeName = "Triage Scheme " + type + " " + cycleNumber;
        EQAProgram scheme = insertScheme(schemeName, type, type == EQASchemeType.IN_HOUSE ? null : "NHLS");
        Long cycleId = insertCycle(scheme, cycleNumber);
        EQACycle cycle = readBack(cycleId);
        Long roundId = insertRound(cycle, 1, "OPEN");
        EQARound round = eqaRoundDAO.get(roundId).orElseThrow(AssertionError::new);

        Long resultId = insertParticipantResult(cycle, round, ENROLLMENT, ANALYTE, EQASubmissionStatus.SUBMITTED,
                value);
        EQAParticipantResult result = eqaParticipantResultDAO.get(resultId).orElseThrow(AssertionError::new);
        result.setAssignedAnalystId(ANALYST);
        result.setSysUserId(USER);
        eqaParticipantResultDAO.update(result);

        participantResultService.recordScore(resultId, performance, zScore, null, USER);
        return new Scored(resultId, cycleId, roundId, schemeName);
    }

    private Long openFollowupFor(Long cycleId) {
        for (Map<String, Object> row : followupService.getQueueRows()) {
            if (String.valueOf(cycleId).equals(String.valueOf(row.get("cycleId")))) {
                return Long.valueOf(String.valueOf(row.get("id")));
            }
        }
        throw new AssertionError("no open follow-up row for cycle " + cycleId);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queueResults(Map<String, Object> queueRow) {
        return (List<Map<String, Object>>) queueRow.get("results");
    }

    private List<Map<String, Object>> eqaNces() {
        return jdbc.queryForList("SELECT id, title, description, severity, status, auto_generated,"
                + " trigger_source_type, trigger_source_id FROM clinlims.nc_event"
                + " WHERE trigger_source_type LIKE 'EQA%' ORDER BY id");
    }

    private Object competencyNceId(Long resultId) {
        return jdbc.queryForObject("SELECT nce_id FROM clinlims.eqa_analyst_competency_event"
                + " WHERE participant_result_id = ? AND event_type IN ('UNACCEPTABLE_SCORE', 'QUESTIONABLE_SCORE')",
                Integer.class, resultId);
    }

    private int competencyEventCount() {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_analyst_competency_event", Integer.class);
    }
}
