package org.openelisglobal.panel.event.listener;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.odoo.service.OdooPanelComboService;
import org.openelisglobal.odoo.service.OdooPanelProductService;
import org.openelisglobal.panel.event.PanelCreatedOrUpdatedEvent;
import org.openelisglobal.panel.valueholder.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class PanelCreatedOrUpdatedEventListener {

    @Autowired
    private OdooPanelProductService odooPanelProductService;
    @Autowired
    private OdooPanelComboService odooPanelComboService;

    @Async
    @EventListener
    public void handlePanelCreatedOrUpdatedEvent(PanelCreatedOrUpdatedEvent event) {
        try {
            Panel panel = event.getPanel();
            odooPanelProductService.syncPanelToOdoo(panel);
            odooPanelComboService.syncPanelComboToOdoo(panel);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "handlePanelCreatedOrUpdatedEvent",
                    "Error processing panel event for panel "
                            + (event.getPanel() != null ? event.getPanel().getId() : "unknown") + ": "
                            + e.getMessage());
            LogEvent.logError(this.getClass().getSimpleName(), "handlePanelCreatedOrUpdatedEvent",
                    "Full stack trace: " + e.toString());
        }
    }
}
