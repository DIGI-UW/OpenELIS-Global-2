package org.openelisglobal.analyzer.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job for time-based analyzer status transitions
 * 
 * Task Reference: T151h, T153a, T153b
 * 
 * Transitions analyzers from ACTIVE to OFFLINE after 7 days of inactivity. Runs
 * daily at 2 AM.
 * 
 * Architecture: - This scheduler handles time-based transitions (e.g., 7-day
 * inactivity check) - Event-driven transitions are handled by
 * AnalyzerStatusEventListeners - Core transition logic is in
 * AnalyzerStatusTransitionService
 * 
 * Includes monitoring and alerting for transition failures (T153b).
 * 
 * @see AnalyzerStatusTransitionService
 * @see AnalyzerStatusEventListeners
 */
@Component
public class AnalyzerLifecycleScheduler {

    @Autowired
    private AnalyzerConfigurationService analyzerConfigurationService;

    // Metrics tracking (T153b)
    private int successCount = 0;
    private int failureCount = 0;
    private long executionTime = 0;

    /**
     * Scheduled job to transition analyzers from ACTIVE to OFFLINE after 7 days
     * 
     * Runs daily at 2 AM (cron: "0 0 2 * * ?") Task Reference: T153a
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void transitionToMaintenance() {
        long startTime = System.currentTimeMillis();
        Date jobStartTime = new Date();

        LogEvent.logInfo(this.getClass().getSimpleName(), "transitionToMaintenance",
                "Starting lifecycle transition job at " + jobStartTime);

        // Reset metrics for this execution (T153b)
        int transitionedCount = 0;
        int failedCount = 0;
        List<String> failedAnalyzerIds = new java.util.ArrayList<>();

        try {
            // Get all analyzer configurations in ACTIVE stage
            List<AnalyzerConfiguration> configurations = analyzerConfigurationService.getAllWithAnalyzers();

            Date sevenDaysAgo = getDateSevenDaysAgo();

            for (AnalyzerConfiguration config : configurations) {
                if (config.getStatus() == AnalyzerConfiguration.AnalyzerStatus.ACTIVE
                        && config.getLastActivatedDate() != null
                        && config.getLastActivatedDate().before(sevenDaysAgo)) {
                    try {
                        // Transition to OFFLINE (maintenance mode)
                        config.setStatus(AnalyzerConfiguration.AnalyzerStatus.OFFLINE);
                        config.setLastupdatedFields();
                        analyzerConfigurationService.update(config);

                        transitionedCount++;
                        successCount++;

                        LogEvent.logInfo(this.getClass().getSimpleName(), "transitionToMaintenance",
                                "Transitioned analyzer " + config.getAnalyzer().getId() + " to OFFLINE stage");
                    } catch (Exception e) {
                        // Log error but continue processing other analyzers (T153b requirement)
                        failedCount++;
                        failureCount++;
                        String analyzerId = config.getAnalyzer() != null ? config.getAnalyzer().getId() : "unknown";
                        failedAnalyzerIds.add(analyzerId);

                        LogEvent.logError(
                                "Failed to transition analyzer " + analyzerId + " to OFFLINE: " + e.getMessage(), e);
                    }
                }
            }

            long executionTimeMs = System.currentTimeMillis() - startTime;
            executionTime = executionTimeMs;

            // Log summary with metrics (T153b)
            LogEvent.logInfo(this.getClass().getSimpleName(), "transitionToMaintenance",
                    "Lifecycle transition job completed. Transitioned " + transitionedCount + " analyzers to OFFLINE, "
                            + failedCount + " failures. Execution time: " + executionTimeMs + "ms");

            // Failure notification: If >3 analyzers fail transition, log WARNING with
            // summary (T153b)
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
     * Get transition metrics (T153b)
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
     * Task Reference: T153a - "7 days" refers to calendar days
     */
    private Date getDateSevenDaysAgo() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        return cal.getTime();
    }
}
