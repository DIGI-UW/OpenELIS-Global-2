package org.openelisglobal.qc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qc.service.QCChartDataService;
import org.openelisglobal.qc.service.SigmaMetrics;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Controller-layer unit tests for {@link QCChartDataRestController}.
 *
 * <p>
 * Standalone MockMvc + Mockito — verifies URL routing, path/query binding,
 * response body shape (ChartDataResponse / ChartStatisticsResponse), and
 * status-code mapping without booting Spring or touching the DB.
 *
 * <p>
 * The statistics endpoint reads the pre-computed
 * {@link QCChartDataService.StatsWithSigma} (the lot -> TEa -> sigma wiring
 * lives in the service now, shared with the OGC-706 export); the sigma
 * computation itself is covered by {@code QCChartDataServiceImplTest}. These
 * tests assert the controller serializes what the service returns.
 */
@RunWith(MockitoJUnitRunner.class)
public class QCChartDataRestControllerTest {

    @Mock
    private QCChartDataService chartDataService;

    @InjectMocks
    private QCChartDataRestController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ==================== getChartData ====================

    @Test
    public void getChartData_returnsDataPointsWithViolationFlag() throws Exception {
        QCResult r1 = new QCResult();
        r1.setId("res-1");
        r1.setResultValue(new BigDecimal("5.20"));
        r1.setZScore(new BigDecimal("0.5"));
        r1.setRunDateTime(new Timestamp(1700000000000L));

        QCResult r2 = new QCResult();
        r2.setId("res-2");
        r2.setResultValue(new BigDecimal("7.80"));
        r2.setZScore(new BigDecimal("3.6"));
        r2.setRunDateTime(new Timestamp(1700003600000L));

        QCRuleViolation v = new QCRuleViolation();
        v.setTriggeringResultId("res-2");
        v.setRuleCode("1_3S");
        v.setSeverity("REJECTION");

        when(chartDataService.getResultsByControlLotAndDateRange(eq("lot-1"), any(), any()))
                .thenReturn(Arrays.asList(r1, r2));
        when(chartDataService.getViolationsForResults(anyList())).thenReturn(Arrays.asList(v));

        mockMvc.perform(get("/rest/qc/charts/lot-1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.controlLotId").value("lot-1"))
                .andExpect(jsonPath("$.dataPoints.length()").value(2))
                .andExpect(jsonPath("$.dataPoints[0].resultId").value("res-1"))
                .andExpect(jsonPath("$.dataPoints[0].hasViolation").value(false))
                .andExpect(jsonPath("$.dataPoints[1].resultId").value("res-2"))
                .andExpect(jsonPath("$.dataPoints[1].hasViolation").value(true))
                .andExpect(jsonPath("$.dataPoints[1].violatedRules[0]").value("1_3S"))
                .andExpect(jsonPath("$.dataPoints[1].severity").value("REJECTION"));
    }

    @Test
    public void getChartData_emptyResults_returnsEmptyDataPoints() throws Exception {
        when(chartDataService.getResultsByControlLotAndDateRange(eq("lot-x"), any(), any()))
                .thenReturn(Collections.<QCResult>emptyList());
        when(chartDataService.getViolationsForResults(anyList()))
                .thenReturn(Collections.<QCRuleViolation>emptyList());

        mockMvc.perform(get("/rest/qc/charts/lot-x")).andExpect(status().isOk())
                .andExpect(jsonPath("$.dataPoints.length()").value(0));
    }

    @Test
    public void getChartData_bindsDateRangeParams() throws Exception {
        when(chartDataService.getResultsByControlLotAndDateRange(eq("lot-1"), any(Timestamp.class),
                any(Timestamp.class))).thenReturn(Collections.<QCResult>emptyList());
        when(chartDataService.getViolationsForResults(anyList())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/rest/qc/charts/lot-1").param("startDate", "2026-01-01").param("endDate", "2026-01-31"))
                .andExpect(status().isOk());
    }

    // ==================== getChartStatistics ====================

    private static QCChartDataService.StatsWithSigma statsWithSigma(BigDecimal mean, BigDecimal sd, String method,
            Integer n, SigmaMetrics.SigmaResult sigma) {
        QCStatistics stats = new QCStatistics();
        stats.setMean(mean);
        stats.setStandardDeviation(sd);
        stats.setCalculationMethod(method);
        stats.setNumValues(n);
        return new QCChartDataService.StatsWithSigma(stats, sigma);
    }

    @Test
    public void getChartStatistics_returns200WithComputedSDLines() throws Exception {
        when(chartDataService.getStatisticsWithSigma("lot-1")).thenReturn(statsWithSigma(new BigDecimal("100.0"),
                new BigDecimal("5.0"), "ROLLING", 25, new SigmaMetrics.SigmaResult(null, null, "NOT_CALCULABLE")));

        mockMvc.perform(get("/rest/qc/charts/lot-1/statistics")).andExpect(status().isOk())
                .andExpect(jsonPath("$.controlLotId").value("lot-1")).andExpect(jsonPath("$.mean").value(100.0))
                .andExpect(jsonPath("$.standardDeviation").value(5.0))
                .andExpect(jsonPath("$.calculationMethod").value("ROLLING"))
                .andExpect(jsonPath("$.resultCount").value(25)).andExpect(jsonPath("$.plus1SD").value(105.0))
                .andExpect(jsonPath("$.plus2SD").value(110.0)).andExpect(jsonPath("$.plus3SD").value(115.0))
                .andExpect(jsonPath("$.minus1SD").value(95.0)).andExpect(jsonPath("$.minus2SD").value(90.0))
                .andExpect(jsonPath("$.minus3SD").value(85.0));
    }

    @Test
    public void getChartStatistics_serializesSigmaFromService() throws Exception {
        when(chartDataService.getStatisticsWithSigma("lot-1")).thenReturn(statsWithSigma(new BigDecimal("100.0"),
                new BigDecimal("2.0"), "INITIAL_RUNS", 20, new SigmaMetrics.SigmaResult(2.0, 5.0, "ACCEPTABLE")));

        mockMvc.perform(get("/rest/qc/charts/lot-1/statistics")).andExpect(status().isOk())
                .andExpect(jsonPath("$.sigma").value(5.0)).andExpect(jsonPath("$.sigmaCategory").value("ACCEPTABLE"));
    }

    @Test
    public void getChartStatistics_serializesNotCalculableSigma() throws Exception {
        when(chartDataService.getStatisticsWithSigma("lot-1")).thenReturn(statsWithSigma(new BigDecimal("100.0"),
                new BigDecimal("2.0"), "INITIAL_RUNS", 20, new SigmaMetrics.SigmaResult(null, null, "NOT_CALCULABLE")));

        mockMvc.perform(get("/rest/qc/charts/lot-1/statistics")).andExpect(status().isOk())
                .andExpect(jsonPath("$.sigma").doesNotExist())
                .andExpect(jsonPath("$.sigmaCategory").value("NOT_CALCULABLE"));
    }

    @Test
    public void getChartStatistics_returns404WhenNoStats() throws Exception {
        when(chartDataService.getStatisticsWithSigma("lot-empty")).thenReturn(null);

        mockMvc.perform(get("/rest/qc/charts/lot-empty/statistics")).andExpect(status().isNotFound());
    }

    @Test
    public void getChartStatistics_handlesNullMeanAndSD() throws Exception {
        // mean + sd null (edge case during establishment) -> defaults to 0.0
        when(chartDataService.getStatisticsWithSigma("lot-1")).thenReturn(
                statsWithSigma(null, null, null, null, new SigmaMetrics.SigmaResult(null, null, "NOT_CALCULABLE")));

        mockMvc.perform(get("/rest/qc/charts/lot-1/statistics")).andExpect(status().isOk())
                .andExpect(jsonPath("$.mean").value(0.0)).andExpect(jsonPath("$.standardDeviation").value(0.0));
    }
}
