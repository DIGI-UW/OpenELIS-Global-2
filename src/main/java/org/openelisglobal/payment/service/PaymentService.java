package org.openelisglobal.payment.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.payment.valueholder.PaymentStatus;

public interface PaymentService extends BaseObjectService<Payment, String> {

    List<Payment> getPaymentsForSample(String sampleId);

    Payment getByGatewayLinkId(String gatewayLinkId);

    Payment getByGatewayEventId(String gatewayEventId);

    Payment getByGatewayPaymentId(String gatewayPaymentId);

    /**
     * Moves a payment to {@code next}. Throws IllegalStateException when the move
     * is not allowed by {@link PaymentStatus#canTransitionTo}.
     */
    Payment transition(String paymentId, PaymentStatus next, String sysUserId);
}
