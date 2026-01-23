package org.openelisglobal.notebook.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service for Quality Control (QC) trending, analysis, and reporting.
 *
 * Provides: - QC result tracking (Low, Medium, High concentration levels) -
 * Levey-Jennings trend chart data generation - Out-of-control pattern detection
 * (Westgard rules) - QC pass/fail rate calculations - Statistical analysis
 * (mean, SD, %CV) - Instrument-specific QC trending
 *
 * Used in Stage 3 (Analytical Test Execution) and Stage 4 (Reporting) of
 * bioanalytical workflow.
 */
public interface QCTrendingService {

    /**
     * QC trending data structure containing all statistical information for trend
     * chart.
     */
    record QCTrendingData(String instrumentId, String qcLevel, // LOW, MEDIUM, HIGH
            List<Double> measurements, Double meanValue, Double standardDeviation, Double coefficientOfVariation,
            Double lowerControlLimit, Double upperControlLimit, Double targetValue, Integer passCount,
            Integer failCount, Double passRate, List<String> outOfControlFlags, LocalDate startDate,
            LocalDate endDate) {
    }

    /**
     * Levey-Jennings chart point representation.
     */
    record LJChartPoint(LocalDate date, Double value, String status, boolean outOfControl) {
    }

    /**
     * Out-of-control pattern detection result.
     */
    record OutOfControlResult(String ruleViolated, String description, List<Integer> violatingPointIndices) {
    }

    /**
     * Generate QC trending data for a specific instrument and QC level over a date
     * range.
     *
     * @param instrumentId The instrument ID
     * @param qcLevel      QC level (LOW, MEDIUM, HIGH)
     * @param startDate    Start date for trend analysis
     * @param endDate      End date for trend analysis
     * @return QC trending data with statistics and control limits
     */
    QCTrendingData generateQCTrending(String instrumentId, String qcLevel, LocalDate startDate, LocalDate endDate);

    /**
     * Generate Levey-Jennings chart data points for graphing.
     *
     * @param instrumentId The instrument ID
     * @param qcLevel      QC level (LOW, MEDIUM, HIGH)
     * @param startDate    Start date
     * @param endDate      End date
     * @return List of chart points ready for graphing
     */
    List<LJChartPoint> generateLeveyJenningsChartData(String instrumentId, String qcLevel, LocalDate startDate,
            LocalDate endDate);

    /**
     * Detect out-of-control patterns using Westgard multi-rule QC criteria.
     *
     * Checks: - 1-3S rule: One point beyond 3 standard deviations - 2-2S rule: Two
     * consecutive points beyond 2SD on same side - R-4S rule: Range between two
     * consecutive points exceeds 4SD - 4-1S rule: Four consecutive points beyond
     * 1SD on same side - 10X rule: Ten consecutive points on same side of mean
     *
     * @param trendingData QC trending data to analyze
     * @return List of detected out-of-control patterns
     */
    List<OutOfControlResult> detectOutOfControlPatterns(QCTrendingData trendingData);

    /**
     * Check if QC results are within acceptance criteria and generate pass/fail
     * determination.
     *
     * @param measuredValue          The measured QC concentration
     * @param expectedValue          The expected/target concentration
     * @param acceptanceLowerPercent Lower acceptance limit (e.g., 85 for 85%)
     * @param acceptanceUpperPercent Upper acceptance limit (e.g., 115 for 115%)
     * @return true if pass, false if fail
     */
    boolean isQCResultAcceptable(Double measuredValue, Double expectedValue, Double acceptanceLowerPercent,
            Double acceptanceUpperPercent);

    /**
     * Get QC pass rate statistics for an instrument over a date range.
     *
     * @param instrumentId The instrument ID
     * @param startDate    Start date
     * @param endDate      End date
     * @return Map with pass/fail counts and pass rate percentage
     */
    Map<String, Object> getQCPassRateStatistics(String instrumentId, LocalDate startDate, LocalDate endDate);

    /**
     * Get QC performance summary across all instruments.
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of instruments with their QC performance metrics
     */
    List<Map<String, Object>> getInstrumentQCPerformanceSummary(LocalDate startDate, LocalDate endDate);

    /**
     * Flag a batch of QC results as failed and trigger investigation workflow.
     *
     * @param batchRunNumber     The batch/run number
     * @param reason             Reason for failure
     * @param investigationNotes Initial investigation notes
     * @return Confirmation of flagging
     */
    boolean flagQCFailureForInvestigation(String batchRunNumber, String reason, String investigationNotes);

    /**
     * Get historical QC performance trending for an instrument (entire database
     * history).
     *
     * @param instrumentId   The instrument ID
     * @param numberOfMonths Number of months of historical data to include
     * @return Historical trending data by month
     */
    List<Map<String, Object>> getHistoricalQCTrending(String instrumentId, int numberOfMonths);
}
