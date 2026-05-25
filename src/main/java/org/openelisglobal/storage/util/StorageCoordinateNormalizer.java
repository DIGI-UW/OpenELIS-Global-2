package org.openelisglobal.storage.util;

import org.apache.commons.validator.GenericValidator;

/**
 * Normalizes storage position coordinates to match a box's position schema hint.
 */
public final class StorageCoordinateNormalizer {

    private StorageCoordinateNormalizer() {
    }

    public static String normalize(String coordinate, String positionSchemaHint, Integer columns) {
        if (GenericValidator.isBlankOrNull(coordinate)) {
            return coordinate;
        }
        int cols = columns != null && columns > 0 ? columns : 12;
        String hint = GenericValidator.isBlankOrNull(positionSchemaHint) ? "letter-number" : positionSchemaHint.trim();

        int[] indices = parseToIndices(coordinate.trim(), cols);
        if (indices == null) {
            return coordinate;
        }
        return format(indices[0], indices[1], cols, hint);
    }

    private static int[] parseToIndices(String coordinate, int columns) {
        java.util.regex.Matcher rowCol = java.util.regex.Pattern.compile("^(\\d+)-(\\d+)$").matcher(coordinate);
        if (rowCol.matches()) {
            return new int[] { Integer.parseInt(rowCol.group(1)) - 1, Integer.parseInt(rowCol.group(2)) - 1 };
        }

        java.util.regex.Matcher letterCol = java.util.regex.Pattern.compile("^([A-Za-z])(\\d+)$").matcher(coordinate);
        if (letterCol.matches()) {
            int rowIdx = Character.toUpperCase(letterCol.group(1).charAt(0)) - 'A';
            int colIdx = Integer.parseInt(letterCol.group(2)) - 1;
            return new int[] { rowIdx, colIdx };
        }

        java.util.regex.Matcher continuous = java.util.regex.Pattern.compile("^(\\d+)$").matcher(coordinate);
        if (continuous.matches() && columns > 0) {
            int index = Integer.parseInt(continuous.group(1)) - 1;
            return new int[] { index / columns, index % columns };
        }

        return null;
    }

    private static String format(int rowIdx, int colIdx, int columns, String hint) {
        if ("letter-number".equals(hint)) {
            char letter = (char) ('A' + rowIdx);
            return letter + String.valueOf(colIdx + 1);
        }
        if ("number-number".equals(hint)) {
            return (rowIdx + 1) + "-" + (colIdx + 1);
        }
        if ("continuous".equals(hint)) {
            return String.valueOf(rowIdx * columns + colIdx + 1);
        }
        return (rowIdx + 1) + "-" + (colIdx + 1);
    }

    public static java.util.Map<Integer, String> normalizeMap(java.util.Map<Integer, String> wellAssignments,
            String positionSchemaHint, Integer columns) {
        if (wellAssignments == null || wellAssignments.isEmpty()) {
            return wellAssignments;
        }
        java.util.Map<Integer, String> normalized = new java.util.HashMap<>();
        for (java.util.Map.Entry<Integer, String> entry : wellAssignments.entrySet()) {
            normalized.put(entry.getKey(), normalize(entry.getValue(), positionSchemaHint, columns));
        }
        return normalized;
    }
}
