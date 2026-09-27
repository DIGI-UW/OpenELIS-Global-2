package org.openelisglobal.analyzer.service;

import java.util.Map;

/** Stable failure from the generic OpenELIS to Bridge connection boundary. */
public class BridgeAnalyzerConnectionException extends RuntimeException {

    private final String messageKey;
    private final Map<String, Object> messageArgs;

    public BridgeAnalyzerConnectionException(String messageKey) {
        this(messageKey, Map.of(), null);
    }

    public BridgeAnalyzerConnectionException(String messageKey, Map<String, Object> messageArgs) {
        this(messageKey, messageArgs, null);
    }

    public BridgeAnalyzerConnectionException(String messageKey, Map<String, Object> messageArgs, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.messageArgs = messageArgs == null ? Map.of() : Map.copyOf(messageArgs);
    }

    public String messageKey() {
        return messageKey;
    }

    public Map<String, Object> messageArgs() {
        return messageArgs;
    }
}
