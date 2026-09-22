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
    public void isNumeric_shouldAcceptScientificNotationInEveryWrittenForm() {
        assertTrue(StringUtil.isNumeric("1.5e5"));
        assertTrue(StringUtil.isNumeric("1.5E+05"));
        assertTrue(StringUtil.isNumeric("1.5 x 10^5"));
        assertTrue(StringUtil.isNumeric("1.5×10⁵"));
        assertTrue(StringUtil.isNumeric("2 X 10^-3"));
        assertTrue(StringUtil.isNumeric("10⁻³"));
    }

    @Test
    public void isNumeric_shouldRejectBareSuperscriptsAndNonFiniteValues() {
        assertFalse(StringUtil.isNumeric("3²"));
        assertFalse(StringUtil.isNumeric("3.5⁻³"));
        assertFalse(StringUtil.isNumeric("NaN"));
        assertFalse(StringUtil.isNumeric("Infinity"));
        assertFalse(StringUtil.isNumeric("1e400"));
        assertFalse(StringUtil.isNumeric("1.5e"));
    }

    @Test
    public void normalizeScientificNotation_shouldRewriteEveryFormAsCanonicalENotation() {
        assertEquals("1.5e5", StringUtil.normalizeScientificNotation("1.5e5"));
        assertEquals("1.5e5", StringUtil.normalizeScientificNotation("1.5E+05"));
        assertEquals("1.5e5", StringUtil.normalizeScientificNotation("1.5 x 10^5"));
        assertEquals("1.5e5", StringUtil.normalizeScientificNotation("1.5×10⁵"));
        assertEquals("1.5e5", StringUtil.normalizeScientificNotation(" 1.5*10^5 "));
        assertEquals("2e-3", StringUtil.normalizeScientificNotation("2×10⁻³"));
        assertEquals("1e-3", StringUtil.normalizeScientificNotation("10⁻³"));
        assertEquals("-1e3", StringUtil.normalizeScientificNotation("-10^3"));
        assertEquals("-2.5e-7", StringUtil.normalizeScientificNotation("-2.5E-07"));
    }

    @Test
    public void normalizeScientificNotation_shouldLeaveEverythingElseUnchanged() {
        assertEquals("3", StringUtil.normalizeScientificNotation("3"));
        assertEquals("3.14", StringUtil.normalizeScientificNotation("3.14"));
        assertEquals("3²", StringUtil.normalizeScientificNotation("3²"));
        assertEquals("abc", StringUtil.normalizeScientificNotation("abc"));
        assertEquals("", StringUtil.normalizeScientificNotation(""));
        assertEquals(null, StringUtil.normalizeScientificNotation(null));
    }

    @Test
    public void getActualNumericValue_shouldStripTheComparatorAndNormalize() {
        assertEquals("1.5e5", StringUtil.getActualNumericValue("<1.5×10⁵"));
        assertEquals("1.5e5", StringUtil.getActualNumericValue("1.5E5"));
        assertEquals("12.5", StringUtil.getActualNumericValue(">12.5"));
        assertEquals("NaN", StringUtil.getActualNumericValue("3²"));
        assertEquals("NaN", StringUtil.getActualNumericValue("abc"));
    }

    @Test
    public void normalizeNumericResultValue_shouldKeepTheComparatorAndPlainValues() {
        assertEquals("1.5e5", StringUtil.normalizeNumericResultValue("1.5 x 10^5"));
        assertEquals("<1.5e5", StringUtil.normalizeNumericResultValue("<1.5×10⁵"));
        assertEquals(">2e-3", StringUtil.normalizeNumericResultValue("> 2E-03"));
        assertEquals("12.50", StringUtil.normalizeNumericResultValue("12.50"));
        assertEquals("<5", StringUtil.normalizeNumericResultValue("<5"));
        assertEquals("3²", StringUtil.normalizeNumericResultValue("3²"));
        assertEquals("", StringUtil.normalizeNumericResultValue(""));
        assertEquals(null, StringUtil.normalizeNumericResultValue(null));
    }

    @Test
    public void padExponentNotation_shouldPadOnlyTheMantissa() {
        assertEquals("1.50e5", StringUtil.padExponentNotation("1.5e5", 2));
        assertEquals("3.00e2", StringUtil.padExponentNotation("3e2", 2));
        assertEquals("<2.50e-3", StringUtil.padExponentNotation("<2.5e-3", 2));
        assertEquals("1.567e5", StringUtil.padExponentNotation("1.567e5", 2));
        assertEquals("1.5e5", StringUtil.padExponentNotation("1.5e5", 0));
        assertEquals("1.5e5", StringUtil.padExponentNotation("1.5e5", -1));
        assertEquals("12.5", StringUtil.padExponentNotation("12.5", 2));
    }

    @Test
    public void isExponentNotation_shouldRecogniseCanonicalValuesWithAComparator() {
        assertTrue(StringUtil.isExponentNotation("1.5e5"));
        assertTrue(StringUtil.isExponentNotation("<1.5E-3"));
        assertFalse(StringUtil.isExponentNotation("1.5×10⁵"));
        assertFalse(StringUtil.isExponentNotation("150000"));
        assertFalse(StringUtil.isExponentNotation(null));
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
