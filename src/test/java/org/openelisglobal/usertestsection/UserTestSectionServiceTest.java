package org.openelisglobal.usertestsection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.usertestsection.service.UserTestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

public class UserTestSectionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private UserTestSectionService userTestSectionService;
    @Autowired
    MockHttpServletRequest mockHttpServletRequest;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/user-test-section.xml");
    }

    @Test
    public void getAllUserTestSectionsByName_ShouldReturnAllTestSectionsWhoseNameMatchesTheParameterValue() {
        List<TestSection> testSections = userTestSectionService.getAllUserTestSectionsByName(mockHttpServletRequest,
                "Hematology");
        assertNotNull(testSections);
        assertEquals(2, testSections.size());
        assertEquals("301", testSections.get(1).getId());
        assertEquals("302", testSections.get(0).getId());
    }

    @Test
    public void getAllUserTests_ShouldReturnOnlyAllTheTestsThatAreNotFullySetUp() {
        List<org.openelisglobal.test.valueholder.Test> notFullySetUpTests = userTestSectionService
                .getAllUserTests(mockHttpServletRequest, false);
        assertNotNull(notFullySetUpTests);
        assertEquals(4, notFullySetUpTests.size());
        assertEquals("1", notFullySetUpTests.get(0).getId());
        assertEquals("2", notFullySetUpTests.get(3).getId());
    }

    @Test
    public void getAllUserTests_ShouldReturnOnlyAllTheTestsThatAreFullySetUp() {
        List<org.openelisglobal.test.valueholder.Test> fullySetUpTests = userTestSectionService
                .getAllUserTests(mockHttpServletRequest, true);
        assertNotNull(fullySetUpTests);
        assertEquals(2, fullySetUpTests.size());
        assertEquals("901", fullySetUpTests.get(0).getId());
        assertEquals("902", fullySetUpTests.get(1).getId());
    }

}
