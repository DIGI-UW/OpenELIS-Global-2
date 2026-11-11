package org.openelisglobal.analyzer.service;

/**
 * A pairing the Bridge refused or that could not be completed; the message is a
 * message key.
 */
public class BridgePairingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BridgePairingException(String messageKey) {
        super(messageKey);
    }

    public BridgePairingException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}
