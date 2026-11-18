package org.openelisglobal.payment;

import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.openelisglobal.payment.gateway.NoOpPaymentGatewayClient;
import org.openelisglobal.payment.gateway.PaymentGatewayDisabledException;
import org.openelisglobal.payment.gateway.PaymentLinkRequest;

public class NoOpPaymentGatewayClientTest {

    @Test
    public void reportsDisabled() {
        assertFalse(new NoOpPaymentGatewayClient().isEnabled());
    }

    @Test(expected = PaymentGatewayDisabledException.class)
    public void createLinkFailsClearlyWhenDisabled() {
        new NoOpPaymentGatewayClient().createPaymentLink(new PaymentLinkRequest("A-1", 100L));
    }

    @Test(expected = PaymentGatewayDisabledException.class)
    public void refundFailsClearlyWhenDisabled() {
        new NoOpPaymentGatewayClient().refund("pay_1", 100L);
    }
}
