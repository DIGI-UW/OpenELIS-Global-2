package org.openelisglobal.inventory.service;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

@Rollback
public class InventoryItemServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    InventoryItemService inventoryItemService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
    }

    @Test
    public void getAll_shouldReturnNonNullList() {
        List<InventoryItem> items = inventoryItemService.getAll();
        assertNotNull(items);
    }

    @Test
    public void getAll_shouldReturnAtLeastOneItem() {
        List<InventoryItem> items = inventoryItemService.getAll();
        assertTrue(items.size() >= 1);
    }

    @Test
    public void getAllActive_shouldReturnNonNullList() {
        List<InventoryItem> items = inventoryItemService.getAllActive();
        assertNotNull(items);
    }

    @Test
    public void getAllActive_shouldReturnAtLeastOneActiveItem() {
        List<InventoryItem> items = inventoryItemService.getAllActive();
        assertTrue(items.size() >= 1);
    }

    @Test
    public void getAllItemTypes_shouldReturnNonNullList() {
        List<?> types = inventoryItemService.getAllItemTypes();
        assertNotNull(types);
    }

    @Test
    public void insert_shouldPersistNewInventoryItem() {
        InventoryItem newItem = new InventoryItem();
        newItem.setName("Test Item Created");
        newItem.setIsActive("Y");
        newItem.setFhirUuid(java.util.UUID.randomUUID());

        Long insertedId = inventoryItemService.insert(newItem);
        assertNotNull(insertedId);

        InventoryItem savedItem = inventoryItemService.get(insertedId);
        assertNotNull(savedItem);
        assertEquals("Test Item Created", savedItem.getName());
    }
}
