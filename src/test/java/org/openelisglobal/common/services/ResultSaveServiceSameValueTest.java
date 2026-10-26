package org.openelisglobal.common.services;

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
}
