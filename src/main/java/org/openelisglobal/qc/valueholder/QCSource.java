package org.openelisglobal.qc.valueholder;

import java.util.List;
import java.util.Locale;

/**
 * OGC-1147 — where a QC result came from, and the single source of truth for
 * that vocabulary. The {@code qc_result.source} CHECK constraint (changeset
 * qc-022) is derived from this enum; a new source means editing the enum and
 * amending the CHECK.
 *
 * <p>
 * Deliberately only the three values the acceptance criteria require. A wider
 * {@code WORKPLAN / QC_MODULE / ANALYZER_IMPORT / ANALYZER_LIST} vocabulary was
 * once proposed but appears in neither OGC-427 nor OGC-428 nor anywhere in this
 * codebase — those stories add their own values when they land.
 */
public enum QCSource {

    /**
     * Analyzer-transmitted via the FHIR import. Every row that predates OGC-1147.
     */
    ASTM,

    /** Bench quantitative control: a measured number judged against a target. */
    MANUAL,

    /** Rapid diagnostic test control line: qualitative, never a number. */
    RDT;

    /** Every bench-entered source, for queries that cover the bench as a whole. */
    public static final List<QCSource> BENCH_SOURCES = List.of(MANUAL, RDT);

    /**
     * Resolve the {@code source} request parameter of the bench listings. Blank or
     * {@code ALL} means every bench source, returned as null so a query can leave
     * the filter off.
     *
     * @throws IllegalArgumentException if the text names no source at all, or names
     *                                  the analyzer source, which belongs to the
     *                                  instrument views rather than a bench listing
     */
    public static QCSource parseBenchFilter(String source) {
        if (source == null || source.isBlank() || "ALL".equalsIgnoreCase(source)) {
            return null;
        }
        QCSource parsed = QCSource.valueOf(source.toUpperCase(Locale.ROOT));
        if (!parsed.isBenchEntered()) {
            throw new IllegalArgumentException("Analyzer QC belongs to the instrument views, not the bench listing");
        }
        return parsed;
    }

    /**
     * Whether results of this source are entered by a technician rather than
     * received from an instrument. Bench-entered results carry the real session
     * user and may have no analyzer; analyzer results carry the automation user.
     */
    public boolean isBenchEntered() {
        return this != ASTM;
    }

    /** Whether a result of this source carries a numeric {@code result_value}. */
    public boolean isQuantitative() {
        return this != RDT;
    }
}
