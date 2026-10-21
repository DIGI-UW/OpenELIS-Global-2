package org.openelisglobal.analyzer.service;

import java.util.Set;

/**
 * How the profile the Bridge serves now reads compared with how it read when
 * the mapping was confirmed (spec point 13): the type as a whole, and each
 * confirmed row, a record or one value of a record. {@code arrival} is how the
 * delivery's own profile read it, when that is known.
 */
public record AnalyzerConfirmationReading(boolean typeChanged, Set<AnalyzerMappingSourceRow> changedRows,
        AnalyzerArrivalReading arrival) {

    public AnalyzerConfirmationReading {
        changedRows = changedRows == null ? Set.of() : Set.copyOf(changedRows);
    }

    public AnalyzerConfirmationReading(boolean typeChanged, Set<AnalyzerMappingSourceRow> changedRows) {
        this(typeChanged, changedRows, null);
    }

    public static AnalyzerConfirmationReading unchanged() {
        return new AnalyzerConfirmationReading(false, Set.of());
    }

    static AnalyzerConfirmationReading typeReadingChanged() {
        return new AnalyzerConfirmationReading(true, Set.of());
    }

    AnalyzerConfirmationReading arrivingAs(AnalyzerArrivalReading reading) {
        return new AnalyzerConfirmationReading(typeChanged, changedRows, reading);
    }

    public boolean isUnchanged() {
        return !typeChanged && changedRows.isEmpty();
    }

    /**
     * Whether results for this record wait for a person to confirm how it now
     * reads.
     */
    public boolean holdsRecord(String code, String subIdentity) {
        return typeChanged || changedRows.contains(new AnalyzerMappingSourceRow(code, null, normalized(subIdentity)));
    }

    /** Whether results carrying this value of the record wait for a person. */
    public boolean holdsValue(String code, String subIdentity, String rawValue) {
        return holdsRecord(code, subIdentity) || rawValue != null
                && changedRows.contains(new AnalyzerMappingSourceRow(code, rawValue, normalized(subIdentity)));
    }

    private static String normalized(String subIdentity) {
        return subIdentity == null ? "" : subIdentity;
    }
}
