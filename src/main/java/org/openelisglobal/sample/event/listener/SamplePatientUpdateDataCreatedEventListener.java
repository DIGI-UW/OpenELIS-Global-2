package org.openelisglobal.sample.event.listener;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.odoo.exception.OdooUnavailableException;
import org.openelisglobal.odoo.service.OdooIntegrationService;
import org.openelisglobal.odoo.service.OdooSyncQueueService;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class SamplePatientUpdateDataCreatedEventListener {

    @Value("${org.openelisglobal.odoo.enabled:false}")
    private boolean odooEnabled;

    @Value("${org.openelisglobal.odoo.retry.queue.enabled:true}")
    private boolean queueEnabled;

    @Autowired
    private OdooIntegrationService odooIntegrationService;

    @Autowired(required = false)
    private OdooSyncQueueService odooSyncQueueService;

    @Async
    @EventListener
    public void handleSamplePatientUpdateDataCreatedEvent(SamplePatientUpdateDataCreatedEvent event) {
        if (!odooEnabled) {
            return;
        }

        SamplePatientUpdateData updateData = event.getUpdateData();
        String accessionNumber = updateData != null ? updateData.getAccessionNumber() : "unknown";

        if (updateData == null) {
            LogEvent.logError(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    "SamplePatientUpdateData is null; skipping Odoo invoice creation for sample " + accessionNumber
                            + ".");
            return;
        }

        try {
            odooIntegrationService.createInvoice(updateData);
        } catch (OdooUnavailableException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    "Odoo unavailable for sample " + accessionNumber + ". Enqueueing for retry.");
            enqueueForRetry(accessionNumber);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    "Odoo invoice creation failed for sample " + accessionNumber + ": " + e.getMessage());
            if (queueEnabled) {
                enqueueForRetry(accessionNumber);
            }
        }
    }

    private void enqueueForRetry(String accessionNumber) {
        if (!queueEnabled || odooSyncQueueService == null) {
            LogEvent.logError(this.getClass().getSimpleName(), "enqueueForRetry",
                    "Queue disabled or unavailable. Invoice for sample " + accessionNumber + " will be lost.");
            return;
        }
        try {
            odooSyncQueueService.enqueue(accessionNumber);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "enqueueForRetry",
                    "Failed to enqueue sample " + accessionNumber + " for Odoo retry: " + e.getMessage());
        }
    }
}
