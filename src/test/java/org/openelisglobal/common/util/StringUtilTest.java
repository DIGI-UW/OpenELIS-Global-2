package org.openelisglobal.common.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StringUtilTest {

    private static final String VALID_INTEGER = "123";

    @Test
    public void isNullorNill_shouldReturnTrueForNull() {
        assertTrue(StringUtil.isNullorNill(null));
    }

    @Test
    public void isNullorNill_shouldReturnTrueForEmptyString() {
        assertTrue(StringUtil.isNullorNill(""));
    }

    @Test
    public void isNullorNill_shouldReturnTrueForNullString() {
        assertTrue(StringUtil.isNullorNill("null"));
    }

    @Test
    public void isNullorNill_shouldReturnFalseForValidString() {
        assertFalse(StringUtil.isNullorNill("test"));
    }

    @Test
    public void isInteger_shouldReturnTrueForValidInteger() {
        assertTrue(StringUtil.isInteger(VALID_INTEGER));
        assertTrue(StringUtil.isInteger("-456"));
    }

    @Test
    public void isInteger_shouldReturnFalseForInvalidValue() {
        assertFalse(StringUtil.isInteger("12.34"));
        assertFalse(StringUtil.isInteger("abc"));
    }

    @Test
    public void isNumeric_shouldReturnTrueForValidNumbers() {
        assertTrue(StringUtil.isNumeric(VALID_INTEGER));
        assertTrue(StringUtil.isNumeric("3.14"));
        assertTrue(StringUtil.isNumeric("-45.67"));
    }

    @Test
    public void isNumeric_shouldReturnFalseForInvalidValue() {
        assertFalse(StringUtil.isNumeric("abc"));
        assertFalse(StringUtil.isNumeric(null));
    }

    @Test
    public void isNumeric_shouldAcceptUnicodeSuperscriptExponents() {
        assertTrue(StringUtil.isNumeric("3²"));
        assertTrue(StringUtil.isNumeric("3.5³"));
        assertTrue(StringUtil.isNumeric("3¹⁰"));
        assertTrue(StringUtil.isNumeric("3⁻²"));
    }

    @Test
    public void convertSuperscriptToScientific_shouldRewriteAsExponent() {
        assertEquals("3e2", StringUtil.convertSuperscriptToScientific("3²"));
        assertEquals("3.5e3", StringUtil.convertSuperscriptToScientific("3.5³"));
        assertEquals("3e10", StringUtil.convertSuperscriptToScientific("3¹⁰"));
        assertEquals("3e-2", StringUtil.convertSuperscriptToScientific("3⁻²"));
    }

    @Test
    public void convertSuperscriptToScientific_shouldLeavePlainInputUnchanged() {
        assertEquals("3", StringUtil.convertSuperscriptToScientific("3"));
        assertEquals("3.14", StringUtil.convertSuperscriptToScientific("3.14"));
        assertEquals("3e2", StringUtil.convertSuperscriptToScientific("3e2"));
        assertEquals("abc", StringUtil.convertSuperscriptToScientific("abc"));
        assertEquals("", StringUtil.convertSuperscriptToScientific(""));
    }

    @Test
    public void getActualNumericValue_shouldReturnConvertedFormForSuperscript() {
        assertEquals("3e2", StringUtil.getActualNumericValue("3²"));
        assertEquals("3e2", StringUtil.getActualNumericValue("<3²"));
        assertEquals("NaN", StringUtil.getActualNumericValue("abc"));
    }

    @Test
    public void blankIfNull_shouldReturnEmptyStringForNull() {
        assertEquals("", StringUtil.blankIfNull(null));
    }

    @Test
    public void blankIfNull_shouldReturnValueForNonNull() {
        assertEquals("test", StringUtil.blankIfNull("test"));
    }

    @Test
    public void safeEquals_shouldReturnTrueForBothNull() {
        assertTrue(StringUtil.safeEquals(null, null));
    }

    @Test
    public void safeEquals_shouldReturnTrueForEqualStrings() {
        assertTrue(StringUtil.safeEquals("hello", "hello"));
    }

    @Test
    public void safeEquals_shouldReturnFalseForDifferentStrings() {
        assertFalse(StringUtil.safeEquals("hello", "world"));
    }

    @Test
    public void containsOnly_shouldReturnTrueForMatchingChars() {
        assertTrue(StringUtil.containsOnly("aaaa", 'a'));
    }

    @Test
    public void containsOnly_shouldReturnFalseForMixedChars() {
        assertFalse(StringUtil.containsOnly("aaba", 'a'));
    }

    @Test
    public void containsOnly_shouldReturnFalseForNull() {
        assertFalse(StringUtil.containsOnly(null, 'a'));
    }

    @Test
    public void ellipsisString_shouldTruncateLongText() {
        assertEquals("Hello...", StringUtil.ellipsisString("Hello World", 5));
    }

    @Test
    public void ellipsisString_shouldReturnShortTextUnchanged() {
        assertEquals("Hi", StringUtil.ellipsisString("Hi", 10));
    }

    @Test
    public void capitalize_shouldCapitalizeFirstLetter() {
        assertEquals("Hello", StringUtil.capitalize("hello"));
    }

    @Test
    public void toArray_shouldSplitByComma() {
        String[] result = StringUtil.toArray("a, b, c");
        assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    @Test
    public void toArray_shouldReturnEmptyArrayForNull() {
        assertArrayEquals(new String[0], StringUtil.toArray(null));
    }

    @Test
    public void replaceCharAtIndex_shouldReplaceCharacter() {
        assertEquals("hallo", StringUtil.replaceCharAtIndex("hello", 'a', 1));
    }

    @Test
    public void replaceCharAtIndex_shouldReturnUnchangedForInvalidIndex() {
        assertEquals("hello", StringUtil.replaceCharAtIndex("hello", 'a', -1));
    }

    @Test
    public void repeat_shouldRepeatString() {
        assertEquals("ababab", StringUtil.repeat("ab", 3));
    }

    @Test
    public void countInstances_shouldCountOccurrences() {
        assertEquals(3, StringUtil.countInstances("hello world", 'l'));
    }
}
