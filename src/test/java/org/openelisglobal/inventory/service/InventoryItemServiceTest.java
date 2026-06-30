package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;

public class InventoryItemServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryItemService inventoryItemService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
    }

    @Test
    public void getAll_validCall_returnsNonNullList() {
        List<InventoryItem> items = inventoryItemService.getAll();
        assertNotNull(items);
    }

    @Test
    public void getAll_validCall_returnsItems() {
        List<InventoryItem> items = inventoryItemService.getAll();
        assertNotNull(items);
        assertTrue(items.size() >= 1);
    }

    @Test
    public void getAllActive_validCall_returnsActiveItems() {
        List<InventoryItem> items = inventoryItemService.getAllActive();
        assertNotNull(items);
        assertTrue(items.size() >= 1);
    }

    @Test
    public void getAllItemTypes_validCall_returnsNonNullList() {
        List<?> types = inventoryItemService.getAllItemTypes();
        assertNotNull(types);
    }
}
