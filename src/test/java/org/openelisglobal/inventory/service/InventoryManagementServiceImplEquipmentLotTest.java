package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class InventoryManagementServiceImplEquipmentLotTest {

    @InjectMocks
    private InventoryManagementServiceImpl inventoryManagementService;

    @InjectMocks
    private InventoryLotServiceImpl inventoryLotService;

    @Mock
    private InventoryItemService inventoryItemService;

    @Mock
    private InventoryLotDAO inventoryLotDAO;

    @Mock
    private InventoryTransactionService transactionService;

    private InventoryItem equipmentItem;
    private InventoryLot lot;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(inventoryManagementService, "inventoryLotService", inventoryLotService);

        equipmentItem = new InventoryItem();
        equipmentItem.setId(99L);
        equipmentItem.setItemType(ItemType.EQUIPMENT);
        equipmentItem.setName("Freezer");

        lot = new InventoryLot();
        lot.setInventoryItem(equipmentItem);
        lot.setLotNumber("LOT-1");
        lot.setCurrentQuantity(1.0);

        when(inventoryItemService.get(99L)).thenReturn(equipmentItem);
    }

    @Test
    public void receiveInventoryRejectsEquipmentCatalogItem() {
        try {
            inventoryManagementService.receiveInventory(lot, "1");
            fail("Expected equipment lot receive to be rejected");
        } catch (IllegalArgumentException e) {
            assertEquals(InventoryLotServiceImpl.EQUIPMENT_LOT_RECEIVE_MESSAGE, e.getMessage());
        }
    }
}
