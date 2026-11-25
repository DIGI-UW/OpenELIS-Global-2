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

    /**
     * The cell as it was meant: a writer's spreadsheet formula guard (an apostrophe
     * before = + - or @, see StringUtil.csvFormulaGuard) is removed, so a
     * qualitative "+" written as "'+" reads as "+".
     */
    static String cell(List<String> cells, int index) {
        String value = index >= 0 && index < cells.size() ? cells.get(index).trim() : "";
        if (value.length() > 1 && value.charAt(0) == '\'' && "=+-@".indexOf(value.charAt(1)) >= 0) {
            return value.substring(1);
        }
        return value;
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
