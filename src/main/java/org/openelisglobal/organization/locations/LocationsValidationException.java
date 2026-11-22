package org.openelisglobal.organization.locations;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A save the menu refuses, with the message for each field that is wrong, so
 * the form can show them inline (FR-C4).
 */
public class LocationsValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> fieldErrors;

    public LocationsValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fieldErrors);
    }

    public LocationsValidationException(String field, String message) {
        super(message);
        this.fieldErrors = new LinkedHashMap<>();
        this.fieldErrors.put(field, message);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
