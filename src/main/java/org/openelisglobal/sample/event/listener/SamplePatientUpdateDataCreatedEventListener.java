package org.openelisglobal.sample.event.listener;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.odoo.service.OdooIntegrationService;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class SamplePatientUpdateDataCreatedEventListener {

    @Autowired
    private OdooIntegrationService odooIntegrationService;

    @Async
    @EventListener
    public void handleSamplePatientUpdateDataCreatedEvent(SamplePatientUpdateDataCreatedEvent event) {
        try {
            SamplePatientUpdateData updateData = event.getUpdateData();
            PatientManagementInfo patientInfo = event.getPatientInfo();

            // OdooIntegrationService will check if connection is available
            // No need to check here since the service handles it gracefully
            odooIntegrationService.createInvoice(updateData);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    "Error processing sample creation event for sample "
                            + (event.getUpdateData() != null ? event.getUpdateData().getAccessionNumber() : "unknown")
                            + ": " + e.getMessage());
            LogEvent.logError(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    "Full stack trace: " + e.toString());
        }
    }
}
