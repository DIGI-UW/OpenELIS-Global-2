package org.openelisglobal.inventory.service;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.service.InventoryManagementService.ConsumptionRecord;
import org.openelisglobal.inventory.service.InventoryManagementService.InventoryAlerts;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

@Rollback
public class InventoryManagementServiceIT extends BaseWebContextSensitiveTest {

    @Autowired
    InventoryManagementService inventoryManagementService;

    @Autowired
    InventoryLotService inventoryLotService;

    @Autowired
    InventoryItemService inventoryItemService;

    @Autowired
    InventoryTransactionService transactionService;

    @Autowired
    InventoryUsageService usageService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
    }

    @Test
    public void consumeInventoryFEFO_shouldConsumeFromEarliestExpiringLot() {
        List<ConsumptionRecord> records = inventoryManagementService.consumeInventoryFEFO("TEST_REAGENT_A", 25.0, 1L,
                1L, "1");

        assertNotNull("Consumption records should not be null", records);
        assertEquals("Should have 1 consumption record", 1, records.size());

        ConsumptionRecord record = records.getFirst();
        assertEquals("Should consume from earliest expiring lot", Long.valueOf(1001L), record.getLotId());
        assertEquals(Double.valueOf(25.0), record.getQuantityConsumed());

        InventoryLot updatedLot = inventoryLotService.get(1001L);
        assertEquals("Lot quantity should be reduced", Double.valueOf(25.0), updatedLot.getCurrentQuantity());
    }

    @Test
    public void consumeInventoryFEFO_shouldConsumeFromMultipleLotsWhenNeeded() {
        List<ConsumptionRecord> records = inventoryManagementService.consumeInventoryFEFO("TEST_REAGENT_A", 120.0, 2L,
                2L, "1");

        assertNotNull("Consumption records should not be null", records);
        assertEquals("Should have 2 consumption records", 2, records.size());

        assertEquals(Long.valueOf(1001L), records.get(0).getLotId());
        assertEquals(Double.valueOf(50.0), records.get(0).getQuantityConsumed());

        assertEquals(Long.valueOf(1000L), records.get(1).getLotId());
        assertEquals(Double.valueOf(70.0), records.get(1).getQuantityConsumed());

        assertEquals(Double.valueOf(0.0), inventoryLotService.get(1001L).getCurrentQuantity());
        assertEquals(Double.valueOf(30.0), inventoryLotService.get(1000L).getCurrentQuantity());
    }

    @Test
    public void consumeInventoryFEFO_shouldCreateTransactionRecords() {
        inventoryManagementService.consumeInventoryFEFO("TEST_REAGENT_A", 25.0, 3L, 3L, "1");

        List<InventoryTransaction> transactions = transactionService.getByLotId(1001L);

        InventoryTransaction consumptionTx = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.CONSUMPTION).findFirst().orElse(null);

        assertNotNull("Should have consumption transaction", consumptionTx);
        assertEquals(Double.valueOf(-25.0), consumptionTx.getQuantityChange());
        assertEquals(Double.valueOf(25.0), consumptionTx.getQuantityAfter());
    }

    @Test
    public void consumeInventoryFEFO_shouldCreateUsageRecords() {
        inventoryManagementService.consumeInventoryFEFO("TEST_REAGENT_A", 25.0, 4L, 4L, "1");

        List<InventoryUsage> usageRecords = usageService.getByTestResultId(4L);

        assertNotNull("Usage records should not be null", usageRecords);
        assertFalse("Should have usage record", usageRecords.isEmpty());

        InventoryUsage usage = usageRecords.getFirst();
        assertEquals(Long.valueOf(1001L), usage.getLot().getId());
        assertEquals(Double.valueOf(25.0), usage.getQuantityUsed());
        assertEquals(Long.valueOf(4L), usage.getTestResultId());
    }

    @Test(expected = IllegalStateException.class)
    public void consumeInventoryFEFO_shouldThrowExceptionWhenInsufficientStock() {
        inventoryManagementService.consumeInventoryFEFO("TEST_REAGENT_A", 200.0, // More than available
                5L, 5L, "1");

    }

    @Test
    public void getInventoryAlerts_shouldIdentifyLowStockItems() {
        inventoryManagementService.consumeInventoryFEFO("TEST_REAGENT_A", 145.0, null, null, "1");

        InventoryAlerts alerts = inventoryManagementService.getInventoryAlerts(30);

        assertNotNull("Alerts should not be null", alerts);
    }

    @Test
    public void receiveInventory_shouldCreateLotAndTransaction() {
        InventoryLot newLot = new InventoryLot();
        newLot.setInventoryItem(inventoryItemService.get("TEST_REAGENT_A"));
        newLot.setLotNumber("LOT-NEW-001");
        newLot.setInitialQuantity(200.0);
        newLot.setCurrentQuantity(200.0);

        InventoryLot receivedLot = inventoryManagementService.receiveInventory(newLot, "1");

        assertNotNull("Received lot should not be null", receivedLot);
        assertNotNull("Lot should have ID", receivedLot.getId());
        assertEquals(Double.valueOf(200.0), receivedLot.getCurrentQuantity());

        List<InventoryTransaction> transactions = transactionService.getByLotId(receivedLot.getId());
        assertFalse("Should have transaction", transactions.isEmpty());

        InventoryTransaction receiptTx = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.RECEIPT).findFirst().orElse(null);

        assertNotNull("Should have receipt transaction", receiptTx);
        assertEquals(Double.valueOf(200.0), receiptTx.getQuantityChange());
    }

    @Test
    public void isSufficientInventoryAvailable_shouldReturnTrueWhenSufficient() {
        boolean available = inventoryManagementService.isSufficientInventoryAvailable("TEST_REAGENT_A", 100.0);
        assertTrue("Should have sufficient inventory", available);
    }

    @Test
    public void isSufficientInventoryAvailable_shouldReturnFalseWhenInsufficient() {
        boolean available = inventoryManagementService.isSufficientInventoryAvailable("TEST_REAGENT_A", 200.0);
        assertFalse("Should not have sufficient inventory", available);
    }
}
