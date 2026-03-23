package org.openelisglobal.patient.merge.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.merge.dao.PatientMergeAuditDAO;
import org.openelisglobal.patient.merge.dto.PatientMergeRequestDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeValidationResultDTO;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;

/**
 * Unit tests for PatientMergeServiceImpl.validateMerge() method. Tests
 * validation logic for merge eligibility checks.
 *
 * TDD Phase: RED - These tests should FAIL before implementation exists.
 */
// TODO: This would be more useful if it was an integration test with some good
// test data.
@RunWith(MockitoJUnitRunner.class)
public class PatientMergeServiceImplTest {

    @Mock
    private PatientDAO patientDAO;

    @Mock
    private PatientMergeAuditDAO patientMergeAuditDAO;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @Mock
    private jakarta.persistence.TypedQuery<Long> mockQuery;

    @Mock
    private SampleItemService sampleItemService;

    @Mock
    private AnalysisService analysisService;

    @Mock
    private IStatusService iStatusService;

    @Mock
    private SampleHumanService sampleHumanService;

    @InjectMocks
    private PatientMergeServiceImpl patientMergeService;

    private PatientMergeRequestDTO validRequest;
    private Patient patient1;
    private Patient patient2;

    @Before
    public void setUp() {
        // Setup valid merge request
        validRequest = new PatientMergeRequestDTO();
        validRequest.setPatient1Id("1");
        validRequest.setPatient2Id("2");
        validRequest.setPrimaryPatientId("1");
        validRequest.setReason("Duplicate patient detected");
        validRequest.setConfirmed(true);

        // Setup valid patients
        patient1 = new Patient();
        patient1.setId("1");
        patient1.setIsMerged(false);
        patient1.setMergedIntoPatientId(null);

        patient2 = new Patient();
        patient2.setId("2");
        patient2.setIsMerged(false);
        patient2.setMergedIntoPatientId(null);

        // Mock EntityManager to return 0 counts for all queries (unit tests don't need
        // real counts)
        // Use lenient() to avoid UnnecessaryStubbingException for tests that don't use
        // all mocks
        lenient().when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        lenient().when(mockQuery.setParameter(anyString(), anyString())).thenReturn(mockQuery);
        lenient().when(mockQuery.setParameter(anyString(), any(Long.class))).thenReturn(mockQuery);
        lenient().when(mockQuery.getSingleResult()).thenReturn(0L);
        lenient().when(mockQuery.getResultList()).thenReturn(java.util.Collections.emptyList());
    }

    /**
     * Test: Validation should fail if same patient ID is used for both patients.
     * Business Rule: Cannot merge a patient with itself.
     */
    @Test
    public void testValidateMerge_SamePatientId_ShouldFail() {
        // Arrange
        validRequest.setPatient1Id("1");
        validRequest.setPatient2Id("1");

        // Act
        PatientMergeValidationResultDTO result = patientMergeService.validateMerge(validRequest);

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("Validation should fail for same patient ID", result.isValid());
        assertTrue("Should have at least one error", !result.getErrors().isEmpty());
        assertTrue("Error should mention 'same patient'",
                result.getErrors().get(0).toLowerCase().contains("same patient"));
    }

    /**
     * Test: Validation should fail if patient 1 is already merged. Business Rule:
     * Cannot merge an already merged patient.
     */
    @Test
    public void testValidateMerge_Patient1AlreadyMerged_ShouldFail() {
        // Arrange
        patient1.setIsMerged(true);
        patient1.setMergedIntoPatientId("999");
        when(patientDAO.getData(anyString())).thenReturn(patient1);

        // Act
        PatientMergeValidationResultDTO result = patientMergeService.validateMerge(validRequest);

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("Validation should fail if patient already merged", result.isValid());
        assertTrue("Should have error about already merged patient",
                result.getErrors().stream().anyMatch(err -> err.toLowerCase().contains("already merged")));
    }

    /**
     * Test: Validation should fail if patient 2 is already merged. Business Rule:
     * Cannot merge an already merged patient.
     */
    @Test
    public void testValidateMerge_Patient2AlreadyMerged_ShouldFail() {
        // Arrange
        patient2.setIsMerged(true);
        patient2.setMergedIntoPatientId("999");
        when(patientDAO.getData("1")).thenReturn(patient1);
        when(patientDAO.getData("2")).thenReturn(patient2);

        // Act
        PatientMergeValidationResultDTO result = patientMergeService.validateMerge(validRequest);

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("Validation should fail if patient already merged", result.isValid());
        assertTrue("Should have error about already merged patient",
                result.getErrors().stream().anyMatch(err -> err.toLowerCase().contains("already merged")));
    }

    /**
     * Test: Validation should fail if circular merge reference detected. Business
     * Rule: Prevent circular reference chains (A→B→A).
     */
    @Test
    public void testValidateMerge_CircularReference_ShouldFail() {
        // Arrange - Patient 1 is merged into Patient 2 (circular scenario)
        patient1.setIsMerged(true);
        patient1.setMergedIntoPatientId("2");
        when(patientDAO.getData("1")).thenReturn(patient1);
        when(patientDAO.getData("2")).thenReturn(patient2);

        // Act
        PatientMergeValidationResultDTO result = patientMergeService.validateMerge(validRequest);

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("Validation should fail for circular reference", result.isValid());
        assertTrue("Should have error about circular reference or already merged", result.getErrors().stream().anyMatch(
                err -> err.toLowerCase().contains("circular") || err.toLowerCase().contains("already merged")));
    }

    /**
     * Test: Validation should succeed for two valid, unmerged patients.
     * Business Rule: Valid patients can be merged.
     */
    @Test
    public void testValidateMerge_ValidPatients_ShouldSucceed() {
        // Arrange
        when(patientDAO.getData("1")).thenReturn(patient1);
        when(patientDAO.getData("2")).thenReturn(patient2);
        when(iStatusService.getStatusID(AnalysisStatus.Canceled)).thenReturn("2");
        when(iStatusService.getStatusID(AnalysisStatus.SampleRejected)).thenReturn("4");
        when(iStatusService.getStatusID(AnalysisStatus.NotStarted)).thenReturn("5");

        // Act
        PatientMergeValidationResultDTO result = patientMergeService.validateMerge(validRequest);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Validation should succeed for valid patients", result.isValid());
        assertTrue("Should have no errors", result.getErrors().isEmpty());
        assertNotNull("Should have data summary", result.getDataSummary());
    }

    /**
     * Test: Validation should fail if patient not found.
     * Business Rule: Both patients must exist in database.
     */
    @Test
    public void testValidateMerge_PatientNotFound_ShouldFail() {
        // Arrange
        when(patientDAO.getData("1")).thenReturn(null);

        // Act
        PatientMergeValidationResultDTO result = patientMergeService.validateMerge(validRequest);

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("Validation should fail if patient not found", result.isValid());
        assertTrue("Should have error about patient not found",
                result.getErrors().stream()
                        .anyMatch(err -> err.toLowerCase().contains("not found")));
    }
}
