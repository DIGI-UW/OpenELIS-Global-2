package org.openelisglobal.coldstorage.event;

import java.math.BigDecimal;
import org.springframework.context.ApplicationEvent;

/** Published when a stored reading's humidity is outside its profile's band. */
public class FreezerHumidityThresholdViolatedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final Long freezerId;
    private final BigDecimal humidity;
    private final BigDecimal thresholdValue;
    private final String thresholdType;
    private final Long readingId;

    public FreezerHumidityThresholdViolatedEvent(Object source, Long freezerId, BigDecimal humidity,
            BigDecimal thresholdValue, String thresholdType, Long readingId) {
        super(source);
        this.freezerId = freezerId;
        this.humidity = humidity;
        this.thresholdValue = thresholdValue;
        this.thresholdType = thresholdType;
        this.readingId = readingId;
    }

    public Long getFreezerId() {
        return freezerId;
    }

    public BigDecimal getHumidity() {
        return humidity;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public String getThresholdType() {
        return thresholdType;
    }

    public Long getReadingId() {
        return readingId;
    }
}
