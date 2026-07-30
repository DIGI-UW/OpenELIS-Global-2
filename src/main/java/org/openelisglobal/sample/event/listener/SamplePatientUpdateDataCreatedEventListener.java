package org.openelisglobal.sample.event.listener;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.odoo.service.OdooIntegrationService;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@SuppressWarnings("unused")
public class SamplePatientUpdateDataCreatedEventListener {

    @Autowired
    private OdooIntegrationService odooIntegrationService;

    /**
     * Fire only AFTER the publishing transaction commits successfully.
     *
     * <p>
     * The event is now published inside {@code SamplePatientEntryServiceImpl
     * .persistData()}'s {@code @Transactional} boundary so the synchronous
     * storage-assignment listener can fail the order save. With a plain
     * {@code @EventListener} the {@code @Async} dispatch happened immediately on
     * publish — which would race the rollback and create an orphan Odoo invoice for
     * an order that never persisted. {@code AFTER_COMMIT} ties dispatch to a
     * successful commit; on rollback this listener simply doesn't fire.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
