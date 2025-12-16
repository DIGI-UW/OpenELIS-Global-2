package org.openelisglobal.patient.merge.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.merge.dao.PatientMergeAuditDAO;
import org.openelisglobal.patient.merge.dto.PatientMergeExecutionResultDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeRequestDTO;
import org.openelisglobal.patient.merge.valueholder.PatientMergeAudit;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.systemuser.dao.SystemUserDAO;

/**
 * Unit tests for PatientMergeServiceImpl.executeMerge() method. Tests merge
 * execution logic.
 *
 * TDD Phase: RED - These tests should FAIL before implementation exists.
 */

// TODO: This would be more useful if it was an integration test with some good
// test data.
@RunWith(MockitoJUnitRunner.class)
public class PatientMergeExecutionTest {

    @Mock
    private PatientDAO patientDAO;

    @Mock
    private PatientMergeAuditDAO patientMergeAuditDAO;

    @Mock
    private SystemUserDAO systemUserDAO;

    @Mock
    private FhirPatientLinkService fhirPatientLinkService;

    @Mock
    private PatientMergeConsolidationService consolidationService;

    @InjectMocks
    private PatientMergeServiceImpl patientMergeService;

    private PatientMergeRequestDTO validRequest;
    private Patient patient1;
    private Patient patient2;
    private Person person1;
    private Person person2;

    @Before
    public void setUp() {
        // Setup valid merge request
        validRequest = new PatientMergeRequestDTO();
        validRequest.setPatient1Id("1");
        validRequest.setPatient2Id("2");
        validRequest.setPrimaryPatientId("1");
        validRequest.setReason("Duplicate patient detected");
        validRequest.setConfirmed(true);

        // Setup persons
        person1 = new Person();
        person1.setId("101");
        person1.setFirstName("John");
        person1.setLastName("Doe");

        person2 = new Person();
        person2.setId("102");
        person2.setFirstName("John");
        person2.setLastName("Doe");

        // Setup valid patients
        patient1 = new Patient();
        patient1.setId("1");
        patient1.setIsMerged(false);
        patient1.setMergedIntoPatientId(null);
        patient1.setPerson(person1);

        patient2 = new Patient();
        patient2.setId("2");
        patient2.setIsMerged(false);
        patient2.setMergedIntoPatientId(null);
        patient2.setPerson(person2);

        // Mock FHIR service to return false (no FHIR resources) by default
        when(fhirPatientLinkService.hasFhirResource(any())).thenReturn(false);

        // Mock consolidation service to return empty result
        when(consolidationService.consolidateClinicalData(any(), any(), any()))
                .thenReturn(new PatientMergeConsolidationService.ConsolidationResult());
        when(consolidationService.mergeDemographics(any(), any())).thenReturn(new java.util.ArrayList<>());
    }

    /**
     * Test: Merge execution should succeed for valid patients.
     * Business Rule: Merge consolidates data and marks merged patient.
     */
    @Test
    public void testExecuteMerge_ValidPatients_ShouldSucceed() {
        // Arrange
        when(patientDAO.getData("1")).thenReturn(patient1);
        when(patientDAO.getData("2")).thenReturn(patient2);
        when(patientMergeAuditDAO.insert(any(PatientMergeAudit.class))).thenReturn(123L);

        // Act
        PatientMergeExecutionResultDTO result = patientMergeService.executeMerge(validRequest, "1");

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Merge should succeed", result.isSuccess());
        assertEquals("Should return correct primary patient ID", "1", result.getPrimaryPatientId());
        assertEquals("Should return correct merged patient ID", "2", result.getMergedPatientId());
        assertNotNull("Should have merge audit ID", result.getMergeAuditId());
    }

    /**
     * Test: Merge should mark merged patient as inactive.
     * Business Rule: Merged patient is flagged with is_merged=true.
     * This enables redirect-on-lookup pattern (interim solution for BLOCKER-001).
     */
    @Test
    public void testExecuteMerge_ShouldMarkMergedPatientAsInactive() {
        // Arrange
        when(patientDAO.getData("1")).thenReturn(patient1);
        when(patientDAO.getData("2")).thenReturn(patient2);
        when(patientMergeAuditDAO.insert(any(PatientMergeAudit.class))).thenReturn(123L);

        // Act
        PatientMergeExecutionResultDTO result = patientMergeService.executeMerge(validRequest, "1");

        // Assert - Verify merge state for redirect-on-lookup
        assertTrue("Merged patient should be marked as merged", Boolean.TRUE.equals(patient2.getIsMerged()));
        assertEquals("Merged patient should reference primary", "1", patient2.getMergedIntoPatientId());
        assertNotNull("Merged patient should have merge date", patient2.getMergeDate());

        // Verify state enables redirect pattern:
        // Application can check: if (patient.getIsMerged()) redirect to
        // patient.getMergedIntoPatientId()
        assertTrue("Redirect condition should be true", Boolean.TRUE.equals(patient2.getIsMerged()));
        assertNotNull("Redirect target should be set", patient2.getMergedIntoPatientId());
    }

    /**
     * Test: Merge should create audit entry.
     * Business Rule: All merges are audited.
     */
    @Test
    public void testExecuteMerge_ShouldCreateAuditEntry() {
        // Arrange
        when(patientDAO.getData("1")).thenReturn(patient1);
        when(patientDAO.getData("2")).thenReturn(patient2);
        when(patientMergeAuditDAO.insert(any(PatientMergeAudit.class))).thenReturn(123L);

        // Act
        PatientMergeExecutionResultDTO result = patientMergeService.executeMerge(validRequest, "1");

        // Assert
        verify(patientMergeAuditDAO, times(1)).insert(any(PatientMergeAudit.class));
        assertTrue("Merge should succeed", result.isSuccess());
    }

    /**
     * Test: Merge should fail if confirmation is false. Business Rule: Merge
     * requires explicit confirmation.
     */
    @Test
    public void testExecuteMerge_NotConfirmed_ShouldFail() {
        // Arrange
        validRequest.setConfirmed(false);

        // Act
        PatientMergeExecutionResultDTO result = patientMergeService.executeMerge(validRequest, "1");

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("Merge should fail without confirmation", result.isSuccess());
        assertTrue("Error message should mention confirmation", result.getMessage().toLowerCase().contains("confirm"));
    }

    /**
     * Test: Merge should fail if patient not found.
     * Business Rule: Both patients must exist.
     */
    @Test
    public void testExecuteMerge_PatientNotFound_ShouldFail() {
        // Arrange
        when(patientDAO.getData("1")).thenReturn(null);
        when(patientDAO.getData("2")).thenReturn(patient2);

        // Act
        PatientMergeExecutionResultDTO result = patientMergeService.executeMerge(validRequest, "1");

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("Merge should fail if patient not found", result.isSuccess());
        assertTrue("Error message should mention not found",
                result.getMessage().toLowerCase().contains("not found"));
    }

    /**
     * Test: Primary patient should remain unchanged.
     * Business Rule: Primary patient demographics are preserved.
     */
    @Test
    public void testExecuteMerge_PrimaryPatientUnchanged() {
        // Arrange
        when(patientDAO.getData("1")).thenReturn(patient1);
        when(patientDAO.getData("2")).thenReturn(patient2);
        when(patientMergeAuditDAO.insert(any(PatientMergeAudit.class))).thenReturn(123L);

        // Act
        PatientMergeExecutionResultDTO result = patientMergeService.executeMerge(validRequest, "1");

        // Assert
        assertFalse("Primary patient should NOT be marked as merged", patient1.getIsMerged());
        assertEquals("Primary patient ID should be unchanged", "1", patient1.getId());
    }
}
