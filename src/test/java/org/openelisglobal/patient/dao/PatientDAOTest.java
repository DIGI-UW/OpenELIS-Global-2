package org.openelisglobal.patient.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link PatientDAO}.
 *
 * <p>
 * These tests verify data integrity at the DAO layer for CRUD operations,
 * edge-case lookups (non-existent IDs, special characters in names), and
 * correct delegation to the underlying Hibernate/DBUnit-managed data source.
 *
 * <p>
 * Dataset: {@code testdata/patient.xml} — 4 patients, 4 persons.
 *
 * @see org.openelisglobal.patient.service.PatientServiceTest
 */
public class PatientDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientDAO patientDAO;

    @Autowired
    private PersonService personService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/patient.xml");
    }

    // ── getAllPatients ────────────────────────────────────────────────────────

    @Test
    public void getAllPatients_shouldReturnAllPatientsInDataset() throws Exception {
        List<Patient> patients = patientDAO.getAllPatients();

        assertNotNull("getAllPatients should never return null", patients);
        assertEquals("Dataset contains exactly 4 patients", 4, patients.size());
    }

    // ── getData(String) ──────────────────────────────────────────────────────

    @Test
    public void getData_shouldReturnCorrectPatientForValidId() throws Exception {
        Patient patient = patientDAO.getData("1");

        assertNotNull("Patient with id=1 should exist in the dataset", patient);
        assertEquals("Patient id should match", "1", patient.getId());
        assertEquals("Gender should match dataset value", "M", patient.getGender());
    }

    @Test
    public void getData_shouldReturnNullForNonExistentId() throws Exception {
        Patient patient = patientDAO.getData("9999");

        assertNull("getData should return null for a non-existent patient ID", patient);
    }

    // ── readPatient ──────────────────────────────────────────────────────────

    @Test
    public void readPatient_shouldReturnPatientWithPersonForValidId() throws Exception {
        Patient patient = patientDAO.readPatient("1");

        assertNotNull("readPatient should return a patient for a valid id", patient);
        assertNotNull("Person should be loaded alongside the patient", patient.getPerson());
        assertEquals("First name should match dataset", "John", patient.getPerson().getFirstName());
        assertEquals("Last name should match dataset", "Doe", patient.getPerson().getLastName());
    }

    // ── externalIDExists ─────────────────────────────────────────────────────

    @Test
    public void externalIDExists_shouldReturnTrueForExistingExternalId() throws Exception {
        assertTrue("EX123 exists in the dataset", patientDAO.externalIDExists("EX123"));
    }

    @Test
    public void externalIDExists_shouldReturnFalseForNonExistentExternalId() throws Exception {
        assertFalse("A random external ID should not exist in the dataset",
                patientDAO.externalIDExists("DOES_NOT_EXIST_XYZ"));
    }

    // ── getPatientByNationalId ───────────────────────────────────────────────

    @Test
    public void getPatientByNationalId_shouldReturnCorrectPatient() throws Exception {
        Patient patient = patientDAO.getPatientByNationalId("1234");

        assertNotNull("Patient with nationalId=1234 should be found", patient);
        assertEquals("Returned patient id should be 1", "1", patient.getId());
    }

    @Test
    public void getPatientByNationalId_shouldReturnNullForUnknownNationalId() throws Exception {
        Patient patient = patientDAO.getPatientByNationalId("UNKNOWN_NATIONAL_ID");

        assertNull("Should return null when no patient has the given national ID", patient);
    }

    // ── getPatientsByNationalId ──────────────────────────────────────────────

    @Test
    public void getPatientsByNationalId_shouldReturnListForExistingId() throws Exception {
        List<Patient> patients = patientDAO.getPatientsByNationalId("1234");

        assertNotNull("Result list should not be null", patients);
        assertEquals("Exactly one patient has nationalId=1234", 1, patients.size());
    }

    @Test
    public void getPatientsByNationalId_shouldReturnEmptyListForUnknownId() throws Exception {
        List<Patient> patients = patientDAO.getPatientsByNationalId("NO_SUCH_ID");

        assertNotNull("Result should be an empty list, not null", patients);
        assertTrue("No patients should match an unknown national ID", patients.isEmpty());
    }

    // ── getPatientByExternalId ───────────────────────────────────────────────

    @Test
    public void getPatientByExternalId_shouldReturnCorrectPatient() throws Exception {
        Patient patient = patientDAO.getPatientByExternalId("EX123");

        assertNotNull("Patient with externalId=EX123 should be found", patient);
        assertEquals("Gender should match dataset value", "F", patient.getGender());
    }

    @Test
    public void getPatientByExternalId_shouldReturnNullForNonExistentExternalId() throws Exception {
        Patient patient = patientDAO.getPatientByExternalId("EX_DOES_NOT_EXIST");

        assertNull("Should return null when no patient has the given external ID", patient);
    }

    // ── getPatientByPerson ───────────────────────────────────────────────────

    @Test
    public void getPatientByPerson_shouldReturnPatientLinkedToPerson() throws Exception {
        // personService.get() returns the Person correctly — the same dataset is loaded
        Person person = personService.get("1");
        Patient patient = patientDAO.getPatientByPerson(person);

        assertNotNull("Patient should be found for person with id=1", patient);
        assertEquals("Patient id should be 1", "1", patient.getId());
    }

    // ── getAllMissingFhirUuid ─────────────────────────────────────────────────

    @Test
    public void getAllMissingFhirUuid_shouldReturnAllPatientsWhenNoneHaveUuid() throws Exception {
        // No fhirUuid is set in the dataset — all 4 patients are missing it
        List<Patient> patients = patientDAO.getAllMissingFhirUuid();

        assertNotNull("Result should not be null", patients);
        assertEquals("All 4 dataset patients are missing a FHIR UUID", 4, patients.size());
    }

    @Test
    public void getAllMissingFhirUuid_shouldExcludePatientOnceUuidIsAssigned() throws Exception {
        // Assign a UUID to patient 1 and persist it at the DAO layer
        Patient patient = patientDAO.getData("1");
        patient.setFhirUuid(UUID.randomUUID());
        patient.setSysUserId("1");
        patientDAO.update(patient);

        List<Patient> missing = patientDAO.getAllMissingFhirUuid();

        assertEquals("Only 3 patients should remain without a FHIR UUID", 3, missing.size());
        assertTrue("Patient 1 should no longer appear in the missing-UUID list",
                missing.stream().noneMatch(p -> "1".equals(p.getId())));
    }

    // ── Edge case: special characters in patient name ────────────────────────

    @Test
    public void insertPatient_withSpecialCharactersInName_shouldPersistAndRetrieveCorrectly() throws Exception {
        // Clear existing rows so the DB sequence starts fresh and has no ID conflicts
        cleanRowsInCurrentConnection(new String[] { "patient", "person" });

        Person person = new Person();
        person.setFirstName("José");
        person.setLastName("O'Brien");
        person.setSysUserId("1");
        personService.save(person);

        Patient patient = new Patient();
        patient.setPerson(person);
        patient.setGender("M");
        patient.setNationalId("SPECIAL-001");
        patient.setBirthDate(Timestamp.valueOf("1990-01-01 00:00:00"));
        patient.setSysUserId("1");

        String insertedId = patientDAO.insert(patient);
        Patient retrieved = patientDAO.getData(insertedId);

        assertNotNull("Patient with special-character name should be retrievable", retrieved);
        assertEquals("First name with accent should be stored and retrieved without corruption", "José",
                retrieved.getPerson().getFirstName());
        assertEquals("Last name with apostrophe should be stored and retrieved without corruption", "O'Brien",
                retrieved.getPerson().getLastName());
    }

    // ── Edge case: lookup by non-existent UUID-formatted external ID ──────────

    @Test
    public void getPatientByExternalId_withUuidFormattedId_shouldReturnNullWhenNotFound() throws Exception {
        // Simulates a FHIR-based lookup using a UUID-formatted string that does not
        // exist in the system — a common integration scenario.
        String nonExistentUuid = UUID.randomUUID().toString();

        Patient patient = patientDAO.getPatientByExternalId(nonExistentUuid);

        assertNull("Searching by a random UUID-formatted external ID should return null", patient);
    }

    // ── getPageOfPatients ────────────────────────────────────────────────────

    @Test
    public void getPageOfPatients_shouldReturnNonEmptyPage() throws Exception {
        List<Patient> page = patientDAO.getPageOfPatients(1);

        assertNotNull("Page result should not be null", page);
        assertFalse("First page should contain at least one patient", page.isEmpty());
    }
}