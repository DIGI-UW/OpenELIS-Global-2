package org.openelisglobal.inventory.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.service.InventoryTransactionService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.systemuser.service.SystemUserService;

/**
 * OGC-658 follow-up — the Reports tab (InventoryReports.jsx) called
 * /rest/inventory/reports/generate, which never existed; this covers the
 * service layer backing the new endpoint.
 */
@RunWith(MockitoJUnitRunner.class)
public class InventoryReportServiceTest {

    @Mock
    private InventoryItemService inventoryItemService;
    @Mock
    private InventoryLotService inventoryLotService;
    @Mock
    private InventoryUsageService inventoryUsageService;
    @Mock
    private InventoryTransactionService inventoryTransactionService;
    @Mock
    private SampleStorageService sampleStorageService;
    @Mock
    private SystemUserService systemUserService;

    @InjectMocks
    private InventoryReportServiceImpl reportService;

    @Before
    public void setup() {
        when(sampleStorageService.getLocationsForInventoryLots(anyList())).thenReturn(Map.of());
    }

    private InventoryItem item(String id, String name, String itemType, boolean active) {
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setName(name);
        item.setItemType(itemType);
        item.setUnits("mL");
        item.setIsActive(active ? "Y" : "N");
        return item;
    }

    private InventoryLot lot(InventoryItem item, String lotNumber, double currentQuantity) {
        InventoryLot lot = new InventoryLot();
        lot.setId((long) lotNumber.hashCode());
        lot.setInventoryItem(item);
        lot.setLotNumber(lotNumber);
        lot.setCurrentQuantity(currentQuantity);
        lot.setInitialQuantity(currentQuantity);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setQcStatus(QCStatus.PASSED);
        return lot;
    }

    private InventoryReportRequest request(String reportType, String exportFormat) {
        return new InventoryReportRequest(reportType, exportFormat, null, null, false, true, false, false);
    }

    @Test
    public void generateReport_unknownType_throws() {
        try {
            reportService.generateReport(request("NOT_A_REPORT", "CSV"));
            org.junit.Assert.fail("Expected LocalizedValidationException");
        } catch (LocalizedValidationException e) {
            assertEquals("reports.error.unknownReportType", e.getErrorCode());
        }
    }

    @Test
    public void generateReport_usageTrendsWithoutDateRange_throws() {
        try {
            reportService.generateReport(request("USAGE_TRENDS", "CSV"));
            org.junit.Assert.fail("Expected LocalizedValidationException");
        } catch (LocalizedValidationException e) {
            assertEquals("reports.error.dateRangeRequired", e.getErrorCode());
        }
    }

    @Test
    public void generateReport_transactionHistoryWithoutDateRange_throws() {
        try {
            reportService.generateReport(request("TRANSACTION_HISTORY", "CSV"));
            org.junit.Assert.fail("Expected LocalizedValidationException");
        } catch (LocalizedValidationException e) {
            assertEquals("reports.error.dateRangeRequired", e.getErrorCode());
        }
    }

    @Test
    public void stockLevels_totalIncludesEveryLot_availableExcludesDeadStock() {
        InventoryItem reagent = item("REAGENT_A", "Reagent A", "REAGENT", true);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(reagent));

        InventoryLot usableLot = lot(reagent, "LOT1", 10.0);
        InventoryLot expiredLot = lot(reagent, "LOT2", 5.0);
        expiredLot.setStatus(LotStatus.EXPIRED);
        when(inventoryLotService.getByInventoryItemId("REAGENT_A")).thenReturn(List.of(usableLot, expiredLot));

        ReportTable table = reportService.generateReport(request("STOCK_LEVELS", "CSV"));

        assertEquals(List.of("Item Code", "Item Name", "Type", "Category", "Location", "Available Quantity",
                "Total Quantity", "Units", "Status"), table.getHeaders());
        assertEquals(2, table.getRows().size()); // data row + totals row
        List<String> row = table.getRows().get(0);
        assertEquals("REAGENT_A", row.get(0));
        assertEquals("10", row.get(5)); // Available: only the usable lot
        assertEquals("15", row.get(6)); // Total: 10 + 5, including the expired lot
        assertEquals("Active", row.get(8));

        List<String> totals = table.getRows().get(1);
        assertEquals("TOTAL (1 items)", totals.get(0));
        assertEquals("10", totals.get(5));
        assertEquals("15", totals.get(6));
    }

    @Test
    public void lowStock_displaysAvailableAndTotalQuantitySeparately() {
        // InventoryItemService.getLowStockItems() (unit-tested on its own in
        // InventoryItemServiceLowStockTest) already judges "low stock" against
        // available quantity — this report just displays whatever it returns,
        // alongside the raw total for transparency.
        InventoryItem lowOnAvailable = item("RDT_A", "RDT A", "RDT", true);
        lowOnAvailable.setLowStockThreshold(20);
        when(inventoryItemService.getLowStockItems()).thenReturn(List.of(lowOnAvailable));

        InventoryLot usable = lot(lowOnAvailable, "LOT1", 3.0);
        InventoryLot disposed = lot(lowOnAvailable, "LOT2", 100.0);
        disposed.setStatus(LotStatus.DISPOSED);
        when(inventoryLotService.getByInventoryItemId("RDT_A")).thenReturn(List.of(usable, disposed));

        ReportTable table = reportService.generateReport(request("LOW_STOCK", "CSV"));

        assertEquals(2, table.getRows().size()); // RDT_A + totals row
        List<String> row = table.getRows().get(0);
        assertEquals("RDT_A", row.get(0));
        assertEquals("3", row.get(5)); // Available Quantity
        assertEquals("103", row.get(6)); // Total Quantity (3 usable + 100 disposed)
        assertEquals("20", row.get(7)); // Low Stock Threshold
    }

    @Test
    public void expirationForecast_excludesExpiredLotsByDefault_includesWhenRequested() {
        InventoryItem item = item("REAGENT_A", "Reagent A", "REAGENT", true);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(item));

        InventoryLot expiredLot = lot(item, "EXPIRED1", 5.0);
        expiredLot.setExpirationDate(new Timestamp(System.currentTimeMillis() - 86_400_000L));
        InventoryLot futureLot = lot(item, "FUTURE1", 5.0);
        futureLot.setExpirationDate(new Timestamp(System.currentTimeMillis() + 86_400_000L * 45));
        when(inventoryLotService.getAll()).thenReturn(List.of(expiredLot, futureLot));

        InventoryReportRequest excludeExpired = new InventoryReportRequest("EXPIRATION_FORECAST", "CSV", null, null,
                false, false, false, false);
        ReportTable withoutExpired = reportService.generateReport(excludeExpired);
        assertEquals(1, withoutExpired.getRows().size());
        assertEquals("FUTURE1", withoutExpired.getRows().get(0).get(3));
        assertEquals("LATER", withoutExpired.getRows().get(0).get(7)); // 45 days out — Urgency bucket

        InventoryReportRequest includeExpired = new InventoryReportRequest("EXPIRATION_FORECAST", "CSV", null, null,
                false, true, false, false);
        ReportTable withExpired = reportService.generateReport(includeExpired);
        assertEquals(2, withExpired.getRows().size());
        List<String> expiredRow = withExpired.getRows().stream().filter(r -> r.get(3).equals("EXPIRED1")).findFirst()
                .orElseThrow();
        assertEquals("EXPIRED", expiredRow.get(7));
    }

    @Test
    public void expirationForecast_honorsDateRange_previouslySilentlyIgnored() {
        InventoryItem item = item("REAGENT_A", "Reagent A", "REAGENT", true);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(item));

        InventoryLot soonLot = lot(item, "SOON1", 5.0);
        soonLot.setExpirationDate(new Timestamp(System.currentTimeMillis() + 86_400_000L * 5));
        InventoryLot laterLot = lot(item, "LATER1", 5.0);
        laterLot.setExpirationDate(new Timestamp(System.currentTimeMillis() + 86_400_000L * 90));
        when(inventoryLotService.getAll()).thenReturn(List.of(soonLot, laterLot));

        InventoryReportRequest scoped = new InventoryReportRequest("EXPIRATION_FORECAST", "CSV",
                new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis() + 86_400_000L * 10),
                false, true, false, false);
        ReportTable table = reportService.generateReport(scoped);

        assertEquals(1, table.getRows().size());
        assertEquals("SOON1", table.getRows().get(0).get(3));
    }

    @Test
    public void usageTrends_aggregatesPerItem_sortedByHeaviestUseFirst() {
        InventoryItem heavilyUsed = item("REAGENT_A", "Reagent A", "REAGENT", true);
        InventoryItem lightlyUsed = item("REAGENT_B", "Reagent B", "REAGENT", true);

        InventoryUsage use1 = new InventoryUsage();
        use1.setInventoryItem(heavilyUsed);
        use1.setLot(lot(heavilyUsed, "LOT1", 10.0));
        use1.setQuantityUsed(6.0);
        use1.setUsageDate(new Timestamp(System.currentTimeMillis() - 3600_000L));
        InventoryUsage use2 = new InventoryUsage();
        use2.setInventoryItem(heavilyUsed);
        use2.setLot(lot(heavilyUsed, "LOT1", 10.0));
        use2.setQuantityUsed(4.0);
        use2.setUsageDate(new Timestamp(System.currentTimeMillis()));
        InventoryUsage use3 = new InventoryUsage();
        use3.setInventoryItem(lightlyUsed);
        use3.setLot(lot(lightlyUsed, "LOT2", 10.0));
        use3.setQuantityUsed(1.0);
        use3.setUsageDate(new Timestamp(System.currentTimeMillis()));

        Timestamp start = new Timestamp(System.currentTimeMillis() - 86_400_000L);
        Timestamp end = new Timestamp(System.currentTimeMillis() + 86_400_000L);
        when(inventoryUsageService.getByDateRange(start, end)).thenReturn(List.of(use1, use2, use3));

        InventoryReportRequest req = new InventoryReportRequest("USAGE_TRENDS", "CSV", start, end, false, true, false,
                false);
        ReportTable table = reportService.generateReport(req);

        assertEquals(List.of("Item Code", "Item Name", "Type", "Total Quantity Used", "Usage Events",
                "Avg Quantity Per Use", "First Use", "Last Use"), table.getHeaders());
        assertEquals(3, table.getRows().size()); // 2 items + totals row
        List<String> topRow = table.getRows().get(0);
        assertEquals("REAGENT_A", topRow.get(0)); // heaviest use sorts first
        assertEquals("10", topRow.get(3)); // 6 + 4
        assertEquals("2", topRow.get(4)); // 2 usage events
        assertEquals("5", topRow.get(5)); // average per use

        List<String> totals = table.getRows().get(2);
        assertEquals("TOTAL (2 items)", totals.get(0));
        assertEquals("11", totals.get(3)); // 10 + 1
        assertEquals("3", totals.get(4)); // 3 events total
    }

    @Test
    public void transactionHistory_filtersByDateRange() {
        InventoryItem item = item("REAGENT_A", "Reagent A", "REAGENT", true);
        InventoryLot lot = lot(item, "LOT1", 10.0);
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setLot(lot);
        transaction.setTransactionType(TransactionType.RECEIPT);
        transaction.setQuantityChange(10.0);
        transaction.setQuantityAfter(10.0);
        transaction.setTransactionDate(new Timestamp(System.currentTimeMillis()));
        transaction.setPerformedByUser(1);

        Timestamp start = new Timestamp(System.currentTimeMillis() - 86_400_000L);
        Timestamp end = new Timestamp(System.currentTimeMillis() + 86_400_000L);
        when(inventoryTransactionService.getByDateRange(start, end)).thenReturn(List.of(transaction));
        when(systemUserService.get(anyString())).thenReturn(null);

        InventoryReportRequest req = new InventoryReportRequest("TRANSACTION_HISTORY", "CSV", start, end, false, true,
                false, false);
        ReportTable table = reportService.generateReport(req);

        assertEquals(1, table.getRows().size());
        assertEquals("RECEIPT", table.getRows().get(0).get(4));
    }

    @Test
    public void lotTraceability_excludesInactiveItemsUnlessRequested() {
        InventoryItem activeItem = item("ACTIVE_A", "Active Item", "REAGENT", true);
        InventoryItem inactiveItem = item("INACTIVE_A", "Inactive Item", "REAGENT", false);
        when(inventoryLotService.getAll())
                .thenReturn(List.of(lot(activeItem, "LOT1", 5.0), lot(inactiveItem, "LOT2", 5.0)));

        ReportTable onlyActive = reportService.generateReport(
                new InventoryReportRequest("LOT_TRACEABILITY", "CSV", null, null, false, true, false, false));
        assertEquals(1, onlyActive.getRows().size());
        assertEquals("ACTIVE_A", onlyActive.getRows().get(0).get(0));

        ReportTable withInactive = reportService.generateReport(
                new InventoryReportRequest("LOT_TRACEABILITY", "CSV", null, null, true, true, false, false));
        assertEquals(2, withInactive.getRows().size());
    }

    @Test
    public void stockLevels_groupByType_sortsRowsByTypeThenName() {
        InventoryItem cartridge = item("CART_A", "Cartridge A", "CARTRIDGE", true);
        InventoryItem reagent = item("REAGENT_A", "Reagent A", "REAGENT", true);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(cartridge, reagent));
        when(inventoryLotService.getByInventoryItemId(any())).thenReturn(List.of());

        InventoryReportRequest req = new InventoryReportRequest("STOCK_LEVELS", "CSV", null, null, false, true, true,
                false);
        ReportTable table = reportService.generateReport(req);

        assertEquals(3, table.getRows().size()); // 2 items + totals row
        assertTrue("CARTRIDGE sorts before REAGENT",
                table.getRows().get(0).get(2).compareTo(table.getRows().get(1).get(2)) < 0);
    }
}
