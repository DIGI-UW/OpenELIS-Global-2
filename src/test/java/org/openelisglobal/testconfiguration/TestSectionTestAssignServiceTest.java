package org.openelisglobal.testconfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.service.TestSectionTestAssignService;
import org.springframework.beans.factory.annotation.Autowired;

public class TestSectionTestAssignServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestSectionTestAssignService testSectionTestAssignService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private TestService testService;

    @Autowired
    private LocalizationService localizationService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test.xml");
    }

    @Test
    public void assignTestsToSection_shouldMoveTestsAndActivateInactiveTargetSection() {
        Localization localization = new Localization();
        localization.setDescription("test unit name");
        localization.setLocalizedValue("en", "Serology");
        localization.setSysUserId("1");
        localizationService.insert(localization);

        TestSection target = new TestSection();
        target.setTestSectionName("Serology");
        target.setDescription("Serology");
        target.setIsActive("N");
        target.setSortOrderInt(Integer.MAX_VALUE);
        target.setLocalization(localization);
        target.setSysUserId("1");
        String targetId = testSectionService.insert(target);

        List<org.openelisglobal.test.valueholder.Test> updated = testSectionTestAssignService
                .assignTestsToSection(Arrays.asList("1", "2"), targetId, "1");

        assertEquals(2, updated.size());
        assertEquals(targetId, testService.get("1").getTestSection().getId());
        assertEquals(targetId, testService.get("2").getTestSection().getId());
        assertEquals("Y", testSectionService.getTestSectionById(targetId).getIsActive());
    }

    @Test
    public void assignTestsToSection_shouldSkipTestsAlreadyInTargetSection() {
        List<org.openelisglobal.test.valueholder.Test> updated = testSectionTestAssignService
                .assignTestsToSection(Arrays.asList("1"), "1", "1");

        assertTrue(updated.isEmpty());
        assertEquals("1", testService.get("1").getTestSection().getId());
    }

    @Test
    public void assignTestsToSection_shouldThrowForUnknownSection() {
        assertThrows(LIMSRuntimeException.class,
                () -> testSectionTestAssignService.assignTestsToSection(Arrays.asList("1"), "999999", "1"));
    }

    @Test
    public void assignTestsToSection_shouldThrowForUnknownTest() {
        assertThrows(LIMSRuntimeException.class,
                () -> testSectionTestAssignService.assignTestsToSection(Arrays.asList("999999"), "1", "1"));
    }
}
