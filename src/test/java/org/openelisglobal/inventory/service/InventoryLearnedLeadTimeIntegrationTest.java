package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.projection.InventoryProjection;
import org.openelisglobal.inventory.projection.InventoryProjection.LeadTimeTier;
import org.openelisglobal.inventory.projection.InventoryProjectionService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryOrderCycle;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A lab that never fills in a lead time should get one anyway, learned from its
 * own deliveries. These cases pin the round trip: marking an item as ordered
 * and then receiving stock closes a cycle, enough cycles produce a median, and
 * the board starts saying "observed" instead of falling back to a placeholder.
 *
 * <p>
 * The one rule that matters more than the arithmetic: a value the lab entered
 * always wins. The learned figure is a suggestion, never a silent overwrite.
 */
public class InventoryLearnedLeadTimeIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryManagementService inventoryManagementService;

    @Autowired
    private InventoryOrderCycleService inventoryOrderCycleService;

    @Autowired
    private InventoryProjectionService inventoryProjectionService;

    private static final String SYS_USER_ID = "1";

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    private Long createItem(Integer setLeadTimeDays) {
        InventoryItem item = new InventoryItem();
        item.setFhirUuid(UUID.randomUUID());
        item.setName("Lead time fixture " + UUID.randomUUID());
        item.setItemType(ItemType.REAGENT);
        item.setUnits("tests");
        item.setLowStockThreshold(5);
        item.setLeadTimeDays(setLeadTimeDays);
        item.setIsActive("Y");
        item.setSysUserId(SYS_USER_ID);
        return inventoryItemService.insert(item);
    }

    /** Marks the item as ordered, backdated, then receives stock against it. */
    private void completeCycle(Long itemId, int daysAgoOrdered) {
        inventoryItemService.markOrdered(List.of(itemId), null, null, SYS_USER_ID);
        InventoryItem item = inventoryItemService.get(itemId);
        item.setOrderedAt(Timestamp.valueOf(LocalDateTime.now().minusDays(daysAgoOrdered)));
        item.setSysUserId(SYS_USER_ID);
        inventoryItemService.update(item);

        InventoryLot lot = new InventoryLot();
        lot.setInventoryItem(inventoryItemService.get(itemId));
        lot.setLotNumber("LT-" + UUID.randomUUID().toString().substring(0, 8));
        lot.setExpirationDate(Timestamp.valueOf(LocalDateTime.now().plusDays(365)));
        lot.setInitialQuantity(10.0);
        lot.setCurrentQuantity(10.0);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setStatus(LotStatus.ACTIVE);
        inventoryManagementService.receiveInventory(lot, SYS_USER_ID);
    }

    private InventoryProjection boardRow(Long itemId) {
        return inventoryProjectionService.getBoard().stream()
                .collect(Collectors.toMap(InventoryProjection::getItemId, Function.identity())).get(itemId);
    }

    private List<Integer> cycleDaysFor(Long itemId) {
        return inventoryOrderCycleService.getReceivedSince(Timestamp.valueOf(LocalDateTime.now().minusDays(400)))
                .stream().filter(c -> c.getInventoryItem().getId().equals(itemId))
                .map(InventoryOrderCycle::getLeadTimeDays).collect(Collectors.toList());
    }

    @Test
    public void receivingStockForAnOrderedItemRecordsHowLongItTookAndTakesTheMarkOff() {
        Long itemId = createItem(null);

        completeCycle(itemId, 12);

        assertEquals("one cycle recorded", List.of(12), cycleDaysFor(itemId));
        assertNull("the order is no longer outstanding", inventoryItemService.get(itemId).getOrderedAt());
    }

    @Test
    public void receivingStockForAnItemThatWasNotOrderedRecordsNothing() {
        Long itemId = createItem(null);

        InventoryLot lot = new InventoryLot();
        lot.setInventoryItem(inventoryItemService.get(itemId));
        lot.setLotNumber("NOORDER-" + UUID.randomUUID().toString().substring(0, 8));
        lot.setInitialQuantity(5.0);
        lot.setCurrentQuantity(5.0);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setStatus(LotStatus.ACTIVE);
        inventoryManagementService.receiveInventory(lot, SYS_USER_ID);

        assertTrue("stock can arrive without an order having been recorded", cycleDaysFor(itemId).isEmpty());
    }

    /**
     * Below the confidence threshold the board must keep saying default rather than
     * quoting one delivery as though it were a pattern.
     */
    @Test
    public void oneCycleIsNotEnoughToQuoteALeadTime() {
        Long itemId = createItem(null);
        completeCycle(itemId, 9);

        InventoryProjection row = boardRow(itemId);
        assertEquals(LeadTimeTier.DEFAULT, row.getLeadTimeTier());
    }

    @Test
    public void threeCyclesProduceTheirMedianAndTheBoardSaysItIsObserved() {
        Long itemId = createItem(null);
        completeCycle(itemId, 10);
        completeCycle(itemId, 20);
        completeCycle(itemId, 12);

        assertEquals(3, cycleDaysFor(itemId).size());

        InventoryProjection row = boardRow(itemId);
        assertEquals(LeadTimeTier.OBSERVED, row.getLeadTimeTier());
        assertEquals("the median of 10, 12 and 20", Integer.valueOf(12), row.getLeadTimeDays());
    }

    /**
     * Spike-robust, for the same reason the consumption rate is: one delivery stuck
     * at customs must not become the lab's expected lead time.
     */
    @Test
    public void oneVeryLateDeliveryDoesNotDragTheLearnedLeadTime() {
        Long itemId = createItem(null);
        completeCycle(itemId, 7);
        completeCycle(itemId, 8);
        completeCycle(itemId, 9);
        completeCycle(itemId, 6);
        completeCycle(itemId, 200);

        InventoryProjection row = boardRow(itemId);
        assertEquals(LeadTimeTier.OBSERVED, row.getLeadTimeTier());
        assertTrue("a 200-day outlier must not move the median far, got " + row.getLeadTimeDays(),
                row.getLeadTimeDays() <= 10);
    }

    @Test
    public void aLeadTimeTheLabEnteredAlwaysWinsOverALearnedOne() {
        Long itemId = createItem(14);
        completeCycle(itemId, 30);
        completeCycle(itemId, 31);
        completeCycle(itemId, 32);

        InventoryProjection row = boardRow(itemId);
        assertEquals("the entered value is authoritative", LeadTimeTier.SET, row.getLeadTimeTier());
        assertEquals(Integer.valueOf(14), row.getLeadTimeDays());
        assertEquals("but the cycles are still recorded, so the suggestion can be offered", 3,
                cycleDaysFor(itemId).size());
    }

    @Test
    public void theLearnedLeadTimeDrivesTheOrderByDate() {
        Long itemId = createItem(null);
        completeCycle(itemId, 5);
        completeCycle(itemId, 5);
        completeCycle(itemId, 5);

        InventoryProjection row = boardRow(itemId);
        assertNotNull(row.getLeadTimeDays());
        assertEquals(Integer.valueOf(5), row.getLeadTimeDays());
        assertEquals(LeadTimeTier.OBSERVED, row.getLeadTimeTier());
    }
}
