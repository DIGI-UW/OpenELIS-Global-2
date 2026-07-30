package org.openelisglobal.sample.event.listener;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SampleFhirTransformEventListener {

    @Autowired
    private FhirTransformService fhirTransformService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSamplePatientUpdateDataCreatedEvent(SamplePatientUpdateDataCreatedEvent event) {
        try {
            SamplePatientUpdateData updateData = event.getUpdateData();
            PatientManagementInfo patientInfo = event.getPatientInfo();
            SamplePatientEntryForm form = event.getForm();

            fhirTransformService.transformPersistOrderEntryFhirObjects(updateData, patientInfo, form.getUseReferral(),
                    form.getReferralItems());

            LogEvent.logInfo(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    String.format("FHIR transformation completed for sample with accession number: %s",
                            updateData.getAccessionNumber()));

        } catch (FhirTransformationException | FhirPersistanceException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    "Error during FHIR transformation: " + e.getMessage());
        }
    }
}
