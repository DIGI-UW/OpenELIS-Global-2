package org.openelisglobal.patient.daoimpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for redirect-on-lookup pattern (interim solution for
 * BLOCKER-001). These tests verify that getData() works at runtime with real
 * database operations.
 *
 * Pattern: When a patient is merged, is_merged=true and merged_into_patient_id
 * points to primary. Application should redirect lookups to primary patient.
 */
public class PatientDAORedirectIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientDAO patientDAO;

    @Autowired
    private PersonService personService;

    private Patient primaryPatient;
    private Patient mergedPatient;
    private Person primaryPerson;
    private Person mergedPerson;

    @Before
    public void setUp() throws Exception {
        // Create persons for test patients
        primaryPerson = new Person();
        primaryPerson.setFirstName("John");
        primaryPerson.setLastName("Doe");
        String primaryPersonId = personService.insert(primaryPerson);
        primaryPerson.setId(primaryPersonId);

        mergedPerson = new Person();
        mergedPerson.setFirstName("Jonathan");
        mergedPerson.setLastName("Doe");
        String mergedPersonId = personService.insert(mergedPerson);
        mergedPerson.setId(mergedPersonId);

        // Create primary patient with unique identifiers
        primaryPatient = new Patient();
        primaryPatient.setPerson(primaryPerson);
        primaryPatient.setNationalId("MERGE-TEST-PRIMARY-" + System.currentTimeMillis());
        primaryPatient.setExternalId("MERGE-TEST-EXT-PRIMARY-" + System.currentTimeMillis());
        primaryPatient.setIsMerged(false);
        String primaryId = patientDAO.insert(primaryPatient);
        primaryPatient.setId(primaryId);

        // Create merged patient (will be marked as merged in tests) with unique
        // identifiers
        mergedPatient = new Patient();
        mergedPatient.setPerson(mergedPerson);
        mergedPatient.setNationalId("MERGE-TEST-MERGED-" + System.currentTimeMillis());
        mergedPatient.setExternalId("MERGE-TEST-EXT-MERGED-" + System.currentTimeMillis());
        mergedPatient.setIsMerged(false);
        String mergedId = patientDAO.insert(mergedPatient);
        mergedPatient.setId(mergedId);
    }

    /**
     * Test: Primary key lookup returns actual patient (NO redirect). Business Rule:
     * Direct ID lookups are for admin/audit, return actual record.
     */
    @Test
    public void testGetDataByPrimaryKey_NonMergedPatient_ReturnsSelf() {
        // Arrange - Primary patient is not merged
        assertFalse("Primary should not be merged", Boolean.TRUE.equals(primaryPatient.getIsMerged()));

        // Act - Retrieve by primary key (NO redirect expected)
        Patient result = patientDAO.getData(primaryPatient.getId());

        // Assert - Should return same patient
        assertNotNull("Result should not be null", result);
        assertEquals("Should return same patient ID", primaryPatient.getId(), result.getId());
        assertEquals("Should return same national ID", primaryPatient.getNationalId(), result.getNationalId());
        assertFalse("Result should not be marked as merged", Boolean.TRUE.equals(result.getIsMerged()));
    }

    /**
     * Test: Primary key lookup returns merged patient record (NO redirect).
     * Business Rule: Direct ID lookups return actual record for admin/audit
     * purposes.
     */
    @Test
    public void testGetDataByPrimaryKey_MergedPatient_ReturnsActualRecord() {
        // Arrange - Mark merged patient as merged into primary
        mergedPatient.setIsMerged(true);
        mergedPatient.setMergedIntoPatientId(primaryPatient.getId());
        mergedPatient.setMergeDate(new java.sql.Timestamp(System.currentTimeMillis()));
        patientDAO.update(mergedPatient);

        // Act - Retrieve merged patient by primary key (NO redirect)
        Patient result = patientDAO.getData(mergedPatient.getId());

        // Assert - Should return MERGED patient (not primary)
        assertNotNull("Result should not be null", result);
        assertEquals("Should return merged patient ID", mergedPatient.getId(), result.getId());
        assertEquals("Should return merged patient national ID", mergedPatient.getNationalId(), result.getNationalId());
        assertTrue("Should be marked as merged", Boolean.TRUE.equals(result.getIsMerged()));
        assertEquals("Should reference primary patient", primaryPatient.getId(), result.getMergedIntoPatientId());
    }

    /**
     * Test: Non-existent patient returns null. Business Rule: Invalid patient IDs
     * return null.
     */
    @Test
    public void testGetPatientWithMergeRedirect_NonExistentPatient_ReturnsNull() {
        // Act - Retrieve non-existent patient
        Patient result = patientDAO.getData("999999");

        // Assert - Should return null
        assertNull("Non-existent patient should return null", result);
    }

    /**
     * Test: Foreign key constraint ensures referential integrity. Business Rule:
     * merged_into_patient_id must reference valid patient. This verifies merged
     * patient record maintains integrity.
     */
    @Test
    public void testMergedPatient_MaintainsReferentialIntegrity() {
        // Arrange - Mark merged patient as merged into primary
        mergedPatient.setIsMerged(true);
        mergedPatient.setMergedIntoPatientId(primaryPatient.getId());
        mergedPatient.setMergeDate(new java.sql.Timestamp(System.currentTimeMillis()));
        patientDAO.update(mergedPatient);

        // Act - Retrieve merged patient by primary key
        Patient result = patientDAO.getData(mergedPatient.getId());

        // Assert - Should return merged patient with valid reference
        assertNotNull("Result should not be null", result);
        assertEquals("Should return merged patient ID", mergedPatient.getId(), result.getId());
        assertTrue("Should be marked as merged", Boolean.TRUE.equals(result.getIsMerged()));
        assertEquals("Should reference primary patient", primaryPatient.getId(), result.getMergedIntoPatientId());

        // Verify referenced primary patient exists
        Patient referencedPrimary = patientDAO.getData(result.getMergedIntoPatientId());
        assertNotNull("Referenced primary patient should exist", referencedPrimary);
        assertFalse("Primary should not be merged", Boolean.TRUE.equals(referencedPrimary.getIsMerged()));
    }

    /**
     * Test: Verify identifier lookup redirects and preserves display values.
     * Business Rule: updateDisplayValues() should be called on redirected patient.
     */
    @Test
    public void testIdentifierLookup_RedirectsAndPreservesDisplayValues() {
        // Arrange - Set birth date on primary patient
        primaryPatient.setBirthDate(new java.sql.Timestamp(System.currentTimeMillis()));
        patientDAO.update(primaryPatient);

        // Mark merged patient
        mergedPatient.setIsMerged(true);
        mergedPatient.setMergedIntoPatientId(primaryPatient.getId());
        mergedPatient.setMergeDate(new java.sql.Timestamp(System.currentTimeMillis()));
        patientDAO.update(mergedPatient);

        // Act - Retrieve by identifier (should redirect and update display values)
        Patient result = patientDAO.getPatientByNationalId(mergedPatient.getNationalId());

        // Assert - Should redirect to primary with display values populated
        assertNotNull("Result should not be null", result);
        assertEquals("Should redirect to primary patient", primaryPatient.getId(), result.getId());
        assertNotNull("Birth date should be set", result.getBirthDate());
        assertNotNull("Birth date for display should be populated", result.getBirthDateForDisplay());
    }

    /**
     * Test: Lookup by National ID redirects to primary patient. Business Rule:
     * Identifier-based lookups should also redirect.
     */
    @Test
    public void testGetPatientByNationalId_MergedPatient_RedirectsToPrimary() {
        // Arrange - Mark merged patient
        mergedPatient.setIsMerged(true);
        mergedPatient.setMergedIntoPatientId(primaryPatient.getId());
        mergedPatient.setMergeDate(new java.sql.Timestamp(System.currentTimeMillis()));
        patientDAO.update(mergedPatient);

        // Act - Lookup by merged patient's national ID
        Patient result = patientDAO.getPatientByNationalId(mergedPatient.getNationalId());

        // Assert - Should redirect to primary patient
        assertNotNull("Result should not be null", result);
        assertEquals("Should return primary patient ID", primaryPatient.getId(), result.getId());
        assertEquals("Should return primary patient national ID", primaryPatient.getNationalId(),
                result.getNationalId());
        assertFalse("Primary patient should not be merged", Boolean.TRUE.equals(result.getIsMerged()));
    }

    /**
     * Test: Lookup by External ID redirects to primary patient. Business Rule: All
     * identifier types should redirect.
     */
    @Test
    public void testGetPatientByExternalId_MergedPatient_RedirectsToPrimary() {
        // Arrange - Mark merged patient
        mergedPatient.setIsMerged(true);
        mergedPatient.setMergedIntoPatientId(primaryPatient.getId());
        mergedPatient.setMergeDate(new java.sql.Timestamp(System.currentTimeMillis()));
        patientDAO.update(mergedPatient);

        // Act - Lookup by merged patient's external ID
        Patient result = patientDAO.getPatientByExternalId(mergedPatient.getExternalId());

        // Assert - Should redirect to primary patient
        assertNotNull("Result should not be null", result);
        assertEquals("Should return primary patient ID", primaryPatient.getId(), result.getId());
        assertEquals("Should return primary patient external ID", primaryPatient.getExternalId(),
                result.getExternalId());
    }

    /**
     * Test: getByExternalId also redirects (different method name, same behavior).
     * Business Rule: All variations of external ID lookup should redirect.
     */
    @Test
    public void testGetByExternalId_MergedPatient_RedirectsToPrimary() {
        // Arrange - Mark merged patient
        mergedPatient.setIsMerged(true);
        mergedPatient.setMergedIntoPatientId(primaryPatient.getId());
        mergedPatient.setMergeDate(new java.sql.Timestamp(System.currentTimeMillis()));
        patientDAO.update(mergedPatient);

        // Act - Lookup using getByExternalId method
        Patient result = patientDAO.getByExternalId(mergedPatient.getExternalId());

        // Assert - Should redirect to primary patient
        assertNotNull("Result should not be null", result);
        assertEquals("Should return primary patient ID", primaryPatient.getId(), result.getId());
        assertEquals("Should return primary patient external ID", primaryPatient.getExternalId(),
                result.getExternalId());
    }
}
