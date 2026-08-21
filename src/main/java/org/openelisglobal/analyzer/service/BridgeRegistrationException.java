package org.openelisglobal.analyzer.service;

public class BridgeRegistrationException extends RuntimeException {

    public BridgeRegistrationException(String message) {
        super(message);
    }

    public BridgeRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
