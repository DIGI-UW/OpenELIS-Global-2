package org.openelisglobal.payment.gateway;

/**
 * Thrown when a gateway operation is requested but the gateway is not enabled.
 */
public class PaymentGatewayDisabledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PaymentGatewayDisabledException() {
        super("Online payment is not enabled");
    }
}
