package org.openelisglobal.common.exception;

import java.util.Collections;
import java.util.Map;

/**
 * A {@link LIMSRuntimeException} carrying a stable, translatable
 * {@code errorCode} (an {@code en.json} message id) plus interpolation
 * {@code params}, so a REST controller can return a machine-readable reason
 * instead of forcing the frontend to display a hardcoded English exception
 * message untranslated. {@link #getMessage()} stays a human-readable fallback
 * for logs and any caller not localizing the response.
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
