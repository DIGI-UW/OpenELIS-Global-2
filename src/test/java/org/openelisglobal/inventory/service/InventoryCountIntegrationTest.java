package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.service.InventoryCountService.CountEntry;
import org.openelisglobal.inventory.service.InventoryCountService.CountResult;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A physical count is the one operation in this module where being careful
 * about what is <i>not</i> written matters more than what is.
 *
 * <p>
 * Counting six shelves and typing into two must move exactly two: the other
 * four were not counted, and treating a blank as a zero would write off stock
 * nobody looked at. Abandoning a count halfway must leave nothing behind at
 * all. And the adjustments one confirmed count makes have to be findable
 * together afterwards, or the count is not auditable.
 */
public class InventoryCountIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryCountService inventoryCountService;

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    private static final String SYS_USER_ID = "1";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private Long createItem(String name) {
        InventoryItem item = new InventoryItem();
        item.setFhirUuid(UUID.randomUUID());
        item.setName(name + " " + UUID.randomUUID());
        item.setUnits("tests");
        item.setIsActive("Y");
        item.setSysUserId(SYS_USER_ID);
        return inventoryItemService.insert(item);
    }

    private Long stock(Long itemId, double quantity) {
        InventoryLot lot = new InventoryLot();
        lot.setFhirUuid(UUID.randomUUID());
        lot.setInventoryItem(inventoryItemService.get(itemId));
        lot.setLotNumber("CNT-" + UUID.randomUUID().toString().substring(0, 8));
        lot.setExpirationDate(Timestamp.valueOf(LocalDateTime.now().plusDays(365)));
        lot.setReceiptDate(Timestamp.valueOf(LocalDateTime.now().minusDays(10)));
        lot.setInitialQuantity(quantity);
        lot.setCurrentQuantity(quantity);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setSysUserId(SYS_USER_ID);
        return inventoryLotService.insert(lot);
    }

    private CountEntry entry(Long lotId, Double counted) {
        CountEntry countEntry = new CountEntry();
        countEntry.setLotId(lotId);
        countEntry.setCountedQuantity(counted);
        return countEntry;
    }

    private double quantityOf(Long lotId) {
        return inventoryLotService.get(lotId).getCurrentQuantity();
    }

    private int adjustmentsFor(Long lotId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clinlims.inventory_transaction"
                + " WHERE lot_id = ? AND transaction_type = 'ADJUSTMENT'", Integer.class, lotId);
    }

    /**
     * The case the card is built around: six on the shelf, two actually counted.
     */
    @Test
    public void countingTwoOfSixMovesExactlyThose() {
        Long itemId = createItem("Six lot item");
        List<Long> lots = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            lots.add(stock(itemId, 10));
        }

        CountResult result = inventoryCountService.recordCount(List.of(entry(lots.get(1), 8d), entry(lots.get(4), 12d)),
                SYS_USER_ID);

        assertEquals(2, result.getAdjusted());
        assertEquals(8d, quantityOf(lots.get(1)), 0.001);
        assertEquals(12d, quantityOf(lots.get(4)), 0.001);
        for (int i : new int[] { 0, 2, 3, 5 }) {
            assertEquals("a lot nobody counted is untouched", 10d, quantityOf(lots.get(i)), 0.001);
            assertEquals("and has no adjustment against it", 0, adjustmentsFor(lots.get(i)));
        }
    }

    @Test
    public void abandoningACountWritesNothingAtAll() {
        Long itemId = createItem("Abandoned count item");
        Long lotId = stock(itemId, 10);

        CountResult result = inventoryCountService.recordCount(List.of(), SYS_USER_ID);

        assertEquals(0, result.getAdjusted());
        assertEquals(0, result.getConfirmed());
        assertNull("no session is opened for a count that was not taken", result.getSessionReference());
        assertEquals(10d, quantityOf(lotId), 0.001);
        assertEquals(0, adjustmentsFor(lotId));
        assertNull("and nothing claims to have been counted", inventoryItemService.get(itemId).getLastCountedAt());
    }

    /**
     * The adjustments one confirmed count makes have to be findable together, or
     * the count cannot be audited as a count.
     */
    @Test
    public void oneSessionGroupsItsAdjustmentsUnderOneReference() {
        Long itemId = createItem("Grouped count item");
        Long first = stock(itemId, 10);
        Long second = stock(itemId, 20);
        Long third = stock(itemId, 30);

        CountResult result = inventoryCountService
                .recordCount(List.of(entry(first, 9d), entry(second, 19d), entry(third, 29d)), SYS_USER_ID);

        assertNotNull(result.getSessionReference());
        Integer grouped = jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM clinlims.inventory_transaction WHERE reference_id = ?"
                        + " AND reference_type = 'ADJUSTMENT'", Integer.class, result.getSessionReference());
        assertEquals("all three adjustments carry the session's reference", Integer.valueOf(3), grouped);
    }

    @Test
    public void twoSessionsAreNotConfusedWithEachOther() {
        Long itemId = createItem("Two session item");
        Long first = stock(itemId, 10);
        Long second = stock(itemId, 20);

        CountResult one = inventoryCountService.recordCount(List.of(entry(first, 9d)), SYS_USER_ID);
        CountResult two = inventoryCountService.recordCount(List.of(entry(second, 19d)), SYS_USER_ID);

        assertTrue("each confirmed count gets its own reference",
                !one.getSessionReference().equals(two.getSessionReference()));
    }

    /**
     * Counting something and finding it correct is still a count. It is the answer
     * a stock check most often produces, and losing it would mean the board could
     * never say an item had been verified.
     */
    @Test
    public void aCountThatAgreesIsRecordedWithoutMovingStock() {
        Long itemId = createItem("Agreeing count item");
        Long lotId = stock(itemId, 10);

        CountResult result = inventoryCountService.recordCount(List.of(entry(lotId, 10d)), SYS_USER_ID);

        assertEquals(1, result.getConfirmed());
        assertEquals(0, result.getAdjusted());
        assertNull("nothing to group, so no session is opened", result.getSessionReference());
        assertEquals(0, adjustmentsFor(lotId));
        assertNotNull("but the item was counted", inventoryItemService.get(itemId).getLastCountedAt());
    }

    @Test
    public void everyItemTouchedIsStampedEvenWhenOnlyOneMoved() {
        Long movedItem = createItem("Moved item");
        Long steadyItem = createItem("Steady item");
        Long movedLot = stock(movedItem, 10);
        Long steadyLot = stock(steadyItem, 5);

        inventoryCountService.recordCount(List.of(entry(movedLot, 7d), entry(steadyLot, 5d)), SYS_USER_ID);

        assertNotNull(inventoryItemService.get(movedItem).getLastCountedAt());
        assertNotNull(inventoryItemService.get(steadyItem).getLastCountedAt());
    }

    @Test
    public void anAdjustmentRecordsTheMovementItMade() {
        Long itemId = createItem("Movement item");
        Long lotId = stock(itemId, 10);

        inventoryCountService.recordCount(List.of(entry(lotId, 4d)), SYS_USER_ID);

        Double change = jdbcTemplate
                .queryForObject("SELECT quantity_change FROM clinlims.inventory_transaction WHERE lot_id = ?"
                        + " AND transaction_type = 'ADJUSTMENT'", Double.class, lotId);
        Double after = jdbcTemplate
                .queryForObject("SELECT quantity_after FROM clinlims.inventory_transaction WHERE lot_id = ?"
                        + " AND transaction_type = 'ADJUSTMENT'", Double.class, lotId);
        assertEquals("the shortfall, not the counted total", -6d, change, 0.001);
        assertEquals(4d, after, 0.001);
    }

    @Test
    public void countingToZeroIsAllowedAndIsNotTreatedAsBlank() {
        Long itemId = createItem("Emptied item");
        Long lotId = stock(itemId, 10);

        CountResult result = inventoryCountService.recordCount(List.of(entry(lotId, 0d)), SYS_USER_ID);

        assertEquals(1, result.getAdjusted());
        assertEquals(0d, quantityOf(lotId), 0.001);
    }

    @Test
    public void countingToZeroRetiresTheLotSoItLeavesTheUsableStock() {
        Long itemId = createItem("Emptied item");
        Long lotId = stock(itemId, 10);

        inventoryCountService.recordCount(List.of(entry(lotId, 0d)), SYS_USER_ID);

        assertEquals("a lot counted down to nothing is consumed, whatever the books said", LotStatus.CONSUMED,
                inventoryLotService.get(lotId).getStatus());
    }

    /**
     * The count's whole point is to believe the shelf over the books. A lot the
     * books had run to zero is CONSUMED, and every on-hand sum and FEFO draw
     * excludes that status, so writing the found quantity without the status would
     * leave the stock counted and still invisible.
     */
    @Test
    public void findingStockInAConsumedLotPutsItBackIntoUse() {
        Long itemId = createItem("Rediscovered item");
        Long lotId = stock(itemId, 10);
        inventoryCountService.recordCount(List.of(entry(lotId, 0d)), SYS_USER_ID);
        assertEquals(LotStatus.CONSUMED, inventoryLotService.get(lotId).getStatus());

        inventoryCountService.recordCount(List.of(entry(lotId, 4d)), SYS_USER_ID);

        InventoryLot found = inventoryLotService.get(lotId);
        assertEquals(4d, found.getCurrentQuantity(), 0.001);
        assertEquals(LotStatus.ACTIVE, found.getStatus());
        assertTrue("stock a count found must count as available again", found.countsAsAvailableStock());
    }

    /**
     * Disposal is a decision, not a bookkeeping state, so a count cannot undo it.
     */
    @Test
    public void findingStockInADisposedLotLeavesItDisposed() {
        Long itemId = createItem("Disposed item");
        Long lotId = stock(itemId, 10);
        inventoryLotService.updateLotStatus(lotId, LotStatus.DISPOSED, SYS_USER_ID);

        inventoryCountService.recordCount(List.of(entry(lotId, 4d)), SYS_USER_ID);

        assertEquals(LotStatus.DISPOSED, inventoryLotService.get(lotId).getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void aNegativeCountIsRefused() {
        Long itemId = createItem("Negative count item");
        Long lotId = stock(itemId, 10);

        inventoryCountService.recordCount(List.of(entry(lotId, -1d)), SYS_USER_ID);
    }

    /**
     * One session is one transaction. A bad entry anywhere in it must not leave the
     * shelf half adjusted.
     */
    @Test
    public void abadEntryRollsBackTheWholeSession() {
        Long itemId = createItem("Rollback item");
        Long good = stock(itemId, 10);

        try {
            inventoryCountService.recordCount(List.of(entry(good, 5d), entry(999_999_999L, 3d)), SYS_USER_ID);
        } catch (IllegalArgumentException expected) {
            // the point of the case
        }

        assertEquals("the good entry is rolled back with the bad one", 10d, quantityOf(good), 0.001);
        assertEquals(0, adjustmentsFor(good));
    }

    @Test
    public void anEntryWithNoCountedValueIsIgnoredRatherThanTreatedAsZero() {
        Long itemId = createItem("Blank entry item");
        Long lotId = stock(itemId, 10);

        CountResult result = inventoryCountService.recordCount(List.of(entry(lotId, null)), SYS_USER_ID);

        assertEquals(0, result.getAdjusted());
        assertEquals(10d, quantityOf(lotId), 0.001);
    }
}
