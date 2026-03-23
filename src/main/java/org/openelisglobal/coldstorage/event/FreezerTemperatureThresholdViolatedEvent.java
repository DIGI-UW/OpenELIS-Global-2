package org.openelisglobal.coldstorage.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a freezer temperature reading violates a threshold.
 */
@Getter
public class FreezerTemperatureThresholdViolatedEvent extends ApplicationEvent {
    private final Long freezerId;
    private final BigDecimal temperature;
    private final BigDecimal thresholdValue;
    private final String thresholdType; // "CRITICAL_HIGH", "WARNING_HIGH", "CRITICAL_LOW", "WARNING_LOW"
    private final Long readingId;
    private final OffsetDateTime detectedAt;

    public FreezerTemperatureThresholdViolatedEvent(Object source, Long freezerId, BigDecimal temperature,
            BigDecimal thresholdValue, String thresholdType, Long readingId) {
        super(source);
        this.freezerId = freezerId;
        this.temperature = temperature;
        this.thresholdValue = thresholdValue;
        this.thresholdType = thresholdType;
        this.readingId = readingId;
        this.detectedAt = OffsetDateTime.now();
    }
}
