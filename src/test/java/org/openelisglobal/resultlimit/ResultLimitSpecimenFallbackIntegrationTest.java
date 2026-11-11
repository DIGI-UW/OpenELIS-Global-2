package org.openelisglobal.resultlimit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A reference range scoped to one specimen overrides the shared range for the
 * patients it matches, and the shared range backs every patient it does not.
 * Selecting from the specimen's rows alone left a Male patient on a specimen
 * that only has a Female override with no range at all, while the range
 * editor's coverage check reported the test fully covered.
 */
public class ResultLimitSpecimenFallbackIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95311L;
    private static final long OVERRIDE_SPECIMEN = 95312L;
    private static final long OTHER_SPECIMEN = 95313L;

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private String overrideSpecimen;
    private String otherSpecimen;

    @Before
    public void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "SpecimenFallback", "specimen fallback test", UUID.randomUUID().toString());
        insertSpecimen(OVERRIDE_SPECIMEN);
        insertSpecimen(OTHER_SPECIMEN);
        overrideSpecimen = String.valueOf(OVERRIDE_SPECIMEN);
        otherSpecimen = String.valueOf(OTHER_SPECIMEN);
        insertLimit("M", null, 12, 18);
        insertLimit("F", null, 14, 21);
        insertLimit("F", overrideSpecimen, 47, 48);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.result_limits WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id IN (?, ?)", OVERRIDE_SPECIMEN, OTHER_SPECIMEN);
        jdbc.update("DELETE FROM clinlims.localization WHERE id IN (?, ?)", OVERRIDE_SPECIMEN, OTHER_SPECIMEN);
    }

    private void insertSpecimen(long id) {
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())", id,
                "SpecimenFallback " + id);
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active,"
                        + " sort_order, name_localization_id, lastupdated) VALUES (?, ?, 'H', ?, true, 9311, ?, NOW())",
                id, "SpecimenFallback " + id, "SF" + id, id);
    }

    private void insertLimit(String gender, String sampleTypeId, double low, double high) {
        jdbc.update("INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age, gender,"
                + " low_normal, high_normal, low_valid, high_valid, low_critical, high_critical, sample_type_id,"
                + " lastupdated) VALUES (nextval('clinlims.result_limits_seq'), ?, (SELECT id FROM"
                + " clinlims.type_of_test_result WHERE test_result_type = 'N'), 0, 'Infinity', ?, ?, ?, 0, 60,"
                + " 'Infinity', 'Infinity', ?::numeric, NOW())", TEST_ID, gender, low, high, sampleTypeId);
    }

    private Patient adult(String gender) {
        Patient patient = new Patient();
        patient.setGender(gender);
        patient.setBirthDate(Timestamp.valueOf(LocalDate.now().minusYears(40).atStartOfDay()));
        return patient;
    }

    private ResultLimit select(String gender, String sampleTypeId) {
        return resultLimitService.getResultLimitForTestAndPatient(String.valueOf(TEST_ID), adult(gender), sampleTypeId);
    }

    @Test
    public void overrideAppliesToThePatientsItMatches() {
        ResultLimit limit = select("F", overrideSpecimen);
        assertNotNull(limit.getId());
        assertEquals(47.0, limit.getLowNormal(), 0.0);
        assertEquals(48.0, limit.getHighNormal(), 0.0);
    }

    @Test
    public void patientsTheOverrideDoesNotCoverKeepTheSharedRange() {
        ResultLimit limit = select("M", overrideSpecimen);
        assertNotNull("a Male patient on the overridden specimen must fall back to the shared range", limit.getId());
        assertEquals(12.0, limit.getLowNormal(), 0.0);
        assertEquals(18.0, limit.getHighNormal(), 0.0);
    }

    @Test
    public void anotherSpecimenUsesTheSharedRange() {
        assertEquals(14.0, select("F", otherSpecimen).getLowNormal(), 0.0);
        assertEquals(12.0, select("M", otherSpecimen).getLowNormal(), 0.0);
    }
}
