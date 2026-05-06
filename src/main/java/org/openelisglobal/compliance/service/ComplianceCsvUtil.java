package org.openelisglobal.compliance.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared CSV parsing helpers for compliance domain configuration handlers.
 *
 * Header keys are lowercased so callers can lookup with case-insensitive column
 * names. The line parser handles quoted fields with embedded commas.
 */
final class ComplianceCsvUtil {

    private ComplianceCsvUtil() {
    }

    /**
     * Parses a CSV line into trimmed fields, honoring double-quoted cells.
     *
     * <p>
     * Implements the RFC 4180 escaped-quote rule: a doubled {@code ""} inside a
     * quoted field decodes to a single literal quote and does NOT end the quoted
     * region. Earlier this method toggled {@code inQuotes} on every {@code "},
     * which desynchronised parsing for the rest of the line the moment a regulation
     * title or note contained an embedded quote.
     */
    static String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped double-quote inside a quoted field — emit one
                    // literal " and skip the second character.
                    currentValue.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString().trim());
        return values.toArray(new String[0]);
    }

    /**
     * Builds a lowercased column-name → index map. Callers use lowercase keys (e.g.
     * {@code columnIndices.get("regulationnumber")}) so the input CSV header is
     * case-insensitive.
     */
    static Map<String, Integer> createColumnMap(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            columnMap.put(headers[i].trim().toLowerCase(), i);
        }
        return columnMap;
    }

    /** Returns the trimmed cell at index, or {@code ""} when out of range/null. */
    static String getValueOrEmpty(String[] values, Integer index) {
        if (index != null && index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value.trim() : "";
        }
        return "";
    }

    /** True for empty or {@code #}-prefixed comment lines. */
    static boolean isSkippableLine(String line) {
        if (line == null) {
            return true;
        }
        String trimmed = line.trim();
        return trimmed.isEmpty() || trimmed.startsWith("#");
    }

    /**
     * Reads forward until the first non-skippable line and returns it as the
     * header. Blank lines and {@code #}-prefixed comments before the header are
     * tolerated so the seed-CSV authoring rules in the README ("blank lines and #
     * comment lines are skipped") apply uniformly to the header position too.
     *
     * <p>
     * Throws {@link IllegalArgumentException} if the stream contains no header row.
     * The returned {@link HeaderRead#lineNumber} reflects the 1-based source-file
     * position of the header row, so callers can carry it forward as the running
     * line counter and keep error messages accurate.
     */
    static HeaderRead readHeaderLine(BufferedReader reader, String fileName, String emptyMessagePrefix)
            throws IOException {
        int lineNumber = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (!isSkippableLine(line)) {
                return new HeaderRead(line, lineNumber);
            }
        }
        throw new IllegalArgumentException(emptyMessagePrefix + " " + fileName + " is empty");
    }

    /** Header line + 1-based source-file line number it was read from. */
    static final class HeaderRead {
        final String line;
        final int lineNumber;

        HeaderRead(String line, int lineNumber) {
            this.line = line;
            this.lineNumber = lineNumber;
        }
    }
}
