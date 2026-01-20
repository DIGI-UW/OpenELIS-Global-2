package org.openelisglobal.test.event.listener;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.test.event.TestCreatedEvent;
import org.openelisglobal.test.service.fhir.TestFhirTransformService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for syncing OpenELIS tests to OpenMRS via FHIR.
 * Listens to TestCreatedEvent and triggers FHIR sync.
 * 
 * Runs AFTER Odoo listener (Order 2) to avoid interfering with existing
 * integrations.
 * Async and non-blocking to prevent impacting test creation/update performance.
 */
@Component
public class TestFhirSyncEventListener {

    @Autowired
    private TestFhirTransformService testFhirTransformService;

    @Value("${org.openelisglobal.fhir.test.sync.enabled:false}")
    private boolean syncEnabled;

    /**
     * Handle test created/updated events.
     * Syncs the test to configured FHIR servers (OpenMRS).
     * 
     * @param event the test created event
     */
    @Async
    @EventListener
    @Order(2) // Run after Odoo listener (default order or Order 1)
    public void handleTestCreatedEvent(TestCreatedEvent event) {
        if (!syncEnabled) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "handleTestCreatedEvent",
                    "FHIR test sync is disabled (org.openelisglobal.fhir.test.sync.enabled=false), skipping");
            return;
        }

        try {
            Test test = event.getTest();
            boolean isUpdate = event.isUpdate();

            String operation = isUpdate ? "UPDATE" : "CREATE";
            LogEvent.logInfo(this.getClass().getSimpleName(), "handleTestCreatedEvent",
                    "Syncing test to FHIR: " + test.getDescription() + " (operation=" + operation + ", guid="
                            + test.getGuid() + ")");

            // Trigger async FHIR sync
            testFhirTransformService.syncTestToFhir(test, isUpdate);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "handleTestCreatedEvent",
                    "Error processing TestCreatedEvent for FHIR sync: " + e.getMessage());
            LogEvent.logError(this.getClass().getSimpleName(), "handleTestCreatedEvent",
                    "Full stack trace: " + e.toString());
            // Don't throw - keep async and non-blocking
            // This ensures that even if FHIR sync fails, test creation/update succeeds
        }
    }
}
