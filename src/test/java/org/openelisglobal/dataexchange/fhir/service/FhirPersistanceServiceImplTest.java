package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;

public class FhirPersistanceServiceImplTest {

    private FhirPersistanceServiceImpl service;

    @Before
    public void setUp() {
        service = new FhirPersistanceServiceImpl();
    }

    @Test
    public void writeMethods_whenLocalFhirClientIsNull_throwFhirLocalPersistingException() {
        Patient patient = new Patient();
        patient.setId("pat-1");

        FhirLocalPersistingException ex = assertThrows(FhirLocalPersistingException.class,
                () -> service.createFhirResourceInFhirStore(patient));
        assertTrue(ex.getMessage().contains("org.openelisglobal.fhirstore.uri"));

        assertThrows(FhirLocalPersistingException.class,
                () -> service.updateFhirResourceInFhirStore(patient));
        assertThrows(FhirLocalPersistingException.class,
                () -> service.createFhirResourcesInFhirStore(Map.of("pat-1", patient)));
        assertThrows(FhirLocalPersistingException.class,
                () -> service.updateFhirResourcesInFhirStore(Map.of("pat-1", patient)));
        assertThrows(FhirLocalPersistingException.class,
                () -> service.createUpdateFhirResourcesInFhirStore(
                        Map.of("pat-1", patient), Collections.emptyMap()));
        assertThrows(FhirLocalPersistingException.class,
                () -> service.createUpdateFhirResourcesInFhirStore(new FhirOperations()));
        assertThrows(FhirLocalPersistingException.class,
                () -> service.createUpdateFhirResourcesInFhirStore(
                        Collections.singletonList(new FhirOperations())));
    }

    @Test
    public void readMethods_whenLocalFhirClientIsNull_returnEmptyWithoutNpe() {
        assertTrue(service.getAllServiceRequestByAccessionNumber("12345").isEmpty());
        assertFalse(service.getFhirOrganizationByName("TestOrg").isPresent());
        assertFalse(service.getFhirOrganizationByUuid("uuid-1").isPresent());
        assertFalse(service.getPatientByUuid("uuid-1").isPresent());
        assertFalse(service.getServiceRequestByAnalysisUuid("uuid-1").isPresent());
        assertFalse(service.getSpecimenBySampleItemUuid("uuid-1").isPresent());
        assertFalse(service.getSpecimenByUuid("uuid-1").isPresent());
        assertFalse(service.getServiceRequestBySpecimenUuid("uuid-1").isPresent());
        assertFalse(service.getSupplyDeliveryByUuid("uuid-1").isPresent());
        assertFalse(service.getDiagnosticReportByAnalysisUuid("uuid-1").isPresent());
        assertFalse(service.getTaskBasedOnServiceRequest("ref-1").isPresent());
        assertFalse(service.getServiceRequestByReferingId("ref-1").isPresent());
        assertFalse(service.getTaskBasedOnTask("task-1").isPresent());
    }
}
