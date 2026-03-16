package org.openelisglobal.patient.valueholder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.person.valueholder.Person;

/**
 * Pure unit tests for the Patient valueholder.
 *
 * <p>
 * These tests require NO Spring context and NO Docker — they validate the
 * entity's field behaviour, date-display synchronisation, FHIR UUID handling,
 * and merge-state logic in isolation.
 */
public class PatientTest {

    private Patient patient;

    @Before
    public void setUp() {
        patient = new Patient();
    }

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    @Test
    public void constructor_ShouldInitialisePatientWithNullFields() {
        assertNull("id should be null by default", patient.getId());
        assertNull("gender should be null by default", patient.getGender());
        assertNull("birthDate should be null by default", patient.getBirthDate());
        assertNull("nationalId should be null by default", patient.getNationalId());
        assertNull("fhirUuid should be null by default", patient.getFhirUuid());
        assertFalse("isMerged should default to false", patient.getIsMerged());
    }

    // -----------------------------------------------------------------------
    // ID
    // -----------------------------------------------------------------------

    @Test
    public void setId_ShouldStoreAndReturnId() {
        patient.setId("42");
        assertEquals("42", patient.getId());
    }

    // -----------------------------------------------------------------------
    // Gender
    // -----------------------------------------------------------------------

    @Test
    public void setGender_ShouldStoreAndReturnGender() {
        patient.setGender("M");
        assertEquals("M", patient.getGender());
    }

    @Test
    public void setGender_ShouldAllowFemaleCode() {
        patient.setGender("F");
        assertEquals("F", patient.getGender());
    }

    @Test
    public void setGender_ShouldAllowUnknownCode() {
        patient.setGender("U");
        assertEquals("U", patient.getGender());
    }

    // -----------------------------------------------------------------------
    // Birth Date — Timestamp <-> display string synchronisation
    // -----------------------------------------------------------------------

    @Test
    public void setBirthDate_ShouldStoreTimestampDirectly() {
        // Tests that the Timestamp field is stored via setBirthDate().
        // NOTE: setBirthDate() also calls DateUtil.convertTimestampToStringDate()
        // which requires Spring. We therefore set the display string explicitly
        // first so the static block is never triggered.
        Timestamp dob = Timestamp.valueOf("1990-06-15 00:00:00");
        // Use the raw field path: set display then overwrite only the timestamp
        // by directly verifying the Timestamp roundtrip without touching DateUtil.
        assertNull("birthDate starts null", patient.getBirthDate());
        // Directly set the private backing field via reflection to avoid DateUtil
        try {
            java.lang.reflect.Field f = Patient.class.getDeclaredField("birthDate");
            f.setAccessible(true);
            f.set(patient, dob);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        assertEquals(dob, patient.getBirthDate());
    }

    @Test
    public void setBirthDateForDisplay_ShouldStoreDisplayString() {
        // We only test that the display string is stored. Back-population of the
        // Timestamp field calls DateUtil.convertAmbiguousStringDateToTimestamp()
        // which requires Spring, so we only assert the getter returns what we set.
        try {
            java.lang.reflect.Field f = Patient.class.getDeclaredField("birthDateForDisplay");
            f.setAccessible(true);
            f.set(patient, "15/06/1990");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        assertEquals("15/06/1990", patient.getBirthDateForDisplay());
    }

    @Test
    public void getBirthDateForDisplay_WhenNeverSet_ShouldReturnNull() {
        // birthDateForDisplay is null until explicitly set
        assertNull(patient.getBirthDateForDisplay());
    }

    // -----------------------------------------------------------------------
    // Birth Time
    // -----------------------------------------------------------------------

    @Test
    public void setBirthTime_ShouldStoreSqlDateDirectly() {
        // setBirthTime() also calls DateUtil — bypass via reflection
        Date birthTime = Date.valueOf("1990-06-15");
        try {
            java.lang.reflect.Field f = Patient.class.getDeclaredField("birthTime");
            f.setAccessible(true);
            f.set(patient, birthTime);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        assertEquals(birthTime, patient.getBirthTime());
    }

    // -----------------------------------------------------------------------
    // Death Date
    // -----------------------------------------------------------------------

    @Test
    public void setDeathDate_ShouldStoreSqlDateDirectly() {
        // setDeathDate() also calls DateUtil — bypass via reflection
        Date deathDate = Date.valueOf("2020-12-31");
        try {
            java.lang.reflect.Field f = Patient.class.getDeclaredField("deathDate");
            f.setAccessible(true);
            f.set(patient, deathDate);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        assertEquals(deathDate, patient.getDeathDate());
    }

    // -----------------------------------------------------------------------
    // EPI Name Fields
    // -----------------------------------------------------------------------

    @Test
    public void epiNameFields_ShouldBeSettableAndRetrievable() {
        patient.setEpiFirstName("John");
        patient.setEpiMiddleName("Arthur");
        patient.setEpiLastName("Doe");

        assertEquals("John", patient.getEpiFirstName());
        assertEquals("Arthur", patient.getEpiMiddleName());
        assertEquals("Doe", patient.getEpiLastName());
    }

    // -----------------------------------------------------------------------
    // National ID / External ID / UPID
    // -----------------------------------------------------------------------

    @Test
    public void setNationalId_ShouldStoreAndReturnNationalId() {
        patient.setNationalId("NAT-20240001");
        assertEquals("NAT-20240001", patient.getNationalId());
    }

    @Test
    public void setExternalId_ShouldStoreAndReturnExternalId() {
        patient.setExternalId("EXT-9876");
        assertEquals("EXT-9876", patient.getExternalId());
    }

    @Test
    public void setUpidCode_ShouldStoreAndReturnUpidCode() {
        patient.setUpidCode("UPID-001");
        assertEquals("UPID-001", patient.getUpidCode());
    }

    // -----------------------------------------------------------------------
    // Chart Number
    // -----------------------------------------------------------------------

    @Test
    public void setChartNumber_ShouldStoreAndReturnChartNumber() {
        patient.setChartNumber("CHART-2024-0099");
        assertEquals("CHART-2024-0099", patient.getChartNumber());
    }

    // -----------------------------------------------------------------------
    // FHIR UUID
    // -----------------------------------------------------------------------

    @Test
    public void setFhirUuid_ShouldStoreAndReturnUuid() {
        UUID uuid = UUID.randomUUID();
        patient.setFhirUuid(uuid);

        assertEquals(uuid, patient.getFhirUuid());
        assertEquals(uuid.toString(), patient.getFhirUuidAsString());
    }

    @Test
    public void getFhirUuidAsString_WhenUuidIsNull_ShouldReturnEmptyString() {
        // fhirUuid is null by default
        assertEquals("getFhirUuidAsString() should return empty string when fhirUuid is null", "",
                patient.getFhirUuidAsString());
    }

    // -----------------------------------------------------------------------
    // Merge State
    // -----------------------------------------------------------------------

    @Test
    public void isMerged_ShouldDefaultToFalse() {
        assertFalse(patient.getIsMerged());
    }

    @Test
    public void setIsMerged_ShouldUpdateMergeFlag() {
        patient.setIsMerged(true);
        assertTrue(patient.getIsMerged());
    }

    @Test
    public void setMergedIntoPatientId_ShouldStoreAndReturnId() {
        patient.setMergedIntoPatientId("99");
        assertEquals("99", patient.getMergedIntoPatientId());
    }

    @Test
    public void setMergeDate_ShouldStoreAndReturnTimestamp() {
        Timestamp mergeDate = Timestamp.valueOf("2024-01-01 12:00:00");
        patient.setMergeDate(mergeDate);
        assertEquals(mergeDate, patient.getMergeDate());
    }

    // -----------------------------------------------------------------------
    // Person association
    // -----------------------------------------------------------------------

    @Test
    public void setPerson_ShouldStoreAndReturnPerson() {
        Person person = new Person();
        person.setId("10");
        patient.setPerson(person);

        assertNotNull(patient.getPerson());
        assertEquals("10", patient.getPerson().getId());
    }

    // -----------------------------------------------------------------------
    // Demographic fields
    // -----------------------------------------------------------------------

    @Test
    public void demographicFields_ShouldBeSettableAndRetrievable() {
        patient.setRace("Asian");
        patient.setEthnicity("Non-Hispanic");
        patient.setBirthPlace("Nairobi");
        patient.setSchoolAttend("University");
        patient.setMedicareId("MCR-001");
        patient.setMedicaidId("MCD-001");

        assertEquals("Asian", patient.getRace());
        assertEquals("Non-Hispanic", patient.getEthnicity());
        assertEquals("Nairobi", patient.getBirthPlace());
        assertEquals("University", patient.getSchoolAttend());
        assertEquals("MCR-001", patient.getMedicareId());
        assertEquals("MCD-001", patient.getMedicaidId());
    }
}
