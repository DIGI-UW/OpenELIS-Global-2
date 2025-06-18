package org.openelisglobal.sample.event.listener;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SamplePatientUpdateDataCreatedEventListener {

    @Async
    @EventListener
    public void handleSamplePatientUpdateDataCreatedEvent(SamplePatientUpdateDataCreatedEvent event) {
        try {
            SamplePatientUpdateData updateData = event.getUpdateData();
            PatientManagementInfo patientInfo = event.getPatientInfo();

            // TODO: Implement Odoo integration

            LogEvent.logInfo(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    String.format("Sample created with accession number: %s at %s", updateData.getAccessionNumber(),
                            event.getCreatedAt()));

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    "Error processing sample creation event: " + e.getMessage());
        }
    }
}
