package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
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
    public void writeMethods_whenNoStore_returnEmptyTransactionResponseWithoutException()
            throws FhirLocalPersistingException {
        Patient patient = new Patient();
        patient.setId("pat-1");

        Bundle b1 = service.createFhirResourceInFhirStore(patient);
        assertNotNull(b1);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b1.getType());

        Bundle b2 = service.updateFhirResourceInFhirStore(patient);
        assertNotNull(b2);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b2.getType());

        Bundle b3 = service.createFhirResourcesInFhirStore(Map.of("pat-1", patient));
        assertNotNull(b3);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b3.getType());

        Bundle b4 = service.updateFhirResourcesInFhirStore(Map.of("pat-1", patient));
        assertNotNull(b4);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b4.getType());

        Bundle b5 = service.createUpdateFhirResourcesInFhirStore(Map.of("pat-1", patient), Collections.emptyMap());
        assertNotNull(b5);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b5.getType());

        Bundle b6 = service.createUpdateFhirResourcesInFhirStore(new FhirOperations());
        assertNotNull(b6);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b6.getType());

        Bundle b7 = service.createUpdateFhirResourcesInFhirStore(Collections.singletonList(new FhirOperations()));
        assertNotNull(b7);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b7.getType());
    }

    @Test
    public void writeMethods_withNullOrEmptyInput_returnEmptyTransactionResponse() throws FhirLocalPersistingException {
        Bundle b1 = service.createFhirResourceInFhirStore(null);
        assertNotNull(b1);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b1.getType());

        Bundle b2 = service.updateFhirResourceInFhirStore(null);
        assertNotNull(b2);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b2.getType());

        Bundle b3 = service.createFhirResourcesInFhirStore(null);
        assertNotNull(b3);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b3.getType());

        Bundle b4 = service.createFhirResourcesInFhirStore(Collections.emptyMap());
        assertNotNull(b4);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b4.getType());

        Bundle b5 = service.updateFhirResourcesInFhirStore(null);
        assertNotNull(b5);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b5.getType());

        Bundle b6 = service.updateFhirResourcesInFhirStore(Collections.emptyMap());
        assertNotNull(b6);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b6.getType());

        Bundle b7 = service.createUpdateFhirResourcesInFhirStore((Map<String, Resource>) null,
                (Map<String, Resource>) null);
        assertNotNull(b7);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b7.getType());

        Bundle b8 = service.createUpdateFhirResourcesInFhirStore((FhirOperations) null);
        assertNotNull(b8);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b8.getType());

        Bundle b9 = service.createUpdateFhirResourcesInFhirStore((List<FhirOperations>) null);
        assertNotNull(b9);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b9.getType());

        Bundle b10 = service.createUpdateFhirResourcesInFhirStore(Collections.emptyList());
        assertNotNull(b10);
        assertEquals(BundleType.TRANSACTIONRESPONSE, b10.getType());
    }

    @Test
    public void readMethods_whenNoStore_returnEmptyWithoutNpeOrErrorLog() {
        List<ServiceRequest> requests = service.getAllServiceRequestByAccessionNumber("12345");
        assertNotNull(requests);
        assertTrue(requests.isEmpty());

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

    @Test
    public void readMethods_withNullOrBlankInput_returnEmptyWithoutNpe() {
        assertTrue(service.getAllServiceRequestByAccessionNumber(null).isEmpty());
        assertTrue(service.getAllServiceRequestByAccessionNumber("  ").isEmpty());

        assertFalse(service.getFhirOrganizationByName(null).isPresent());
        assertFalse(service.getFhirOrganizationByName("").isPresent());

        assertFalse(service.getFhirOrganizationByUuid(null).isPresent());
        assertFalse(service.getFhirOrganizationByUuid("   ").isPresent());

        assertFalse(service.getPatientByUuid(null).isPresent());
        assertFalse(service.getPatientByUuid("").isPresent());

        assertFalse(service.getServiceRequestByAnalysisUuid(null).isPresent());
        assertFalse(service.getServiceRequestByAnalysisUuid(" ").isPresent());

        assertFalse(service.getSpecimenBySampleItemUuid(null).isPresent());
        assertFalse(service.getSpecimenBySampleItemUuid("").isPresent());

        assertFalse(service.getSpecimenByUuid(null).isPresent());
        assertFalse(service.getSpecimenByUuid("  ").isPresent());

        assertFalse(service.getServiceRequestBySpecimenUuid(null).isPresent());
        assertFalse(service.getServiceRequestBySpecimenUuid("").isPresent());

        assertFalse(service.getSupplyDeliveryByUuid(null).isPresent());
        assertFalse(service.getSupplyDeliveryByUuid(" ").isPresent());

        assertFalse(service.getDiagnosticReportByAnalysisUuid(null).isPresent());
        assertFalse(service.getDiagnosticReportByAnalysisUuid("").isPresent());

        assertFalse(service.getTaskBasedOnServiceRequest(null).isPresent());
        assertFalse(service.getTaskBasedOnServiceRequest("   ").isPresent());

        assertFalse(service.getServiceRequestByReferingId(null).isPresent());
        assertFalse(service.getServiceRequestByReferingId("").isPresent());

        assertFalse(service.getTaskBasedOnTask(null).isPresent());
        assertFalse(service.getTaskBasedOnTask(" ").isPresent());
    }
}
