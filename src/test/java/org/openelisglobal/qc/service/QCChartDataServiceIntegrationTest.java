package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.beans.factory.annotation.Autowired;

public class QCChartDataServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QCChartDataService qcChartDataService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/qc-dashboard.xml");
        ensureReferenceTables("analyzer", "analyzer_type", "qc_control_lot", "qc_result", "qc_rule_violation",
                "qc_statistics");
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
}
