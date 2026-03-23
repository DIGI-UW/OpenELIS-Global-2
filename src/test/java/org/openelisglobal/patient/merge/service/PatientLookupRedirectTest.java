package org.openelisglobal.patient.merge.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
 * Tests for redirect-on-lookup pattern (interim solution for BLOCKER-001).
 * These tests verify that merged patient state enables proper redirection at
 * the service layer.
 *
 * Pattern: When a patient is merged, is_merged=true and merged_into_patient_id
 * points to primary. Application should redirect lookups to primary patient.
 */
public class PatientLookupRedirectTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientDAO patientDAO;

    @Autowired
    private PersonService personService;

    private Patient primaryPatient;
    private Patient mergedPatient;

    @Before
    public void setUp() throws Exception {
        // Create persons for test patients
        Person primaryPerson = new Person();
        primaryPerson.setFirstName("John");
        primaryPerson.setLastName("Doe");
        String primaryPersonId = personService.insert(primaryPerson);
        primaryPerson.setId(primaryPersonId);

        Person mergedPerson = new Person();
        mergedPerson.setFirstName("Jonathan");
        mergedPerson.setLastName("Doe");
        String mergedPersonId = personService.insert(mergedPerson);
        mergedPerson.setId(mergedPersonId);

        // Create primary patient with unique identifiers
        primaryPatient = new Patient();
        primaryPatient.setPerson(primaryPerson);
        primaryPatient.setNationalId("LOOKUP-TEST-PRIMARY-" + System.currentTimeMillis());
        primaryPatient.setExternalId("LOOKUP-TEST-EXT-PRIMARY-" + System.currentTimeMillis());
        primaryPatient.setIsMerged(false);
        String primaryId = patientDAO.insert(primaryPatient);
        primaryPatient.setId(primaryId);

        // Create merged patient with unique identifiers
        mergedPatient = new Patient();
        mergedPatient.setPerson(mergedPerson);
        mergedPatient.setNationalId("LOOKUP-TEST-MERGED-" + System.currentTimeMillis());
        mergedPatient.setExternalId("LOOKUP-TEST-EXT-MERGED-" + System.currentTimeMillis());
        mergedPatient.setIsMerged(false);
        String mergedId = patientDAO.insert(mergedPatient);
        mergedPatient.setId(mergedId);
    }

    /**
     * Test: Identifier-based lookup redirects to primary patient. Business Rule:
     * When is_merged=true, identifier lookups redirect to primary.
     */
    @Test
    public void testMergedPatient_IdentifierLookupRedirectsToPrimary() {
        // Arrange - Simulate merge
        mergedPatient.setIsMerged(true);
        mergedPatient.setMergedIntoPatientId(primaryPatient.getId());
        mergedPatient.setMergeDate(new java.sql.Timestamp(System.currentTimeMillis()));
        patientDAO.update(mergedPatient);

        // Act - Retrieve by identifier (should redirect to primary)
        Patient retrievedPatient = patientDAO.getPatientByNationalId(mergedPatient.getNationalId());

        // Assert - Should return primary patient (redirect happened)
        assertNotNull("Result should exist", retrievedPatient);
        assertEquals("Should return primary patient ID", primaryPatient.getId(), retrievedPatient.getId());
        assertEquals("Should return primary patient national ID", primaryPatient.getNationalId(),
                retrievedPatient.getNationalId());
        assertFalse("Primary patient should NOT be marked as merged",
                Boolean.TRUE.equals(retrievedPatient.getIsMerged()));
    }

    /**
     * Test: Primary patient does NOT have redirect state. Business Rule: Primary
     * patient remains active, is_merged=false.
     */
    @Test
    public void testPrimaryPatient_NoRedirectState() {
        // Act - Retrieve primary patient
        Patient retrievedPrimary = patientDAO.getData(primaryPatient.getId());

        // Assert - Primary patient has no redirect
        assertNotNull("Primary patient should exist", retrievedPrimary);
        assertFalse("Primary should NOT be marked as merged", Boolean.TRUE.equals(retrievedPrimary.getIsMerged()));
        assertEquals("Primary ID unchanged", primaryPatient.getId(), retrievedPrimary.getId());
    }

    /**
     * Test: Primary key lookup returns actual merged patient record (NO redirect).
     * Business Rule: Direct ID lookups are for admin/audit, return actual record.
     */
    @Test
    public void testPrimaryKeyLookup_NoRedirect() {
        // Arrange - Mark merged patient
        mergedPatient.setIsMerged(true);
        mergedPatient.setMergedIntoPatientId(primaryPatient.getId());
        mergedPatient.setMergeDate(new java.sql.Timestamp(System.currentTimeMillis()));
        patientDAO.update(mergedPatient);

        // Act - Use primary key lookup (NO redirect)
        Patient result = patientDAO.getData(mergedPatient.getId());

        // Assert - Should return merged patient (NOT primary)
        assertNotNull("Result should not be null", result);
        assertEquals("Should return merged patient ID", mergedPatient.getId(), result.getId());
        assertEquals("Should return merged patient national ID", mergedPatient.getNationalId(), result.getNationalId());
        assertTrue("Should be marked as merged", Boolean.TRUE.equals(result.getIsMerged()));
        assertEquals("Should reference primary", primaryPatient.getId(), result.getMergedIntoPatientId());
    }

    /**
     * Test: Non-merged patient returns self (no redirect).
     */
    @Test
    public void testLookup_NonMergedPatient_ReturnsSelf() {
        // Arrange - Ensure primary is not merged
        assertFalse("Primary should not be merged", Boolean.TRUE.equals(primaryPatient.getIsMerged()));

        // Act - Use redirect method on non-merged patient
        Patient result = patientDAO.getData(primaryPatient.getId());

        // Assert - Returns self (no redirect)
        assertNotNull("Result should not be null", result);
        assertEquals("Should return same patient ID", primaryPatient.getId(), result.getId());
        assertEquals("Should return same national ID", primaryPatient.getNationalId(), result.getNationalId());
    }
}
