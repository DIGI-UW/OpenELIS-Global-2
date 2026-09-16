package org.openelisglobal.configuration.service;

import org.openelisglobal.common.log.LogEvent;

/**
 * Outcome counters for one catalog CSV file. A handler records every data row
 * as created, updated or skipped and closes the file with a single greppable
 * line: {@code SUMMARY file=<name> domain=<domain> created=<n> updated=<n>
 * skipped=<n>}. Skipped rows are logged with their line number and the reason
 * at the moment they are recorded.
 */
public final class CsvLoadSummary {

    public enum Outcome {
        CREATED, UPDATED, SKIPPED
    }

    private final String domain;
    private final String fileName;
    private int created;
    private int updated;
    private int skipped;

    public CsvLoadSummary(String domain, String fileName) {
        this.domain = domain;
        this.fileName = fileName;
    }

    public void record(LoadedRow<?> row, String loggingClass, int lineNumber) {
        switch (row.outcome()) {
        case CREATED -> created++;
        case UPDATED -> updated++;
        default -> skipped(loggingClass, lineNumber, row.reason());
        }
    }

    public void skipped(String loggingClass, int lineNumber, String reason) {
        skipped++;
        LogEvent.logWarn(loggingClass, "processConfiguration",
                "Skipping line " + lineNumber + " in " + fileName + ": " + reason);
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
