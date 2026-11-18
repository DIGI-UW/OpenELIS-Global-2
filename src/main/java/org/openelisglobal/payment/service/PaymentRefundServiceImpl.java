package org.openelisglobal.payment.service;

import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.payment.gateway.PaymentGatewayClient;
import org.openelisglobal.payment.gateway.PaymentGatewayDisabledException;
import org.openelisglobal.payment.gateway.PaymentGatewayProvider;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.payment.valueholder.PaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentRefundServiceImpl implements PaymentRefundService {

    private final PaymentService paymentService;
    private final PaymentGatewayProvider gatewayProvider;

    @Autowired
    public PaymentRefundServiceImpl(PaymentService paymentService, PaymentGatewayProvider gatewayProvider) {
        this.paymentService = paymentService;
        this.gatewayProvider = gatewayProvider;
    }

    @Override
    public String refund(String paymentId, String sysUserId) {
        PaymentGatewayClient gateway = gatewayProvider.current();
        if (!gateway.isEnabled()) {
            throw new PaymentGatewayDisabledException();
        }
        Payment payment;
        try {
            // get() throws for a missing row; this method is not transactional
            // (it calls the gateway), so the exception can become "not found".
            payment = paymentService.get(paymentId);
        } catch (ObjectNotFoundException e) {
            payment = null;
        }
        if (payment == null) {
            throw new IllegalArgumentException("Payment not found: " + paymentId);
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Only a paid payment can be refunded; this one is " + payment.getStatus());
        }
        if (payment.getGatewayPaymentId() == null || payment.getGatewayPaymentId().isBlank()) {
            throw new IllegalStateException("Payment has no gateway payment id to refund");
        }
        // Full refund of the amount the patient paid (subtotal plus GST).
        return gateway.refund(payment.getGatewayPaymentId(), payment.getAmountPaise());
    }
}
