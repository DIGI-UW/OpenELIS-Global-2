package org.openelisglobal.testcalculated.action.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * A stored result is written into the calculation's script, so only a number
 * may stand in for a parameter; anything else leaves the parameter missing.
 */
public class CalculationOperandValueTest {

    @Test
    public void aNumberIsAUsableParameter() {
        assertTrue(TestCalculatedUtil.isUsableOperandValue("7.5"));
        assertTrue(TestCalculatedUtil.isUsableOperandValue("-12"));
        assertTrue(TestCalculatedUtil.isUsableOperandValue("1.2E3"));
    }

    @Test
    public void anythingElseLeavesTheParameterMissing() {
        assertFalse(TestCalculatedUtil.isUsableOperandValue(null));
        assertFalse(TestCalculatedUtil.isUsableOperandValue(""));
        assertFalse(TestCalculatedUtil.isUsableOperandValue("<5"));
        assertFalse(TestCalculatedUtil.isUsableOperandValue("1; java.lang.Runtime.getRuntime().exec('id')"));
        assertFalse(TestCalculatedUtil.isUsableOperandValue("1E+2000000000"));
    }
}
