package org.openelisglobal.configuration.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;

/**
 * Outcome counters for one catalog CSV file. A handler records every data row
 * as created, updated or skipped and closes the file with a single greppable
 * line: {@code SUMMARY file=<name> domain=<domain> created=<n> updated=<n>
 * skipped=<n>}. Skipped rows are logged with their line number and the reason
 * at the moment they are recorded. Every row outcome is also kept, so an import
 * preview can show the plan line by line.
 */
public final class CsvLoadSummary {

    public enum Outcome {
        CREATED, UPDATED, SKIPPED
    }

    /**
     * One data row's fate: its line number, outcome and, when skipped, the reason.
     */
    public record RowOutcome(int lineNumber, Outcome outcome, String reason) {
    }

    private final String domain;
    private final String fileName;
    private final List<RowOutcome> rows = new ArrayList<>();
    private int created;
    private int updated;
    private int skipped;

    public CsvLoadSummary(String domain, String fileName) {
        this.domain = domain;
        this.fileName = fileName;
    }

    public void record(LoadedRow<?> row, String loggingClass, int lineNumber) {
        switch (row.outcome()) {
        case CREATED -> {
            created++;
            rows.add(new RowOutcome(lineNumber, Outcome.CREATED, null));
        }
        case UPDATED -> {
            updated++;
            rows.add(new RowOutcome(lineNumber, Outcome.UPDATED, null));
        }
        default -> skipped(loggingClass, lineNumber, row.reason());
        }
    }

    public void skipped(String loggingClass, int lineNumber, String reason) {
        skipped++;
        rows.add(new RowOutcome(lineNumber, Outcome.SKIPPED, reason));
        LogEvent.logWarn(loggingClass, "processConfiguration",
                "Skipping line " + lineNumber + " in " + fileName + ": " + reason);
    }

    public String getDomain() {
        return domain;
    }

    public String getFileName() {
        return fileName;
    }

    public int getCreated() {
        return created;
    }

    public int getUpdated() {
        return updated;
    }

    public int getSkipped() {
        return skipped;
    }

    public List<RowOutcome> getRows() {
        return Collections.unmodifiableList(rows);
    }

    public String toLine() {
        return "SUMMARY file=" + fileName + " domain=" + domain + " created=" + created + " updated=" + updated
                + " skipped=" + skipped;
    }

    public void log(String loggingClass) {
        LogEvent.logInfo(loggingClass, "processConfiguration", toLine());
    }

    /**
     * The deepest non-blank message in a cause chain, so a skipped row names the
     * database's own complaint ("value too long for type character varying(10)")
     * rather than the wrapper's ("could not execute batch").
     */
    public static String reason(Throwable failure) {
        String message = null;
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                message = cause.getMessage().trim();
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return message != null ? message : failure.getClass().getSimpleName();
    }
}
