package org.openelisglobal.coldstorage.event;

import java.time.OffsetDateTime;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a Modbus poll of a freezer fails (device unreachable,
 * timed out, or returned a malformed response). Distinct from a temperature
 * threshold violation: this fires even though no temperature was read at all,
 * so a disconnected sensor cannot hide behind "no threshold breach".
 */
@Getter
public class FreezerTransmissionFailedEvent extends ApplicationEvent {
    private final Long freezerId;
    private final String errorMessage;
    private final Long readingId;
    private final OffsetDateTime detectedAt;

    public FreezerTransmissionFailedEvent(Object source, Long freezerId, String errorMessage, Long readingId) {
        super(source);
        this.freezerId = freezerId;
        this.errorMessage = errorMessage;
        this.readingId = readingId;
        this.detectedAt = OffsetDateTime.now();
    }
}
