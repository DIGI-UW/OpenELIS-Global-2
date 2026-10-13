package org.openelisglobal.inventory.projection;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.projection.InventoryProjection.BoardStatus;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryOrderCycleService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryUsage;

@RunWith(MockitoJUnitRunner.class)
public class InventoryProjectionServiceQueryCountTest {

    private static final int ITEM_COUNT = 25;
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    @Mock
    private InventoryItemService inventoryItemService;

    @Mock
    private InventoryOrderCycleService inventoryOrderCycleService;

    @Mock
    private InventoryLotDAO inventoryLotDAO;

    @Mock
    private InventoryUsageService inventoryUsageService;

    @InjectMocks
    private InventoryProjectionServiceImpl service;

    private InventoryItem item(long id) {
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setCode("ITEM_" + id);
        item.setName("Item " + id);
        item.setItemType(ItemType.REAGENT);
        item.setUnits("tests");
        item.setLowStockThreshold(5);
        return item;
    }

    private InventoryLot usableLot(InventoryItem item) {
        InventoryLot lot = new InventoryLot();
        lot.setId(item.getId() * 10);
        lot.setInventoryItem(item);
        lot.setCurrentQuantity(40.0);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setExpirationDate(new Timestamp(System.currentTimeMillis() + 365 * DAY_MILLIS));
        return lot;
    }

    private InventoryUsage usage(InventoryItem item, InventoryLot lot, int daysAgo) {
        InventoryUsage usage = new InventoryUsage();
        usage.setInventoryItem(item);
        usage.setLot(lot);
        usage.setQuantityUsed(2.0);
        usage.setUsageDate(new Timestamp(System.currentTimeMillis() - daysAgo * DAY_MILLIS));
        return usage;
    }

    @Test
    public void theBoardCostsThreeReadsWhateverTheCatalogSize() {
        List<InventoryItem> items = new ArrayList<>();
        Map<Long, Double> onHand = new HashMap<>();
        List<InventoryUsage> usages = new ArrayList<>();
        for (long id = 1; id <= ITEM_COUNT; id++) {
            InventoryItem item = item(id);
            InventoryLot lot = usableLot(item);
            items.add(item);
            onHand.put(id, lot.getCurrentQuantity());
            for (int daysAgo = 1; daysAgo <= 20; daysAgo++) {
                usages.add(usage(item, lot, daysAgo));
            }
        }
        when(inventoryItemService.getAllActive()).thenReturn(items);
        when(inventoryLotDAO.getAvailableQuantityByItem()).thenReturn(onHand);
        when(inventoryUsageService.getByDateRange(any(), any())).thenReturn(usages);

        List<InventoryProjection> board = service.getBoard();

        assertEquals(ITEM_COUNT, board.size());
        verify(inventoryItemService, times(1)).getAllActive();
        verify(inventoryLotDAO, times(1)).getAvailableQuantityByItem();
        verify(inventoryUsageService, times(1)).getByDateRange(any(), any());
        verifyNoMoreInteractions(inventoryLotDAO);
        verify(inventoryUsageService, never()).getByInventoryItemId(anyLong());
    }

    @Test
    public void usageIsAttributedToItsOwnItemAndNotSharedAcrossTheCatalog() {
        InventoryItem busy = item(1);
        InventoryItem quiet = item(2);
        InventoryLot busyLot = usableLot(busy);

        List<InventoryUsage> usages = new ArrayList<>();
        for (int daysAgo = 1; daysAgo <= 28; daysAgo++) {
            usages.add(usage(busy, busyLot, daysAgo));
        }
        when(inventoryItemService.getAllActive()).thenReturn(List.of(busy, quiet));
        when(inventoryLotDAO.getAvailableQuantityByItem()).thenReturn(Map.of(1L, 40.0, 2L, 40.0));
        when(inventoryUsageService.getByDateRange(any(), any())).thenReturn(usages);

        List<InventoryProjection> board = service.getBoard();
        InventoryProjection busyRow = board.stream().filter(row -> row.getItemId() == 1L).findFirst().orElseThrow();
        InventoryProjection quietRow = board.stream().filter(row -> row.getItemId() == 2L).findFirst().orElseThrow();

        assertEquals(2.0, busyRow.getMedianDailyUse(), 0.0001);
        assertEquals("the quiet item must not inherit its neighbour's consumption", 0.0, quietRow.getMedianDailyUse(),
                0.0001);
        assertEquals(BoardStatus.BUILDING_DATA, quietRow.getStatus());
    }
}
