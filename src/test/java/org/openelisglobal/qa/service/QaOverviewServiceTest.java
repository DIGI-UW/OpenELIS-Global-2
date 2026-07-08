package org.openelisglobal.qa.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qa.dto.QaOverviewSummary;
import org.openelisglobal.qa.dto.QaOverviewSummary.ActivityItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for the QA Overview aggregation service (OGC-694 WS-F).
 *
 * <p>
 * QC fixture rows (instruments, results, violations) come from
 * testdata/qc-dashboard.xml — the same dataset QCDashboardServiceTest verifies
 * — then violation timestamps are re-pinned relative to "now" so the 24h/week
 * windows are deterministic: viol-001 and viol-002 land seconds ago (inside
 * both windows), everything else lands before the week started. The
 * electronic_signature / history / sample_eqa tables are cleaned through
 * qa-overview-clean.xml (the managed truncate path — the base class runs
 * non-transactionally, so ad-hoc DELETEs would leak into later tests) and
 * seeded directly.
 */
public class QaOverviewServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QaOverviewService qaOverviewService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/qc-dashboard.xml");
        executeDataSetWithStateManagement("testdata/qa-overview-clean.xml");
        jdbc = new JdbcTemplate(dataSource);
        seedTimeline();
    }

    private Timestamp secondsAgo(int seconds) {
        return Timestamp.from(Instant.now().minus(seconds, ChronoUnit.SECONDS));
    }

    private Instant weekStartInstant() {
        return LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Timestamp beforeWeekStart() {
        return Timestamp.from(weekStartInstant().minus(2, ChronoUnit.DAYS));
    }

    private void seedTimeline() {
        // All dataset violations out of every window, then pin two just-now
        jdbc.update("UPDATE qc_rule_violation SET violation_date_time = ?", beforeWeekStart());
        jdbc.update("UPDATE qc_rule_violation SET violation_date_time = ? WHERE id = 'viol-001'", secondsAgo(2));
        jdbc.update("UPDATE qc_rule_violation SET violation_date_time = ? WHERE id = 'viol-002'", secondsAgo(6));

        String esigInsert = "INSERT INTO electronic_signature (id, signer_id, signer_name_printed,"
                + " signature_meaning, signed_at, record_type, record_id, session_signing_sequence, auth_method)"
                + " VALUES (nextval('electronic_signature_seq'), 1, ?, ?, ?, 'RESULT', ?, 1, 'LOCAL')";
        jdbc.update(esigInsert, "Alice Tech", "AUTHORED", secondsAgo(4), 101L);
        jdbc.update(esigInsert, "Bob Supervisor", "VALIDATED_AND_RELEASED", secondsAgo(8), 102L);
        jdbc.update(esigInsert, "Old Signer", "AUTHORED", beforeWeekStart(), 103L);

        String historyInsert = "INSERT INTO history (id, reference_id, reference_table, timestamp, activity,"
                + " sys_user_id) VALUES (?, 1, 1, ?, 'U', 1)";
        jdbc.update(historyInsert, 9900001L, secondsAgo(3));
        jdbc.update(historyInsert, 9900002L, secondsAgo(5));
        jdbc.update(historyInsert, 9900003L, beforeWeekStart());
    }

    /**
     * The in-window rows are seeded 2-8 seconds in the past; within the first
     * minute after the weekly Monday-00:00 boundary they would fall into last week.
     * Skip rather than flake in that one-minute weekly window.
     */
    private void assumeNotAtWeekBoundary() {
        Assume.assumeTrue("within 60s of the week boundary — in-week seeds would straddle it",
                Instant.now().isAfter(weekStartInstant().plusSeconds(60)));
    }

    @Test
    public void getSummary_countsThisWeeksViolationsAuditAndSignatures() {
        assumeNotAtWeekBoundary();
        QaOverviewSummary summary = qaOverviewService.getSummary();

        assertEquals(LocalDate.now().with(DayOfWeek.MONDAY).toString(), summary.week.weekStart);
        assertEquals(weekStartInstant().toString(), summary.week.weekStartInstant);

        assertEquals(2, summary.qc.violations24h);
        assertEquals(2, summary.qc.violationsThisWeek);
        assertEquals(Long.valueOf(1), summary.qc.weekRuleBreakdown.get("1_3s"));
        assertEquals(Long.valueOf(1), summary.qc.weekRuleBreakdown.get("2_2s"));

        assertEquals(2, summary.week.auditEntries);
        assertEquals(2, summary.week.signatureEvents);
    }

    @Test
    public void getSummary_instrumentRollupIsInternallyConsistent() {
        QaOverviewSummary summary = qaOverviewService.getSummary();

        // Per-instrument compliance truth is QCDashboardServiceTest's concern;
        // here we only require the rollup to add up and see the unresolved
        // rejection instruments from the dataset.
        assertEquals(summary.qc.totalInstruments,
                summary.qc.compliantInstruments + summary.qc.warningInstruments + summary.qc.nonCompliantInstruments);
        assertTrue(summary.qc.nonCompliantInstruments >= 1);
    }

    @Test
    public void getSummary_emptyEqaYieldsZeroCounts() {
        QaOverviewSummary summary = qaOverviewService.getSummary();

        assertEquals(0, summary.eqa.open);
        assertEquals(0, summary.eqa.overdue);
        assertEquals(0, summary.eqa.dueSoon14d);
    }

    @Test
    public void getSummary_activityMergesSignaturesAndQcAlertsNewestFirst() {
        assumeNotAtWeekBoundary();
        QaOverviewSummary summary = qaOverviewService.getSummary();

        List<ActivityItem> activity = summary.activity;
        assertEquals(4, activity.size());
        for (int i = 1; i < activity.size(); i++) {
            // Instant.parse, not string compare: Instant.toString() emits
            // variable fractional precision.
            assertTrue("activity must be newest-first",
                    !Instant.parse(activity.get(i - 1).timestamp).isBefore(Instant.parse(activity.get(i).timestamp)));
        }

        List<ActivityItem> esig = activity.stream().filter(a -> ActivityItem.TYPE_ESIG.equals(a.type))
                .collect(Collectors.toList());
        assertEquals(2, esig.size());
        assertEquals("Alice Tech", esig.get(0).actor);
        assertEquals("AUTHORED", esig.get(0).meaning);
        assertEquals("RESULT", esig.get(0).recordType);
        assertEquals(Long.valueOf(101), esig.get(0).recordId);

        List<ActivityItem> qcAlerts = activity.stream().filter(a -> ActivityItem.TYPE_QC_VIOLATION.equals(a.type))
                .collect(Collectors.toList());
        assertEquals(2, qcAlerts.size());
        assertEquals("1_3s", qcAlerts.get(0).ruleCode);
        assertEquals("REJECTION", qcAlerts.get(0).severity);
        assertNotNull(qcAlerts.get(0).instrumentName);
    }
}
