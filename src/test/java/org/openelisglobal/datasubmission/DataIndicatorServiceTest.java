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
import org.openelisglobal.datasubmission.service.DataIndicatorService;
import org.openelisglobal.datasubmission.service.TypeOfDataIndicatorService;
import org.openelisglobal.datasubmission.valueholder.DataIndicator;
import org.openelisglobal.datasubmission.valueholder.TypeOfDataIndicator;
import org.springframework.beans.factory.annotation.Autowired;

public class DataIndicatorServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataIndicatorService dataIndicatorService;

    @Autowired
    private TypeOfDataIndicatorService typeOfDataIndicatorService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/data-indicator.xml");

    }

    @Test
    public void getAll_shouldReturnAllDataIndicators() {
        // This test will verify that all data indicators are returned correctly
        List<DataIndicator> dataIndicators = dataIndicatorService.getAll();
        assertNotNull(dataIndicators);
        assertEquals(4, dataIndicators.size());
        assertEquals("1", dataIndicators.get(0).getId());
        assertEquals("2", dataIndicators.get(1).getId());
        assertEquals("3", dataIndicators.get(2).getId());
        assertEquals("4", dataIndicators.get(3).getId());

    }

    @Test
    public void get_shouldReturnIndicatorById() {
        DataIndicator dataIndicator = dataIndicatorService.get("1");
        assertNotNull(dataIndicator);
        assertEquals("1", dataIndicator.getId());
        assertEquals("SENT", dataIndicator.getStatus());
    }

    @Test
    public void getIndicatorByTypeYearMonth_shouldReturnIndicator() {
        TypeOfDataIndicator type = typeOfDataIndicatorService.get("1");
        DataIndicator dataIndicator = dataIndicatorService.getIndicatorByTypeYearMonth(type, 2023, 1);
        assertNotNull(dataIndicator);
        assertEquals("1", dataIndicator.getId());
        assertEquals("SENT", dataIndicator.getStatus());

    }

    @Test
    public void getIndicatorsByStatus_shouldReturnIndicatorsByStatus() {
        List<DataIndicator> sentIndicators = dataIndicatorService.getIndicatorsByStatus("SENT");
        assertNotNull(sentIndicators);
        assertEquals(1, sentIndicators.size());
        assertEquals("1", sentIndicators.get(0).getId());
        List<DataIndicator> failedIndicators = dataIndicatorService.getIndicatorsByStatus("FAILED");
        assertNotNull(failedIndicators);
        assertEquals(1, failedIndicators.size());
        assertEquals("3", failedIndicators.get(0).getId());
        List<DataIndicator> receivedIndicators = dataIndicatorService.getIndicatorsByStatus("RECEIVED");
        assertNotNull(receivedIndicators);
        assertEquals(1, receivedIndicators.size());
        assertEquals("2", receivedIndicators.get(0).getId());
    }

    @Test
    public void getPage_shouldReturnPageOfDataIndicators() {

        List<DataIndicator> dataIndicators = dataIndicatorService.getPage(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(dataIndicators.size() <= expectedPages);

    }

    @Test
    public void getAllMatching_shouldReturnMatchingIndicators() {
        List<DataIndicator> dataIndicators = dataIndicatorService.getAllMatching("status", "SENT");
        assertNotNull(dataIndicators);
        assertEquals(1, dataIndicators.size());
        assertEquals("1", dataIndicators.get(0).getId());
    }

    @Test
    public void getAllMatchingGivenMap_shouldReturnMatchingIndicators() {
        Map<String, Object> properties = Map.of("status", "SENT");
        List<DataIndicator> dataIndicators = dataIndicatorService.getAllMatching(properties);
        assertNotNull(dataIndicators);
        assertEquals(1, dataIndicators.size());
        assertEquals("1", dataIndicators.get(0).getId());
    }

    @Test
    public void getAllOrdered_shouldReturnOrderedIndicators() {
        List<DataIndicator> dataIndicators = dataIndicatorService.getAllOrdered("id", false);
        assertNotNull(dataIndicators);
        assertEquals(4, dataIndicators.size());
        assertEquals("1", dataIndicators.get(0).getId());
        assertEquals("2", dataIndicators.get(1).getId());
        assertEquals("3", dataIndicators.get(2).getId());
        assertEquals("4", dataIndicators.get(3).getId());
    }

    @Test
    public void getAllOrderedGivenList_shouldReturnOrderedIndicators() {
        List<String> orderBy = List.of("id");
        List<DataIndicator> dataIndicators = dataIndicatorService.getAllOrdered(orderBy, false);
        assertNotNull(dataIndicators);
        assertEquals(4, dataIndicators.size());
        assertEquals("1", dataIndicators.get(0).getId());
        assertEquals("2", dataIndicators.get(1).getId());
        assertEquals("3", dataIndicators.get(2).getId());
        assertEquals("4", dataIndicators.get(3).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenMap_shouldReturnOrderedIndicators() {
        Map<String, Object> properties = Map.of("status", "SENT");
        List<DataIndicator> dataIndicators = dataIndicatorService.getAllMatchingOrdered(properties, "id", false);
        assertNotNull(dataIndicators);
        assertEquals(1, dataIndicators.size());
        assertEquals("1", dataIndicators.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenListAndMap_shouldReturnOrderedIndicators() {
        Map<String, Object> properties = Map.of("status", "SENT");
        List<String> orderBy = List.of("id");
        List<DataIndicator> dataIndicators = dataIndicatorService.getAllMatchingOrdered(properties, orderBy, false);
        assertNotNull(dataIndicators);
        assertEquals(1, dataIndicators.size());
        assertEquals("1", dataIndicators.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnOrderedIndicators() {
        List<DataIndicator> dataIndicators = dataIndicatorService.getAllMatchingOrdered("status", "SENT", "id", false);
        assertNotNull(dataIndicators);
        assertEquals(1, dataIndicators.size());
        assertEquals("1", dataIndicators.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenList_shouldReturnOrderedIndicators() {
        List<String> orderBy = List.of("id");
        List<DataIndicator> dataIndicators = dataIndicatorService.getAllMatchingOrdered("status", "SENT", orderBy,
                false);
        assertNotNull(dataIndicators);
        assertEquals(1, dataIndicators.size());
        assertEquals("1", dataIndicators.get(0).getId());
    }

    @Test
    public void getOrderedPage_shouldReturnPageOfOrderedIndicators() {
        List<DataIndicator> dataIndicators = dataIndicatorService.getOrderedPage("id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(dataIndicators.size() <= expectedPages);

    }

    @Test
    public void getOrderedPageGivenList_shouldReturnPageOfOrderedIndicators() {
        List<String> orderBy = List.of("id");
        List<DataIndicator> dataIndicators = dataIndicatorService.getOrderedPage(orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(dataIndicators.size() <= expectedPages);

    }

    @Test
    public void getAllMatchingPaged_shouldReturnPageOfMatchingIndicators() {
        List<DataIndicator> dataIndicators = dataIndicatorService.getMatchingPage("status", "SENT", 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(dataIndicators.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingPagedGivenMap_shouldReturnPageOfMatchingIndicators() {
        Map<String, Object> properties = Map.of("status", "SENT");
        List<DataIndicator> dataIndicators = dataIndicatorService.getMatchingPage(properties, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(dataIndicators.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingOrderedPaged_shouldReturnPageOfOrderedMatchingIndicators() {
        List<DataIndicator> dataIndicators = dataIndicatorService.getMatchingOrderedPage("status", "SENT", "id", false,
                1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(dataIndicators.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingOrderedPagedGivenList_shouldReturnPageOfOrderedMatchingIndicators() {
        List<String> orderBy = List.of("id");
        List<DataIndicator> dataIndicators = dataIndicatorService.getMatchingOrderedPage("status", "SENT", orderBy,
                false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(dataIndicators.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingOrderedPagedGivenMap_shouldReturnPageOfOrderedMatchingIndicators() {
        Map<String, Object> properties = Map.of("status", "SENT");
        List<DataIndicator> dataIndicators = dataIndicatorService.getMatchingOrderedPage(properties, "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(dataIndicators.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingOrderedPagedGivenListAndMap_shouldReturnPageOfOrderedMatchingIndicators() {
        Map<String, Object> properties = Map.of("status", "SENT");
        List<String> orderBy = List.of("id");
        List<DataIndicator> dataIndicators = dataIndicatorService.getMatchingOrderedPage(properties, orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(dataIndicators.size() <= expectedPages);
    }

    @Test
    public void getNext_shouldReturnNextDataIndicator() {
        DataIndicator dataIndicator = dataIndicatorService.getNext("1");
        assertNotNull(dataIndicator);
        assertEquals("2", dataIndicator.getId());
    }

    @Test
    public void getPrevious_shouldReturnPreviousDataIndicator() {
        DataIndicator dataIndicator = dataIndicatorService.getPrevious("2");
        assertNotNull(dataIndicator);
        assertEquals("1", dataIndicator.getId());
    }

    @Test
    public void getCount_shouldReturnCountOfDataIndicators() {
        int count = dataIndicatorService.getCount();
        assertEquals(4, count);
    }

    @Test
    public void deleteAll_shouldDeleteAllDataIndicators() {
        List<DataIndicator> dataIndicators1 = dataIndicatorService.getAll();
        dataIndicatorService.deleteAll(dataIndicators1);
        List<DataIndicator> dataIndicators = dataIndicatorService.getAll();
        assertNotNull(dataIndicators);
        assertEquals(0, dataIndicators.size());
    }

    @Test
    public void delete_shouldDeleteDataIndicator() {
        DataIndicator dataIndicator = dataIndicatorService.get("1");
        assertNotNull(dataIndicator);
        dataIndicatorService.delete(dataIndicator);
        List<DataIndicator> dataIndicators = dataIndicatorService.getAll();
        assertEquals(3, dataIndicators.size());
    }

    @Test
    public void insert_shouldInsertDataIndicator() {
        List<DataIndicator> dataIndicators = dataIndicatorService.getAll();
        dataIndicatorService.deleteAll(dataIndicators);
        DataIndicator dataIndicator = new DataIndicator();
        dataIndicator.setYear(2023);
        dataIndicator.setMonth(1);
        TypeOfDataIndicator type = typeOfDataIndicatorService.get("1");
        dataIndicator.setTypeOfIndicator(type);
        dataIndicator.setStatus("SENT");

        String insertedId = dataIndicatorService.insert(dataIndicator);

        DataIndicator insertedDataIndicator = dataIndicatorService.get(insertedId);
        assertNotNull(insertedDataIndicator);
        assertEquals(insertedId, insertedDataIndicator.getId());
    }

    @Test
    public void save_shoulsSaveDataIndicator() {
        List<DataIndicator> dataIndicators = dataIndicatorService.getAll();
        dataIndicatorService.deleteAll(dataIndicators);
        DataIndicator dataIndicator = new DataIndicator();
        dataIndicator.setYear(2023);
        dataIndicator.setMonth(1);
        TypeOfDataIndicator type = typeOfDataIndicatorService.get("1");
        dataIndicator.setTypeOfIndicator(type);
        dataIndicator.setStatus("UNSAVED");
        DataIndicator indicator2 = dataIndicatorService.save(dataIndicator);
        assertNotNull(indicator2);
        List<DataIndicator> indicators1 = dataIndicatorService.getAll();
        assertEquals(1, indicators1.size());
        assertEquals(indicator2.getStatus(), indicators1.get(0).getStatus());
    }

    @Test
    public void update_shouldUpdateDataIndicator() {
        DataIndicator dataIndicator = dataIndicatorService.get("1");
        assertNotNull(dataIndicator);
        dataIndicator.setStatus("FAILED");
        DataIndicator updatedDataIndicator = dataIndicatorService.update(dataIndicator);
        assertNotNull(updatedDataIndicator);
        DataIndicator fetchedDataIndicator = dataIndicatorService.get("1");
        assertEquals(updatedDataIndicator.getStatus(), fetchedDataIndicator.getStatus());
    }
}