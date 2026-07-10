package org.openelisglobal.testResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openelisglobal.testresult.valueholder.TestResultSignificance;

/**
 * Contract for the positivity classification enum — the single shared
 * vocabulary between the WRITE path ({@code TestResultConfigurationHandler}
 * canonicalizes catalog input to one of these names) and the READ path
 * ({@code VectorSurveillanceDAOImpl} matches
 * {@code tr.significance = 'POSITIVE'} via
 * {@code TestResultSignificance.POSITIVE.name()}). If the recognized set or the
 * exact canonical spelling drifts, positivity silently under- or over-counts;
 * these tests pin both so that drift fails a test rather than the dashboard.
 */
public class TestResultSignificanceTest {

    @Test
    public void isRecognized_acceptsEveryDeclaredClassification() {
        assertTrue(TestResultSignificance.isRecognized("POSITIVE"));
        assertTrue(TestResultSignificance.isRecognized("NEGATIVE"));
        assertTrue(TestResultSignificance.isRecognized("INDETERMINATE"));
    }

    @Test
    public void isRecognized_isCaseSensitive() {
        // The DAO matches the exact canonical name, so a lowercase/mixed value
        // could never be counted — it must not be treated as recognized.
        assertFalse(TestResultSignificance.isRecognized("positive"));
        assertFalse(TestResultSignificance.isRecognized("Positive"));
        assertFalse(TestResultSignificance.isRecognized("Indeterminate"));
    }

    @Test
    public void isRecognized_rejectsUnknownBlankAndNull() {
        assertFalse(TestResultSignificance.isRecognized("POSITIF"));
        assertFalse(TestResultSignificance.isRecognized("PRESENT"));
        assertFalse(TestResultSignificance.isRecognized(""));
        assertFalse(TestResultSignificance.isRecognized(null));
    }

    @Test
    public void canonicalNames_matchTheStoredAndQueriedContract() {
        // These exact strings are what the handler stores and what the positivity
        // queries match on; changing them is a data-model break, not a rename.
        assertEquals("POSITIVE", TestResultSignificance.POSITIVE.name());
        assertEquals("NEGATIVE", TestResultSignificance.NEGATIVE.name());
        assertEquals("INDETERMINATE", TestResultSignificance.INDETERMINATE.name());
    }
}
