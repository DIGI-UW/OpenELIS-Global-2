package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.service.EQAProviderScoringService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provider-side intake of participant results (OGC-613): phoned and emailed
 * values keyed on the provider, numbers and qualitative words alike, a
 * participant's export bundle imported by analyte name, peer statistics per
 * test, and qualitative verdicts judged against the panel's sealed target.
 */
public class EQAProviderIntakeIntegrationTest extends EQASpineTestBase {

    private static final long FIRST_ORG = 9975L;
    private static final int ORGS = 12;
    private static final long TEST_VL = 9987L;
    private static final long TEST_SERO = 9988L;
    private static final long TEST_CD4 = 9989L;
    private static final long ANALYTE_VL = 9811L;
    private static final long ANALYTE_SERO = 9812L;
    private static final long ANALYTE_CD4 = 9813L;

    @Autowired
    private EQAProviderScoringService scoringService;
    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;

    private EQAProgram scheme;
    private EQACycle cycle;
    private EQAPanel panel;

    @Before
    public void seed() {
        for (long id = FIRST_ORG; id < FIRST_ORG + ORGS; id++) {
            jdbc.update("INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                    + " VALUES (?, ?, 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING", id, "Intake lab " + id);
        }
        analyte(ANALYTE_VL, "Intake HIV VL");
        analyte(ANALYTE_SERO, "Intake HIV serology");
        analyte(ANALYTE_CD4, "Intake CD4");
        test(TEST_VL, "Intake HIV VL test", ANALYTE_VL, 99811);
        test(TEST_SERO, "Intake HIV serology test", ANALYTE_SERO, 99812);
        test(TEST_CD4, "Intake CD4 test", ANALYTE_CD4, 99813);

        scheme = insertScheme("Intake scheme " + System.nanoTime(), EQASchemeType.REGIONAL_PT, "CPHL");
        eqaProgramService.assignTest(scheme.getId(), TEST_VL);
        eqaProgramService.assignTest(scheme.getId(), TEST_SERO);
        eqaProgramService.assignTest(scheme.getId(), TEST_CD4);
        cycle = readBack(insertCycle(scheme, 1));
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'SUBMISSIONS_OPEN' WHERE id = ?", cycle.getId());

        panel = insertPanel(scheme, p -> {
            p.setCycle(cycle);
            p.setPanelName("Intake panel");
        });
        EQAPanelSample sample = new EQAPanelSample();
        sample.setPanel(panel);
        sample.setSampleCode("S01");
        sample.setAnalyteId(ANALYTE_SERO);
        sample.setTargetValue("Reactive");
        sample.setSysUserId(USER);
        eqaPanelSampleDAO.insert(sample);
    }

    @Override
    protected void cleanEqaTables() {
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.eqa_result");
            jdbc.update("DELETE FROM clinlims.eqa_distribution WHERE cycle_id IS NOT NULL");
        }
        super.cleanEqaTables();
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.test_analyte WHERE id IN (99811, 99812, 99813)");
            jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?, ?)", TEST_VL, TEST_SERO, TEST_CD4);
            jdbc.update("DELETE FROM clinlims.analyte WHERE id IN (?, ?, ?)", ANALYTE_VL, ANALYTE_SERO, ANALYTE_CD4);
            jdbc.update("DELETE FROM clinlims.organization WHERE id BETWEEN ? AND ?", FIRST_ORG, FIRST_ORG + ORGS);
        }
    }

    @Test
    public void reportedValuesLandAsNumbersOrWordsAndAreEchoedBack() {
        Map<String, Object> grid = scoringService.takeIn(cycle.getId(), FIRST_ORG,
                Map.of(TEST_VL, "105.5", TEST_SERO, "Reactive"), EQASubmissionMethod.MANUAL, USER);

        assertEquals(new BigDecimal("105.50000"), resultValue(FIRST_ORG, TEST_VL));
        assertNull(resultText(FIRST_ORG, TEST_VL));
        assertEquals("Reactive", resultText(FIRST_ORG, TEST_SERO));
        assertNull(resultValue(FIRST_ORG, TEST_SERO));
        assertEquals("Reactive", reportedInGrid(grid, TEST_SERO));
        assertEquals(0, new BigDecimal("105.5").compareTo((BigDecimal) reportedInGrid(grid, TEST_VL)));
        assertNull("nothing reported for CD4 yet", reportedInGrid(grid, TEST_CD4));

        scoringService.takeIn(cycle.getId(), FIRST_ORG, Map.of(TEST_SERO, "Non-reactive"), EQASubmissionMethod.MANUAL,
                USER);
        assertEquals("a second entry overwrites, it does not duplicate", "Non-reactive",
                resultText(FIRST_ORG, TEST_SERO));
        assertEquals(Integer.valueOf(2),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_result WHERE participant_organization_id = ?",
                        Integer.class, FIRST_ORG));
    }

    @Test
    public void peerStatisticsAreComputedPerTestNotAcrossThePanel() {
        for (int i = 0; i < ORGS; i++) {
            long org = FIRST_ORG + i;
            String viralLoad = i == ORGS - 1 ? "400" : "100";
            String cd4 = String.valueOf(1000 + i);
            scoringService.takeIn(cycle.getId(), org, Map.of(TEST_VL, viralLoad, TEST_CD4, cd4),
                    EQASubmissionMethod.MANUAL, USER);
        }

        scoringService.scoreCycle(cycle.getId(), USER);

        assertEquals("the viral-load outlier is judged against viral loads", "UNACCEPTABLE",
                verdict(FIRST_ORG + ORGS - 1, TEST_VL));
        assertEquals("ACCEPTABLE", verdict(FIRST_ORG, TEST_VL));
        // Pooled across the panel, every CD4 (~1000) would sit far above a mean pulled
        // down by the viral loads and read as positive Z; per test, the lowest CD4 is
        // below its own peers and the highest above.
        assertTrue("lowest CD4 sits below its peers", zScore(FIRST_ORG, TEST_CD4).signum() < 0);
        assertTrue("highest CD4 sits above its peers", zScore(FIRST_ORG + ORGS - 1, TEST_CD4).signum() > 0);
        assertEquals("ACCEPTABLE", verdict(FIRST_ORG + ORGS - 1, TEST_CD4));
    }

    @Test
    public void qualitativeResultsAreJudgedAgainstThePanelTarget() {
        for (int i = 0; i < ORGS; i++) {
            scoringService.takeIn(cycle.getId(), FIRST_ORG + i,
                    Map.of(TEST_SERO, i == ORGS - 1 ? "Non-reactive" : "reactive"), EQASubmissionMethod.MANUAL, USER);
        }

        scoringService.scoreCycle(cycle.getId(), USER);

        assertEquals("ACCEPTABLE", verdict(FIRST_ORG, TEST_SERO));
        assertEquals("UNACCEPTABLE", verdict(FIRST_ORG + ORGS - 1, TEST_SERO));
        assertNull("a word has no Z", zScore(FIRST_ORG + ORGS - 1, TEST_SERO));
        assertEquals("the failing participant reaches the follow-up register", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_participant_followup WHERE cycle_id = ?",
                        Integer.class, cycle.getId()));
    }

    /**
     * The peer z-score cannot carry a verdict at the roster size scoring requires:
     * with the sample standard deviation taken over every reported value, the
     * largest z anyone can reach is (n-1)/sqrt(n), which is 1.79 for five
     * participants — below the questionable threshold, let alone the unacceptable
     * one. So a laboratory reporting double the target used to score acceptable.
     * The panel's sealed target and range decide instead, and the z stays on the
     * row as the reported statistic. The other tests here keep twelve participants,
     * where the ceiling happens to clear 3, which is why this went unnoticed.
     */
    @Test
    public void aNumericOutlierIsUnacceptableAtTheFiveParticipantScoringFloor() {
        EQAPanelSample sealed = new EQAPanelSample();
        sealed.setPanel(panel);
        sealed.setSampleCode("S02");
        sealed.setAnalyteId(ANALYTE_VL);
        sealed.setTargetValue("40");
        sealed.setAcceptanceRangeLow(new BigDecimal("36"));
        sealed.setAcceptanceRangeHigh(new BigDecimal("44"));
        sealed.setSysUserId(USER);
        eqaPanelSampleDAO.insert(sealed);

        String[] reported = { "39.5", "40", "40.5", "41", "80" };
        for (int i = 0; i < reported.length; i++) {
            scoringService.takeIn(cycle.getId(), FIRST_ORG + i, Map.of(TEST_VL, reported[i]),
                    EQASubmissionMethod.MANUAL, USER);
        }
        long outlier = FIRST_ORG + reported.length - 1;

        scoringService.scoreCycle(cycle.getId(), USER);

        assertEquals("a value inside the sealed range is acceptable", "ACCEPTABLE", verdict(FIRST_ORG, TEST_VL));
        assertEquals("double the target is unacceptable however tight the peer group is", "UNACCEPTABLE",
                verdict(outlier, TEST_VL));
        assertTrue("the peer z is still reported, and is below the threshold it could not reach",
                zScore(outlier, TEST_VL).abs().compareTo(new BigDecimal("2")) < 0);
        assertEquals("the report and the scores CSV can show what the number was judged against", 0,
                targetValue(outlier, TEST_VL).compareTo(new BigDecimal("40")));
        assertEquals("the failure reaches the follow-up register", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_participant_followup WHERE cycle_id = ?",
                        Integer.class, cycle.getId()));
    }

    @Test
    public void aParticipantExportBundleImportsByAnalyteName() {
        String csv = "cycle_id,cycle_name,round_number,analyte_id,analyte_name,result_value,result_unit,"
                + "submission_status,entered_at\n" + "9,,1,4711,Intake HIV serology,Reactive,,SUBMITTED,\n"
                + "9,,1,4712,intake hiv vl,250,copies/mL,SUBMITTED,\n"
                + "9,,1,4713,Something this scheme does not run,5,,SUBMITTED,\n";

        Map<String, Object> outcome = scoringService.importReportedCsv(cycle.getId(), FIRST_ORG, csv, USER);

        assertEquals(2, outcome.get("imported"));
        List<?> errors = (List<?>) outcome.get("errors");
        assertEquals(1, errors.size());
        assertTrue(String.valueOf(errors.get(0)), String.valueOf(errors.get(0)).contains("Something this scheme"));
        assertEquals("Reactive", resultText(FIRST_ORG, TEST_SERO));
        assertEquals(0, new BigDecimal("250").compareTo(resultValue(FIRST_ORG, TEST_VL)));
        assertEquals("FILE_UPLOAD", jdbc.queryForObject(
                "SELECT submission_method FROM clinlims.eqa_result WHERE participant_organization_id = ? AND test_id = ?",
                String.class, FIRST_ORG, TEST_VL));
    }

    @Test
    public void intakeRefusesATestOutsideTheSchemeAndACycleNotYetShipped() {
        try {
            scoringService.takeIn(cycle.getId(), FIRST_ORG, Map.of(424242L, "1"), EQASubmissionMethod.MANUAL, USER);
            fail("a test the scheme does not run must be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("not part of scheme"));
        }
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'PLANNED' WHERE id = ?", cycle.getId());
        try {
            scoringService.takeIn(cycle.getId(), FIRST_ORG, Map.of(TEST_VL, "1"), EQASubmissionMethod.MANUAL, USER);
            fail("a cycle that has not shipped has no results to take in");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("PLANNED"));
        }
        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_result", Integer.class));
    }

    // ---- helpers ----

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

    private BigDecimal resultValue(long organizationId, long testId) {
        return jdbc.queryForObject(
                "SELECT result_value FROM clinlims.eqa_result"
                        + " WHERE participant_organization_id = ? AND test_id = ?",
                BigDecimal.class, organizationId, testId);
    }

    private String resultText(long organizationId, long testId) {
        return jdbc.queryForObject(
                "SELECT result_text FROM clinlims.eqa_result"
                        + " WHERE participant_organization_id = ? AND test_id = ?",
                String.class, organizationId, testId);
    }

    private String verdict(long organizationId, long testId) {
        return jdbc.queryForObject(
                "SELECT performance_status FROM clinlims.eqa_result"
                        + " WHERE participant_organization_id = ? AND test_id = ?",
                String.class, organizationId, testId);
    }

    private BigDecimal targetValue(long organizationId, long testId) {
        return jdbc.queryForObject(
                "SELECT target_value FROM clinlims.eqa_result"
                        + " WHERE participant_organization_id = ? AND test_id = ?",
                BigDecimal.class, organizationId, testId);
    }

    private BigDecimal zScore(long organizationId, long testId) {
        return jdbc.queryForObject(
                "SELECT z_score FROM clinlims.eqa_result" + " WHERE participant_organization_id = ? AND test_id = ?",
                BigDecimal.class, organizationId, testId);
    }

    @SuppressWarnings("unchecked")
    private Object reportedInGrid(Map<String, Object> grid, long testId) {
        for (Map<String, Object> row : (List<Map<String, Object>>) grid.get("tests")) {
            if (Long.valueOf(testId).equals(row.get("testId"))) {
                return row.get("reported");
            }
        }
        throw new AssertionError("test " + testId + " missing from the grid");
    }
}
