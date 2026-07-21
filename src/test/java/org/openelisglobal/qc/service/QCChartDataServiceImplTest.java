package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCRuleViolationDAO;
import org.openelisglobal.qc.service.QCChartDataService.QCExportModel;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.openelisglobal.test.service.TestService;

/**
 * Unit tests for {@link QCChartDataServiceImpl}'s OGC-706 additions: the shared
 * sigma wiring ({@code getStatisticsWithSigma}) and the export-model assembly
 * ({@code getExportModel} — instrument/test/level filtering, empty-lot
 * skipping, and the CSV row cap). DAOs/services are mocked; the logic under
 * test is the assembly, not persistence.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class QCChartDataServiceImplTest {

    @Mock
    private QCResultDAO resultDAO;

    @Mock
    private QCStatisticsService statisticsService;

    @Mock
    private QCRuleViolationDAO violationDAO;

    @Mock
    private QCControlLotService controlLotService;

    @Mock
    private TestService testService;

    @Mock
    private AnalyzerService analyzerService;

    @InjectMocks
    private QCChartDataServiceImpl service;

    private static QCStatistics stats(String mean, String sd, int n) {
        QCStatistics s = new QCStatistics();
        s.setMean(new BigDecimal(mean));
        s.setStandardDeviation(new BigDecimal(sd));
        s.setNumValues(n);
        s.setCalculationMethod("ROLLING");
        s.setCalculationDate(Timestamp.valueOf("2026-06-30 12:00:00"));
        return s;
    }

    private static QCControlLot lot(String id, String instrumentId, String testId, String level) {
        QCControlLot lot = new QCControlLot();
        lot.setId(id);
        lot.setInstrumentId(instrumentId);
        lot.setTestId(testId);
        lot.setControlLevel(level);
        lot.setLotNumber("LOT-" + id);
        return lot;
    }

    private static QCResult result(String id) {
        QCResult r = new QCResult();
        r.setId(id);
        r.setResultValue(new BigDecimal("100.0"));
        r.setRunDateTime(Timestamp.valueOf("2026-06-15 09:00:00"));
        return r;
    }

    // ==================== getStatisticsWithSigma ====================

    @Test
    public void getStatisticsWithSigma_nullWhenNoStats() {
        when(statisticsService.getLatestStatistics("lot-1")).thenReturn(null);
        assertNull(service.getStatisticsWithSigma("lot-1"));
    }

    @Test
    public void getStatisticsWithSigma_computesSigmaFromLotTea() {
        // mean=100, sd=2 -> CV=2.0%; TEa=10 -> sigma=5.0 -> ACCEPTABLE
        when(statisticsService.getLatestStatistics("lot-1")).thenReturn(stats("100.0", "2.0", 30));
        when(controlLotService.get("lot-1")).thenReturn(lot("lot-1", "1", "412", "NORMAL"));
        org.openelisglobal.test.valueholder.Test test = mock(org.openelisglobal.test.valueholder.Test.class);
        when(test.getTea()).thenReturn(10.0);
        when(test.getLocalizedName()).thenReturn("Glucose");
        when(testService.getTestById("412")).thenReturn(test);

        QCChartDataService.StatsWithSigma result = service.getStatisticsWithSigma("lot-1");

        assertEquals(2.0, result.sigma().cv(), 0.0001);
        assertEquals(5.0, result.sigma().sigma(), 0.0001);
        assertEquals(SigmaMetrics.ACCEPTABLE, result.sigma().category());
        assertEquals(new BigDecimal("100.0"), result.statistics().getMean());
    }

    @Test
    public void getStatisticsWithSigma_notCalculableWhenLotMissing() {
        when(statisticsService.getLatestStatistics("lot-1")).thenReturn(stats("100.0", "2.0", 30));
        when(controlLotService.get("lot-1")).thenReturn(null);

        QCChartDataService.StatsWithSigma result = service.getStatisticsWithSigma("lot-1");

        assertEquals(SigmaMetrics.NOT_CALCULABLE, result.sigma().category());
        assertNull(result.sigma().sigma());
    }

    // ==================== getExportModel ====================

    @Test
    public void getExportModel_filtersByInstrumentAndSkipsEmptyLots() {
        QCControlLot lotA = lot("a", "1", "412", "NORMAL"); // in scope, has runs
        QCControlLot lotB = lot("b", "1", "412", "HIGH"); // in scope, no runs -> skipped
        // The ACTIVE-by-instrument query already scopes to instrument "1"; only lots
        // it returns are considered (matches the on-screen chart workflow).
        when(controlLotService.getActiveControlLotsByInstrument("1")).thenReturn(Arrays.asList(lotA, lotB));
        when(analyzerService.getWithType("1")).thenReturn(Optional.of(analyzer("Cobas 6000")));

        when(resultDAO.findByControlLotAndDateRange(eq("a"), any(), any()))
                .thenReturn(Arrays.asList(result("r1"), result("r2")));
        when(resultDAO.findByControlLotAndDateRange(eq("b"), any(), any())).thenReturn(Collections.emptyList());
        when(statisticsService.getLatestStatistics("a")).thenReturn(stats("100.0", "2.0", 30));
        when(violationDAO.findByTriggeringResultId(any())).thenReturn(Collections.emptyList());
        org.openelisglobal.test.valueholder.Test test = mock(org.openelisglobal.test.valueholder.Test.class);
        when(test.getTea()).thenReturn(10.0);
        when(test.getLocalizedName()).thenReturn("Glucose");
        when(testService.getTestById("412")).thenReturn(test);

        QCExportModel model = service.getExportModel("1", null, null, ts("2026-06-01"), ts("2026-06-30"), 10000);

        assertEquals("Cobas 6000", model.instrumentName());
        assertEquals("only lot A has runs in window", 1, model.sections().size());
        assertEquals("a", model.sections().get(0).lot().getId());
        assertEquals(2, model.sections().get(0).results().size());
        assertEquals(2, model.totalRuns());
        assertEquals(0, model.totalViolations());
        assertFalse(model.truncated());
        assertEquals(5.0, model.sections().get(0).sigma().sigma(), 0.0001);
        assertEquals("Glucose", model.sections().get(0).testName());
    }

    @Test
    public void getExportModel_controlLevelFilterNarrowsLots() {
        QCControlLot normal = lot("a", "1", "412", "NORMAL");
        QCControlLot high = lot("b", "1", "412", "HIGH");
        when(controlLotService.getActiveControlLotsByInstrument("1")).thenReturn(Arrays.asList(normal, high));
        when(analyzerService.getWithType("1")).thenReturn(Optional.of(analyzer("Cobas")));
        when(resultDAO.findByControlLotAndDateRange(eq("b"), any(), any())).thenReturn(Arrays.asList(result("r1")));
        when(statisticsService.getLatestStatistics("b")).thenReturn(stats("50.0", "1.0", 25));
        when(violationDAO.findByTriggeringResultId(any())).thenReturn(Collections.emptyList());

        QCExportModel model = service.getExportModel("1", null, "HIGH", ts("2026-06-01"), ts("2026-06-30"), 10000);

        assertEquals(1, model.sections().size());
        assertEquals("HIGH", model.sections().get(0).lot().getControlLevel());
    }

    @Test
    public void getExportModel_capsRowsAndFlagsTruncated() {
        QCControlLot lotA = lot("a", "1", "412", "NORMAL");
        when(controlLotService.getActiveControlLotsByInstrument("1")).thenReturn(Arrays.asList(lotA));
        when(analyzerService.getWithType("1")).thenReturn(Optional.of(analyzer("Cobas")));
        when(resultDAO.findByControlLotAndDateRange(eq("a"), any(), any()))
                .thenReturn(Arrays.asList(result("r1"), result("r2"), result("r3"), result("r4"), result("r5")));
        when(statisticsService.getLatestStatistics("a")).thenReturn(stats("100.0", "2.0", 30));
        when(violationDAO.findByTriggeringResultId(any())).thenReturn(Collections.emptyList());

        QCExportModel model = service.getExportModel("1", null, null, ts("2026-06-01"), ts("2026-06-30"), 3);

        assertTrue("cap of 3 with 5 rows should truncate", model.truncated());
        assertEquals(3, model.sections().get(0).results().size());
        assertEquals(3, model.totalRuns());
    }

    @Test
    public void getExportModel_withTestIdUsesActiveByTestQuery() {
        QCControlLot lotA = lot("a", "1", "412", "NORMAL");
        when(controlLotService.getActiveControlLots("412", "1")).thenReturn(Arrays.asList(lotA));
        when(analyzerService.getWithType("1")).thenReturn(Optional.of(analyzer("Cobas")));
        when(resultDAO.findByControlLotAndDateRange(eq("a"), any(), any())).thenReturn(Arrays.asList(result("r1")));
        when(statisticsService.getLatestStatistics("a")).thenReturn(stats("100.0", "2.0", 30));
        when(violationDAO.findByTriggeringResultId(any())).thenReturn(Collections.emptyList());
        org.openelisglobal.test.valueholder.Test test = mock(org.openelisglobal.test.valueholder.Test.class);
        when(test.getLocalizedName()).thenReturn("Glucose");
        when(testService.getTestById("412")).thenReturn(test);

        QCExportModel model = service.getExportModel("1", "412", null, ts("2026-06-01"), ts("2026-06-30"), 10000);

        assertEquals("testId branch selects the active-by-test query", 1, model.sections().size());
        assertEquals("a", model.sections().get(0).lot().getId());
    }

    private static Analyzer analyzer(String name) {
        Analyzer a = new Analyzer();
        a.setName(name);
        return a;
    }

    private static Timestamp ts(String date) {
        return Timestamp.valueOf(date + " 00:00:00");
    }
}
