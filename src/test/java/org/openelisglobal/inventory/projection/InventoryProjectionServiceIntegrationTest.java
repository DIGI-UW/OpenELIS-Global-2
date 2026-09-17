package org.openelisglobal.inventory.projection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.projection.InventoryProjection.BoardStatus;
import org.openelisglobal.inventory.projection.InventoryProjection.LeadTimeTier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

@Rollback
public class InventoryProjectionServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long REAGENT_ITEM_ID = 1000L;
    private static final long RDT_ITEM_ID = 1001L;
    private static final long RETIRED_ITEM_ID = 1002L;
    private static final long USABLE_LOT_ID = 9000L;
    private static final long PENDING_LOT_ID = 9001L;
    private static final long FAILED_LOT_ID = 9002L;
    private static final long RETIRED_LOT_ID = 9003L;
    private static final double DAILY_USE = 2.0;
    private static final double USABLE_QUANTITY = 60.0;
    private static final double PENDING_QUANTITY = 25.0;

    @Autowired
    private InventoryProjectionService inventoryProjectionService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private JdbcTemplate jdbc;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
        jdbc = new JdbcTemplate(dataSource);

        // All shared-fixture lots have expired; this lot is the only stock.
        jdbc.update("DELETE FROM clinlims.inventory_usage WHERE id >= 9000");
        jdbc.update("DELETE FROM clinlims.inventory_lot WHERE id IN (?, ?, ?, ?)", USABLE_LOT_ID, PENDING_LOT_ID,
                FAILED_LOT_ID, RETIRED_LOT_ID);
        jdbc.update("DELETE FROM clinlims.inventory_item WHERE id = ?", RETIRED_ITEM_ID);

        jdbc.update("INSERT INTO clinlims.inventory_lot (id, fhir_uuid, inventory_item_id, lot_number,"
                + " expiration_date, receipt_date, initial_quantity, current_quantity, qc_status, status, version,"
                + " last_updated) VALUES (?, gen_random_uuid(), ?, 'PROJ-LOT-1', NOW() + INTERVAL '1 year', NOW(),"
                + " ?, ?, 'PASSED', 'ACTIVE', 0, NOW())", USABLE_LOT_ID, REAGENT_ITEM_ID, USABLE_QUANTITY,
                USABLE_QUANTITY);

        // Days 1-28 ago; today and the window's oldest day stay at zero.
        for (int daysAgo = 1; daysAgo <= 28; daysAgo++) {
            jdbc.update(
                    "INSERT INTO clinlims.inventory_usage (id, inventory_item_id, lot_id, quantity_used,"
                            + " usage_date, performed_by_user, last_updated)"
                            + " VALUES (?, ?, ?, ?, NOW() - (? * INTERVAL '1 day'), 1, NOW())",
                    9000L + daysAgo, REAGENT_ITEM_ID, USABLE_LOT_ID, DAILY_USE, daysAgo);
        }
        resyncSequence("clinlims.inventory_usage_seq", "clinlims.inventory_usage");
        resyncSequence("clinlims.inventory_lot_seq", "clinlims.inventory_lot");
    }

    private void insertLot(long id, String lotNumber, double quantity, String qcStatus, String status) {
        jdbc.update("INSERT INTO clinlims.inventory_lot (id, fhir_uuid, inventory_item_id, lot_number,"
                + " expiration_date, receipt_date, initial_quantity, current_quantity, qc_status, status, version,"
                + " last_updated) VALUES (?, gen_random_uuid(), ?, ?, NOW() + INTERVAL '1 year', NOW(), ?, ?, ?, ?,"
                + " 0, NOW())", id, REAGENT_ITEM_ID, lotNumber, quantity, quantity, qcStatus, status);
        resyncSequence("clinlims.inventory_lot_seq", "clinlims.inventory_lot");
    }

    private Map<Long, InventoryProjection> board() {
        List<InventoryProjection> rows = inventoryProjectionService.getBoard();
        return rows.stream().collect(Collectors.toMap(InventoryProjection::getItemId, Function.identity()));
    }

    @Test
    public void onHandExcludesStockThatHasExpired() {
        InventoryProjection reagent = board().get(REAGENT_ITEM_ID);

        // The fixture's two lots of this item (150 units) are long expired.
        assertEquals(USABLE_QUANTITY, reagent.getOnHand(), 0.0001);
        assertEquals("Test Reagent A", reagent.getName());
        assertEquals("TEST_REAGENT_A", reagent.getCode());
        assertEquals("mL", reagent.getUnits());
    }

    @Test
    public void onHandCountsStockAwaitingQcButNotStockThatFailedIt() {
        insertLot(PENDING_LOT_ID, "PROJ-LOT-PENDING", PENDING_QUANTITY, "PENDING", "ACTIVE");
        insertLot(FAILED_LOT_ID, "PROJ-LOT-FAILED", 40.0, "FAILED", "ACTIVE");

        InventoryProjection reagent = board().get(REAGENT_ITEM_ID);

        assertEquals("the lot awaiting QC counts toward on-hand, the failed one does not",
                USABLE_QUANTITY + PENDING_QUANTITY, reagent.getOnHand(), 0.0001);
    }

    @Test
    public void aQuarantinedLotIsNotStockOnAnySurface() {
        insertLot(PENDING_LOT_ID, "PROJ-LOT-QUARANTINED", 75.0, "PASSED", "QUARANTINED");

        InventoryProjection reagent = board().get(REAGENT_ITEM_ID);

        assertEquals("quarantined stock is excluded however its QC reads", USABLE_QUANTITY, reagent.getOnHand(),
                0.0001);
    }

    @Test
    public void onHandExcludesALotPastItsExpiryAfterOpening() {
        jdbc.update("INSERT INTO clinlims.inventory_lot (id, fhir_uuid, inventory_item_id, lot_number,"
                + " expiration_date, date_opened, calculated_expiry_after_opening, receipt_date, initial_quantity,"
                + " current_quantity, qc_status, status, version, last_updated) VALUES (?, gen_random_uuid(), ?,"
                + " 'PROJ-LOT-OPENED', NOW() + INTERVAL '1 year', NOW() - INTERVAL '31 days',"
                + " NOW() - INTERVAL '1 day', NOW(), 30, 30, 'PASSED', 'IN_USE', 0, NOW())", PENDING_LOT_ID,
                REAGENT_ITEM_ID);

        InventoryProjection reagent = board().get(REAGENT_ITEM_ID);

        assertEquals("an opened lot past its stability window is not stock", USABLE_QUANTITY, reagent.getOnHand(),
                0.0001);
    }

    @Test
    public void theBoardReadCostDoesNotGrowWithLotsOrRetiredItems() {
        jdbc.update("INSERT INTO clinlims.inventory_item (id, fhir_uuid, code, name, item_type, units, is_active,"
                + " last_updated) VALUES (?, gen_random_uuid(), 'PROJ_RETIRED', 'Retired item', 'REAGENT', 'mL',"
                + " 'N', NOW())", RETIRED_ITEM_ID);
        jdbc.update("INSERT INTO clinlims.inventory_lot (id, fhir_uuid, inventory_item_id, lot_number,"
                + " expiration_date, receipt_date, initial_quantity, current_quantity, qc_status, status, version,"
                + " last_updated) VALUES (?, gen_random_uuid(), ?, 'PROJ-LOT-RETIRED', NOW() + INTERVAL '1 year',"
                + " NOW(), 10, 10, 'PASSED', 'ACTIVE', 0, NOW())", RETIRED_LOT_ID, RETIRED_ITEM_ID);
        resyncSequence("clinlims.inventory_lot_seq", "clinlims.inventory_lot");

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean wasEnabled = statistics.isStatisticsEnabled();
        statistics.clear();
        statistics.setStatisticsEnabled(true);
        try {
            Map<Long, InventoryProjection> rows = board();

            assertEquals("items, stock, usage, and order cycles: one statement each", 4,
                    statistics.getPrepareStatementCount());
            assertEquals(USABLE_QUANTITY, rows.get(REAGENT_ITEM_ID).getOnHand(), 0.0001);
            assertNull("a retired item gets no row", rows.get(RETIRED_ITEM_ID));
        } finally {
            statistics.setStatisticsEnabled(wasEnabled);
        }
    }

    @Test
    public void usageIsAggregatedIntoAMedianDailyRateAndARunOutWindow() {
        InventoryProjection reagent = board().get(REAGENT_ITEM_ID);
        LocalDate today = LocalDate.now();

        assertEquals(DAILY_USE, reagent.getMedianDailyUse(), 0.0001);
        assertEquals(today.minusDays(1), reagent.getBasisDate());
        assertEquals("steady use leaves no spread, so both ends of the window land together", today.plusDays(30),
                reagent.getRunOutEarly());
        assertEquals(today.plusDays(30), reagent.getRunOutLate());
        assertEquals("usage yesterday is not stale", false, reagent.isStale());
    }

    @Test
    public void anItemWithNoUsableStockAndNoHistoryStillFlagsOnItsThresholdAlone() {
        InventoryProjection rdt = board().get(RDT_ITEM_ID);

        assertEquals(0.0, rdt.getOnHand(), 0.0001);
        assertEquals(BoardStatus.REORDER_NOW, rdt.getStatus());
        assertNull("no usage means no date the board could stand behind", rdt.getRunOutEarly());
        assertNull(rdt.getOrderByDate());
        assertTrue(rdt.isStale());
    }

    @Test
    public void theLeadTimeColumnRoundTripsAndDrivesTheOrderByDate() {
        InventoryProjection beforeSetting = board().get(REAGENT_ITEM_ID);
        assertEquals("nothing entered yet, so the board uses a placeholder and says so", LeadTimeTier.DEFAULT,
                beforeSetting.getLeadTimeTier());
        assertEquals(InventoryProjectionCalculator.DEFAULT_LEAD_TIME_DAYS, beforeSetting.getLeadTimeDays().intValue());

        jdbc.update("UPDATE clinlims.inventory_item SET lead_time_days = 14 WHERE id = ?", REAGENT_ITEM_ID);

        InventoryProjection afterSetting = board().get(REAGENT_ITEM_ID);
        assertEquals(LeadTimeTier.SET, afterSetting.getLeadTimeTier());
        assertEquals(14, afterSetting.getLeadTimeDays().intValue());
        assertEquals("order-by is the early end of the window less the lead time",
                afterSetting.getRunOutEarly().minusDays(14), afterSetting.getOrderByDate());
    }

    private LocalDate basisDateForUsageAged(int daysAgo) {
        jdbc.update("DELETE FROM clinlims.inventory_usage WHERE id = 9500");
        jdbc.update("INSERT INTO clinlims.inventory_usage (id, inventory_item_id, lot_id, quantity_used,"
                + " usage_date, performed_by_user, last_updated)"
                + " VALUES (9500, ?, 1002, 1, NOW() - (? * INTERVAL '1 day'), 1, NOW())", RDT_ITEM_ID, daysAgo);
        return board().get(RDT_ITEM_ID).getBasisDate();
    }

    @Test
    public void usageRecordedTodayIsInsideTheWindow() {
        assertEquals(LocalDate.now(), basisDateForUsageAged(0));
    }

    @Test
    public void usageOnTheOldestDayOfTheWindowIsStillInsideIt() {
        int oldestDay = InventoryProjectionCalculator.WINDOW_DAYS - 1;

        assertEquals("the window is the 30 days ending today, inclusive at both ends",
                LocalDate.now().minusDays(oldestDay), basisDateForUsageAged(oldestDay));
    }

    @Test
    public void usageOneDayOlderThanTheWindowIsNotRead() {
        assertNull("a record just outside the window must not become the basis of an estimate",
                basisDateForUsageAged(InventoryProjectionCalculator.WINDOW_DAYS));
    }

    @Test
    public void theBoardLeadsWithTheItemsNeedingAttention() {
        List<InventoryProjection> rows = inventoryProjectionService.getBoard();

        assertTrue("the board should carry both fixture items", rows.size() >= 2);
        assertEquals("the item that is out of usable stock sorts above the one that is covered", RDT_ITEM_ID,
                rows.get(0).getItemId().longValue());
        for (int index = 1; index < rows.size(); index++) {
            assertTrue("rows must be ordered by urgency",
                    rows.get(index - 1).getStatus().ordinal() <= rows.get(index).getStatus().ordinal());
        }
    }
}
