package org.openelisglobal.alert.event;

import java.time.OffsetDateTime;
import lombok.Getter;
import org.openelisglobal.alert.valueholder.Alert;
import org.springframework.context.ApplicationEvent;

@Getter
public class AlertResolvedEvent extends ApplicationEvent {
    private final Alert alert;
    private final Long resolvedByUserId;
    private final String resolutionNotes;
    private final OffsetDateTime resolvedAt;

    public AlertResolvedEvent(Object source, Alert alert, Long resolvedByUserId, String resolutionNotes) {
        super(source);
        this.alert = alert;
        this.resolvedByUserId = resolvedByUserId;
        this.resolutionNotes = resolutionNotes;
        this.resolvedAt = OffsetDateTime.now();
    }
}
