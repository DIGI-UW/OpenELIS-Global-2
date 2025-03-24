package org.openelisglobal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
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

    // @Test
    public void testDataBaseData() {
        List<TestSection> testSections = testSectionService.getAll();
        testSections.forEach(testSection -> {
            System.out.print(testSection.getTestSectionName() + " ");
        });
    }

    @Test
    public void getData_shouldReturnDataGivenTestSection() {
        TestSection testSection1 = testSectionService.get("1");
        testSectionService.getData(testSection1);
        assertEquals("SectionName1", testSection1.getTestSectionName());
        assertEquals("SectionDescription1", testSection1.getDescription());
    }

    @Test
    public void getTestSections_shouldReturnTestSectionsGivenFilter() {
        String filter = "Section";
        List<TestSection> testSections = testSectionService.getTestSections(filter);
        assertTrue(testSections.size() > 0);
    }

    @Test
    public void getTestSectionByName() {
        TestSection testSection1 = testSectionService.getTestSectionByName("SectionName1");
        assertEquals("SectionName1", testSection1.getTestSectionName());
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
        assertEquals("SectionName1", testSections.get(0).getTestSectionName());
        assertEquals("SectionName2", testSections.get(1).getTestSectionName());
    }

    @Test
    public void getTestSectionById() {
        TestSection testSection1 = testSectionService.getTestSectionById("1");
        assertEquals("SectionName1", testSection1.getTestSectionName());
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
