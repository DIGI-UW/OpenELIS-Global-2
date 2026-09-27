package org.openelisglobal.testresult.valueholder;

/**
 * Catalog-configured surveillance positivity classification of a test result
 * ({@code test_result.significance}). This is the single source of truth for
 * the allowed values; positivity indices match on the exact canonical name, so
 * a catalog value outside this set can never be counted and must be rejected at
 * import rather than silently stored.
 */
public enum TestResultSignificance {
    POSITIVE, NEGATIVE, INDETERMINATE;

    /**
     * True when {@code value} exactly names a known classification
     * (case-sensitive).
     */
    public static boolean isRecognized(String value) {
        if (value == null) {
            return false;
        }
        for (TestResultSignificance s : values()) {
            if (s.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
