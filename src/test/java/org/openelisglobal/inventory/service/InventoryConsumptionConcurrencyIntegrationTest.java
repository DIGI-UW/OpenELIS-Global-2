package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class InventoryConsumptionConcurrencyIntegrationTest extends BaseWebContextSensitiveTest {

    private static final int BENCHES = 8;

    @Autowired
    private InventoryManagementService inventoryManagementService;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryTransactionService inventoryTransactionService;

    @Autowired
    private DataSource dataSource;

    private final List<Long> lotIds = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
        // The fixture's own lots have expired, so consumption needs live ones; two
        // with the same expiry leave their FEFO order to the database.
        lotIds.add(insertLot("CONCURRENT-1"));
        lotIds.add(insertLot("CONCURRENT-2"));
    }

    @After
    public void deleteCommittedRows() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (Long lotId : lotIds) {
            jdbc.update("DELETE FROM clinlims.inventory_usage WHERE lot_id = ?", lotId);
            jdbc.update("DELETE FROM clinlims.inventory_transaction WHERE lot_id = ?", lotId);
            jdbc.update("DELETE FROM clinlims.inventory_lot WHERE id = ?", lotId);
        }
    }

    private Long insertLot(String lotNumber) {
        InventoryLot lot = new InventoryLot();
        lot.setFhirUuid(UUID.randomUUID());
        lot.setInventoryItem(inventoryItemService.get(1000L));
        lot.setLotNumber(lotNumber);
        lot.setExpirationDate(Timestamp.valueOf("2099-01-01 00:00:00"));
        lot.setReceiptDate(Timestamp.valueOf("2025-01-01 00:00:00"));
        lot.setInitialQuantity(100.0);
        lot.setCurrentQuantity(100.0);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setSysUserId("1");
        return inventoryLotService.insert(lot);
    }

    @Test
    public void simultaneousConsumptionOfOneItem_succeedsForEveryBenchWhileStockLasts() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(BENCHES);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Throwable>> outcomes = new ArrayList<>();
        for (int i = 0; i < BENCHES; i++) {
            outcomes.add(pool.submit(() -> {
                start.await();
                try {
                    inventoryManagementService.consumeInventoryFEFO(1000L, 5.0, null, null, "1");
                    return null;
                } catch (Throwable t) {
                    return t;
                }
            }));
        }
        start.countDown();
        pool.shutdown();

        List<String> failures = new ArrayList<>();
        for (Future<Throwable> outcome : outcomes) {
            Throwable failure = outcome.get();
            if (failure != null) {
                failures.add(failure.toString());
            }
        }
        assertEquals("no bench should be refused while stock lasts: " + failures, 0, failures.size());

        double remaining = 0;
        long consumptions = 0;
        for (Long lotId : lotIds) {
            remaining += inventoryLotService.get(lotId).getCurrentQuantity();
            consumptions += inventoryTransactionService.getByLotId(lotId).stream()
                    .filter(t -> t.getTransactionType() == TransactionType.CONSUMPTION).count();
        }
        assertEquals(200.0 - BENCHES * 5.0, remaining, 0.0001);
        assertEquals(BENCHES, consumptions);
    }
}
