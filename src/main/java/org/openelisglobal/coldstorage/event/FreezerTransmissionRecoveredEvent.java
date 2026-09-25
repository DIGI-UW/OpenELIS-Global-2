package org.openelisglobal.coldstorage.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a freezer answers a Modbus poll again after a failed
 * one, so the offline alert raised for that outage can be resolved.
 */
@Getter
public class FreezerTransmissionRecoveredEvent extends ApplicationEvent {
    private final Long freezerId;

    public FreezerTransmissionRecoveredEvent(Object source, Long freezerId) {
        super(source);
        this.freezerId = freezerId;
    }
}
