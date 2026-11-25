package org.openelisglobal.delivery.service.sender;

/**
 * A send that did not happen, with a reason a laboratory user can act on. The
 * code tells the outbox whether to retry (a transport problem) or to fail at
 * once (the channel is not set up).
 */
public class DeliveryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Code {
        NOT_SET_UP, AUTHENTICATION, UNREACHABLE, REJECTED, OTHER
    }

    private final Code code;

    public DeliveryException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public DeliveryException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

    /** A channel that is not set up fails the message at once, no retry. */
    public boolean isRetryable() {
        return code != Code.NOT_SET_UP;
    }
}
