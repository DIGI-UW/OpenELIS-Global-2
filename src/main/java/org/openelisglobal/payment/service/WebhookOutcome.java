package org.openelisglobal.payment.service;

/**
 * What a gateway webhook call did. Only REJECTED_SIGNATURE and MALFORMED are
 * errors.
 */
public enum WebhookOutcome {
    /** Signature did not match: nothing was read or changed. */
    REJECTED_SIGNATURE,
    /** Body or event id was missing or unreadable. */
    MALFORMED,
    /** This event id was already processed. */
    DUPLICATE,
    /** The payment moved to the new status. */
    APPLIED,
    /** Event type not handled, unknown payment, or the move is not allowed. */
    IGNORED
}
