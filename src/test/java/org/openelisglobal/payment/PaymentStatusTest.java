package org.openelisglobal.payment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openelisglobal.payment.valueholder.PaymentStatus;

public class PaymentStatusTest {

    @Test
    public void createdCanMoveToPendingOrPaid() {
        assertTrue(PaymentStatus.CREATED.canTransitionTo(PaymentStatus.PENDING));
        assertTrue(PaymentStatus.CREATED.canTransitionTo(PaymentStatus.PAID));
    }

    @Test
    public void pendingCanBecomePaidFailedOrCancelled() {
        assertTrue(PaymentStatus.PENDING.canTransitionTo(PaymentStatus.PAID));
        assertTrue(PaymentStatus.PENDING.canTransitionTo(PaymentStatus.FAILED));
        assertTrue(PaymentStatus.PENDING.canTransitionTo(PaymentStatus.CANCELLED));
    }

    @Test
    public void paidCanOnlyBeRefunded() {
        assertTrue(PaymentStatus.PAID.canTransitionTo(PaymentStatus.REFUNDED));
        assertFalse(PaymentStatus.PAID.canTransitionTo(PaymentStatus.FAILED));
        assertFalse(PaymentStatus.PAID.canTransitionTo(PaymentStatus.CANCELLED));
        assertFalse(PaymentStatus.PAID.canTransitionTo(PaymentStatus.PENDING));
    }

    @Test
    public void paidIsNotReopenedByALateFailureEvent() {
        // A replayed or out-of-order "failed" webhook must not undo a payment.
        assertFalse(PaymentStatus.PAID.canTransitionTo(PaymentStatus.FAILED));
    }

    @Test
    public void terminalStatesHaveNoExits() {
        for (PaymentStatus to : PaymentStatus.values()) {
            assertFalse(PaymentStatus.FAILED.canTransitionTo(to));
            assertFalse(PaymentStatus.CANCELLED.canTransitionTo(to));
            assertFalse(PaymentStatus.REFUNDED.canTransitionTo(to));
        }
    }

    @Test
    public void sameStatusIsNotATransition() {
        for (PaymentStatus s : PaymentStatus.values()) {
            assertFalse(s.canTransitionTo(s));
        }
    }
}
