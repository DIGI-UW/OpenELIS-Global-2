package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class MicroBreakpointClinicalContextTest {

    @Test
    public void aRunMayBeForMeningitisEndocarditisOrNeither() {
        assertEquals("MENINGITIS", MicroBreakpointClinicalContext.validate("MENINGITIS"));
        assertEquals("ENDOCARDITIS", MicroBreakpointClinicalContext.validate("ENDOCARDITIS"));
        assertEquals("NON_MENINGITIS", MicroBreakpointClinicalContext.validate("NON_MENINGITIS"));
    }

    @Test
    public void noIndicationIsNotStated() {
        assertNull(MicroBreakpointClinicalContext.validate(null));
        assertNull(MicroBreakpointClinicalContext.validate(" "));
    }

    @Test
    public void anotherIndicationIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> MicroBreakpointClinicalContext.validate("PNEUMONIA"));
    }
}
