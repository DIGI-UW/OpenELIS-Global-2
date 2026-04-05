package org.openelisglobal.analyzerimport.analyzerreaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts ELISA plate-grid CSV (8x12) to well-per-row format for the
 * GenericFile pipeline.
 *
 * <p>
 * Detects Tecan Magellan / Thermo Multiskan FC plate grid layout: metadata
 * key-value rows, then header row (&lt;&gt; or empty, 1-12), then 8 data rows
 * (A-H with 12 OD values each).
 *
 * <p>
 * Supports dual-grid format (Multiskan FC SkanIt export): first grid contains
 * OD values, second grid contains sample IDs. Both grids are merged into a
 * single well-per-row output.
 *
 * <p>
 * Output: tab-delimited rows with WellPosition, SampleID, and result column
 * (configurable, defaults to OD_450).
 */
public final class PlateGridNormalizer {

    private static final String[] ROW_LETTERS = { "A", "B", "C", "D", "E", "F", "G", "H" };
    private static final String DEFAULT_RESULT_COLUMN = "OD_450";

    private PlateGridNormalizer() {
    }

    /**
     * Detect if content appears to be plate-grid format.
     */
    public static boolean isPlateGridFormat(List<String> lines) {
        if (lines == null || lines.size() < 10) {
            return false;
        }
        int gridStart = findGridStart(lines, 0);
        if (gridStart < 0) {
            return false;
        }
        if (gridStart + 9 > lines.size()) {
            return false;
        }
        String headerLine = lines.get(gridStart);
        if (!headerLine.contains("\t") && !headerLine.contains(",") && !headerLine.contains(";")) {
            return false;
        }
        String delim = detectDelimiter(headerLine);
        String[] headerCells = headerLine.split(escapeDelim(delim), -1);
        if (headerCells.length < 13) {
            return false;
        }
        for (int r = 1; r <= 8; r++) {
            String rowLine = lines.get(gridStart + r);
            if (rowLine == null || rowLine.isEmpty()) {
                return false;
            }
            String[] cells = rowLine.split(escapeDelim(delim), -1);
            if (cells.length < 13) {
                return false;
            }
            String rowLabel = cells[0].trim();
            if (!rowLabel.equalsIgnoreCase(ROW_LETTERS[r - 1])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convert plate-grid lines to well-per-row format.
     *
     * @param lines     Raw file lines
     * @param delimiter Delimiter used in the file
     * @return List of tab-delimited lines: header row + one row per well
     */
    public static List<String> normalizeToWellPerRow(List<String> lines, String delimiter) {
        return normalizeToWellPerRow(lines, delimiter, DEFAULT_RESULT_COLUMN);
    }

    /**
     * Convert plate-grid lines to well-per-row format with configurable result
     * column name and optional dual-grid sample ID extraction.
     *
     * @param lines            Raw file lines
     * @param delimiter        Delimiter used in the file
     * @param resultColumnName Name for the result column (e.g. "OD_450", "Abs")
     * @return List of tab-delimited lines: header row + one row per well
     */
    public static List<String> normalizeToWellPerRow(List<String> lines, String delimiter, String resultColumnName) {
        List<String> result = new ArrayList<>();
        int gridStart = findGridStart(lines, 0);
        if (gridStart < 0 || gridStart + 9 > lines.size()) {
            return result;
        }

        String headerLine = lines.get(gridStart);
        String delim = (delimiter != null && !delimiter.isEmpty()) ? delimiter : detectDelimiter(headerLine);
        boolean commaDecimal = ";".equals(delim);

        String colName = (resultColumnName != null && !resultColumnName.isEmpty()) ? resultColumnName
                : DEFAULT_RESULT_COLUMN;

        // Extract OD values from first grid
        Map<String, String> odValues = extractGridValues(lines, gridStart, delim, commaDecimal);

        // Look for a second grid (sample IDs) after the first grid
        Map<String, String> sampleIds = new HashMap<>();
        int secondGridStart = findGridStart(lines, gridStart + 9);
        if (secondGridStart >= 0 && secondGridStart + 9 <= lines.size()) {
            sampleIds = extractGridValues(lines, secondGridStart, delim, false, null);
        }

        result.add("WellPosition\tSampleID\t" + colName);

        for (int r = 0; r < 8; r++) {
            for (int c = 1; c <= 12; c++) {
                String wellPos = ROW_LETTERS[r] + c;
                String odValue = odValues.getOrDefault(wellPos, "0");
                String sampleId = sampleIds.getOrDefault(wellPos, "");
                result.add(wellPos + "\t" + sampleId + "\t" + odValue);
            }
        }
        return result;
    }

    /**
     * Read stream, detect plate grid, and return normalized content if applicable.
     *
     * @param stream    Input file stream
     * @param delimiter Configured delimiter (e.g. "\t" or ",")
     * @return Normalized well-per-row lines, or null if not plate grid
     */
    public static List<String> normalizeIfPlateGrid(InputStream stream, String delimiter) throws IOException {
        List<String> lines = stripBom(readAllLines(stream));
        if (!isPlateGridFormat(lines)) {
            return null;
        }
        return normalizeToWellPerRow(lines, delimiter);
    }

    /**
     * Strip UTF-8 BOM from the first line if present.
     */
    public static List<String> stripBom(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return lines;
        }
        String first = lines.get(0);
        if (first != null && first.startsWith("\uFEFF")) {
            List<String> result = new ArrayList<>(lines);
            result.set(0, first.substring(1));
            return result;
        }
        return lines;
    }

    private static Map<String, String> extractGridValues(List<String> lines, int gridStart, String delim,
            boolean commaDecimal) {
        return extractGridValues(lines, gridStart, delim, commaDecimal, "0");
    }

    private static Map<String, String> extractGridValues(List<String> lines, int gridStart, String delim,
            boolean commaDecimal, String emptyDefault) {
        Map<String, String> values = new HashMap<>();
        for (int r = 0; r < 8; r++) {
            int lineIndex = gridStart + 1 + r;
            if (lineIndex >= lines.size()) {
                break;
            }
            String rowLine = lines.get(lineIndex);
            String[] cells = rowLine.split(escapeDelim(delim), -1);
            String rowLabel = cells.length > 0 ? cells[0].trim() : ROW_LETTERS[r];
            if (rowLabel.length() == 1 && Character.isLetter(rowLabel.charAt(0))) {
                rowLabel = rowLabel.toUpperCase();
            }
            for (int c = 1; c <= 12 && c < cells.length; c++) {
                String value = cells[c].trim();
                if (value.isEmpty()) {
                    if (emptyDefault != null) {
                        value = emptyDefault;
                    } else {
                        continue; // Skip empty cells (e.g., unassigned wells in sample ID grid)
                    }
                } else if (commaDecimal) {
                    value = normalizeCommaDecimal(value);
                }
                values.put(rowLabel + c, value);
            }
        }
        return values;
    }

    /**
     * Normalize French-locale comma decimal to period decimal. Only converts if the
     * value looks numeric with a comma (e.g. "2,345" → "2.345"). Non-numeric values
     * (sample IDs, QC names) are returned unchanged.
     */
    static String normalizeCommaDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        // Only convert if the value matches a numeric pattern with comma decimal
        if (value.matches("-?\\d+,\\d+")) {
            return value.replace(',', '.');
        }
        return value;
    }

    private static List<String> readAllLines(InputStream stream) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * Find the start of a plate grid, searching from the given offset.
     */
    static int findGridStart(List<String> lines, int fromIndex) {
        for (int i = fromIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("[\t,;]", -1);
            if (parts.length >= 12) {
                String first = parts[0].trim();
                if ((first.equals("<>") || first.isEmpty()) && parts[1].trim().matches("1")) {
                    return i;
                }
                if (first.matches("[A-Ha-h]") && parts.length >= 13) {
                    String second = parts[1].trim();
                    // Check if the second cell looks like a number (OD value)
                    // or a comma-decimal number (e.g. "2,345" for French locale)
                    if (isNumericOrCommaDecimal(second)) {
                        return i > 0 ? i - 1 : i;
                    }
                }
            }
        }
        return -1;
    }

    private static boolean isNumericOrCommaDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            // Try comma-decimal (French locale)
            return value.matches("-?\\d+,\\d+");
        }
    }

    private static String detectDelimiter(String line) {
        if (line.contains("\t")) {
            return "\t";
        }
        if (line.contains(";")) {
            return ";";
        }
        return ",";
    }

    /**
     * Escape delimiter for use in String.split() regex.
     */
    private static String escapeDelim(String delim) {
        if ("|".equals(delim) || ".".equals(delim)) {
            return "\\" + delim;
        }
        return delim;
    }
}
