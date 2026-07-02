package org.openelisglobal.coldstorage.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@NoArgsConstructor
@Component("freezerMonitoringProperties")
public class FreezerMonitoringProperties {

    @Value("${org.openelisglobal.freezermonitoring.enabled:false}")
    private boolean enabled;

    @Value("${org.openelisglobal.freezermonitoring.modbus.poll-interval:PT5M}")
    private String pollInterval;

    @Value("${org.openelisglobal.freezermonitoring.modbus.initial-delay:PT15S}")
    private String initialDelay;

    @Value("${org.openelisglobal.freezermonitoring.modbus.timeout-millis:2000}")
    private int timeoutMillis;

    @Value("${org.openelisglobal.freezermonitoring.modbus.retries:1}")
    private int retries;

    @Value("${org.openelisglobal.freezermonitoring.modbus.retry-backoff-millis:300}")
    private int retryBackoffMillis;

    @Value("${org.openelisglobal.freezermonitoring.modbus.poll-pool-size:8}")
    private int pollPoolSize;

    @Value("${org.openelisglobal.freezermonitoring.retention-days:400}")
    private int retentionDays;

    @Value("${org.openelisglobal.freezermonitoring.retention-cron:0 30 2 * * ?}")
    private String retentionCron;

    /**
     * Validates configuration at startup. Nonsensical values (negative/zero poll
     * interval, negative timeout, negative retries, etc.) are rejected outright
     * rather than merely logged, since a misconfigured cold-chain monitor that
     * silently limps along can mean an undetected temperature excursion.
     */
    public void validateConfig() {
        log.info("Freezer Monitoring Configuration:");
        log.info("  Enabled: {}", enabled);
        log.info("  Modbus Poll Interval: {}", pollInterval);
        log.info("  Modbus Initial Delay: {}", initialDelay);
        log.info("  Modbus Timeout: {}ms", timeoutMillis);
        log.info("  Modbus Retries: {}", retries);
        log.info("  Modbus Retry Backoff: {}ms", retryBackoffMillis);
        log.info("  Modbus Poll Pool Size: {}", pollPoolSize);
        log.info("  Reading Retention: {} days", retentionDays);

        if (timeoutMillis <= 0) {
            throw new IllegalStateException(
                    "org.openelisglobal.freezermonitoring.modbus.timeout-millis must be positive, was: "
                            + timeoutMillis);
        }
        if (timeoutMillis < 500 || timeoutMillis > 30000) {
            log.warn("Modbus timeout {}ms is outside recommended range (500-30000ms)", timeoutMillis);
        }

        if (retries < 0) {
            throw new IllegalStateException(
                    "org.openelisglobal.freezermonitoring.modbus.retries must not be negative, was: " + retries);
        }
        if (retries > 5) {
            log.warn("Modbus retries {} is outside recommended range (0-5)", retries);
        }

        if (retryBackoffMillis < 0) {
            throw new IllegalStateException(
                    "org.openelisglobal.freezermonitoring.modbus.retry-backoff-millis must not be negative, was: "
                            + retryBackoffMillis);
        }

        if (pollPoolSize <= 0) {
            throw new IllegalStateException(
                    "org.openelisglobal.freezermonitoring.modbus.poll-pool-size must be positive, was: "
                            + pollPoolSize);
        }

        try {
            long pollIntervalMillis = java.time.Duration.parse(pollInterval).toMillis();
            if (pollIntervalMillis <= 0) {
                throw new IllegalStateException(
                        "org.openelisglobal.freezermonitoring.modbus.poll-interval must be positive, was: "
                                + pollInterval);
            }
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalStateException(
                    "org.openelisglobal.freezermonitoring.modbus.poll-interval is not a valid ISO-8601 duration: "
                            + pollInterval,
                    ex);
        }

        try {
            long initialDelayMillis = java.time.Duration.parse(initialDelay).toMillis();
            if (initialDelayMillis < 0) {
                throw new IllegalStateException(
                        "org.openelisglobal.freezermonitoring.modbus.initial-delay must not be negative, was: "
                                + initialDelay);
            }
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalStateException(
                    "org.openelisglobal.freezermonitoring.modbus.initial-delay is not a valid ISO-8601 duration: "
                            + initialDelay,
                    ex);
        }

        if (retentionDays <= 0) {
            throw new IllegalStateException(
                    "org.openelisglobal.freezermonitoring.retention-days must be positive, was: " + retentionDays);
        }
    }
}
