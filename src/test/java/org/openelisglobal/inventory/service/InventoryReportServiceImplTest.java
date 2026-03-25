package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.inventory.service.InventoryReportService.GeneratedReport;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;

@RunWith(MockitoJUnitRunner.class)
public class InventoryReportServiceImplTest {

    @Mock
    private InventoryItemService inventoryItemService;

    @Mock
    private InventoryLotService inventoryLotService;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    @InjectMocks
    private InventoryReportServiceImpl inventoryReportService;

    @Test
    public void generateReport_shouldBuildCsvStockLevelsReport_whenRequestIsValid() {
        InventoryItem item = new InventoryItem();
        item.setId(1L);
        item.setFhirUuid(UUID.randomUUID());
        item.setName("Test Reagent");
        item.setItemType(ItemType.REAGENT);
        item.setUnits("mL");
        item.setLowStockThreshold(5);
        item.setIsActive("Y");

        InventoryLot lot = new InventoryLot();
        lot.setId(10L);
        lot.setFhirUuid(UUID.randomUUID());
        lot.setInventoryItem(item);
        lot.setLotNumber("LOT-001");
        lot.setInitialQuantity(10.0);
        lot.setCurrentQuantity(10.0);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setStoragePath("Freezer 1 / Shelf A");

        when(inventoryItemService.getAllActive()).thenReturn(List.of(item));
        when(inventoryLotService.getAll()).thenReturn(List.of(lot));

        GeneratedReport report = inventoryReportService.generateReport("stock_levels", "csv", null, null, false,
                false, false, false);

        assertEquals("text/csv", report.getContentType());
        assertTrue(report.getFileName().matches("inventory_stock_levels_\\d{8}\\.csv"));

        String csv = new String(report.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("\"Item Name\""));
        assertTrue(csv.contains("\"Test Reagent\""));
        assertTrue(csv.contains("\"In Stock\""));
        assertTrue(csv.contains("\"Freezer 1 / Shelf A\""));
    }

    @Test
    public void generateReport_shouldRejectUnsupportedExportFormat() {
        try {
            inventoryReportService.generateReport("stock_levels", "json", null, null, false, false, false, false);
            fail("Expected IllegalArgumentException for unsupported export format");
        } catch (IllegalArgumentException e) {
            assertEquals("Unsupported export format: json", e.getMessage());
        }
    }

    @Test
    public void generateReport_shouldRequireDatesForTransactionHistory() {
        try {
            inventoryReportService.generateReport("transaction_history", "csv", null, null, false, false, false,
                    false);
            fail("Expected IllegalArgumentException when transaction history dates are missing");
        } catch (IllegalArgumentException e) {
            assertEquals("Transaction history report requires startDate and endDate", e.getMessage());
        }
    }
}
