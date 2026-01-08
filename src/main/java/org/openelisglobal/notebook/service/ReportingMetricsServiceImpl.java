package org.openelisglobal.notebook.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Reporting Metrics Service Implementation.
 *
 * Aggregates performance metrics for bioanalytical laboratory dashboards and
 * external reporting. Provides: - Sample throughput (received, analyzed,
 * reported) - Turnaround time (TAT) by test type - Quality metrics (QC pass
 * rates, calibration acceptance, instrument uptime) - Bioequivalence study
 * progress tracking - Instrument utilization metrics - External reporting
 * summaries (regulatory, LMIS, sponsors) - Performance trending (monthly
 * snapshots) - Dashboard alerts
 *
 * TODO: Integrate with NotebookEntry, QCResult, Sample DAOs for database
 * queries
 */
@Service
public class ReportingMetricsServiceImpl implements ReportingMetricsService {

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private WestgardRulesService westgardRulesService;

    @Override
    public ThroughputMetrics calculateThroughputMetrics(LocalDate startDate, LocalDate endDate) {

        // TODO: Query from notebook_entry and sample tables
        // Count samples by status (RECEIVED, IN_PROGRESS, REPORTED)
        // Calculate TAT = (reported_date - receipt_date)

        // Placeholder implementation
        int samplesReceived = 150;
        int samplesAnalyzed = 120;
        int samplesReported = 100;
        double averageTATDays = 3.5;

        Map<String, Double> tatByTestType = new HashMap<>();
        tatByTestType.put("Assay", 2.1);
        tatByTestType.put("Dissolution", 3.5);
        tatByTestType.put("Drug Concentration (HPLC)", 1.8);
        tatByTestType.put("Drug Concentration (LC-MS/MS)", 4.2);
        tatByTestType.put("Hardness", 1.2);
        tatByTestType.put("Friability", 1.3);
        tatByTestType.put("Disintegration", 1.5);
        tatByTestType.put("Identity Test", 2.0);

        int analyticalQueueLength = 20;
        int backlogSamples = 5;

        return new ThroughputMetrics(samplesReceived, samplesAnalyzed, samplesReported, averageTATDays, tatByTestType,
                analyticalQueueLength, backlogSamples);
    }

    @Override
    public QualityMetrics calculateQualityMetrics(LocalDate startDate, LocalDate endDate) {

        // TODO: Query from qc_result, calibration_curve tables
        // Calculate pass rates, acceptance rates, instrument uptime

        // Placeholder implementation
        double qcPassRate = 98.5;
        double calibrationAcceptanceRate = 99.2;
        double systemSuitabilityPassRate = 99.0;

        Map<String, InstrumentQuality> instrumentMetrics = new HashMap<>();
        instrumentMetrics.put("HPLC-001",
                new InstrumentQuality("HPLC-001", 97.8, 2, 99.1, LocalDate.now().minusDays(3)));
        instrumentMetrics.put("LCMSMS-001",
                new InstrumentQuality("LCMSMS-001", 98.5, 1, 99.5, LocalDate.now().minusDays(1)));

        List<String> methodValidationStatus = List.of("HPLC UV-Vis: VALIDATED", "LC-MS/MS: VALIDATED",
                "Dissolution USP II: PENDING_VALIDATION", "Assay: VALIDATED");

        return new QualityMetrics(qcPassRate, calibrationAcceptanceRate, systemSuitabilityPassRate, instrumentMetrics,
                methodValidationStatus);
    }

    @Override
    public List<StudyProgressMetrics> getStudyProgressMetrics(String studyId) {

        // TODO: Query from bioequivalence_study and be_sample_assignment tables
        List<StudyProgressMetrics> studyProgress = new ArrayList<>();

        StudyProgressMetrics study1 = new StudyProgressMetrics("BE-2025-001", "Bioequivalence of Formulation X", 60, 45,
                75.0, "0h, 0.5h, 1h, 2h, 3h (18/20 timepoints)", LocalDate.now().toString());

        StudyProgressMetrics study2 = new StudyProgressMetrics("BE-2025-002", "Bioequivalence of Formulation Y", 40, 32,
                80.0, "Pre-dose, 1h, 2h, 4h, 8h (16/20 timepoints)", LocalDate.now().minusDays(1).toString());

        studyProgress.add(study1);
        studyProgress.add(study2);

        return studyProgress;
    }

    @Override
    public List<InstrumentUtilization> getInstrumentUtilization(int numberOfDays) {

        // TODO: Query from notebook_page_entry and analytical_execution tables
        // Calculate: total runs, hours in use, maintenance events

        List<InstrumentUtilization> utilization = new ArrayList<>();

        InstrumentUtilization hplc = new InstrumentUtilization("HPLC-001", "HPLC", 87.5, 12, 65, LocalDate.now());

        InstrumentUtilization lcmsms = new InstrumentUtilization("LCMSMS-001", "LC-MS/MS", 92.1, 18, 98,
                LocalDate.now());

        InstrumentUtilization dissolution = new InstrumentUtilization("DISSOLUTION-001", "Dissolution Apparatus", 75.2,
                8, 45, LocalDate.now().minusDays(1));

        utilization.add(hplc);
        utilization.add(lcmsms);
        utilization.add(dissolution);

        return utilization;
    }

    @Override
    public DashboardMetrics getDashboardMetrics(int numberOfDays) {

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(numberOfDays);

        ThroughputMetrics throughput = calculateThroughputMetrics(startDate, endDate);
        QualityMetrics quality = calculateQualityMetrics(startDate, endDate);
        List<StudyProgressMetrics> studyProgress = getStudyProgressMetrics(null);
        List<InstrumentUtilization> instrumentUtilization = getInstrumentUtilization(numberOfDays);

        return new DashboardMetrics(throughput, quality, studyProgress, instrumentUtilization, LocalDate.now());
    }

    @Override
    public double getAverageTATByTestType(String testType, LocalDate startDate, LocalDate endDate) {

        // TODO: Query samples by test type and calculate average TAT
        Map<String, Double> tatByType = new HashMap<>();
        tatByType.put("Assay", 2.1);
        tatByType.put("Dissolution", 3.5);
        tatByType.put("Drug Concentration (HPLC)", 1.8);
        tatByType.put("Drug Concentration (LC-MS/MS)", 4.2);
        tatByType.put("Hardness", 1.2);
        tatByType.put("Friability", 1.3);
        tatByType.put("Disintegration", 1.5);
        tatByType.put("Identity Test", 2.0);

        return tatByType.getOrDefault(testType, 0.0);
    }

    @Override
    public Map<String, Double> getAnalyticalSuccessRateByTestType(LocalDate startDate, LocalDate endDate) {

        // TODO: Query notebook_entry by test type and calculate success rates
        Map<String, Double> successRates = new HashMap<>();
        successRates.put("Assay", 99.5);
        successRates.put("Dissolution", 98.2);
        successRates.put("Drug Concentration (HPLC)", 99.8);
        successRates.put("Drug Concentration (LC-MS/MS)", 99.1);
        successRates.put("Hardness", 100.0);
        successRates.put("Friability", 99.7);
        successRates.put("Disintegration", 99.9);
        successRates.put("Identity Test", 99.4);

        return successRates;
    }

    @Override
    public Map<String, Integer> getSampleQueueStatus() {

        // TODO: Query current sample queue
        Map<String, Integer> queueStatus = new HashMap<>();
        queueStatus.put("pending", 20);
        queueStatus.put("in_progress", 15);
        queueStatus.put("completed_today", 25);

        return queueStatus;
    }

    @Override
    public int getAnalyticalBacklogCount() {

        // TODO: Query samples waiting for analysis
        return 5;
    }

    @Override
    public Map<String, Object> getExternalReportingSummary(String reportType, LocalDate startDate, LocalDate endDate) {

        // TODO: Aggregate data specific to report type
        Map<String, Object> summary = new HashMap<>();
        summary.put("reportType", reportType);
        summary.put("startDate", startDate);
        summary.put("endDate", endDate);
        summary.put("generatedDate", LocalDate.now());

        switch (reportType.toUpperCase()) {
        case "BIOEQUIVALENCE":
            summary.put("totalSamples", 150);
            summary.put("studiesCompleted", 3);
            summary.put("studiesInProgress", 2);
            summary.put("qcPassRate", 98.5);
            break;

        case "REGULATORY":
            summary.put("totalAnalyses", 500);
            summary.put("validatedMethods", 8);
            summary.put("cdisc_sdtm_compliant", true);
            summary.put("auditTrailComplete", true);
            break;

        case "MEDICAL_LAB":
            summary.put("resultsSubmitted", 120);
            summary.put("averageTAT", 3.5);
            summary.put("lmisIntegrationStatus", "ACTIVE");
            break;

        case "RESEARCH":
            summary.put("projectsSupported", 12);
            summary.put("samplesAnalyzed", 450);
            summary.put("publicationsSupported", 5);
            break;
        }

        return summary;
    }

    @Override
    public List<Map<String, Object>> getPerformanceTrends(int numberOfMonths) {

        // TODO: Query historical data and aggregate by month
        List<Map<String, Object>> trends = new ArrayList<>();

        LocalDate endDate = LocalDate.now();
        for (int m = 0; m < numberOfMonths; m++) {
            LocalDate monthStart = endDate.minusMonths(m).withDayOfMonth(1);

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthStart.getMonth());
            monthData.put("year", monthStart.getYear());
            monthData.put("samplesReceived", 150 - (m * 5));
            monthData.put("samplesAnalyzed", 120 - (m * 4));
            monthData.put("samplesReported", 100 - (m * 3));
            monthData.put("averageTAT", 3.5 + (m * 0.1));
            monthData.put("qcPassRate", 98.5 - (m * 0.2));

            trends.add(monthData);
        }

        return trends;
    }

    @Override
    public List<String> getDashboardAlerts() {

        // TODO: Query for problematic conditions
        List<String> alerts = new ArrayList<>();

        // Check for failed QC
        // if (failedQCCount > threshold) {
        // alerts.add("QC FAILURE: " + failedQCCount + " QC results failed in last 24
        // hours");
        // }

        // Check for overdue samples
        // if (overdueCount > 0) {
        // alerts.add("OVERDUE: " + overdueCount + " samples overdue for analysis");
        // }

        // Check for instrument issues
        // if (instrumentDowntime > threshold) {
        // alerts.add("INSTRUMENT: " + instrumentName + " offline for " + hours + "
        // hours");
        // }

        // Placeholder - no alerts
        return alerts;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> calculateBioequivalenceStatistics(Integer pageId) {

        if (pageId == null) {
            return null;
        }

        // Get all samples for this page from the database
        List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(pageId);

        if (pageSamples == null || pageSamples.isEmpty()) {
            return null;
        }

        // Collect all QC results from all samples' data
        List<Map<String, Object>> allQcResults = new ArrayList<>();
        String analyticalMethod = null;
        int dataPoints = 0;

        for (NotebookPageSample pageSample : pageSamples) {
            Map<String, Object> sampleData = pageSample.getData();

            if (sampleData == null) {
                continue;
            }

            // Get analytical method from first sample that has it
            if (analyticalMethod == null && sampleData.containsKey("analyticalMethod")) {
                analyticalMethod = (String) sampleData.get("analyticalMethod");
            }

            // Extract QC results
            if (sampleData.containsKey("qcResults")) {
                Object qcResultsObj = sampleData.get("qcResults");
                if (qcResultsObj instanceof List<?>) {
                    List<Map<String, Object>> qcResults = (List<Map<String, Object>>) qcResultsObj;
                    allQcResults.addAll(qcResults);
                    dataPoints += qcResults.size();
                }
            }
        }

        if (allQcResults.isEmpty()) {
            return null;
        }

        // Extract accuracy values for statistical calculations
        List<Double> accuracyValues = new ArrayList<>();
        List<Double> concentrationValues = new ArrayList<>();

        for (Map<String, Object> qcResult : allQcResults) {
            // Extract accuracy
            if (qcResult.containsKey("accuracy")) {
                Object accuracyObj = qcResult.get("accuracy");
                Double accuracy = null;
                if (accuracyObj instanceof Number) {
                    accuracy = ((Number) accuracyObj).doubleValue();
                } else if (accuracyObj instanceof String) {
                    try {
                        accuracy = Double.parseDouble((String) accuracyObj);
                    } catch (NumberFormatException e) {
                        // Skip invalid values
                    }
                }
                if (accuracy != null && accuracy > 0) {
                    accuracyValues.add(accuracy);
                }
            }

            // Extract concentration (measuredValue)
            if (qcResult.containsKey("measuredValue")) {
                Object concentrationObj = qcResult.get("measuredValue");
                Double concentration = null;
                if (concentrationObj instanceof Number) {
                    concentration = ((Number) concentrationObj).doubleValue();
                } else if (concentrationObj instanceof String) {
                    try {
                        concentration = Double.parseDouble((String) concentrationObj);
                    } catch (NumberFormatException e) {
                        // Skip invalid values
                    }
                }
                if (concentration != null && concentration > 0) {
                    concentrationValues.add(concentration);
                }
            }
        }

        if (accuracyValues.isEmpty()) {
            return null;
        }

        // Calculate statistics from accuracy values
        double meanAccuracy = accuracyValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double accuracyVariance = accuracyValues.stream().mapToDouble(value -> Math.pow(value - meanAccuracy, 2)).sum()
                / accuracyValues.size();
        double accuracySd = Math.sqrt(accuracyVariance);
        double accuracyCv = (accuracySd / meanAccuracy) * 100;

        // Calculate concentration statistics if available
        Double concentrationMean = null;
        Double concentrationSd = null;
        Double concentrationCv = null;
        Double minConcentration = null;
        Double maxConcentration = null;

        if (!concentrationValues.isEmpty()) {
            concentrationMean = concentrationValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            final double finalConcentrationMean = concentrationMean; // Make it effectively final for lambda
            double concentrationVariance = concentrationValues.stream()
                    .mapToDouble(value -> Math.pow(value - finalConcentrationMean, 2)).sum()
                    / concentrationValues.size();
            concentrationSd = Math.sqrt(concentrationVariance);
            concentrationCv = (concentrationSd / concentrationMean) * 100;
            minConcentration = concentrationValues.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            maxConcentration = concentrationValues.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        }

        // Evaluate Westgard rules for QC validation
        WestgardRulesService.WestgardEvaluation westgardEvaluation = null;
        boolean qcPassed = true;
        try {
            westgardEvaluation = westgardRulesService
                    .evaluateWestgardRules(westgardRulesService.extractQCControlsFromResults(allQcResults));
            qcPassed = "PASS".equals(westgardEvaluation.overallStatus());
        } catch (Exception e) {
            // Log error but don't fail the statistics calculation
            System.err.println("Westgard rules evaluation failed: " + e.getMessage());
        }

        // Determine regulatory compliance (FDA Bioequivalence criteria + Westgard
        // rules)
        boolean isCompliant = accuracyCv < 20 && meanAccuracy >= 80 && meanAccuracy <= 120 && qcPassed;

        // Build response matching frontend expectations
        Map<String, Object> statistics = new HashMap<>();

        // Use concentration stats if available, otherwise accuracy stats
        if (concentrationMean != null) {
            statistics.put("mean", String.format("%.1f", concentrationMean));
            statistics.put("sd", String.format("%.2f", concentrationSd));
            statistics.put("cv", String.format("%.1f%%", concentrationCv));
            statistics.put("min", String.format("%.1f", minConcentration));
            statistics.put("max", String.format("%.1f", maxConcentration));
        } else {
            statistics.put("mean", String.format("%.1f%%", meanAccuracy));
            statistics.put("sd", String.format("%.2f", accuracySd));
            statistics.put("cv", String.format("%.1f%%", accuracyCv));
            statistics.put("min", String.format("%.1f%%",
                    accuracyValues.stream().mapToDouble(Double::doubleValue).min().orElse(0.0)));
            statistics.put("max", String.format("%.1f%%",
                    accuracyValues.stream().mapToDouble(Double::doubleValue).max().orElse(0.0)));
        }

        statistics.put("testName", analyticalMethod != null ? analyticalMethod : "Unknown Type");
        statistics.put("dataPoints", dataPoints);
        statistics.put("meanAccuracy", String.format("%.1f%%", meanAccuracy));
        statistics.put("regulatoryStatus", isCompliant ? "COMPLIANT" : "NON_COMPLIANT");
        statistics.put("pageId", pageId);
        statistics.put("samplesAnalyzed", pageSamples.size());
        statistics.put("calculatedAt", LocalDate.now().toString());

        // Add Westgard rules evaluation results
        if (westgardEvaluation != null) {
            Map<String, Object> qcValidation = new HashMap<>();
            qcValidation.put("westgardStatus", westgardEvaluation.overallStatus());
            qcValidation.put("westgardRecommendation", westgardEvaluation.recommendation());
            qcValidation.put("rulesEvaluated", westgardEvaluation.ruleResults().size());
            qcValidation.put("rulesPassed", westgardEvaluation.passedRules());
            qcValidation.put("rulesFailed", westgardEvaluation.failedRules());

            // Summary of specific rule results
            List<Map<String, Object>> rulesSummary = new ArrayList<>();
            for (WestgardRulesService.WestgardRuleResult ruleResult : westgardEvaluation.ruleResults()) {
                Map<String, Object> ruleSummary = new HashMap<>();
                ruleSummary.put("ruleCode", ruleResult.ruleCode());
                ruleSummary.put("ruleName", ruleResult.ruleName());
                ruleSummary.put("status", ruleResult.status());
                ruleSummary.put("message", ruleResult.message());
                rulesSummary.add(ruleSummary);
            }
            qcValidation.put("ruleResults", rulesSummary);

            statistics.put("qcValidation", qcValidation);
        }

        return statistics;
    }
}
