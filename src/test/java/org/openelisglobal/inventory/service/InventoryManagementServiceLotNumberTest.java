package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;

/**
 * The receive form lets the lot number be left blank, so the server has to fill
 * one in from the item's code. A manufacturer-supplied lot number must survive
 * exactly as typed.
 */
@RunWith(MockitoJUnitRunner.class)
public class InventoryManagementServiceLotNumberTest {

    @Mock
    private InventoryItemService inventoryItemService;

    @Mock
    private InventoryLotService inventoryLotService;

    @Mock
    private InventoryTransactionService transactionService;

    @InjectMocks
    private InventoryManagementServiceImpl inventoryManagementService;

    private InventoryItem item;

    @Before
    public void setup() {
        item = new InventoryItem();
        item.setId(1000L);
        item.setCode("TAQ_DNA_POLYMERASE");
        item.setName("Taq DNA Polymerase");
        item.setUnits("mL");
        item.setItemType(ItemType.REAGENT);
        when(inventoryItemService.get(anyLong())).thenReturn(item);
        when(inventoryLotService.save(any(InventoryLot.class))).thenAnswer(invocation -> {
            InventoryLot saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
    }

    private InventoryLot lotWithNumber(String lotNumber) {
        InventoryLot lot = new InventoryLot();
        lot.setInventoryItem(item);
        lot.setLotNumber(lotNumber);
        lot.setInitialQuantity(10.0);
        lot.setCurrentQuantity(10.0);
        return lot;
    }

    private String today() {
        return new SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
    }

    @Test
    public void receiveInventory_generatesLotNumberFromItemCode_whenBlank() {
        when(inventoryLotService.getByInventoryItemId(1000L)).thenReturn(Collections.emptyList());

        InventoryLot received = inventoryManagementService.receiveInventory(lotWithNumber(null), "1");

        assertEquals("TAQ_DNA_POLYMERASE_" + today(), received.getLotNumber());
    }

    @Test
    public void receiveInventory_generatesLotNumber_whenEmptyString() {
        when(inventoryLotService.getByInventoryItemId(1000L)).thenReturn(Collections.emptyList());

        InventoryLot received = inventoryManagementService.receiveInventory(lotWithNumber("   "), "1");

        assertEquals("TAQ_DNA_POLYMERASE_" + today(), received.getLotNumber());
    }

    @Test
    public void receiveInventory_suffixesGeneratedLotNumber_whenTodaysAlreadyExists() {
        InventoryLot existing = lotWithNumber("TAQ_DNA_POLYMERASE_" + today());
        when(inventoryLotService.getByInventoryItemId(1000L)).thenReturn(List.of(existing));

        InventoryLot received = inventoryManagementService.receiveInventory(lotWithNumber(null), "1");

        assertTrue("A second lot received today must not reuse the first one's number",
                received.getLotNumber().endsWith("_2"));
    }

    @Test
    public void receiveInventory_leavesSuppliedLotNumberExactlyAsTyped() {
        InventoryLot received = inventoryManagementService.receiveInventory(lotWithNumber("ab-123/xy"), "1");

        assertEquals("A manufacturer's lot number must not be normalised", "ab-123/xy", received.getLotNumber());
    }
}
