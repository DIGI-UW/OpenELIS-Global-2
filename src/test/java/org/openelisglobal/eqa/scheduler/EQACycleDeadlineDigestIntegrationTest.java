package org.openelisglobal.eqa.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.eqa.EQASpineTestBase;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-610 [EQA V2.2 / T-16] — the 7/3/1-day cycle deadline digest (FR-V2.2-14),
 * against a real DB with a pinned clock.
 *
 * <p>
 * The clock is pinned so "7 days out" is a fact of the fixture, not of when CI
 * happens to run. The digest's own dedupe (one alert per cycle+threshold, ever)
 * is what these tests exercise — createAlert's built-in 30-minute window would
 * mask a missing guard by folding duplicates into duplicate_count, so the rerun
 * test asserts BOTH the row count and duplicate_count stay flat.
 */
public class EQACycleDeadlineDigestIntegrationTest extends EQASpineTestBase {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 1);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private AlertService alertService;

    /**
     * Built by hand: AppTestConfig excludes eqa.scheduler.* from scanning so
     * that @Scheduled jobs cannot fire mid-test. The collaborators are still the
     * context's transactional proxies, so the digest runs here exactly as it does
     * in production — a non-transactional caller of transactional beans.
     */
    private EQADeadlineAlertScheduler scheduler;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        scheduler = new EQADeadlineAlertScheduler();
        ReflectionTestUtils.setField(scheduler, "alertService", alertService);
        ReflectionTestUtils.setField(scheduler, "eqaRoundDAO", eqaRoundDAO);
        scheduler.setClock(Clock.fixed(Instant.parse("2026-09-01T08:00:00Z"), ZoneId.of("UTC")));
        cleanAlerts();
    }

    @After
    public void tearDownDigest() {
        cleanAlerts();
    }

    private void cleanAlerts() {
        jdbc.update("DELETE FROM clinlims.alert WHERE alert_entity_type = 'EQARound'");
    }

    private long deadlineDaysOut(EQACycle cycle, int roundNumber, int daysOut) {
        Long roundId = insertRound(readBack(cycle.getId()), roundNumber, "OPEN");
        jdbc.update("UPDATE clinlims.eqa_round SET submission_deadline = ? WHERE id = ?",
                Timestamp.valueOf(TODAY.plusDays(daysOut).atStartOfDay().plusHours(17)), roundId);
        return roundId;
    }

    private long thresholdOf(Alert a) {
        try {
            return MAPPER.readTree(a.getContextData()).path("thresholdDays").asLong();
        } catch (Exception e) {
            return -999;
        }
    }

    private List<Alert> digestAlerts() {
        return alertService.getAllMatching(Map.of("alertType", AlertType.EQA_DEADLINE, "alertEntityType", "EQARound"));
    }

    @Test
    public void firesExactlyOncePerThresholdAndNeverAgain() {
        EQAProgram scheme = insertScheme("Digest scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        deadlineDaysOut(cycle, 1, 7);
        deadlineDaysOut(cycle, 2, 3);
        deadlineDaysOut(cycle, 3, 1);
        deadlineDaysOut(cycle, 4, 5); // not a threshold — must stay silent
        deadlineDaysOut(cycle, 5, -1); // already past — the digest is not an overdue alarm

        scheduler.sendCycleDeadlineDigest();

        List<Alert> alerts = digestAlerts();
        assertEquals("one alert per threshold, nothing for day 5 or the past", 3, alerts.size());
        for (long d : new long[] { 7, 3, 1 }) {
            assertEquals("exactly one alert carries thresholdDays " + d, 1,
                    alerts.stream().filter(a -> thresholdOf(a) == d).count());
        }
        Alert dayOne = alerts.stream().filter(a -> thresholdOf(a) == 1).findFirst().orElseThrow(AssertionError::new);
        assertEquals("the last-day reminder escalates", AlertSeverity.CRITICAL, dayOne.getSeverity());
        assertEquals("earlier reminders warn", 2,
                alerts.stream().filter(a -> a.getSeverity() == AlertSeverity.WARNING).count());
        assertTrue("message names the scheme and round",
                dayOne.getMessage().contains("Digest scheme") && dayOne.getMessage().contains("round 3"));

        // Rerun the tick: the digest's own dedupe must skip BEFORE createAlert, so
        // neither new rows nor duplicate_count may move.
        scheduler.sendCycleDeadlineDigest();
        List<Alert> after = digestAlerts();
        assertEquals("rerun creates nothing", 3, after.size());
        assertEquals("rerun does not even count a duplicate", 0,
                after.stream().mapToInt(Alert::getDuplicateCount).sum());
    }

    @Test
    public void cyclesNoLongerAwaitingSubmissionAreSilent() {
        EQAProgram scheme = insertScheme("Submitted scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        deadlineDaysOut(cycle, 1, 7);
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'SUBMITTED' WHERE id = ?", cycle.getId());

        scheduler.sendCycleDeadlineDigest();

        assertEquals("a submitted cycle needs no reminder", 0, digestAlerts().size());
    }

    @Test
    public void eachCycleDedupesIndependently() {
        EQAProgram scheme = insertScheme("Two-cycle scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQACycle first = readBack(insertCycle(scheme, 1));
        EQACycle second = readBack(insertCycle(scheme, 2));
        deadlineDaysOut(first, 1, 3);
        deadlineDaysOut(second, 1, 3);

        scheduler.sendCycleDeadlineDigest();

        List<Alert> alerts = digestAlerts();
        assertEquals("same threshold on two cycles is two alerts", 2, alerts.size());
        assertEquals(2, alerts.stream().map(Alert::getAlertEntityId).distinct().count());
    }
}
