package org.openelisglobal.configuration.service;

/**
 * What became of one catalog CSV row: the outcome, the value it produced (an
 * entity, an id, a list of ids) and, for a skipped row, the reason.
 */
public record LoadedRow<T>(CsvLoadSummary.Outcome outcome, T value, String reason) {

    public static <T> LoadedRow<T> created(T value) {
        return new LoadedRow<>(CsvLoadSummary.Outcome.CREATED, value, null);
    }

    public static <T> LoadedRow<T> updated(T value) {
        return new LoadedRow<>(CsvLoadSummary.Outcome.UPDATED, value, null);
    }

    public static <T> LoadedRow<T> skipped(String reason) {
        return new LoadedRow<>(CsvLoadSummary.Outcome.SKIPPED, null, reason);
    }

    public boolean isSkipped() {
        return outcome == CsvLoadSummary.Outcome.SKIPPED;
    }
}
