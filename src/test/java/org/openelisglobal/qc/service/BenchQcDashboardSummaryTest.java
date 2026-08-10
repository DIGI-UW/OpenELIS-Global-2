package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.dto.BenchQcSummaryRow;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-1147 FR-D1 — the bench QC listing that makes manual and RDT controls
 * visible to a supervisor.
 *
 * <p>
 * This exists as its own roll-up rather than a source filter on the instrument
 * tiles because those tiles are built from the distinct analyzers on QC results
 * ({@code QCDashboardServiceImpl.getAllInstrumentComplianceStatus}); a bench
 * control has no analyzer, so it can never appear there however the request is
 * filtered.
 */
public class BenchQcDashboardSummaryTest extends BaseWebContextSensitiveTest {

    private static final Timestamp WINDOW_START = Timestamp.valueOf("2025-06-10 00:00:00");
    private static final Timestamp WINDOW_END = Timestamp.valueOf("2025-06-11 00:00:00");
    private static final String TEST_ID = "6601";
    private static final String RDT_TEST_ID = "6602";
    private static final String LAB_UNIT = "701";
    private static final int TECHNICIAN = 7701;

    @Autowired
    private QCDashboardService dashboardService;
    @Autowired
    private QCResultService qcResultService;
    @Autowired
    private QCChartDataService chartDataService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/bench-qc-fail-signal.xml");
    }

    @Test
    public void groupsByLabUnitAndTestWithPassAndFailCounts() {
        recordManual(QCQualitativeOutcome.PASS, new BigDecimal("100.00000"), "2025-06-10 08:00:00");
        recordManual(QCQualitativeOutcome.PASS, new BigDecimal("101.00000"), "2025-06-10 09:00:00");
        recordManual(QCQualitativeOutcome.FAIL, new BigDecimal("140.00000"), "2025-06-10 12:00:00");
        recordRdt(QCQualitativeOutcome.INVALID, "2025-06-10 10:00:00");

        List<BenchQcSummaryRow> rows = dashboardService.getBenchQcSummary(WINDOW_START, WINDOW_END, null);

        assertEquals("one row per lab unit + test + source", 2, rows.size());

        BenchQcSummaryRow manual = row(rows, TEST_ID);
        assertEquals(QCSource.MANUAL.name(), manual.source());
        assertEquals(LAB_UNIT, manual.testSectionId());
        assertEquals(3L, manual.totalRuns());
        assertEquals(1L, manual.failedRuns());
        assertTrue("a failure must be countable for the UI to tag it", manual.failedRuns() > 0);
        // Latest run in the group, which is what a supervisor scans for staleness.
        assertEquals(Timestamp.valueOf("2025-06-10 12:00:00"), manual.lastRun());
        // Names are resolved so the row is readable without a second lookup.
        assertEquals("Bench Haemoglobin", manual.testName());

        BenchQcSummaryRow rdt = row(rows, RDT_TEST_ID);
        assertEquals(QCSource.RDT.name(), rdt.source());
        assertEquals(1L, rdt.totalRuns());
        assertEquals(1L, rdt.failedRuns());
        assertTrue("an Invalid RDT control must be visible here — it appears on no other QC surface",
                rdt.failedRuns() > 0);
    }

    @Test
    public void filtersToASingleSource() {
        recordManual(QCQualitativeOutcome.PASS, new BigDecimal("100.00000"), "2025-06-10 08:00:00");
        recordRdt(QCQualitativeOutcome.VALID, "2025-06-10 10:00:00");

        List<BenchQcSummaryRow> manualOnly = dashboardService.getBenchQcSummary(WINDOW_START, WINDOW_END,
                QCSource.MANUAL);
        assertEquals(Set.of(QCSource.MANUAL.name()),
                manualOnly.stream().map(BenchQcSummaryRow::source).collect(Collectors.toSet()));

        List<BenchQcSummaryRow> rdtOnly = dashboardService.getBenchQcSummary(WINDOW_START, WINDOW_END, QCSource.RDT);
        assertEquals(Set.of(QCSource.RDT.name()),
                rdtOnly.stream().map(BenchQcSummaryRow::source).collect(Collectors.toSet()));
    }

    @Test
    public void excludesAnalyzerResultsAndRunsOutsideTheWindow() {
        recordManual(QCQualitativeOutcome.PASS, new BigDecimal("100.00000"), "2025-06-09 08:00:00");

        // The seeded fixture also carries analyzer-sourced rows via other tests;
        // whatever
        // is present, this listing is bench-only and window-bound.
        List<BenchQcSummaryRow> rows = dashboardService.getBenchQcSummary(WINDOW_START, WINDOW_END, null);

        assertTrue("a run before the window must not appear", rows.isEmpty());
        assertFalse(rows.stream().anyMatch(r -> QCSource.ASTM.name().equals(r.source())));
    }

    @Test
    public void passingOnlyGroupIsReportedWithoutFailures() {
        recordManual(QCQualitativeOutcome.PASS, new BigDecimal("100.00000"), "2025-06-10 08:00:00");

        BenchQcSummaryRow manual = row(dashboardService.getBenchQcSummary(WINDOW_START, WINDOW_END, null), TEST_ID);
        assertEquals(1L, manual.totalRuns());
        assertEquals(0L, manual.failedRuns());
        assertFalse("a clean group must not read as a failure", manual.failedRuns() > 0);
    }

    /**
     * FR-D2 end-to-end: a manual quantitative control reaches the Levey-Jennings
     * chart's own data path, not merely "has a z-score". The chart queries by
     * control lot with no analyzer filter, which is why this works without touching
     * the charting code — but that is a property worth asserting rather than
     * assuming.
     */
    @Test
    public void manualQuantitativeControlAppearsInChartDataForItsLot() {
        recordManual(QCQualitativeOutcome.PASS, new BigDecimal("110.00000"), "2025-06-10 08:00:00");

        List<org.openelisglobal.qc.valueholder.QCResult> plotted = chartDataService
                .getResultsByControlLotAndDateRange("bench-lot-a", WINDOW_START, WINDOW_END);

        assertEquals(1, plotted.size());
        assertEquals(QCSource.MANUAL, plotted.get(0).getSource());
        // Non-null z-score is what LeveyJenningsChart filters on before plotting.
        assertEquals(0, new BigDecimal("2.0000").compareTo(plotted.get(0).getZScore()));
    }

    /**
     * The other half of FR-D2/D3: an RDT control must never reach the chart. It has
     * no lot and no number, so there is nothing to plot and no statistics to plot
     * it against.
     */
    @Test
    public void rdtControlNeverReachesChartData() {
        recordRdt(QCQualitativeOutcome.INVALID, "2025-06-10 10:00:00");

        assertTrue(
                chartDataService.getResultsByControlLotAndDateRange("bench-lot-a", WINDOW_START, WINDOW_END).isEmpty());
    }

    private BenchQcSummaryRow row(List<BenchQcSummaryRow> rows, String testId) {
        return rows.stream().filter(r -> testId.equals(r.testId())).findFirst()
                .orElseThrow(() -> new AssertionError("no bench row for test " + testId + " in " + rows.size()));
    }

    private void recordManual(QCQualitativeOutcome outcome, BigDecimal value, String runAt) {
        BenchQCCaptureForm form = new BenchQCCaptureForm();
        form.setSource(QCSource.MANUAL);
        form.setTestId(TEST_ID);
        form.setTestSectionId(LAB_UNIT);
        form.setControlLotId("bench-lot-a");
        form.setQualitativeOutcome(outcome);
        form.setResultValue(value);
        form.setExpectedValue(new BigDecimal("100.00000"));
        form.setUncertainty(new BigDecimal("5.00000"));
        form.setRunDateTime(Timestamp.valueOf(runAt).toLocalDateTime());
        qcResultService.createBenchQCResult(form, TECHNICIAN);
    }

    private void recordRdt(QCQualitativeOutcome outcome, String runAt) {
        BenchQCCaptureForm form = new BenchQCCaptureForm();
        form.setSource(QCSource.RDT);
        form.setTestId(RDT_TEST_ID);
        form.setTestSectionId(LAB_UNIT);
        form.setControlLabel("Malaria RDT · LOT-BENCH-1");
        form.setQualitativeOutcome(outcome);
        form.setRunDateTime(Timestamp.valueOf(runAt).toLocalDateTime());
        qcResultService.createBenchQCResult(form, TECHNICIAN);
    }
}
