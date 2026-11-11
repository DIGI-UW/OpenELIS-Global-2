package org.openelisglobal.common.rest;

import java.sql.Date;
import java.util.Map;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.ControllerUtills;
import org.springframework.stereotype.Component;

@Component
public class BaseRestController extends ControllerUtills implements IActionConstants {

    /**
     * A field of a {@code Map<String, Object>} request body as text, or null when
     * absent. Jackson hands numbers over as Integer/Double and ids as either, so
     * reading through {@code String.valueOf} is what makes the parsers below
     * indifferent to which.
     */
    protected String stringField(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * A numeric id field, or null when absent or blank.
     *
     * @throws IllegalArgumentException when present but not a number
     */
    protected Long longField(Map<String, Object> body, String key) {
        String value = stringField(body, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a number");
        }
    }

    /**
     * A whole-number field, or null when absent or blank.
     *
     * @throws IllegalArgumentException when present but not a whole number
     */
    protected Integer integerField(Map<String, Object> body, String key) {
        String value = stringField(body, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a whole number");
        }
    }

    /**
     * A date field in ISO yyyy-MM-dd — the shape the SPA's date inputs post — or
     * null when absent or blank.
     *
     * @throws IllegalArgumentException when present but not an ISO date
     */
    protected Date dateField(Map<String, Object> body, String key) {
        String value = stringField(body, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Date.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(key + " must be an ISO date (yyyy-MM-dd)");
        }
    }
}
