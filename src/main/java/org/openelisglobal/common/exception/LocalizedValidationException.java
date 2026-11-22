package org.openelisglobal.common.exception;

import java.util.Collections;
import java.util.Map;

/**
 * Carries an {@code en.json} message id and params so a controller can answer
 * with a translatable reason; {@link #getMessage()} is not translated.
 */
public class LocalizedValidationException extends LIMSRuntimeException {

    private final String errorCode;
    private final Map<String, String> params;

    public LocalizedValidationException(String errorCode, String fallbackMessage) {
        this(errorCode, fallbackMessage, Collections.emptyMap());
    }

    public LocalizedValidationException(String errorCode, String fallbackMessage, Map<String, String> params) {
        super(fallbackMessage);
        this.errorCode = errorCode;
        this.params = params;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, String> getParams() {
        return params;
    }
}
