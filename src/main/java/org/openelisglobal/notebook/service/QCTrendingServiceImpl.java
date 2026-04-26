package org.openelisglobal.notebook.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private org.openelisglobal.medlab.dao.QCResultDAO qcResultDAO;

    @Autowired
    private WestgardRulesService westgardRulesService;

    @Override
    public QCTrendingData generateQCTrending(String instrumentId, String qcLevel, LocalDate startDate,
            LocalDate endDate) {

        Integer analyzerId = null;
        try {
            analyzerId = Integer.parseInt(instrumentId);
        } catch (NumberFormatException e) {
            // If not numeric, we might need a mapping or just return empty
        }

        List<org.openelisglobal.medlab.valueholder.QCResult> qcResults = new ArrayList<>();
        if (analyzerId != null) {
            qcResults = qcResultDAO.getQCResultsByAnalyzerAndDateRange(analyzerId,
                    java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate));
        }

        // Filter by QC level if provided
        final String level = qcLevel;
        List<org.openelisglobal.medlab.valueholder.QCResult> filteredResults = qcResults.stream()
                .filter(res -> level == null || res.getQcLevel().name().equalsIgnoreCase(level))
                .toList();

        List<Double> measurements = filteredResults.stream()
                .map(res -> res.getResultValue().doubleValue())
                .toList();

        if (measurements.isEmpty()) {
            return new QCTrendingData(instrumentId, qcLevel, new ArrayList<>(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0,
                    new ArrayList<>(), startDate, endDate);
        }

        double meanValue = calculateMean(measurements);
        double standardDeviation = calculateStandardDeviation(measurements, meanValue);
        double coefficientOfVariation = meanValue != 0 ? (standardDeviation / meanValue) * 100 : 0.0;

        // Use target value from the latest QC result if available
        double targetValue = filteredResults.get(filteredResults.size() - 1).getTargetValue().doubleValue();

        double lowerControlLimit = meanValue - (3 * standardDeviation);
        double upperControlLimit = meanValue + (3 * standardDeviation);

        long passCount = filteredResults.stream()
                .filter(res -> res.getPassFail() == org.openelisglobal.medlab.valueholder.QCResult.PassFail.PASS)
                .count();
        long failCount = filteredResults.size() - passCount;
        double passRate = (double) passCount / filteredResults.size() * 100;

        QCTrendingData trendingData = new QCTrendingData(instrumentId, qcLevel, measurements, meanValue,
                standardDeviation, coefficientOfVariation, lowerControlLimit, upperControlLimit, targetValue,
                (int) passCount, (int) failCount, passRate, new ArrayList<>(), startDate, endDate);

        // Detect Westgard patterns
        List<OutOfControlResult> patterns = detectOutOfControlPatterns(trendingData);
        List<String> flags = patterns.stream().map(p -> p.ruleViolated() + ": " + p.description()).toList();

        return new QCTrendingData(instrumentId, qcLevel, measurements, meanValue, standardDeviation,
                coefficientOfVariation, lowerControlLimit, upperControlLimit, targetValue, (int) passCount,
                (int) failCount, passRate, flags, startDate, endDate);
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
        Integer analyzerId = null;
        try {
            analyzerId = Integer.parseInt(instrumentId);
        } catch (NumberFormatException e) {}

        List<org.openelisglobal.medlab.valueholder.QCResult> results = new ArrayList<>();
        if (analyzerId != null) {
            results = qcResultDAO.getQCResultsByAnalyzerAndDateRange(analyzerId,
                java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate));
        }

        long passCount = results.stream().filter(r -> r.getPassFail() == org.openelisglobal.medlab.valueholder.QCResult.PassFail.PASS).count();
        long total = results.size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("instrumentId", instrumentId);
        stats.put("startDate", startDate);
        stats.put("endDate", endDate);
        stats.put("totalQCResults", total);
        stats.put("passCount", (int)passCount);
        stats.put("failCount", (int)(total - passCount));
        stats.put("passRatePercent", total > 0 ? (double) passCount / total * 100 : 0.0);

        return stats;
    }

    @Override
    public List<Map<String, Object>> getInstrumentQCPerformanceSummary(LocalDate startDate, LocalDate endDate) {
        // This would ideally involve an aggregation query, for now 
        // we can fetch recent records and aggregate in memory or use a stored procedure view
        List<Map<String, Object>> summary = new ArrayList<>();
        
        // Let's assume we have a way to get all distinct analyzer IDs that have QC results in this range
        // For now, return what we have in the DB 
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
        List<Map<String, Object>> historicalTrending = new ArrayList<>();
        LocalDate endDate = LocalDate.now();
        
        Integer analyzerId = null;
        try { analyzerId = Integer.parseInt(instrumentId); } catch (Exception e) {}

        for (int m = 0; m < numberOfMonths; m++) {
            LocalDate monthStart = endDate.minusMonths(m).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            List<org.openelisglobal.medlab.valueholder.QCResult> results = new ArrayList<>();
            if (analyzerId != null) {
                results = qcResultDAO.getQCResultsByAnalyzerAndDateRange(analyzerId,
                    java.sql.Date.valueOf(monthStart), java.sql.Date.valueOf(monthEnd));
            }

            long passCount = results.stream().filter(r -> r.getPassFail() == org.openelisglobal.medlab.valueholder.QCResult.PassFail.PASS).count();
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthStart.getMonth());
            monthData.put("year", monthStart.getYear());
            monthData.put("startDate", monthStart);
            monthData.put("endDate", monthEnd);
            monthData.put("averageQCPassRate", results.size() > 0 ? (double)passCount/results.size()*100 : 100.0);
            monthData.put("totalQCResults", results.size());

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
