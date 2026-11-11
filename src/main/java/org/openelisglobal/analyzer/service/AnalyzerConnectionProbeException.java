package org.openelisglobal.analyzer.service;

import java.util.Map;

/**
 * Stable, localizable failure returned while obtaining Bridge probe evidence.
 */
public class AnalyzerConnectionProbeException extends RuntimeException {

    private final String messageKey;
    private final Map<String, Object> messageArgs;

    public AnalyzerConnectionProbeException(String messageKey) {
        this(messageKey, Map.of(), null);
    }

    public AnalyzerConnectionProbeException(String messageKey, Map<String, Object> messageArgs) {
        this(messageKey, messageArgs, null);
    }

    public AnalyzerConnectionProbeException(String messageKey, Map<String, Object> messageArgs, Throwable cause) {
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
