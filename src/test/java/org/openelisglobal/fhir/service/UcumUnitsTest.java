package org.openelisglobal.fhir.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * OGC-1400 (IG Known Issue 12): only unit names UCUM accepts are sent with the
 * UCUM system. The invalid names are ones this lab's unit list really holds.
 */
public class UcumUnitsTest {

    @Test
    public void ucumUnitsAreValid() {
        for (String unit : new String[] { "g/L", "mg/dL", "mL", "%", "mm/h", "u/L", "pg" }) {
            assertTrue(unit, UcumUnits.isValid(unit));
        }
    }

    @Test
    public void labTypedUnitNamesAreNot() {
        for (String unit : new String[] { "10^3/µl", "Cell/µl", "mille/mm^3", "tubes", "slides", "UI/L", "", " ",
                null }) {
            assertFalse(String.valueOf(unit), UcumUnits.isValid(unit));
        }
    }
}
