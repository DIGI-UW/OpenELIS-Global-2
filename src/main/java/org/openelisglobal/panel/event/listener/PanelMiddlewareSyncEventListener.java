package org.openelisglobal.panel.event.listener;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.panel.event.PanelCreatedOrUpdatedEvent;
import org.openelisglobal.panel.service.middleware.PanelMiddlewareSyncService;
import org.openelisglobal.panel.valueholder.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class PanelMiddlewareSyncEventListener {

    @Autowired
    private PanelMiddlewareSyncService panelMiddlewareSyncService;

    @Async
    @EventListener
    public void handlePanelCreatedOrUpdatedEvent(PanelCreatedOrUpdatedEvent event) {
        try {
            Panel panel = event.getPanel();
            LogEvent.logInfo(this.getClass().getSimpleName(), "handlePanelCreatedOrUpdatedEvent",
                    "Syncing panel to middleware: " + panel.getDescription() + " (guid=" + panel.getGuid() + ")");

            panelMiddlewareSyncService.syncPanelToMiddleware(panel);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "handlePanelCreatedOrUpdatedEvent",
                    "Error processing PanelCreatedOrUpdatedEvent for middleware sync: " + e.getMessage());
            LogEvent.logError(this.getClass().getSimpleName(), "handlePanelCreatedOrUpdatedEvent",
                    "Full stack trace: " + e.toString());
        }
    }
}
