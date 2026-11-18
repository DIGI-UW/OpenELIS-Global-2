package org.openelisglobal.payment.gateway;

/** The gateway rejected a request or could not be reached. */
public class PaymentGatewayException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
