package org.openelisglobal.accreditation.valueholder;

/**
 * OGC-686 — when an accrediting body's logo appears on a patient report.
 *
 * <p>
 * Evaluated per body and independently of every other body, so one body can be
 * {@link #ANY_ACCREDITED_TEST} while another on the same report is
 * {@link #PERCENTAGE}. The stored {@code threshold_pct} is ignored at render
 * time under {@code ANY_ACCREDITED_TEST}, which is why the column is never
 * nulled when a lab toggles the mode back and forth.
 *
 * <p>
 * This enum is the source of truth for the
 * {@code accrediting_body_visibility_mode_chk} CHECK constraint in
 * {@code liquibase/qa/013}; adding a value means widening that constraint in a
 * new changeset.
 */
public enum LogoVisibilityMode {

    /** Logo shows when at least one test on the report is accredited. Default. */
    ANY_ACCREDITED_TEST,

    /**
     * Logo shows when the accredited share of the report meets
     * {@code threshold_pct}.
     */
    PERCENTAGE
}
