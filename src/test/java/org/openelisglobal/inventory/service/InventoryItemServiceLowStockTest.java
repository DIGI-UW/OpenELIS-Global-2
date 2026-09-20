package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;

/**
 * Low stock is {@link InventoryLot#countsAsAvailableStock()} strictly below the
 * threshold, for the dashboard tile, the alerts endpoint and the report alike.
 */
@RunWith(MockitoJUnitRunner.class)
public class InventoryItemServiceLowStockTest {

    @Mock
    private InventoryItemDAO inventoryItemDAO;

    @Mock
    private InventoryLotDAO inventoryLotDAO;

    @InjectMocks
    private InventoryItemServiceImpl inventoryItemService;

    private InventoryItem item(Long id, String code, Integer threshold) {
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setCode(code);
        item.setName(code);
        item.setItemType(ItemType.REAGENT);
        item.setUnits("mL");
        item.setLowStockThreshold(threshold);
        return item;
    }

    private InventoryLot lot(double currentQuantity, LotStatus status, QCStatus qcStatus) {
        InventoryLot lot = new InventoryLot();
        lot.setCurrentQuantity(currentQuantity);
        lot.setInitialQuantity(currentQuantity);
        lot.setStatus(status);
        lot.setQcStatus(qcStatus);
        return lot;
    }

    @Test
    public void getLowStockItems_flagsItem_whenAvailableQuantityBelowThreshold() {
        InventoryItem lowItem = item(1000L, "REAGENT_A", 20);
        when(inventoryItemDAO.getAllActive()).thenReturn(List.of(lowItem));
        // 3 usable + 100 disposed = 103 total, but only 3 usable — a raw-total
        // check would miss this as "well stocked".
        when(inventoryLotDAO.getByInventoryItemId(1000L)).thenReturn(
                List.of(lot(3.0, LotStatus.ACTIVE, QCStatus.PASSED), lot(100.0, LotStatus.DISPOSED, QCStatus.PASSED)));

        List<InventoryItem> result = inventoryItemService.getLowStockItems();

        assertEquals(1, result.size());
        assertEquals("REAGENT_A", result.get(0).getCode());
    }

    @Test
    public void getLowStockItems_excludesItem_whenAvailableQuantityAboveThreshold() {
        InventoryItem wellStocked = item(1001L, "REAGENT_B", 20);
        when(inventoryItemDAO.getAllActive()).thenReturn(List.of(wellStocked));
        when(inventoryLotDAO.getByInventoryItemId(1001L))
                .thenReturn(List.of(lot(50.0, LotStatus.ACTIVE, QCStatus.PASSED)));

        List<InventoryItem> result = inventoryItemService.getLowStockItems();

        assertTrue("well-stocked item should not be flagged", result.isEmpty());
    }

    @Test
    public void getLowStockItems_ignoresExpiredQuarantinedAndFailedQcLots() {
        InventoryItem item = item(1002L, "REAGENT_C", 10);
        when(inventoryItemDAO.getAllActive()).thenReturn(List.of(item));
        when(inventoryLotDAO.getByInventoryItemId(1002L)).thenReturn(List.of(
                lot(30.0, LotStatus.EXPIRED, QCStatus.PASSED), lot(30.0, LotStatus.QUARANTINED, QCStatus.PASSED),
                lot(30.0, LotStatus.ACTIVE, QCStatus.FAILED)));

        List<InventoryItem> result = inventoryItemService.getLowStockItems();

        // Available quantity is 0 (none of these lots are usable) — below the
        // threshold of 10, so the item IS flagged despite 90 units of total stock.
        assertEquals(1, result.size());
    }

    @Test
    public void getLowStockItems_countsStockAwaitingQcAsOnTheShelf() {
        InventoryItem awaitingQc = item(1004L, "REAGENT_E", 20);
        when(inventoryItemDAO.getAllActive()).thenReturn(List.of(awaitingQc));
        when(inventoryLotDAO.getByInventoryItemId(1004L))
                .thenReturn(List.of(lot(500.0, LotStatus.ACTIVE, QCStatus.PENDING)));

        List<InventoryItem> result = inventoryItemService.getLowStockItems();

        assertTrue("500 units awaiting QC are on the shelf, not a reorder trigger", result.isEmpty());
    }

    @Test
    public void getLowStockItems_doesNotFlagItemExactlyAtThreshold() {
        InventoryItem atThreshold = item(1005L, "REAGENT_F", 10);
        when(inventoryItemDAO.getAllActive()).thenReturn(List.of(atThreshold));
        when(inventoryLotDAO.getByInventoryItemId(1005L))
                .thenReturn(List.of(lot(10.0, LotStatus.ACTIVE, QCStatus.PASSED)));

        List<InventoryItem> result = inventoryItemService.getLowStockItems();

        assertTrue("low stock means strictly below the threshold", result.isEmpty());
    }

    @Test
    public void getLowStockItems_neverFlagsThresholdZero() {
        InventoryItem noAlert = item(1006L, "REAGENT_G", 0);
        when(inventoryItemDAO.getAllActive()).thenReturn(List.of(noAlert));
        when(inventoryLotDAO.getByInventoryItemId(1006L)).thenReturn(List.of());

        List<InventoryItem> result = inventoryItemService.getLowStockItems();

        assertTrue("threshold 0 means 'no alert for this item'", result.isEmpty());
    }

    @Test
    public void getLowStockItems_skipsItemsWithNoThresholdSet() {
        InventoryItem noThreshold = item(1003L, "REAGENT_D", null);
        when(inventoryItemDAO.getAllActive()).thenReturn(List.of(noThreshold));

        List<InventoryItem> result = inventoryItemService.getLowStockItems();

        assertTrue(result.isEmpty());
    }
}
