package org.openelisglobal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;

public class TestSectionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestSectionService testSectionService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test.xml");
    }

    public void testDatabaseData() {
        List<TestSection> testSections = testSectionService.getAll();

        Assert.assertFalse("The test_sections table should not be empty!", testSections.isEmpty());

        for (TestSection testSection : testSections) {
            Assert.assertNotNull("Test section name should not be null", testSection.getTestSectionName());
        }
    }

    @Test
    public void getData_shouldReturnDataGivenTestSection() {
        TestSection testSection1 = testSectionService.get("1");
        testSectionService.getData(testSection1);
        assertEquals("TB", testSection1.getTestSectionName());
        assertEquals("SectionDescription1", testSection1.getDescription());
    }

    @Test
    public void getTestSections_shouldReturnTestSectionsGivenFilter() {
        String filter = "T";
        List<TestSection> testSections = testSectionService.getTestSections(filter);
        assertTrue(testSections.size() > 0);
    }

    @Test
    public void getTestSectionByName() {
        TestSection testSection1 = testSectionService.getTestSectionByName("TB");
        assertEquals("TB", testSection1.getTestSectionName());
        assertEquals("SectionDescription1", testSection1.getDescription());
    }

    @Test
    public void getPagesOfTestSections() {
        List<TestSection> testSections = testSectionService.getPageOfTestSections(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue(("page.defaultPageSize")));
        assertTrue(testSections.size() <= expectedPages);

    }

    @Test
    public void getTotalTestSectionCount() {
        int totalTestSections = testSectionService.getTotalTestSectionCount();
        List<TestSection> testSections = testSectionService.getAll();
        assertEquals(testSections.size(), totalTestSections);
    }

    @Test
    public void getAllTestSections() {
        List<TestSection> testSections = testSectionService.getAllTestSections();
        assertEquals("TB", testSections.get(0).getTestSectionName());
        assertEquals("TestSection2", testSections.get(1).getTestSectionName());
    }

    @Test
    public void getTestSectionById() {
        TestSection testSection1 = testSectionService.getTestSectionById("1");
        assertEquals("TB", testSection1.getTestSectionName());
        assertEquals("SectionDescription1", testSection1.getDescription());

    }

    @Test
    public void getAllActiveTestSections() {
        List<TestSection> testSections = testSectionService.getAllActiveTestSections();
        testSections.forEach(testSection -> {
            assertEquals("Y", testSection.getIsActive());
        });

    }

    @Test
    public void getAllInActiveTestSections() {
        List<TestSection> testSections = testSectionService.getAllInActiveTestSections();
        testSections.forEach(testSection -> {
            assertEquals("N", testSection.getIsActive());
        });

    }

    @Test
    public void getTestsInSection() {
        List<TestSection> testSections = testSectionService.getAll();
        List<org.openelisglobal.test.valueholder.Test> tests = testSectionService
                .getTestsInSection(testSections.get(0).getId());
        assertTrue(tests.size() > 0);
    }

    @Test
    public void getUserLocalizedTesSectionName() {
        TestSection testSection1 = testSectionService.get("1");
        String localizedName = testSectionService.getUserLocalizedTesSectionName(testSection1);
        assertEquals("", localizedName);
    }

}
