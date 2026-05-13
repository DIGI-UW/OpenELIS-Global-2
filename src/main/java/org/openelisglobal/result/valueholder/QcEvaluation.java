package org.openelisglobal.result.valueholder;

/**
 * QC evaluation outcome for a result whose sample item has a QC profile.
 */
public enum QcEvaluation {

    /** Result meets acceptance criteria. */
    PASS,

    /** Result fails acceptance criteria. */
    FAIL,

    /**
     * Evaluation not applicable (threshold not configured, non-numeric result,
     * etc.).
     */
    N_A;

    /**
     * Returns the enum value matching the database string, or null if not found.
     */
    public static QcEvaluation fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
