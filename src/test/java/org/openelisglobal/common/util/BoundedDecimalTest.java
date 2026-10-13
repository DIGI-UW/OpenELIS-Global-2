package org.openelisglobal.common.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import org.junit.Test;

public class BoundedDecimalTest {

    @Test
    public void acceptsTheNumbersAnalyzersReport() {
        for (String value : new String[] { "7.5", "-0.003", "+12", ".5", "12.", "1.2E3", "4e-2",
                "123456789012345678" }) {
            assertTrue(value, BoundedDecimal.isBoundedDecimal(value));
        }
    }

    @Test
    public void rejectsTextComparisonsAndHugeExponents() {
        for (String value : new String[] { "<5", ">1000", "12,5", "NaN", "Infinity", "1E+2000000000", "1e1000", "",
                " 7.5", "7.5;", "1234567890123456789", "0x1F" }) {
            assertFalse(value, BoundedDecimal.isBoundedDecimal(value));
        }
        assertFalse(BoundedDecimal.isBoundedDecimal(null));
    }

    @Test
    public void judgesAParsedNumberWithoutExpandingIt() {
        assertTrue(BoundedDecimal.isBounded(new BigDecimal("1E+999")));
        assertTrue(BoundedDecimal.isBounded(new BigDecimal("0.000125")));
        assertFalse(BoundedDecimal.isBounded(new BigDecimal("1E+2000000000")));
        assertFalse(BoundedDecimal.isBounded(new BigDecimal("1E-2000000000")));
        assertFalse(BoundedDecimal.isBounded(null));
    }
}
