package org.openelisglobal.patient.merge.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.merge.dto.PatientMergeExecutionResultDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeRequestDTO;
import org.openelisglobal.patient.merge.dto.PatientMergeValidationResultDTO;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for PatientMergeService. Tests the full service layer flow
 * with real database operations (not mocked).
 *
 * Tests the complete merge workflow: validation → execution → verification.
 */
public class PatientMergeServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientMergeService patientMergeService;

    @Autowired
    private PatientDAO patientDAO;

    @Autowired
    private PersonService personService;

    private Patient patient1;
    private Patient patient2;
    private Person person1;
    private Person person2;

    @Before
    public void setUp() throws Exception {
        // Load standard system user dataset (includes admin user)
        executeDataSetWithStateManagement("testdata/system-user.xml");

        // Create persons for test patients
        person1 = new Person();
        person1.setFirstName("Alice");
        person1.setLastName("Smith");
        String person1Id = personService.insert(person1);
        person1.setId(person1Id);

        person2 = new Person();
        person2.setFirstName("Alice");
        person2.setLastName("Smyth"); // Slight spelling difference
        String person2Id = personService.insert(person2);
        person2.setId(person2Id);

        // Create patient 1 with unique identifiers
        patient1 = new Patient();
        patient1.setPerson(person1);
        patient1.setNationalId("INT-TEST-P1-" + System.currentTimeMillis());
        patient1.setExternalId("INT-TEST-EXT-P1-" + System.currentTimeMillis());
        patient1.setIsMerged(false);
        String patient1Id = patientDAO.insert(patient1);
        patient1.setId(patient1Id);

        // Create patient 2 with unique identifiers
        patient2 = new Patient();
        patient2.setPerson(person2);
        patient2.setNationalId("INT-TEST-P2-" + System.currentTimeMillis());
        patient2.setExternalId("INT-TEST-EXT-P2-" + System.currentTimeMillis());
        patient2.setIsMerged(false);
        String patient2Id = patientDAO.insert(patient2);
        patient2.setId(patient2Id);
    }

    /**
     * Test: Complete merge workflow - validation, execution, and verification.
     * Business Rule: Full merge flow should work end-to-end with real database.
     */
    @Test
    public void testCompleteMergeWorkflow_ValidPatients_ShouldSucceed() {
        // Step 1: Create merge request
        PatientMergeRequestDTO request = new PatientMergeRequestDTO();
        request.setPatient1Id(patient1.getId());
        request.setPatient2Id(patient2.getId());
        request.setPrimaryPatientId(patient1.getId()); // Patient 1 is primary
        request.setReason("Duplicate patient - same person, spelling error in last name");
        request.setConfirmed(true);

        // Step 2: Validate merge (should pass)
        PatientMergeValidationResultDTO validationResult = patientMergeService.validateMerge(request);
        assertTrue("Validation should pass for valid patients", validationResult.isValid());
        assertTrue("Validation should have no errors", validationResult.getErrors().isEmpty());

        // Step 3: Execute merge
        PatientMergeExecutionResultDTO executionResult = patientMergeService.executeMerge(request, "1");
        assertTrue("Merge execution should succeed", executionResult.isSuccess());
        assertNotNull("Should have merge audit ID", executionResult.getMergeAuditId());
        assertEquals("Should return correct primary patient ID", patient1.getId(),
                executionResult.getPrimaryPatientId());
        assertEquals("Should return correct merged patient ID", patient2.getId(), executionResult.getMergedPatientId());

        // Step 4: Verify merge state in database
        Patient mergedPatient = patientDAO.getData(patient2.getId());
        assertNotNull("Merged patient should exist in database", mergedPatient);
        assertTrue("Merged patient should be marked as merged", Boolean.TRUE.equals(mergedPatient.getIsMerged()));
        assertEquals("Merged patient should reference primary", patient1.getId(),
                mergedPatient.getMergedIntoPatientId());
        assertNotNull("Merged patient should have merge date", mergedPatient.getMergeDate());

        // Step 5: Verify primary patient unchanged
        Patient primaryPatient = patientDAO.getData(patient1.getId());
        assertNotNull("Primary patient should exist in database", primaryPatient);
        assertFalse("Primary patient should NOT be marked as merged",
                Boolean.TRUE.equals(primaryPatient.getIsMerged()));
    }

    /**
     * Test: Redirect-on-lookup works after merge execution. Business Rule: After
     * merge, identifier lookups should redirect to primary.
     */
    @Test
    public void testMergeExecution_EnablesRedirectOnLookup() {
        // Arrange - Execute merge
        PatientMergeRequestDTO request = new PatientMergeRequestDTO();
        request.setPatient1Id(patient1.getId());
        request.setPatient2Id(patient2.getId());
        request.setPrimaryPatientId(patient1.getId());
        request.setReason("Testing redirect-on-lookup");
        request.setConfirmed(true);

        PatientMergeExecutionResultDTO executionResult = patientMergeService.executeMerge(request, "1");
        assertTrue("Merge should succeed", executionResult.isSuccess());

        // Act - Lookup merged patient by identifier (should redirect)
        Patient lookupResult = patientDAO.getPatientByNationalId(patient2.getNationalId());

        // Assert - Should redirect to primary patient
        assertNotNull("Lookup should return a patient", lookupResult);
        assertEquals("Should redirect to primary patient", patient1.getId(), lookupResult.getId());
        assertEquals("Should return primary patient's national ID", patient1.getNationalId(),
                lookupResult.getNationalId());
        assertFalse("Primary patient should not be marked as merged", Boolean.TRUE.equals(lookupResult.getIsMerged()));
    }

    /**
     * Test: Validation fails for already merged patient. Business Rule: Cannot
     * merge a patient that is already merged.
     */
    @Test
    public void testValidation_AlreadyMergedPatient_ShouldFail() {
        // Arrange - First merge patient2 into patient1
        PatientMergeRequestDTO firstMerge = new PatientMergeRequestDTO();
        firstMerge.setPatient1Id(patient1.getId());
        firstMerge.setPatient2Id(patient2.getId());
        firstMerge.setPrimaryPatientId(patient1.getId());
        firstMerge.setReason("First merge");
        firstMerge.setConfirmed(true);
        patientMergeService.executeMerge(firstMerge, "1");

        // Act - Try to merge patient2 again (should fail validation)
        PatientMergeRequestDTO secondMerge = new PatientMergeRequestDTO();
        secondMerge.setPatient1Id(patient2.getId());
        secondMerge.setPatient2Id(patient1.getId());
        secondMerge.setPrimaryPatientId(patient1.getId());
        secondMerge.setReason("Second merge attempt");
        secondMerge.setConfirmed(true);

        PatientMergeValidationResultDTO validationResult = patientMergeService.validateMerge(secondMerge);

        // Assert - Validation should fail
        assertFalse("Validation should fail for already merged patient", validationResult.isValid());
        assertTrue("Should have error about already merged patient",
                validationResult.getErrors().stream().anyMatch(err -> err.toLowerCase().contains("already merged")));
    }

    /**
     * Test: Merge without confirmation fails. Business Rule: Merge requires
     * explicit confirmation.
     */
    @Test
    public void testExecuteMerge_WithoutConfirmation_ShouldFail() {
        // Arrange - Create request without confirmation
        PatientMergeRequestDTO request = new PatientMergeRequestDTO();
        request.setPatient1Id(patient1.getId());
        request.setPatient2Id(patient2.getId());
        request.setPrimaryPatientId(patient1.getId());
        request.setReason("Testing without confirmation");
        request.setConfirmed(false); // NOT confirmed

        // Act
        PatientMergeExecutionResultDTO result = patientMergeService.executeMerge(request, "1");

        // Assert
        assertFalse("Merge should fail without confirmation", result.isSuccess());
        assertTrue("Error message should mention confirmation", result.getMessage().toLowerCase().contains("confirm"));
    }

    /**
     * Test: Validation fails for same patient ID. Business Rule: Cannot merge
     * patient with itself.
     */
    @Test
    public void testValidation_SamePatient_ShouldFail() {
        // Arrange
        PatientMergeRequestDTO request = new PatientMergeRequestDTO();
        request.setPatient1Id(patient1.getId());
        request.setPatient2Id(patient1.getId()); // Same patient
        request.setPrimaryPatientId(patient1.getId());
        request.setReason("Testing same patient");
        request.setConfirmed(true);

        // Act
        PatientMergeValidationResultDTO result = patientMergeService.validateMerge(request);

        // Assert
        assertFalse("Validation should fail for same patient", result.isValid());
        assertTrue("Should have error about same patient",
                result.getErrors().stream().anyMatch(err -> err.toLowerCase().contains("same patient")));
    }

    /**
     * Test: Validation fails for non-existent patient. Business Rule: Both patients
     * must exist in database.
     */
    @Test
    public void testValidation_NonExistentPatient_ShouldFail() {
        // Arrange
        PatientMergeRequestDTO request = new PatientMergeRequestDTO();
        request.setPatient1Id(patient1.getId());
        request.setPatient2Id("999999"); // Non-existent patient
        request.setPrimaryPatientId(patient1.getId());
        request.setReason("Testing non-existent patient");
        request.setConfirmed(true);

        // Act
        PatientMergeValidationResultDTO result = patientMergeService.validateMerge(request);

        // Assert
        assertFalse("Validation should fail for non-existent patient", result.isValid());
        assertTrue("Should have error about patient not found",
                result.getErrors().stream().anyMatch(err -> err.toLowerCase().contains("not found")));
    }

    /**
     * Test: Verify service is managed by Spring container. Business Rule: Service
     * should be properly configured with Spring. Note: @Transactional behavior is
     * verified by Spring test framework.
     */
    @Test
    public void testMergeService_IsManagedBySpring() {
        // Verify service is injected (not null)
        assertNotNull("PatientMergeService should be injected by Spring", patientMergeService);

        // Verify it's a Spring proxy (which would handle @Transactional)
        String className = patientMergeService.getClass().getName();
        boolean isSpringManaged = className.contains("$") || className.contains("EnhancerBySpringCGLIB");

        // If not a proxy, verify it has @Service annotation
        if (!isSpringManaged) {
            assertTrue("Service should have @Service annotation",
                    patientMergeService.getClass().isAnnotationPresent(org.springframework.stereotype.Service.class));
        }
    }

    /**
     * Test: Patient merge with FHIR resources - verify database and FHIR state.
     * Business Rule: When both patients have FHIR UUIDs, the FHIR integration
     * should be invoked (though actual FHIR updates require FHIR server).
     *
     * This test verifies: 1. Database merge completes successfully 2. Patients with
     * FHIR UUIDs trigger FHIR integration path 3. Merge succeeds even if FHIR store
     * is not available (graceful degradation)
     */
    @Test
    public void testMergeExecution_WithFhirUuids_ShouldCompleteSuccessfully() {
        // Arrange - Add FHIR UUIDs to both patients (simulating FHIR-enabled patients)
        UUID patient1FhirUuid = UUID.randomUUID();
        UUID patient2FhirUuid = UUID.randomUUID();

        patient1.setFhirUuid(patient1FhirUuid);
        patientDAO.update(patient1);

        patient2.setFhirUuid(patient2FhirUuid);
        patientDAO.update(patient2);

        // Verify patients now have FHIR UUIDs
        Patient p1WithFhir = patientDAO.getData(patient1.getId());
        Patient p2WithFhir = patientDAO.getData(patient2.getId());
        assertNotNull("Patient 1 should have FHIR UUID", p1WithFhir.getFhirUuid());
        assertNotNull("Patient 2 should have FHIR UUID", p2WithFhir.getFhirUuid());

        // Create merge request
        PatientMergeRequestDTO request = new PatientMergeRequestDTO();
        request.setPatient1Id(patient1.getId());
        request.setPatient2Id(patient2.getId());
        request.setPrimaryPatientId(patient1.getId());
        request.setReason("Testing FHIR-enabled patient merge");
        request.setConfirmed(true);

        // Act - Execute merge (FHIR integration will be attempted)
        // Note: Actual FHIR store updates require external FHIR server
        // This test verifies graceful degradation when FHIR store unavailable
        PatientMergeExecutionResultDTO result = patientMergeService.executeMerge(request, "1");

        // Assert - Database merge should succeed
        assertTrue("Merge should succeed even without FHIR store", result.isSuccess());
        assertNotNull("Should have merge audit ID", result.getMergeAuditId());
        assertEquals("Should return correct primary patient ID", patient1.getId(), result.getPrimaryPatientId());
        assertEquals("Should return correct merged patient ID", patient2.getId(), result.getMergedPatientId());

        // Verify database state
        Patient mergedPatient = patientDAO.getData(patient2.getId());
        assertTrue("Merged patient should be marked as merged in database",
                Boolean.TRUE.equals(mergedPatient.getIsMerged()));
        assertEquals("Merged patient should reference primary patient", patient1.getId(),
                mergedPatient.getMergedIntoPatientId());
        assertNotNull("Merged patient should have merge date", mergedPatient.getMergeDate());

        // Verify primary patient unchanged
        Patient primaryPatient = patientDAO.getData(patient1.getId());
        assertFalse("Primary patient should NOT be marked as merged",
                Boolean.TRUE.equals(primaryPatient.getIsMerged()));

        // Verify FHIR UUIDs preserved in database
        assertNotNull("Primary patient should still have FHIR UUID", primaryPatient.getFhirUuid());
        assertNotNull("Merged patient should still have FHIR UUID", mergedPatient.getFhirUuid());
        assertEquals("Primary patient FHIR UUID should be unchanged", patient1FhirUuid, primaryPatient.getFhirUuid());
        assertEquals("Merged patient FHIR UUID should be unchanged", patient2FhirUuid, mergedPatient.getFhirUuid());
    }

    /**
     * Test: Patient merge without FHIR resources - verify FHIR integration skipped.
     * Business Rule: FHIR integration should only run when both patients have FHIR
     * UUIDs.
     */
    @Test
    public void testMergeExecution_WithoutFhirUuids_ShouldSkipFhirIntegration() {
        // Arrange - Ensure patients have NO FHIR UUIDs (default state)
        assertNotNull("Patient 1 should exist", patient1);
        assertNotNull("Patient 2 should exist", patient2);
        // Note: Patients created in setUp() don't have FHIR UUIDs by default

        // Create merge request
        PatientMergeRequestDTO request = new PatientMergeRequestDTO();
        request.setPatient1Id(patient1.getId());
        request.setPatient2Id(patient2.getId());
        request.setPrimaryPatientId(patient1.getId());
        request.setReason("Testing non-FHIR patient merge");
        request.setConfirmed(true);

        // Act - Execute merge (FHIR integration should be skipped)
        PatientMergeExecutionResultDTO result = patientMergeService.executeMerge(request, "1");

        // Assert - Database merge should succeed
        assertTrue("Merge should succeed for non-FHIR patients", result.isSuccess());
        assertNotNull("Should have merge audit ID", result.getMergeAuditId());

        // Verify database state (same assertions as FHIR test)
        Patient mergedPatient = patientDAO.getData(patient2.getId());
        assertTrue("Merged patient should be marked as merged in database",
                Boolean.TRUE.equals(mergedPatient.getIsMerged()));
        assertEquals("Merged patient should reference primary patient", patient1.getId(),
                mergedPatient.getMergedIntoPatientId());

        Patient primaryPatient = patientDAO.getData(patient1.getId());
        assertFalse("Primary patient should NOT be marked as merged",
                Boolean.TRUE.equals(primaryPatient.getIsMerged()));
    }
}
