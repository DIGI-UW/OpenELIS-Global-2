package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class IntegerUtilTest {
    
    @Test
    public void toStringBase27_and_parseIntBase27_roundTrip_variousValues() {
        int[] values = {0, 1, -1, 27, 12345, -12345, Integer.MAX_VALUE, Integer.MIN_VALUE + 1}; 
        for (int v : values) {
            String s = IntegerUtil.toStringBase27(v);
            int parsed = IntegerUtil.parseIntBase27(s);
            assertEquals("Round trip failed for value: " + v, v, parsed);
        }
    }

    @Test
    public void toStringBase27_negativeStartsWithMinus() {
        String s = IntegerUtil.toStringBase27(-42);
        // negative representation includes '-' char
        org.junit.Assert.assertTrue("Expected negative representation to contain '-'", s.indexOf('-') >= 0);
    }

    @Test
    public void parseIntBase27_invalidInput_throwsNumberFormatException() {
        try {
            IntegerUtil.parseIntBase27("!!invalid!!");
            fail("Expected NumberFormatException for invalid input");
        } catch (NumberFormatException e) {
            // expected
        }

        try {
            IntegerUtil.parseIntBase27(null);
            fail("Expected NumberFormatException for null");
        } catch (NumberFormatException e) {
            // expected
        }
    }
}

