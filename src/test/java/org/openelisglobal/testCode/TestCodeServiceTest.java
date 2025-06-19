package org.openelisglobal.testCode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcodes.service.TestCodeService;
import org.openelisglobal.testcodes.service.TestCodeTypeService;
import org.openelisglobal.testcodes.valueholder.TestCode;
import org.openelisglobal.testcodes.valueholder.TestCodeType;
import org.openelisglobal.testcodes.valueholder.TestSchemaPK;
import org.springframework.beans.factory.annotation.Autowired;

public class TestCodeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestCodeService testCodeService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestCodeTypeService testCodeTypeService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test-code.xml");
    }

    @Test
    public void getAll_shouldReturnAllTestCodes() {
        List<TestCode> testCodes = testCodeService.getAll();
        assertNotNull(testCodes);
        assertEquals(3, testCodes.size());
        assertEquals("1", testCodes.get(0).getCodeTypeId());
        assertEquals("2", testCodes.get(1).getCodeTypeId());
        assertEquals("3", testCodes.get(2).getCodeTypeId());

    }

    @Test
    public void getAllMatchingCodes_shouldReturnMatchingTestCodes() {
        List<TestCode> testCodes = testCodeService.getAllMatching("value", "T01");
        assertNotNull(testCodes);
        assertEquals(1, testCodes.size());
        assertEquals("1", testCodes.get(0).getCodeTypeId());
    }

    @Test
    public void getAllMatchingGivenMap_shouldReturnMatchingTestCodes() {
        Map<String, Object> map = Map.of("value", "T01");
        List<TestCode> testCodes = testCodeService.getAllMatching(map);
        assertNotNull(testCodes);
        assertEquals(1, testCodes.size());
        assertEquals("T01", testCodes.get(0).getValue());
    }

    @Test
    public void getAllOrdered_shouldReturnAllTestCodesOrdered() {
        List<TestCode> testCodes = testCodeService.getAllOrdered("value", false);
        assertNotNull(testCodes);
        assertEquals(3, testCodes.size());
        assertEquals("T01", testCodes.get(0).getValue());
        assertEquals("T02", testCodes.get(1).getValue());
        assertEquals("T02", testCodes.get(1).getValue());
    }

    @Test
    public void getAllOrderedGivenList_shouldReturnAllTestCodesOrdered() {
        List<String> orderBy = List.of("value");
        List<TestCode> testCodes = testCodeService.getAllOrdered(orderBy, false);
        assertNotNull(testCodes);
        assertEquals(3, testCodes.size());
        assertEquals("1", testCodes.get(0).getCodeTypeId());
        assertEquals("2", testCodes.get(1).getCodeTypeId());
        assertEquals("3", testCodes.get(2).getCodeTypeId());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnMatchingTestCodesOrdered() {
        List<TestCode> testCodes = testCodeService.getAllMatchingOrdered("value", "T01", "value", false);
        assertNotNull(testCodes);
        assertEquals(1, testCodes.size());
        assertEquals("1", testCodes.get(0).getCodeTypeId());
    }

    @Test
    public void getAllMatchingOrderedGivenList_shouldReturnMatchingTestCodesOrdered() {
        List<String> orderBy = List.of("value");
        List<TestCode> testCodes = testCodeService.getAllMatchingOrdered("value", "T01", orderBy, false);
        assertNotNull(testCodes);
        assertEquals(1, testCodes.size());
        assertEquals("1", testCodes.get(0).getCodeTypeId());
    }

    @Test
    public void getAllMatchingOrderedGivenMap_shouldReturnMatchingTestCodesOrdered() {
        Map<String, Object> map = Map.of("value", "T01");
        List<TestCode> testCodes = testCodeService.getAllMatchingOrdered(map, "value", false);
        assertNotNull(testCodes);
        assertEquals(1, testCodes.size());
        assertEquals("1", testCodes.get(0).getCodeTypeId());
    }

    @Test
    public void getAllMatchingOrderedGivenMapAndList_shouldReturnMatchingTestCodesOrdered() {
        Map<String, Object> map = Map.of("value", "T01");
        List<String> orderBy = List.of("value");
        List<TestCode> testCodes = testCodeService.getAllMatchingOrdered(map, orderBy, false);
        assertNotNull(testCodes);
        assertEquals(1, testCodes.size());
        assertEquals("1", testCodes.get(0).getCodeTypeId());
    }

    @Test
    public void getAllMatchingPage_shouldReturnPageOfMatchingTestCodes() {
        List<TestCode> testCodes = testCodeService.getMatchingPage("value", "T01", 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodes.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingPageGivenMap_shouldReturnPageOfMatchingTestCodes() {
        Map<String, Object> map = Map.of("value", "T01");
        List<TestCode> testCodes = testCodeService.getMatchingPage(map, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodes.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingPageOrdered_shouldReturnPageOfMatchingTestCodesOrdered() {
        List<TestCode> testCodes = testCodeService.getMatchingOrderedPage("value", "T01", "value", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodes.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingPageOrderedGivenList_shouldReturnPageOfMatchingTestCodesOrdered() {
        List<String> orderBy = List.of("value");
        List<TestCode> testCodes = testCodeService.getMatchingOrderedPage("value", "T01", orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodes.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingPageOrderedGivenMap_shouldReturnPageOfMatchingTestCodesOrdered() {
        Map<String, Object> map = Map.of("value", "T01");
        List<TestCode> testCodes = testCodeService.getMatchingOrderedPage(map, "value", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodes.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingPageOrderedGivenMapAndList_shouldReturnPageOfMatchingTestCodesOrdered() {
        Map<String, Object> map = Map.of("value", "T01");
        List<String> orderBy = List.of("value");
        List<TestCode> testCodes = testCodeService.getMatchingOrderedPage(map, orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodes.size() <= expectedPages);
    }

    @Test
    public void getAllOrderedPage_shouldReturnPageOfAllTestCodesOrdered() {
        List<TestCode> testCodes = testCodeService.getOrderedPage("value", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodes.size() <= expectedPages);
    }

    @Test
    public void getAllOrderedPageGivenList_shouldReturnPageOfAllTestCodesOrdered() {
        List<String> orderBy = List.of("value");
        List<TestCode> testCodes = testCodeService.getOrderedPage(orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodes.size() <= expectedPages);
    }

    @Test
    public void deleteAll_shouldDeleteAllTestCodes() {
        List<TestCode> testCodes1 = testCodeService.getAll();
        testCodeService.deleteAll(testCodes1);
        List<TestCode> testCodes = testCodeService.getAll();
        assertNotNull(testCodes);
        assertEquals(0, testCodes.size());
    }

    @Test
    public void delete_shouldDeleteTestCode() {

        TestCode testCode1 = testCodeService.getAll().get(0);
        testCodeService.delete(testCode1);
        List<TestCode> deletedTestCode = testCodeService.getAll();
        assertEquals(2, deletedTestCode.size());

    }

    @Test
    public void insert_shouldInsertTestCode() {
        List<TestCode> testCodes1 = testCodeService.getAll();
        testCodeService.deleteAll(testCodes1);
        org.openelisglobal.test.valueholder.Test test = testService.get("104");
        TestCodeType testCodeType = testCodeTypeService.get("1");
        TestCode testCode = new TestCode();
        testCode.setValue("T04");
        testCode.setTestId(test.getId());
        testCode.setCodeTypeId(testCodeType.getId());
        TestSchemaPK testCodeId = testCodeService.insert(testCode);
        List<TestCode> testCodes = testCodeService.getAll();
        assertNotNull(testCodes);
        assertEquals(1, testCodes.size());
        assertEquals(testCodeId.getCodeTypeId(), testCodes.get(0).getCodeTypeId());
    }

}
