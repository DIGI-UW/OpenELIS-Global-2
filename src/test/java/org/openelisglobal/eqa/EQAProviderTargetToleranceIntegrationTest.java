package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.service.EQAIntakeValue;
import org.openelisglobal.eqa.service.EQAProviderScoringService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * What a sealed target may and may not decide on the provider side, and when a
 * cycle can be scored at all.
 *
 * <p>
 * A target with an acceptance range states a tolerance, and a measurement
 * outside it has failed. A target with no range states only a number, and
 * comparing a measurement to it for equality would fail a laboratory that
 * answered 39.5 to a target of 40 — so there the peer statistic stays the
 * verdict. The participant floor exists to protect that statistic, which means
 * a cycle whose panel does seal a target does not need the crowd.
 */
public class EQAProviderTargetToleranceIntegrationTest extends EQASpineTestBase {

    private static final long FIRST_ORG = 9925L;
    private static final long TEST_CD4 = 9927L;
    private static final long ANALYTE_CD4 = 9828L;
    private static final int TEST_ANALYTE_LINK = 99827;
    /**
     * A second analyte on the same cycle, so a panel can target one and not the
     * other.
     */
    private static final long TEST_HB = 9929L;
    private static final long ANALYTE_HB = 9830L;
    private static final int TEST_ANALYTE_LINK_HB = 99829;

    @Autowired
    private EQAProviderScoringService scoringService;
    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;

    private EQACycle cycle;
    private EQAPanel panel;

    @Before
    public void seed() {
        for (long id = FIRST_ORG; id < FIRST_ORG + 5; id++) {
            jdbc.update("INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                    + " VALUES (?, ?, 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING", id, "Tolerance lab " + id);
        }
        jdbc.update("INSERT INTO clinlims.analyte (id, name, is_active, lastupdated) VALUES (?, ?, 'Y', now())"
                + " ON CONFLICT (id) DO NOTHING", ANALYTE_CD4, "Tolerance CD4");
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " SELECT ?, ?, ?, 'Y', ?, now() WHERE NOT EXISTS (SELECT 1 FROM clinlims.test WHERE id = ?)",
                TEST_CD4, "Tolerance CD4 test", "Tolerance CD4 test", UUID.randomUUID().toString(), TEST_CD4);
        jdbc.update("DELETE FROM clinlims.test_analyte WHERE id = ?", TEST_ANALYTE_LINK);
        jdbc.update("INSERT INTO clinlims.test_analyte (id, test_id, analyte_id, lastupdated) VALUES (?, ?, ?, now())",
                TEST_ANALYTE_LINK, TEST_CD4, ANALYTE_CD4);
        jdbc.update("INSERT INTO clinlims.analyte (id, name, is_active, lastupdated) VALUES (?, ?, 'Y', now())"
                + " ON CONFLICT (id) DO NOTHING", ANALYTE_HB, "Tolerance HB");
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " SELECT ?, ?, ?, 'Y', ?, now() WHERE NOT EXISTS (SELECT 1 FROM clinlims.test WHERE id = ?)",
                TEST_HB, "Tolerance HB test", "Tolerance HB test", UUID.randomUUID().toString(), TEST_HB);
        jdbc.update("DELETE FROM clinlims.test_analyte WHERE id = ?", TEST_ANALYTE_LINK_HB);
        jdbc.update("INSERT INTO clinlims.test_analyte (id, test_id, analyte_id, lastupdated) VALUES (?, ?, ?, now())",
                TEST_ANALYTE_LINK_HB, TEST_HB, ANALYTE_HB);

        EQAProgram scheme = insertScheme("Tolerance scheme " + System.nanoTime(), EQASchemeType.REGIONAL_PT, "CPHL");
        eqaProgramService.assignTest(scheme.getId(), TEST_CD4);
        eqaProgramService.assignTest(scheme.getId(), TEST_HB);
        cycle = readBack(insertCycle(scheme, 1));
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'SUBMISSIONS_OPEN' WHERE id = ?", cycle.getId());
        panel = insertPanel(scheme, p -> {
            p.setCycle(cycle);
            p.setPanelName("Tolerance panel");
        });
    }

    @Override
    protected void cleanEqaTables() {
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.eqa_result");
            jdbc.update("DELETE FROM clinlims.eqa_distribution WHERE cycle_id IS NOT NULL");
        }
        super.cleanEqaTables();
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.test_analyte WHERE id IN (?, ?)", TEST_ANALYTE_LINK,
                    TEST_ANALYTE_LINK_HB);
            jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?)", TEST_CD4, TEST_HB);
            jdbc.update("DELETE FROM clinlims.analyte WHERE id IN (?, ?)", ANALYTE_CD4, ANALYTE_HB);
            jdbc.update("DELETE FROM clinlims.organization WHERE id BETWEEN ? AND ?", FIRST_ORG, FIRST_ORG + 5);
        }
    }

    @Test
    public void aTargetWithNoAcceptanceRangeDoesNotFailANearMiss() {
        sealTarget("40", null, null);
        report("39.5", "40", "40.5", "41", "80");

        scoringService.scoreCycle(cycle.getId(), USER);

        assertEquals("39.5 against a bare target of 40 is not a proficiency failure", "ACCEPTABLE", verdict(FIRST_ORG));
        assertEquals("nor is 40.5", "ACCEPTABLE", verdict(FIRST_ORG + 2));
        assertEquals("the target is still recorded for the report", 0,
                new BigDecimal("40").compareTo(targetValue(FIRST_ORG)));
    }

    @Test
    public void aSealedRangeStillDecides() {
        sealTarget("40", new BigDecimal("36"), new BigDecimal("44"));
        report("39.5", "40", "40.5", "41", "80");

        scoringService.scoreCycle(cycle.getId(), USER);

        assertEquals("outside the range", "UNACCEPTABLE", verdict(FIRST_ORG + 4));
        assertEquals("inside it", "ACCEPTABLE", verdict(FIRST_ORG));
    }

    @Test
    public void aTargetedCycleScoresBelowThePeerFloor() {
        sealTarget("40", new BigDecimal("36"), new BigDecimal("44"));
        report("40", "41", "80");

        scoringService.scoreCycle(cycle.getId(), USER);

        assertEquals("UNACCEPTABLE", verdict(FIRST_ORG + 2));
        assertEquals("ACCEPTABLE", verdict(FIRST_ORG));
    }

    @Test
    public void anUntargetedCycleBelowThePeerFloorIsStillRefused() {
        report("40", "41", "80");

        try {
            scoringService.scoreCycle(cycle.getId(), USER);
            fail("with no target and no crowd there is nothing to judge against");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("no target"));
        }
    }

    @Test
    public void aCycleWithNothingReportedIsRefused() {
        sealTarget("40", new BigDecimal("36"), new BigDecimal("44"));

        try {
            scoringService.scoreCycle(cycle.getId(), USER);
            fail("there is nothing to score");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("no reported results"));
        }
    }

    @Test
    public void anAnalyteWithNoSealedTargetIsCountedAndNamed() {
        sealTarget("40", new BigDecimal("36"), new BigDecimal("44"));
        reportBoth("40", "41", "80");

        Map<String, Object> summary = scoringService.scoreCycle(cycle.getId(), USER);

        assertEquals("CD4 carries the sealed target, so it is judged", "ACCEPTABLE", verdict(FIRST_ORG, TEST_CD4));
        assertEquals("UNACCEPTABLE", verdict(FIRST_ORG + 2, TEST_CD4));
        assertNull("HB has no target, and three laboratories is below the peer floor", verdict(FIRST_ORG, TEST_HB));

        assertEquals("one unjudged result per laboratory", 3, summary.get("unjudgedCount"));
        assertEquals("and the operator is told which test they sit on", List.of("Tolerance HB test"),
                summary.get("unjudgedTests"));
    }

    @Test
    public void everyAnalyteJudgedLeavesNothingToReport() {
        // The control. Without it the assertions above cannot tell "counted the
        // unjudged" from "counted every result on the second test".
        sealTarget("40", new BigDecimal("36"), new BigDecimal("44"));
        sealTargetFor(ANALYTE_HB, "T02", "12", new BigDecimal("11"), new BigDecimal("13"));
        reportBoth("40", "41", "80");

        Map<String, Object> summary = scoringService.scoreCycle(cycle.getId(), USER);

        assertEquals("HB now carries a target of its own", "ACCEPTABLE", verdict(FIRST_ORG, TEST_HB));
        assertEquals(0, summary.get("unjudgedCount"));
        assertEquals(List.of(), summary.get("unjudgedTests"));
    }

    // ---- OGC-1243: several samples of one test ----

    /**
     * A multi-level panel: two samples of one test with different targets. Each is
     * its own intake row, holds its own value, and is judged against its own range.
     * 260 passes only against the low level and 950 only against the high one, so
     * any cross-match shows as a wrong verdict.
     */
    @Test
    public void twoSamplesOfOneTestAreEachJudgedAgainstTheirOwnTarget() {
        Map<String, Long> samples = sealTwoCd4Levels();

        List<Map<String, Object>> cd4Rows = cd4Rows(scoringService.intakeGrid(cycle.getId(), FIRST_ORG));
        assertEquals("one intake row per panel sample", List.of("T01", "T02"),
                cd4Rows.stream().map(row -> row.get("sampleCode")).toList());

        scoringService.takeIn(cycle.getId(), FIRST_ORG, List.of(new EQAIntakeValue(TEST_CD4, samples.get("T01"), "260"),
                new EQAIntakeValue(TEST_CD4, samples.get("T02"), "950")), EQASubmissionMethod.MANUAL, USER);
        scoringService.scoreCycle(cycle.getId(), USER);

        assertEquals(List.of("T01|260.00000|250.00000|ACCEPTABLE", "T02|950.00000|900.00000|ACCEPTABLE"),
                storedCd4(FIRST_ORG));
    }

    @Test
    public void aValueThatDoesNotSayWhichOfTwoSamplesItAnswersIsRefused() {
        sealTwoCd4Levels();

        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, () -> scoringService
                .takeIn(cycle.getId(), FIRST_ORG, byTest(Map.of(TEST_CD4, "260")), EQASubmissionMethod.MANUAL, USER));
        assertTrue(refused.getMessage(), refused.getMessage().contains("has 2 samples"));
        assertEquals("nothing is written", 0,
                (int) jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_result", Integer.class));
    }

    @Test
    public void theScoresCsvCarriesOneRowPerSampleWithItsOwnTarget() {
        Map<String, Long> samples = sealTwoCd4Levels();
        scoringService.takeIn(cycle.getId(), FIRST_ORG, List.of(new EQAIntakeValue(TEST_CD4, samples.get("T01"), "260"),
                new EQAIntakeValue(TEST_CD4, samples.get("T02"), "600")), EQASubmissionMethod.MANUAL, USER);
        scoringService.scoreCycle(cycle.getId(), USER);

        List<String> lines = List.of(scoringService.buildScoreCsv(cycle.getId(), FIRST_ORG).split("\n"));
        assertEquals("test,analyte_name,result_value,target_value,z_score,performance_status,scored_on,sample_code",
                lines.get(0));
        List<String> rows = lines.subList(1, lines.size()).stream()
                .map(line -> String.join("|", List.of(line.split(",", -1)).subList(2, 6)) + "|"
                        + line.substring(line.lastIndexOf(',') + 1))
                .sorted().toList();
        assertEquals(List.of("260.00000|250.00000||ACCEPTABLE|T01", "600.00000|900.00000||UNACCEPTABLE|T02"), rows);
    }

    @Test
    public void aCsvImportMatchesEachRowToItsSampleByCode() {
        sealTwoCd4Levels();

        Map<String, Object> imported = scoringService.importReportedCsv(cycle.getId(), FIRST_ORG,
                "analyte_name,result_value,sample_code\nTolerance CD4,260,T01\nTolerance CD4,950,t02\n", USER);

        assertEquals(2, imported.get("imported"));
        assertEquals(List.of(), imported.get("errors"));
        scoringService.scoreCycle(cycle.getId(), USER);
        assertEquals(List.of("T01|260.00000|250.00000|ACCEPTABLE", "T02|950.00000|900.00000|ACCEPTABLE"),
                storedCd4(FIRST_ORG));
    }

    @Test
    public void aCsvRowNamingOnlyTheAnalyteOfTwoSamplesIsRejectedNotGuessed() {
        sealTwoCd4Levels();

        Map<String, Object> imported = scoringService.importReportedCsv(cycle.getId(), FIRST_ORG,
                "analyte_name,result_value\nTolerance CD4,260\n", USER);

        assertEquals(0, imported.get("imported"));
        assertEquals(List.of("Row 2: 'Tolerance CD4' is reported by 2 samples on this cycle's panel;"
                + " add a sample_code column naming which one"), imported.get("errors"));
        assertEquals(0, (int) jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_result", Integer.class));
    }

    /**
     * Another instance sending over FHIR keys each value by the sample code it was
     * given, where a one-sample panel would key it by analyte name.
     */
    @Test
    public void valuesKeyedBySampleCodeReachTheirOwnSamples() {
        sealTwoCd4Levels();
        Map<String, String> byLabel = new java.util.LinkedHashMap<>();
        byLabel.put("T01", "260");
        byLabel.put("T02", "950");

        Map<String, Object> grid = scoringService.takeInByAnalyteName(cycle.getId(), FIRST_ORG, byLabel,
                EQASubmissionMethod.FHIR, USER);

        assertEquals(List.of(), grid.get("unmapped"));
        scoringService.scoreCycle(cycle.getId(), USER);
        assertEquals(List.of("T01|260.00000|250.00000|ACCEPTABLE", "T02|950.00000|900.00000|ACCEPTABLE"),
                storedCd4(FIRST_ORG));
    }

    // ---- helpers ----

    private void sealTarget(String targetValue, BigDecimal low, BigDecimal high) {
        sealTargetFor(ANALYTE_CD4, "T01", targetValue, low, high);
    }

    private void sealTargetFor(long analyteId, String sampleCode, String targetValue, BigDecimal low, BigDecimal high) {
        EQAPanelSample sample = new EQAPanelSample();
        sample.setPanel(panel);
        sample.setSampleCode(sampleCode);
        sample.setAnalyteId(analyteId);
        sample.setTargetValue(targetValue);
        sample.setAcceptanceRangeLow(low);
        sample.setAcceptanceRangeHigh(high);
        sample.setSysUserId(USER);
        eqaPanelSampleDAO.insert(sample);
    }

    private void report(String... values) {
        for (int i = 0; i < values.length; i++) {
            scoringService.takeIn(cycle.getId(), FIRST_ORG + i, byTest(Map.of(TEST_CD4, values[i])),
                    EQASubmissionMethod.MANUAL, USER);
        }
    }

    /**
     * The same CD4 values, each laboratory also reporting the untargeted HB test.
     */
    private void reportBoth(String... values) {
        for (int i = 0; i < values.length; i++) {
            scoringService.takeIn(cycle.getId(), FIRST_ORG + i, byTest(Map.of(TEST_CD4, values[i], TEST_HB, "12")),
                    EQASubmissionMethod.MANUAL, USER);
        }
    }

    private String verdict(long organizationId) {
        return verdict(organizationId, TEST_CD4);
    }

    private String verdict(long organizationId, long testId) {
        return jdbc.queryForObject(
                "SELECT performance_status FROM clinlims.eqa_result"
                        + " WHERE participant_organization_id = ? AND test_id = ?",
                String.class, organizationId, testId);
    }

    private BigDecimal targetValue(long organizationId) {
        return jdbc.queryForObject(
                "SELECT target_value FROM clinlims.eqa_result WHERE participant_organization_id = ? AND test_id = ?",
                BigDecimal.class, organizationId, TEST_CD4);
    }

    /**
     * Two CD4 levels on the panel: T01 at 250 (200-300) and T02 at 900 (720-1080).
     */
    private Map<String, Long> sealTwoCd4Levels() {
        sealTargetFor(ANALYTE_CD4, "T01", "250", new BigDecimal("200"), new BigDecimal("300"));
        sealTargetFor(ANALYTE_CD4, "T02", "900", new BigDecimal("720"), new BigDecimal("1080"));
        Map<String, Long> ids = new java.util.HashMap<>();
        jdbc.query("SELECT id, sample_code FROM clinlims.eqa_panel_sample WHERE panel_id = ?", rs -> {
            ids.put(rs.getString("sample_code"), rs.getLong("id"));
        }, panel.getId());
        return ids;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> cd4Rows(Map<String, Object> grid) {
        return ((List<Map<String, Object>>) grid.get("tests")).stream()
                .filter(row -> Long.valueOf(TEST_CD4).equals(row.get("testId"))).toList();
    }

    /**
     * One laboratory's CD4 results as sample|reported|target|verdict, by sample
     * code.
     */
    private List<String> storedCd4(long organizationId) {
        return jdbc.query(
                "SELECT s.sample_code, r.result_value, r.target_value, r.performance_status"
                        + " FROM clinlims.eqa_result r JOIN clinlims.eqa_panel_sample s ON s.id = r.eqa_panel_sample_id"
                        + " WHERE r.participant_organization_id = ? AND r.test_id = ? ORDER BY s.sample_code",
                (rs, i) -> rs.getString(1) + "|" + rs.getBigDecimal(2).toPlainString() + "|"
                        + rs.getBigDecimal(3).toPlainString() + "|" + rs.getString(4),
                organizationId, TEST_CD4);
    }
}
