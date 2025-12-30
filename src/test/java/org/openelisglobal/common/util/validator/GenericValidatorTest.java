package org.openelisglobal.common.util.validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GenericValidatorTest {

    @Test
    public void isBool_shouldReturnTrueForTrue() {
        assertTrue(GenericValidator.isBool("true"));
        assertTrue(GenericValidator.isBool("TRUE"));
        assertTrue(GenericValidator.isBool("True"));
    }

    @Test
    public void isBool_shouldReturnTrueForFalse() {
        assertTrue(GenericValidator.isBool("false"));
        assertTrue(GenericValidator.isBool("FALSE"));
        assertTrue(GenericValidator.isBool("False"));
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
