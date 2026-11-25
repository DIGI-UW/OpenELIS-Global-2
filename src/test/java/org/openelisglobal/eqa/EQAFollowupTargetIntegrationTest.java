package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.service.EQAFollowupTargetBackfill;
import org.openelisglobal.eqa.service.EQAParticipantFollowupService;
import org.openelisglobal.eqa.service.EQAPerformanceReportPDFService;
import org.openelisglobal.eqa.service.EQAProviderScoringService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.testsupport.PdfText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The provider's follow-up register prints, for each unacceptable result, the
 * target the result was judged against: the same value the participant's
 * Performance Report prints. A qualitative target lives only on the sealed
 * panel sample, so a register that copied the result's numeric target showed a
 * dash for every qualitative failure (OGC-1665). Follow-ups scored before the
 * fix get the missing target once, without touching any other row and without
 * writing to the participant again.
 */
public class EQAFollowupTargetIntegrationTest extends EQASpineTestBase {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long FIRST_ORG = 99610L;
    private static final int ORGS = 5;
    private static final long FAILING = FIRST_ORG + ORGS - 1;
    private static final long TEST_SERO = 99620L;
    private static final long TEST_VL = 99621L;
    private static final long ANALYTE_SERO = 99622L;
    private static final long ANALYTE_VL = 99623L;
    private static final int SERO_LINK = 99624;
    private static final int VL_LINK = 99625;

    @Autowired
    private EQAProviderScoringService scoringService;
    @Autowired
    private EQAParticipantFollowupService followupService;
    @Autowired
    private EQAPerformanceReportPDFService reportService;
    @Autowired
    private EQAFollowupTargetBackfill backfill;
    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ReferenceTablesService referenceTablesService;

    private EQACycle cycle;

    @Before
    public void seed() {
        for (long id = FIRST_ORG; id < FIRST_ORG + ORGS; id++) {
            jdbc.update("INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                    + " VALUES (?, ?, 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING", id, "Target lab " + id);
        }
        analyte(ANALYTE_SERO, "Target HIV serology");
        analyte(ANALYTE_VL, "Target HIV VL");
        test(TEST_SERO, "Target HIV serology test", ANALYTE_SERO, SERO_LINK);
        test(TEST_VL, "Target HIV VL test", ANALYTE_VL, VL_LINK);

        EQAProgram scheme = insertScheme("Target scheme " + System.nanoTime(), EQASchemeType.REGIONAL_PT, "CPHL");
        eqaProgramService.assignTest(scheme.getId(), TEST_SERO);
        eqaProgramService.assignTest(scheme.getId(), TEST_VL);
        cycle = readBack(insertCycle(scheme, 1));
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'SUBMISSIONS_OPEN' WHERE id = ?", cycle.getId());
        EQAPanel panel = insertPanel(scheme, p -> {
            p.setCycle(cycle);
            p.setPanelName("Target panel");
        });
        seal(panel, "HIV-C-2", ANALYTE_SERO, "Reactive", null, null);
        seal(panel, "VL-1", ANALYTE_VL, "40", new BigDecimal("36"), new BigDecimal("44"));

        AuditTrailServiceImpl realAuditTrail = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(realAuditTrail, "referenceTablesService", referenceTablesService);
        ReflectionTestUtils.setField(realAuditTrail, "historyService", historyService);
        Object target = AopTestUtils.getUltimateTargetObject(backfill);
        ReflectionTestUtils.setField(target, "auditTrailService", realAuditTrail);
    }

    @Override
    protected void cleanEqaTables() {
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.eqa_result");
            jdbc.update("DELETE FROM clinlims.eqa_distribution WHERE cycle_id IS NOT NULL");
        }
        super.cleanEqaTables();
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.test_analyte WHERE id IN (?, ?)", SERO_LINK, VL_LINK);
            jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?)", TEST_SERO, TEST_VL);
            jdbc.update("DELETE FROM clinlims.analyte WHERE id IN (?, ?)", ANALYTE_SERO, ANALYTE_VL);
            jdbc.update("DELETE FROM clinlims.organization WHERE id BETWEEN ? AND ?", FIRST_ORG, FIRST_ORG + ORGS);
        }
    }

    @Test
    public void aQualitativeFailureShowsTheTargetThePerformanceReportPrints() throws IOException {
        reportSerology();

        scoringService.scoreCycle(cycle.getId(), USER);

        List<Map<String, Object>> results = registerResults(FAILING);
        assertEquals("one unacceptable result on the register", 1, results.size());
        assertEquals("Non-reactive", results.get(0).get("reported"));
        assertEquals("the sealed target, where the register printed a dash", "Reactive", results.get(0).get("target"));
        assertTrue("the Performance Report prints the same target", PdfText
                .of(reportService.generateParticipantPerformanceReport(cycle.getId(), FAILING)).contains("Reactive"));
    }

    /**
     * A participating laboratory imports this file and keeps target_value as the
     * target its own follow-up shows, so a blank cell left that follow-up without a
     * target too.
     */
    @Test
    public void theScoresCsvCarriesAQualitativeTarget() {
        reportSerology();
        scoringService.scoreCycle(cycle.getId(), USER);

        String[] lines = scoringService.buildScoreCsv(cycle.getId(), FAILING).split("\n");

        assertEquals("the header and the one result", 2, lines.length);
        List<String> cells = List.of(lines[1].split(",", -1));
        assertEquals("reported, target and verdict", List.of("Non-reactive", "Reactive", "UNACCEPTABLE"),
                List.of(cells.get(2), cells.get(3), cells.get(5)));
    }

    @Test
    public void aNumericFailureShowsTheNumberItWasJudgedAgainst() {
        String[] reported = { "39.5", "40", "40.5", "41", "80" };
        for (int i = 0; i < ORGS; i++) {
            scoringService.takeIn(cycle.getId(), FIRST_ORG + i, byTest(Map.of(TEST_VL, reported[i])),
                    EQASubmissionMethod.MANUAL, USER);
        }

        scoringService.scoreCycle(cycle.getId(), USER);

        List<Map<String, Object>> results = registerResults(FAILING);
        assertEquals(1, results.size());
        assertEquals("the number as the Performance Report prints it", "40", results.get(0).get("target"));
        Long followupId = jdbc.queryForObject("SELECT id FROM clinlims.eqa_participant_followup WHERE cycle_id = ?",
                Long.class, cycle.getId());
        String message = String.valueOf(followupService.notifyParticipant(followupId, USER).get("message"));
        assertTrue("the message to the laboratory prints both numbers the same way: " + message,
                message.contains(": reported 80, target 40"));
    }

    @Test
    public void theBackfillFillsOnlyAMissingTargetOnceAndWritesToNobody() throws IOException {
        reportSerology();
        scoringService.scoreCycle(cycle.getId(), USER);
        Long followupId = jdbc.queryForObject("SELECT id FROM clinlims.eqa_participant_followup WHERE cycle_id = ?",
                Long.class, cycle.getId());
        jdbc.update("UPDATE clinlims.eqa_participant_followup SET participant_result_summary_json = ? WHERE id = ?",
                scoredBeforeTheFix(followupId), followupId);
        Map<String, Object> before = jdbc.queryForMap(
                "SELECT followup_status, notified_at FROM clinlims.eqa_participant_followup WHERE id = ?", followupId);
        int notificationsBefore = notificationCount();
        int auditBefore = auditCount(followupId);

        assertEquals("one row was missing a target its result has", 1, backfill.backfill(USER));

        assertEquals(List.of("Reactive", "Kept as scored", "null"),
                registerResults(FAILING).stream().map(row -> String.valueOf(row.get("target"))).toList());
        assertEquals("the change is in the audit trail", auditBefore + 1, auditCount(followupId));
        String afterFirstRun = summaryJson(followupId);
        assertTrue("a number on another row is written back as it was stored: " + afterFirstRun,
                afterFirstRun.contains("\"reported\":60.00000"));

        assertEquals("a second run finds nothing to fill", 0, backfill.backfill(USER));
        assertEquals(afterFirstRun, summaryJson(followupId));
        assertEquals("and writes no second audit row", auditBefore + 1, auditCount(followupId));

        assertEquals("the participant is not notified again", notificationsBefore, notificationCount());
        assertEquals("nor is the row's state or notification date touched", before, jdbc.queryForMap(
                "SELECT followup_status, notified_at FROM clinlims.eqa_participant_followup WHERE id = ?", followupId));
    }

    /**
     * The register row as develop wrote it: the qualitative failure with no target,
     * beside a row whose target was already set (it must be left as it is, its
     * number written the way develop wrote a BigDecimal) and a row naming a test
     * this laboratory never reported, which has no result to take a target from.
     */
    private String scoredBeforeTheFix(Long followupId) throws IOException {
        ObjectNode summary = (ObjectNode) JSON.readTree(summaryJson(followupId));
        ArrayNode rows = (ArrayNode) summary.get("unacceptable");
        ObjectNode scored = (ObjectNode) rows.get(0);
        scored.putNull("target");
        ObjectNode alreadySet = scored.deepCopy();
        alreadySet.put("target", "Kept as scored");
        alreadySet.put("reported", "REPORTED_NUMBER");
        ObjectNode unreported = scored.deepCopy();
        unreported.put("testId", TEST_VL);
        unreported.putNull("panelSampleId");
        rows.add(alreadySet);
        rows.add(unreported);
        return JSON.writeValueAsString(summary).replace("\"REPORTED_NUMBER\"", "60.00000");
    }

    private void reportSerology() {
        for (int i = 0; i < ORGS; i++) {
            scoringService.takeIn(cycle.getId(), FIRST_ORG + i,
                    byTest(Map.of(TEST_SERO, FIRST_ORG + i == FAILING ? "Non-reactive" : "Reactive")),
                    EQASubmissionMethod.MANUAL, USER);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> registerResults(long organizationId) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> row : followupService.getProviderRegisterRows()) {
            if (Long.valueOf(organizationId).equals(((Number) row.get("participantOrgId")).longValue())) {
                results.addAll((List<Map<String, Object>>) row.get("results"));
            }
        }
        return results;
    }

    private String summaryJson(Long followupId) {
        return jdbc.queryForObject(
                "SELECT participant_result_summary_json FROM clinlims.eqa_participant_followup WHERE id = ?",
                String.class, followupId);
    }

    private int notificationCount() {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.notification_log", Integer.class);
    }

    private int auditCount(Long followupId) {
        String tableId = referenceTablesService.getReferenceTableByName("eqa_participant_followup").getId();
        return historyService.getHistoryByRefIdAndRefTableId(String.valueOf(followupId), tableId).size();
    }

    private void seal(EQAPanel panel, String code, long analyteId, String target, BigDecimal low, BigDecimal high) {
        EQAPanelSample sample = new EQAPanelSample();
        sample.setPanel(panel);
        sample.setSampleCode(code);
        sample.setAnalyteId(analyteId);
        sample.setTargetValue(target);
        sample.setAcceptanceRangeLow(low);
        sample.setAcceptanceRangeHigh(high);
        sample.setSysUserId(USER);
        eqaPanelSampleDAO.insert(sample);
    }

    private void analyte(long id, String name) {
        jdbc.update("INSERT INTO clinlims.analyte (id, name, is_active, lastupdated) VALUES (?, ?, 'Y', now())"
                + " ON CONFLICT (id) DO NOTHING", id, name);
    }

    private void test(long id, String name, long analyteId, int linkId) {
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " SELECT ?, ?, ?, 'Y', ?, now() WHERE NOT EXISTS (SELECT 1 FROM clinlims.test WHERE id = ?)",
                id, name, name, UUID.randomUUID().toString(), id);
        jdbc.update("DELETE FROM clinlims.test_analyte WHERE id = ?", linkId);
        jdbc.update("INSERT INTO clinlims.test_analyte (id, test_id, analyte_id, lastupdated) VALUES (?, ?, ?, now())",
                linkId, id, analyteId);
    }
}
