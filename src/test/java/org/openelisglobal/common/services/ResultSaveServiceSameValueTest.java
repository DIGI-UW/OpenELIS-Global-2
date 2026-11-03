package org.openelisglobal.common.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

/**
 * Whether a saved result value changed, which decides if a result already on an
 * issued patient report is booked as corrected. Validation saves a numeric
 * value back padded to its significant digits; that is not a correction.
 */
public class ResultSaveServiceSameValueTest extends BaseWebContextSensitiveTest {

    @Test
    public void aNumericValuePaddedToItsSignificantDigitsIsTheSameValue() {
        assertTrue(ResultSaveService.isSameValue("1.20", "1.2", "N"));
        assertTrue(ResultSaveService.isSameValue("0.50", "0.5", "N"));
        assertTrue(ResultSaveService.isSameValue("25", "25.0", "N"));
    }

    @Test
    public void aNumericValueInAnotherNotationIsTheSameValue() {
        assertTrue(ResultSaveService.isSameValue("6.5E1", "65", "N"));
    }

    @Test
    public void aDifferentNumberIsAChange() {
        assertFalse(ResultSaveService.isSameValue("1.3", "1.2", "N"));
        assertFalse(ResultSaveService.isSameValue("<5", "5", "N"));
    }

    @Test
    public void nonNumericValuesCompareAsWritten() {
        assertFalse(ResultSaveService.isSameValue("1.20", "1.2", "A"));
        assertTrue(ResultSaveService.isSameValue("Positive", "Positive", "A"));
    }

    /**
     * OGC-1569: the Results API serves a numeric result rounded to its decimal
     * places; posting the row back unchanged sent that rounded form and replaced
     * the measurement with it. A client that posts no measurement of its own, or
     * the stored one, has not changed the value.
     */
    @Test
    public void theRoundedFormOfTheStoredValueKeepsTheStoredValue() {
        assertEquals("0.9", ResultSaveService.valueToStore("1", null, "0.9", "N", 0));
        assertEquals("0.9", ResultSaveService.valueToStore("1", "0.9", "0.9", "N", 0));
        assertEquals("12.345", ResultSaveService.valueToStore("12.35", "", "12.345", "N", 2));
        assertEquals("0.044", ResultSaveService.valueToStore("0.04", "0.044", "0.044", "N", 2));
    }

    /**
     * The Results page edits from the measurement and sends what was typed as its
     * measurement, so typing the rounded form of the stored value is a change.
     */
    @Test
    public void aMeasurementThatDiffersFromTheStoredOneIsWhatTheUserEntered() {
        assertEquals("1", ResultSaveService.valueToStore("1", "1", "0.9", "N", 0));
        assertEquals("12.35", ResultSaveService.valueToStore("12.35", "12.35", "12.345", "N", 2));
    }

    @Test
    public void anyOtherValueIsWhatTheUserEnteredAndIsStored() {
        assertEquals("2", ResultSaveService.valueToStore("2", null, "0.9", "N", 0));
        assertEquals("2", ResultSaveService.valueToStore("2", "0.9", "0.9", "N", 0));
        assertEquals("1.0", ResultSaveService.valueToStore("1.0", null, "0.9", "N", 0));
        assertEquals("12.34", ResultSaveService.valueToStore("12.34", null, "12.345", "N", 2));
    }

    @Test
    public void aFirstValueTextAndUnroundedTestsAreStoredAsSent() {
        assertEquals("1", ResultSaveService.valueToStore("1", null, "", "N", 0));
        assertEquals("1", ResultSaveService.valueToStore("1", null, "0.9", "A", 0));
        assertEquals("1", ResultSaveService.valueToStore("1", null, "0.9", "N", -1));
        assertEquals("", ResultSaveService.valueToStore("", null, "0.9", "N", 0));
    }

    @Test
    public void anUnchangedValueIsStoredAsSentSoPaddingStillCountsAsTheSame() {
        assertEquals("1.20", ResultSaveService.valueToStore("1.20", null, "1.2", "N", 2));
    }
}
