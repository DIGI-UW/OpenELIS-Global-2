package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.dto.AnalyteDetail;
import org.openelisglobal.qc.dto.InstrumentQCStatus;
import org.openelisglobal.qc.dto.QCDashboardSummary;
import org.openelisglobal.qc.dto.TriggeredRuleDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for QCDashboardService.
 *
 * <p>
 * Test data loaded via DBUnit from testdata/qc-dashboard.xml:
 * <ul>
 * <li>Instrument 100: Chemistry Analyzer A (type=Clinical Chemistry,
 * location=Core Lab - Room 101), 1 REJECTION + 1 WARNING violation</li>
 * <li>Instrument 200: Standalone Hematology (no analyzer type,
 * location=Hematology Wing), 1 WARNING violation</li>
 * <li>Instrument 300: Resolved Only Analyzer, only RESOLVED violations (should
 * not appear)</li>
 * <li>Instrument 400: Clean Immunoassay Analyzer, has QC results but NO
 * violations (should appear as GREEN)</li>
 * <li>Instrument 500: Coagulation Analyzer, 4 QC results across 2 tests, NO
 * violations, 2 active control lots (GREEN)</li>
 * <li>Instrument 600: Electrolyte Analyzer, 4 QC results across 2 tests, 1
 * REJECTION + 1 WARNING violation, 1 active control lot (RED)</li>
 * <li>Resolved violation on instrument 100 (should not be counted)</li>
 * </ul>
 */
public class QCDashboardServiceTest extends BaseWebContextSensitiveTest {

    /**
     * The test data XML uses dates anchored around 2026-02-20. After loading,
     * setUp() shifts ALL timestamps forward so they land within 5 days of "now".
     * This keeps the default 30-day service window working regardless of when the
     * tests run.
     */
    private static final String DATA_ANCHOR = "2026-02-20 12:00:00";

    @Autowired
    private QCDashboardService dashboardService;

    @Autowired
    private DataSource dataSource;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/qc-dashboard.xml");
        rebaseTimestampsToNow();
    }

    /**
     * Shift every timestamp in the QC test tables by the delta between the
     * hardcoded anchor date and "now minus 2 days". This preserves relative
     * ordering while ensuring all dates fall within the service's default 30-day
     * window.
     */
    private void rebaseTimestampsToNow() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String interval = "NOW() - INTERVAL '2 days' - TIMESTAMP '" + DATA_ANCHOR + "'";

        jdbc.execute("UPDATE qc_result SET run_date_time = run_date_time + (" + interval + ")");
        jdbc.execute("UPDATE qc_rule_violation SET violation_date_time = violation_date_time + (" + interval + ")");
        jdbc.execute("UPDATE qc_rule_violation SET resolved_date_time = resolved_date_time + (" + interval
                + ") WHERE resolved_date_time IS NOT NULL");
    }

    // ==================== getAllInstrumentComplianceStatus ====================

    @Test
    public void getAllInstrumentComplianceStatus_returnsInstrumentsWithResultsOrViolations() {
        List<InstrumentQCStatus> statuses = dashboardService.getAllInstrumentComplianceStatus();

        // 5 instruments: 100 (RED), 600 (RED), 200 (YELLOW), 400 (GREEN), 500 (GREEN)
        // Instrument 300 has only resolved violations and no QC results → excluded
        assertEquals(5, statuses.size());
    }

    @Test
    public void getAllInstrumentComplianceStatus_sortedByColorPriority_redFirst() {
        List<InstrumentQCStatus> statuses = dashboardService.getAllInstrumentComplianceStatus();

        // RED instruments first (sorted by ID), then YELLOW, then GREEN
        assertEquals("RED", statuses.get(0).getComplianceColor());
        assertEquals("100", statuses.get(0).getInstrumentId());
        assertEquals("RED", statuses.get(1).getComplianceColor());
        assertEquals("600", statuses.get(1).getInstrumentId());
        assertEquals("YELLOW", statuses.get(2).getComplianceColor());
        assertEquals("200", statuses.get(2).getInstrumentId());
    }

    @Test
    public void getAllInstrumentComplianceStatus_aggregatesViolationCounts() {
        List<InstrumentQCStatus> statuses = dashboardService.getAllInstrumentComplianceStatus();

        InstrumentQCStatus instrument100 = statuses.stream().filter(s -> "100".equals(s.getInstrumentId())).findFirst()
                .orElse(null);
        assertNotNull(instrument100);
        assertEquals(1, instrument100.getUnresolvedRejections());
        assertEquals(1, instrument100.getUnresolvedWarnings());

        InstrumentQCStatus instrument200 = statuses.stream().filter(s -> "200".equals(s.getInstrumentId())).findFirst()
                .orElse(null);
        assertNotNull(instrument200);
        assertEquals(0, instrument200.getUnresolvedRejections());
        assertEquals(1, instrument200.getUnresolvedWarnings());
    }

    @Test
    public void getAllInstrumentComplianceStatus_excludesResolvedViolations() {
        List<InstrumentQCStatus> statuses = dashboardService.getAllInstrumentComplianceStatus();

        // Instrument 300 has ONLY resolved violations — must not appear at all
        Set<String> instrumentIds = statuses.stream().map(InstrumentQCStatus::getInstrumentId)
                .collect(Collectors.toSet());
        assertTrue("Instrument 300 (resolved-only) should not appear", !instrumentIds.contains("300"));

        // viol-004 is RESOLVED on instrument 100 — should not inflate rejection count
        assertEquals(5, statuses.size());
    }

    // ==================== Instrument Metadata ====================

    @Test
    public void getInstrumentComplianceStatus_withAnalyzerType_setsNameTypeLocation() {
        InstrumentQCStatus instrument100 = dashboardService.getInstrumentComplianceStatus("100");

        assertEquals("Chemistry Analyzer A", instrument100.getInstrumentName());
        assertEquals("Clinical Chemistry", instrument100.getInstrumentType());
        assertEquals("Core Lab - Room 101", instrument100.getInstrumentLocation());
    }

    @Test
    public void getInstrumentComplianceStatus_withoutAnalyzerType_setsNameAndLocation() {
        InstrumentQCStatus instrument200 = dashboardService.getInstrumentComplianceStatus("200");

        assertEquals("Standalone Hematology", instrument200.getInstrumentName());
        assertEquals("Hematology Wing", instrument200.getInstrumentLocation());
        // Analyzer 200 has no analyzer_type_id and no analyzer_type column — type
        // should be null
        assertNull(instrument200.getInstrumentType());
    }

    // ==================== Triggered Rule Details ====================

    @Test
    public void getInstrumentComplianceStatus_populatesTriggeredRuleDetails() {
        InstrumentQCStatus instrument100 = dashboardService.getInstrumentComplianceStatus("100");
        List<TriggeredRuleDetail> details = instrument100.getTriggeredRuleDetails();
        assertNotNull(details);
        assertEquals(2, details.size());

        boolean found1_3s = false;
        boolean found2_2s = false;
        for (TriggeredRuleDetail detail : details) {
            if ("1_3s".equals(detail.getRuleCode())) {
                assertEquals("REJECTION", detail.getSeverity());
                found1_3s = true;
            }
            if ("2_2s".equals(detail.getRuleCode())) {
                assertEquals("WARNING", detail.getSeverity());
                found2_2s = true;
            }
        }
        assertTrue("Should contain 1_3s rule", found1_3s);
        assertTrue("Should contain 2_2s rule", found2_2s);
    }

    @Test
    public void getInstrumentComplianceStatus_triggeredRulesListMatchesDetails() {
        InstrumentQCStatus instrument100 = dashboardService.getInstrumentComplianceStatus("100");
        assertEquals(instrument100.getTriggeredRuleDetails().size(), instrument100.getTriggeredRules().size());
        assertTrue(instrument100.getTriggeredRules().contains("1_3s"));
        assertTrue(instrument100.getTriggeredRules().contains("2_2s"));
    }

    // ==================== Analyte Details ====================

    @Test
    public void getInstrumentComplianceStatus_populatesAnalyteDetailsFromViolatedTests() {
        InstrumentQCStatus instrument100 = dashboardService.getInstrumentComplianceStatus("100");

        List<AnalyteDetail> analyteDetails = instrument100.getAnalyteDetails();
        assertNotNull(analyteDetails);
        // Instrument 100 has QC results for test 1 and test 2
        assertEquals(2, analyteDetails.size());

        Set<String> testIds = analyteDetails.stream().map(AnalyteDetail::getTestId).collect(Collectors.toSet());
        assertTrue("Should contain test ID 1 (Glucose)", testIds.contains("1"));
        assertTrue("Should contain test ID 2 (Cholesterol)", testIds.contains("2"));
    }

    @Test
    public void getInstrumentComplianceStatus_analyteDetailsContainZScores() {
        InstrumentQCStatus instrument100 = dashboardService.getInstrumentComplianceStatus("100");

        List<AnalyteDetail> analyteDetails = instrument100.getAnalyteDetails();

        boolean foundGlucose = false;
        boolean foundCholesterol = false;
        for (AnalyteDetail detail : analyteDetails) {
            if ("1".equals(detail.getTestId())) {
                assertEquals("Fasting Blood Glucose", detail.getTestName());
                assertEquals(0, new BigDecimal("4.2000").compareTo(detail.getLatestZScore()));
                // Verify lastRunTime is a valid ISO instant (not just non-null)
                Instant glucoseRunTime = Instant.parse(detail.getLastRunTime());
                assertNotNull(glucoseRunTime);
                foundGlucose = true;
            }
            if ("2".equals(detail.getTestId())) {
                assertEquals("Total Cholesterol", detail.getTestName());
                assertEquals(0, new BigDecimal("3.0000").compareTo(detail.getLatestZScore()));
                Instant cholesterolRunTime = Instant.parse(detail.getLastRunTime());
                assertNotNull(cholesterolRunTime);
                foundCholesterol = true;
            }
        }
        assertTrue("Should contain test 1 analyte detail", foundGlucose);
        assertTrue("Should contain test 2 analyte detail", foundCholesterol);
    }

    // ==================== Single Instrument ====================

    @Test
    public void getInstrumentComplianceStatus_singleInstrument_returnsCorrectStatus() {
        InstrumentQCStatus status = dashboardService.getInstrumentComplianceStatus("100");

        assertNotNull(status);
        assertEquals("100", status.getInstrumentId());
        assertEquals("RED", status.getComplianceColor());
        assertEquals(1, status.getUnresolvedRejections());
        assertEquals(1, status.getUnresolvedWarnings());
    }

    @Test
    public void getInstrumentComplianceStatus_instrumentWithNoViolations_returnsGreen() {
        // Instrument 999 doesn't exist in test data — no violations
        InstrumentQCStatus status = dashboardService.getInstrumentComplianceStatus("999");

        assertNotNull(status);
        assertEquals("GREEN", status.getComplianceColor());
        assertEquals(0, status.getUnresolvedRejections());
        assertEquals(0, status.getUnresolvedWarnings());
    }

    // ==================== Instruments with QC results but no violations
    // ====================

    @Test
    public void getAllInstrumentComplianceStatus_includesInstrumentsWithQCResultsButNoViolations() {
        List<InstrumentQCStatus> statuses = dashboardService.getAllInstrumentComplianceStatus();

        // Instrument 400 has QC results but zero violations — should appear as GREEN
        Set<String> instrumentIds = statuses.stream().map(InstrumentQCStatus::getInstrumentId)
                .collect(Collectors.toSet());
        assertTrue("Instrument 400 (QC results, no violations) should appear in dashboard",
                instrumentIds.contains("400"));
    }

    @Test
    public void getAllInstrumentComplianceStatus_instrumentWithNoViolations_isGreen() {
        List<InstrumentQCStatus> statuses = dashboardService.getAllInstrumentComplianceStatus();

        InstrumentQCStatus instrument400 = statuses.stream().filter(s -> "400".equals(s.getInstrumentId())).findFirst()
                .orElse(null);
        assertNotNull("Instrument 400 should be present", instrument400);
        assertEquals("GREEN", instrument400.getComplianceColor());
        assertEquals(0, instrument400.getUnresolvedRejections());
        assertEquals(0, instrument400.getUnresolvedWarnings());
        assertEquals("Clean Immunoassay Analyzer", instrument400.getInstrumentName());
        assertEquals("Immunology Lab", instrument400.getInstrumentLocation());
    }

    // ==================== analyteDetails & activeControlLots population
    // ====================

    @Test
    public void getAllInstrumentComplianceStatus_instrumentWithNoViolations_populatesAnalyteDetailsFromResults() {
        List<InstrumentQCStatus> statuses = dashboardService.getAllInstrumentComplianceStatus();

        // Instrument 400 has QC result qr-003 (test 1, z-score 0.3) but no violations
        InstrumentQCStatus instrument400 = statuses.stream().filter(s -> "400".equals(s.getInstrumentId())).findFirst()
                .orElse(null);
        assertNotNull("Instrument 400 should be present", instrument400);

        // analyteDetails should be populated from QC results, not just violations
        List<AnalyteDetail> analyteDetails = instrument400.getAnalyteDetails();
        assertNotNull(analyteDetails);
        assertEquals("Instrument 400 has QC results for test 1 — analyteDetails should contain it", 1,
                analyteDetails.size());
        assertEquals("1", analyteDetails.get(0).getTestId());
        assertEquals(0, new java.math.BigDecimal("0.3000").compareTo(analyteDetails.get(0).getLatestZScore()));
    }

    @Test
    public void getAllInstrumentComplianceStatus_populatesActiveControlLotCount() {
        List<InstrumentQCStatus> statuses = dashboardService.getAllInstrumentComplianceStatus();

        // Instrument 100 has lot-001 (ACTIVE) → activeControlLots should be 1
        InstrumentQCStatus instrument100 = statuses.stream().filter(s -> "100".equals(s.getInstrumentId())).findFirst()
                .orElse(null);
        assertNotNull(instrument100);
        assertEquals("Instrument 100 has 1 active control lot", 1, instrument100.getActiveControlLots());

        // Instrument 400 shares lot-001 but its QC result references instrument 400
        // — activeControlLots depends on lots assigned to this instrument
        InstrumentQCStatus instrument400 = statuses.stream().filter(s -> "400".equals(s.getInstrumentId())).findFirst()
                .orElse(null);
        assertNotNull(instrument400);
        assertEquals("GREEN", instrument400.getComplianceColor());
    }

    // ==================== Dashboard Summary ====================

    @Test
    public void getDashboardSummary_aggregatesAllInstruments() {
        QCDashboardSummary summary = dashboardService.getDashboardSummary();

        assertNotNull(summary);
        // 5 instruments: 100 (RED), 600 (RED), 200 (YELLOW), 400 (GREEN), 500 (GREEN)
        assertEquals(5, summary.getTotalInstruments());
        assertEquals(2, summary.getCompliantInstruments());
        assertEquals(1, summary.getWarningInstruments());
        assertEquals(2, summary.getNonCompliantInstruments());
        assertEquals(2, summary.getTotalRejections());
        assertEquals(3, summary.getTotalWarnings());
        assertEquals(5, summary.getTotalUnresolvedViolations());
        // Verify lastUpdateTime is a valid ISO instant, not just non-null
        Instant lastUpdate = Instant.parse(summary.getLastUpdateTime());
        assertNotNull(lastUpdate);
    }

    @Test
    public void getDashboardSummary_lastUpdateTimeIsISOFormat() {
        QCDashboardSummary summary = dashboardService.getDashboardSummary();

        // Must be parseable as a valid ISO-8601 instant (e.g. "2026-02-20T12:00:00Z")
        Instant parsed = Instant.parse(summary.getLastUpdateTime());
        assertNotNull(parsed);
    }

    // ==================== Date-range scoping ====================

    @Test
    public void getAllInstrumentComplianceStatus_withDateRange_excludesResultsOutsideWindow() {
        // Use a narrow window that ends well before "now" so no rebased QC results
        // fall within it (data is anchored around now-2d after rebase)
        Instant farPast = Instant.now().minus(365, ChronoUnit.DAYS);
        Timestamp startDate = Timestamp.from(farPast);
        Timestamp endDate = Timestamp.from(farPast.plus(30, ChronoUnit.DAYS));

        List<InstrumentQCStatus> statuses = dashboardService.getAllInstrumentComplianceStatus(startDate, endDate);

        // No QC results in Jan 2025, but unresolved violations still determine
        // compliance — instruments with unresolved violations should still appear
        // Instruments 100 and 200 have unresolved violations → appear with empty
        // analyteDetails
        // Instrument 400 has no violations and no results in this window → excluded
        for (InstrumentQCStatus status : statuses) {
            assertTrue("analyteDetails should be empty outside date window", status.getAnalyteDetails().isEmpty());
            // Compliance color still reflects full unresolved state
            assertTrue("Compliance color should still reflect unresolved violations",
                    "RED".equals(status.getComplianceColor()) || "YELLOW".equals(status.getComplianceColor()));
        }
    }

    @Test
    public void getAllInstrumentComplianceStatus_complianceColorReflectsAllUnresolved_notJustDateWindow() {
        // Use a window that includes the rebased test data (anchored around now-2d)
        Instant start = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant end = Instant.now().plus(1, ChronoUnit.DAYS);
        Timestamp startDate = Timestamp.from(start);
        Timestamp endDate = Timestamp.from(end);

        List<InstrumentQCStatus> statuses = dashboardService.getAllInstrumentComplianceStatus(startDate, endDate);

        // Instrument 100: has unresolved REJECTION → RED regardless of date window
        InstrumentQCStatus instrument100 = statuses.stream().filter(s -> "100".equals(s.getInstrumentId())).findFirst()
                .orElse(null);
        assertNotNull(instrument100);
        assertEquals("RED", instrument100.getComplianceColor());
        assertEquals(1, instrument100.getUnresolvedRejections());
        assertEquals(1, instrument100.getUnresolvedWarnings());

        // Instrument 400: no violations → GREEN, but has results in window
        InstrumentQCStatus instrument400 = statuses.stream().filter(s -> "400".equals(s.getInstrumentId())).findFirst()
                .orElse(null);
        assertNotNull(instrument400);
        assertEquals("GREEN", instrument400.getComplianceColor());
        assertEquals(1, instrument400.getAnalyteDetails().size());
    }

    // ==================== Comprehensive InstrumentQCStatus field verification
    // ====================

    /**
     * Scenario 1: Instrument 500 has 4 QC results across 2 tests, NO violations, 2
     * active control lots. Verify every field of InstrumentQCStatus.
     */
    @Test
    public void instrumentWithMultipleResultsNoViolations_allFieldsPopulatedCorrectly() {
        InstrumentQCStatus status = dashboardService.getInstrumentComplianceStatus("500");

        assertNotNull(status);

        // Instrument metadata
        assertEquals("500", status.getInstrumentId());
        assertEquals("Coagulation Analyzer", status.getInstrumentName());
        assertEquals("Clinical Chemistry", status.getInstrumentType());
        assertEquals("Coag Lab", status.getInstrumentLocation());

        // Compliance — no violations → GREEN
        assertEquals("GREEN", status.getComplianceColor());
        assertEquals(0, status.getUnresolvedRejections());
        assertEquals(0, status.getUnresolvedWarnings());

        // Triggered rules — empty, no violations
        assertNotNull(status.getTriggeredRules());
        assertTrue("No violations → triggeredRules should be empty", status.getTriggeredRules().isEmpty());

        assertNotNull(status.getTriggeredRuleDetails());
        assertTrue("No violations → triggeredRuleDetails should be empty", status.getTriggeredRuleDetails().isEmpty());

        // Violation time — null, no violations
        assertNull("No violations → lastViolationTime should be null", status.getLastViolationTime());

        // Active control lots — lot-500a and lot-500b are both ACTIVE
        assertEquals(2, status.getActiveControlLots());

        // Analyte details — 2 tests with QC results
        List<AnalyteDetail> analyteDetails = status.getAnalyteDetails();
        assertNotNull(analyteDetails);
        assertEquals(2, analyteDetails.size());

        Set<String> testIds = analyteDetails.stream().map(AnalyteDetail::getTestId).collect(Collectors.toSet());
        assertTrue("Should contain test 1", testIds.contains("1"));
        assertTrue("Should contain test 2", testIds.contains("2"));

        // Verify latest z-scores (latest by run_date_time per test)
        for (AnalyteDetail detail : analyteDetails) {
            assertNotNull(detail.getLastRunTime());
            Instant runTime = Instant.parse(detail.getLastRunTime());
            assertNotNull(runTime);

            if ("1".equals(detail.getTestId())) {
                assertEquals("Fasting Blood Glucose", detail.getTestName());
                // qr-500b (2026-02-19 09:00, z=0.2) is newer than qr-500a (2026-02-18 09:00)
                assertEquals(0, new BigDecimal("0.2000").compareTo(detail.getLatestZScore()));
            }
            if ("2".equals(detail.getTestId())) {
                assertEquals("Total Cholesterol", detail.getTestName());
                // qr-500d (2026-02-20 10:00, z=0.1) is newer than qr-500c (2026-02-18 10:00)
                assertEquals(0, new BigDecimal("0.1000").compareTo(detail.getLatestZScore()));
            }
        }

        // Last result time — should match the most recent result across all tests
        assertNotNull("lastResultTime should be set", status.getLastResultTime());
        Instant lastResult = Instant.parse(status.getLastResultTime());
        assertNotNull(lastResult);
    }

    /**
     * Scenario 2: Instrument 600 has 4 QC results across 2 tests, 1 REJECTION + 1
     * WARNING violation, 1 active control lot. Verify every field of
     * InstrumentQCStatus.
     */
    @Test
    public void instrumentWithMultipleResultsAndViolations_allFieldsPopulatedCorrectly() {
        InstrumentQCStatus status = dashboardService.getInstrumentComplianceStatus("600");

        assertNotNull(status);

        // Instrument metadata
        assertEquals("600", status.getInstrumentId());
        assertEquals("Electrolyte Analyzer", status.getInstrumentName());
        assertEquals("Clinical Chemistry", status.getInstrumentType());
        assertEquals("Chemistry Wing", status.getInstrumentLocation());

        // Compliance — has REJECTION → RED
        assertEquals("RED", status.getComplianceColor());
        assertEquals(1, status.getUnresolvedRejections());
        assertEquals(1, status.getUnresolvedWarnings());

        // Triggered rules — 2 distinct rules
        assertNotNull(status.getTriggeredRules());
        assertEquals(2, status.getTriggeredRules().size());
        assertTrue("Should contain 1_3s", status.getTriggeredRules().contains("1_3s"));
        assertTrue("Should contain 2_2s", status.getTriggeredRules().contains("2_2s"));

        // Triggered rule details — verify severity for each rule
        assertNotNull(status.getTriggeredRuleDetails());
        assertEquals(2, status.getTriggeredRuleDetails().size());
        for (TriggeredRuleDetail ruleDetail : status.getTriggeredRuleDetails()) {
            if ("1_3s".equals(ruleDetail.getRuleCode())) {
                assertEquals("REJECTION", ruleDetail.getSeverity());
            }
            if ("2_2s".equals(ruleDetail.getRuleCode())) {
                assertEquals("WARNING", ruleDetail.getSeverity());
            }
        }

        // Violation time — should be set and parseable
        assertNotNull("Has violations → lastViolationTime should be set", status.getLastViolationTime());
        Instant violationTime = Instant.parse(status.getLastViolationTime());
        assertNotNull(violationTime);

        // Active control lots — lot-600a is ACTIVE
        assertEquals(1, status.getActiveControlLots());

        // Analyte details — 2 tests with QC results
        List<AnalyteDetail> analyteDetails = status.getAnalyteDetails();
        assertNotNull(analyteDetails);
        assertEquals(2, analyteDetails.size());

        Set<String> testIds = analyteDetails.stream().map(AnalyteDetail::getTestId).collect(Collectors.toSet());
        assertTrue("Should contain test 1", testIds.contains("1"));
        assertTrue("Should contain test 2", testIds.contains("2"));

        // Verify latest z-scores
        for (AnalyteDetail detail : analyteDetails) {
            assertNotNull(detail.getLastRunTime());
            Instant runTime = Instant.parse(detail.getLastRunTime());
            assertNotNull(runTime);

            if ("1".equals(detail.getTestId())) {
                assertEquals("Fasting Blood Glucose", detail.getTestName());
                // qr-600b (2026-02-19 08:00, z=3.8) is newer than qr-600a (2026-02-17 08:00)
                assertEquals(0, new BigDecimal("3.8000").compareTo(detail.getLatestZScore()));
            }
            if ("2".equals(detail.getTestId())) {
                assertEquals("Total Cholesterol", detail.getTestName());
                // qr-600d (2026-02-20 08:00, z=1.0) is newer than qr-600c (2026-02-18 08:00)
                assertEquals(0, new BigDecimal("1.0000").compareTo(detail.getLatestZScore()));
            }
        }

        // Last result time — should be set
        assertNotNull("lastResultTime should be set", status.getLastResultTime());
        Instant lastResult = Instant.parse(status.getLastResultTime());
        assertNotNull(lastResult);
    }
}
