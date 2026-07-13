package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;

/**
 * Lot number can be auto-generated (from the item code + today's date,
 * collision-suffixed) or typed explicitly on the "Receive New Inventory Lot"
 * form, mirroring the auto-generate-or-type pattern already used for
 * inventory_item.code / inventory_item_type.code.
 */
@RunWith(MockitoJUnitRunner.class)
public class InventoryManagementServiceLotNumberTest {

    @Mock
    private InventoryItemService inventoryItemService;
    @Mock
    private InventoryLotService inventoryLotService;
    @Mock
    private InventoryTransactionService transactionService;
    @Mock
    private InventoryUsageService usageService;

    @InjectMocks
    private InventoryManagementServiceImpl managementService;

    private InventoryItem item(String id) {
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setName("Reagent A");
        item.setItemType("REAGENT");
        item.setUnits("mL");
        return item;
    }

    private InventoryLot newLot(String itemId, String lotNumber) {
        InventoryLot lot = new InventoryLot();
        InventoryItem lotItem = new InventoryItem();
        lotItem.setId(itemId);
        lot.setInventoryItem(lotItem);
        lot.setLotNumber(lotNumber);
        lot.setCurrentQuantity(10.0);
        lot.setInitialQuantity(10.0);
        return lot;
    }

    private void stubSave() {
        when(inventoryLotService.save(any(InventoryLot.class))).thenAnswer(invocation -> {
            InventoryLot lot = invocation.getArgument(0);
            lot.setId(1L);
            return lot;
        });
    }

    @Test
    public void receiveInventory_generatesLotNumber_whenBlank() {
        when(inventoryItemService.get("REAGENT_A")).thenReturn(item("REAGENT_A"));
        when(inventoryLotService.getByInventoryItemId("REAGENT_A")).thenReturn(List.of());
        stubSave();

        InventoryLot result = managementService.receiveInventory(newLot("REAGENT_A", ""), "1");

        assertEquals(true, result.getLotNumber().startsWith("REAGENT_A_"));
    }

    @Test
    public void receiveInventory_generatesLotNumber_whenNull() {
        when(inventoryItemService.get("REAGENT_A")).thenReturn(item("REAGENT_A"));
        when(inventoryLotService.getByInventoryItemId("REAGENT_A")).thenReturn(List.of());
        stubSave();

        InventoryLot result = managementService.receiveInventory(newLot("REAGENT_A", null), "1");

        assertEquals(true, result.getLotNumber().startsWith("REAGENT_A_"));
    }

    @Test
    public void receiveInventory_keepsExplicitLotNumberAsTyped_notNormalized() {
        when(inventoryItemService.get("REAGENT_A")).thenReturn(item("REAGENT_A"));
        stubSave();

        InventoryLot result = managementService.receiveInventory(newLot("REAGENT_A", "abc-123/XY"), "1");

        // Explicit lot numbers are manufacturer-provided values — left exactly
        // as typed, unlike the UPPER_SNAKE codes CodeGenerator produces for
        // inventory_item.code / inventory_item_type.code.
        assertEquals("abc-123/XY", result.getLotNumber());
    }

    @Test
    public void receiveInventory_suffixesGeneratedLotNumber_onCollisionForSameItem() {
        when(inventoryItemService.get("REAGENT_A")).thenReturn(item("REAGENT_A"));
        stubSave();

        String firstGenerated = managementService.receiveInventory(newLot("REAGENT_A", null), "1").getLotNumber();

        when(inventoryLotService.getByInventoryItemId("REAGENT_A"))
                .thenReturn(List.of(newLot("REAGENT_A", firstGenerated)));

        String secondGenerated = managementService.receiveInventory(newLot("REAGENT_A", null), "1").getLotNumber();

        assertEquals(firstGenerated + "_2", secondGenerated);
    }
}
