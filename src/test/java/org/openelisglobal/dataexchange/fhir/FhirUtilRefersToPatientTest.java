package org.openelisglobal.dataexchange.fhir;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hl7.fhir.r4.model.Reference;
import org.junit.Test;

/**
 * OGC-1399: the subject of an environmental or vector ServiceRequest is a
 * Location or a contained Group; readers that look up the order's patient must
 * not read it as a Patient.
 */
public class FhirUtilRefersToPatientTest {

    @Test
    public void aPatientReferenceRefersToAPatient() {
        assertTrue(FhirUtil.refersToPatient(new Reference("Patient/7a8b9c0d-1399-4e5f-a6b7-c8d9e0f1a2b3")));
    }

    @Test
    public void aLocationOrAContainedGroupDoesNot() {
        assertFalse(FhirUtil.refersToPatient(new Reference("Location/20e61b21-1399-42cc-b708-1a1194888781")));
        assertFalse(FhirUtil.refersToPatient(new Reference("#subject")));
    }

    @Test
    public void anEmptyOrMissingReferenceDoesNot() {
        assertFalse(FhirUtil.refersToPatient(new Reference()));
        assertFalse(FhirUtil.refersToPatient(new Reference("Patient/")));
        assertFalse(FhirUtil.refersToPatient(null));
    }
}
