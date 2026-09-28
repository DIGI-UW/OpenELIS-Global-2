package org.openelisglobal.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.test.service.EffectiveTestStatusService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-189 (M4): the effectiveActive rule.
 *
 * <p>
 * Before this, a lab unit's active flag did not participate in orderability at
 * all — the ordering path filtered on the test's own flag, so deactivating a
 * unit was purely presentational (QA LU-W-11/LU-W-12, measured 2026-09-02).
 *
 * <p>
 * Fixtures: test 1 is active and orderable in section 1, which is active. Each
 * case flips one input and asserts the derived answer, so a rule that ignored
 * the lab unit — or one that blocked everything — fails.
 */
public class EffectiveTestStatusServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private EffectiveTestStatusService effectiveTestStatusService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestSectionService testSectionService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test.xml");
    }

    /**
     * Flips the lab unit's status in the database directly. Going through
     * TestSectionService here would drag the section's Localization graph into the
     * flush and fail on an unsaved transient — irrelevant to what these cases are
     * about, which is how the status is *read*.
     */
    private void setLabUnitActive(String sectionId, boolean active) {
        jdbcTemplate.update("update clinlims.test_section set is_active = ? where id = ?", active ? "Y" : "N",
                Integer.valueOf(sectionId));
        // Deliberately NOT calling testSectionService.refreshNames() here.
        // isEffectivelyActive reads only TestSection.isActive (via
        // testSectionService.get), never the localized-name cache, so the
        // refresh is unnecessary — and it is process-wide state: populating it
        // leaked into TestSectionServiceTest.getUserLocalizedTesSectionName,
        // which asserts the map is still empty. That surfaced only in a
        // full-suite run, where class order puts this test first.
    }

    @org.junit.Test
    public void activeTestInActiveLabUnit_isEffectivelyActive() {
        // The positive control. Without it, a rule that returned false for
        // everything would pass every other case in this class.
        Assert.assertTrue(effectiveTestStatusService.isEffectivelyActive(testService.getTestById("1")));
    }

    @Test
    public void activeTestInDeactivatedLabUnit_isNotEffectivelyActive() {
        // The defect itself: the test is untouched and still active, but its
        // lab unit is switched off, so no new analysis may be created for it.
        setLabUnitActive("1", false);

        Assert.assertFalse(effectiveTestStatusService.isEffectivelyActive(testService.getTestById("1")));
    }

    @Test
    public void deactivatingALabUnitDoesNotMutateTheTestsOwnFlags() {
        setLabUnitActive("1", false);

        // Derived, never written — this is what makes reactivation lossless.
        org.openelisglobal.test.valueholder.Test test = testService.getTestById("1");
        Assert.assertEquals("the test's own active flag must be untouched", "Y", test.getIsActive());
        Assert.assertTrue("the test's own orderable flag must be untouched", test.getOrderable());
    }

    @Test
    public void reactivatingTheLabUnitRestoresOrderability() {
        setLabUnitActive("1", false);
        Assert.assertFalse(effectiveTestStatusService.isEffectivelyActive(testService.getTestById("1")));

        // No restore step: the test returns to what its own config says.
        setLabUnitActive("1", true);

        Assert.assertTrue(effectiveTestStatusService.isEffectivelyActive(testService.getTestById("1")));
    }

    @Test
    public void inactiveTestInActiveLabUnit_isNotEffectivelyActive() {
        // Direct SQL for the same reason as setLabUnitActive: updating a Test
        // through the service pulls its Localization graph into the flush.
        jdbcTemplate.update("update clinlims.test set is_active = 'N' where id = 1");

        // The test's own flag still governs — the lab unit rule is additional,
        // not a replacement.
        Assert.assertFalse(effectiveTestStatusService.isEffectivelyActive(testService.getTestById("1")));
    }

    @Test
    public void effectivelyOrderable_followsEffectivelyActive() {
        Assert.assertTrue(effectiveTestStatusService.isEffectivelyOrderable(testService.getTestById("1")));

        setLabUnitActive("1", false);

        // orderable is the manual-picker flag and rides on top of active, so a
        // deactivated unit removes the test from the picker too.
        Assert.assertFalse(effectiveTestStatusService.isEffectivelyOrderable(testService.getTestById("1")));
    }

    @Test
    public void isLabUnitInactive_isNullSafe() {
        // A test with no lab unit must never be blocked by a rule about lab
        // units, so a null section is not "inactive".
        Assert.assertFalse(effectiveTestStatusService.isLabUnitInactive(null));
    }

    @Test
    public void nullTest_isNotEffectivelyActive() {
        Assert.assertFalse(effectiveTestStatusService.isEffectivelyActive(null));
    }
}
