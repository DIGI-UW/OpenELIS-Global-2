package org.openelisglobal.analyzerimport.analyzerreaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts ELISA plate-grid CSV (8x12) to well-per-row format for the
 * GenericFile pipeline.
 *
 * <p>
 * Detects Tecan Magellan / Thermo Multiskan FC plate grid layout: metadata
 * key-value rows, then header row (<> or empty, 1-12), then 8 data rows (A-H
 * with 12 OD values each).
 *
 * <p>
 * Output: tab-delimited rows with WellPosition, SampleID, OD_450 (or configured
 * result column).
 */
public final class PlateGridNormalizer {

    private static final String[] ROW_LETTERS = { "A", "B", "C", "D", "E", "F", "G", "H" };

    private PlateGridNormalizer() {
    }

    /**
     * Detect if content appears to be plate-grid format.
     */
    public static boolean isPlateGridFormat(List<String> lines) {
        if (lines == null || lines.size() < 10) {
            return false;
        }
        int gridStart = findGridStart(lines);
        if (gridStart < 0) {
            return false;
        }
        if (gridStart + 9 > lines.size()) {
            return false;
        }
        String headerLine = lines.get(gridStart);
        if (!headerLine.contains("\t") && !headerLine.contains(",")) {
            return false;
        }
        String delim = headerLine.contains("\t") ? "\t" : ",";
        String[] headerCells = headerLine.split(delim, -1);
        if (headerCells.length < 13) {
            return false;
        }
        for (int r = 1; r <= 8; r++) {
            String rowLine = lines.get(gridStart + r);
            if (rowLine == null || rowLine.isEmpty()) {
                return false;
            }
            String[] cells = rowLine.split(delim, -1);
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
        List<String> result = new ArrayList<>();
        int gridStart = findGridStart(lines);
        if (gridStart < 0 || gridStart + 9 > lines.size()) {
            return result;
        }

        String headerLine = lines.get(gridStart);
        String delim = (delimiter != null && !delimiter.isEmpty()) ? delimiter
                : (headerLine.contains("\t") ? "\t" : ",");

        result.add("WellPosition\tSampleID\tOD_450");

        for (int r = 0; r < 8; r++) {
            String rowLine = lines.get(gridStart + 1 + r);
            String[] cells = rowLine.split(delim, -1);
            String rowLabel = cells.length > 0 ? cells[0].trim() : ROW_LETTERS[r];
            for (int c = 1; c <= 12 && c < cells.length; c++) {
                String wellPos = rowLabel + c;
                String odValue = cells[c].trim();
                if (odValue.isEmpty()) {
                    odValue = "0";
                }
                result.add(wellPos + "\t\t" + odValue);
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
        List<String> lines = readAllLines(stream);
        if (!isPlateGridFormat(lines)) {
            return null;
        }
        return normalizeToWellPerRow(lines, delimiter);
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

    private static int findGridStart(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("[\t,]", -1);
            if (parts.length >= 12) {
                String first = parts[0].trim();
                if ((first.equals("<>") || first.isEmpty()) && parts[1].trim().matches("1")) {
                    return i;
                }
                if (first.matches("[A-Ha-h]") && parts.length >= 13) {
                    try {
                        Double.parseDouble(parts[1].trim());
                        return i - 1;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return -1;
    }
}
