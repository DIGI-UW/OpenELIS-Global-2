package org.openelisglobal.qc.valueholder;

/**
 * OGC-1147 — where a QC result came from, and the single source of truth for
 * that vocabulary. The {@code qc_result.source} CHECK constraint (changeset
 * qc-022) is derived from this enum; a new source means editing the enum and
 * amending the CHECK.
 *
 * <p>
 * Deliberately only the three values the acceptance criteria require. The
 * {@code WORKPLAN / QC_MODULE / ANALYZER_IMPORT / ANALYZER_LIST} vocabulary
 * quoted by the FRS appears in neither OGC-427 nor OGC-428 nor anywhere in this
 * codebase — those stories add their own values when they land.
 */
public enum QCSource {

    /**
     * Analyzer-transmitted via the FHIR import. Every row that predates OGC-1147.
     */
    ASTM,

    /** Bench quantitative control: a measured number judged against a target. */
    MANUAL,

    /** Rapid diagnostic test control line: qualitative, never a number (FR-A3). */
    RDT;

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
