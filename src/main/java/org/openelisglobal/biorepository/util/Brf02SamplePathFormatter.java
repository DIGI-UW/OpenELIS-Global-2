package org.openelisglobal.biorepository.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Formats biorepository storage locations for AHRI BR-F-02 sample path display.
 * Example: Zn Room-A / FRZ Freezer-1 / SH S2 / RK R15 / Box BX078 / Pos B3
 */
public final class Brf02SamplePathFormatter {

    private Brf02SamplePathFormatter() {
    }

    public static String format(Map<String, Object> location) {
        if (location == null || location.isEmpty()) {
            return null;
        }

        String roomName = text(location.get("roomName"));
        String deviceName = text(location.get("deviceName"));
        String shelfLabel = text(location.get("shelfLabel"));
        String rackLabel = text(location.get("rackLabel"));
        String boxLabel = text(location.get("boxLabel"));
        String positionCoordinate = text(location.get("positionCoordinate"));

        if (roomName == null && deviceName == null && shelfLabel == null && rackLabel == null
                && boxLabel == null) {
            String hierarchicalPath = text(location.get("hierarchicalPath"));
            if (hierarchicalPath == null) {
                hierarchicalPath = text(location.get("location"));
            }
            return formatFromHierarchicalPath(hierarchicalPath, positionCoordinate);
        }

        List<String> segments = new ArrayList<>();
        appendSegment(segments, "Zn", roomName);
        appendSegment(segments, "FRZ", deviceName);
        appendSegment(segments, "SH", shelfLabel);
        appendSegment(segments, "RK", rackLabel);
        appendSegment(segments, "Box", boxLabel);
        appendSegment(segments, "Pos", positionCoordinate);

        return segments.isEmpty() ? null : String.join(" / ", segments);
    }

    public static String formatFromHierarchicalPath(String hierarchicalPath, String positionCoordinate) {
        if (hierarchicalPath == null || hierarchicalPath.isBlank()) {
            return positionCoordinate != null && !positionCoordinate.isBlank()
                    ? "Pos " + positionCoordinate.trim()
                    : null;
        }

        String[] parts = hierarchicalPath.split("\\s*>\\s*");
        String roomName = parts.length > 0 ? parts[0].trim() : null;
        String deviceName = parts.length > 1 ? parts[1].trim() : null;
        String shelfLabel = parts.length > 2 ? parts[2].trim() : null;
        String rackLabel = parts.length > 3 ? parts[3].trim() : null;
        String boxLabel = parts.length > 4 ? parts[4].trim() : null;

        List<String> segments = new ArrayList<>();
        appendSegment(segments, "Zn", roomName);
        appendSegment(segments, "FRZ", deviceName);
        appendSegment(segments, "SH", shelfLabel);
        appendSegment(segments, "RK", rackLabel);
        appendSegment(segments, "Box", boxLabel);
        appendSegment(segments, "Pos", positionCoordinate);

        return segments.isEmpty() ? hierarchicalPath.trim() : String.join(" / ", segments);
    }

    private static void appendSegment(List<String> segments, String prefix, String value) {
        if (value != null && !value.isBlank()) {
            segments.add(prefix + " " + value.trim());
        }
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String str = String.valueOf(value).trim();
        return str.isEmpty() ? null : str;
    }
}
