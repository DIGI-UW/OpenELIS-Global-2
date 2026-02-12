package org.openelisglobal.analyzer.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job for time-based analyzer status transitions
 * 
 * 
 * Transitions analyzers from ACTIVE to OFFLINE after 7 days of inactivity. Runs
 * daily at 2 AM.
 * 
 * Architecture: - This scheduler handles time-based transitions (e.g., 7-day
 * inactivity check) - Event-driven transitions are handled by
 * AnalyzerStatusEventListeners - Core transition logic is in
 * AnalyzerStatusTransitionService
 * 
 * Includes monitoring and alerting for transition failures.
 * 
 * @see AnalyzerStatusTransitionService
 * @see AnalyzerStatusEventListeners
 */
@Component
public class AnalyzerLifecycleScheduler {

    @Autowired
    private AnalyzerService analyzerService;

    // Metrics tracking
    private int successCount = 0;
    private int failureCount = 0;
    private long executionTime = 0;

    /**
     * Scheduled job to transition analyzers from ACTIVE to OFFLINE after 7 days
     * 
     * Runs daily at 2 AM (cron: "0 0 2 * * ?")
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void transitionToMaintenance() {
        long startTime = System.currentTimeMillis();
        Date jobStartTime = new Date();

        LogEvent.logInfo(this.getClass().getSimpleName(), "transitionToMaintenance",
                "Starting lifecycle transition job at " + jobStartTime);

        // Reset metrics for this execution
        int transitionedCount = 0;
        int failedCount = 0;
        List<String> failedAnalyzerIds = new java.util.ArrayList<>();

        try {
            // Get all analyzers in ACTIVE stage
            List<Analyzer> analyzers = analyzerService.getAll();

            Date sevenDaysAgo = getDateSevenDaysAgo();

            for (Analyzer analyzer : analyzers) {
                if (analyzer.getStatus() == AnalyzerStatus.ACTIVE && analyzer.getLastActivatedDate() != null
                        && analyzer.getLastActivatedDate().before(sevenDaysAgo)) {
                    try {
                        // Transition to OFFLINE (maintenance mode)
                        analyzer.setStatus(AnalyzerStatus.OFFLINE);
                        analyzer.setLastupdatedFields();
                        analyzerService.update(analyzer);

                        transitionedCount++;
                        successCount++;

                        LogEvent.logInfo(this.getClass().getSimpleName(), "transitionToMaintenance",
                                "Transitioned analyzer " + analyzer.getId() + " to OFFLINE stage");
                    } catch (Exception e) {
                        // Log error but continue processing other analyzers
                        failedCount++;
                        failureCount++;
                        String analyzerId = analyzer.getId() != null ? analyzer.getId() : "unknown";
                        failedAnalyzerIds.add(analyzerId);

                        LogEvent.logError(
                                "Failed to transition analyzer " + analyzerId + " to OFFLINE: " + e.getMessage(), e);
                    }
                }
            }

            long executionTimeMs = System.currentTimeMillis() - startTime;
            executionTime = executionTimeMs;

            // Log summary with metrics
            LogEvent.logInfo(this.getClass().getSimpleName(), "transitionToMaintenance",
                    "Lifecycle transition job completed. Transitioned " + transitionedCount + " analyzers to OFFLINE, "
                            + failedCount + " failures. Execution time: " + executionTimeMs + "ms");

            // Failure notification: If >3 analyzers fail transition, log WARNING with
            // summary
            if (failedCount > 3) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "transitionToMaintenance",
                        "WARNING: " + failedCount + " analyzers failed transition to OFFLINE. "
                                + "Failed analyzer IDs: " + String.join(", ", failedAnalyzerIds));
            }

        } catch (Exception e) {
            failureCount++;
            long executionTimeMs = System.currentTimeMillis() - startTime;
            executionTime = executionTimeMs;

            LogEvent.logError("Error in lifecycle transition job: " + e.getMessage() + ". Execution time: "
                    + executionTimeMs + "ms", e);
        }
    }

    /**
     * Get transition metrics
     * 
     * @return Map with success count, failure count, and execution time
     */
    public java.util.Map<String, Object> getMetrics() {
        java.util.Map<String, Object> metrics = new java.util.HashMap<>();
        metrics.put("successCount", successCount);
        metrics.put("failureCount", failureCount);
        metrics.put("executionTime", executionTime);
        return metrics;
    }

    /**
     * Get date 7 days ago (calendar days)
     * 
     */
    private Date getDateSevenDaysAgo() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        return cal.getTime();
    }
}
