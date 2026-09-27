package org.openelisglobal.result.action.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

/**
 * OGC-1266: a Results row for an environmental or vector order, which has no
 * patient, read "DEV…238 , , pH".
 */
public class ResultsLoadUtilityPatientInfoTest extends BaseWebContextSensitiveTest {

    @Test
    public void patientInfo_joinsWhatThePatientHas() {
        assertEquals("NID-1, F, 05/08/1986", ResultsLoadUtility.patientInfo("NID-1", "F", "05/08/1986"));
        assertEquals("F, 05/08/1986", ResultsLoadUtility.patientInfo("", "F", "05/08/1986"));
    }

    @Test
    public void patientInfo_isEmptyWithoutAPatient() {
        assertEquals("", ResultsLoadUtility.patientInfo("", null, ""));
    }
}
