package org.openelisglobal.terminology;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CodingTest {

    @Test
    public void aTerminologySourceAndItsCanonicalUriAreTheSameSystem() {
        assertTrue(new Coding("LOINC", "94500-6").sameAs(new Coding("http://loinc.org", "94500-6")));
        assertTrue(new Coding("snomed", "10828004").sameAs(new Coding("http://snomed.info/sct", "10828004")));
    }

    @Test
    public void aDistroSystemMatchesItselfOnly() {
        assertTrue(new Coding("CPHL", "HIVVL").sameAs(new Coding("CPHL", " hivvl ")));
        assertFalse(new Coding("CPHL", "HIVVL").sameAs(new Coding("OTHER", "HIVVL")));
        assertFalse(new Coding("CPHL", "HIVVL").sameAs(new Coding("http://loinc.org", "HIVVL")));
    }

    @Test
    public void aBlankCodeOrSystemNamesNothing() {
        assertFalse(new Coding("CPHL", " ").sameAs(new Coding("CPHL", " ")));
        assertFalse(new Coding(null, "HIVVL").sameAs(new Coding(null, "HIVVL")));
    }
}
