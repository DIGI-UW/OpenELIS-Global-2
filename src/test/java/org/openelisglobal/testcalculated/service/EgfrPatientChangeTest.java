package org.openelisglobal.testcalculated.service;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Saving a patient whose sex or birth date changed recalculates the eGFR that
 * was calculated for them, through the real services and database.
 */
public class EgfrPatientChangeTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientService patientService;
    @Autowired
    private ResultService resultService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/egfr-patient-change.xml");
        ensureReferenceTables("PATIENT", "PERSON", "ANALYSIS", "RESULT", "NOTE");
    }

    private void savePatient(String gender, LocalDateTime birthDate, String nationalId) {
        // loaded outside the saving transaction, as the patient pages do
        Patient patient = patientService.get("1");
        patient.setGender(gender);
        patient.setBirthDate(Timestamp.valueOf(birthDate));
        patient.setNationalId(nationalId);
        patient.setSysUserId("1");
        patientService.update(patient);
    }

    private String egfr() {
        return resultService.get("2").getValue();
    }

    private int notes(String startingWith) {
        return jdbcTemplate.queryForObject("select count(*) from clinlims.note where reference_id = 2 and text like ?",
                Integer.class, startingWith + "%");
    }

    @Test
    public void aSexCorrectionRecalculatesTheCalculatedEgfr() {
        savePatient("M", LocalDateTime.of(1999, 3, 1, 0, 0), null);

        assertEquals("134", egfr());
        assertEquals(1, notes("Recalculated after patient sex/birth date changed: 125 → 134 mL/min/1.73 m2."));
    }

    @Test
    public void aBirthDateCorrectionRecalculatesTheCalculatedEgfr() {
        savePatient("F", LocalDateTime.of(1959, 3, 1, 0, 0), null);

        assertEquals("98", egfr());
        assertEquals(1, notes("Recalculated after patient sex/birth date changed: 125 → 98 mL/min/1.73 m2."));
    }

    @Test
    public void anotherPatientEditLeavesTheEgfrAlone() {
        // a stale value proves that nothing recalculated it
        jdbcTemplate.update("update clinlims.result set value = '120' where id = 2");

        savePatient("F", LocalDateTime.of(1999, 3, 1, 0, 0), "NEW-ID");

        assertEquals("120", egfr());
        assertEquals(0, notes("Recalculated"));
    }

    @Test
    public void anEgfrEnteredByHandIsNotTouched() {
        jdbcTemplate.update("update clinlims.analysis set result_calculated = false where id = 2");

        savePatient("M", LocalDateTime.of(1999, 3, 1, 0, 0), null);

        assertEquals("125", egfr());
        assertEquals(0, notes(""));
    }
}
