package org.openelisglobal.result.util;

/**
 * Optional display/report formatting; never a clinical measurement constraint.
 */
public final class ReportingDecimalPlaces {
    private ReportingDecimalPlaces() {
    }

    public static Integer parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int places = Integer.parseInt(value.trim());
            if (places >= 0 && places <= 10) {
                return places;
            }
        } catch (NumberFormatException ignored) {
            // Report the same constraint for non-integer and out-of-range input.
        }
        throw new IllegalArgumentException("Reporting decimal places must be blank or an integer from 0 to 10");
    }
}
