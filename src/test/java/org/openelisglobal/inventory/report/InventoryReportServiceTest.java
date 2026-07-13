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
import org.openelisglobal.inventory.service.InventoryItemTypeService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.service.InventoryTransactionService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryItemType;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

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
    private InventoryItemTypeService inventoryItemTypeService;
    @Mock
    private SampleStorageService sampleStorageService;
    @Mock
    private SystemUserService systemUserService;

    @InjectMocks
    private InventoryReportServiceImpl reportService;

    @Before
    public void setup() {
        when(sampleStorageService.getLocationsForInventoryLots(anyList())).thenReturn(Map.of());
        when(inventoryItemTypeService.getByCode(anyString())).thenAnswer(invocation -> {
            InventoryItemType type = new InventoryItemType();
            type.setCode(invocation.getArgument(0));
            return type;
        });
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
    public void stockLevels_includesActiveItemsWithTotalQuantityAcrossLots() {
        InventoryItem reagent = item("REAGENT_A", "Reagent A", "REAGENT", true);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(reagent));
        when(inventoryLotService.getByInventoryItemId("REAGENT_A"))
                .thenReturn(List.of(lot(reagent, "LOT1", 10.0), lot(reagent, "LOT2", 5.0)));

        ReportTable table = reportService.generateReport(request("STOCK_LEVELS", "CSV"));

        assertEquals(List.of("Item Code", "Item Name", "Type", "Location", "Total Quantity", "Units", "Status"),
                table.getHeaders());
        assertEquals(1, table.getRows().size());
        List<String> row = table.getRows().get(0);
        assertEquals("REAGENT_A", row.get(0));
        assertEquals("15", row.get(4)); // 10 + 5, formatted as an integer
        assertEquals("Active", row.get(6));
    }

    @Test
    public void lowStock_usesLowStockItemsSource() {
        InventoryItem item = item("RDT_A", "RDT A", "RDT", true);
        item.setLowStockThreshold(20);
        when(inventoryItemService.getLowStockItems()).thenReturn(List.of(item));
        when(inventoryLotService.getByInventoryItemId("RDT_A")).thenReturn(List.of(lot(item, "LOT1", 3.0)));

        ReportTable table = reportService.generateReport(request("LOW_STOCK", "CSV"));

        assertEquals(1, table.getRows().size());
        assertEquals("20", table.getRows().get(0).get(5));
    }

    @Test
    public void expirationForecast_excludesExpiredLotsByDefault_includesWhenRequested() {
        InventoryItem item = item("REAGENT_A", "Reagent A", "REAGENT", true);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(item));

        InventoryLot expiredLot = lot(item, "EXPIRED1", 5.0);
        expiredLot.setExpirationDate(new Timestamp(System.currentTimeMillis() - 86_400_000L));
        InventoryLot futureLot = lot(item, "FUTURE1", 5.0);
        futureLot.setExpirationDate(new Timestamp(System.currentTimeMillis() + 86_400_000L * 30));
        when(inventoryLotService.getAll()).thenReturn(List.of(expiredLot, futureLot));

        InventoryReportRequest excludeExpired = new InventoryReportRequest("EXPIRATION_FORECAST", "CSV", null, null,
                false, false, false, false);
        ReportTable withoutExpired = reportService.generateReport(excludeExpired);
        assertEquals(1, withoutExpired.getRows().size());
        assertEquals("FUTURE1", withoutExpired.getRows().get(0).get(3));

        InventoryReportRequest includeExpired = new InventoryReportRequest("EXPIRATION_FORECAST", "CSV", null, null,
                false, true, false, false);
        ReportTable withExpired = reportService.generateReport(includeExpired);
        assertEquals(2, withExpired.getRows().size());
    }

    @Test
    public void usageTrends_filtersByDateRangeAndResolvesUserName() {
        InventoryItem item = item("REAGENT_A", "Reagent A", "REAGENT", true);
        InventoryLot lot = lot(item, "LOT1", 10.0);
        InventoryUsage usage = new InventoryUsage();
        usage.setInventoryItem(item);
        usage.setLot(lot);
        usage.setQuantityUsed(2.0);
        usage.setUsageDate(new Timestamp(System.currentTimeMillis()));
        usage.setPerformedByUser(1);

        Timestamp start = new Timestamp(System.currentTimeMillis() - 86_400_000L);
        Timestamp end = new Timestamp(System.currentTimeMillis() + 86_400_000L);
        when(inventoryUsageService.getByDateRange(start, end)).thenReturn(List.of(usage));

        SystemUser user = new SystemUser();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        when(systemUserService.get("1")).thenReturn(user);

        InventoryReportRequest req = new InventoryReportRequest("USAGE_TRENDS", "CSV", start, end, false, true, false,
                false);
        ReportTable table = reportService.generateReport(req);

        assertEquals(1, table.getRows().size());
        assertEquals("Jane Doe", table.getRows().get(0).get(5));
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

        assertEquals(2, table.getRows().size());
        assertTrue("CARTRIDGE sorts before REAGENT",
                table.getRows().get(0).get(2).compareTo(table.getRows().get(1).get(2)) < 0);
    }
}
