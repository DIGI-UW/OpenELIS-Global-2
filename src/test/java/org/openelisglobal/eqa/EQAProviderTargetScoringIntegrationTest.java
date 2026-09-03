package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
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
 * Provider scoring judges a quantitative result against the acceptance range
 * the panel seals, not only against the other participants.
 *
 * <p>
 * The peer score alone cannot reach its own thresholds on a small roster — the
 * largest |Z| attainable over n values is (n−1)/√n, so at five participants no
 * value can exceed 1.79 and nothing downstream of an unacceptable verdict (a
 * follow-up, a non-conformity, a competency event) could ever fire.
 */
public class EQAProviderTargetScoringIntegrationTest extends EQASpineTestBase {

    private static final long FIRST_ORG = 9955L;
    private static final int ORGS = 5;
    private static final long TEST_CD4 = 9967L;
    private static final long ANALYTE_CD4 = 9868L;
    private static final int TEST_ANALYTE_LINK = 99867;

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
                    + " VALUES (?, ?, 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING", id, "Target lab " + id);
        }
        jdbc.update("INSERT INTO clinlims.analyte (id, name, is_active, lastupdated) VALUES (?, ?, 'Y', now())"
                + " ON CONFLICT (id) DO NOTHING", ANALYTE_CD4, "Target CD4");
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " SELECT ?, ?, ?, 'Y', ?, now() WHERE NOT EXISTS (SELECT 1 FROM clinlims.test WHERE id = ?)",
                TEST_CD4, "Target CD4 test", "Target CD4 test", UUID.randomUUID().toString(), TEST_CD4);
        jdbc.update("DELETE FROM clinlims.test_analyte WHERE id = ?", TEST_ANALYTE_LINK);
        jdbc.update("INSERT INTO clinlims.test_analyte (id, test_id, analyte_id, lastupdated) VALUES (?, ?, ?, now())",
                TEST_ANALYTE_LINK, TEST_CD4, ANALYTE_CD4);

        scheme = insertScheme("Target scheme " + System.nanoTime(), EQASchemeType.REGIONAL_PT, "CPHL");
        eqaProgramService.assignTest(scheme.getId(), TEST_CD4);
        cycle = readBack(insertCycle(scheme, 1));
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'SUBMISSIONS_OPEN' WHERE id = ?", cycle.getId());
        panel = insertPanel(scheme, p -> {
            p.setCycle(cycle);
            p.setPanelName("Target panel");
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
            jdbc.update("DELETE FROM clinlims.test_analyte WHERE id = ?", TEST_ANALYTE_LINK);
            jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_CD4);
            jdbc.update("DELETE FROM clinlims.analyte WHERE id = ?", ANALYTE_CD4);
            jdbc.update("DELETE FROM clinlims.organization WHERE id BETWEEN ? AND ?", FIRST_ORG, FIRST_ORG + ORGS);
        }
    }

    @Test
    public void aLaboratoryReportingDoubleTheTargetIsUnacceptableAtFiveParticipants() {
        sealTarget("40", new BigDecimal("36"), new BigDecimal("44"));
        report("39.5", "40", "40.5", "41", "80");

        Map<String, Object> summary = scoringService.scoreCycle(cycle.getId(), USER);

        long outlier = FIRST_ORG + ORGS - 1;
        assertEquals("double the target is outside the sealed range", "UNACCEPTABLE", verdict(outlier));
        for (int i = 0; i < ORGS - 1; i++) {
            assertEquals("inside the range", "ACCEPTABLE", verdict(FIRST_ORG + i));
        }
        assertEquals(5, summary.get("judgedAgainstTargetCount"));
        assertEquals("the failing laboratory reaches the follow-up register", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_participant_followup WHERE cycle_id = ?",
                        Integer.class, cycle.getId()));
        assertEquals(Integer.valueOf(1), summary.get("followupCount"));
    }

    @Test
    public void thePeerScoreIsKeptBesideTheVerdictAndTheTargetIsRecorded() {
        sealTarget("40", new BigDecimal("36"), new BigDecimal("44"));
        report("39.5", "40", "40.5", "41", "80");

        scoringService.scoreCycle(cycle.getId(), USER);

        long outlier = FIRST_ORG + ORGS - 1;
        assertNotNull("the peer statistic still reports, it just no longer decides", zScore(outlier));
        assertEquals(0, new BigDecimal("40").compareTo(targetValue(outlier)));
        String csv = scoringService.buildScoreCsv(cycle.getId(), outlier);
        String row = csv.lines().filter(line -> line.startsWith("Target CD4 test")).findFirst().orElse("");
        assertTrue("the scores CSV shows the target the verdict was measured against: " + row,
                row.contains(",40.00000,") || row.contains(",40,"));
        assertTrue("and the verdict beside it: " + row, row.contains("UNACCEPTABLE"));
    }

    @Test
    public void aTargetWithNoAcceptanceRangeLeavesThePeerVerdictAlone() {
        // No range sealed: comparing for equality would fail a laboratory for
        // answering 39.5 to a target of 40, which is not what a range-less target
        // means. The peer verdict stands and the target is recorded for the report.
        sealTarget("40", null, null);
        report("39.5", "40", "40.5", "41", "80");

        Map<String, Object> summary = scoringService.scoreCycle(cycle.getId(), USER);

        assertEquals("ACCEPTABLE", verdict(FIRST_ORG));
        assertEquals(0, summary.get("judgedAgainstTargetCount"));
        assertEquals(0, new BigDecimal("40").compareTo(targetValue(FIRST_ORG)));
    }

    @Test
    public void aTargetedCycleScoresBelowThePeerFloor() {
        sealTarget("40", new BigDecimal("36"), new BigDecimal("44"));
        report("40", "41", "80");

        Map<String, Object> summary = scoringService.scoreCycle(cycle.getId(), USER);

        assertEquals("UNACCEPTABLE", verdict(FIRST_ORG + 2));
        assertEquals("ACCEPTABLE", verdict(FIRST_ORG));
        assertEquals(3, summary.get("judgedAgainstTargetCount"));
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

    // ---- helpers ----

    private void sealTarget(String targetValue, BigDecimal low, BigDecimal high) {
        EQAPanelSample sample = new EQAPanelSample();
        sample.setPanel(panel);
        sample.setSampleCode("T01");
        sample.setAnalyteId(ANALYTE_CD4);
        sample.setTargetValue(targetValue);
        sample.setAcceptanceRangeLow(low);
        sample.setAcceptanceRangeHigh(high);
        sample.setSysUserId(USER);
        eqaPanelSampleDAO.insert(sample);
    }

    private void report(String... values) {
        for (int i = 0; i < values.length; i++) {
            scoringService.takeIn(cycle.getId(), FIRST_ORG + i, Map.of(TEST_CD4, values[i]), EQASubmissionMethod.MANUAL,
                    USER);
        }
    }

    private String verdict(long organizationId) {
        return jdbc.queryForObject(
                "SELECT performance_status FROM clinlims.eqa_result"
                        + " WHERE participant_organization_id = ? AND test_id = ?",
                String.class, organizationId, TEST_CD4);
    }

    private BigDecimal zScore(long organizationId) {
        return jdbc.queryForObject(
                "SELECT z_score FROM clinlims.eqa_result WHERE participant_organization_id = ? AND test_id = ?",
                BigDecimal.class, organizationId, TEST_CD4);
    }

    private BigDecimal targetValue(long organizationId) {
        return jdbc.queryForObject(
                "SELECT target_value FROM clinlims.eqa_result WHERE participant_organization_id = ? AND test_id = ?",
                BigDecimal.class, organizationId, TEST_CD4);
    }
}
