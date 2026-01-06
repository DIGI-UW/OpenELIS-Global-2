package org.openelisglobal.notebook.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quality Control (QC) Trending Service Implementation.
 *
 * Provides QC result tracking, statistical analysis, and out-of-control pattern
 * detection using Westgard multi-rule QC criteria. Generates Levey-Jennings
 * trend charts for visualization and quality monitoring.
 *
 * Key features: - QC result aggregation and statistics (mean, SD, %CV) -
 * Levey-Jennings chart data generation - Westgard rule detection (1-3S, 2-2S,
 * R-4S, 4-1S, 10X) - QC pass/fail determination - Historical trending (monthly
 * aggregation)
 *
 * TODO: Integrate with QCResult DAO for database queries
 */
@Service
public class QCTrendingServiceImpl implements QCTrendingService {

    @Override
    public QCTrendingData generateQCTrending(String instrumentId, String qcLevel, LocalDate startDate,
            LocalDate endDate) {

        // TODO: Query QC results from database for date range
        // List<QCResult> qcResults =
        // qcResultService.getQCResultsByInstrumentAndDateRange(
        // instrumentId, qcLevel, startDate, endDate);

        // Placeholder implementation
        List<Double> measurements = new ArrayList<>();
        measurements.add(98.5);
        measurements.add(99.2);
        measurements.add(98.8);
        measurements.add(101.2);
        measurements.add(99.5);
        measurements.add(98.9);
        measurements.add(100.1);
        measurements.add(99.4);
        measurements.add(98.7);
        measurements.add(99.0);

        double meanValue = calculateMean(measurements);
        double standardDeviation = calculateStandardDeviation(measurements, meanValue);
        double coefficientOfVariation = (standardDeviation / meanValue) * 100;

        double lowerControlLimit = meanValue - (3 * standardDeviation);
        double upperControlLimit = meanValue + (3 * standardDeviation);

        long passCount = measurements.stream().filter(v -> v >= lowerControlLimit && v <= upperControlLimit).count();
        long failCount = measurements.size() - passCount;
        double passRate = (double) passCount / measurements.size() * 100;

        return new QCTrendingData(instrumentId, qcLevel, measurements, meanValue, standardDeviation,
                coefficientOfVariation, lowerControlLimit, upperControlLimit, 100.0, // Target value
                (int) passCount, (int) failCount, passRate, new ArrayList<>(), // Out-of-control flags to be populated
                startDate, endDate);
    }

    @Override
    public List<LJChartPoint> generateLeveyJenningsChartData(String instrumentId, String qcLevel, LocalDate startDate,
            LocalDate endDate) {

        QCTrendingData trendingData = generateQCTrending(instrumentId, qcLevel, startDate, endDate);

        List<LJChartPoint> chartPoints = new ArrayList<>();
        LocalDate currentDate = startDate;
        int measurementIndex = 0;

        while (!currentDate.isAfter(endDate) && measurementIndex < trendingData.measurements().size()) {
            Double value = trendingData.measurements().get(measurementIndex);

            String status = "NORMAL";
            if (value < trendingData.lowerControlLimit() || value > trendingData.upperControlLimit()) {
                status = "OUT_OF_CONTROL";
            }

            boolean outOfControl = "OUT_OF_CONTROL".equals(status);
            chartPoints.add(new LJChartPoint(currentDate, value, status, outOfControl));

            currentDate = currentDate.plusDays(1);
            measurementIndex++;
        }

        return chartPoints;
    }

    @Override
    public List<OutOfControlResult> detectOutOfControlPatterns(QCTrendingData trendingData) {

        List<OutOfControlResult> results = new ArrayList<>();
        List<Double> measurements = trendingData.measurements();
        double mean = trendingData.meanValue();
        double sd = trendingData.standardDeviation();

        // 1-3S Rule: One point beyond 3 standard deviations
        for (int i = 0; i < measurements.size(); i++) {
            double value = measurements.get(i);
            if (Math.abs(value - mean) > 3 * sd) {
                results.add(
                        new OutOfControlResult("1-3S", "One point beyond 3 standard deviations from mean", List.of(i)));
            }
        }

        // 2-2S Rule: Two consecutive points beyond 2SD on same side
        for (int i = 0; i < measurements.size() - 1; i++) {
            double value1 = measurements.get(i);
            double value2 = measurements.get(i + 1);

            if ((value1 > mean + 2 * sd && value2 > mean + 2 * sd)
                    || (value1 < mean - 2 * sd && value2 < mean - 2 * sd)) {
                results.add(new OutOfControlResult("2-2S", "Two consecutive points beyond 2 SD on same side of mean",
                        List.of(i, i + 1)));
            }
        }

        // R-4S Rule: Range exceeds 4 SD
        for (int i = 0; i < measurements.size() - 1; i++) {
            double range = Math.abs(measurements.get(i) - measurements.get(i + 1));
            if (range > 4 * sd) {
                results.add(new OutOfControlResult("R-4S", "Range between two consecutive points exceeds 4 SD",
                        List.of(i, i + 1)));
            }
        }

        // 4-1S Rule: Four consecutive points beyond 1SD on same side
        for (int i = 0; i < measurements.size() - 3; i++) {
            boolean allAbove = measurements.get(i) > mean + sd && measurements.get(i + 1) > mean + sd
                    && measurements.get(i + 2) > mean + sd && measurements.get(i + 3) > mean + sd;

            boolean allBelow = measurements.get(i) < mean - sd && measurements.get(i + 1) < mean - sd
                    && measurements.get(i + 2) < mean - sd && measurements.get(i + 3) < mean - sd;

            if (allAbove || allBelow) {
                results.add(new OutOfControlResult("4-1S", "Four consecutive points beyond 1 SD on same side of mean",
                        List.of(i, i + 1, i + 2, i + 3)));
            }
        }

        // 10X Rule: Ten consecutive points on same side of mean
        for (int i = 0; i < measurements.size() - 9; i++) {
            boolean allAbove = measurements.subList(i, i + 10).stream().allMatch(v -> v > mean);
            boolean allBelow = measurements.subList(i, i + 10).stream().allMatch(v -> v < mean);

            if (allAbove || allBelow) {
                List<Integer> indices = new ArrayList<>();
                for (int j = 0; j < 10; j++) {
                    indices.add(i + j);
                }
                results.add(new OutOfControlResult("10X", "Ten consecutive points on same side of mean", indices));
            }
        }

        return results;
    }

    @Override
    public boolean isQCResultAcceptable(Double measuredValue, Double expectedValue, Double acceptanceLowerPercent,
            Double acceptanceUpperPercent) {

        double lowerBound = expectedValue * acceptanceLowerPercent / 100;
        double upperBound = expectedValue * acceptanceUpperPercent / 100;

        return measuredValue >= lowerBound && measuredValue <= upperBound;
    }

    @Override
    public Map<String, Object> getQCPassRateStatistics(String instrumentId, LocalDate startDate, LocalDate endDate) {

        // TODO: Query QC results from database
        Map<String, Object> stats = new HashMap<>();
        stats.put("instrumentId", instrumentId);
        stats.put("startDate", startDate);
        stats.put("endDate", endDate);
        stats.put("totalQCResults", 0);
        stats.put("passCount", 0);
        stats.put("failCount", 0);
        stats.put("passRatePercent", 0.0);

        return stats;
    }

    @Override
    public List<Map<String, Object>> getInstrumentQCPerformanceSummary(LocalDate startDate, LocalDate endDate) {

        // TODO: Query all instruments and aggregate QC performance
        List<Map<String, Object>> summary = new ArrayList<>();

        // Placeholder
        Map<String, Object> instrument1 = new HashMap<>();
        instrument1.put("instrumentId", "HPLC-001");
        instrument1.put("instrumentType", "HPLC");
        instrument1.put("qcPassRate", 98.5);
        instrument1.put("qcFailRate", 1.5);
        summary.add(instrument1);

        Map<String, Object> instrument2 = new HashMap<>();
        instrument2.put("instrumentId", "LCMSMS-001");
        instrument2.put("instrumentType", "LC-MS/MS");
        instrument2.put("qcPassRate", 99.2);
        instrument2.put("qcFailRate", 0.8);
        summary.add(instrument2);

        return summary;
    }

    @Override
    @Transactional
    public boolean flagQCFailureForInvestigation(String batchRunNumber, String reason, String investigationNotes) {

        // TODO: Create QC failure investigation record
        // Call service to persist investigation request
        return true;
    }

    @Override
    public List<Map<String, Object>> getHistoricalQCTrending(String instrumentId, int numberOfMonths) {

        // TODO: Query historical QC data by month and aggregate
        List<Map<String, Object>> historicalTrending = new ArrayList<>();

        LocalDate endDate = LocalDate.now();
        for (int m = 0; m < numberOfMonths; m++) {
            LocalDate monthStart = endDate.minusMonths(m).withDayOfMonth(1);
            LocalDate monthEnd = endDate.minusMonths(m).plusMonths(1).withDayOfMonth(1).minusDays(1);

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthStart.getMonth());
            monthData.put("year", monthStart.getYear());
            monthData.put("startDate", monthStart);
            monthData.put("endDate", monthEnd);
            monthData.put("averageQCPassRate", 98.5);
            monthData.put("totalQCResults", 0);

            historicalTrending.add(monthData);
        }

        return historicalTrending;
    }

    // ==================== Private Helper Methods ====================

    private double calculateMean(List<Double> values) {
        if (values.isEmpty())
            return 0.0;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateStandardDeviation(List<Double> values, double mean) {
        if (values.isEmpty())
            return 0.0;

        double sumOfSquaredDifferences = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum();

        double variance = sumOfSquaredDifferences / values.size();
        return Math.sqrt(variance);
    }
}
