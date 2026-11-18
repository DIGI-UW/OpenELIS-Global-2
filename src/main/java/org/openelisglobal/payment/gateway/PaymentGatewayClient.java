package org.openelisglobal.payment.gateway;

/**
 * Provider-agnostic payment gateway. Implementations: Razorpay (live) and a
 * no-op used when online payment is disabled.
 */
public interface PaymentGatewayClient {

    /** True only when the gateway is switched on and fully configured. */
    boolean isEnabled();

    /**
     * True when the gateway should send the link to the patient by SMS or email.
     * Only then is the patient's phone and email read and passed on.
     */
    default boolean notifiesCustomer() {
        return false;
    }

    /** Creates a hosted payment link. Throws PaymentGatewayException on failure. */
    PaymentLinkResult createPaymentLink(PaymentLinkRequest request);

    /** Refunds a captured payment and returns the refund id. */
    String refund(String gatewayPaymentId, long amountPaise);

    /** Checks a webhook's signature header against its raw body. */
    boolean verifyWebhookSignature(String rawBody, String signatureHeader);
}
