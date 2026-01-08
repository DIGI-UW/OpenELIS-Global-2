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

    public void validateConfig() {
        log.info("Freezer Monitoring Configuration:");
        log.info("  Enabled: {}", enabled);
        log.info("  Modbus Poll Interval: {}", pollInterval);
        log.info("  Modbus Initial Delay: {}", initialDelay);
        log.info("  Modbus Timeout: {}ms", timeoutMillis);
        log.info("  Modbus Retries: {}", retries);

        if (timeoutMillis < 500 || timeoutMillis > 30000) {
            log.warn("Modbus timeout {}ms is outside recommended range (500-30000ms)", timeoutMillis);
        }

        if (retries < 0 || retries > 5) {
            log.warn("Modbus retries {} is outside recommended range (0-5)", retries);
        }
    }
}
