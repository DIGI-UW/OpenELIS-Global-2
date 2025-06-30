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
import org.openelisglobal.datasubmission.service.DataResourceService;
import org.openelisglobal.datasubmission.valueholder.DataResource;
import org.springframework.beans.factory.annotation.Autowired;

public class DataResourceServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    private DataResourceService dataResourceService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/data-resource.xml");
    }

    @Test
    public void getAll_shouldReturnAllResources() {
        List<DataResource> dataResources = dataResourceService.getAll();
        assertNotNull(dataResources);
        assertEquals(4, dataResources.size());
        assertEquals("1", dataResources.get(0).getId());
        assertEquals("2", dataResources.get(1).getId());
        assertEquals("3", dataResources.get(2).getId());
        assertEquals("4", dataResources.get(3).getId());

    }

    @Test
    public void getPage_shouldReturnPageOfDataResources() {
        List<DataResource> dataResources = dataResourceService.getPage(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(dataResources.size() <= expectedPages);
    }

    @Test
    public void getAllMatching_shouldReturnMatchingDataResources() {
        List<DataResource> dataResources = dataResourceService.getAllMatching("name", "FacilityA");
        assertNotNull(dataResources);
        assertEquals(1, dataResources.size());
        assertEquals("1", dataResources.get(0).getId());
        assertEquals("FacilityA", dataResources.get(0).getName());
    }

    @Test
    public void getAllMatchingGivenMap_shouldReturnMatchingDataResources() {
        Map<String, Object> properties = Map.of("name", "FacilityA");
        List<DataResource> dataResources = dataResourceService.getAllMatching(properties);
        assertNotNull(dataResources);
        assertEquals(1, dataResources.size());
        assertEquals("1", dataResources.get(0).getId());
        assertEquals("FacilityA", dataResources.get(0).getName());
    }

    @Test
    public void getAllOrdered_shouldReturnAllOrderedDataResources() {
        List<DataResource> dataResources = dataResourceService.getAllOrdered("id", false);
        assertNotNull(dataResources);
        assertEquals(4, dataResources.size());
        assertEquals("1", dataResources.get(0).getId());
        assertEquals("2", dataResources.get(1).getId());
        assertEquals("3", dataResources.get(2).getId());
        assertEquals("4", dataResources.get(3).getId());
    }

    @Test
    public void getAllOrderedGivenList_shouldReturnAllOrderedDataResources() {
        List<String> orderBy = List.of("id");
        List<DataResource> dataResources = dataResourceService.getAllOrdered(orderBy, false);
        assertNotNull(dataResources);
        assertEquals(4, dataResources.size());
        assertEquals("1", dataResources.get(0).getId());
        assertEquals("2", dataResources.get(1).getId());
        assertEquals("3", dataResources.get(2).getId());
        assertEquals("4", dataResources.get(3).getId());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnAllMatchingOrderedDataResources() {
        List<DataResource> dataResources = dataResourceService.getAllMatchingOrdered("name", "FacilityA", "id", false);
        assertNotNull(dataResources);
        assertEquals(1, dataResources.size());
        assertEquals("1", dataResources.get(0).getId());
        assertEquals("FacilityA", dataResources.get(0).getName());
    }

    @Test
    public void getAllMatchingOrderedGivenMap_shouldReturnAllMatchingOrderedDataResources() {
        Map<String, Object> properties = Map.of("name", "FacilityA");
        List<DataResource> dataResources = dataResourceService.getAllMatchingOrdered(properties, "id", false);
        assertNotNull(dataResources);
        assertEquals(1, dataResources.size());
        assertEquals("1", dataResources.get(0).getId());
        assertEquals("FacilityA", dataResources.get(0).getName());
    }

    @Test
    public void getAllMatchingOrderedGivenList_shouldReturnAllMatchingOrderedDataResources() {
        List<String> orderBy = List.of("id");
        List<DataResource> dataResources = dataResourceService.getAllMatchingOrdered("name", "FacilityA", orderBy,
                false);
        assertNotNull(dataResources);
        assertEquals(1, dataResources.size());
        assertEquals("1", dataResources.get(0).getId());
        assertEquals("FacilityA", dataResources.get(0).getName());
    }

    @Test
    public void getAllMatchingOrderedGivenMapAndList_shouldReturnAllMatchingOrderedDataResources() {
        Map<String, Object> properties = Map.of("name", "FacilityA");
        List<String> orderBy = List.of("id");
        List<DataResource> dataResources = dataResourceService.getAllMatchingOrdered(properties, orderBy, false);
        assertNotNull(dataResources);
        assertEquals(1, dataResources.size());
        assertEquals("1", dataResources.get(0).getId());
        assertEquals("FacilityA", dataResources.get(0).getName());
    }

    @Test
    public void getMatchingPage_shouldReturnPageOfAllMatchingDataResources() {
        List<DataResource> dataResources = dataResourceService.getMatchingPage("name", "FacilityA", 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataResources);
        assertTrue(dataResources.size() <= expectedPages);

    }

    @Test
    public void getMatchingPageGivenMap_shouldReturnPageOfAllMatchingDataResources() {
        Map<String, Object> properties = Map.of("name", "FacilityA");
        List<DataResource> dataResources = dataResourceService.getMatchingPage(properties, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataResources);
        assertTrue(dataResources.size() <= expectedPages);
    }

    @Test
    public void getOrderedPage_shouldReturnPageOfAllOrderedDataResources() {
        List<DataResource> dataResources = dataResourceService.getOrderedPage("id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataResources);
        assertTrue(dataResources.size() <= expectedPages);
    }

    @Test
    public void getOrderedPageGivenList_shouldReturnPageOfAllOrderedDataResources() {
        List<String> orderBy = List.of("id");
        List<DataResource> dataResources = dataResourceService.getOrderedPage(orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataResources);
        assertTrue(dataResources.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPage_shouldReturnPageOfAllOrderedMatchingDataResources() {
        List<DataResource> dataResources = dataResourceService.getMatchingOrderedPage("name", "FacilityA", "id", false,
                1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataResources);
        assertTrue(dataResources.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPageGivenMap_shouldReturnPageOfAllOrderedMatchingDataResources() {
        Map<String, Object> properties = Map.of("name", "FacilityA");
        List<DataResource> dataResources = dataResourceService.getMatchingOrderedPage(properties, "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataResources);
        assertTrue(dataResources.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPageGivenList_shouldReturnPageOfAllOrderedMatchingDataResources() {
        List<String> orderBy = List.of("id");
        List<DataResource> dataResources = dataResourceService.getMatchingOrderedPage("name", "FacilityA", orderBy,
                false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataResources);
        assertTrue(dataResources.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPageGivenMapAndList_shouldReturnPageOfAllOrderedMatchingDataResources() {
        Map<String, Object> properties = Map.of("name", "FacilityA");
        List<String> orderBy = List.of("id");
        List<DataResource> dataResources = dataResourceService.getMatchingOrderedPage(properties, orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(dataResources);
        assertTrue(dataResources.size() <= expectedPages);
    }

    @Test
    public void getCount_shouldReturnCountOfAllDataResources() {
        int count = dataResourceService.getCount();
        assertEquals(4, count);
    }

    @Test
    public void getCountLike_shouldReturnCountOfAllDataResourcesLike() {
        int count = dataResourceService.getCountLike("name", "FacilityA");
        assertEquals(1, count);
    }

    @Test
    public void getNext_shouldReturnNextDataResource() {
        DataResource dataResource = dataResourceService.getNext("1");
        assertNotNull(dataResource);
        assertEquals("2", dataResource.getId());
        assertEquals("FacilityB", dataResource.getName());
    }

    @Test
    public void getPrevious_shouldReturnPreviousDataResources() {
        DataResource dataResource = dataResourceService.getPrevious("2");
        assertNotNull(dataResource);
        assertEquals("1", dataResource.getId());
        assertEquals("FacilityA", dataResource.getName());
    }

    @Test
    public void deleteAll_shouldDeleteAllDataResources() {
        List<DataResource> dataResources = dataResourceService.getAll();
        dataResourceService.deleteAll(dataResources);
        List<DataResource> deletedDataResources = dataResourceService.getAll();
        assertNotNull(deletedDataResources);
        assertEquals(0, deletedDataResources.size());
    }

    @Test
    public void delete_shouldDeleteDataResource() {
        DataResource resource = dataResourceService.get("1");
        assertNotNull(resource);
        dataResourceService.delete(resource);
        List<DataResource> resources = dataResourceService.getAll();
        assertEquals(3, resources.size());
    }

    @Test
    public void insert_shouldInsertDataResource() {
        List<DataResource> dataResources = dataResourceService.getAll();
        dataResourceService.deleteAll(dataResources);
        DataResource newDataResource = new DataResource();
        newDataResource.setId("1");
        newDataResource.setName("FacilityX");
        String insertedId = dataResourceService.insert(newDataResource);
        DataResource insertedDataResource = dataResourceService.get(insertedId);
        assertNotNull(insertedDataResource);
        assertEquals(insertedId, insertedDataResource.getId());
        assertEquals("FacilityX", insertedDataResource.getName());
    }

    @Test
    public void update_shouldUpdateDataResource() {
        DataResource dataResource = dataResourceService.get("1");
        assertNotNull(dataResource);
        dataResource.setName("FacilityX");
        DataResource resource1 = dataResourceService.update(dataResource);
        DataResource updatedResource = dataResourceService.get("1");
        assertNotNull(updatedResource);
        assertEquals(resource1.getName(), updatedResource.getName());
    }

    @Test
    public void save_shouldSaveDataResource() {
        List<DataResource> dataResources = dataResourceService.getAll();
        dataResourceService.deleteAll(dataResources);
        DataResource newDataResource = new DataResource();
        newDataResource.setName("FacilityX");
        DataResource savedDataResource = dataResourceService.save(newDataResource);
        DataResource updatedDataResource = dataResourceService.get(savedDataResource.getId());
        assertNotNull(updatedDataResource);
        assertEquals(savedDataResource.getName(), updatedDataResource.getName());
    }

}
