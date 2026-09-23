package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.projection.InventoryProjection;
import org.openelisglobal.inventory.projection.InventoryProjection.BoardStatus;
import org.openelisglobal.inventory.projection.InventoryProjectionService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Reorder suggestions are whatever the board already calls REORDER_NOW or
 * REORDER_SOON — there is deliberately no second rule to keep in step. These
 * cases pin that the two agree, including the part that is easy to get wrong:
 * two items with the same projection but different lead times are not both
 * suggestions, because each item is judged against its own resupply time.
 *
 * <p>
 * They also pin that marking an item as ordered records the stamp without
 * hiding the row or changing its status. An item that has been ordered is still
 * short of stock; the only thing that changes is that the page stops demanding
 * attention it has already been given.
 */
public class InventoryOrderingStateIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryUsageService inventoryUsageService;

    @Autowired
    private InventoryProjectionService inventoryProjectionService;

    private static final String SYS_USER_ID = "1";

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    private Long createItem(String name, Integer threshold, Integer leadTimeDays) {
        InventoryItem item = new InventoryItem();
        item.setFhirUuid(UUID.randomUUID());
        item.setName(name + " " + UUID.randomUUID());
        item.setUnits("tests");
        item.setLowStockThreshold(threshold);
        item.setLeadTimeDays(leadTimeDays);
        item.setIsActive("Y");
        item.setSysUserId(SYS_USER_ID);
        return inventoryItemService.insert(item);
    }

    private Long stock(Long itemId, double quantity) {
        InventoryLot lot = new InventoryLot();
        lot.setFhirUuid(UUID.randomUUID());
        lot.setInventoryItem(inventoryItemService.get(itemId));
        lot.setLotNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8));
        lot.setExpirationDate(Timestamp.valueOf(LocalDateTime.now().plusDays(365)));
        lot.setReceiptDate(Timestamp.valueOf(LocalDateTime.now().minusDays(60)));
        lot.setInitialQuantity(quantity);
        lot.setCurrentQuantity(quantity);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setSysUserId(SYS_USER_ID);
        return inventoryLotService.insert(lot);
    }

    /**
     * Steady daily consumption across the whole trailing window. Usage rows carry a
     * non-null lot, so they hang off the item's stock rather than floating.
     */
    private void steadyUsage(Long itemId, Long lotId, double perDay, int days) {
        for (int dayOffset = 1; dayOffset <= days; dayOffset++) {
            InventoryUsage usage = new InventoryUsage();
            usage.setInventoryItem(inventoryItemService.get(itemId));
            usage.setLot(inventoryLotService.get(lotId));
            usage.setQuantityUsed(perDay);
            usage.setUsageDate(Timestamp.valueOf(LocalDateTime.now().minusDays(dayOffset)));
            usage.setPerformedByUser(Integer.valueOf(SYS_USER_ID));
            usage.setSysUserId(SYS_USER_ID);
            inventoryUsageService.insert(usage);
        }
    }

    private Map<Long, InventoryProjection> boardByItemId() {
        return inventoryProjectionService.getBoard().stream()
                .collect(Collectors.toMap(InventoryProjection::getItemId, Function.identity()));
    }

    private static boolean isSuggestion(InventoryProjection row) {
        return row.getStatus() == BoardStatus.REORDER_NOW || row.getStatus() == BoardStatus.REORDER_SOON;
    }

    @Test
    public void anItemAtOrBelowItsThresholdIsASuggestion() {
        Long itemId = createItem("At threshold", 20, 7);
        stock(itemId, 20);

        InventoryProjection row = boardByItemId().get(itemId);
        assertEquals(BoardStatus.REORDER_NOW, row.getStatus());
        assertTrue(isSuggestion(row));
    }

    /**
     * The case the whole feature turns on: same stock, same consumption, different
     * lead time. The slow-to-arrive item has to be ordered now; the fast one does
     * not. A single global look-ahead would have flagged both or neither.
     */
    @Test
    public void twoItemsWithTheSameProjectionDifferOnlyByTheirOwnLeadTime() {
        Long slowToArrive = createItem("Slow lead", 20, 30);
        steadyUsage(slowToArrive, stock(slowToArrive, 60), 2, 30);

        Long quickToArrive = createItem("Fast lead", 20, 1);
        steadyUsage(quickToArrive, stock(quickToArrive, 60), 2, 30);

        Map<Long, InventoryProjection> board = boardByItemId();
        InventoryProjection slow = board.get(slowToArrive);
        InventoryProjection quick = board.get(quickToArrive);

        assertEquals("both are projected to run out at the same time", slow.getRunOutEarly(), quick.getRunOutEarly());
        assertEquals(BoardStatus.REORDER_SOON, slow.getStatus());
        assertTrue("a 30-day lead time means ordering now", isSuggestion(slow));
        assertEquals(BoardStatus.ADEQUATE, quick.getStatus());
        assertTrue("a 1-day lead time means there is no hurry", !isSuggestion(quick));
    }

    @Test
    public void markingOrderedStampsTheItemWithoutChangingWhatTheBoardSaysAboutItsStock() {
        Long itemId = createItem("Ordered but still short", 20, 7);
        stock(itemId, 5);

        InventoryProjection before = boardByItemId().get(itemId);
        assertEquals(BoardStatus.REORDER_NOW, before.getStatus());
        assertNull("nothing is on order to begin with", before.getOrderedOn());

        LocalDate expected = LocalDate.now().plusDays(14);
        assertEquals(1, inventoryItemService.markOrdered(List.of(itemId), "PO-4471", expected, SYS_USER_ID));

        InventoryProjection after = boardByItemId().get(itemId);
        assertEquals("the row is still visible", before.getItemId(), after.getItemId());
        assertEquals("and still says the stock is short", BoardStatus.REORDER_NOW, after.getStatus());
        assertEquals("on-order is now visible on the row", LocalDate.now(), after.getOrderedOn());
        assertEquals(expected, after.getOrderExpectedDate());
        assertEquals("PO-4471", after.getOrderNote());
    }

    @Test
    public void markingOrderedAppliesToAWholeSelectionAtOnce() {
        Long first = createItem("Bulk one", 10, 7);
        Long second = createItem("Bulk two", 10, 7);
        Long third = createItem("Bulk three", 10, 7);
        stock(first, 1);
        stock(second, 1);
        stock(third, 1);

        assertEquals(3, inventoryItemService.markOrdered(List.of(first, second, third), null, null, SYS_USER_ID));

        Map<Long, InventoryProjection> board = boardByItemId();
        for (Long itemId : List.of(first, second, third)) {
            assertNotNull("every selected item carries the mark", board.get(itemId).getOrderedOn());
        }
    }

    @Test
    public void markingIsReversible() {
        Long itemId = createItem("Marked in error", 10, 7);
        stock(itemId, 1);
        inventoryItemService.markOrdered(List.of(itemId), "wrong row", LocalDate.now().plusDays(3), SYS_USER_ID);

        assertEquals(1, inventoryItemService.clearOrdered(List.of(itemId), SYS_USER_ID));

        InventoryProjection row = boardByItemId().get(itemId);
        assertNull(row.getOrderedOn());
        assertNull(row.getOrderExpectedDate());
        assertNull(row.getOrderNote());
    }

    /**
     * Re-marking must not restart the clock. The stamp is what a learned lead time
     * will later be measured from, so refreshing it every time somebody re-selects
     * the row would quietly erase the order-to-receipt history before it is used.
     */
    @Test
    public void reMarkingKeepsTheOriginalStampButUpdatesTheNote() {
        Long itemId = createItem("Re-marked", 10, 7);
        stock(itemId, 1);
        inventoryItemService.markOrdered(List.of(itemId), "first note", null, SYS_USER_ID);
        Timestamp firstStamp = inventoryItemService.get(itemId).getOrderedAt();

        inventoryItemService.markOrdered(List.of(itemId), "second note", LocalDate.now().plusDays(5), SYS_USER_ID);

        InventoryItem item = inventoryItemService.get(itemId);
        assertEquals("the original stamp survives", firstStamp, item.getOrderedAt());
        assertEquals("second note", item.getOrderNote());
        assertEquals(LocalDate.now().plusDays(5), item.getOrderExpectedDate());
    }

    @Test
    public void clearingAnItemThatWasNeverOrderedChangesNothing() {
        Long itemId = createItem("Never ordered", 10, 7);
        stock(itemId, 1);

        assertEquals(0, inventoryItemService.clearOrdered(List.of(itemId), SYS_USER_ID));
        assertNull(inventoryItemService.get(itemId).getOrderedAt());
    }

    @Test
    public void anEmptySelectionIsANoOpRatherThanAnError() {
        assertEquals(0, inventoryItemService.markOrdered(List.of(), "nothing", null, SYS_USER_ID));
        assertEquals(0, inventoryItemService.clearOrdered(null, SYS_USER_ID));
    }
}
