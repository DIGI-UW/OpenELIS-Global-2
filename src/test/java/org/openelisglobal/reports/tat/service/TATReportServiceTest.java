package org.openelisglobal.reports.tat.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATDetailResponse;
import org.openelisglobal.reports.tat.bean.TATResult;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.openelisglobal.reports.tat.bean.TATSummaryResponse;
import org.openelisglobal.reports.tat.bean.TATSummaryResponse.BreakdownRow;
import org.openelisglobal.reports.tat.bean.TATSummaryResponse.HistogramBin;
import org.openelisglobal.reports.tat.bean.TATTrendResponse;
import org.openelisglobal.reports.tat.bean.TATTrendResponse.TrendDataPoint;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.TestSection;

/**
 * Unit tests for TATReportServiceImpl — summary aggregation, histogram bins,
 * breakdown, detail pagination/sorting, and trend aggregation.
 *
 * Mocks the EntityManager → Session → Query chain to inject controlled
 * Analysis/Sample data without requiring a real database.
 */
@RunWith(MockitoJUnitRunner.class)
public class TATReportServiceTest {

    @InjectMocks
    private TATReportServiceImpl tatReportService;

    @Mock
    private TATCalculationService tatCalculationService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Query query;

    private static final LocalDate FROM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TO = LocalDate.of(2026, 3, 31);

    @Before
    public void setUp() {
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    // ========== Helper Methods ==========

    /**
     * Build a minimal Analysis + Sample + SampleItem row tuple for the HQL result.
     * Uses Mockito mocks to avoid triggering DateUtil static initialization which
     * requires the full Spring context.
     */
    private Object[] makeRow(String accession, String labUnit, String priority, Timestamp received,
            Timestamp resultEntered, Timestamp validated) {
        Analysis analysis = mock(Analysis.class);
        when(analysis.getStartedDate()).thenReturn(received);
        when(analysis.getCompletedDate()).thenReturn(resultEntered);
        when(analysis.getReleasedDate()).thenReturn(validated);

        if (labUnit != null) {
            TestSection ts = mock(TestSection.class);
            when(ts.getLocalizedName()).thenReturn(labUnit);
            when(analysis.getTestSection()).thenReturn(ts);
        }

        SampleItem sampleItem = mock(SampleItem.class);
        when(analysis.getSampleItem()).thenReturn(sampleItem);

        Sample sample = mock(Sample.class);
        when(sample.getAccessionNumber()).thenReturn(accession);
        when(sample.getReceivedTimestamp()).thenReturn(received);
        when(sample.getEnteredDate()).thenReturn(received != null ? new java.sql.Date(received.getTime()) : null);

        return new Object[] { analysis, sample, sampleItem };
    }

    private void stubQueryResults(List<Object[]> rows) {
        when(query.list()).thenReturn(rows);
    }

    /**
     * Stub the TAT calculation to return a fixed value for CALENDAR mode.
     */
    private void stubTatCalculation(BigDecimal value) {
        when(tatCalculationService.calculateTatHours(any(), any(), any())).thenReturn(value);
    }

    private void stubTatCalculationSequence(BigDecimal... values) {
        // First call returns values[0], second returns values[1], etc.
        org.mockito.stubbing.OngoingStubbing<BigDecimal> stub = when(
                tatCalculationService.calculateTatHours(any(), any(), any()));
        for (BigDecimal v : values) {
            stub = stub.thenReturn(v);
        }
    }

    private List<Object[]> buildSampleRows(int count, String labUnit) {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Timestamp ts = Timestamp.valueOf("2026-03-" + String.format("%02d", (i % 28) + 1) + " 09:00:00");
            rows.add(makeRow("LAB-" + String.format("%03d", i), labUnit, "Routine", ts,
                    new Timestamp(ts.getTime() + (i + 1) * 3600000L), // resultEntered = received + (i+1) hours
                    new Timestamp(ts.getTime() + (i + 2) * 3600000L)));
        }
        return rows;
    }

    // ========== Summary: Statistics Tests ==========

    @Test
    public void getSummary_calculatesCorrectMean() {
        // 3 results with TAT values: 2h, 4h, 6h → mean = 4.0
        List<Object[]> rows = buildSampleRows(3, "Hematology");
        stubQueryResults(rows);
        // Return 2, 4, 6 for segment TAT, then 2, 4, 6 for overall TAT
        stubTatCalculationSequence(new BigDecimal("2.00"), new BigDecimal("2.00"), new BigDecimal("4.00"),
                new BigDecimal("4.00"), new BigDecimal("6.00"), new BigDecimal("6.00"));

        TATSummaryResponse response = tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, "LAB_UNIT");

        Assert.assertNotNull(response.getMean());
        Assert.assertEquals(0, new BigDecimal("4.00").compareTo(response.getMean()));
    }

    @Test
    public void getSummary_calculatesCorrectMedian() {
        // 5 results: 1, 3, 5, 7, 9 → median (50th percentile) = 5
        List<Object[]> rows = buildSampleRows(5, "Chemistry");
        stubQueryResults(rows);
        BigDecimal[] vals = { new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("3"), new BigDecimal("3"),
                new BigDecimal("5"), new BigDecimal("5"), new BigDecimal("7"), new BigDecimal("7"), new BigDecimal("9"),
                new BigDecimal("9") };
        stubTatCalculationSequence(vals);

        TATSummaryResponse response = tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, null);

        Assert.assertNotNull(response.getMedian());
        Assert.assertEquals(0, new BigDecimal("5").compareTo(response.getMedian()));
    }

    @Test
    public void getSummary_calculatesCorrectP90() {
        // 10 results: 1..10 → p90 = index ceil(0.9*10)-1 = index 8 → value 9
        List<Object[]> rows = buildSampleRows(10, "Microbiology");
        stubQueryResults(rows);
        BigDecimal[] vals = new BigDecimal[20]; // 10 segment + 10 overall
        for (int i = 0; i < 10; i++) {
            vals[i * 2] = BigDecimal.valueOf(i + 1); // segment TAT
            vals[i * 2 + 1] = BigDecimal.valueOf(i + 1); // overall TAT
        }
        stubTatCalculationSequence(vals);

        TATSummaryResponse response = tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, null);

        Assert.assertNotNull(response.getPercentile90());
        Assert.assertEquals(0, new BigDecimal("9").compareTo(response.getPercentile90()));
    }

    @Test
    public void getSummary_calculatesCorrectStdDev() {
        // 3 results: 2, 4, 6 → mean=4, var=((4+0+4)/2)=4, stddev=2.0
        List<Object[]> rows = buildSampleRows(3, "Hematology");
        stubQueryResults(rows);
        stubTatCalculationSequence(new BigDecimal("2.00"), new BigDecimal("2.00"), new BigDecimal("4.00"),
                new BigDecimal("4.00"), new BigDecimal("6.00"), new BigDecimal("6.00"));

        TATSummaryResponse response = tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, null);

        Assert.assertNotNull(response.getStdDeviation());
        Assert.assertEquals(0, new BigDecimal("2.00").compareTo(response.getStdDeviation()));
    }

    @Test
    public void getSummary_handlesEmptyResults() {
        stubQueryResults(Collections.emptyList());

        TATSummaryResponse response = tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, null);

        Assert.assertEquals(0, response.getTotalCount());
        Assert.assertNull(response.getMean());
        Assert.assertNull(response.getMedian());
        Assert.assertNull(response.getMin());
        Assert.assertNull(response.getMax());
    }

    @Test
    public void getSummary_handlesAllNullTatValues() {
        // Results exist but all TAT values are null (missing timestamps)
        List<Object[]> rows = buildSampleRows(3, "Hematology");
        stubQueryResults(rows);
        stubTatCalculation(null); // all TAT calcs return null

        TATSummaryResponse response = tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, null);

        Assert.assertEquals(0, response.getTotalCount());
        Assert.assertNull(response.getMean());
    }

    // ========== Histogram Tests ==========

    @Test
    public void getSummary_histogramBinsMatchNonUniformSpec() {
        // Just need to verify the 10 bins are returned with correct labels
        stubQueryResults(Collections.emptyList());

        TATSummaryResponse response = tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, null);

        List<HistogramBin> histogram = response.getHistogram();
        Assert.assertEquals(10, histogram.size());
        Assert.assertEquals("0-1h", histogram.get(0).getBinLabel());
        Assert.assertEquals("1-2h", histogram.get(1).getBinLabel());
        Assert.assertEquals("2-3h", histogram.get(2).getBinLabel());
        Assert.assertEquals("3-4h", histogram.get(3).getBinLabel());
        Assert.assertEquals("4-6h", histogram.get(4).getBinLabel());
        Assert.assertEquals("6-8h", histogram.get(5).getBinLabel());
        Assert.assertEquals("8-12h", histogram.get(6).getBinLabel());
        Assert.assertEquals("12-24h", histogram.get(7).getBinLabel());
        Assert.assertEquals("24-48h", histogram.get(8).getBinLabel());
        Assert.assertEquals("48h+", histogram.get(9).getBinLabel());
    }

    @Test
    public void getSummary_histogramCountsCorrect() {
        // 4 results: 0.5h (bin 0-1), 1.5h (bin 1-2), 3.5h (bin 3-4), 25h (bin 24-48)
        List<Object[]> rows = buildSampleRows(4, "Hematology");
        stubQueryResults(rows);
        stubTatCalculationSequence(new BigDecimal("0.5"), new BigDecimal("0.5"), new BigDecimal("1.5"),
                new BigDecimal("1.5"), new BigDecimal("3.5"), new BigDecimal("3.5"), new BigDecimal("25"),
                new BigDecimal("25"));

        TATSummaryResponse response = tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, null);

        List<HistogramBin> histogram = response.getHistogram();
        Assert.assertEquals(1, histogram.get(0).getCount()); // 0-1h
        Assert.assertEquals(1, histogram.get(1).getCount()); // 1-2h
        Assert.assertEquals(0, histogram.get(2).getCount()); // 2-3h
        Assert.assertEquals(1, histogram.get(3).getCount()); // 3-4h
        Assert.assertEquals(1, histogram.get(8).getCount()); // 24-48h
    }

    // ========== Breakdown Tests ==========

    @Test
    public void getSummary_breakdownByLabUnit_groupsCorrectly() {
        List<Object[]> rows = new ArrayList<>();
        rows.addAll(buildSampleRows(3, "Hematology"));
        rows.addAll(buildSampleRows(2, "Chemistry"));
        stubQueryResults(rows);
        stubTatCalculation(new BigDecimal("5.00"));

        TATSummaryResponse response = tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, "LAB_UNIT");

        List<BreakdownRow> breakdown = response.getBreakdown();
        Assert.assertEquals(2, breakdown.size());

        // Find each group
        BreakdownRow hema = breakdown.stream().filter(b -> "Hematology".equals(b.getDimensionValue())).findFirst()
                .orElse(null);
        BreakdownRow chem = breakdown.stream().filter(b -> "Chemistry".equals(b.getDimensionValue())).findFirst()
                .orElse(null);

        Assert.assertNotNull(hema);
        Assert.assertNotNull(chem);
        Assert.assertEquals(3, hema.getCount());
        Assert.assertEquals(2, chem.getCount());
    }

    @Test
    public void getSummary_breakdownByPriority_groupsCorrectly() {
        List<Object[]> rows = buildSampleRows(4, "Hematology");
        stubQueryResults(rows);
        stubTatCalculation(new BigDecimal("3.00"));

        TATSummaryResponse response = tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, "PRIORITY");

        List<BreakdownRow> breakdown = response.getBreakdown();
        Assert.assertFalse(breakdown.isEmpty());
        // All samples have default "Routine" priority
        Assert.assertTrue(breakdown.stream().anyMatch(b -> "Routine".equals(b.getDimensionValue())));
    }

    // ========== Detail: Pagination Tests ==========

    @Test
    public void getDetail_paginatesCorrectly() {
        List<Object[]> rows = buildSampleRows(100, "Hematology");
        stubQueryResults(rows);
        stubTatCalculation(new BigDecimal("5.00"));

        TATDetailResponse response = tatReportService.getDetail(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, 0, 25, null, null, null, null);

        Assert.assertEquals(100, response.getTotalCount());
        Assert.assertEquals(0, response.getPage());
        Assert.assertEquals(25, response.getPageSize());
        Assert.assertEquals(25, response.getResults().size());
    }

    @Test
    public void getDetail_sortsDescByDefault() {
        List<Object[]> rows = buildSampleRows(5, "Hematology");
        stubQueryResults(rows);
        // Return ascending values: 1, 2, 3, 4, 5 for segment TAT
        BigDecimal[] vals = new BigDecimal[10];
        for (int i = 0; i < 5; i++) {
            vals[i * 2] = BigDecimal.valueOf(i + 1);
            vals[i * 2 + 1] = BigDecimal.valueOf(i + 1);
        }
        stubTatCalculationSequence(vals);

        TATDetailResponse response = tatReportService.getDetail(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, 0, 25, null, "desc", null,
                null);

        List<TATResult> results = response.getResults();
        Assert.assertEquals(5, results.size());
        // Descending: first result should have the highest TAT
        Assert.assertTrue(results.get(0).getSelectedSegmentTat()
                .compareTo(results.get(results.size() - 1).getSelectedSegmentTat()) >= 0);
    }

    @Test
    public void getDetail_secondPageReturnsCorrectSlice() {
        List<Object[]> rows = buildSampleRows(50, "Hematology");
        stubQueryResults(rows);
        stubTatCalculation(new BigDecimal("3.00"));

        TATDetailResponse response = tatReportService.getDetail(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, 1, 25, null, null, null, null);

        Assert.assertEquals(50, response.getTotalCount());
        Assert.assertEquals(1, response.getPage());
        Assert.assertEquals(25, response.getResults().size());
    }

    @Test
    public void getDetail_lastPageReturnsRemainder() {
        List<Object[]> rows = buildSampleRows(30, "Hematology");
        stubQueryResults(rows);
        stubTatCalculation(new BigDecimal("3.00"));

        TATDetailResponse response = tatReportService.getDetail(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, 1, 25, null, null, null, null);

        Assert.assertEquals(30, response.getTotalCount());
        Assert.assertEquals(5, response.getResults().size());
    }

    @Test
    public void getDetail_pageOutOfRangeReturnsEmpty() {
        List<Object[]> rows = buildSampleRows(10, "Hematology");
        stubQueryResults(rows);
        stubTatCalculation(new BigDecimal("3.00"));

        TATDetailResponse response = tatReportService.getDetail(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, 5, 25, null, null, null, null);

        Assert.assertEquals(10, response.getTotalCount());
        Assert.assertTrue(response.getResults().isEmpty());
    }

    // ========== Trend: Aggregation Tests ==========

    @Test
    public void getTrend_dailyAggregation() {
        List<Object[]> rows = buildSampleRows(5, "Hematology");
        stubQueryResults(rows);
        stubTatCalculation(new BigDecimal("3.00"));

        TATTrendResponse response = tatReportService.getTrend(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, "DAILY", null);

        Assert.assertNotNull(response.getSeries());
        Assert.assertFalse(response.getSeries().isEmpty());
        Assert.assertEquals("All", response.getSeries().get(0).getLabel());

        // Each result is on a different day → each day should have its own data point
        List<TrendDataPoint> points = response.getSeries().get(0).getDataPoints();
        Assert.assertFalse(points.isEmpty());
        // Daily aggregation: period key should be a date string (YYYY-MM-DD format)
        Assert.assertTrue(points.get(0).getPeriod().matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void getTrend_weeklyAggregation() {
        List<Object[]> rows = buildSampleRows(10, "Hematology");
        stubQueryResults(rows);
        stubTatCalculation(new BigDecimal("3.00"));

        TATTrendResponse response = tatReportService.getTrend(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, "WEEKLY", null);

        List<TrendDataPoint> points = response.getSeries().get(0).getDataPoints();
        Assert.assertFalse(points.isEmpty());
        // Weekly period key format: YYYY-WNN
        Assert.assertTrue(points.get(0).getPeriod().matches("\\d{4}-W\\d{2}"));
    }

    @Test
    public void getTrend_monthlyAggregation() {
        List<Object[]> rows = buildSampleRows(5, "Hematology");
        stubQueryResults(rows);
        stubTatCalculation(new BigDecimal("3.00"));

        TATTrendResponse response = tatReportService.getTrend(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, "MONTHLY", null);

        List<TrendDataPoint> points = response.getSeries().get(0).getDataPoints();
        Assert.assertFalse(points.isEmpty());
        // Monthly period key format: YYYY-MM
        Assert.assertTrue(points.get(0).getPeriod().matches("\\d{4}-\\d{2}"));
    }

    @Test
    public void getTrend_withCompareBy_returnMultipleSeries() {
        List<Object[]> rows = new ArrayList<>();
        rows.addAll(buildSampleRows(3, "Hematology"));
        rows.addAll(buildSampleRows(2, "Chemistry"));
        stubQueryResults(rows);
        stubTatCalculation(new BigDecimal("3.00"));

        TATTrendResponse response = tatReportService.getTrend(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, "DAILY", "LAB_UNIT");

        Assert.assertNotNull(response.getSeries());
        Assert.assertEquals(2, response.getSeries().size());
    }

    // ========== Working Time Mode ==========

    @Test
    public void getSummary_workingTimeMode_setsExcludedDaysCount() {
        stubQueryResults(Collections.emptyList());
        when(tatCalculationService.countExcludedDays(FROM, TO)).thenReturn(10);

        TATSummaryResponse response = tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.WORKING_TIME, null, null, null, null, null, null, false, null);

        Assert.assertEquals("WORKING_TIME", response.getCalculationMode());
        Assert.assertEquals(10, response.getExcludedDaysCount());
    }

    @Test
    public void getSummary_calendarMode_excludedDaysCountIsZero() {
        stubQueryResults(Collections.emptyList());

        TATSummaryResponse response = tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, null);

        Assert.assertEquals("CALENDAR", response.getCalculationMode());
        Assert.assertEquals(0, response.getExcludedDaysCount());
    }

    // ========== Min / Max ==========

    @Test
    public void getSummary_minAndMaxCorrect() {
        List<Object[]> rows = buildSampleRows(4, "Hematology");
        stubQueryResults(rows);
        stubTatCalculationSequence(new BigDecimal("1.00"), new BigDecimal("1.00"), new BigDecimal("5.00"),
                new BigDecimal("5.00"), new BigDecimal("3.00"), new BigDecimal("3.00"), new BigDecimal("10.00"),
                new BigDecimal("10.00"));

        TATSummaryResponse response = tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, null);

        Assert.assertEquals(0, new BigDecimal("1.00").compareTo(response.getMin()));
        Assert.assertEquals(0, new BigDecimal("10.00").compareTo(response.getMax()));
    }

    // ========== Filter and Cancelled Tests ==========

    @Test
    public void testGetSummary_withPriorityFilter_filterIsPassedToQuery() {
        stubQueryResults(Collections.emptyList());

        tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR, null, null,
                null, "STAT", null, null, false, null);

        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(session).createQuery(hqlCaptor.capture());
        String hql = hqlCaptor.getValue();
        Assert.assertTrue("HQL should contain priority filter when priority is non-null",
                hql.contains("s.priority = :priority"));
    }

    @Test
    public void testQueryResults_withLabUnitFilter_hqlContainsTestSectionFilter() {
        stubQueryResults(Collections.emptyList());

        tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR, "1,2",
                null, null, null, null, null, false, null);

        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(session).createQuery(hqlCaptor.capture());
        String hql = hqlCaptor.getValue();
        Assert.assertTrue("HQL should contain testSection filter when labUnitIds is non-null",
                hql.contains("a.testSection.id IN (:labUnitIds)"));
    }

    @Test
    public void testQueryResults_withTestFilter_hqlContainsTestFilter() {
        stubQueryResults(Collections.emptyList());

        tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR, null, "5",
                null, null, null, null, false, null);

        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(session).createQuery(hqlCaptor.capture());
        String hql = hqlCaptor.getValue();
        Assert.assertTrue("HQL should contain test filter when testIds is non-null",
                hql.contains("a.test.id IN (:testIds)"));
    }

    @Test
    public void testQueryResults_withSampleTypeFilter_hqlContainsSampleTypeFilter() {
        stubQueryResults(Collections.emptyList());

        tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR, null, null,
                null, null, "3", null, false, null);

        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(session).createQuery(hqlCaptor.capture());
        String hql = hqlCaptor.getValue();
        Assert.assertTrue("HQL should contain sampleType filter when sampleTypeId is non-null",
                hql.contains("si.typeOfSample.id = :sampleTypeId"));
    }

    @Test
    public void testIncludeCancelled_statusNameIsTestCanceled() {
        stubQueryResults(Collections.emptyList());

        // includeCancelled = false should add a filter excluding "Test Canceled"
        tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR, null, null,
                null, null, null, null, false, null);

        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(session).createQuery(hqlCaptor.capture());
        String hql = hqlCaptor.getValue();
        Assert.assertTrue("HQL should filter out 'Test Canceled' status when includeCancelled=false",
                hql.contains("Test Canceled"));
    }

    @Test
    public void testQueryResults_withOrderingSiteFilter_hqlContainsExistsSubquery() {
        stubQueryResults(Collections.emptyList());

        tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR, null, null,
                null, null, null, "42", false, null);

        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(session).createQuery(hqlCaptor.capture());
        String hql = hqlCaptor.getValue();
        Assert.assertTrue("HQL should contain SampleOrganization EXISTS subquery when orderingSiteId is set",
                hql.contains("SampleOrganization"));
    }

    @Test
    public void testQueryResults_withoutOrderingSiteFilter_hqlDoesNotContainSampleOrg() {
        stubQueryResults(Collections.emptyList());

        tatReportService.getSummary(FROM, TO, TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR, null, null,
                null, null, null, null, false, null);

        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(session).createQuery(hqlCaptor.capture());
        String hql = hqlCaptor.getValue();
        Assert.assertFalse("HQL should NOT contain SampleOrganization when orderingSiteId is null",
                hql.contains("SampleOrganization"));
    }
}
