package org.openelisglobal.fhir.facade.provider;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.patient.service.PatientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for PatientResourceProvider.
 */
@RunWith(MockitoJUnitRunner.class)
public class PatientResourceProviderTest {

    @Mock
    private PatientService patientService;

    @Mock
    private FhirTransformService fhirTransformService;

    @InjectMocks
    private PatientResourceProvider patientResourceProvider;

    private org.openelisglobal.patient.valueholder.Patient mockOePatient;
    private Patient mockFhirPatient;
    private IParser jsonParser;

    @Before
    public void setUp() {
        // Setup mock OpenELIS patient
        mockOePatient = new org.openelisglobal.patient.valueholder.Patient();
        mockOePatient.setId("123");

        // Setup mock FHIR patient
        mockFhirPatient = new Patient();
        mockFhirPatient.setId("patient-123");
        mockFhirPatient.addName().setFamily("Smith").addGiven("John");

        // Setup FHIR JSON parser
        jsonParser = FhirContext.forR4().newJsonParser();
    }

    @Test
    public void testRead_PatientFound_ReturnsPatient() throws FhirTransformationException {
        // Arrange
        when(patientService.get("123")).thenReturn(mockOePatient);
        when(fhirTransformService.transformToFhirPatient("123")).thenReturn(mockFhirPatient);

        // Act
        ResponseEntity<String> response = patientResourceProvider.read("123");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Patient"));
        assertTrue(response.getBody().contains("Smith"));

        verify(patientService).get("123");
        verify(fhirTransformService).transformToFhirPatient("123");
    }

    @Test
    public void testRead_PatientNotFound_Returns404() {
        // Arrange
        when(patientService.get("999")).thenReturn(null);
        when(patientService.getPatientForGuid("999")).thenReturn(null);

        // Act
        ResponseEntity<String> response = patientResourceProvider.read("999");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().contains("OperationOutcome"));
        assertTrue(response.getBody().contains("not found"));
    }

    @Test
    public void testRead_EmptyId_Returns400() {
        // Act
        ResponseEntity<String> response = patientResourceProvider.read("");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("OperationOutcome"));
    }

    @Test
    public void testRead_NullId_Returns400() {
        // Act
        ResponseEntity<String> response = patientResourceProvider.read(null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testRead_PatientFoundByGuid_ReturnsPatient() throws FhirTransformationException {
        // Arrange
        when(patientService.get("abc-uuid")).thenReturn(null);
        when(patientService.getPatientForGuid("abc-uuid")).thenReturn(mockOePatient);
        when(fhirTransformService.transformToFhirPatient("123")).thenReturn(mockFhirPatient);

        // Act
        ResponseEntity<String> response = patientResourceProvider.read("abc-uuid");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(patientService).getPatientForGuid("abc-uuid");
    }

    @Test
    public void testSearch_NoParams_ReturnsAllPatients() throws FhirTransformationException {
        // Arrange
        List<org.openelisglobal.patient.valueholder.Patient> patients = Arrays.asList(mockOePatient);
        when(patientService.getPageOfPatients(1)).thenReturn(patients);
        when(fhirTransformService.transformToFhirPatient("123")).thenReturn(mockFhirPatient);

        // Act
        ResponseEntity<String> response = patientResourceProvider.search(null, null, null, null, 20);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Bundle"));
        assertTrue(response.getBody().contains("searchset"));
    }

    @Test
    public void testSearch_ByIdentifier_ReturnsMatchingPatients() throws FhirTransformationException {
        // Arrange
        List<org.openelisglobal.patient.valueholder.Patient> patients = Arrays.asList(mockOePatient);
        when(patientService.getPatientsByNationalId("ID123")).thenReturn(patients);
        when(patientService.getPatientByExternalId("ID123")).thenReturn(null);
        when(patientService.getPatientForGuid("ID123")).thenReturn(null);
        when(fhirTransformService.transformToFhirPatient("123")).thenReturn(mockFhirPatient);

        // Act
        ResponseEntity<String> response = patientResourceProvider.search("ID123", null, null, null, 20);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Bundle"));
        verify(patientService).getPatientsByNationalId("ID123");
    }

    @Test
    public void testSearch_ByFamilyName_ReturnsMatchingPatients() throws FhirTransformationException {
        // Arrange
        List<org.openelisglobal.patient.valueholder.Patient> allPatients = Arrays.asList(mockOePatient);
        when(patientService.getPageOfPatients(1)).thenReturn(allPatients);
        when(patientService.getLastName(mockOePatient)).thenReturn("Smith");
        when(patientService.getFirstName(mockOePatient)).thenReturn("John");
        when(fhirTransformService.transformToFhirPatient("123")).thenReturn(mockFhirPatient);

        // Act
        ResponseEntity<String> response = patientResourceProvider.search(null, "Smith", null, null, 20);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Bundle"));
    }

    @Test
    public void testSearch_ByFamilyName_NoMatch_ReturnsEmptyBundle() {
        // Arrange
        List<org.openelisglobal.patient.valueholder.Patient> allPatients = Arrays.asList(mockOePatient);
        when(patientService.getPageOfPatients(1)).thenReturn(allPatients);
        when(patientService.getLastName(mockOePatient)).thenReturn("Jones");
        when(patientService.getFirstName(mockOePatient)).thenReturn("Jane");

        // Act
        ResponseEntity<String> response = patientResourceProvider.search(null, "Smith", null, null, 20);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Parse and verify empty bundle
        Bundle bundle = jsonParser.parseResource(Bundle.class, response.getBody());
        assertEquals(0, bundle.getTotal());
    }

    @Test
    public void testSearch_EmptyDatabase_ReturnsEmptyBundle() {
        // Arrange
        when(patientService.getPageOfPatients(1)).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<String> response = patientResourceProvider.search(null, null, null, null, 20);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Bundle bundle = jsonParser.parseResource(Bundle.class, response.getBody());
        assertEquals(0, bundle.getTotal());
    }

    @Test
    public void testSearch_WithCount_LimitsResults() throws FhirTransformationException {
        // Arrange
        org.openelisglobal.patient.valueholder.Patient patient1 = new org.openelisglobal.patient.valueholder.Patient();
        patient1.setId("1");
        org.openelisglobal.patient.valueholder.Patient patient2 = new org.openelisglobal.patient.valueholder.Patient();
        patient2.setId("2");
        org.openelisglobal.patient.valueholder.Patient patient3 = new org.openelisglobal.patient.valueholder.Patient();
        patient3.setId("3");

        List<org.openelisglobal.patient.valueholder.Patient> allPatients = Arrays.asList(patient1, patient2, patient3);
        when(patientService.getPageOfPatients(1)).thenReturn(allPatients);

        Patient fhir1 = new Patient();
        fhir1.setId("1");
        Patient fhir2 = new Patient();
        fhir2.setId("2");

        when(fhirTransformService.transformToFhirPatient("1")).thenReturn(fhir1);
        when(fhirTransformService.transformToFhirPatient("2")).thenReturn(fhir2);

        // Act - limit to 2 results
        ResponseEntity<String> response = patientResourceProvider.search(null, null, null, null, 2);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Bundle bundle = jsonParser.parseResource(Bundle.class, response.getBody());
        assertEquals(2, bundle.getTotal());

        // Verify patient3 was never transformed (limit applied)
        verify(fhirTransformService, never()).transformToFhirPatient("3");
    }
}
