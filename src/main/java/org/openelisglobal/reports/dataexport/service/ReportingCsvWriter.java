package org.openelisglobal.reports.dataexport.service;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.reports.dataexport.form.ExportField;
import org.openelisglobal.reports.dataexport.form.ExportRecord;
import org.springframework.stereotype.Component;

/**
 * Streams CSV without holding the dataset or all repeats of a specimen in
 * memory. Sources order records by group, then real event (if any), then stable
 * identity.
 */
@Component
public class ReportingCsvWriter {
    public enum Layout {
        SPREADSHEET, RESULT_LIST, TABLE
    }

    public long write(Writer output, Layout layout, List<ExportField> fields, Iterator<ExportRecord> records)
            throws IOException {
        if (layout == null || fields == null || fields.isEmpty()
                || fields.stream().map(ExportField::id).distinct().count() != fields.size()) {
            throw new IllegalArgumentException("reporting.columns.invalid");
        }
        output.write('\uFEFF');
        writeRow(output, fields.stream().map(ExportField::header).toList());
        Sink sink = new Sink(output, fields);
        if (layout != Layout.SPREADSHEET) {
            while (records.hasNext()) {
                ExportRecord record = records.next();
                sink.row(record.attributes(), record.measurements());
            }
        } else {
            Spreadsheet group = null;
            while (records.hasNext()) {
                ExportRecord record = records.next();
                if (group == null || !group.id.equals(record.groupId())) {
                    if (group != null) {
                        group.finish();
                    }
                    group = new Spreadsheet(record, sink);
                }
                group.accept(record);
            }
            if (group != null) {
                group.finish();
            }
        }
        output.flush();
        return sink.rows;
    }

    /**
     * Prefix a cell that a spreadsheet would evaluate as a formula.
     *
     * <p>
     * Excel and LibreOffice treat a leading {@code = + - @} as the start of a
     * formula, so an exported patient name, result value or analyzer comment can
     * execute when reporting staff open the file. CSV quoting does not help: the
     * quotes are consumed by the parser and the cell is still evaluated. The
     * leading character may also be preceded by tab, CR or LF, which those
     * applications skip before deciding.
     *
     * <p>
     * A bare {@code '} prefix is the usual mitigation, but applying it to every
     * match would corrupt legitimate negative numbers, and deltas and reference
     * ranges are routinely negative here. So a numeric cell leading with {@code -}
     * is left alone; every other match is prefixed.
     */
    private static String neutralizeFormula(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int start = 0;
        while (start < value.length() && isSkippedBeforeFormula(value.charAt(start))) {
            start++;
        }
        if (start == value.length()) {
            return value;
        }
        char lead = value.charAt(start);
        if (lead != '=' && lead != '+' && lead != '-' && lead != '@') {
            return value;
        }
        // Only '-' gets the numeric exemption. A leading '+' also starts a formula
        // in Excel and parses as a number, so exempting every numeric-looking cell
        // would let "+1" through; an explicitly signed positive is not a value this
        // export produces, while negatives are.
        if (lead == '-' && isNumeric(value.substring(start))) {
            return value;
        }
        return "'" + value;
    }

    private static boolean isSkippedBeforeFormula(char c) {
        return c == '\t' || c == '\r' || c == '\n' || c == ' ';
    }

    private static boolean isNumeric(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void writeRow(Writer output, List<String> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i != 0) {
                output.write(',');
            }
            String value = neutralizeFormula(cells.get(i));
            if (value == null) {
                continue;
            }
            if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0
                    || value.indexOf('\r') >= 0) {
                output.write('"');
                output.write(value.replace("\"", "\"\""));
                output.write('"');
            } else {
                output.write(value);
            }
        }
        output.write("\r\n");
    }

    private static final class Sink {
        private final Writer output;
        private final List<ExportField> fields;
        private final Set<String> measurementIds;
        private long rows;

        private Sink(Writer output, List<ExportField> fields) {
            this.output = output;
            this.fields = List.copyOf(fields);
            measurementIds = fields.stream().filter(ExportField::measurement).map(ExportField::id)
                    .collect(Collectors.toSet());
        }

        private void row(Map<String, String> attributes, Map<String, String> measurements) throws IOException {
            List<String> cells = new ArrayList<>(fields.size());
            for (ExportField field : fields) {
                cells.add((field.measurement() ? measurements : attributes).get(field.id()));
            }
            writeRow(output, cells);
            rows++;
        }
    }

    private static final class Spreadsheet {
        private final String id;
        private final Map<String, String> attributes;
        private final Sink sink;
        private final long startingRows;
        // First readings remain pending only until a second reading proves that
        // field repeats. At most one pending reading per selected field is held.
        private final Map<String, Map<String, String>> pending = new LinkedHashMap<>();
        private final Set<String> repeated = new HashSet<>();
        private final Map<String, String> eventValues = new LinkedHashMap<>();
        private String eventId;

        private Spreadsheet(ExportRecord first, Sink sink) {
            id = first.groupId();
            attributes = first.attributes();
            this.sink = sink;
            startingRows = sink.rows;
        }

        private void accept(ExportRecord record) throws IOException {
            if (sink.measurementIds.isEmpty()) {
                return;
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (String key : sink.measurementIds) {
                if (record.measurements().containsKey(key)) {
                    values.put(key, record.measurements().get(key));
                }
            }
            if (values.isEmpty()) {
                return;
            }
            if (!Objects.equals(eventId, record.eventId())) {
                flushEvent();
                eventId = record.eventId();
            }
            if (eventId != null) {
                if (overlap(eventValues.keySet(), values.keySet())) {
                    flushEvent();
                }
                eventValues.putAll(values);
                return;
            }
            boolean isRepeat = overlap(repeated, values.keySet());
            Iterator<Map<String, String>> previous = pending.values().iterator();
            while (previous.hasNext()) {
                Map<String, String> first = previous.next();
                if (overlap(first.keySet(), values.keySet())) {
                    sink.row(attributes, first);
                    repeated.addAll(first.keySet());
                    previous.remove();
                    isRepeat = true;
                }
            }
            if (isRepeat) {
                repeated.addAll(values.keySet());
                sink.row(attributes, values);
            } else {
                pending.put(record.id(), values);
            }
        }

        private static boolean overlap(Set<String> left, Set<String> right) {
            return left.stream().anyMatch(right::contains);
        }

        private void flushEvent() throws IOException {
            if (!eventValues.isEmpty()) {
                sink.row(attributes, eventValues);
                eventValues.clear();
            }
        }

        private void finish() throws IOException {
            flushEvent();
            Map<String, String> singletons = new LinkedHashMap<>();
            pending.values().forEach(singletons::putAll);
            if (!singletons.isEmpty() || sink.rows == startingRows) {
                sink.row(attributes, singletons);
            }
        }
    }
}
