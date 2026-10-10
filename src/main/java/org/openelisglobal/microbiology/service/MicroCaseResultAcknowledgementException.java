package org.openelisglobal.microbiology.service;

import java.util.Map;

public class MicroCaseResultAcknowledgementException extends RuntimeException {
    private final Map<String, Object> body;

    public MicroCaseResultAcknowledgementException(Map<String, Object> body) {
        super("ACKNOWLEDGEMENT_REQUIRED");
        this.body = body;
    }

    public Map<String, Object> getBody() {
        return body;
    }
}
