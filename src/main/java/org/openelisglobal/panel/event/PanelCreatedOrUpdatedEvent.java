package org.openelisglobal.panel.event;

import java.time.OffsetDateTime;
import lombok.Getter;
import org.openelisglobal.panel.valueholder.Panel;
import org.springframework.context.ApplicationEvent;

@Getter
public class PanelCreatedOrUpdatedEvent extends ApplicationEvent {

    private final Panel panel;
    private final OffsetDateTime createdAt;

    public PanelCreatedOrUpdatedEvent(Object source, Panel panel) {
        super(source);
        this.panel = panel;
        this.createdAt = OffsetDateTime.now();
    }
}
