package org.openelisglobal.analyzer.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A refused analyzer request the operator can act on. The screen shows the
 * message its key names; the English message is for logs and API callers.
 */
public class AnalyzerRequestException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final String messageKey;
    private final Map<String, Object> messageArgs;

    public AnalyzerRequestException(String messageKey, String message) {
        this(messageKey, Map.of(), message);
    }

    public AnalyzerRequestException(String messageKey, Map<String, Object> messageArgs, String message) {
        super(message);
        this.messageKey = messageKey;
        this.messageArgs = Map.copyOf(messageArgs);
    }

    public String messageKey() {
        return messageKey;
    }

    public Map<String, Object> messageArgs() {
        return messageArgs;
    }

    /**
     * The body of a refused request: the message, and its key and arguments when
     * the refusal has one.
     */
    public static Map<String, Object> body(IllegalArgumentException exception, String fallback) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", exception.getMessage() == null ? fallback : exception.getMessage());
        if (exception instanceof AnalyzerRequestException keyed) {
            body.put("messageKey", keyed.messageKey());
            body.put("messageArgs", keyed.messageArgs());
        }
        return body;
    }
}
