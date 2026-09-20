package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.inventory.service.InventoryManagementService.ConsumptionRecord;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;

/**
 * check-availability must answer with the rule consume enforces, and the
 * QC-gate refusal must tell the user which item and which kind of stock is in
 * the way rather than quoting a numeric id.
 */
@RunWith(MockitoJUnitRunner.class)
public class InventoryManagementServiceAvailabilityTest {

    @Mock
    private InventoryItemService inventoryItemService;

    @Mock
    private InventoryLotService inventoryLotService;

    @Mock
    private InventoryTransactionService transactionService;

    @Mock
    private InventoryUsageService usageService;

    @InjectMocks
    private InventoryManagementServiceImpl service;

    private InventoryItem item;

    @Before
    public void setup() {
        item = new InventoryItem();
        item.setId(13L);
        item.setCode("BLOOD_AGAR");
        item.setName("Blood agar");
        lenient().when(inventoryItemService.get(13L)).thenReturn(item);
        lenient().when(inventoryLotService.getAvailableLotsByItemFEFO(13L)).thenReturn(List.of());
    }

    private InventoryLot lot(double quantity, LotStatus status, QCStatus qcStatus) {
        InventoryLot lot = new InventoryLot();
        lot.setInventoryItem(item);
        lot.setLotNumber("LOT-" + qcStatus + "-" + quantity);
        lot.setInitialQuantity(quantity);
        lot.setCurrentQuantity(quantity);
        lot.setStatus(status);
        lot.setQcStatus(qcStatus);
        return lot;
    }

    @Test
    public void isSufficientInventoryAvailable_countsOnlyStockConsumeWouldAccept() {
        when(inventoryLotService.getByInventoryItemId(13L)).thenReturn(List.of(lot(50.0, LotStatus.ACTIVE,
                QCStatus.PASSED), lot(500.0, LotStatus.ACTIVE, QCStatus.PENDING)));
        // The raw ACTIVE/IN_USE total the old check used; a revert to it must fail here.
        lenient().when(inventoryLotService.getTotalCurrentQuantity(13L)).thenReturn(550.0);

        assertTrue(service.isSufficientInventoryAvailable(13L, 50.0));
        assertFalse("stock awaiting QC cannot be consumed, so it is not available",
                service.isSufficientInventoryAvailable(13L, 51.0));
    }

    @Test
    public void isSufficientInventoryAvailable_ignoresExpiredLots() {
        InventoryLot expired = lot(100.0, LotStatus.ACTIVE, QCStatus.PASSED);
        expired.setExpirationDate(new Timestamp(System.currentTimeMillis() - 60_000));
        when(inventoryLotService.getByInventoryItemId(13L)).thenReturn(List.of(expired));
        lenient().when(inventoryLotService.getTotalCurrentQuantity(13L)).thenReturn(100.0);

        assertFalse(service.isSufficientInventoryAvailable(13L, 1.0));
    }

    @Test
    public void consumeInventoryFEFO_namesItemAndCountsUnexpiredStockAwaitingQc() {
        InventoryLot expiredPending = lot(5.0, LotStatus.ACTIVE, QCStatus.PENDING);
        expiredPending.setExpirationDate(new Timestamp(System.currentTimeMillis() - 60_000));
        when(inventoryLotService.getByInventoryItemId(13L))
                .thenReturn(List.of(lot(5.0, LotStatus.ACTIVE, QCStatus.PENDING),
                        lot(0.0, LotStatus.CONSUMED, QCStatus.PENDING), expiredPending));

        LocalizedValidationException refusal = consumeExpectingRefusal();

        assertEquals("inventory.consume.error.noLotsAwaitingQc", refusal.getErrorCode());
        assertEquals("BLOOD_AGAR", refusal.getParams().get("code"));
        assertEquals("Blood agar", refusal.getParams().get("name"));
        assertEquals("1", refusal.getParams().get("count"));
        assertTrue(refusal.getMessage(), refusal.getMessage().contains("BLOOD_AGAR"));
        assertFalse("the numeric id is not a name", refusal.getMessage().contains("13"));
    }

    @Test
    public void consumeInventoryFEFO_reportsStockThatFailedQc() {
        when(inventoryLotService.getByInventoryItemId(13L))
                .thenReturn(List.of(lot(8.0, LotStatus.ACTIVE, QCStatus.FAILED)));

        LocalizedValidationException refusal = consumeExpectingRefusal();

        assertEquals("inventory.consume.error.noLotsQcFailed", refusal.getErrorCode());
        assertEquals("1", refusal.getParams().get("count"));
    }

    @Test
    public void consumeInventoryFEFO_reportsQuarantinedStockByEitherStatusField() {
        when(inventoryLotService.getByInventoryItemId(13L)).thenReturn(List.of(
                lot(8.0, LotStatus.QUARANTINED, QCStatus.PASSED), lot(2.0, LotStatus.ACTIVE, QCStatus.QUARANTINED)));

        LocalizedValidationException refusal = consumeExpectingRefusal();

        assertEquals("inventory.consume.error.noLotsQuarantined", refusal.getErrorCode());
        assertEquals("2", refusal.getParams().get("count"));
    }

    @Test
    public void consumeInventoryFEFO_callsAQuarantinedLotQuarantinedThoughItsQcIsStillPending() {
        // A lot received into quarantine keeps the default PENDING qc; passing QC
        // would not release it, so do not ask for that.
        when(inventoryLotService.getByInventoryItemId(13L))
                .thenReturn(List.of(lot(6.0, LotStatus.QUARANTINED, QCStatus.PENDING)));

        LocalizedValidationException refusal = consumeExpectingRefusal();

        assertEquals("inventory.consume.error.noLotsQuarantined", refusal.getErrorCode());
        assertEquals("1", refusal.getParams().get("count"));
    }

    @Test
    public void consumeInventoryFEFO_reportsNoStockWhenNothingIsInTheWay() {
        when(inventoryLotService.getByInventoryItemId(13L)).thenReturn(List.of());

        LocalizedValidationException refusal = consumeExpectingRefusal();

        assertEquals("inventory.consume.error.noLots", refusal.getErrorCode());
        assertEquals("BLOOD_AGAR", refusal.getParams().get("code"));
    }

    @Test
    public void consumeInventoryFEFO_skipsExpiredLotsTheFefoQueryReturns() {
        InventoryLot expired = lot(5.0, LotStatus.ACTIVE, QCStatus.PASSED);
        expired.setExpirationDate(new Timestamp(System.currentTimeMillis() - 60_000));
        InventoryLot fresh = lot(3.0, LotStatus.ACTIVE, QCStatus.PASSED);
        when(inventoryLotService.getAvailableLotsByItemFEFO(13L)).thenReturn(List.of(expired, fresh));

        List<ConsumptionRecord> consumed = service.consumeInventoryFEFO(13L, 2.0, null, null, "9");

        assertEquals(1, consumed.size());
        assertEquals(fresh.getLotNumber(), consumed.get(0).getLotNumber());
        assertEquals("expired stock is never handed out", Double.valueOf(5.0), expired.getCurrentQuantity());
        assertEquals(Double.valueOf(1.0), fresh.getCurrentQuantity());
    }

    @Test
    public void consumeInventoryFEFO_refusesWhenTheOnlyPassedLotIsExpired() {
        InventoryLot expired = lot(5.0, LotStatus.ACTIVE, QCStatus.PASSED);
        expired.setExpirationDate(new Timestamp(System.currentTimeMillis() - 60_000));
        when(inventoryLotService.getAvailableLotsByItemFEFO(13L)).thenReturn(List.of(expired));
        when(inventoryLotService.getByInventoryItemId(13L)).thenReturn(List.of(expired));

        LocalizedValidationException refusal = consumeExpectingRefusal();

        assertEquals("inventory.consume.error.noLots", refusal.getErrorCode());
        assertEquals(Double.valueOf(5.0), expired.getCurrentQuantity());
    }

    private LocalizedValidationException consumeExpectingRefusal() {
        try {
            service.consumeInventoryFEFO(13L, 1.0, null, null, "9");
            fail("Expected consumption to be refused");
            return null;
        } catch (LocalizedValidationException expected) {
            return expected;
        }
    }
}
