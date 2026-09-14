package org.openelisglobal.microbiology.service;

import org.openelisglobal.alert.event.AlertAcknowledgedEvent;
import org.openelisglobal.alert.event.AlertResolvedEvent;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertType;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps the microbiology clinical record aligned when generic Alerts are
 * actioned.
 */
@Component
public class MicrobiologyCriticalAlertLifecycleListener {

    private static final String ENTITY_TYPE = "MicrobiologyCriticalCommunication";

    private final MicroCriticalCommunicationService communicationService;

    public MicrobiologyCriticalAlertLifecycleListener(MicroCriticalCommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @EventListener
    public void onAlertAcknowledged(AlertAcknowledgedEvent event) {
        Alert alert = event.getAlert();
        if (isMicrobiologyCritical(alert)) {
            communicationService.synchronizeAcknowledgementFromAlert(alert.getAlertEntityRef(),
                    String.valueOf(event.getAcknowledgedByUserId()));
        }
    }

    @EventListener
    public void onAlertResolved(AlertResolvedEvent event) {
        Alert alert = event.getAlert();
        if (isMicrobiologyCritical(alert)) {
            communicationService.synchronizeResolutionFromAlert(alert.getAlertEntityRef(), event.getResolutionNotes(),
                    String.valueOf(event.getResolvedByUserId()));
        }
    }

    private boolean isMicrobiologyCritical(Alert alert) {
        return alert != null && alert.getAlertType() == AlertType.MICROBIOLOGY_CRITICAL
                && ENTITY_TYPE.equals(alert.getAlertEntityType()) && alert.getAlertEntityRef() != null;
    }
}
