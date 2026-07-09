package org.openelisglobal.barcode.util;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;

/**
 * Safe parsing helpers for barcode configuration values (admin-configured DB
 * values). Use these instead of Integer.parseInt/Float.parseFloat to avoid
 * NumberFormatException when values are missing or invalid.
 */
public final class BarcodeConfigUtil {

    private BarcodeConfigUtil() {
    }

    /**
     * Parse a string to float; return defaultValue if value is null, blank, or not
     * a valid number. Logs a warning on parse failure.
     */
    public static float parseFloatSafe(String value, float defaultValue) {
        if (GenericValidator.isBlankOrNull(value)) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            LogEvent.logWarn("BarcodeConfigUtil", "parseFloatSafe",
                    "Invalid float value '" + value + "', using default " + defaultValue + ": " + e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Parse a string to int; return defaultValue if value is null, blank, or not a
     * valid integer. Logs a warning on parse failure.
     */
    public static int parseIntSafe(String value, int defaultValue) {
        if (GenericValidator.isBlankOrNull(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LogEvent.logWarn("BarcodeConfigUtil", "parseIntSafe",
                    "Invalid int value '" + value + "', using default " + defaultValue + ": " + e.getMessage());
            return defaultValue;
        }
    }
}
