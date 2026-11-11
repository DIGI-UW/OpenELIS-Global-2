package org.openelisglobal.analyzer.service;

public class BridgeProfileCatalogException extends RuntimeException {

    private final boolean bridgeNotPaired;

    public BridgeProfileCatalogException(String message) {
        super(message);
        this.bridgeNotPaired = false;
    }

    public BridgeProfileCatalogException(String message, Throwable cause) {
        this(message, cause, false);
    }

    private BridgeProfileCatalogException(String message, Throwable cause, boolean bridgeNotPaired) {
        super(message, cause);
        this.bridgeNotPaired = bridgeNotPaired;
    }

    /** The Bridge has not been paired with OpenELIS, so it could not be asked. */
    public static BridgeProfileCatalogException notPaired(Throwable cause) {
        return new BridgeProfileCatalogException("The Analyzer Bridge is not paired with OpenELIS", cause, true);
    }

    public boolean bridgeNotPaired() {
        return bridgeNotPaired;
    }
}
