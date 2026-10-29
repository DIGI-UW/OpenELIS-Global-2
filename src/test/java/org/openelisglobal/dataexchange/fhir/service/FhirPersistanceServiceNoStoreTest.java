package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * A light installation leaves {@code org.openelisglobal.fhirstore.uri} empty
 * and runs no FHIR server. Patient, order, result and validation saves still
 * hand their resources to this service from an asynchronous transform, which
 * used to end in a NullPointerException on every save (OGC-1546). Without a
 * store the service answers as an empty store: writes are no-ops and reads
 * find nothing, so the lab workflows run from the OpenELIS database alone.
 */
@RunWith(MockitoJUnitRunner.class)
public class FhirPersistanceServiceNoStoreTest {

    private static final String PATIENT_UUID = "4175d853-e703-4d2c-aa95-9803cb355215";
    private static final String STORE = "https://fhir.openelis.org:8443/fhir/";

    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private FhirUtil fhirUtil;

    private FhirPersistanceServiceImpl service;

    @Before
    public void setUp() {
        service = new FhirPersistanceServiceImpl();
        ReflectionTestUtils.setField(service, "fhirConfig", fhirConfig);
        ReflectionTestUtils.setField(service, "fhirUtil", fhirUtil);
        ReflectionTestUtils.setField(service, "fhirContext", FhirContext.forR4Cached());
    }

    @Test
    public void writesAreNoOpsWithoutAStore() throws FhirLocalPersistingException {
        when(fhirConfig.getLocalFhirStorePath()).thenReturn("");
        service.init();

        Patient patient = new Patient();
        patient.setId(PATIENT_UUID);

        assertEmptyResponse(service.createUpdateFhirResourcesInFhirStore(new FhirOperations()));
        assertEmptyResponse(service.createFhirResourceInFhirStore(patient));
        assertEmptyResponse(service.updateFhirResourceInFhirStore(patient));
        assertEmptyResponse(service.createUpdateFhirResourcesInFhirStore(Map.of(), Map.of(PATIENT_UUID, patient)));
        verify(fhirUtil, never()).getFhirClient(anyString());
    }

    @Test
    public void readsFindNothingWithoutAStore() {
        when(fhirConfig.getLocalFhirStorePath()).thenReturn(" ");
        service.init();

        assertTrue(service.getAllServiceRequestByAccessionNumber("DEV01260000000000001").isEmpty());
        assertFalse(service.getFhirOrganizationByName("Reference Laboratory").isPresent());
        assertFalse(service.getFhirOrganizationByUuid(PATIENT_UUID).isPresent());
        assertFalse(service.getPatientByUuid(PATIENT_UUID).isPresent());
        assertFalse(service.getServiceRequestByAnalysisUuid(PATIENT_UUID).isPresent());
        assertFalse(service.getSpecimenBySampleItemUuid(PATIENT_UUID).isPresent());
        assertFalse(service.getSpecimenByUuid(PATIENT_UUID).isPresent());
        assertFalse(service.getServiceRequestBySpecimenUuid(PATIENT_UUID).isPresent());
        assertFalse(service.getSupplyDeliveryByUuid(PATIENT_UUID).isPresent());
        assertFalse(service.getDiagnosticReportByAnalysisUuid(PATIENT_UUID).isPresent());
        assertFalse(service.getTaskBasedOnServiceRequest("ORDER-1").isPresent());
        assertFalse(service.getServiceRequestByReferingId("ORDER-1").isPresent());
        assertFalse(service.getTaskBasedOnTask("TASK-1").isPresent());
    }

    @Test
    public void aConfiguredStoreStillGetsItsClient() {
        when(fhirConfig.getLocalFhirStorePath()).thenReturn(STORE);
        IGenericClient client = mock(IGenericClient.class);
        when(fhirUtil.getFhirClient(STORE)).thenReturn(client);

        service.init();

        verify(fhirUtil).getFhirClient(STORE);
        assertSame(client, ReflectionTestUtils.getField(service, "localFhirClient"));
    }

    private static void assertEmptyResponse(Bundle bundle) {
        assertEquals(BundleType.TRANSACTIONRESPONSE, bundle.getType());
        assertFalse(bundle.hasEntry());
    }
}
