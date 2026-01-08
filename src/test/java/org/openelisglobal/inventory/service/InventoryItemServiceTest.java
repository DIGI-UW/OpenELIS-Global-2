package org.openelisglobal.inventory.service;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

@Rollback
public class InventoryItemServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    InventoryItemService inventoryItemService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
    }

    @Test
    public void get_shouldReturnInventoryItemWhenExists() {
        InventoryItem item = inventoryItemService.get(1000L);

        assertNotNull("Item should be loaded from dataset", item);
        assertEquals("Test Reagent A", item.getName());
        assertEquals(ItemType.REAGENT, item.getItemType());
        assertEquals("Y", item.getIsActive());
    }

    @Test
    public void get_shouldReturnCorrectItemDetails() {
        InventoryItem item = inventoryItemService.get(1001L);

        assertNotNull("Should find test item 2", item);
        assertEquals("Test RDT Kit", item.getName());
        assertEquals(ItemType.RDT, item.getItemType());
        assertEquals("QC", item.getCategory());
    }

    @Test
    public void getAllActive_shouldReturnActiveItemsFromDataset() {
        List<InventoryItem> activeItems = inventoryItemService.getAllActive();

        assertNotNull("Active items should not be null", activeItems);
        assertTrue("Should have at least 2 active items", activeItems.size() >= 2);
    }

    @Test
    @org.junit.Ignore("Optimistic lock issue - needs investigation")
    public void update_shouldUpdateInventoryItem() {
        InventoryItem item = inventoryItemService.get(1000L);
        item.setDescription("Updated description for testing");

        InventoryItem updatedItem = inventoryItemService.update(item);

        assertNotNull("Updated item should not be null", updatedItem);
        assertEquals("Updated description for testing", updatedItem.getDescription());
    }

    @Test
    public void getTotalCurrentStock_shouldCalculateStockFromLots() {
        Double totalStock = inventoryItemService.getTotalCurrentStock(1000L);

        assertNotNull("Total stock should not be null", totalStock);
        assertEquals("Total stock should be sum of all lots", Double.valueOf(150.0), totalStock);
    }

    @Test
    public void createInventoryItem_shouldInsertNewItem() {
        InventoryItem newItem = new InventoryItem();
        newItem.setName("Test Item Created");
        newItem.setItemType(ItemType.RDT);
        newItem.setUnits("pieces");
        newItem.setIsActive("Y");
        newItem.setFhirUuid(java.util.UUID.randomUUID());

        Long insertedId = inventoryItemService.insert(newItem);

        assertNotNull("Inserted ID should not be null", insertedId);

        InventoryItem savedItem = inventoryItemService.get(insertedId);
        assertNotNull("Saved item should be retrievable", savedItem);
        assertEquals("Test Item Created", savedItem.getName());
    }
}
