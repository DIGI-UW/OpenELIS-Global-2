package org.openelisglobal.biorepository.controller.rest;

import java.util.Arrays;
import java.util.List;

/**
 * Parses storage hierarchical paths for biorepository QC scope matching.
 */
public final class BiorepositoryQcHierarchyParser {

    private BiorepositoryQcHierarchyParser() {
    }

    public static String[] parseHierarchyLevels(String locationPath) {
        String[] defaults = new String[] { "Unknown", "Unknown", "Unknown", "Unknown" };
        if (locationPath == null || locationPath.isBlank()) {
            return defaults;
        }
        List<String> segments = Arrays.stream(locationPath.split("\\s*>\\s*")).map(String::trim)
                .filter(s -> !s.isBlank()).toList();
        if (segments.isEmpty()) {
            return defaults;
        }
        int end = segments.size();
        String tail = segments.get(end - 1);
        if (isWellCoordinateTail(tail)) {
            end = end - 1;
        }
        List<String> structural = segments.subList(0, Math.max(end, 0));
        if (structural.isEmpty()) {
            return defaults;
        }
        if (structural.size() >= 4) {
            int boxIdx = structural.size() - 1;
            int rackIdx = structural.size() - 2;
            int shelfIdx = structural.size() - 3;
            int deviceIdx = structural.size() - 4;
            return new String[] { structural.get(deviceIdx), structural.get(shelfIdx), structural.get(rackIdx),
                    structural.get(boxIdx) };
        }
        if (structural.size() == 3) {
            return new String[] { structural.get(0), structural.get(1), structural.get(2), "Unknown" };
        }
        if (structural.size() == 2) {
            return new String[] { structural.get(0), structural.get(1), "Unknown", "Unknown" };
        }
        return new String[] { structural.get(0), "Unknown", "Unknown", "Unknown" };
    }

    public static String[] parseRoomPrefixedRackLevels(String locationPath) {
        String[] defaults = new String[] { "Unknown", "Unknown", "Unknown", "Unknown" };
        if (locationPath == null || locationPath.isBlank()) {
            return defaults;
        }
        List<String> segments = Arrays.stream(locationPath.split("\\s*>\\s*")).map(String::trim)
                .filter(s -> !s.isBlank()).toList();
        if (segments.size() < 4) {
            return defaults;
        }
        int end = segments.size();
        String tail = segments.get(end - 1);
        if (isWellCoordinateTail(tail)) {
            end = end - 1;
        }
        List<String> structural = segments.subList(0, Math.max(end, 0));
        if (structural.size() == 4) {
            return new String[] { structural.get(1), structural.get(2), structural.get(3), "Unknown" };
        }
        return defaults;
    }

    static boolean isWellCoordinateTail(String tail) {
        if (tail == null || tail.isBlank()) {
            return false;
        }
        return tail.matches("^[A-Za-z]+\\d+$") || tail.matches("^\\d+$");
    }
}
