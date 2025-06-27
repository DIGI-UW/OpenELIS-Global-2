package org.openelisglobal.datasubmission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.datasubmission.service.DataValueService;
import org.openelisglobal.datasubmission.valueholder.DataValue;
import org.springframework.beans.factory.annotation.Autowired;

public class DataValueServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataValueService dataValueService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/data-value.xml");
    }

    @Test
    public void getAll_shouldReturnAllDataValues() {

        List<DataValue> dataValues = dataValueService.getAll();
        assertEquals(4, dataValues.size());
        assertEquals("1", dataValues.get(0).getId());
        assertEquals("2", dataValues.get(1).getId());
        assertEquals("3", dataValues.get(2).getId());
        assertEquals("4", dataValues.get(3).getId());
    }

    @Test
    public void getPage_shouldReturnPageOfDataValues() {
        List<DataValue> dataValues = dataValueService.getPage(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(dataValues.size() <= expectedPages);
    }

    @Test
    public void getAllMatching_shouldReturnMatchingDataValues() {
        List<DataValue> dataValues = dataValueService.getAllMatching("columnName", "cases");
        assertNotNull(dataValues);
        assertEquals(1, dataValues.size());
        assertEquals("1", dataValues.get(0).getId());
        assertEquals("cases", dataValues.get(0).getColumnName());
    }

    @Test
    public void getAllMatchingGivenMap_shouldReturnMatchingDataValues() {
        Map<String, Object> properties = Map.of("columnName", "cases");
        List<DataValue> dataValues = dataValueService.getAllMatching(properties);
        assertNotNull(dataValues);
        assertEquals(1, dataValues.size());
        assertEquals("1", dataValues.get(0).getId());
        assertEquals("cases", dataValues.get(0).getColumnName());
    }

    @Test
    public void getAllOrdered_shouldReturnAllOrderedDataValues() {
        List<DataValue> dataValues = dataValueService.getAllOrdered("id", false);
        assertNotNull(dataValues);
        assertEquals(4, dataValues.size());
        assertEquals("1", dataValues.get(0).getId());
        assertEquals("2", dataValues.get(1).getId());
        assertEquals("3", dataValues.get(2).getId());
        assertEquals("4", dataValues.get(3).getId());
    }

    @Test
    public void getAllOrderedGivenList_shouldReturnAllOrderedDataValues() {
        List<String> orderBy = List.of("id");
        List<DataValue> dataValues = dataValueService.getAllOrdered(orderBy, false);
        assertNotNull(dataValues);
        assertEquals(4, dataValues.size());
        assertEquals("1", dataValues.get(0).getId());
        assertEquals("2", dataValues.get(1).getId());
        assertEquals("3", dataValues.get(2).getId());
        assertEquals("4", dataValues.get(3).getId());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnAllMatchingOrderedDataValues() {
        List<DataValue> dataValues = dataValueService.getAllMatchingOrdered("columnName", "cases", "id", false);
        assertNotNull(dataValues);
        assertEquals(1, dataValues.size());
        assertEquals("1", dataValues.get(0).getId());
        assertEquals("cases", dataValues.get(0).getColumnName());
    }

    @Test
    public void getAllMatchingOrderedGivenMap_shouldReturnAllMatchingOrderedDataValues() {
        Map<String, Object> properties = Map.of("columnName", "cases");
        List<DataValue> dataValues = dataValueService.getAllMatchingOrdered(properties, "id", false);
        assertNotNull(dataValues);
        assertEquals(1, dataValues.size());
        assertEquals("1", dataValues.get(0).getId());
        assertEquals("cases", dataValues.get(0).getColumnName());
    }

    @Test
    public void getAllMatchingOrderedGivenList_shouldReturnAllMatchingOrderedDataValues() {
        List<String> orderBy = List.of("id");
        List<DataValue> dataValues = dataValueService.getAllMatchingOrdered("columnName", "cases", orderBy, false);
        assertNotNull(dataValues);
        assertEquals(1, dataValues.size());
        assertEquals("1", dataValues.get(0).getId());
        assertEquals("cases", dataValues.get(0).getColumnName());
    }

    @Test
    public void getAllMatchingOrderedGivenMapAndList_shouldReturnAllMatchingOrderedDataValues() {
        Map<String, Object> properties = Map.of("columnName", "cases");
        List<String> orderBy = List.of("id");
        List<DataValue> dataValues = dataValueService.getAllMatchingOrdered(properties, orderBy, false);
        assertNotNull(dataValues);
        assertEquals(1, dataValues.size());
        assertEquals("1", dataValues.get(0).getId());
        assertEquals("cases", dataValues.get(0).getColumnName());
    }

    @Test
    public void getMatchingPage_shouldReturnPageOfAllMatchingDataValues() {
        List<DataValue> dataValues = dataValueService.getMatchingPage("columnName", "cases", 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataValues);
        assertTrue(dataValues.size() <= expectedPages);

    }

    @Test
    public void getMatchingPageGivenMap_shouldReturnPageOfAllMatchingDataValues() {
        Map<String, Object> properties = Map.of("columnName", "cases");
        List<DataValue> dataValues = dataValueService.getMatchingPage(properties, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataValues);
        assertTrue(dataValues.size() <= expectedPages);
    }

    @Test
    public void getOrderedPage_shouldReturnPageOfAllOrderedDataValues() {
        List<DataValue> dataValues = dataValueService.getOrderedPage("id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataValues);
        assertTrue(dataValues.size() <= expectedPages);
    }

    @Test
    public void getOrderedPageGivenList_shouldReturnPageOfAllOrderedDataValues() {
        List<String> orderBy = List.of("id");
        List<DataValue> dataValues = dataValueService.getOrderedPage(orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataValues);
        assertTrue(dataValues.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPage_shouldReturnPageOfAllOrderedMatchingDataValues() {
        List<DataValue> dataValues = dataValueService.getMatchingOrderedPage("columnName", "cases", "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataValues);
        assertTrue(dataValues.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPageGivenMap_shouldReturnPageOfAllOrderedMatchingDataValues() {
        Map<String, Object> properties = Map.of("columnName", "cases");
        List<DataValue> dataValues = dataValueService.getMatchingOrderedPage(properties, "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataValues);
        assertTrue(dataValues.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPageGivenList_shouldReturnPageOfAllOrderedMatchingDataValues() {
        List<String> orderBy = List.of("id");
        List<DataValue> dataValues = dataValueService.getMatchingOrderedPage("columnName", "cases", orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataValues);
        assertTrue(dataValues.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPageGivenMapAndList_shouldReturnPageOfAllOrderedMatchingDataValues() {
        Map<String, Object> properties = Map.of("columnName", "cases");
        List<String> orderBy = List.of("id");
        List<DataValue> dataValues = dataValueService.getMatchingOrderedPage(properties, orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataValues);
        assertTrue(dataValues.size() <= expectedPages);
    }

    @Test
    public void getCount_shouldReturnCountOfAllDataValues() {
        int count = dataValueService.getCount();
        assertEquals(4, count);
    }

    @Test
    public void getCountLike_shouldReturnCountOfAllDataValuesLike() {
        int count = dataValueService.getCountLike("columnName", "cases");
        assertEquals(1, count);
    }

    @Test
    public void getNext_shouldReturnNextDataValue() {
        DataValue dataValue = dataValueService.getNext("1");
        assertNotNull(dataValue);
        assertEquals("2", dataValue.getId());
        assertEquals("tests", dataValue.getColumnName());
    }

    @Test
    public void getPrevious_shouldReturnPreviousDataValues() {
        DataValue dataValue = dataValueService.getPrevious("2");
        assertNotNull(dataValue);
        assertEquals("1", dataValue.getId());
        assertEquals("cases", dataValue.getColumnName());
    }

    @Test
    public void deleteAll_shouldDeleteAllDataValues() {
        List<DataValue> dataValues = dataValueService.getAll();
        dataValueService.deleteAll(dataValues);
        List<DataValue> deletedDataValues = dataValueService.getAll();
        assertNotNull(deletedDataValues);
        assertEquals(0, deletedDataValues.size());
    }

    @Test
    public void delete_shouldDeleteDataValue() {
        DataValue dataValue = dataValueService.get("1");
        assertNotNull(dataValue);
        dataValueService.delete(dataValue);
        List<DataValue> dataValues = dataValueService.getAll();
        assertEquals(3, dataValues.size());
    }

    @Test
    public void insert_shouldInsertDataValue() {
        List<DataValue> dataValues = dataValueService.getAll();
        dataValueService.deleteAll(dataValues);
        DataValue newDataValue = new DataValue();
        newDataValue.setId("1");
        newDataValue.setColumnName("FacilityX");
        String insertedId = dataValueService.insert(newDataValue);
        DataValue insertedDataValue = dataValueService.get(insertedId);
        assertNotNull(insertedDataValue);
        assertEquals(insertedId, insertedDataValue.getId());
        assertEquals("FacilityX", insertedDataValue.getColumnName());
    }

    @Test
    public void update_shouldUpdateDatavalue() {
        DataValue dataValue = dataValueService.get("1");
        assertNotNull(dataValue);
        dataValue.setColumnName("FacilityX");
        DataValue resource1 = dataValueService.update(dataValue);
        DataValue updatedResource = dataValueService.get("1");
        assertNotNull(updatedResource);
        assertEquals(resource1.getColumnName(), updatedResource.getColumnName());
    }

    @Test
    public void save_shouldSaveDataValue() {
        List<DataValue> dataValues = dataValueService.getAll();
        dataValueService.deleteAll(dataValues);
        DataValue newDataValue = new DataValue();
        newDataValue.setColumnName("FacilityX");
        DataValue savedDataValue = dataValueService.save(newDataValue);
        DataValue updatedDataValue = dataValueService.get(savedDataValue.getId());
        assertNotNull(updatedDataValue);
        assertEquals(savedDataValue.getColumnName(), updatedDataValue.getColumnName());
    }

}
