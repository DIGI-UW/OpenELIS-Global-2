package org.openelisglobal.analyzer.service;

public class BridgeProfileManagementException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    public BridgeProfileManagementException(int status, String message) {
        super(message);
        this.status = status;
    }

    public BridgeProfileManagementException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
