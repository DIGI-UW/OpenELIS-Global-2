package org.openelisglobal.qaevent.criticalcallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qaevent.criticalcallback.bean.CallbackDetailResponse;
import org.openelisglobal.qaevent.criticalcallback.bean.CallbackEvent;
import org.openelisglobal.qaevent.criticalcallback.bean.CallbackSummaryResponse;
import org.openelisglobal.qaevent.criticalcallback.service.CriticalCallbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-714 read side — Critical Callback Compliance compute against a real DB.
 *
 * <p>
 * Window and clock anchor on {@code analysis.released_date}: the seed releases
 * four critical analyses inside the window (CONFIRMED within the 60-min SLA,
 * CONFIRMED late, UNABLE_TO_REACH, never logged), one non-critical analysis
 * inside the window, and one critical analysis released outside it — so the
 * expected summary is exactly 4 critical / 1 confirmed / 25.00%. Also covers
 * the disabled short-circuit (the qa/009 CALLBACK row flipped off), the
 * negative-delta rule (a call logged before release is compliant), the
 * repeat-POST attempt log (UNABLE then CONFIRMED counts once, detail shows the
 * latest attempt), and that the SLA window itself is configurable rather than
 * fixed at 60 minutes.
 */
public class CriticalCallbackServiceTest extends BaseWebContextSensitiveTest {

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);

    private static final String RELEASED = "2026-01-15 08:00:00";
    private static final String RELEASED_OUTSIDE = "2026-03-10 08:00:00";

    private static final long TEST_ID = 95441L;
    private static final long LIMIT_ID = 95441L;
    private static final long SAMPLE_ID = 95441L;
    private static final long SAMPLE_ITEM_ID = 95441L;

    private static final long ANALYSIS_IN_SLA = 95441L;
    private static final long ANALYSIS_LATE = 95442L;
    private static final long ANALYSIS_UNREACHED = 95443L;
    private static final long ANALYSIS_UNLOGGED = 95444L;
    private static final long ANALYSIS_NON_CRITICAL = 95445L;
    private static final long ANALYSIS_OUT_OF_WINDOW = 95446L;

    @Autowired
    private CriticalCallbackService callbackService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();

        // The base test harness truncates the shipped qi_config seed (and the
        // sibling QiConfigServiceIntegrationTest wipes the table), so self-seed
        // the CALLBACK default row — the INSERT itself proves qa/009's CHECK
        // swap accepts 'CALLBACK'.
        jdbc.update("DELETE FROM clinlims.qi_config WHERE indicator_key = 'CALLBACK'");
        jdbc.update("INSERT INTO clinlims.qi_config (id, indicator_key, is_enabled, target_threshold,"
                + " action_threshold, last_updated) VALUES (nextval('clinlims.qi_config_id_seq'), 'CALLBACK',"
                + " true, 100, 95, NOW())");

        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "CallbackComputeIT", "CallbackComputeIT desc", UUID.randomUUID().toString());
        // Default demographic row with a 10–90 critical band.
        jdbc.update(
                "INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age,"
                        + " low_critical, high_critical, lastupdated) VALUES (?, ?, 4, 0, ?, 10, 90, NOW())",
                LIMIT_ID, TEST_ID, Double.POSITIVE_INFINITY);
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date, is_confirmation,"
                + " lastupdated) VALUES (?, ?, NOW(), NOW(), false, NOW())", SAMPLE_ID, "CBCT" + SAMPLE_ID);
        jdbc.update("INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, lastupdated)"
                + " VALUES (?, ?, 1, 1, NOW())", SAMPLE_ITEM_ID, SAMPLE_ID);

        seedAnalysis(ANALYSIS_IN_SLA, RELEASED, "95");
        seedAnalysis(ANALYSIS_LATE, RELEASED, "96");
        seedAnalysis(ANALYSIS_UNREACHED, RELEASED, "97");
        seedAnalysis(ANALYSIS_UNLOGGED, RELEASED, "98");
        seedAnalysis(ANALYSIS_NON_CRITICAL, RELEASED, "50");
        seedAnalysis(ANALYSIS_OUT_OF_WINDOW, RELEASED_OUTSIDE, "99");

        seedCallback(ANALYSIS_IN_SLA, "CONFIRMED", "2026-01-15 08:30:00", "Dr. In-Sla");
        seedCallback(ANALYSIS_LATE, "CONFIRMED", "2026-01-15 10:30:00", "Dr. Late");
        seedCallback(ANALYSIS_UNREACHED, "UNABLE_TO_REACH", "2026-01-15 08:10:00", "Ward clerk");
        // the out-of-window analysis has an in-SLA CONFIRMED call — it must not
        // leak into the window's counts via the callback side of the join
        seedCallback(ANALYSIS_OUT_OF_WINDOW, "CONFIRMED", "2026-03-10 08:20:00", "Dr. Outside");
    }

    @After
    public void tearDown() {
        cleanup();
        jdbc.update("DELETE FROM clinlims.qi_config WHERE indicator_key = 'CALLBACK'");
    }

    private void seedAnalysis(long analysisId, String releasedAt, String value) {
        jdbc.update(
                "INSERT INTO clinlims.analysis (id, analysis_type, test_id, sampitem_id, released_date, lastupdated)"
                        + " VALUES (?, 'MANUAL', ?, ?, CAST(? AS timestamp), NOW())",
                analysisId, TEST_ID, SAMPLE_ITEM_ID, releasedAt);
        jdbc.update("INSERT INTO clinlims.result (id, analysis_id, value, result_type, lastupdated)"
                + " VALUES (?, ?, ?, 'N', NOW())", analysisId, analysisId, value);
    }

    private void seedCallback(long analysisId, String status, String loggedAt, String recipient) {
        jdbc.update(
                "INSERT INTO clinlims.critical_callback (id, result_id, analysis_id, result_value, logged_by,"
                        + " logged_at, recipient_name, status, last_updated)"
                        + " VALUES (?, ?, ?, 'seeded', 1, CAST(? AS timestamp), ?, ?, NOW())",
                UUID.randomUUID().toString(), analysisId, analysisId, loggedAt, recipient, status);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.critical_callback WHERE analysis_id BETWEEN ? AND ?", ANALYSIS_IN_SLA,
                ANALYSIS_OUT_OF_WINDOW);
        jdbc.update("DELETE FROM clinlims.result WHERE id BETWEEN ? AND ?", ANALYSIS_IN_SLA, ANALYSIS_OUT_OF_WINDOW);
        jdbc.update("DELETE FROM clinlims.analysis WHERE id BETWEEN ? AND ?", ANALYSIS_IN_SLA, ANALYSIS_OUT_OF_WINDOW);
        jdbc.update("DELETE FROM clinlims.sample_item WHERE id = ?", SAMPLE_ITEM_ID);
        jdbc.update("DELETE FROM clinlims.sample WHERE id = ?", SAMPLE_ID);
        jdbc.update("DELETE FROM clinlims.result_limits WHERE id = ?", LIMIT_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }

    @Test
    public void getSummary_countsCriticalsAndInSlaConfirmations() {
        CallbackSummaryResponse summary = callbackService.getSummary(FROM, TO);

        assertTrue(summary.isEnabled());
        assertEquals(4, summary.getCriticalCount());
        assertEquals(1, summary.getConfirmedCount());
        assertEquals(Double.valueOf(25.00), summary.getCompliancePercent());
        assertEquals(Double.valueOf(100.0), summary.getTarget());
        assertEquals(60, summary.getSlaMinutes());
    }

    @Test
    public void getSummary_slaWindowIsConfigurable() {
        // The "late" call at 150 min is non-compliant at the default 60-min target
        // and compliant at 180 — proving the numerator reads the configured window
        // instead of a hard-coded 60.
        CriticalCallbackService target = AopTestUtils.getTargetObject(callbackService);
        // Restore whatever was configured, not a literal — the field lives on a
        // context-cached singleton, so a wrong restore poisons every later test.
        Object configured = ReflectionTestUtils.getField(target, "slaMinutes");
        ReflectionTestUtils.setField(target, "slaMinutes", 180);
        try {
            CallbackSummaryResponse summary = callbackService.getSummary(FROM, TO);

            assertEquals(180, summary.getSlaMinutes());
            assertEquals(4, summary.getCriticalCount());
            assertEquals(2, summary.getConfirmedCount());
            assertEquals(Double.valueOf(50.00), summary.getCompliancePercent());

            // the same widened window moves the late call out of the failure table
            CallbackDetailResponse detail = callbackService.getDetail(FROM, TO, 0, 25);
            assertEquals(Long.valueOf(0), detail.getFailureCounts().get("overTarget"));
            // …but the latency histogram is absolute, so it still reads "over 60"
            assertEquals(Long.valueOf(1), detail.getAckDistribution().get("over60"));
        } finally {
            ReflectionTestUtils.setField(target, "slaMinutes", configured);
        }
    }

    @Test
    public void getSummary_disabled_shortCircuitsWithZeroCounts() {
        jdbc.update("UPDATE clinlims.qi_config SET is_enabled = false WHERE indicator_key = 'CALLBACK'"
                + " AND test_category_id IS NULL");

        CallbackSummaryResponse summary = callbackService.getSummary(FROM, TO);

        assertEquals(false, summary.isEnabled());
        assertEquals(0, summary.getCriticalCount());
        assertEquals(0, summary.getConfirmedCount());
        assertNull(summary.getCompliancePercent());
    }

    @Test
    public void getSummary_emptyWindow_hasNullPercent() {
        CallbackSummaryResponse summary = callbackService.getSummary(LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 6, 30));

        assertTrue(summary.isEnabled());
        assertEquals(0, summary.getCriticalCount());
        assertEquals(0, summary.getConfirmedCount());
        assertNull(summary.getCompliancePercent());
    }

    @Test
    public void getSummary_callLoggedBeforeRelease_isCompliant() {
        // negative delta: the callback happened at result entry, release came after
        jdbc.update("DELETE FROM clinlims.critical_callback WHERE analysis_id = ?", ANALYSIS_UNLOGGED);
        seedCallback(ANALYSIS_UNLOGGED, "CONFIRMED", "2026-01-15 06:00:00", "Dr. Early");

        CallbackSummaryResponse summary = callbackService.getSummary(FROM, TO);

        assertEquals(4, summary.getCriticalCount());
        assertEquals(2, summary.getConfirmedCount());
        assertEquals(Double.valueOf(50.00), summary.getCompliancePercent());
    }

    @Test
    public void getDetail_returnsExactRows_gapsFirst() {
        CallbackDetailResponse detail = callbackService.getDetail(FROM, TO, 0, 25);

        assertEquals(4, detail.getTotalCount());
        assertEquals(4, detail.getItems().size());
        assertEquals(0, detail.getPage());
        assertEquals(25, detail.getPageSize());

        // never-logged critical sorts first — it is the actionable gap
        CallbackEvent unlogged = detail.getItems().get(0);
        assertEquals(String.valueOf(ANALYSIS_UNLOGGED), unlogged.getAnalysisId());
        assertEquals("CBCT" + SAMPLE_ID, unlogged.getLabNumber());
        assertEquals("CallbackComputeIT", unlogged.getTestName());
        assertEquals("98", unlogged.getResultValue());
        assertEquals("≤ 10 / ≥ 90", unlogged.getCriticalRange());
        assertNull(unlogged.getStatus());
        assertNull(unlogged.getRecipientName());
        assertNull(unlogged.getLoggedAt());
        assertNull(unlogged.getMinutesToCallback());

        CallbackEvent inSla = itemFor(detail, ANALYSIS_IN_SLA);
        assertEquals("CONFIRMED", inSla.getStatus());
        assertEquals("Dr. In-Sla", inSla.getRecipientName());
        assertEquals("seeded", inSla.getResultValue());
        assertEquals(Long.valueOf(30), inSla.getMinutesToCallback());

        CallbackEvent late = itemFor(detail, ANALYSIS_LATE);
        assertEquals("CONFIRMED", late.getStatus());
        assertEquals(Long.valueOf(150), late.getMinutesToCallback());

        CallbackEvent unreached = itemFor(detail, ANALYSIS_UNREACHED);
        assertEquals("UNABLE_TO_REACH", unreached.getStatus());
        assertEquals(Long.valueOf(10), unreached.getMinutesToCallback());

        // window-wide aggregates for the design's histogram + failure table:
        // CONFIRMED at 30m → "30-60"; CONFIRMED at 150m → "over60";
        // UNABLE_TO_REACH + never-logged → "noAck"
        java.util.Map<String, Long> distribution = detail.getAckDistribution();
        assertEquals(Long.valueOf(0), distribution.get("0-5"));
        assertEquals(Long.valueOf(0), distribution.get("5-15"));
        assertEquals(Long.valueOf(0), distribution.get("15-30"));
        assertEquals(Long.valueOf(1), distribution.get("30-60"));
        assertEquals(Long.valueOf(1), distribution.get("over60"));
        assertEquals(Long.valueOf(2), distribution.get("noAck"));

        java.util.Map<String, Long> failures = detail.getFailureCounts();
        assertEquals(Long.valueOf(1), failures.get("overTarget"));
        assertEquals(Long.valueOf(1), failures.get("unableToReach"));
        assertEquals(Long.valueOf(0), failures.get("noReadback"));
        assertEquals(Long.valueOf(1), failures.get("noCallback"));
    }

    @Test
    public void getDetail_repeatAttempts_countOnceAndShowLatest() {
        // UNABLE at 08:10 then CONFIRMED at 08:40 — one denominator row, latest wins
        seedCallback(ANALYSIS_UNREACHED, "CONFIRMED", "2026-01-15 08:40:00", "Dr. Retry");

        CallbackSummaryResponse summary = callbackService.getSummary(FROM, TO);
        assertEquals(4, summary.getCriticalCount());
        assertEquals(2, summary.getConfirmedCount());
        assertEquals(Double.valueOf(50.00), summary.getCompliancePercent());

        CallbackDetailResponse detail = callbackService.getDetail(FROM, TO, 0, 25);
        assertEquals(4, detail.getTotalCount());
        CallbackEvent retried = itemFor(detail, ANALYSIS_UNREACHED);
        assertEquals("CONFIRMED", retried.getStatus());
        assertEquals("Dr. Retry", retried.getRecipientName());
        assertEquals(Long.valueOf(40), retried.getMinutesToCallback());
    }

    @Test
    public void getDetail_disabled_returnsEmpty() {
        jdbc.update("UPDATE clinlims.qi_config SET is_enabled = false WHERE indicator_key = 'CALLBACK'"
                + " AND test_category_id IS NULL");

        CallbackDetailResponse detail = callbackService.getDetail(FROM, TO, 0, 25);

        assertEquals(0, detail.getTotalCount());
        assertTrue(detail.getItems().isEmpty());
    }

    @Test
    public void getDetail_pagination_clampsToWindowRows() {
        CallbackDetailResponse firstPage = callbackService.getDetail(FROM, TO, 0, 3);
        assertEquals(4, firstPage.getTotalCount());
        assertEquals(3, firstPage.getItems().size());

        CallbackDetailResponse secondPage = callbackService.getDetail(FROM, TO, 1, 3);
        assertEquals(4, secondPage.getTotalCount());
        assertEquals(1, secondPage.getItems().size());
    }

    @Test
    public void getLoggedResultIds_returnsOnlyResultsWithCallbackRows() {
        java.util.List<String> logged = callbackService
                .getLoggedResultIds(java.util.List.of(String.valueOf(ANALYSIS_IN_SLA),
                        String.valueOf(ANALYSIS_UNLOGGED), String.valueOf(ANALYSIS_NON_CRITICAL)));

        assertEquals(java.util.List.of(String.valueOf(ANALYSIS_IN_SLA)), logged);
    }

    private static CallbackEvent itemFor(CallbackDetailResponse detail, long analysisId) {
        return detail.getItems().stream().filter(e -> String.valueOf(analysisId).equals(e.getAnalysisId())).findFirst()
                .orElseThrow(() -> new AssertionError("no detail row for analysis " + analysisId));
    }
}
