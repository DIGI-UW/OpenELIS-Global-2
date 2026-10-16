package org.openelisglobal.alert.event;

import java.time.OffsetDateTime;
import lombok.Getter;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.springframework.context.ApplicationEvent;

@Getter
public class AlertAcknowledgedEvent extends ApplicationEvent {
    private final Alert alert;
    private final Long acknowledgedByUserId;
    private final OffsetDateTime acknowledgedAt;

    private final Long alertId;
    private final String acknowledgementReason;
    private final AlertStatus previousStatus;
    private final AlertStatus currentStatus;

    public AlertAcknowledgedEvent(Object source, Alert alert, Long acknowledgedByUserId) {
        this(source, alert, acknowledgedByUserId, null, null, null, null, null);
    }

    public AlertAcknowledgedEvent(Object source, Alert alert, Long acknowledgedByUserId, Long alertId,
            String acknowledgementReason, AlertStatus previousStatus, AlertStatus currentStatus,
            OffsetDateTime acknowledgedAt) {
        super(source);
        this.alert = alert;
        this.acknowledgedByUserId = acknowledgedByUserId;
        this.alertId = alertId;
        this.acknowledgementReason = acknowledgementReason;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;

        this.acknowledgedAt = (acknowledgedAt != null) ? acknowledgedAt : OffsetDateTime.now();
    }
}
