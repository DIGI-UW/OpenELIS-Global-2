package org.openelisglobal.testResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;

public class TestResultServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestService testService;

    @Autowired
    private TestResultService testResultService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test-result.xml");
    }

    @Test
    public void getData_shouldDataForTestResult() {
        TestResult testResult = testResultService.get("1");
        testResultService.getData(testResult);
        assertEquals("1", testResult.getId());
        assertTrue(testResult.getIsQuantifiable());
        assertTrue(testResult.getIsActive());
    }

    @Test
    public void getTestResultById_shouldReturnTestResult() {
        TestResult testResult = testResultService.get("1");
        TestResult testResultById = testResultService.getTestResultById(testResult);
        assertEquals("1", testResultById.getId());
        assertTrue(testResultById.getIsQuantifiable());
        assertTrue(testResultById.getIsActive());
    }

    @Test
    public void getAllActiveTestResultsPerTest_shouldReturnAllActiveTestResultsPerTest() {
        org.openelisglobal.test.valueholder.Test test = testService.get("1");
        List<TestResult> testResults = testResultService.getAllActiveTestResultsPerTest(test);
        testResults.forEach(testResult -> {
            assertTrue(testResult.getIsActive());
        });

    }

    @Test
    public void getPageOfTestResults_shouldReturnPageOfTestResults() {
        List<TestResult> testResults = testResultService.getPageOfTestResults(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testResults.size() <= expectedPages);

    }

    @Test
    public void getAllTestResults_shouldReturnAllTestResults() {
        List<TestResult> testResults = testResultService.getAllTestResults();
        assertEquals(2, testResults.size());
        assertEquals("1", testResults.get(0).getId());
        assertEquals("2", testResults.get(1).getId());
    }

    @Test
    public void getAllSortedTestResults_shouldReturnAllSortedTestResults() {
        List<TestResult> testResults = testResultService.getAllSortedTestResults();
        assertEquals(2, testResults.size());
        assertEquals("1", testResults.get(0).getId());
        assertEquals("2", testResults.get(1).getId());
    }

    @Test
    public void getActiveTestResultsByTest_Id_shouldReturnActiveTestResultsByTestId() {
        List<TestResult> testResults = testResultService.getActiveTestResultsByTest("1");
        testResults.forEach(testResult -> {
            assertTrue(testResult.getIsActive());
        });
    }

    @Test
    public void getAllMatchingTestResults_shouldReturnAllMatchingTestResults() {
        List<TestResult> testResults = testResultService.getAllMatching("contLevel", "High");
        assertEquals(2, testResults.size());
        assertEquals("1", testResults.get(0).getId());
        assertEquals("2", testResults.get(1).getId());
    }

    @Test
    public void getAllMatchingGivenMaps_shouldReturnAllMatchingGivenMaps() {
        Map<String, Object> map = Map.of("contLevel", "High");
        List<TestResult> testResults = testResultService.getAllMatching(map);
        assertEquals(2, testResults.size());
    }

    @Test
    public void getAllMatchingOrdered_By_shouldReturnAllMatchingOrderedBy() {
        List<TestResult> testResults = testResultService.getAllMatchingOrdered("contLevel", "High", "id", true);
        assertEquals(2, testResults.size());
        assertEquals("2", testResults.get(0).getId());
        assertEquals("1", testResults.get(1).getId());
    }

    @Test
    public void getAllMathchingOrderedGivenMaps_shouldReturnAllMatchingOrderedGivenMaps() {
        Map<String, Object> map = Map.of("contLevel", "High");
        List<TestResult> testResults = testResultService.getAllMatchingOrdered(map, "id", true);
        assertEquals(2, testResults.size());
        assertEquals("2", testResults.get(0).getId());
        assertEquals("1", testResults.get(1).getId());
    }

    @Test
    public void getAllOrdered_shouldReturnOrderTestResults() {
        List<TestResult> testResults = testResultService.getAllOrdered("id", true);
        assertEquals(2, testResults.size());
        assertEquals("2", testResults.get(0).getId());
        assertEquals("1", testResults.get(1).getId());
    }

    @Test
    public void getAlOrderedGivenListshouldReturnAllOrderedGivenLists() {
        List<String> list = List.of("id");
        List<TestResult> testResults = testResultService.getAllOrdered(list, true);
        assertEquals(2, testResults.size());
        assertEquals("2", testResults.get(0).getId());
        assertEquals("1", testResults.get(1).getId());
    }

    @Test
    public void getPage_shouldReturnPageOfTestResults() {
        List<TestResult> testResults = testResultService.getPage(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testResults.size() <= expectedPages);

    }

    @Test
    public void getAll_shouldReturnAllTestResults() {
        List<TestResult> testResults = testResultService.getAll();
        assertEquals(2, testResults.size());
        assertEquals("1", testResults.get(0).getId());
        assertEquals("2", testResults.get(1).getId());
    }

    @Test
    public void getAllMatchingPage_shouldReturnAllMatchingPage() {
        List<TestResult> testResults = testResultService.getMatchingPage("contLevel", "High", 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testResults.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingPageGivenMaps_shouldReturnAllMatchingPageGivenMaps() {
        Map<String, Object> map = Map.of("contLevel", "High");
        List<TestResult> testResults = testResultService.getMatchingPage(map, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testResults.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingPageOrdered_shouldReturnAllMatchingPageOrdered() {
        List<TestResult> testResults = testResultService.getMatchingOrderedPage("contLevel", "High", "id", true, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testResults.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingPageOrderedGivenMaps_shouldReturnAllMatchingPageOrderedGivenMaps() {
        Map<String, Object> map = Map.of("contLevel", "High");
        List<TestResult> testResults = testResultService.getMatchingOrderedPage(map, "id", true, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testResults.size() <= expectedPages);
    }

    @Test
    public void getOrderedPage_shouldReturnOrderedPage() {
        List<TestResult> testResults = testResultService.getOrderedPage("id", true, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testResults.size() <= expectedPages);
    }

    @Test
    public void getOrderedPageGivenList_shouldReturnOrderedPageGivenList() {
        List<String> list = List.of("id");
        List<TestResult> testResults = testResultService.getOrderedPage(list, true, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testResults.size() <= expectedPages);
    }

    @Test
    public void getCount_shouldReturnCountOfTestResults() {
        int count = testResultService.getCount();
        assertEquals(2, count);
    }

    @Test
    public void get_shouldReturnTestResultGivenId() {
        TestResult testResult = testResultService.get("1");
        assertEquals("1", testResult.getId());
        assertTrue(testResult.getIsQuantifiable());
        assertTrue(testResult.getIsActive());
    }

    @Test
    public void getNextId_shouldReturnNextId() {
        TestResult nextId = testResultService.getNext("1");
        assertEquals("2", nextId.getId());
    }

    @Test
    public void getPrevious_Id_shouldReturnPreviousId() {
        TestResult previousId = testResultService.getPrevious("2");
        assertEquals("1", previousId.getId());
    }

    @Test
    public void deleteAll_shouldDeleteAllTestResults() {
        List<TestResult> testResults = testResultService.getAll();
        testResultService.deleteAll(testResults);
        List<TestResult> testResult = testResultService.getAll();
        assertEquals(0, testResult.size());
    }

    @Test
    public void delete_shouldDeleteTestResult() {
        TestResult testResult = testResultService.get("1");
        testResultService.delete(testResult);
        List<TestResult> testResults = testResultService.getAll();
        assertEquals(1, testResults.size());
        assertEquals("2", testResults.get(0).getId());
    }

}
