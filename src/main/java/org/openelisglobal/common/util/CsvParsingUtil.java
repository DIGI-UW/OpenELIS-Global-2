package org.openelisglobal.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared CSV parsing helpers for startup configuration handlers
 * ({@code DomainConfigurationHandler} implementations) and any other seed-CSV
 * reader.
 *
 * <p>
 * This is the single canonical implementation. Configuration handlers used to
 * each carry a private copy of {@code parseCsvLine} that toggled
 * {@code inQuotes} on every {@code "}; that desynchronises the rest of the line
 * as soon as a quoted cell contains an escaped quote. Add call sites here
 * rather than reintroducing per-handler copies.
 */
public final class CsvParsingUtil {

    private CsvParsingUtil() {
    }

    /**
     * Opens a CSV file the way a spreadsheet saved it. Excel's "CSV UTF-8" starts
     * with a byte order mark, which is dropped so it never becomes part of the
     * first column name; a file that is not valid UTF-8 is Excel's plain "CSV",
     * which is Windows-1252, and is read as such so its accented letters survive.
     */
    public static BufferedReader openCsvReader(InputStream in) throws IOException {
        byte[] bytes = in.readAllBytes();
        int start = bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF ? 3 : 0;
        ByteBuffer body = ByteBuffer.wrap(bytes, start, bytes.length - start);
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(body).toString();
        } catch (CharacterCodingException notUtf8) {
            text = new String(bytes, start, bytes.length - start, Charset.forName("windows-1252"));
        }
        return new BufferedReader(new StringReader(text));
    }

    /**
     * Parses a CSV line into trimmed fields, honoring double-quoted cells.
     *
     * <p>
     * Implements the RFC 4180 escaped-quote rule: a doubled {@code ""} inside a
     * quoted field decodes to a single literal quote and does NOT end the quoted
     * region.
     */
    public static String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
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
    public static Map<String, Integer> createColumnMap(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            if (i == 0) {
                header = stripByteOrderMark(header);
            }
            columnMap.put(header.trim().toLowerCase(), i);
        }
        return columnMap;
    }

    public static String stripByteOrderMark(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF' ? value.substring(1) : value;
    }

    /** Returns the trimmed cell at index, or {@code ""} when out of range/null. */
    public static String getValueOrEmpty(String[] values, Integer index) {
        if (index != null && index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value.trim() : "";
        }
        return "";
    }

    /** True for empty or {@code #}-prefixed comment lines. */
    public static boolean isSkippableLine(String line) {
        if (line == null) {
            return true;
        }
        String trimmed = line.trim();
        return trimmed.isEmpty() || trimmed.startsWith("#");
    }

    /**
     * Returns the index of the column whose header equals {@code name}, ignoring
     * case and surrounding whitespace, or {@code -1} when absent.
     */
    public static int findColumn(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i] != null && name.equalsIgnoreCase(stripByteOrderMark(headers[i]).trim())) {
                return i;
            }
        }
        return -1;
    }
}
