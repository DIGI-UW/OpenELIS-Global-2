package org.openelisglobal.orderentry.service;

/**
 * An order-entry request the service refuses on purpose (an unknown order, a
 * test that is not on it, an unknown laboratory or record kind, an over-long
 * value). Controllers answer it with 400 and its message; anything else is a
 * real fault and must surface as one.
 */
public class OrderEntryRequestRefusedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OrderEntryRequestRefusedException(String message) {
        super(message);
    }
}
