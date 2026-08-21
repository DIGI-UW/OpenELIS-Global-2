package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.service.EQALabPerformanceService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-611 (FR-V2.3-07) — the Lab Performance rollup against the real schema:
 * which results count towards the twelve-month KPIs, what a coverage cell says,
 * and which cycles reach the Recent Cycles list.
 */
public class EQALabPerformanceIntegrationTest extends EQASpineTestBase {

    private static final long FIRST_ANALYTE = 9820L;
    private static final int ANALYTE_COUNT = 8;
    private static final long ENROLLMENT = 9920L;

    /**
     * External PT is unique on round, enrollment and analyte, so every result in a
     * cycle needs an analyte of its own — the rollups do not care which.
     */
    private final Map<Long, EQARound> roundsByCycle = new HashMap<>();
    private int nextAnalyte;

    @Autowired
    private EQALabPerformanceService labPerformanceService;

    @Before
    public void seedEnrollmentRow() {
        seedEnrollment(ENROLLMENT, "EQA Performance Programme");
        for (int i = 0; i < ANALYTE_COUNT; i++) {
            long id = FIRST_ANALYTE + i;
            jdbc.update(
                    "INSERT INTO clinlims.analyte (id, name, is_active, lastupdated)"
                            + " SELECT ?, ?, 'Y', now() WHERE NOT EXISTS (SELECT 1 FROM clinlims.analyte WHERE id = ?)",
                    id, "EQA Performance Analyte " + i, id);
        }
        roundsByCycle.clear();
        nextAnalyte = 0;
    }

    @Test
    public void acceptanceRateCountsScoredResultsInTheWindowAndIgnoresDrafts() {
        EQAProgram scheme = insertScheme("Perf scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQACycle cycle = cycleEnding(scheme, 1, LocalDate.now().minusMonths(2));
        scored(cycle, "ACCEPTABLE", LocalDate.now().minusMonths(2));
        scored(cycle, "ACCEPTABLE", LocalDate.now().minusMonths(2));
        scored(cycle, "UNACCEPTABLE", LocalDate.now().minusMonths(2));
        // A draft is a working value the provider has never seen.
        Long draft = insertParticipantResult(cycle, roundsByCycle.get(cycle.getId()), ENROLLMENT, freshAnalyte(),
                EQASubmissionStatus.DRAFT, "99");
        setScore(draft, "UNACCEPTABLE", LocalDate.now().minusMonths(1));

        Map<String, Object> kpis = kpis();
        assertEquals(3, kpis.get("scoredCount"));
        assertEquals(2, kpis.get("acceptableCount"));
        assertEquals(Integer.valueOf(67), kpis.get("acceptanceRate"));
    }

    @Test
    public void aResultOlderThanTheWindowDropsOutOfTheRateAndIntoTheComparison() {
        EQAProgram scheme = insertScheme("Perf scheme aged", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQACycle recent = cycleEnding(scheme, 1, LocalDate.now().minusMonths(3));
        scored(recent, "ACCEPTABLE", LocalDate.now().minusMonths(3));
        EQACycle old = cycleEnding(scheme, 2, LocalDate.now().minusMonths(18));
        scored(old, "UNACCEPTABLE", LocalDate.now().minusMonths(18));

        Map<String, Object> kpis = kpis();
        assertEquals("only the last 12 months count", 1, kpis.get("scoredCount"));
        assertEquals(Integer.valueOf(100), kpis.get("acceptanceRate"));
        assertEquals(Integer.valueOf(0), kpis.get("priorAcceptanceRate"));
        assertEquals(Integer.valueOf(100), kpis.get("acceptanceDelta"));
    }

    @Test
    public void aSubmissionAfterThePlannedEndIsLateAndOneWithoutADeadlineIsNot() {
        EQAProgram scheme = insertScheme("Perf scheme timing", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQACycle onTime = cycleEnding(scheme, 1, LocalDate.now().minusMonths(2));
        submitted(onTime, LocalDate.now().minusMonths(3));
        EQACycle late = cycleEnding(scheme, 2, LocalDate.now().minusMonths(4));
        submitted(late, LocalDate.now().minusMonths(2));
        EQACycle undated = insertCycleWithDates(scheme, 3, null, null);
        submitted(undated, LocalDate.now().minusMonths(1));

        Map<String, Object> kpis = kpis();
        assertEquals(3, kpis.get("submittedCount"));
        assertEquals("a cycle with no planned end states no deadline to miss", 1, kpis.get("lateCount"));
        assertEquals(Integer.valueOf(67), kpis.get("onTimeRate"));
    }

    @Test
    public void coverageKeepsTheLastFourCyclesAndTheWorstVerdictPerCell() {
        EQAProgram scheme = insertScheme("Perf scheme matrix", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        for (int number = 1; number <= 5; number++) {
            EQACycle cycle = cycleStarting(scheme, number, LocalDate.now().minusMonths(6L - number));
            scored(cycle, number == 5 ? "QUESTIONABLE" : "ACCEPTABLE", LocalDate.now().minusMonths(6L - number));
            if (number == 5) {
                // Two analytes in one cell: the worse of the two is what the cell says.
                scored(cycle, "UNACCEPTABLE", LocalDate.now().minusMonths(1));
            }
        }

        List<Map<String, Object>> coverage = coverage();
        assertEquals(1, coverage.size());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cells = (List<Map<String, Object>>) coverage.get(0).get("cells");
        assertEquals("the matrix is four cycles wide", 4, cells.size());
        assertEquals("acceptable", cells.get(0).get("verdict"));
        assertEquals("unacceptable", cells.get(3).get("verdict"));
        assertEquals("three of four cells acceptable", Integer.valueOf(75), coverage.get(0).get("acceptanceRate"));
    }

    @Test
    public void aCycleWithNoScoredResultIsListedWithoutAVerdict() {
        EQAProgram scheme = insertScheme("Perf scheme pending", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQACycle cycle = cycleEnding(scheme, 1, LocalDate.now().minusMonths(1));
        submitted(cycle, LocalDate.now().minusMonths(1));

        List<Map<String, Object>> recent = recentCycles();
        assertEquals(1, recent.size());
        assertNull("nothing scored yet is pending, not acceptable", recent.get(0).get("performance"));
        assertEquals(0, recent.get(0).get("scoredCount"));
        assertTrue("coverage needs a verdict, so an unscored cycle contributes no row", coverage().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> kpis() {
        return (Map<String, Object>) labPerformanceService.getLabPerformance().get("kpis");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> coverage() {
        return (List<Map<String, Object>>) labPerformanceService.getLabPerformance().get("coverage");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> recentCycles() {
        return (List<Map<String, Object>>) labPerformanceService.getLabPerformance().get("recentCycles");
    }

    private EQACycle cycleEnding(EQAProgram scheme, int number, LocalDate end) {
        return insertCycleWithDates(scheme, number, end.minusMonths(1), end);
    }

    private EQACycle cycleStarting(EQAProgram scheme, int number, LocalDate start) {
        return insertCycleWithDates(scheme, number, start, start.plusMonths(1));
    }

    private EQACycle insertCycleWithDates(EQAProgram scheme, int number, LocalDate start, LocalDate end) {
        Long id = insertCycle(scheme, number);
        jdbc.update("UPDATE clinlims.eqa_cycle SET planned_start_date = ?, planned_end_date = ? WHERE id = ?",
                start == null ? null : java.sql.Date.valueOf(start), end == null ? null : java.sql.Date.valueOf(end),
                id);
        EQACycle cycle = readBack(id);
        Long roundId = insertRound(cycle, 1, "OPEN");
        roundsByCycle.put(id, eqaRoundDAO.get(roundId).orElseThrow(AssertionError::new));
        return cycle;
    }

    private long freshAnalyte() {
        return FIRST_ANALYTE + (nextAnalyte++ % ANALYTE_COUNT);
    }

    /** A submitted-but-unscored result, which is what the on-time rate reads. */
    private Long submitted(EQACycle cycle, LocalDate submittedOn) {
        Long id = insertParticipantResult(cycle, roundsByCycle.get(cycle.getId()), ENROLLMENT, freshAnalyte(),
                EQASubmissionStatus.SUBMITTED, "100");
        jdbc.update("UPDATE clinlims.eqa_participant_result SET submitted_at = ? WHERE id = ?",
                java.sql.Timestamp.valueOf(submittedOn.atStartOfDay()), id);
        return id;
    }

    private void scored(EQACycle cycle, String verdict, LocalDate scoredOn) {
        Long id = insertParticipantResult(cycle, roundsByCycle.get(cycle.getId()), ENROLLMENT, freshAnalyte(),
                EQASubmissionStatus.SCORED, "100");
        setScore(id, verdict, scoredOn);
    }

    private void setScore(Long resultId, String verdict, LocalDate scoredOn) {
        jdbc.update("UPDATE clinlims.eqa_participant_result SET performance_status = ?, score_received_at = ?"
                + " WHERE id = ?", verdict, java.sql.Timestamp.valueOf(scoredOn.atStartOfDay()), resultId);
    }
}
