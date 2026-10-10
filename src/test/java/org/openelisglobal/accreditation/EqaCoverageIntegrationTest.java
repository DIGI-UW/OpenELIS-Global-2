package org.openelisglobal.accreditation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.accreditation.dto.EqaCoverageView;

/**
 * OGC-686 — accredited scope vs. live EQA cover, against a real DB.
 *
 * <p>
 * The question is ISO 15189 §7.7's: is every test the lab claims as accredited
 * taking part in external quality assessment? The load-bearing cases are the
 * ones a naive join gets wrong — a test covered only through an enrolled
 * <em>panel</em> is covered, and a test whose EQA enrollment has been
 * deactivated is not.
 */
public class EqaCoverageIntegrationTest extends AccreditationIntegrationTestBase {

    private static final long EQA_ENROLLMENT_ID = 9901L;
    private static final long EQA_MAP_ID = 9911L;
    private static final long PANEL_ID = 9921L;
    private static final long PANEL_ITEM_ID = 9931L;
    private static final long LOCALIZATION_ID = 9941L;

    /** This suite also seeds the EQA enrollment the coverage join reads. */
    @Override
    protected void clean() {
        super.clean();
        jdbc.update("DELETE FROM clinlims.eqa_lab_enrollment_test_map WHERE id = ?", EQA_MAP_ID);
        jdbc.update("DELETE FROM clinlims.eqa_lab_program_enrollment WHERE id = ?", EQA_ENROLLMENT_ID);
        jdbc.update("DELETE FROM clinlims.panel_item WHERE id = ?", PANEL_ITEM_ID);
        jdbc.update("DELETE FROM clinlims.panel WHERE id = ?", PANEL_ID);
        jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", LOCALIZATION_ID);
    }

    @Test
    public void withoutAnyEqaEnrollment_everyAccreditedTestIsAGap() {
        Long body = createBodyWithTests(TEST_GLUCOSE, TEST_SODIUM);

        EqaCoverageView row = onlyRow();
        assertEquals(body, row.accreditingBodyId);
        assertEquals(2, row.enrolledTestCount);
        assertEquals(0, row.coveredTestCount);
        assertEquals(2, row.gaps.size());
    }

    @Test
    public void directlyEnrolledTestIsCovered() {
        createBodyWithTests(TEST_GLUCOSE, TEST_SODIUM);
        giveEqaEnrollment(true);
        mapTestToEqa(TEST_GLUCOSE);

        EqaCoverageView row = onlyRow();
        assertEquals(1, row.coveredTestCount);
        assertEquals(1, row.gaps.size());
        assertEquals(TEST_SODIUM, row.gaps.get(0).testId);
        // The gap names the test, because "one gap" is not an actionable answer.
        assertTrue(row.gaps.get(0).testName != null && !row.gaps.get(0).testName.isBlank());
    }

    @Test
    public void testCoveredThroughAnEnrolledPanelIsCovered() {
        createBodyWithTests(TEST_GLUCOSE);
        giveEqaEnrollment(true);
        mapPanelToEqa(TEST_GLUCOSE);

        EqaCoverageView row = onlyRow();
        assertEquals("a panel enrollment covers each test in it", 1, row.coveredTestCount);
        assertTrue(row.gaps.isEmpty());
    }

    @Test
    public void deactivatedEqaEnrollmentDoesNotCount() {
        createBodyWithTests(TEST_GLUCOSE);
        giveEqaEnrollment(false);
        mapTestToEqa(TEST_GLUCOSE);

        EqaCoverageView row = onlyRow();
        assertEquals("a lapsed EQA enrollment is not current cover", 0, row.coveredTestCount);
        assertEquals(1, row.gaps.size());
    }

    @Test
    public void bodyWithNoAccreditedTestsHasNothingToAnswerFor() {
        accreditingBodyService.createBody(body("ISO15189", "ISO 15189", LocalDate.now().plusYears(1)), USER);

        assertTrue(testAccreditationService.getEqaCoverage().isEmpty());
    }

    // ---- helpers ----

    private EqaCoverageView onlyRow() {
        List<EqaCoverageView> rows = testAccreditationService.getEqaCoverage();
        assertEquals(1, rows.size());
        return rows.get(0);
    }

    private Long createBodyWithTests(String... testIds) {
        Long bodyId = accreditingBodyService
                .createBody(body("ISO15189", "ISO 15189", LocalDate.now().plusYears(1)), USER).getId();
        for (String testId : testIds) {
            testAccreditationService.enroll(testId, bodyId, null, USER);
        }
        return bodyId;
    }

    private void giveEqaEnrollment(boolean active) {
        jdbc.update(
                "INSERT INTO clinlims.eqa_lab_program_enrollment"
                        + " (id, program_name, provider, is_active, created_date, sys_user_id, lastupdated)"
                        + " VALUES (?, 'Coverage check', 'Test provider', ?, now(), ?, now())",
                EQA_ENROLLMENT_ID, active, USER);
    }

    private void mapTestToEqa(String testId) {
        jdbc.update(
                "INSERT INTO clinlims.eqa_lab_enrollment_test_map"
                        + " (id, enrollment_id, test_id, sys_user_id, lastupdated) VALUES (?, ?, ?, ?, now())",
                EQA_MAP_ID, EQA_ENROLLMENT_ID, Long.valueOf(testId), USER);
    }

    private void mapPanelToEqa(String testId) {
        jdbc.update("INSERT INTO clinlims.localization (id, description) VALUES (?, 'EQA coverage panel')",
                LOCALIZATION_ID);
        jdbc.update("INSERT INTO clinlims.panel (id, name, description, is_active, name_localization_id)"
                + " VALUES (?, 'EQA panel', 'EQA coverage panel', 'Y', ?)", PANEL_ID, LOCALIZATION_ID);
        jdbc.update("INSERT INTO clinlims.panel_item (id, panel_id, test_id) VALUES (?, ?, ?)", PANEL_ITEM_ID, PANEL_ID,
                Long.valueOf(testId));
        jdbc.update(
                "INSERT INTO clinlims.eqa_lab_enrollment_test_map"
                        + " (id, enrollment_id, panel_id, sys_user_id, lastupdated) VALUES (?, ?, ?, ?, now())",
                EQA_MAP_ID, EQA_ENROLLMENT_ID, PANEL_ID, USER);
    }

}
