package org.openelisglobal.notebook.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service for performance metrics aggregation and reporting dashboard.
 *
 * Provides real-time and historical metrics for the bioanalytical laboratory
 * including: - Sample throughput (received, analyzed, reported) - Turnaround
 * time (TAT) by test type - Analytical run success rates - Instrument
 * utilization - QC performance trends - Study-specific progress tracking
 *
 * Metrics are aggregated from notebook entries and displayed in the Stage 4
 * (Reporting) dashboard.
 */
public interface ReportingMetricsService {

    /**
     * Overall throughput metrics for sample processing.
     */
    record ThroughputMetrics(int samplesReceived, int samplesAnalyzed, int samplesReported, double averageTATDays,
            Map<String, Double> tatByTestType, int analyticalQueueLength, int backlogSamples) {
    }

    /**
     * Quality metrics for analytical performance.
     */
    record QualityMetrics(double qcPassRate, double calibrationAcceptanceRate, double systemSuitabilityPassRate,
            Map<String, InstrumentQuality> instrumentMetrics, List<String> methodValidationStatus) {
    }

    /**
     * Per-instrument quality metrics.
     */
    record InstrumentQuality(String instrumentId, double uptimePercentage, int maintenanceEvents, double successRate,
            LocalDate lastMaintenanceDate) {
    }

    /**
     * Bioequivalence study progress tracking.
     */
    record StudyProgressMetrics(String studyId, String studyTitle, int targetSamples, int completedSamples,
            double progressPercent, String samplingTimepointsCovered, String lastUpdateDate) {
    }

    /**
     * Instrument utilization metrics.
     */
    record InstrumentUtilization(String instrumentId, String instrumentType, double utilizationPercent,
            int totalRunsToday, int totalRunsThisWeek, LocalDate lastUsedDate) {
    }

    /**
     * Dashboard data combining all metric categories.
     */
    record DashboardMetrics(ThroughputMetrics throughput, QualityMetrics quality,
            List<StudyProgressMetrics> studyProgress, List<InstrumentUtilization> instrumentUtilization,
            LocalDate reportDate) {
    }

    /**
     * Calculate throughput metrics for a date range.
     *
     * @param startDate Start date for metrics
     * @param endDate   End date for metrics
     * @return Throughput metrics with TAT calculations
     */
    ThroughputMetrics calculateThroughputMetrics(LocalDate startDate, LocalDate endDate);

    /**
     * Calculate quality metrics for a date range.
     *
     * @param startDate Start date for metrics
     * @param endDate   End date for metrics
     * @return Quality metrics including QC and instrument performance
     */
    QualityMetrics calculateQualityMetrics(LocalDate startDate, LocalDate endDate);

    /**
     * Get bioequivalence study progress metrics.
     *
     * @param studyId The study ID (null for all studies)
     * @return List of study progress metrics
     */
    List<StudyProgressMetrics> getStudyProgressMetrics(String studyId);

    /**
     * Get instrument utilization metrics for current and recent periods.
     *
     * @param numberOfDays Number of days of historical data to include
     * @return List of instrument utilization metrics
     */
    List<InstrumentUtilization> getInstrumentUtilization(int numberOfDays);

    /**
     * Get complete dashboard metrics (all categories combined).
     *
     * @param numberOfDays Number of days for historical data
     * @return Complete dashboard metrics
     */
    DashboardMetrics getDashboardMetrics(int numberOfDays);

    /**
     * Calculate average turnaround time (TAT) for a specific test type.
     *
     * @param testType  The test type (e.g., "Assay", "Dissolution", "Drug
     *                  Concentration (HPLC)")
     * @param startDate Start date
     * @param endDate   End date
     * @return Average TAT in days
     */
    double getAverageTATByTestType(String testType, LocalDate startDate, LocalDate endDate);

    /**
     * Get analytical success/failure rates by test type.
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return Map of test type to success rate percentage
     */
    Map<String, Double> getAnalyticalSuccessRateByTestType(LocalDate startDate, LocalDate endDate);

    /**
     * Get real-time sample queue status.
     *
     * @return Map with queue statistics (pending, in_progress, completed today)
     */
    Map<String, Integer> getSampleQueueStatus();

    /**
     * Get analytical backlog status (samples pending analysis).
     *
     * @return Number of samples in analytical backlog
     */
    int getAnalyticalBacklogCount();

    /**
     * Get external reporting summary (for study sponsors, regulators, medical lab).
     *
     * @param reportType Type of report (BIOEQUIVALENCE, REGULATORY, MEDICAL_LAB,
     *                   RESEARCH)
     * @param startDate  Start date
     * @param endDate    End date
     * @return Report summary with key metrics
     */
    Map<String, Object> getExternalReportingSummary(String reportType, LocalDate startDate, LocalDate endDate);

    /**
     * Generate performance trend data (monthly aggregation for last N months).
     *
     * @param numberOfMonths Number of months of historical data
     * @return List of monthly performance snapshots
     */
    List<Map<String, Object>> getPerformanceTrends(int numberOfMonths);

    /**
     * Get critical alerts/warnings for dashboard display (e.g., failed QC, overdue
     * samples).
     *
     * @return List of alert messages
     */
    List<String> getDashboardAlerts();
}
