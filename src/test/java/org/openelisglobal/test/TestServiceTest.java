package org.openelisglobal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;

public class TestServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestService testService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test.xml");
    }

    @Test
    public void getAllTests_ShouldReturnAllTests() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getAllTests(false);
        assertEquals("Blood Test", tests.get(0).getDescription());
        assertEquals("Urine Test", tests.get(1).getDescription());
    }

    @Test
    public void getData_shouldReturnDataGivenTest() {
        org.openelisglobal.test.valueholder.Test test1 = testService.get("1");
        testService.getData(test1);
        assertEquals("Blood Test", test1.getDescription());
        assertEquals("therapy", test1.getMethod().getMethodName());

        org.openelisglobal.test.valueholder.Test test2 = testService.get("2");
        testService.getData(test2);
        assertEquals("Urine Test", test2.getDescription());
        assertEquals("imagining", test2.getMethod().getMethodName());
    }

    @Test
    public void getActiveTestById_ShouldReturnActiveTest() {
        org.openelisglobal.test.valueholder.Test test = testService.getActiveTestById(1);
        assertEquals("Y", test.getIsActive());
    }

    @Test
    public void getTotalTestCount_ShouldReturnTotalTestCount() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getAll();
        int totalTestCount = testService.getTotalTestCount();
        assertEquals(tests.size(), totalTestCount);
    }

    @Test
    public void getAllActiveTests_ShouldReturnAllActiveTests() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getAllActiveTests(false);
        tests.forEach(test -> assertEquals("Y", test.getIsActive()));
    }

    @Test
    public void getAllActiveTests_ShouldReturnAllActiveTestsFullySetup() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getAllActiveTests(false);
        tests.forEach(test -> assertEquals("Y", test.getIsActive()));
    }

    @Test
    public void getTestsByTestSectionIds_ShouldReturnTestsByTestSectionIds() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getTestsByTestSectionIds(List.of(1, 2));
        assertEquals(2, tests.size());
    }

    @Test
    public void getTestsByTestSectionId_shouldReturnTestGivenTestSectionId() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getTestsByTestSectionId("1");
        assertEquals(1, tests.size());
        assertEquals("Blood Test", tests.get(0).getDescription());
        List<org.openelisglobal.test.valueholder.Test> tests2 = testService.getTestsByTestSectionId("2");
        assertEquals(1, tests2.size());
        assertEquals("Urine Test", tests2.get(0).getDescription());
    }

    @Test
    public void deactivateAllTests_ShouldDeactivateAllTests() {
        testService.deactivateAllTests();
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getAll();
        tests.forEach(test -> assertEquals("N", test.getIsActive()));
    }

    @Test
    public void getPageOfTests_shouldReturnPageOfTests() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getPageOfTests(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(tests.size() <= expectedPages);

    }

    @Test
    public void getTestByGUID_shouldReturnTestGivenGUID() {
        org.openelisglobal.test.valueholder.Test test = testService.getTestByGUID("abc-123");
        assertEquals("Blood Test", test.getDescription());
    }

    @Test
    public void getTestById_shouldReturnTestGivenId() {
        org.openelisglobal.test.valueholder.Test test2 = testService.getTestById("1");
        assertEquals("Blood Test", test2.getDescription());
        org.openelisglobal.test.valueholder.Test test = testService.getTestById("2");
        assertEquals("Urine Test", test.getDescription());

    }

    @Test
    public void getAllActiveOrderableTests() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getAllActiveOrderableTests();
        tests.forEach(test -> {
            assertEquals("Y", test.getIsActive());
            assertTrue(test.getOrderable());
        });

    }

    @Test
    public void getIsReportable() {
        org.openelisglobal.test.valueholder.Test test1 = testService.get("1");
        boolean isReportable1 = testService.isReportable(test1);
        assertFalse(isReportable1);
        org.openelisglobal.test.valueholder.Test test2 = testService.get("2");
        boolean isReportable2 = testService.isReportable(test2);
        assertTrue(isReportable2);

    }

    @Test
    public void getTestMethodName() {
        org.openelisglobal.test.valueholder.Test test1 = testService.get("1");
        String methodName1 = testService.getTestMethodName(test1);
        assertEquals(methodName1, test1.getMethod().getMethodName());
        org.openelisglobal.test.valueholder.Test test2 = testService.get("2");
        String methodName2 = testService.getTestMethodName(test2);
        assertEquals(methodName2, test2.getMethod().getMethodName());
    }

    @Test
    public void getSortOrder() {
        org.openelisglobal.test.valueholder.Test test1 = testService.get("1");
        String sortOrder1 = testService.getSortOrder(test1);
        assertEquals(sortOrder1, test1.getSortOrder());
        org.openelisglobal.test.valueholder.Test test2 = testService.get("2");
        String sortOrder2 = testService.getSortOrder(test2);
        assertEquals(sortOrder2, test2.getSortOrder());
    }

    @Test
    public void getActiveTestsByLoinc() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getActiveTestsByLoinc("123456");
        tests.forEach(test -> {
            assertEquals("Y", test.getIsActive());
        });
    }

    @Test
    public void getTestsByLoinicCode() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getTestsByLoincCode("123456");
        assertTrue(tests.size() > 0);

    }

    @Test
    public void getTbTests() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getTbTest();
        assertEquals(1, tests.size());
        assertEquals("Blood Test", tests.get(0).getDescription());
    }

    @Test
    public void getTotalTestCount() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getAll();
        int totalCount = testService.getTotalTestCount();
        assertEquals(tests.size(), totalCount);

    }

    @Test
    public void getTestByDescription() {
        org.openelisglobal.test.valueholder.Test test = testService.getTestByDescription("Blood Test");
        assertEquals("Blood Test", test.getDescription());
    }

    @Test
    public void activateTestsAndDeactivateOthers_ShouldActivateTestsAndDeactivateOthers() {
        testService.activateTestsAndDeactivateOthers(List.of("1", "2"));
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getAll();
        assertEquals(2, tests.size());
        tests.forEach(test -> {
            assertEquals("N", test.getIsActive());
        });
    }

    @Test
    public void getTestsByTestSection() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getTestsByTestSection("1");
        assertEquals(1, tests.size());
        assertEquals("Blood Test", tests.get(0).getDescription());
        List<org.openelisglobal.test.valueholder.Test> tests2 = testService.getTestsByTestSection("2");
        assertEquals(1, tests2.size());
        assertEquals("Urine Test", tests2.get(0).getDescription());
    }

    @Test
    public void getAllOrderBy() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getAllOrderBy("description");
        assertEquals(2, tests.size());
        assertEquals("Blood Test", tests.get(0).getDescription());
        assertEquals("Urine Test", tests.get(1).getDescription());
    }

    @Test
    public void isTestFullySetup() {
        org.openelisglobal.test.valueholder.Test test = testService.get("1");
        boolean isFullySetup = testService.isTestFullySetup(test);
        assertFalse(isFullySetup);
    }

    @Test
    public void getNextAvailableSortOrderByTestSection() {
        org.openelisglobal.test.valueholder.Test test = testService.get("1");
        int sortOrder = testService.getNextAvailableSortOrderByTestSection(test);
        assertTrue(sortOrder > 0);
    }

    @Test
    public void getTotalSearchedTestCount() {
        String searchString = "Blood Test";
        int totalSearchedTestCount = testService.getTotalSearchedTestCount(searchString);
        assertTrue(totalSearchedTestCount > 0);
    }

    @Test
    public void getUOM() {
        org.openelisglobal.test.valueholder.Test test = testService.get("1");
        String uom = testService.getUOM(test, false);
        assertEquals("mg/dL", uom);
    }

    @Test
    public void getActiveTestsByPanelName() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getActiveTestsByPanel("TB");
        assertEquals(1, tests.size());
        assertEquals("Blood Test", tests.get(0).getDescription());
    }

    @Test
    public void getTestByName() {
        org.openelisglobal.test.valueholder.Test test = testService.getTestByName("TB");
        assertEquals("Blood Test", test.getDescription());
    }

    @Test
    public void getActiveTestsByName() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getActiveTestsByName("TB");
        assertEquals(1, tests.size());
        assertEquals("Y", tests.get(0).getIsActive());
    }

    @Test
    public void getActiveTestByLocalizedName() {
        org.openelisglobal.test.valueholder.Test test = testService.getActiveTestByLocalizedName("TB", Locale.ENGLISH);
        assertEquals("Y", test.getIsActive());
        assertEquals("Blood Test", test.getDescription());
    }

    @Test
    public void getTestByLocalizedName() {
        org.openelisglobal.test.valueholder.Test test = testService.getTestByLocalizedName("TB", Locale.ENGLISH);
        assertEquals("Blood Test", test.getDescription());
        org.openelisglobal.test.valueholder.Test test2 = testService.getTestByLocalizedName("Urinalysis",
                Locale.FRENCH);
        assertEquals("Urine Test", test2.getDescription());
    }

}
