package org.openelisglobal.panel.event.listener;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.odoo.service.OdooPanelComboService;
import org.openelisglobal.odoo.service.OdooPanelProductService;
import org.openelisglobal.panel.event.PanelCreatedOrUpdatedEvent;
import org.openelisglobal.panel.service.fhir.PanelFhirTransformService;
import org.openelisglobal.panel.valueholder.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired(required = false)
    private PanelFhirTransformService panelFhirTransformService;

    @Value("${org.openelisglobal.fhir.panel.sync.enabled:false}")
    private boolean panelFhirSyncEnabled;

    @Async
    @EventListener
    public void handlePanelCreatedOrUpdatedEvent(PanelCreatedOrUpdatedEvent event) {
        try {
            Panel panel = event.getPanel();

            // Existing Odoo sync
            odooPanelProductService.syncPanelToOdoo(panel);
            odooPanelComboService.syncPanelComboToOdoo(panel);

            // NEW: FHIR sync
            if (panelFhirSyncEnabled && panelFhirTransformService != null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "handlePanelCreatedOrUpdatedEvent",
                        "Syncing panel to FHIR: " + panel.getDescription() + " (guid=" + panel.getGuid() + ")");
                panelFhirTransformService.syncPanelToFhir(panel);
            }

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
