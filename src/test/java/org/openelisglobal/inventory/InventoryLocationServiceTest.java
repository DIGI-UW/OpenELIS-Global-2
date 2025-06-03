package org.openelisglobal.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.inventory.service.InventoryLocationService;
import org.openelisglobal.inventory.valueholder.InventoryLocation;
import org.springframework.beans.factory.annotation.Autowired;

public class InventoryLocationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryLocationService inventoryLocationService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/inventory-location.xml");
        // Initialize any required data or configurations here
    }

    @Test
    public void getAll_shouldReturnAllLocations() {
        // Call the method to test
        List<InventoryLocation> locations = inventoryLocationService.getAll();
        assertEquals(3, locations.size());
        assertEquals("1", locations.get(0).getId());
        assertEquals("2", locations.get(1).getId());
        assertEquals("3", locations.get(2).getId());

    }

    @Test
    public void getAllMatching_shouldReturnMatchingLocations() {
        // Call the method to test
        List<InventoryLocation> locations = inventoryLocationService.getAllMatching("lotNumber", "LOT-A123");
        assertEquals(1, locations.size());
        assertEquals("1", locations.get(0).getId());
    }

    @Test
    public void getAllMatchingGivenMap_shouldReturnMatchingLocations() {
        Map<String, Object> map = Map.of("lotNumber", "LOT-A123");
        List<InventoryLocation> locations = inventoryLocationService.getAllMatching(map);
        assertEquals(1, locations.size());
        assertEquals("1", locations.get(0).getId());
    }

    @Test
    public void getAllOdered_shouldReturnAllOrdered() {
        List<InventoryLocation> locations = inventoryLocationService.getAllOrdered("id", false);
        assertEquals(3, locations.size());
        assertEquals("1", locations.get(0).getId());
        assertEquals("2", locations.get(1).getId());
        assertEquals("3", locations.get(2).getId());
    }

    @Test
    public void getAllOrderedGivenList_shouldReturnAllOrdered() {
        List<String> list = List.of("id");
        List<InventoryLocation> locations = inventoryLocationService.getAllOrdered(list, false);
        assertEquals(3, locations.size());
        assertEquals("1", locations.get(0).getId());
        assertEquals("2", locations.get(1).getId());
        assertEquals("3", locations.get(2).getId());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnMatchingOrderedLocations() {
        List<InventoryLocation> locations = inventoryLocationService.getAllMatchingOrdered("lotNumber", "LOT-A123",
                "id", false);
        assertEquals(1, locations.size());
        assertEquals("1", locations.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenMap_shouldReturnMatchingOrderedLocations() {
        Map<String, Object> map = Map.of("lotNumber", "LOT-A123");
        List<InventoryLocation> locations = inventoryLocationService.getAllMatchingOrdered(map, "id", false);
        assertEquals(1, locations.size());
        assertEquals("1", locations.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenListAndMap_shouldReturnMatchingOrderedLocations() {
        List<String> list = List.of("id");
        Map<String, Object> map = Map.of("lotNumber", "LOT-A123");
        List<InventoryLocation> locations = inventoryLocationService.getAllMatchingOrdered(map, list, false);
        assertEquals(1, locations.size());
        assertEquals("1", locations.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenList_shouldReturnMatchingOrderedLocations() {
        List<String> list = List.of("id");
        List<InventoryLocation> locations = inventoryLocationService.getAllMatchingOrdered("lotNumber", "LOT-A123",
                list, false);
        assertEquals(1, locations.size());
        assertEquals("1", locations.get(0).getId());
    }

    @Test
    public void getPage_shouldReturnPageOfLocations() {
        // Call the method to test
        List<InventoryLocation> locations = inventoryLocationService.getPage(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= locations.size());

    }

    @Test
    public void getMatchingPage_shouldReturnMatchingPageOfLocations() {
        // Call the method to test
        List<InventoryLocation> locations = inventoryLocationService.getMatchingPage("lotNumber", "LOT-A123", 1);
        assertEquals(1, locations.size());
        assertEquals("1", locations.get(0).getId());
    }

    @Test
    public void getMatchingPageGivenMap_shouldReturnMatchingPageOfLocations() {
        Map<String, Object> map = Map.of("lotNumber", "LOT-A123");
        List<InventoryLocation> locations = inventoryLocationService.getMatchingPage(map, 1);
        assertEquals(1, locations.size());
        assertEquals("1", locations.get(0).getId());
    }

    @Test
    public void getMatchingOrderedPage_shouldReturnMatchingOrderedPageOfLocations() {
        List<InventoryLocation> locations = inventoryLocationService.getMatchingOrderedPage("lotNumber", "LOT-A123",
                "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= locations.size());
    }

    @Test
    public void getMatchingOrderedPageGivenMap_shouldReturnMatchingOrderedPageOfLocations() {
        Map<String, Object> map = Map.of("lotNumber", "LOT-A123");
        List<InventoryLocation> locations = inventoryLocationService.getMatchingOrderedPage(map, "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= locations.size());

    }

    @Test
    public void getMatchingOrderedPageGivenList_shouldReturnMatchingOrderedPageOfLocations() {
        List<String> list = List.of("id");
        List<InventoryLocation> locations = inventoryLocationService.getMatchingOrderedPage("lotNumber", "LOT-A123",
                list, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= locations.size());
    }

    @Test
    public void getMatchingOrderedPageGivenListAndMap_shouldReturnMatchingOrderedPageOfLocations() {
        List<String> list = List.of("id");
        Map<String, Object> map = Map.of("lotNumber", "LOT-A123");
        List<InventoryLocation> locations = inventoryLocationService.getMatchingOrderedPage(map, list, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(expectedPages >= locations.size());
    }

    @Test
    public void getCount_shouldReturnCountOfLocations() {
        int count = inventoryLocationService.getCount();
        assertEquals(3, count);
    }

    @Test
    public void getCountLike_shouldReturnCountOfMatchingLocations() {
        int count = inventoryLocationService.getCountLike("lotNumber", "LOT-A123");
        assertEquals(1, count);
    }

    @Test
    public void deleteAllShouldDeleteAllLocations() {
        List<InventoryLocation> locations2 = inventoryLocationService.getAll();
        inventoryLocationService.deleteAll(locations2);
        List<InventoryLocation> locations = inventoryLocationService.getAll();
        assertEquals(0, locations.size());
    }

    @Test
    public void deleteShouldDeleteLocation() {
        InventoryLocation location = inventoryLocationService.get("1");
        inventoryLocationService.delete(location);
        List<InventoryLocation> locations = inventoryLocationService.getAll();
        assertEquals(2, locations.size());
    }

    @Test
    public void get_shouldReturnLocationGivenId() {
        InventoryLocation location = inventoryLocationService.get("1");
        assertEquals("1", location.getId());
        assertEquals("LOT-A123", location.getLotNumber());
    }

    @Test
    public void save_shouldSaveLocation() {
        List<InventoryLocation> locations1 = inventoryLocationService.getAll();
        inventoryLocationService.deleteAll(locations1);
        InventoryLocation location = new InventoryLocation();
        location.setLotNumber("LOT-A456");
        InventoryLocation savedLocation = inventoryLocationService.save(location);
        List<InventoryLocation> locations = inventoryLocationService.getAll();
        assertEquals(1, locations.size());
        assertEquals(savedLocation.getId(), locations.get(0).getId());
        assertEquals(savedLocation.getLotNumber(), locations.get(0).getLotNumber());
    }

    @Test
    public void insert_shouldInsertLocation() {
        List<InventoryLocation> locations1 = inventoryLocationService.getAll();
        inventoryLocationService.deleteAll(locations1);
        InventoryLocation location = new InventoryLocation();
        location.setLotNumber("LOT-A456");
        String savedLocation = inventoryLocationService.insert(location);
        List<InventoryLocation> locations = inventoryLocationService.getAll();
        assertEquals(1, locations.size());
        assertEquals(savedLocation, locations.get(0).getId());

    }

    @Test
    public void update_shouldUpdateLocation() {
        InventoryLocation location = inventoryLocationService.get("1");
        location.setLotNumber("LOT-A789");
        inventoryLocationService.update(location);
        InventoryLocation updatedLocation = inventoryLocationService.get("1");
        assertEquals("LOT-A789", updatedLocation.getLotNumber());
    }

    @Test
    public void getNext_shouldReturnNextLocation() {
        InventoryLocation location = inventoryLocationService.getNext("1");
        assertEquals("2", location.getId());
    }

    @Test
    public void getPrevious_shouldReturnPreviousLocation() {
        InventoryLocation location = inventoryLocationService.getPrevious("2");
        assertEquals("1", location.getId());
    }

}
