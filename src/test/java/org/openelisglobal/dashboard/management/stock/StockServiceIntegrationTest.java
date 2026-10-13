package org.openelisglobal.dashboard.management.stock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dashboard.management.stock.StockResponse.Reorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class StockServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private StockService stockService;
    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    // Items 1000 (Test Reagent A, threshold 10) and 1001 (Test RDT Kit, threshold
    // 5) and their long-expired lots 1000-1002 come from the shared fixture.
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
        jdbc = new JdbcTemplate(dataSource);
        tearDown();
        insertItem(9200, "Test Cartridge", null);
        insertItem(9201, "Test Kit B", 5);
        insertLot(9100, 1000, "STK-A", 64, 20);
        insertLot(9101, 1001, "STK-RDT", 3, 45);
        insertLot(9102, 1000, "STK-A-EMPTY", 0, 10);
        insertLot(9103, 9201, "STK-B", 50, 200);
        insertLot(9104, 9201, "STK-OPENED", 40, 180);
        jdbc.update("UPDATE clinlims.inventory_lot SET status = 'IN_USE', date_opened = CURRENT_DATE - 8,"
                + " calculated_expiry_after_opening = CURRENT_DATE - 1 WHERE id = 9104");
        for (int daysAgo = 1; daysAgo <= 28; daysAgo++) {
            jdbc.update(
                    "INSERT INTO clinlims.inventory_usage (id, inventory_item_id, lot_id, quantity_used,"
                            + " usage_date, performed_by_user, last_updated)"
                            + " VALUES (?, 1000, 9100, 2, NOW() - (? * INTERVAL '1 day'), 1, NOW())",
                    9300 + daysAgo, daysAgo);
        }
    }

    @After
    public void tearDown() {
        jdbc.update("DELETE FROM clinlims.inventory_usage WHERE id BETWEEN 9301 AND 9328");
        jdbc.update("DELETE FROM clinlims.inventory_lot WHERE id BETWEEN 9100 AND 9104");
        jdbc.update("DELETE FROM clinlims.inventory_item WHERE id IN (9200, 9201)");
    }

    private void insertItem(long id, String name, Integer threshold) {
        jdbc.update("INSERT INTO clinlims.inventory_item (id, code, fhir_uuid, name, item_type, units,"
                + " low_stock_threshold, is_active) VALUES (?, ?, gen_random_uuid(), ?, 'CARTRIDGE', 'units', ?, 'Y')",
                id, "STOCK_" + id, name, threshold);
    }

    private void insertLot(long id, long itemId, String lotNumber, double quantity, int expiresInDays) {
        jdbc.update("INSERT INTO clinlims.inventory_lot (id, fhir_uuid, inventory_item_id, lot_number,"
                + " expiration_date, receipt_date, initial_quantity, current_quantity, qc_status, status, version,"
                + " last_updated) VALUES (?, gen_random_uuid(), ?, ?, CURRENT_DATE + ?, NOW(), GREATEST(?, 1), ?, 'PASSED',"
                + " 'ACTIVE', 0, NOW())", id, itemId, lotNumber, expiresInDays, quantity, quantity);
    }

    @Test
    public void criticalReagentsAreTheLowestOnHandShareOfThresholdWithTheirNextLot() {
        List<StockResponse.Reagent> critical = stockService.getStock().criticalReagents();

        assertEquals(List.of("Test RDT Kit", "Test Reagent A", "Test Kit B"),
                critical.stream().map(StockResponse.Reagent::name).toList());
        assertEquals("STK-RDT", critical.get(0).lot());
        assertEquals(TODAY.plusDays(45), critical.get(0).expiry());
        assertEquals(3.0, critical.get(0).onHand(), 0.001);
        assertEquals(5, critical.get(0).threshold());
        assertEquals("STK-A", critical.get(1).lot());
    }

    @Test
    public void expiringListsExpiredAndThirtyDayLotsThatStillHoldStockIncludingOpenedOnes() {
        List<StockResponse.ExpiringLot> expiring = stockService.getStock().expiring();

        assertEquals(List.of("LOT-2025-002", "LOT-2025-001", "CTRL-2025-001", "STK-OPENED", "STK-A"),
                expiring.stream().map(StockResponse.ExpiringLot::lot).toList());
        assertEquals(-1, expiring.get(3).daysLeft());
        assertTrue(expiring.get(3).expired());
        assertEquals(20, expiring.get(4).daysLeft());
        assertFalse(expiring.get(4).expired());
        assertEquals(64.0, expiring.get(4).remaining(), 0.001);
        assertEquals("Test Reagent A", expiring.get(4).name());
    }

    @Test
    public void reorderListsBelowThresholdFirstThenItemsReachingItWithinTheLeadTime() {
        List<Reorder> reorder = stockService.getStock().reorder();

        assertEquals(List.of("Test Cartridge:BELOW", "Test RDT Kit:BELOW", "Test Reagent A:APPROACHING"),
                reorder.stream().map(r -> r.name() + ":" + r.kind()).sorted().toList());
        Reorder approaching = reorder.stream().filter(r -> r.kind() == Reorder.Kind.APPROACHING).findFirst()
                .orElseThrow();
        assertEquals(TODAY.plusDays(2), approaching.orderBy());
        assertEquals(Reorder.Kind.BELOW, reorder.get(0).kind());
    }

    @Test
    public void anEmptyLotTableReadsAsEmpty() {
        assertEquals(StockResponse.State.OK, stockService.getStock().state());

        jdbc.update("TRUNCATE clinlims.inventory_lot CASCADE");

        assertEquals(StockResponse.State.EMPTY, stockService.getStock().state());
    }
}
