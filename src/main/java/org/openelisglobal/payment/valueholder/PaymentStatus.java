package org.openelisglobal.payment.valueholder;

/**
 * Lifecycle of a payment request. Transitions are guarded so a replayed or
 * out-of-order gateway event cannot move a payment backwards (e.g. a late
 * "failed" event must not undo a PAID payment).
 */
public enum PaymentStatus {
    CREATED, PENDING, PAID, FAILED, CANCELLED, REFUNDED;

    public boolean canTransitionTo(PaymentStatus next) {
        if (next == null || next == this) {
            return false;
        }
        switch (this) {
        case CREATED:
            return next == PENDING || next == PAID || next == FAILED || next == CANCELLED;
        case PENDING:
            return next == PAID || next == FAILED || next == CANCELLED;
        case PAID:
            return next == REFUNDED;
        default:
            // FAILED, CANCELLED and REFUNDED are terminal.
            return false;
        }
    }
}
