package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class IntegerUtilTest {

    @Test
    public void testToStringBase27_Zero() {
        assertEquals("0", IntegerUtil.toStringBase27(0));
    }

    @Test
    public void testToStringBase27_SingleDigits() {
        assertEquals("1", IntegerUtil.toStringBase27(1));
        assertEquals("9", IntegerUtil.toStringBase27(9));
    }

    @Test
    public void testToStringBase27_LetterMappings() {
        assertEquals("C", IntegerUtil.toStringBase27(10));
        assertEquals("Y", IntegerUtil.toStringBase27(26));
    }

    @Test
    public void testToStringBase27_TwoDigits() {
        assertEquals("10", IntegerUtil.toStringBase27(27));
        assertEquals("11", IntegerUtil.toStringBase27(28));
    }

    @Test
    public void testToStringBase27_Negative() {
        assertEquals("-1", IntegerUtil.toStringBase27(-1));
        assertEquals("-C", IntegerUtil.toStringBase27(-10));
    }

    @Test
    public void testToStringBase27_Boundaries() {
        String maxStr = IntegerUtil.toStringBase27(Integer.MAX_VALUE);
        String minStr = IntegerUtil.toStringBase27(Integer.MIN_VALUE);
        assertEquals(7, maxStr.length());
        assertEquals(8, minStr.length()); // Includes '-' sign
    }

    @Test
    public void testParseIntBase27_Simple() {
        assertEquals(0, IntegerUtil.parseIntBase27("0"));
        assertEquals(1, IntegerUtil.parseIntBase27("1"));
        assertEquals(10, IntegerUtil.parseIntBase27("C"));
    }

    @Test
    public void testParseIntBase27_Complex() {
        assertEquals(27, IntegerUtil.parseIntBase27("10"));
        assertEquals(28, IntegerUtil.parseIntBase27("11"));
    }

    @Test
    public void testParseIntBase27_Negative() {
        assertEquals(-1, IntegerUtil.parseIntBase27("-1"));
        assertEquals(-27, IntegerUtil.parseIntBase27("-10"));
    }

    @Test
    public void testParseIntBase27_ExplicitPlusSign() {
        assertEquals(5, IntegerUtil.parseIntBase27("+5"));
    }

    @Test
    public void testRoundTrip() {
        int[] testValues = { 0, 1, 9, 10, 26, 27, 100, 1000, 1234567, -1, -100, -1234567, Integer.MAX_VALUE,
                Integer.MIN_VALUE };

        for (int val : testValues) {
            String encoded = IntegerUtil.toStringBase27(val);
            int decoded = IntegerUtil.parseIntBase27(encoded);
            assertEquals("Round trip failed for value: " + val, val, decoded);
        }
    }

    @Test
    public void testParseIntBase27_Null() {
        assertThrows(NumberFormatException.class, () -> IntegerUtil.parseIntBase27(null));
    }

    @Test
    public void testParseIntBase27_Empty() {
        assertThrows(NumberFormatException.class, () -> IntegerUtil.parseIntBase27(""));
    }

    @Test
    public void testParseIntBase27_SignOnly() {
        assertThrows(NumberFormatException.class, () -> IntegerUtil.parseIntBase27("-"));
        assertThrows(NumberFormatException.class, () -> IntegerUtil.parseIntBase27("+"));
    }

    @Test
    public void testParseIntBase27_InvalidCharacters() {
        assertThrows(NumberFormatException.class, () -> IntegerUtil.parseIntBase27("A"));
        assertThrows(NumberFormatException.class, () -> IntegerUtil.parseIntBase27("1B"));
        assertThrows(NumberFormatException.class, () -> IntegerUtil.parseIntBase27("1!"));
    }

    @Test
    public void testParseIntBase27_Overflow() {
        assertThrows(NumberFormatException.class, () -> IntegerUtil.parseIntBase27("YYYYYYYY"));
    }

    @Test
    public void testConstructor() {
        new IntegerUtil();
    }
}