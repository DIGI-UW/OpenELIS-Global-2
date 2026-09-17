package org.openelisglobal.configuration.service;

import java.util.Locale;
import java.util.Map;

/**
 * One data row of a catalog CSV file, addressed by column name
 * (case-insensitive) rather than position. A missing or blank cell reads as the
 * empty string.
 */
public record CsvRow(Map<String, Integer> columns, String[] values, int lineNumber) {

    public String get(String column) {
        Integer index = columns.get(column.toLowerCase(Locale.ROOT));
        if (index == null || index >= values.length || values[index] == null) {
            return "";
        }
        return values[index].trim();
    }

    public boolean has(String column) {
        return columns.containsKey(column.toLowerCase(Locale.ROOT));
    }

    public boolean isBlank(String column) {
        return get(column).isEmpty();
    }

    /** A Y/N-style cell; blank falls back to the default. */
    public boolean flag(String column, boolean defaultValue) {
        String value = get(column);
        if (value.isEmpty()) {
            return defaultValue;
        }
        return "Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    /** A whole-number cell; blank reads as null, anything else must parse. */
    public Integer integer(String column) {
        String value = get(column);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(column + " must be a whole number, was '" + value + "'");
        }
    }

    /** A decimal cell; blank reads as null, anything else must parse. */
    public Double decimal(String column) {
        String value = get(column);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(column + " must be a number, was '" + value + "'");
        }
    }
}
