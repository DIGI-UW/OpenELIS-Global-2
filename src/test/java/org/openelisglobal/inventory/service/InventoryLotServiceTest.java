package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryLot;

/**
 * inventory_lot.barcode is UNIQUE and nullable, so "no barcode" has to reach
 * the database as NULL from every write path, and a real collision has to be a
 * translatable 400 rather than a constraint violation.
 */
@RunWith(MockitoJUnitRunner.class)
public class InventoryLotServiceTest {

    @Mock
    private InventoryLotDAO inventoryLotDAO;

    @Mock
    private InventoryTransactionService transactionService;

    @InjectMocks
    private InventoryLotServiceImpl inventoryLotService;

    private InventoryLot lot(Long id, String barcode, LotStatus status) {
        InventoryLot lot = new InventoryLot();
        lot.setId(id);
        lot.setLotNumber("LOT-" + id);
        lot.setBarcode(barcode);
        lot.setStatus(status);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setInitialQuantity(10.0);
        lot.setCurrentQuantity(10.0);
        lot.setSysUserId("1");
        return lot;
    }

    @Test
    public void insert_storesNullForBlankBarcode() {
        InventoryLot lot = lot(null, "   ", LotStatus.ACTIVE);

        inventoryLotService.insert(lot);

        assertNull("blank barcode must not reach the UNIQUE column as ''", lot.getBarcode());
        verify(inventoryLotDAO).insert(argThat(saved -> saved.getBarcode() == null));
    }

    @Test
    public void update_storesNullForBlankBarcode() {
        InventoryLot lot = lot(5L, "", LotStatus.ACTIVE);

        inventoryLotService.update(lot);

        assertNull(lot.getBarcode());
        verify(inventoryLotDAO).update(argThat(saved -> saved.getBarcode() == null));
    }

    @Test
    public void insert_rejectsBarcodeAlreadyOnAnotherLot() {
        when(inventoryLotDAO.getByBarcode("BC-123")).thenReturn(lot(7L, "BC-123", LotStatus.ACTIVE));

        try {
            inventoryLotService.insert(lot(null, "BC-123", LotStatus.ACTIVE));
            fail("Expected duplicate barcode to be rejected before insert");
        } catch (LocalizedValidationException expected) {
            assertEquals("inventory.lot.error.duplicateBarcode", expected.getErrorCode());
            assertEquals("BC-123", expected.getParams().get("barcode"));
        }

        verify(inventoryLotDAO, never()).insert(any(InventoryLot.class));
    }

    @Test
    public void update_letsLotKeepItsOwnBarcode() {
        InventoryLot lot = lot(5L, "BC-123", LotStatus.ACTIVE);
        when(inventoryLotDAO.getByBarcode("BC-123")).thenReturn(lot(5L, "BC-123", LotStatus.ACTIVE));

        inventoryLotService.update(lot);

        assertEquals("BC-123", lot.getBarcode());
        verify(inventoryLotDAO).update(lot);
    }

    @Test
    public void adjustLotQuantity_refusesDisposedLot() {
        when(inventoryLotDAO.get(5L)).thenReturn(Optional.of(lot(5L, null, LotStatus.DISPOSED)));

        try {
            inventoryLotService.adjustLotQuantity(5L, 3.0, "recount", "1");
            fail("Expected adjustment of a DISPOSED lot to be refused");
        } catch (IllegalStateException expected) {
            assertEquals("Cannot adjust a DISPOSED lot: LOT-5", expected.getMessage());
        }

        verify(inventoryLotDAO, never()).update(any(InventoryLot.class));
        verify(transactionService, never()).recordTransaction(anyLong(), any(TransactionType.class), anyDouble(),
                anyDouble(), any(), any(), anyString(), anyString());
    }

    @Test
    public void adjustLotQuantity_refusesConsumedLot() {
        when(inventoryLotDAO.get(5L)).thenReturn(Optional.of(lot(5L, null, LotStatus.CONSUMED)));

        try {
            inventoryLotService.adjustLotQuantity(5L, 3.0, "recount", "1");
            fail("Expected adjustment of a CONSUMED lot to be refused");
        } catch (IllegalStateException expected) {
            assertEquals("Cannot adjust a CONSUMED lot: LOT-5", expected.getMessage());
        }

        verify(inventoryLotDAO, never()).update(any(InventoryLot.class));
    }

    @Test
    public void disposeLot_refusesLotAlreadyDisposed() {
        InventoryLot disposed = lot(5L, null, LotStatus.DISPOSED);
        disposed.setCurrentQuantity(0.0);
        when(inventoryLotDAO.get(5L)).thenReturn(Optional.of(disposed));

        try {
            inventoryLotService.disposeLot(5L, "damaged", null, "1");
            fail("Expected a second disposal to be refused");
        } catch (IllegalStateException expected) {
            assertEquals("Lot already disposed: LOT-5", expected.getMessage());
        }

        verify(inventoryLotDAO, never()).update(any(InventoryLot.class));
        verify(transactionService, never()).recordTransaction(anyLong(), any(TransactionType.class), anyDouble(),
                anyDouble(), any(), any(), anyString(), anyString());
    }
}
