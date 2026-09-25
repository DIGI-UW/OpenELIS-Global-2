package org.openelisglobal.inventory.report;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
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
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.systemuser.service.SystemUserService;

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

    private InventoryItem item(Long id, String code, String name, ItemType itemType, boolean active) {
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setCode(code);
        item.setName(name);
        item.setItemType(itemType);
        item.setUnits("mL");
        item.setIsActive(active ? "Y" : "N");
        return item;
    }

    private InventoryLot lot(InventoryItem item, String lotNumber, double currentQuantity, QCStatus qcStatus) {
        InventoryLot lot = new InventoryLot();
        lot.setId((long) lotNumber.hashCode());
        lot.setInventoryItem(item);
        lot.setLotNumber(lotNumber);
        lot.setCurrentQuantity(currentQuantity);
        lot.setInitialQuantity(currentQuantity);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setQcStatus(qcStatus);
        return lot;
    }

    private InventoryReportRequest request(String reportType, String exportFormat) {
        return new InventoryReportRequest(reportType, exportFormat, null, null, false, true, false, false);
    }

    private void assertNumericColumns(ReportTable table, int... expected) {
        java.util.Set<Integer> expectedSet = java.util.Arrays.stream(expected).boxed()
                .collect(java.util.stream.Collectors.toSet());
        for (int col = 0; col < table.getHeaders().size(); col++) {
            assertEquals(table.getHeaders().get(col), expectedSet.contains(col), table.isNumericColumn(col));
        }
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
        InventoryItem reagent = item(1000L, "REAGENT_A", "Reagent A", ItemType.REAGENT, true);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(reagent));

        InventoryLot usableLot = lot(reagent, "LOT1", 10.0, QCStatus.PASSED);
        InventoryLot expiredLot = lot(reagent, "LOT2", 5.0, QCStatus.PASSED);
        expiredLot.setStatus(LotStatus.EXPIRED);
        when(inventoryLotService.getAll()).thenReturn(List.of(usableLot, expiredLot));

        ReportTable table = reportService.generateReport(request("STOCK_LEVELS", "CSV"));

        assertEquals(List.of("Item Code", "Item Name", "Type", "Category", "Location", "Available Quantity",
                "Total Quantity", "Units", "Status"), table.getHeaders());
        assertNumericColumns(table, 5, 6);
        assertEquals(2, table.getRows().size());
        List<String> row = table.getRows().get(0);
        assertEquals("REAGENT_A", row.get(0));
        assertEquals("10", row.get(5));
        assertEquals("15", row.get(6));
        assertEquals("Active", row.get(8));

        List<String> totals = table.getRows().get(1);
        assertEquals("TOTAL (1 items)", totals.get(0));
        assertEquals("10", totals.get(5));
        assertEquals("15", totals.get(6));
    }

    @Test
    public void lowStock_selectsOnAvailableQuantity_soDeadStockCannotPadTheTotal() {
        InventoryItem lowOnAvailable = item(1001L, "RDT_A", "RDT A", ItemType.RDT, true);
        lowOnAvailable.setLowStockThreshold(20);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(lowOnAvailable));

        InventoryLot usable = lot(lowOnAvailable, "LOT1", 3.0, QCStatus.PASSED);
        InventoryLot disposed = lot(lowOnAvailable, "LOT2", 100.0, QCStatus.PASSED);
        disposed.setStatus(LotStatus.DISPOSED);
        when(inventoryLotService.getAll()).thenReturn(List.of(usable, disposed));

        ReportTable table = reportService.generateReport(request("LOW_STOCK", "CSV"));

        assertEquals(List.of("Item Code", "Item Name", "Type", "Category", "Location", "Available Quantity",
                "Total Quantity", "Low Stock Threshold", "Units"), table.getHeaders());
        assertNumericColumns(table, 5, 6, 7);
        assertEquals(2, table.getRows().size());
        List<String> row = table.getRows().get(0);
        assertEquals("RDT_A", row.get(0));
        assertEquals("3", row.get(5));
        assertEquals("103", row.get(6));
        assertEquals("20", row.get(7));
    }

    @Test
    public void lowStock_omitsItemsWithEnoughAvailableStockOrNoThreshold() {
        InventoryItem wellStocked = item(1001L, "RDT_A", "RDT A", ItemType.RDT, true);
        wellStocked.setLowStockThreshold(20);
        InventoryItem noThreshold = item(1002L, "RDT_B", "RDT B", ItemType.RDT, true);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(wellStocked, noThreshold));
        when(inventoryLotService.getAll()).thenReturn(List.of(lot(wellStocked, "LOT1", 50.0, QCStatus.PASSED),
                lot(noThreshold, "LOT2", 1.0, QCStatus.PASSED)));

        ReportTable table = reportService.generateReport(request("LOW_STOCK", "CSV"));

        assertEquals(1, table.getRows().size());
        assertEquals("TOTAL (0 items)", table.getRows().get(0).get(0));
    }

    @Test
    public void stockLevels_availableCountsQcPendingLots_notQcFailedOrQuarantinedOnes() {
        InventoryItem reagent = item(1000L, "REAGENT_A", "Reagent A", ItemType.REAGENT, true);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(reagent));
        when(inventoryLotService.getAll()).thenReturn(List.of(lot(reagent, "PENDING1", 10.0, QCStatus.PENDING),
                lot(reagent, "PASSED1", 5.0, QCStatus.PASSED), lot(reagent, "FAILED1", 100.0, QCStatus.FAILED),
                lot(reagent, "QUARANTINED1", 50.0, QCStatus.QUARANTINED)));

        ReportTable table = reportService.generateReport(request("STOCK_LEVELS", "CSV"));

        List<String> row = table.getRows().get(0);
        assertEquals("REAGENT_A", row.get(0));
        assertEquals("15", row.get(5));
        assertEquals("165", row.get(6));
    }

    @Test
    public void lowStock_qcPendingStockKeepsAnItemOffTheReport() {
        InventoryItem awaitingQc = item(1001L, "RDT_A", "RDT A", ItemType.RDT, true);
        awaitingQc.setLowStockThreshold(20);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(awaitingQc));
        when(inventoryLotService.getAll()).thenReturn(List.of(lot(awaitingQc, "LOT1", 500.0, QCStatus.PENDING)));

        ReportTable table = reportService.generateReport(request("LOW_STOCK", "CSV"));

        assertEquals(1, table.getRows().size());
        assertEquals("TOTAL (0 items)", table.getRows().get(0).get(0));
    }

    @Test
    public void lowStock_qcFailedStockDoesNotKeepAnItemOffTheReport() {
        InventoryItem failedQc = item(1001L, "RDT_A", "RDT A", ItemType.RDT, true);
        failedQc.setLowStockThreshold(20);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(failedQc));
        when(inventoryLotService.getAll()).thenReturn(
                List.of(lot(failedQc, "LOT1", 3.0, QCStatus.PASSED), lot(failedQc, "LOT2", 500.0, QCStatus.FAILED)));

        ReportTable table = reportService.generateReport(request("LOW_STOCK", "CSV"));

        assertEquals(2, table.getRows().size());
        List<String> row = table.getRows().get(0);
        assertEquals("RDT_A", row.get(0));
        assertEquals("3", row.get(5));
        assertEquals("503", row.get(6));
    }

    @Test
    public void quantities_useADotDecimalUnderADecimalCommaDefaultLocale() {
        InventoryItem reagent = item(1000L, "REAGENT_A", "Reagent A", ItemType.REAGENT, true);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(reagent));
        when(inventoryLotService.getAll()).thenReturn(List.of(lot(reagent, "LOT1", 1.5, QCStatus.PASSED)));

        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.FRANCE);
        try {
            ReportTable table = reportService.generateReport(request("STOCK_LEVELS", "CSV"));
            assertEquals("1.50", table.getRows().get(0).get(5));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    public void expirationForecast_excludesExpiredLotsByDefault_includesWhenRequested() {
        InventoryItem item = item(1000L, "REAGENT_A", "Reagent A", ItemType.REAGENT, true);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(item));

        InventoryLot expiredLot = lot(item, "EXPIRED1", 5.0, QCStatus.PASSED);
        expiredLot.setExpirationDate(new Timestamp(System.currentTimeMillis() - 86_400_000L));
        InventoryLot futureLot = lot(item, "FUTURE1", 5.0, QCStatus.PASSED);
        futureLot.setExpirationDate(new Timestamp(System.currentTimeMillis() + 86_400_000L * 45));
        when(inventoryLotService.getAll()).thenReturn(List.of(expiredLot, futureLot));

        InventoryReportRequest excludeExpired = new InventoryReportRequest("EXPIRATION_FORECAST", "CSV", null, null,
                false, false, false, false);
        ReportTable withoutExpired = reportService.generateReport(excludeExpired);
        assertEquals(1, withoutExpired.getRows().size());
        assertEquals("FUTURE1", withoutExpired.getRows().get(0).get(3));
        assertEquals("LATER", withoutExpired.getRows().get(0).get(7));

        InventoryReportRequest includeExpired = new InventoryReportRequest("EXPIRATION_FORECAST", "CSV", null, null,
                false, true, false, false);
        ReportTable withExpired = reportService.generateReport(includeExpired);
        assertEquals(2, withExpired.getRows().size());
        List<String> expiredRow = withExpired.getRows().stream().filter(r -> r.get(3).equals("EXPIRED1")).findFirst()
                .orElseThrow();
        assertEquals("EXPIRED", expiredRow.get(7));
    }

    @Test
    public void expirationForecast_lotExpiredEarlierTodayIsExpired_notThisWeek() {
        InventoryItem item = item(1000L, "REAGENT_A", "Reagent A", ItemType.REAGENT, true);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(item));
        InventoryLot expiredThisMorning = lot(item, "TODAY1", 5.0, QCStatus.PASSED);
        expiredThisMorning.setExpirationDate(new Timestamp(System.currentTimeMillis() - 12 * 3600_000L));
        when(inventoryLotService.getAll()).thenReturn(List.of(expiredThisMorning));

        ReportTable table = reportService.generateReport(
                new InventoryReportRequest("EXPIRATION_FORECAST", "CSV", null, null, false, true, false, false));

        List<String> row = table.getRows().get(0);
        assertEquals("-1", row.get(6));
        assertEquals("EXPIRED", row.get(7));
    }

    @Test
    public void expirationForecast_honorsDateRange() {
        InventoryItem item = item(1000L, "REAGENT_A", "Reagent A", ItemType.REAGENT, true);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(item));

        InventoryLot soonLot = lot(item, "SOON1", 5.0, QCStatus.PASSED);
        soonLot.setExpirationDate(new Timestamp(System.currentTimeMillis() + 86_400_000L * 5));
        InventoryLot laterLot = lot(item, "LATER1", 5.0, QCStatus.PASSED);
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
        InventoryItem heavilyUsed = item(1000L, "REAGENT_A", "Reagent A", ItemType.REAGENT, true);
        InventoryItem lightlyUsed = item(1002L, "REAGENT_B", "Reagent B", ItemType.REAGENT, true);

        InventoryUsage use1 = new InventoryUsage();
        use1.setInventoryItem(heavilyUsed);
        use1.setLot(lot(heavilyUsed, "LOT1", 10.0, QCStatus.PASSED));
        use1.setQuantityUsed(6.0);
        use1.setUsageDate(new Timestamp(System.currentTimeMillis() - 3600_000L));
        InventoryUsage use2 = new InventoryUsage();
        use2.setInventoryItem(heavilyUsed);
        use2.setLot(lot(heavilyUsed, "LOT1", 10.0, QCStatus.PASSED));
        use2.setQuantityUsed(4.0);
        use2.setUsageDate(new Timestamp(System.currentTimeMillis()));
        InventoryUsage use3 = new InventoryUsage();
        use3.setInventoryItem(lightlyUsed);
        use3.setLot(lot(lightlyUsed, "LOT2", 10.0, QCStatus.PASSED));
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
        assertNumericColumns(table, 3, 4, 5);
        assertEquals(3, table.getRows().size());
        List<String> topRow = table.getRows().get(0);
        assertEquals("REAGENT_A", topRow.get(0));
        assertEquals("10", topRow.get(3));
        assertEquals("2", topRow.get(4));
        assertEquals("5", topRow.get(5));

        List<String> totals = table.getRows().get(2);
        assertEquals("TOTAL (2 items)", totals.get(0));
        assertEquals("11", totals.get(3));
        assertEquals("3", totals.get(4));
    }

    @Test
    public void transactionHistory_rendersEachTransactionAsARow() {
        InventoryItem item = item(1000L, "REAGENT_A", "Reagent A", ItemType.REAGENT, true);
        InventoryLot lot = lot(item, "LOT1", 10.0, QCStatus.PASSED);
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
        List<String> row = table.getRows().get(0);
        assertEquals("REAGENT_A", row.get(1));
        assertEquals("LOT1", row.get(3));
        assertEquals("RECEIPT", row.get(4));
        assertEquals("10", row.get(5));
        assertEquals("1", row.get(7));
    }

    @Test
    public void lotTraceability_excludesInactiveItemsUnlessRequested() {
        InventoryItem activeItem = item(1003L, "ACTIVE_A", "Active Item", ItemType.REAGENT, true);
        InventoryItem inactiveItem = item(1004L, "INACTIVE_A", "Inactive Item", ItemType.REAGENT, false);
        when(inventoryLotService.getAll()).thenReturn(List.of(lot(activeItem, "LOT1", 5.0, QCStatus.PASSED),
                lot(inactiveItem, "LOT2", 5.0, QCStatus.PASSED)));

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
        InventoryItem cartridge = item(1005L, "CART_A", "Zeta Cartridge", ItemType.CARTRIDGE, true);
        InventoryItem reagent = item(1000L, "REAGENT_A", "Alpha Reagent", ItemType.REAGENT, true);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(reagent, cartridge));
        when(inventoryLotService.getAll()).thenReturn(List.of());

        InventoryReportRequest req = new InventoryReportRequest("STOCK_LEVELS", "CSV", null, null, false, true, true,
                false);
        ReportTable table = reportService.generateReport(req);

        assertEquals(3, table.getRows().size());
        assertEquals("Zeta Cartridge", table.getRows().get(0).get(1));
        assertEquals("Alpha Reagent", table.getRows().get(1).get(1));
    }
}
