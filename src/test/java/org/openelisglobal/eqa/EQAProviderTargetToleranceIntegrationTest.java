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
            scoringService.takeIn(cycle.getId(), FIRST_ORG + i, Map.of(TEST_CD4, values[i]), EQASubmissionMethod.MANUAL,
                    USER);
        }
    }

    /**
     * The same CD4 values, each laboratory also reporting the untargeted HB test.
     */
    private void reportBoth(String... values) {
        for (int i = 0; i < values.length; i++) {
            scoringService.takeIn(cycle.getId(), FIRST_ORG + i, Map.of(TEST_CD4, values[i], TEST_HB, "12"),
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
}
