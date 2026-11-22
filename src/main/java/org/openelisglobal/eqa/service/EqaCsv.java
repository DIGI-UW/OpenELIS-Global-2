package org.openelisglobal.eqa.service;

import java.util.ArrayList;
import java.util.List;

/**
 * RFC 4180 enough for the EQA exchange files (export bundles, score CSVs):
 * quoted cells may carry commas and doubled quotes; headers match by name,
 * case-insensitively.
 */
final class EqaCsv {

    private EqaCsv() {
    }

    static String[] lines(String csv) {
        return csv.split("\\r?\\n");
    }

    static int indexOf(List<String> header, String name) {
        for (int i = 0; i < header.size(); i++) {
            if (name.equalsIgnoreCase(header.get(i).trim())) {
                return i;
            }
        }
        return -1;
    }

    static String cell(List<String> cells, int index) {
        return index >= 0 && index < cells.size() ? cells.get(index).trim() : "";
    }

    static List<String> split(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString());
        return cells;
    }
}
