package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.service.EQACycleSubmissionService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Scores by file (OGC-610): a participant pastes the provider's scores CSV and
 * its submitted results are scored, matched by analyte name; rows naming an
 * analyte this laboratory does not know are reported, and a second paste has
 * nothing left to score.
 */
public class EQAScoreCsvIntakeIntegrationTest extends EQASpineTestBase {

    private static final long ENROLLMENT = 9906L;
    private static final long ANALYTE = 9831L;
    private static final String ANALYTE_NAME = "Csv intake analyte";

    @Autowired
    private EQACycleSubmissionService cycleSubmissionService;

    private EQACycle cycle;

    @Before
    public void seed() {
        jdbc.update("INSERT INTO clinlims.analyte (id, name, is_active, lastupdated) VALUES (?, ?, 'Y', now())"
                + " ON CONFLICT (id) DO NOTHING", ANALYTE, ANALYTE_NAME);
        seedEnrollment(ENROLLMENT, "Csv intake programme");
        EQAProgram scheme = insertScheme("Csv intake scheme " + System.nanoTime(), EQASchemeType.REGIONAL_PT, "CPHL");
        cycle = readBack(insertCycle(scheme, 1));
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'SUBMITTED' WHERE id = ?", cycle.getId());
        Long roundId = insertRound(cycle, 1, "OPEN");
        EQARound round = eqaRoundDAO.get(roundId).orElseThrow(AssertionError::new);
        insertParticipantResult(cycle, round, ENROLLMENT, ANALYTE, EQASubmissionStatus.SUBMITTED, "105");
    }

    @Override
    protected void cleanEqaTables() {
        super.cleanEqaTables();
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.analyte WHERE id = ?", ANALYTE);
        }
    }

    @Test
    public void theProviderScoresCsvScoresTheSubmittedResultsByAnalyteName() {
        String csv = "test,analyte_name,result_value,target_value,z_score,performance_status,scored_on\n"
                + "HIV VL (provider's name),csv intake analyte,105,100,1.2,ACCEPTABLE,2026-09-03\n"
                + "Other,An analyte nobody here runs,1,,,UNACCEPTABLE,2026-09-03\n";

        Map<String, Object> outcome = cycleSubmissionService.intakeScoresCsv(cycle.getId(), ENROLLMENT, csv, USER);

        assertEquals(1, outcome.get("scored"));
        assertEquals(List.of("An analyte nobody here runs"), outcome.get("unmapped"));
        Map<String, Object> result = jdbc.queryForMap("SELECT submission_status, performance_status, z_score"
                + " FROM clinlims.eqa_participant_result WHERE cycle_id = ?", cycle.getId());
        assertEquals("SCORED", result.get("submission_status"));
        assertEquals("ACCEPTABLE", result.get("performance_status"));
        assertEquals(0, new BigDecimal("1.2").compareTo((BigDecimal) result.get("z_score")));
        assertEquals("SCORED",
                jdbc.queryForObject("SELECT status FROM clinlims.eqa_cycle WHERE id = ?", String.class, cycle.getId()));

        try {
            cycleSubmissionService.intakeScoresCsv(cycle.getId(), ENROLLMENT, csv, USER);
            fail("a second paste has no submitted result left to score");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("No submitted result"));
        }
    }

    @Test
    public void aCsvWithoutTheScoreColumnsIsRefused() {
        try {
            cycleSubmissionService.intakeScoresCsv(cycle.getId(), ENROLLMENT, "a,b\n1,2\n", USER);
            fail("the header must name analyte_name and performance_status");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("analyte_name"));
        }
        assertEquals("SUBMITTED",
                jdbc.queryForObject("SELECT submission_status FROM clinlims.eqa_participant_result WHERE cycle_id = ?",
                        String.class, cycle.getId()));
    }
}
