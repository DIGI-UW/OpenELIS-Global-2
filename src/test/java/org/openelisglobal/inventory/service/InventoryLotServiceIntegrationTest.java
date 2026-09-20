package org.openelisglobal.inventory.service;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

@Rollback
public class InventoryLotServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    InventoryLotService inventoryLotService;

    @Autowired
    InventoryItemService inventoryItemService;

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
    public void getByBarcode_shouldFindLotByBarcode() {
        InventoryLot lot = inventoryLotService.getByBarcode("LOT-BC-1001");

        assertNotNull("Should find lot by barcode", lot);
        assertEquals(Long.valueOf(1001L), lot.getId());
        assertEquals("LOT-2025-002", lot.getLotNumber());
    }

    @Test
    public void getByBarcode_shouldTrimSurroundingWhitespace() {
        // A barcode pasted into the search box can carry surrounding spaces.
        InventoryLot lot = inventoryLotService.getByBarcode("  LOT-BC-1000  ");

        assertNotNull("Should find lot despite surrounding whitespace", lot);
        assertEquals(Long.valueOf(1000L), lot.getId());
    }

    @Test
    public void getByBarcode_shouldMatchTheStoredFormWhenCaseOrSeparatorsDiffer() {
        // Hand-keyed from a damaged label; barcodes this service writes are kebab.
        assertEquals(Long.valueOf(1000L), inventoryLotService.getByBarcode("lot-bc-1000").getId());
        assertEquals(Long.valueOf(1000L), inventoryLotService.getByBarcode("lot bc 1000").getId());
    }

    @Test
    public void getByBarcode_shouldReturnNullWhenNoLotMatches() {
        assertNull("Unknown barcode should not match a lot", inventoryLotService.getByBarcode("NO-SUCH-BARCODE"));
    }

    @Test
    public void getByBarcode_shouldReturnNullForBlankInputRatherThanMatchingABarcodelessLot() {
        // Lot 1002 carries no usable barcode; a blank scan must not resolve to it.
        assertNull("Null barcode should not match", inventoryLotService.getByBarcode(null));
        assertNull("Empty barcode should not match", inventoryLotService.getByBarcode(""));
        assertNull("Whitespace barcode should not match", inventoryLotService.getByBarcode("   "));
    }

    private InventoryLot newLot(String lotNumber, String barcode) {
        InventoryLot lot = new InventoryLot();
        // The REST controller mints the FHIR UUID; a service-level insert must too.
        lot.setFhirUuid(java.util.UUID.randomUUID());
        lot.setInventoryItem(inventoryItemService.get(1000L));
        lot.setLotNumber(lotNumber);
        lot.setBarcode(barcode);
        lot.setExpirationDate(Timestamp.valueOf("2099-01-01 00:00:00"));
        lot.setReceiptDate(Timestamp.valueOf("2025-01-01 00:00:00"));
        lot.setInitialQuantity(10.0);
        lot.setCurrentQuantity(10.0);
        lot.setQcStatus(QCStatus.PENDING);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setSysUserId("1");
        return lot;
    }

    @Test
    public void insert_shouldGenerateBarcodeFromItemCodeAndLotNumberWhenBlank() {
        InventoryLot saved = inventoryLotService.get(inventoryLotService.insert(newLot("LOT-2025-900", null)));

        // Item 1000's code is TEST_REAGENT_A; CodeGenerator separates with hyphens.
        assertEquals("TEST-REAGENT-A-LOT-2025-900", saved.getBarcode());
    }

    @Test
    public void insert_shouldGenerateBarcodeWhenSuppliedValueIsBlank() {
        InventoryLot saved = inventoryLotService.get(inventoryLotService.insert(newLot("LOT-2025-901", "   ")));

        assertEquals("TEST-REAGENT-A-LOT-2025-901", saved.getBarcode());
    }

    @Test
    public void insert_shouldNotRepeatTheItemCodeWhenTheLotNumberAlreadyCarriesIt() {
        // A naive seed produced TEST-REAGENT-A-TEST-REAGENT-A-20260810.
        InventoryLot saved = inventoryLotService
                .get(inventoryLotService.insert(newLot("TEST_REAGENT_A-20260810", null)));

        assertEquals("TEST-REAGENT-A-20260810", saved.getBarcode());
    }

    @Test
    public void insert_shouldNormalizeAnExplicitlySuppliedBarcode() {
        InventoryLot saved = inventoryLotService.get(inventoryLotService.insert(newLot("LOT-2025-902", "my-own bc/1")));

        assertEquals("MY-OWN-BC-1", saved.getBarcode());
    }

    @Test
    public void insert_shouldSuffixAGeneratedBarcodeRatherThanCollide() {
        inventoryLotService.insert(newLot("LOT-2025-903", null));
        InventoryLot second = inventoryLotService.get(inventoryLotService.insert(newLot("LOT-2025-903", null)));

        assertEquals("TEST-REAGENT-A-LOT-2025-903-2", second.getBarcode());
    }

    @Test(expected = LocalizedValidationException.class)
    public void insert_shouldRejectAnExplicitBarcodeThatAlreadyExists() {
        inventoryLotService.insert(newLot("LOT-2025-904", "LOT-BC-9999"));
        inventoryLotService.insert(newLot("LOT-2025-905", "LOT-BC-9999"));
    }

    @Test
    public void update_shouldKeepTheGeneratedBarcodeWhenTheLotIsSavedUnchanged() {
        InventoryLot saved = inventoryLotService.get(inventoryLotService.insert(newLot("LOT-2025-906", null)));

        inventoryLotService.update(saved);

        assertEquals("Re-saving a lot must not reject its own barcode", "TEST-REAGENT-A-LOT-2025-906",
                inventoryLotService.get(saved.getId()).getBarcode());
    }

    @Test
    public void update_shouldNormalizeTheFirstBarcodeGivenToABarcodelessLot() {
        // Lot 1002 predates minting on insert, so the edit modal enables its field.
        InventoryLot barcodeless = inventoryLotService.get(1002L);
        barcodeless.setBarcode("ph 001");
        barcodeless.setSysUserId("1");

        inventoryLotService.update(barcodeless);

        assertEquals("PH-001", inventoryLotService.get(1002L).getBarcode());
    }

    @Test
    public void update_shouldRejectAFirstBarcodeThatNormalizesOntoAnotherLot() {
        // Lot 1000 holds LOT-BC-1000; the duplicate check must see the final form.
        InventoryLot barcodeless = inventoryLotService.get(1002L);
        barcodeless.setBarcode("lot bc 1000");
        barcodeless.setSysUserId("1");

        try {
            inventoryLotService.update(barcodeless);
            fail("Expected the normalized barcode to collide with lot 1000");
        } catch (LocalizedValidationException expected) {
            assertEquals("inventory.lot.error.duplicateBarcode", expected.getErrorCode());
        }

        // Fixture 1002 stands in for a barcodeless lot with '', so it stays blank.
        assertEquals("", inventoryLotService.get(1002L).getBarcode());
    }

    @Test
    public void update_shouldNotReshapeABarcodeTheLotAlreadyHolds() {
        InventoryLot saved = inventoryLotService.get(inventoryLotService.insert(newLot("LOT-2025-910", null)));
        saved.setBarcode("legacy bc 910");
        inventoryLotService.update(saved);

        InventoryLot reloaded = inventoryLotService.get(saved.getId());
        reloaded.setCurrentQuantity(5.0);
        inventoryLotService.update(reloaded);

        assertEquals("A stored barcode must survive an unrelated save", "legacy bc 910",
                inventoryLotService.get(saved.getId()).getBarcode());
    }

    @Test
    public void update_shouldStoreNullWhenTheBarcodeIsClearedRatherThanMintingANewOne() {
        InventoryLot saved = inventoryLotService.get(inventoryLotService.insert(newLot("LOT-2025-907", null)));
        saved.setBarcode("   ");

        inventoryLotService.update(saved);

        assertNull("Update must not mint a replacement barcode", inventoryLotService.get(saved.getId()).getBarcode());
    }

    @Test
    public void getAvailableLotsByItemFEFO_shouldReturnLotsInFEFOOrder() {
        // Lot 1001 expires 2025-06-30 (earlier), lot 1000 expires 2025-12-31 (later).
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

    // Every fixture lot has expired by now, so the test owns its lot.
    private Long insertPassedLot(String lotNumber) {
        InventoryLot lot = newLot(lotNumber, null);
        lot.setQcStatus(QCStatus.PASSED);
        return inventoryLotService.insert(lot);
    }

    @Test
    public void isAvailableForUse_shouldReturnTrueForActivePassedLot() {
        Long id = insertPassedLot("LOT-2025-908");

        assertTrue("Lot should be available for use", inventoryLotService.get(id).isAvailableForUse());
    }

    @Test
    public void isAvailableForUse_shouldReturnFalseForDisposedLot() {
        Long id = insertPassedLot("LOT-2025-909");

        inventoryLotService.disposeLot(id, "Test disposal", null, "1");

        assertFalse("Disposed lot should not be available", inventoryLotService.get(id).isAvailableForUse());
    }
}
