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

@RunWith(MockitoJUnitRunner.class)
public class InventoryLotServiceImplEquipmentLotTest {

    @InjectMocks
    private InventoryLotServiceImpl inventoryLotService;

    @Mock
    private InventoryLotDAO inventoryLotDAO;

    @Mock
    private InventoryTransactionService transactionService;

    @Mock
    private InventoryItemService inventoryItemService;

    private InventoryLot lot;
    private InventoryItem equipmentItem;

    @Before
    public void setUp() {
        equipmentItem = new InventoryItem();
        equipmentItem.setId(42L);
        equipmentItem.setItemType(ItemType.EQUIPMENT);
        equipmentItem.setName("Centrifuge");

        lot = new InventoryLot();
        lot.setInventoryItem(equipmentItem);
        lot.setLotNumber("EQ-001");
    }

    @Test
    public void insertRejectsEquipmentCatalogItem() {
        try {
            inventoryLotService.insert(lot);
            fail("Expected equipment lot receive to be rejected");
        } catch (IllegalArgumentException e) {
            assertEquals(InventoryLotServiceImpl.EQUIPMENT_LOT_RECEIVE_MESSAGE, e.getMessage());
        }
    }

    @Test
    public void insertRejectsEquipmentWhenOnlyItemIdProvided() {
        InventoryItem stub = new InventoryItem();
        stub.setId(42L);
        lot.setInventoryItem(stub);
        when(inventoryItemService.get(42L)).thenReturn(equipmentItem);

        try {
            inventoryLotService.insert(lot);
            fail("Expected equipment lot receive to be rejected");
        } catch (IllegalArgumentException e) {
            assertEquals(InventoryLotServiceImpl.EQUIPMENT_LOT_RECEIVE_MESSAGE, e.getMessage());
        }
    }
}
