package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.service.QCChartDataService.QCExportModel;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The chart and export reads against a real database.
 *
 * <p>
 * Fixture {@code qc-dashboard.xml}: analyzer 100 carries lot-001 (test 1
 * "Glucose") with statistics and two runs on 2026-02-20, and analyzer 500
 * carries two ACTIVE NORMAL lots — lot-500a (test 1) with runs on 18 and 19
 * February and lot-500b (test 2) with runs on 18 and 20 February — so a window
 * can put a lot in or out of scope without seeding anything.
 */
public class QCChartDataServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final Timestamp FEB_18 = Timestamp.valueOf("2026-02-18 00:00:00");
    private static final Timestamp FEB_19 = Timestamp.valueOf("2026-02-19 00:00:00");
    private static final Timestamp FEB_20_END = Timestamp.valueOf("2026-02-20 23:59:59");

    @Autowired
    private QCChartDataService qcChartDataService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/qc-dashboard.xml");
        ensureReferenceTables("analyzer", "analyzer_type", "qc_control_lot", "qc_result", "qc_rule_violation",
                "qc_statistics");
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    public void getResultsByControlLotAndDateRange_shouldReturnExpectedResultsFilteredByDates() {
        Timestamp startDate = Timestamp.valueOf("2026-02-20 11:30:00");
        Timestamp endDate = Timestamp.valueOf("2026-02-20 12:30:00");
        String controlLotId = "lot-001";

        List<QCResult> results = qcChartDataService.getResultsByControlLotAndDateRange(controlLotId, startDate,
                endDate);

        assertEquals("Should return exactly 1 matching result in the date range", 1, results.size());

        QCResult result = results.get(0);
        assertEquals("qr-001", result.getId());
        assertEquals("lot-001", result.getControlLotId());
        assertEquals(121.0, result.getResultValue().doubleValue(), 0.0001);
        assertEquals(4.2, result.getZScore().doubleValue(), 0.0001);
        assertEquals("mg/dL", result.getUnitOfMeasure());
        assertEquals(Timestamp.valueOf("2026-02-20 12:00:00"), result.getRunDateTime());
    }

    @Test
    public void getResultsByControlLotAndDateRange_withNullDates_shouldReturnAllResultsForLot() {
        String controlLotId = "lot-500a";

        List<QCResult> results = qcChartDataService.getResultsByControlLotAndDateRange(controlLotId, null, null);

        assertEquals("Should return all 2 results for lot-500a", 2, results.size());

        boolean foundQr500a = false;
        boolean foundQr500b = false;

        for (QCResult result : results) {
            if ("qr-500a".equals(result.getId())) {
                assertEquals(102.5, result.getResultValue().doubleValue(), 0.0001);
                assertEquals(0.5, result.getZScore().doubleValue(), 0.0001);
                foundQr500a = true;
            } else if ("qr-500b".equals(result.getId())) {
                assertEquals(101.0, result.getResultValue().doubleValue(), 0.0001);
                assertEquals(0.2, result.getZScore().doubleValue(), 0.0001);
                foundQr500b = true;
            }
        }

        assertTrue("qr-500a should be in the results", foundQr500a);
        assertTrue("qr-500b should be in the results", foundQr500b);
    }

    @Test
    public void getViolationsForResults_shouldReturnCorrespondingViolations() {
        List<String> resultIds = List.of("qr-001", "qr-002");

        List<QCRuleViolation> violations = qcChartDataService.getViolationsForResults(resultIds);

        assertEquals("Should return exactly 5 violations for the given results", 5, violations.size());

        boolean foundViol001 = false;
        boolean foundViol002 = false;

        for (QCRuleViolation violation : violations) {
            if ("viol-001".equals(violation.getId())) {
                assertEquals("qr-001", violation.getTriggeringResultId());
                assertEquals("1_3s", violation.getRuleCode());
                assertEquals("REJECTION", violation.getSeverity());
                assertEquals("UNRESOLVED", violation.getResolutionStatus());
                foundViol001 = true;
            } else if ("viol-002".equals(violation.getId())) {
                assertEquals("qr-002", violation.getTriggeringResultId());
                assertEquals("2_2s", violation.getRuleCode());
                assertEquals("WARNING", violation.getSeverity());
                assertEquals("UNRESOLVED", violation.getResolutionStatus());
                foundViol002 = true;
            }
        }

        assertTrue("viol-001 should be correctly fetched and mapped", foundViol001);
        assertTrue("viol-002 should be correctly fetched and mapped", foundViol002);
    }

    @Test
    public void getViolationsForResults_withNoViolations_shouldReturnEmptyList() {
        List<String> resultIds = List.of("qr-003");

        List<QCRuleViolation> violations = qcChartDataService.getViolationsForResults(resultIds);

        assertEquals("Should return 0 violations for a result without any violations", 0, violations.size());
    }

    @Test
    public void getLatestStatistics_shouldReturnStatsForLot() {
        String controlLotId = "lot-001";

        QCStatistics stats = qcChartDataService.getLatestStatistics(controlLotId);

        assertTrue("Statistics should not be null", stats != null);
        assertEquals("stats-001", stats.getId());
        assertEquals("lot-001", stats.getControlLotId());
        assertEquals(100.0, stats.getMean().doubleValue(), 0.0001);
        assertEquals(5.0, stats.getStandardDeviation().doubleValue(), 0.0001);
        assertEquals(Integer.valueOf(20), stats.getNumValues());
        assertEquals("INITIAL_RUNS", stats.getCalculationMethod());
        assertEquals(Timestamp.valueOf("2025-01-15 10:00:00"), stats.getCalculationDate());
    }

    @Test
    public void getLatestStatistics_withNoStats_shouldReturnNull() {
        String controlLotId = "lot-500a";

        QCStatistics stats = qcChartDataService.getLatestStatistics(controlLotId);

        assertNull("Statistics should be null for a lot with no calculated stats", stats);
    }

    // ==================== statistics with sigma (OGC-704) ====================

    @Test
    public void getStatisticsWithSigma_isNullWhenTheLotHasNoStatistics() {
        assertNull(qcChartDataService.getStatisticsWithSigma("lot-500a"));
    }

    @Test
    public void getStatisticsWithSigma_derivesSigmaFromTheTestsTotalAllowableError() {
        // lot-001 has mean 100 / SD 5, so CV = 5%; a TEa of 20 gives sigma 4.0.
        jdbc.update("UPDATE clinlims.test SET tea = 20 WHERE id = 1");

        QCChartDataService.StatsWithSigma result = qcChartDataService.getStatisticsWithSigma("lot-001");

        assertEquals(5.0, result.sigma().cv(), 0.0001);
        assertEquals(4.0, result.sigma().sigma(), 0.0001);
        assertEquals(SigmaMetrics.ACCEPTABLE, result.sigma().category());
        assertEquals(100.0, result.statistics().getMean().doubleValue(), 0.0001);
    }

    @Test
    public void getStatisticsWithSigma_reportsTheCvButNoSigmaWhenNoTeaIsConfigured() {
        // The shipped fixture leaves tea unset, as most catalogs do.
        QCChartDataService.StatsWithSigma result = qcChartDataService.getStatisticsWithSigma("lot-001");

        assertEquals(5.0, result.sigma().cv(), 0.0001);
        assertNull(result.sigma().sigma());
        assertEquals(SigmaMetrics.NOT_CALCULABLE, result.sigma().category());
    }

    // ==================== export model (OGC-706) ====================

    @Test
    public void getExportModel_takesTheAnalyzersActiveLotsAndDropsTheOnesWithNoRunsInWindow() {
        // On 19 February only lot-500a ran; lot-500b's runs are on the 18th and 20th.
        QCExportModel model = qcChartDataService.getExportModel("500", null, null, FEB_19,
                Timestamp.valueOf("2026-02-19 23:59:59"), 10000);

        assertEquals("Coagulation Analyzer", model.instrumentName());
        assertEquals("a lot with no runs in the window contributes no section", 1, model.sections().size());
        assertEquals("lot-500a", model.sections().get(0).lot().getId());
        assertEquals(1, model.sections().get(0).results().size());
        assertEquals(1, model.totalRuns());
        assertEquals(0, model.totalViolations());
        assertFalse(model.truncated());
        // The section is labelled with the test's localized name, which is what the
        // report reads — not the catalog description.
        assertEquals("Glucose", model.sections().get(0).testName());
    }

    @Test
    public void getExportModel_controlLevelFilterNarrowsTheLots() {
        assertEquals("both of the analyzer's lots are NORMAL", 2,
                qcChartDataService.getExportModel("500", null, "normal", FEB_18, FEB_20_END, 10000).sections().size());
        assertTrue("no lot is stocked at the HIGH level",
                qcChartDataService.getExportModel("500", null, "HIGH", FEB_18, FEB_20_END, 10000).sections().isEmpty());
    }

    @Test
    public void getExportModel_capsRunRowsAndSaysSo() {
        // Four runs across the two lots, capped at three.
        QCExportModel model = qcChartDataService.getExportModel("500", null, null, FEB_18, FEB_20_END, 3);

        assertTrue("a cap that bites must be reported, never applied silently", model.truncated());
        assertEquals(3, model.totalRuns());
    }

    @Test
    public void getExportModel_narrowsToOneTestWhenAskedFor() {
        QCExportModel model = qcChartDataService.getExportModel("500", "2", null, FEB_18, FEB_20_END, 10000);

        assertEquals(1, model.sections().size());
        assertEquals("lot-500b", model.sections().get(0).lot().getId());
        assertEquals("Cholesterol", model.sections().get(0).testName());
    }
}
