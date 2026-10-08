package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;

public class InventoryFefoExpiryIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryLotDAO inventoryLotDAO;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryManagementService inventoryManagementService;

    private static final String SYS_USER_ID = "1";

    private Long itemId;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InventoryItem item = new InventoryItem();
        item.setFhirUuid(UUID.randomUUID());
        item.setName("FEFO expiry fixture " + UUID.randomUUID());
        item.setUnits("tests");
        item.setLowStockThreshold(5);
        item.setIsActive("Y");
        item.setSysUserId(SYS_USER_ID);
        itemId = inventoryItemService.insert(item);
    }

    private Timestamp daysFromNow(int days) {
        return Timestamp.valueOf(LocalDateTime.now().plusDays(days));
    }

    private Long createLot(String lotNumber, double quantity, Timestamp expiry) {
        InventoryLot lot = new InventoryLot();
        lot.setFhirUuid(UUID.randomUUID());
        lot.setInventoryItem(inventoryItemService.get(itemId));
        lot.setLotNumber(lotNumber);
        lot.setExpirationDate(expiry);
        lot.setReceiptDate(daysFromNow(-60));
        lot.setInitialQuantity(quantity);
        lot.setCurrentQuantity(quantity);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setSysUserId(SYS_USER_ID);
        return inventoryLotService.insert(lot);
    }

    private List<String> availableLotNumbers() {
        return inventoryLotDAO.getAvailableLotsByItemFEFO(itemId).stream().map(InventoryLot::getLotNumber)
                .collect(Collectors.toList());
    }

    @Test
    public void getAvailableLotsByItemFEFO_omitsAnExpiredLotEvenThoughItSortsFirst() {
        createLot("EXPIRED-YESTERDAY", 40, daysFromNow(-1));
        createLot("GOOD-NEXT-MONTH", 10, daysFromNow(30));

        List<String> available = availableLotNumbers();

        assertEquals("only the unexpired lot is available", List.of("GOOD-NEXT-MONTH"), available);
        assertFalse("an expired lot must never lead the FEFO list", available.contains("EXPIRED-YESTERDAY"));
    }

    @Test
    public void getAvailableLotsByItemFEFO_stillOrdersTheUnexpiredLotsEarliestFirst() {
        createLot("EXPIRED", 5, daysFromNow(-3));
        createLot("LATER", 5, daysFromNow(90));
        createLot("SOONER", 5, daysFromNow(10));

        assertEquals("earliest unexpired first, expired dropped", List.of("SOONER", "LATER"), availableLotNumbers());
    }

    @Test
    public void getAvailableLotsByItemFEFO_treatsALotWithNoExpiryAsUsable() {
        createLot("NO-EXPIRY", 7, null);

        assertEquals(List.of("NO-EXPIRY"), availableLotNumbers());
    }

    @Test
    public void getAvailableLotsByItemFEFO_dropsALotWhoseAfterOpeningExpiryHasPassed() {
        Long lotId = createLot("OPENED-TOO-LONG-AGO", 12, daysFromNow(120));
        InventoryLot opened = inventoryLotService.get(lotId);
        opened.setCalculatedExpiryAfterOpening(daysFromNow(-2));
        opened.setSysUserId(SYS_USER_ID);
        inventoryLotService.update(opened);

        assertTrue("the manufacturer date is still in the future, but the opened lot has gone off",
                availableLotNumbers().isEmpty());
    }

    @Test
    public void consumeInventoryFEFO_refusesToConsumeWhenTheOnlyStockIsExpired() {
        createLot("ALL-WE-HAVE-IS-EXPIRED", 50, daysFromNow(-1));

        try {
            inventoryManagementService.consumeInventoryFEFO(itemId, 1.0, null, null, SYS_USER_ID);
            fail("expired stock must not be consumable through the automatic path");
        } catch (LocalizedValidationException expected) {
            assertEquals("inventory.consume.error.noLots", expected.getErrorCode());
        }

        assertEquals("the expired lot must be left untouched", Double.valueOf(50),
                inventoryLotService.getByInventoryItemId(itemId).get(0).getCurrentQuantity());
    }

    @Test
    public void consumeInventoryFEFO_drawsFromTheUnexpiredLotAndLeavesTheExpiredOneAlone() {
        Long expiredId = createLot("EXPIRED-BUT-FULL", 40, daysFromNow(-1));
        Long usableId = createLot("USABLE", 10, daysFromNow(30));

        inventoryManagementService.consumeInventoryFEFO(itemId, 4.0, null, null, SYS_USER_ID);

        assertEquals("the expired lot keeps every unit", Double.valueOf(40),
                inventoryLotService.get(expiredId).getCurrentQuantity());
        assertEquals("the usable lot absorbs the whole consumption", Double.valueOf(6),
                inventoryLotService.get(usableId).getCurrentQuantity());
    }

    @Test
    public void isInStock_doesNotCountExpiredStock() {
        createLot("EXPIRED-ONLY", 99, daysFromNow(-5));

        assertFalse("99 expired units is not stock", inventoryItemService.isInStock(itemId));

        createLot("FRESH", 1, daysFromNow(5));

        assertTrue("one usable unit is", inventoryItemService.isInStock(itemId));
    }
}
