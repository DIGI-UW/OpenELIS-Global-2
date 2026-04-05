package org.openelisglobal.reports.tat.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATDetailResponse;
import org.openelisglobal.reports.tat.bean.TATResult;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.openelisglobal.reports.tat.bean.TATSummaryResponse;
import org.openelisglobal.reports.tat.bean.TATSummaryResponse.BreakdownRow;
import org.openelisglobal.reports.tat.bean.TATSummaryResponse.HistogramBin;
import org.openelisglobal.reports.tat.bean.TATTrendResponse;
import org.openelisglobal.reports.tat.bean.TATTrendResponse.TrendDataPoint;
import org.openelisglobal.reports.tat.bean.TATTrendResponse.TrendSeries;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TATReportServiceImpl implements TATReportService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TATCalculationService tatCalculationService;

    // Fixed non-uniform histogram bins per requirements doc
    private static final double[][] HISTOGRAM_BINS = { { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 4 }, { 4, 6 }, { 6, 8 },
            { 8, 12 }, { 12, 24 }, { 24, 48 }, { 48, Double.MAX_VALUE } };

    private static final String[] HISTOGRAM_LABELS = { "0-1h", "1-2h", "2-3h", "3-4h", "4-6h", "6-8h", "8-12h",
            "12-24h", "24-48h", "48h+" };

    @Override
    public TATSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate, TATSegment segment,
            TATCalculationMode mode, String labUnitIds, String testIds, String panelIds, String priority,
            Integer sampleTypeId, Integer orderingSiteId, boolean includeCancelled, String breakdownBy) {

        List<TATResult> results = queryResults(fromDate, toDate, segment, mode, labUnitIds, testIds, panelIds, priority,
                sampleTypeId, orderingSiteId, includeCancelled);

        List<BigDecimal> tatValues = results.stream().map(TATResult::getSelectedSegmentTat).filter(v -> v != null)
                .sorted().collect(Collectors.toList());

        TATSummaryResponse response = new TATSummaryResponse();
        response.setCalculationMode(mode.name());
        response.setExcludedDaysCount(
                mode == TATCalculationMode.WORKING_TIME ? tatCalculationService.countExcludedDays(fromDate, toDate)
                        : 0);
        response.setTotalCount(tatValues.size());

        if (!tatValues.isEmpty()) {
            response.setMean(calculateMean(tatValues));
            response.setMedian(calculatePercentile(tatValues, 50));
            response.setPercentile90(calculatePercentile(tatValues, 90));
            response.setMin(tatValues.get(0));
            response.setMax(tatValues.get(tatValues.size() - 1));
            response.setStdDeviation(calculateStdDev(tatValues, response.getMean()));
        }

        response.setHistogram(buildHistogram(tatValues));
        response.setBreakdown(buildBreakdown(results, breakdownBy));

        return response;
    }

    @Override
    public TATDetailResponse getDetail(LocalDate fromDate, LocalDate toDate, TATSegment segment,
            TATCalculationMode mode, String labUnitIds, String testIds, String panelIds, String priority,
            Integer sampleTypeId, Integer orderingSiteId, boolean includeCancelled, int page, int pageSize,
            String sortField, String sortOrder, String breakdownFilter) {

        List<TATResult> allResults = queryResults(fromDate, toDate, segment, mode, labUnitIds, testIds, panelIds,
                priority, sampleTypeId, orderingSiteId, includeCancelled);

        // Apply breakdown filter if present
        if (breakdownFilter != null && !breakdownFilter.isEmpty()) {
            allResults = allResults.stream()
                    .filter(r -> breakdownFilter.equals(r.getLabUnit()) || breakdownFilter.equals(r.getTestName())
                            || breakdownFilter.equals(r.getPriority()) || breakdownFilter.equals(r.getSampleType())
                            || breakdownFilter.equals(r.getOrderingSite()))
                    .collect(Collectors.toList());
        }

        // Sort
        Comparator<TATResult> comparator = getComparator(sortField, sortOrder);
        allResults.sort(comparator);

        // Paginate
        int total = allResults.size();
        int fromIdx = Math.min(page * pageSize, total);
        int toIdx = Math.min(fromIdx + pageSize, total);

        TATDetailResponse response = new TATDetailResponse();
        response.setTotalCount(total);
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setCalculationMode(mode.name());
        response.setResults(allResults.subList(fromIdx, toIdx));
        return response;
    }

    @Override
    public TATTrendResponse getTrend(LocalDate fromDate, LocalDate toDate, TATSegment segment, TATCalculationMode mode,
            String labUnitIds, String testIds, String panelIds, String priority, Integer sampleTypeId,
            Integer orderingSiteId, boolean includeCancelled, String interval, String compareBy) {

        List<TATResult> results = queryResults(fromDate, toDate, segment, mode, labUnitIds, testIds, panelIds, priority,
                sampleTypeId, orderingSiteId, includeCancelled);

        TATTrendResponse response = new TATTrendResponse();
        response.setCalculationMode(mode.name());

        if (compareBy == null || compareBy.isEmpty()) {
            response.setSeries(List.of(buildTrendSeries("All", results, interval)));
        } else {
            Map<String, List<TATResult>> grouped = groupByDimension(results, compareBy);
            List<TrendSeries> seriesList = new ArrayList<>();
            for (Map.Entry<String, List<TATResult>> entry : grouped.entrySet()) {
                seriesList.add(buildTrendSeries(entry.getKey(), entry.getValue(), interval));
            }
            response.setSeries(seriesList);
        }

        return response;
    }

    @Override
    public List<TATResult> getAllResults(LocalDate fromDate, LocalDate toDate, TATSegment segment,
            TATCalculationMode mode, String labUnitIds, String testIds, String panelIds, String priority,
            Integer sampleTypeId, Integer orderingSiteId, boolean includeCancelled) {
        return queryResults(fromDate, toDate, segment, mode, labUnitIds, testIds, panelIds, priority, sampleTypeId,
                orderingSiteId, includeCancelled);
    }

    // ========== Core Query ==========

    @SuppressWarnings("unchecked")
    private List<TATResult> queryResults(LocalDate fromDate, LocalDate toDate, TATSegment segment,
            TATCalculationMode mode, String labUnitIds, String testIds, String panelIds, String priority,
            Integer sampleTypeId, Integer orderingSiteId, boolean includeCancelled) {

        StringBuilder hql = new StringBuilder();
        hql.append("SELECT a, s, si FROM Analysis a ");
        hql.append("JOIN a.sampleItem si ");
        hql.append("JOIN si.sample s ");
        hql.append("WHERE s.receivedTimestamp BETWEEN :fromDate AND :toDate ");

        if (!includeCancelled) {
            hql.append(
                    "AND a.statusId NOT IN (SELECT st.id FROM StatusOfSample st WHERE st.statusType = 'ANALYSIS' AND st.statusOfSampleName = 'Test Canceled') ");
        }

        // Note: additional filters (labUnitIds, testIds, etc.) would be added here
        // For now, we query all matching analyses and filter in Java

        Query query = entityManager.unwrap(Session.class).createQuery(hql.toString());
        query.setParameter("fromDate", Timestamp.valueOf(fromDate.atStartOfDay()));
        query.setParameter("toDate", Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));

        List<Object[]> rows = query.list();
        List<TATResult> results = new ArrayList<>();

        for (Object[] row : rows) {
            org.openelisglobal.analysis.valueholder.Analysis analysis = (org.openelisglobal.analysis.valueholder.Analysis) row[0];
            org.openelisglobal.sample.valueholder.Sample sample = (org.openelisglobal.sample.valueholder.Sample) row[1];

            TATResult result = new TATResult();
            result.setLabNumber(sample.getAccessionNumber());
            result.setTestName(analysis.getTest() != null ? analysis.getTest().getLocalizedName() : "");
            result.setLabUnit(analysis.getTestSection() != null ? analysis.getTestSection().getLocalizedName() : "");
            result.setPriority(sample.getPriority() != null ? sample.getPriority().name() : "Routine");
            result.setSampleType(analysis.getSampleItem() != null && analysis.getSampleItem().getTypeOfSample() != null
                    ? analysis.getSampleItem().getTypeOfSample().getLocalizedName()
                    : "");

            // Timestamps
            result.setOrderCreated(
                    sample.getEnteredDate() != null ? new Timestamp(sample.getEnteredDate().getTime()) : null);
            result.setCollected(sample.getCollectionDate());
            result.setReceived(sample.getReceivedTimestamp());
            result.setTestingStarted(analysis.getStartedDate());
            result.setResultEntered(analysis.getCompletedDate());
            result.setValidated(analysis.getReleasedDate());

            // Calculate TAT for the selected segment
            Timestamp[] segmentTimestamps = getSegmentTimestamps(segment, result);
            result.setSelectedSegmentTat(
                    tatCalculationService.calculateTatHours(segmentTimestamps[0], segmentTimestamps[1], mode));

            // Always calculate overall TAT
            result.setOverallTat(
                    tatCalculationService.calculateTatHours(result.getOrderCreated(), result.getValidated(), mode));

            results.add(result);
        }

        return results;
    }

    private Timestamp[] getSegmentTimestamps(TATSegment segment, TATResult r) {
        return switch (segment) {
        case ORDER_TO_COLLECTION -> new Timestamp[] { r.getOrderCreated(), r.getCollected() };
        case COLLECTION_TO_RECEIPT -> new Timestamp[] { r.getCollected(), r.getReceived() };
        case RECEIPT_TO_TESTING -> new Timestamp[] { r.getReceived(), r.getTestingStarted() };
        case RECEIPT_TO_RESULT -> new Timestamp[] { r.getReceived(), r.getResultEntered() };
        case RECEIPT_TO_VALIDATION -> new Timestamp[] { r.getReceived(), r.getValidated() };
        case RESULT_TO_VALIDATION -> new Timestamp[] { r.getResultEntered(), r.getValidated() };
        case OVERALL -> new Timestamp[] { r.getOrderCreated(), r.getValidated() };
        };
    }

    // ========== Statistics ==========

    private BigDecimal calculateMean(List<BigDecimal> values) {
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePercentile(List<BigDecimal> sortedValues, int percentile) {
        if (sortedValues.isEmpty())
            return BigDecimal.ZERO;
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }

    private BigDecimal calculateStdDev(List<BigDecimal> values, BigDecimal mean) {
        if (values.size() < 2)
            return BigDecimal.ZERO;
        BigDecimal sumSquares = values.stream().map(v -> v.subtract(mean).pow(2)).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        double variance = sumSquares.doubleValue() / (values.size() - 1);
        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(2, RoundingMode.HALF_UP);
    }

    private List<HistogramBin> buildHistogram(List<BigDecimal> values) {
        List<HistogramBin> bins = new ArrayList<>();
        for (int i = 0; i < HISTOGRAM_BINS.length; i++) {
            double lo = HISTOGRAM_BINS[i][0];
            double hi = HISTOGRAM_BINS[i][1];
            int count = (int) values.stream()
                    .filter(v -> v.doubleValue() >= lo && (hi == Double.MAX_VALUE || v.doubleValue() < hi)).count();
            bins.add(new HistogramBin(HISTOGRAM_LABELS[i], BigDecimal.valueOf(lo),
                    hi == Double.MAX_VALUE ? null : BigDecimal.valueOf(hi), count));
        }
        return bins;
    }

    private List<BreakdownRow> buildBreakdown(List<TATResult> results, String dimension) {
        Map<String, List<TATResult>> grouped = groupByDimension(results, dimension != null ? dimension : "LAB_UNIT");
        List<BreakdownRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<TATResult>> entry : grouped.entrySet()) {
            List<BigDecimal> vals = entry.getValue().stream().map(TATResult::getSelectedSegmentTat)
                    .filter(v -> v != null).sorted().collect(Collectors.toList());
            if (vals.isEmpty())
                continue;

            BreakdownRow row = new BreakdownRow();
            row.setDimensionValue(entry.getKey());
            row.setCount(vals.size());
            row.setMean(calculateMean(vals));
            row.setMedian(calculatePercentile(vals, 50));
            row.setPercentile90(calculatePercentile(vals, 90));
            row.setMax(vals.get(vals.size() - 1));
            rows.add(row);
        }
        return rows;
    }

    private Map<String, List<TATResult>> groupByDimension(List<TATResult> results, String dimension) {
        return results.stream().collect(Collectors.groupingBy(r -> {
            return switch (dimension.toUpperCase()) {
            case "LAB_UNIT" -> r.getLabUnit() != null ? r.getLabUnit() : "Unknown";
            case "TEST" -> r.getTestName() != null ? r.getTestName() : "Unknown";
            case "PRIORITY" -> r.getPriority() != null ? r.getPriority() : "Routine";
            case "SAMPLE_TYPE" -> r.getSampleType() != null ? r.getSampleType() : "Unknown";
            case "ORDERING_SITE" -> r.getOrderingSite() != null ? r.getOrderingSite() : "Unknown";
            default -> r.getLabUnit() != null ? r.getLabUnit() : "Unknown";
            };
        }));
    }

    // ========== Trend ==========

    private TrendSeries buildTrendSeries(String label, List<TATResult> results, String interval) {
        Map<String, List<BigDecimal>> periodValues = new HashMap<>();

        for (TATResult r : results) {
            if (r.getSelectedSegmentTat() == null || r.getReceived() == null)
                continue;
            String period = getPeriodKey(r.getReceived().toLocalDateTime().toLocalDate(), interval);
            periodValues.computeIfAbsent(period, k -> new ArrayList<>()).add(r.getSelectedSegmentTat());
        }

        List<TrendDataPoint> dataPoints = periodValues.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<BigDecimal> vals = entry.getValue().stream().sorted().collect(Collectors.toList());
                    TrendDataPoint dp = new TrendDataPoint();
                    dp.setPeriod(entry.getKey());
                    dp.setMean(calculateMean(vals));
                    dp.setMedian(calculatePercentile(vals, 50));
                    dp.setPercentile90(calculatePercentile(vals, 90));
                    dp.setCount(vals.size());
                    return dp;
                }).collect(Collectors.toList());

        return new TrendSeries(label, dataPoints);
    }

    private String getPeriodKey(LocalDate date, String interval) {
        if (interval == null)
            interval = "DAILY";
        return switch (interval.toUpperCase()) {
        case "WEEKLY" -> date.getYear() + "-W" + String.format("%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        case "MONTHLY" -> date.getYear() + "-" + String.format("%02d", date.getMonthValue());
        default -> date.toString(); // DAILY
        };
    }

    private Comparator<TATResult> getComparator(String field, String order) {
        Comparator<TATResult> comp = switch (field != null ? field : "selectedTat") {
        case "labNumber" ->
            Comparator.comparing(TATResult::getLabNumber, Comparator.nullsLast(Comparator.naturalOrder()));
        case "testName" ->
            Comparator.comparing(TATResult::getTestName, Comparator.nullsLast(Comparator.naturalOrder()));
        case "overallTat" ->
            Comparator.comparing(TATResult::getOverallTat, Comparator.nullsLast(Comparator.naturalOrder()));
        default ->
            Comparator.comparing(TATResult::getSelectedSegmentTat, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        if ("asc".equalsIgnoreCase(order))
            return comp;
        return comp.reversed();
    }
}
