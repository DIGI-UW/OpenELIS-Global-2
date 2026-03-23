package org.openelisglobal.alert.event;

import java.time.OffsetDateTime;
import lombok.Getter;
import org.openelisglobal.alert.valueholder.Alert;
import org.springframework.context.ApplicationEvent;

@Getter
public class AlertAcknowledgedEvent extends ApplicationEvent {
    private final Alert alert;
    private final Long acknowledgedByUserId;
    private final OffsetDateTime acknowledgedAt;

    public AlertAcknowledgedEvent(Object source, Alert alert, Long acknowledgedByUserId) {
        super(source);
        this.alert = alert;
        this.acknowledgedByUserId = acknowledgedByUserId;
        this.acknowledgedAt = OffsetDateTime.now();
    }
}
