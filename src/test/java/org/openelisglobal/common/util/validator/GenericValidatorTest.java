package org.openelisglobal.common.util.validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GenericValidatorTest {

    @Test
    public void isBool_shouldReturnTrueForTrueLowercase() {
        assertTrue(GenericValidator.isBool("true"));
    }

    @Test
    public void isBool_shouldReturnTrueForTrueUppercase() {
        assertTrue(GenericValidator.isBool("TRUE"));
    }

    @Test
    public void isBool_shouldReturnTrueForFalseLowercase() {
        assertTrue(GenericValidator.isBool("false"));
    }

    @Test
    public void isBool_shouldReturnTrueForFalseUppercase() {
        assertTrue(GenericValidator.isBool("FALSE"));
    }

    @Test
    public void isBool_shouldReturnFalseForInvalidValue() {
        assertFalse(GenericValidator.isBool("yes"));
        assertFalse(GenericValidator.isBool("no"));
        assertFalse(GenericValidator.isBool("1"));
        assertFalse(GenericValidator.isBool("0"));
    }

    @Test
    public void is24HourTime_shouldReturnTrueForValidTime() {
        assertTrue(GenericValidator.is24HourTime("00:00"));
        assertTrue(GenericValidator.is24HourTime("14:30"));
        assertTrue(GenericValidator.is24HourTime("23:59"));
        assertTrue(GenericValidator.is24HourTime("9:30"));
    }

    @Test
    public void is24HourTime_shouldReturnFalseForInvalidTime() {
        assertFalse(GenericValidator.is24HourTime("25:00"));
        assertFalse(GenericValidator.is24HourTime("12:60"));
        assertFalse(GenericValidator.is24HourTime("24:00"));
        assertFalse(GenericValidator.is24HourTime("abc"));
    }

    @Test
    public void is24HourTime_shouldReturnFalseForNull() {
        assertFalse(GenericValidator.is24HourTime(null));
    }

    @Test
    public void is24HourTime_shouldReturnFalseForEmptyString() {
        assertFalse(GenericValidator.is24HourTime(""));
    }
}
