package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class InventoryManagementServiceSelectedLotTest {

    @Mock
    private InventoryLotService inventoryLotService;

    @Mock
    private InventoryTransactionService transactionService;

    @Mock
    private InventoryUsageService usageService;

    private InventoryManagementService service;

    @Before
    public void setUp() {
        InventoryManagementServiceImpl implementation = new InventoryManagementServiceImpl();
        ReflectionTestUtils.setField(implementation, "inventoryLotService", inventoryLotService);
        ReflectionTestUtils.setField(implementation, "transactionService", transactionService);
        ReflectionTestUtils.setField(implementation, "usageService", usageService);
        service = implementation;
    }

    @Test
    public void consumeSelectedLotRevalidatesAndConsumesOnlyThatLot() {
        InventoryLot lot = availableLot(7L, "MEDIA-2026-07", 10.0);
        InventoryUsage usage = new InventoryUsage();
        usage.setId(31L);
        when(inventoryLotService.getForUpdate(7L)).thenReturn(lot);
        when(usageService.recordUsage(lot, 2.0, null, 41L, "9")).thenReturn(usage);

        InventoryUsage recorded = service.consumeSelectedLot(7L, 2.0, null, 41L, "9");

        assertSame(usage, recorded);
        assertEquals(Double.valueOf(8.0), lot.getCurrentQuantity());
        verify(inventoryLotService).update(lot);
        verify(transactionService).recordTransaction(7L,
                org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType.CONSUMPTION, -2.0, 8.0, null,
                org.openelisglobal.inventory.valueholder.InventoryEnums.ReferenceType.MANUAL.name(),
                "Consumed selected lot for analysis 41", "9");
        verify(usageService).recordUsage(lot, 2.0, null, 41L, "9");
    }

    @Test
    public void consumeSelectedLotRejectsExpiredLotWithNamedReason() {
        InventoryLot lot = availableLot(7L, "MEDIA-EXPIRED", 10.0);
        lot.setExpirationDate(new Timestamp(System.currentTimeMillis() - 60_000));
        when(inventoryLotService.getForUpdate(7L)).thenReturn(lot);

        try {
            service.consumeSelectedLot(7L, 1.0, null, 41L, "9");
            fail("Expected expired lot to be rejected");
        } catch (InventoryLotUnavailableException expected) {
            assertEquals("INVENTORY_LOT_EXPIRED", expected.getCode());
            assertTrue(expected.getMessage().contains("MEDIA-EXPIRED"));
        }

        verify(inventoryLotService, never()).update(lot);
        verify(usageService, never()).recordUsage(lot, 1.0, null, 41L, "9");
    }

    @Test
    public void consumeSelectedLotRejectsLotWhenQcNoLongerPassed() {
        InventoryLot lot = availableLot(7L, "CARD-QC-FAILED", 10.0);
        lot.setQcStatus(QCStatus.FAILED);
        when(inventoryLotService.getForUpdate(7L)).thenReturn(lot);

        try {
            service.consumeSelectedLot(7L, 1.0, null, 41L, "9");
            fail("Expected failed-QC lot to be rejected");
        } catch (InventoryLotUnavailableException expected) {
            assertEquals("INVENTORY_LOT_QC_FAILED", expected.getCode());
            assertTrue(expected.getMessage().contains("CARD-QC-FAILED"));
        }

        verify(inventoryLotService, never()).update(lot);
    }

    @Test
    public void consumeSelectedLotRejectsInsufficientQuantityWithoutPartialConsumption() {
        InventoryLot lot = availableLot(7L, "DISC-LOW", 0.5);
        when(inventoryLotService.getForUpdate(7L)).thenReturn(lot);

        try {
            service.consumeSelectedLot(7L, 1.0, null, 41L, "9");
            fail("Expected insufficient selected-lot quantity to be rejected");
        } catch (InventoryLotUnavailableException expected) {
            assertEquals("INVENTORY_LOT_INSUFFICIENT_QUANTITY", expected.getCode());
            assertTrue(expected.getMessage().contains("DISC-LOW"));
        }

        assertEquals(Double.valueOf(0.5), lot.getCurrentQuantity());
        verify(inventoryLotService, never()).update(lot);
    }

    private InventoryLot availableLot(Long id, String number, Double quantity) {
        InventoryItem item = new InventoryItem();
        item.setId(13L);
        item.setName("Blood agar");
        InventoryLot lot = new InventoryLot();
        lot.setId(id);
        lot.setInventoryItem(item);
        lot.setLotNumber(number);
        lot.setCurrentQuantity(quantity);
        lot.setInitialQuantity(quantity);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setQcStatus(QCStatus.PASSED);
        return lot;
    }
}
