package org.openelisglobal.inventory.service;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

@Rollback
public class InventoryLotServiceIT extends BaseWebContextSensitiveTest {

    @Autowired
    InventoryLotService inventoryLotService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
    }

    @Test
    public void get_shouldReturnInventoryLotWhenExists() {
        InventoryLot lot = inventoryLotService.get(1000L);

        assertNotNull("Lot should be loaded from dataset", lot);
        assertEquals("LOT-2025-001", lot.getLotNumber());
        assertEquals(Double.valueOf(100.0), lot.getCurrentQuantity());
        assertEquals(QCStatus.PASSED, lot.getQcStatus());
        assertEquals(LotStatus.ACTIVE, lot.getStatus());
    }

    @Test
    public void getByLotNumber_shouldFindLotByLotNumber() {
        InventoryLot lot = inventoryLotService.getByLotNumber("LOT-2025-002");

        assertNotNull("Should find lot by lot number", lot);
        assertEquals(Long.valueOf(1001L), lot.getId());
        assertEquals(Double.valueOf(50.0), lot.getCurrentQuantity());
    }

    @Test
    public void getAvailableLotsByItemFEFO_shouldReturnLotsInFEFOOrder() {
        // lot 1001 (LOT-2025-002) expires 2099-06-30 (earlier)
        // lot 1000 (LOT-2025-001) expires 2099-12-31 (later)
        List<InventoryLot> lots = inventoryLotService.getAvailableLotsByItemFEFO(1000L);

        assertNotNull("Lots should not be null", lots);
        assertEquals("Should have 2 active lots", 2, lots.size());

        // First lot should expire earliest
        assertEquals("First lot should be earliest expiring", "LOT-2025-002", lots.get(0).getLotNumber());
        assertEquals("Second lot should expire later", "LOT-2025-001", lots.get(1).getLotNumber());
    }

    @Test
    public void updateQCStatus_shouldUpdateLotQCStatus() {
        InventoryLot lot = inventoryLotService.get(1000L);
        assertEquals(QCStatus.PASSED, lot.getQcStatus());

        InventoryLot updatedLot = inventoryLotService.updateQCStatus(1000L, QCStatus.FAILED, "QC test failed", "1");

        assertNotNull("Updated lot should not be null", updatedLot);
        assertEquals(QCStatus.FAILED, updatedLot.getQcStatus());

        // Verify persisted
        assertEquals(QCStatus.FAILED, inventoryLotService.get(1000L).getQcStatus());
    }

    @Test
    public void updateLotStatus_shouldUpdateLotStatus() {
        InventoryLot lot = inventoryLotService.get(1000L);
        assertEquals(LotStatus.ACTIVE, lot.getStatus());

        InventoryLot updatedLot = inventoryLotService.updateLotStatus(1000L, LotStatus.IN_USE, "1");

        assertNotNull("Updated lot should not be null", updatedLot);
        assertEquals(LotStatus.IN_USE, updatedLot.getStatus());

        // Verify persisted
        assertEquals(LotStatus.IN_USE, inventoryLotService.get(1000L).getStatus());
    }

    @Test
    public void adjustLotQuantity_shouldUpdateQuantity() {
        InventoryLot lot = inventoryLotService.get(1000L);
        assertEquals(Double.valueOf(100.0), lot.getCurrentQuantity());

        InventoryLot updatedLot = inventoryLotService.adjustLotQuantity(1000L, 75.0, "Test adjustment", "1");

        assertNotNull("Updated lot should not be null", updatedLot);
        assertEquals(Double.valueOf(75.0), updatedLot.getCurrentQuantity());

        // Verify persisted
        assertEquals(Double.valueOf(75.0), inventoryLotService.get(1000L).getCurrentQuantity());
    }

    @Test
    public void openLot_shouldSetDateOpenedAndStatus() {
        InventoryLot lot = inventoryLotService.get(1000L);
        assertNull("Lot should not be opened yet", lot.getDateOpened());
        assertEquals(LotStatus.ACTIVE, lot.getStatus());

        Timestamp openDate = new Timestamp(System.currentTimeMillis());
        InventoryLot openedLot = inventoryLotService.openLot(1000L, openDate, "1");

        assertNotNull("Opened lot should not be null", openedLot);
        assertEquals(LotStatus.IN_USE, openedLot.getStatus());
        assertNotNull("Date opened should be set", openedLot.getDateOpened());
    }

    @Test
    public void disposeLot_shouldSetStatusToDisposed() {
        InventoryLot lot = inventoryLotService.get(1000L);
        assertEquals(LotStatus.ACTIVE, lot.getStatus());

        InventoryLot disposedLot = inventoryLotService.disposeLot(1000L, "Expired", null, "1");

        assertNotNull("Disposed lot should not be null", disposedLot);
        assertEquals(LotStatus.DISPOSED, disposedLot.getStatus());
        assertEquals(Double.valueOf(0.0), disposedLot.getCurrentQuantity());
    }

    @Test
    public void isAvailableForUse_shouldReturnTrueForActivePassedLot() {
        InventoryLot lot = inventoryLotService.get(1000L);

        assertTrue("Lot should be available for use", lot.isAvailableForUse());
    }

    @Test
    public void isAvailableForUse_shouldReturnFalseForDisposedLot() {
        // Dispose the lot first
        inventoryLotService.disposeLot(1000L, "Test disposal", null, "1");

        InventoryLot lot = inventoryLotService.get(1000L);
        assertFalse("Disposed lot should not be available", lot.isAvailableForUse());
    }
}
