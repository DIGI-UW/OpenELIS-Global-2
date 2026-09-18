package org.openelisglobal.alert.event;

import java.time.OffsetDateTime;
import lombok.Getter;
import org.openelisglobal.alert.valueholder.Alert;
import org.springframework.context.ApplicationEvent;

@Getter
public class AlertAcknowledgedEvent extends ApplicationEvent {

    private final Alert alert;

    // Convenience field so listeners don't have to inspect the Alert object
    private final String alertId;

    // User who acknowledged the alert
    private final Long acknowledgedByUserId;

    // When the acknowledgement occurred
    private final OffsetDateTime acknowledgedAt;

    // Optional reason supplied by the user
    private final String acknowledgementReason;

    // Previous and current status for auditing
    private final String previousStatus;
    private final String currentStatus;

    public AlertAcknowledgedEvent(
            Object source,
            Alert alert,
            Long acknowledgedByUserId,
            String acknowledgementReason,
            String previousStatus,
            String currentStatus,
            OffsetDateTime acknowledgedAt) {

        super(source);

        this.alert = alert;
        this.alertId = alert != null ? String.valueOf(alert.getId()) : null;
        this.acknowledgedByUserId = acknowledgedByUserId;
        this.acknowledgementReason = acknowledgementReason;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.acknowledgedAt =
                acknowledgedAt != null ? acknowledgedAt : OffsetDateTime.now();
    }
}
