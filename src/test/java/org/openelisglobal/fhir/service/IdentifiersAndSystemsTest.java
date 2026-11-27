package org.openelisglobal.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.dataexchange.fhir.FhirConfig;

/**
 * OGC-1401 (IG Known Issues 5 and 28): an identifier is sent only when it has a
 * value, and the patient identifiers read back from a FHIR server use the
 * configured identifier base, not a hard-coded one.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class IdentifiersAndSystemsTest {

    private static final String SITE_SYSTEM = "https://lab.example.org/fhir";

    @Mock
    private FhirConfig fhirConfig;

    @InjectMocks
    private FhirCommonTransformServiceImpl common;

    @InjectMocks
    private PatientTransformServiceImpl patientTransform;

    @Before
    public void setUp() {
        when(fhirConfig.getOeFhirSystem()).thenReturn(SITE_SYSTEM);
    }

    @Test
    public void anIdentifierWithoutAValueIsNotCreated() {
        assertNull(common.createIdentifier(SITE_SYSTEM + "/provider_uuid", ""));
        assertNull(common.createIdentifier(SITE_SYSTEM + "/provider_uuid", " "));
        assertNull(common.createIdentifier(SITE_SYSTEM + "/provider_uuid", null));

        Practitioner practitioner = new Practitioner();
        practitioner.addIdentifier(common.createIdentifier(SITE_SYSTEM + "/provider_uuid", ""));
        assertEquals(0, practitioner.getIdentifier().size());
    }

    @Test
    public void anIdentifierWithAValueKeepsItsSystemAndValue() {
        Identifier identifier = common.createIdentifier(SITE_SYSTEM + "/provider_uuid", "4f6a");

        assertEquals(SITE_SYSTEM + "/provider_uuid", identifier.getSystem());
        assertEquals("4f6a", identifier.getValue());
    }

    @Test
    public void patientSearchReadsIdentifiersUnderTheConfiguredBase() {
        org.hl7.fhir.r4.model.Patient fhirPatient = new org.hl7.fhir.r4.model.Patient();
        fhirPatient.addIdentifier().setSystem(SITE_SYSTEM + "/pat_nationalId").setValue("NID-1401");
        fhirPatient.addIdentifier().setSystem(SITE_SYSTEM + "/pat_guid").setValue("GUID-1401");
        fhirPatient.addIdentifier().setSystem(SITE_SYSTEM + "/pat_uuid").setValue("UUID-1401");
        fhirPatient.addIdentifier().setSystem("http://openelis-global.org/pat_nationalId").setValue("DEFAULT-BASE");

        PatientSearchResults results = patientTransform.transformToOpenElisPatientSearchResults(fhirPatient);

        assertEquals("NID-1401", results.getNationalId());
        assertEquals("GUID-1401", results.getExternalId());
        assertEquals("UUID-1401", results.getGUID());
    }
}
