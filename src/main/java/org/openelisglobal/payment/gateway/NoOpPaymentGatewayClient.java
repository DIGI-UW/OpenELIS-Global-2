package org.openelisglobal.payment.gateway;

/**
 * Used when online payment is disabled. Reports disabled and refuses actions.
 */
public class NoOpPaymentGatewayClient implements PaymentGatewayClient {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public PaymentLinkResult createPaymentLink(PaymentLinkRequest request) {
        throw new PaymentGatewayDisabledException();
    }

    @Override
    public String refund(String gatewayPaymentId, long amountPaise) {
        throw new PaymentGatewayDisabledException();
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        return false;
    }
}
