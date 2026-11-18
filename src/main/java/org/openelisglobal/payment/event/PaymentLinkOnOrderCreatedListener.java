package org.openelisglobal.payment.event;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.payment.gateway.PaymentGatewayProvider;
import org.openelisglobal.payment.service.PaymentLinkService;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Creates the payment link once a new order has committed. Runs after commit so
 * a rolled-back order never gets a link. Does nothing while online payment is
 * disabled. Failures are logged and never fail order entry; staff can retry
 * from the order.
 */
@Component
public class PaymentLinkOnOrderCreatedListener {

    @Autowired
    private PaymentGatewayProvider gatewayProvider;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private PaymentLinkService paymentLinkService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SamplePatientUpdateDataCreatedEvent event) {
        String accession = null;
        try {
            if (!gatewayProvider.current().isEnabled()) {
                return;
            }
            SamplePatientUpdateData updateData = event.getUpdateData();
            accession = updateData.getAccessionNumber();
            Sample sample = sampleService.getSampleByAccessionNumber(accession);
            if (sample == null) {
                LogEvent.logWarn(getClass().getSimpleName(), "handle",
                        "No sample found for accession " + accession + "; no payment link created");
                return;
            }
            paymentLinkService.requestLink(sample.getId(), updateData.getCurrentUserId());
        } catch (Exception e) {
            LogEvent.logError(getClass().getSimpleName(), "handle",
                    "Payment link not created for accession " + accession + ": " + e.getClass().getSimpleName());
        }
    }
}
