package org.openelisglobal.alert.event;

import java.time.OffsetDateTime;
import lombok.Getter;
import org.openelisglobal.alert.valueholder.Alert;
import org.springframework.context.ApplicationEvent;

@Getter
public class AlertCreatedEvent extends ApplicationEvent {
    private final Alert alert;
    private final OffsetDateTime createdAt;

    public AlertCreatedEvent(Object source, Alert alert) {
        super(source);
        this.alert = alert;
        this.createdAt = OffsetDateTime.now();
    }
}
