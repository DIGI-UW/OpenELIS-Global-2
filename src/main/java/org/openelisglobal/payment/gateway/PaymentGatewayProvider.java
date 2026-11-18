package org.openelisglobal.payment.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Returns the gateway to use right now: Razorpay when it is enabled and
 * configured, otherwise the no-op client.
 */
@Component
public class PaymentGatewayProvider {

    @Autowired
    private RazorpayPaymentGatewayClient razorpay;

    private final PaymentGatewayClient disabled = new NoOpPaymentGatewayClient();

    public PaymentGatewayClient current() {
        return razorpay.isEnabled() ? razorpay : disabled;
    }
}
