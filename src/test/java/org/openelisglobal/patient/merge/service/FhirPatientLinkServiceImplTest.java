package org.openelisglobal.patient.merge.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.LinkType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.patient.dao.PatientDAO;

/**
 * Unit tests for FhirPatientLinkServiceImpl. Tests FHIR Patient.link updates
 * during patient merge operations (FR-016, FR-017).
 */
@RunWith(MockitoJUnitRunner.class)
public class FhirPatientLinkServiceImplTest {

    @Mock
    private FhirPersistanceService fhirPersistanceService;

    @Mock
    private PatientDAO patientDAO;

    @InjectMocks
    private FhirPatientLinkServiceImpl fhirPatientLinkService;

    private Patient primaryFhirPatient;
    private Patient mergedFhirPatient;
    private org.openelisglobal.patient.valueholder.Patient primaryPatient;
    private org.openelisglobal.patient.valueholder.Patient mergedPatient;

    @Before
    public void setUp() {
        // Setup FHIR Patient resources
        primaryFhirPatient = new Patient();
        primaryFhirPatient.setId("primary-fhir-uuid");
        primaryFhirPatient.setActive(true);

        mergedFhirPatient = new Patient();
        mergedFhirPatient.setId("merged-fhir-uuid");
        mergedFhirPatient.setActive(true);

        // Setup OpenELIS Patient valueholders
        primaryPatient = new org.openelisglobal.patient.valueholder.Patient();
        primaryPatient.setId("1");
        primaryPatient.setFhirUuid(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));

        mergedPatient = new org.openelisglobal.patient.valueholder.Patient();
        mergedPatient.setId("2");
        mergedPatient.setFhirUuid(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"));
    }

    /**
     * Test: FR-016 - Primary patient should have link with type "replaces"
     * pointing to merged patient.
     */
    @Test
    public void testUpdatePatientLinks_PrimaryPatientLink_ShouldHaveReplacesType()
            throws FhirLocalPersistingException {
        // Arrange
        when(fhirPersistanceService.getPatientByUuid("primary-fhir-uuid"))
                .thenReturn(Optional.of(primaryFhirPatient));
        when(fhirPersistanceService.getPatientByUuid("merged-fhir-uuid"))
                .thenReturn(Optional.of(mergedFhirPatient));

        // Act
        fhirPatientLinkService.updatePatientLinks("1", "2", "primary-fhir-uuid", "merged-fhir-uuid");

        // Assert
        ArgumentCaptor<Patient> primaryCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(fhirPersistanceService, times(2)).updateFhirResourceInFhirStore(primaryCaptor.capture());

        // First call is for primary patient
        Patient updatedPrimary = primaryCaptor.getAllValues().get(0);
        assertTrue("Primary patient should have at least one link", updatedPrimary.hasLink());
        assertTrue("Primary patient should have link with type REPLACES",
                updatedPrimary.getLink().stream().anyMatch(link -> link.getType() == LinkType.REPLACES));
        assertTrue("Primary patient link should reference merged patient",
                updatedPrimary.getLink().stream()
                        .anyMatch(link -> link.getOther().getReference().contains("merged-fhir-uuid")));
    }

    /**
     * Test: FR-017 - Merged patient should have link with type "replaced-by"
     * pointing to primary patient AND active flag set to false.
     */
    @Test
    public void testUpdatePatientLinks_MergedPatientLink_ShouldHaveReplacedByTypeAndInactive()
            throws FhirLocalPersistingException {
        // Arrange
        when(fhirPersistanceService.getPatientByUuid("primary-fhir-uuid"))
                .thenReturn(Optional.of(primaryFhirPatient));
        when(fhirPersistanceService.getPatientByUuid("merged-fhir-uuid"))
                .thenReturn(Optional.of(mergedFhirPatient));

        // Act
        fhirPatientLinkService.updatePatientLinks("1", "2", "primary-fhir-uuid", "merged-fhir-uuid");

        // Assert
        ArgumentCaptor<Patient> mergedCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(fhirPersistanceService, times(2)).updateFhirResourceInFhirStore(mergedCaptor.capture());

        // Second call is for merged patient
        Patient updatedMerged = mergedCaptor.getAllValues().get(1);
        assertTrue("Merged patient should have at least one link", updatedMerged.hasLink());
        assertTrue("Merged patient should have link with type REPLACEDBY",
                updatedMerged.getLink().stream().anyMatch(link -> link.getType() == LinkType.REPLACEDBY));
        assertTrue("Merged patient link should reference primary patient",
                updatedMerged.getLink().stream()
                        .anyMatch(link -> link.getOther().getReference().contains("primary-fhir-uuid")));
        assertFalse("Merged patient should be marked as inactive (active=false)", updatedMerged.getActive());
    }

    /**
     * Test: Should throw exception if primary patient FHIR resource not found.
     */
    @Test(expected = FhirLocalPersistingException.class)
    public void testUpdatePatientLinks_PrimaryPatientNotFound_ShouldThrowException()
            throws FhirLocalPersistingException {
        // Arrange
        when(fhirPersistanceService.getPatientByUuid("primary-fhir-uuid")).thenReturn(Optional.empty());

        // Act
        fhirPatientLinkService.updatePatientLinks("1", "2", "primary-fhir-uuid", "merged-fhir-uuid");

        // Assert: Exception should be thrown
    }

    /**
     * Test: Should throw exception if merged patient FHIR resource not found.
     */
    @Test(expected = FhirLocalPersistingException.class)
    public void testUpdatePatientLinks_MergedPatientNotFound_ShouldThrowException() throws FhirLocalPersistingException {
        // Arrange
        when(fhirPersistanceService.getPatientByUuid("primary-fhir-uuid"))
                .thenReturn(Optional.of(primaryFhirPatient));
        when(fhirPersistanceService.getPatientByUuid("merged-fhir-uuid")).thenReturn(Optional.empty());

        // Act
        fhirPatientLinkService.updatePatientLinks("1", "2", "primary-fhir-uuid", "merged-fhir-uuid");

        // Assert: Exception should be thrown
    }

    /**
     * Test: Should not persist any updates if FHIR resource retrieval fails.
     */
    @Test
    public void testUpdatePatientLinks_ResourceRetrievalFails_ShouldNotPersist() throws FhirLocalPersistingException {
        // Arrange
        when(fhirPersistanceService.getPatientByUuid("primary-fhir-uuid")).thenReturn(Optional.empty());

        // Act
        try {
            fhirPatientLinkService.updatePatientLinks("1", "2", "primary-fhir-uuid", "merged-fhir-uuid");
        } catch (FhirLocalPersistingException e) {
            // Expected exception
        }

        // Assert
        verify(fhirPersistanceService, never()).updateFhirResourceInFhirStore(any(Patient.class));
    }

    /**
     * Test: hasFhirResource() should return true when patient has FHIR UUID.
     */
    @Test
    public void testHasFhirResource_PatientWithFhirUuid_ShouldReturnTrue() {
        // Arrange
        when(patientDAO.getData("1")).thenReturn(primaryPatient);

        // Act
        boolean result = fhirPatientLinkService.hasFhirResource("1");

        // Assert
        assertTrue("Should return true when patient has FHIR UUID", result);
    }

    /**
     * Test: hasFhirResource() should return false when patient has no FHIR UUID.
     */
    @Test
    public void testHasFhirResource_PatientWithoutFhirUuid_ShouldReturnFalse() {
        // Arrange
        org.openelisglobal.patient.valueholder.Patient patientWithoutFhir = new org.openelisglobal.patient.valueholder.Patient();
        patientWithoutFhir.setId("3");
        patientWithoutFhir.setFhirUuid(null);
        when(patientDAO.getData("3")).thenReturn(patientWithoutFhir);

        // Act
        boolean result = fhirPatientLinkService.hasFhirResource("3");

        // Assert
        assertFalse("Should return false when patient has no FHIR UUID", result);
    }

    /**
     * Test: hasFhirResource() should return false when patient not found.
     */
    @Test
    public void testHasFhirResource_PatientNotFound_ShouldReturnFalse() {
        // Arrange
        when(patientDAO.getData(anyString())).thenReturn(null);

        // Act
        boolean result = fhirPatientLinkService.hasFhirResource("999");

        // Assert
        assertFalse("Should return false when patient not found", result);
    }

    /**
     * Test: Bidirectional links - both patients should reference each other.
     */
    @Test
    public void testUpdatePatientLinks_BidirectionalLinks_BothPatientsShouldReferenceEachOther()
            throws FhirLocalPersistingException {
        // Arrange
        when(fhirPersistanceService.getPatientByUuid("primary-fhir-uuid"))
                .thenReturn(Optional.of(primaryFhirPatient));
        when(fhirPersistanceService.getPatientByUuid("merged-fhir-uuid"))
                .thenReturn(Optional.of(mergedFhirPatient));

        // Act
        fhirPatientLinkService.updatePatientLinks("1", "2", "primary-fhir-uuid", "merged-fhir-uuid");

        // Assert
        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(fhirPersistanceService, times(2)).updateFhirResourceInFhirStore(captor.capture());

        Patient updatedPrimary = captor.getAllValues().get(0);
        Patient updatedMerged = captor.getAllValues().get(1);

        // Verify bidirectional links
        String primaryLinkRef = updatedPrimary.getLinkFirstRep().getOther().getReference();
        String mergedLinkRef = updatedMerged.getLinkFirstRep().getOther().getReference();

        assertTrue("Primary patient should reference merged patient", primaryLinkRef.contains("merged-fhir-uuid"));
        assertTrue("Merged patient should reference primary patient", mergedLinkRef.contains("primary-fhir-uuid"));
    }
}
