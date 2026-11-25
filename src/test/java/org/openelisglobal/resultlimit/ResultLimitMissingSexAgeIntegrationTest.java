package org.openelisglobal.resultlimit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimit.valueholder.ResultLimitSelection;
import org.openelisglobal.resultlimit.valueholder.ResultLimitSelection.RangeNotAppliedReason;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A patient with no recorded sex or birth date gets only a range that does not
 * depend on the missing value. Borrowing a sex- or age-specific range (the male
 * haemoglobin range for a woman whose sex was not recorded) is never done; when
 * nothing applies the selection says which missing value mattered.
 */
public class ResultLimitMissingSexAgeIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long SEX_TEST_ID = 95321L;
    private static final long AGE_TEST_ID = 95322L;
    private static final long MIXED_TEST_ID = 95323L;
    private static final long NEUTRAL_TEST_ID = 95324L;
    private static final long BOTH_TEST_ID = 95325L;
    private static final double YEAR_DAYS = 365.25;

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        insertTest(SEX_TEST_ID);
        insertLimit(SEX_TEST_ID, "M", 0, Double.POSITIVE_INFINITY, 13, 17);
        insertLimit(SEX_TEST_ID, "F", 0, Double.POSITIVE_INFINITY, 12, 15);

        insertTest(AGE_TEST_ID);
        insertLimit(AGE_TEST_ID, null, 0, 12 * YEAR_DAYS, 20, 30);
        insertLimit(AGE_TEST_ID, null, 12 * YEAR_DAYS, Double.POSITIVE_INFINITY, 40, 50);

        insertTest(MIXED_TEST_ID);
        insertLimit(MIXED_TEST_ID, "M", 18 * YEAR_DAYS, Double.POSITIVE_INFINITY, 13, 17);
        insertLimit(MIXED_TEST_ID, null, 0, 18 * YEAR_DAYS, 11, 14);
        insertLimit(MIXED_TEST_ID, null, 0, Double.POSITIVE_INFINITY, 1, 99);

        insertTest(NEUTRAL_TEST_ID);
        insertLimit(NEUTRAL_TEST_ID, "F", 60 * YEAR_DAYS, Double.POSITIVE_INFINITY, 5, 6);

        insertTest(BOTH_TEST_ID);
        insertLimit(BOTH_TEST_ID, "M", 0, Double.POSITIVE_INFINITY, 13, 17);
        insertLimit(BOTH_TEST_ID, null, 0, 12 * YEAR_DAYS, 20, 30);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        for (long id : new long[] { SEX_TEST_ID, AGE_TEST_ID, MIXED_TEST_ID, NEUTRAL_TEST_ID, BOTH_TEST_ID }) {
            jdbc.update("DELETE FROM clinlims.result_limits WHERE test_id = ?", id);
            jdbc.update("DELETE FROM clinlims.test WHERE id = ?", id);
        }
    }

    private void insertTest(long id) {
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                id, "MissingSexAge" + id, "missing sex age " + id, UUID.randomUUID().toString());
    }

    private void insertLimit(long testId, String gender, double minAge, double maxAge, double low, double high) {
        jdbc.update("INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age, gender,"
                + " low_normal, high_normal, low_valid, high_valid, low_critical, high_critical, lastupdated)"
                + " VALUES (nextval('clinlims.result_limits_seq'), ?, (SELECT id FROM clinlims.type_of_test_result"
                + " WHERE test_result_type = 'N'), ?, ?, ?, ?, ?, 0, 'Infinity', '-Infinity', 'Infinity', NOW())",
                testId, minAge, maxAge, gender, low, high);
    }

    private Patient patient(String gender, Integer ageYears) {
        Patient patient = new Patient();
        patient.setGender(gender);
        if (ageYears != null) {
            patient.setBirthDate(Timestamp.valueOf(LocalDate.now().minusYears(ageYears).atStartOfDay()));
        }
        return patient;
    }

    private ResultLimitSelection select(long testId, Patient patient) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(String.valueOf(testId));
        Analysis analysis = new Analysis();
        analysis.setTest(test);
        return resultLimitService.selectResultLimitForResult(analysis, null, patient, null);
    }

    private ResultLimit limit(long testId, Patient patient) {
        return resultLimitService.getResultLimitForTestAndPatient(String.valueOf(testId), patient);
    }

    @Test
    public void recordedSexAndAgeStillSelectTheMatchingRange() {
        assertEquals(12.0, limit(SEX_TEST_ID, patient("F", 30)).getLowNormal(), 0.0);
        assertEquals(40.0, limit(AGE_TEST_ID, patient("M", 30)).getLowNormal(), 0.0);
        assertFalse(select(SEX_TEST_ID, patient("F", 30)).isRangeNotApplied());
    }

    @Test
    public void missingSexNeverBorrowsASexSpecificRange() {
        ResultLimitSelection selection = select(SEX_TEST_ID, patient(null, 30));
        assertFalse("a range for one sex must not apply to a patient whose sex was not recorded",
                selection.hasResultLimit());
        assertEquals(RangeNotAppliedReason.SEX_NOT_RECORDED, selection.getReason());
        assertNull(limit(SEX_TEST_ID, patient(null, 30)).getId());
    }

    @Test
    public void missingAgeNeverBorrowsAnAgeBandedRange() {
        ResultLimitSelection selection = select(AGE_TEST_ID, patient("F", null));
        assertFalse(selection.hasResultLimit());
        assertEquals(RangeNotAppliedReason.AGE_NOT_RECORDED, selection.getReason());
    }

    @Test
    public void missingBothNamesBoth() {
        assertEquals(RangeNotAppliedReason.SEX_AND_AGE_NOT_RECORDED,
                select(BOTH_TEST_ID, patient(null, null)).getReason());
        assertEquals(RangeNotAppliedReason.SEX_AND_AGE_NOT_RECORDED, select(SEX_TEST_ID, patient(null, null))
                .combinedWith(select(AGE_TEST_ID, patient(null, null))).getReason());
        assertEquals(RangeNotAppliedReason.SEX_NOT_RECORDED, select(SEX_TEST_ID, patient(null, null)).getReason());
    }

    @Test
    public void aRangeThatDoesNotDependOnTheMissingValueStillApplies() {
        ResultLimitSelection sexMissing = select(MIXED_TEST_ID, patient(null, 30));
        assertTrue(sexMissing.hasResultLimit());
        assertEquals("an adult with no sex gets the any-sex any-age range, not the male adult one", 1.0,
                sexMissing.getResultLimit().getLowNormal(), 0.0);
        assertEquals("a child with no sex gets the any-sex child range", 11.0,
                select(MIXED_TEST_ID, patient(null, 10)).getResultLimit().getLowNormal(), 0.0);
        assertEquals("a man with no birth date gets the any-age range", 1.0,
                select(MIXED_TEST_ID, patient("M", null)).getResultLimit().getLowNormal(), 0.0);
    }

    @Test
    public void aMissingValueThatWouldNotHaveMatteredGivesNoReason() {
        ResultLimitSelection selection = select(NEUTRAL_TEST_ID, patient(null, 30));
        assertFalse(selection.hasResultLimit());
        assertFalse("the only range is for women over 60, so sex did not matter for a 30-year-old",
                selection.isRangeNotApplied());
    }

    @Test
    public void noPatientKeepsTheNoPatientBehaviour() {
        ResultLimitSelection selection = select(SEX_TEST_ID, null);
        assertFalse(selection.hasResultLimit());
        assertFalse(selection.isRangeNotApplied());
    }

    @Test
    public void orderedTestsNameOnlyTheTestsWhoseRangeCannotApply() {
        java.util.List<String> names = resultLimitService
                .getTestNamesWithRangeNotApplied(
                        java.util.List.of(new ResultLimitService.OrderedTest(String.valueOf(SEX_TEST_ID), null),
                                new ResultLimitService.OrderedTest(String.valueOf(MIXED_TEST_ID), null),
                                new ResultLimitService.OrderedTest(String.valueOf(SEX_TEST_ID), null)),
                        patient(null, 30));
        assertEquals(1, names.size());
        assertTrue(resultLimitService.getTestNamesWithRangeNotApplied(
                java.util.List.of(new ResultLimitService.OrderedTest(String.valueOf(SEX_TEST_ID), null)),
                patient("F", 30)).isEmpty());
    }
}
