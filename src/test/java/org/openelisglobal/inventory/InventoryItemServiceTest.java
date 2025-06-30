package org.openelisglobal.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;

public class InventoryItemServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryItemService inventoryItemService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/inventory-item.xml");

    }

    @Test
    public void getAll_shouldReturnAllInventoryItems() {
        List<InventoryItem> inventoryItems = inventoryItemService.getAll();
        assertNotNull(inventoryItems);
        assertEquals("1", inventoryItems.get(0).getId());
        assertEquals("2", inventoryItems.get(1).getId());
        assertEquals("3", inventoryItems.get(2).getId());

    }

    @Test
    public void getAllInventoryItems_shouldReturnAllInventoryItems() {
        List<InventoryItem> inventoryItems = inventoryItemService.getAllInventoryItems();
        assertNotNull(inventoryItems);
        assertEquals("1", inventoryItems.get(0).getId());
        assertEquals("2", inventoryItems.get(1).getId());
        assertEquals("3", inventoryItems.get(2).getId());
    }

    @Test
    public void readInventoryItem_shouldReturnInventoryItem() {
        InventoryItem inventoryItem = inventoryItemService.readInventoryItem("1");
        assertNotNull(inventoryItem);
        assertEquals("1", inventoryItem.getId());
        assertEquals("Gloves", inventoryItem.getName());
    }

    @Test
    public void getAllMatching_shouldReturnMatchingInventoryItems() {
        List<InventoryItem> inventoryItems = inventoryItemService.getAllMatching("name", "Gloves");
        assertNotNull(inventoryItems);
        assertEquals(1, inventoryItems.size());
        assertEquals("1", inventoryItems.get(0).getId());
        assertEquals("Gloves", inventoryItems.get(0).getName());
    }

    @Test
    public void getAllMatchingGiveMap_shouldReturnMatchingInventoryItems() {
        Map<String, Object> map = Map.of("name", "Gloves");
        List<InventoryItem> inventoryItems = inventoryItemService.getAllMatching(map);

        assertNotNull(inventoryItems);
        assertEquals(1, inventoryItems.size());
        assertEquals("1", inventoryItems.get(0).getId());
        assertEquals("Gloves", inventoryItems.get(0).getName());
    }

    @Test
    public void getAllOrdered_shouldReturnOrderedInventoryItems() {
        List<InventoryItem> inventoryItems = inventoryItemService.getAllOrdered("id", false);
        assertNotNull(inventoryItems);
        assertEquals(3, inventoryItems.size());
        assertEquals("1", inventoryItems.get(0).getId());
        assertEquals("2", inventoryItems.get(1).getId());
        assertEquals("3", inventoryItems.get(2).getId());
    }

    @Test
    public void getAllOrderedGivenList_shouldReturnOrderedInventoryItems() {
        List<String> list = List.of("id");
        List<InventoryItem> inventoryItems = inventoryItemService.getAllOrdered(list, false);
        assertNotNull(inventoryItems);
        assertEquals(3, inventoryItems.size());
        assertEquals("1", inventoryItems.get(0).getId());
        assertEquals("2", inventoryItems.get(1).getId());
        assertEquals("3", inventoryItems.get(2).getId());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnMatchingOrderedInventoryItems() {
        List<InventoryItem> inventoryItems = inventoryItemService.getAllMatchingOrdered("name", "Gloves", "id", false);
        assertNotNull(inventoryItems);
        assertEquals(1, inventoryItems.size());
        assertEquals("1", inventoryItems.get(0).getId());
        assertEquals("Gloves", inventoryItems.get(0).getName());
    }

    @Test
    public void getAllMatchingOrderedGivenList_shouldReturnMatchingOrderedInventoryItems() {
        List<String> list = List.of("id");
        List<InventoryItem> inventoryItems = inventoryItemService.getAllMatchingOrdered("name", "Gloves", list, false);
        assertNotNull(inventoryItems);
        assertEquals(1, inventoryItems.size());
        assertEquals("1", inventoryItems.get(0).getId());
        assertEquals("Gloves", inventoryItems.get(0).getName());
    }

    @Test
    public void getAllMatchingOrderedGivenMap_shouldReturnMatchingOrderedInventoryItems() {
        Map<String, Object> map = Map.of("name", "Gloves");
        List<InventoryItem> inventoryItems = inventoryItemService.getAllMatchingOrdered(map, "id", false);
        assertNotNull(inventoryItems);
        assertEquals(1, inventoryItems.size());
        assertEquals("1", inventoryItems.get(0).getId());
        assertEquals("Gloves", inventoryItems.get(0).getName());

    }

    @Test
    public void getAllMatchingOrderedGivenMapAndList_shouldReturnMatchingOrderedInventoryItems() {
        Map<String, Object> map = Map.of("name", "Gloves");
        List<String> list = List.of("id");
        List<InventoryItem> inventoryItems = inventoryItemService.getAllMatchingOrdered(map, list, false);
        assertNotNull(inventoryItems);
        assertEquals(1, inventoryItems.size());
        assertEquals("1", inventoryItems.get(0).getId());
        assertEquals("Gloves", inventoryItems.get(0).getName());

    }

    @Test
    public void getPage_shouldReturnPageOfInventoryItems() {
        List<InventoryItem> inventoryItems = inventoryItemService.getPage(1);
        int pageSize = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(inventoryItems.size() <= pageSize);
    }

    @Test
    public void getAllMatchingPage_shouldReturnPageOfMatchingInventoryItems() {
        List<InventoryItem> inventoryItems = inventoryItemService.getMatchingPage("name", "Gloves", 1);
        int pageSize = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(inventoryItems.size() <= pageSize);
    }

    @Test
    public void getAllMatchingPageGivenMap_shouldReturnPageOfMatchingInventoryItems() {
        Map<String, Object> map = Map.of("name", "Gloves");
        List<InventoryItem> inventoryItems = inventoryItemService.getMatchingPage(map, 1);
        int pageSize = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(inventoryItems.size() <= pageSize);
    }

    @Test
    public void getAllMatchingOrderedPage_shouldReturnPageOfMatchingOrderedInventoryItems() {
        List<InventoryItem> inventoryItems = inventoryItemService.getMatchingOrderedPage("name", "Gloves", "id", false,
                1);
        int pageSize = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(inventoryItems.size() <= pageSize);
    }

    @Test
    public void getAllMatchingOrderedPageGivenMap_shouldReturnPageOfMatchingOrderedInventoryItems() {
        Map<String, Object> map = Map.of("name", "Gloves");
        List<InventoryItem> inventoryItems = inventoryItemService.getMatchingOrderedPage(map, "id", false, 1);
        int pageSize = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(inventoryItems.size() <= pageSize);
    }

    @Test
    public void getAllMatchingOrderedPageGivenMapAndList_shouldReturnPageOfMatchingOrderedInventoryItems() {
        Map<String, Object> map = Map.of("name", "Gloves");
        List<String> list = List.of("id");
        List<InventoryItem> inventoryItems = inventoryItemService.getMatchingOrderedPage(map, list, false, 1);
        int pageSize = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(inventoryItems.size() <= pageSize);
    }

    @Test
    public void getAllMatchingOrderedPageGivenList_shouldReturnPageOfMatchingOrderedInventoryItems() {
        List<String> list = List.of("id");
        List<InventoryItem> inventoryItems = inventoryItemService.getMatchingOrderedPage("name", "Gloves", list, false,
                1);
        int pageSize = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(inventoryItems.size() <= pageSize);

    }

    @Test
    public void deleteAll_shouldDeleteAllInventoryItems() {
        List<InventoryItem> inventoryItems = inventoryItemService.getAll();
        inventoryItemService.deleteAll(inventoryItems);
        List<InventoryItem> inventoryItems2 = inventoryItemService.getAll();
        assertNotNull(inventoryItems);
        assertEquals(0, inventoryItems2.size());
    }

    @Test
    public void delete_shouldDeleteInventoryItem() {
        InventoryItem inventoryItem = inventoryItemService.readInventoryItem("1");
        assertNotNull(inventoryItem);
        inventoryItemService.delete(inventoryItem);
        List<InventoryItem> inventoryItems = inventoryItemService.getAll();
        assertNotNull(inventoryItems);
        assertEquals(2, inventoryItems.size());

    }

    @Test
    public void getNext_shouldReturnNextInventoryItem() {
        InventoryItem inventoryItem = inventoryItemService.getNext("1");
        assertNotNull(inventoryItem);
        assertEquals("2", inventoryItem.getId());
    }

    @Test
    public void getPrevious_shouldReturnPreviousInventoryItem() {
        InventoryItem inventoryItem = inventoryItemService.getPrevious("2");
        assertNotNull(inventoryItem);
        assertEquals("1", inventoryItem.getId());
    }

    @Test
    public void update_shouldUpdateInventoryItem() {
        InventoryItem inventoryItem = inventoryItemService.readInventoryItem("1");
        assertNotNull(inventoryItem);
        inventoryItem.setName("Updated Gloves");
        inventoryItemService.update(inventoryItem);
        InventoryItem updatedInventoryItem = inventoryItemService.readInventoryItem("1");
        assertNotNull(updatedInventoryItem);
        assertEquals("Updated Gloves", updatedInventoryItem.getName());
    }

    @Test
    public void insert_shouldInsertInventoryItem() {
        List<InventoryItem> inventorys = inventoryItemService.getAll();
        inventoryItemService.deleteAll(inventorys);
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setName("New Gloves");
        String inventId = inventoryItemService.insert(inventoryItem);
        List<InventoryItem> inventoryItems = inventoryItemService.getAll();
        assertNotNull(inventoryItems);
        assertEquals(1, inventoryItems.size());
        assertEquals(inventId, inventoryItems.get(0).getId());
    }

    @Test
    public void save_shouldSaveInventoryItem() {
        List<InventoryItem> inventorys = inventoryItemService.getAll();
        inventoryItemService.deleteAll(inventorys);
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setName("Saved Gloves");
        InventoryItem inventoryItem2 = inventoryItemService.save(inventoryItem);
        List<InventoryItem> inventoryItems = inventoryItemService.getAll();
        assertNotNull(inventoryItems);
        assertEquals(1, inventoryItems.size());
        assertEquals(inventoryItem2.getName(), inventoryItems.get(0).getName());
    }

    @Test
    public void getCount_shouldReturnCountOfInventoryItems() {
        int count = inventoryItemService.getCount();
        assertEquals(3, count);
    }

    @Test
    public void getCountLike_shouldReturnCountOfMatchingInventoryItems() {
        int count = inventoryItemService.getCountLike("name", "Gloves");
        assertEquals(1, count);
    }

}
