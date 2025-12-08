package org.openelisglobal.testCode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.testcodes.service.TestCodeTypeService;
import org.openelisglobal.testcodes.valueholder.TestCodeType;
import org.springframework.beans.factory.annotation.Autowired;

public class TestCodeTypeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestCodeTypeService testCodeTypeService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test-code-type.xml");
    }

    @Test
    public void getAll_shouldReturnAllTestCodeTypes() {
        List<TestCodeType> testCodeTypes = testCodeTypeService.getAll();
        assertEquals(4, testCodeTypes.size());
        assertEquals("1", testCodeTypes.get(0).getId());
        assertEquals("2", testCodeTypes.get(1).getId());
        assertEquals("3", testCodeTypes.get(2).getId());
        assertEquals("4", testCodeTypes.get(3).getId());

    }

    @Test
    public void getTestCodeTypeByName_shouldReturnTestCodeType() {
        TestCodeType testCodeType = testCodeTypeService.getTestCodeTypeByName("chemistry");
        assertEquals("1", testCodeType.getId());
        assertEquals("chemistry", testCodeType.getSchemaName());
    }

    @Test
    public void get_shouldReturnTestcodeTypeById() {
        TestCodeType testCodeType = testCodeTypeService.get("1");
        assertEquals("1", testCodeType.getId());
        assertEquals("chemistry", testCodeType.getSchemaName());
    }

    @Test
    public void getMatchingTestCodeType_shouldReturnTestCodeType() {
        List<TestCodeType> testCodeTypes = testCodeTypeService.getAllMatching("schemaName", "chemistry");
        assertEquals(1, testCodeTypes.size());
        assertEquals("1", testCodeTypes.get(0).getId());
        assertEquals("chemistry", testCodeTypes.get(0).getSchemaName());
    }

    @Test
    public void getAllMatchingGivenMap__shouldReturnTestCodeType() {
        Map<String, Object> map = Map.of("schemaName", "chemistry");
        List<TestCodeType> testCodeTypes = testCodeTypeService.getAllMatching(map);
        assertEquals(1, testCodeTypes.size());
        assertEquals("1", testCodeTypes.get(0).getId());
        assertEquals("chemistry", testCodeTypes.get(0).getSchemaName());
    }

    @Test
    public void getAllOrdered_shouldReturnAllOrdered() {
        List<TestCodeType> testCodeTypes = testCodeTypeService.getAllOrdered("id", false);
        assertEquals(4, testCodeTypes.size());
        assertEquals("1", testCodeTypes.get(0).getId());
        assertEquals("2", testCodeTypes.get(1).getId());
        assertEquals("3", testCodeTypes.get(2).getId());
        assertEquals("4", testCodeTypes.get(3).getId());
    }

    @Test
    public void getAllOrderedGiveList_shouldReturnAllOrdered() {
        List<String> list = List.of("id");
        List<TestCodeType> testCodeTypes = testCodeTypeService.getAllOrdered(list, false);
        assertEquals(4, testCodeTypes.size());
        assertEquals("1", testCodeTypes.get(0).getId());
        assertEquals("2", testCodeTypes.get(1).getId());
        assertEquals("3", testCodeTypes.get(2).getId());
        assertEquals("4", testCodeTypes.get(3).getId());
    }

    @Test
    public void getAllMatchingOredered_shouldReturnAllMatchingOrderd() {
        List<TestCodeType> testCodeTypes = testCodeTypeService.getAllMatchingOrdered("schemaName", "chemistry", "id",
                false);
        assertEquals(1, testCodeTypes.size());
        assertEquals("1", testCodeTypes.get(0).getId());
        assertEquals("chemistry", testCodeTypes.get(0).getSchemaName());
    }

    @Test
    public void getAllMatchingOrderedGivenMap_shouldReturnAllMatchingOrdered() {
        Map<String, Object> map = Map.of("schemaName", "chemistry");
        List<TestCodeType> testCodeTypes = testCodeTypeService.getAllMatchingOrdered(map, "id", false);
        assertEquals(1, testCodeTypes.size());
        assertEquals("1", testCodeTypes.get(0).getId());
        assertEquals("chemistry", testCodeTypes.get(0).getSchemaName());
    }

    @Test
    public void getAllMatchingOrderedGivenList_shouldReturnAllMatchingOrdered() {
        List<String> list = List.of("id");
        List<TestCodeType> testCodeTypes = testCodeTypeService.getAllMatchingOrdered("schemaName", "chemistry", list,
                false);
        assertEquals(1, testCodeTypes.size());
        assertEquals("1", testCodeTypes.get(0).getId());
        assertEquals("chemistry", testCodeTypes.get(0).getSchemaName());
    }

    @Test
    public void getAllMatchingOrderedGivenListAndMap_shouldReturnAllMatchingOrdered() {
        List<String> list = List.of("id");
        Map<String, Object> map = Map.of("schemaName", "chemistry");
        List<TestCodeType> testCodeTypes = testCodeTypeService.getAllMatchingOrdered(map, list, false);
        assertEquals(1, testCodeTypes.size());
        assertEquals("1", testCodeTypes.get(0).getId());
        assertEquals("chemistry", testCodeTypes.get(0).getSchemaName());
    }

    @Test
    public void getPage_shouldReturnPageOfTestCodeType() {
        List<TestCodeType> testCodeTypes = testCodeTypeService.getPage(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodeTypes.size() <= expectedPages);

    }

    @Test
    public void getAllOrderedPage_shouldReturnAllOrderedPage() {
        List<TestCodeType> testCodeTypes = testCodeTypeService.getOrderedPage("id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodeTypes.size() <= expectedPages);
    }

    @Test
    public void getAllOrderedPageGivenList_shouldReturnAllOrderedPage() {
        List<String> list = List.of("id");
        List<TestCodeType> testCodeTypes = testCodeTypeService.getOrderedPage(list, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodeTypes.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingOrderedPage_shouldReturnAllMatchingOrderedPage() {
        List<TestCodeType> testCodeTypes = testCodeTypeService.getMatchingOrderedPage("schemaName", "chemistry", "id",
                false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodeTypes.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingOrderedPageGivenMap_shouldReturnAllMatchingOrderedPage() {
        Map<String, Object> map = Map.of("schemaName", "chemistry");
        List<TestCodeType> testCodeTypes = testCodeTypeService.getMatchingOrderedPage(map, "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodeTypes.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingOrderedPageGivenList_shouldReturnAllMatchingOrderedPage() {
        List<String> list = List.of("id");
        List<TestCodeType> testCodeTypes = testCodeTypeService.getMatchingOrderedPage("schemaName", "chemistry", list,
                false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodeTypes.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingOrderedPageGivenListAndMap_shouldReturnAllMatchingOrderedPage() {
        List<String> list = List.of("id");
        Map<String, Object> map = Map.of("schemaName", "chemistry");
        List<TestCodeType> testCodeTypes = testCodeTypeService.getMatchingOrderedPage(map, list, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testCodeTypes.size() <= expectedPages);
    }

    @Test
    public void getCount_shouldReturnCountOfTestCodeType() {
        int count = testCodeTypeService.getCount();
        assertEquals(4, count);
    }

    @Test
    public void getNext_shouldReturnNextTestCodeType() {
        TestCodeType testCodeType = testCodeTypeService.getNext("1");
        assertEquals("2", testCodeType.getId());
        assertEquals("hematology", testCodeType.getSchemaName());
    }

    @Test
    public void getProvious_shouldReturnPreviousTestCodeType() {
        TestCodeType testCodeType = testCodeTypeService.getPrevious("2");
        assertEquals("1", testCodeType.getId());
        assertEquals("chemistry", testCodeType.getSchemaName());
    }

    @Test
    public void deleteAll_shouldDeleteAllTestCodeType() {
        List<TestCodeType> testCodeTypes1 = testCodeTypeService.getAll();
        testCodeTypeService.deleteAll(testCodeTypes1);
        List<TestCodeType> testCodeTypes = testCodeTypeService.getAll();
        assertEquals(0, testCodeTypes.size());
    }

    @Test
    public void delete_shouldDeleteTestCodeType() {
        TestCodeType testCodeType = testCodeTypeService.get("1");
        testCodeTypeService.delete(testCodeType);
        List<TestCodeType> testCodeTypes = testCodeTypeService.getAll();
        assertEquals(3, testCodeTypes.size());
    }

    @Test
    public void save_shouldSaveTestCodeType() {
        List<TestCodeType> testCodeTypes1 = testCodeTypeService.getAll();
        testCodeTypeService.deleteAll(testCodeTypes1);
        TestCodeType testCodeType = new TestCodeType();
        testCodeType.setSchemaName("hematology");
        testCodeTypeService.save(testCodeType);
        List<TestCodeType> testCodeTypes = testCodeTypeService.getAll();
        assertEquals(1, testCodeTypes.size());
        assertEquals("hematology", testCodeTypes.get(0).getSchemaName());
    }

    @Test
    public void insert_shouldInsertTestCodeType() {
        List<TestCodeType> testCodeTypes1 = testCodeTypeService.getAll();
        testCodeTypeService.deleteAll(testCodeTypes1);
        TestCodeType testCodeType = new TestCodeType();
        testCodeType.setSchemaName("hematology");
        testCodeTypeService.insert(testCodeType);
        List<TestCodeType> testCodeTypes = testCodeTypeService.getAll();
        assertEquals(1, testCodeTypes.size());
        assertEquals("hematology", testCodeTypes.get(0).getSchemaName());
    }

    @Test
    public void update_shouldUpdateTestCodeType() {
        TestCodeType testCodeType = testCodeTypeService.get("1");
        testCodeType.setSchemaName("hematology");
        testCodeTypeService.update(testCodeType);
        TestCodeType updatedTestCodeType = testCodeTypeService.get("1");
        assertEquals("hematology", updatedTestCodeType.getSchemaName());
    }

}
