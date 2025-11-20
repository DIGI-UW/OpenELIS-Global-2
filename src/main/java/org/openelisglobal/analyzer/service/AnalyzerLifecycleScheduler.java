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
 * Scheduled job for analyzer lifecycle stage transitions
 * 
 * Task Reference: T153a, T153b
 * 
 * Transitions analyzers from GO_LIVE to MAINTENANCE after 7 days of being
 * active. Runs daily at 2 AM.
 * 
 * Includes monitoring and alerting for transition failures (T153b).
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
     * Scheduled job to transition analyzers from GO_LIVE to MAINTENANCE after 7
     * days
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
            // Get all analyzer configurations in GO_LIVE stage
            List<AnalyzerConfiguration> configurations = analyzerConfigurationService.getAllWithAnalyzers();

            Date sevenDaysAgo = getDateSevenDaysAgo();

            for (AnalyzerConfiguration config : configurations) {
                if (config.getLifecycleStage() == AnalyzerConfiguration.LifecycleStage.GO_LIVE
                        && config.getLastActivatedDate() != null
                        && config.getLastActivatedDate().before(sevenDaysAgo)) {
                    try {
                        // Transition to MAINTENANCE
                        config.setLifecycleStage(AnalyzerConfiguration.LifecycleStage.MAINTENANCE);
                        config.setLastupdatedFields();
                        analyzerConfigurationService.update(config);

                        transitionedCount++;
                        successCount++;

                        LogEvent.logInfo(this.getClass().getSimpleName(), "transitionToMaintenance",
                                "Transitioned analyzer " + config.getAnalyzer().getId() + " to MAINTENANCE stage");
                    } catch (Exception e) {
                        // Log error but continue processing other analyzers (T153b requirement)
                        failedCount++;
                        failureCount++;
                        String analyzerId = config.getAnalyzer() != null ? config.getAnalyzer().getId() : "unknown";
                        failedAnalyzerIds.add(analyzerId);

                        LogEvent.logError(
                                "Failed to transition analyzer " + analyzerId + " to MAINTENANCE: " + e.getMessage(),
                                e);
                    }
                }
            }

            long executionTimeMs = System.currentTimeMillis() - startTime;
            executionTime = executionTimeMs;

            // Log summary with metrics (T153b)
            LogEvent.logInfo(this.getClass().getSimpleName(), "transitionToMaintenance",
                    "Lifecycle transition job completed. Transitioned " + transitionedCount
                            + " analyzers to MAINTENANCE, " + failedCount + " failures. Execution time: "
                            + executionTimeMs + "ms");

            // Failure notification: If >3 analyzers fail transition, log WARNING with
            // summary (T153b)
            if (failedCount > 3) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "transitionToMaintenance",
                        "WARNING: " + failedCount + " analyzers failed transition to MAINTENANCE. "
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
