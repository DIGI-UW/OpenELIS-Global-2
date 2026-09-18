package org.openelisglobal.inventory.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.storage.service.SampleStorageService;

/**
 * The cases mocks answer better than a database does: locale-dependent number
 * formatting, the day boundaries of the expiry buckets, and the tag rule.
 * Everything about what each report actually returns is pinned over HTTP in
 * {@code InventoryReportRestControllerTest} instead.
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

    @InjectMocks
    private InventoryReportServiceImpl reportService;

    @Before
    public void setup() {
        when(sampleStorageService.getLocationsForInventoryLots(anyList())).thenReturn(Map.of());
    }

    private InventoryItem item(Long id, String code, String name, String... tags) {
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setCode(code);
        item.setName(name);
        item.setUnits("mL");
        item.setIsActive("Y");
        item.setTags(new java.util.LinkedHashSet<>(List.of(tags)));
        return item;
    }

    private InventoryLot lot(InventoryItem item, String lotNumber, double quantity, LocalDate expiry) {
        InventoryLot lot = new InventoryLot();
        lot.setId((long) Math.abs(lotNumber.hashCode()));
        lot.setInventoryItem(item);
        lot.setLotNumber(lotNumber);
        lot.setCurrentQuantity(quantity);
        lot.setInitialQuantity(quantity);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setQcStatus(QCStatus.PASSED);
        if (expiry != null) {
            lot.setExpirationDate(Timestamp.valueOf(expiry.atStartOfDay()));
        }
        return lot;
    }

    private InventoryReportRequest request(String reportType, Timestamp start, Timestamp end, List<String> tags) {
        return new InventoryReportRequest(reportType, "CSV", start, end, false, true, tags);
    }

    private String cell(ReportTable table, int row, int column) {
        return table.getRows().get(row).get(column);
    }

    @Test
    public void generateReport_unknownType_throws() {
        try {
            reportService.generateReport(request("NOT_A_REPORT", null, null, List.of()));
            throw new AssertionError("expected a validation failure");
        } catch (LocalizedValidationException e) {
            assertEquals("reports.error.unknownReportType", e.getErrorCode());
        }
    }

    @Test
    public void receivedAndConsumed_bothRequireADateRange() {
        for (String reportType : List.of("RECEIVED", "CONSUMED")) {
            try {
                reportService.generateReport(request(reportType, null, null, List.of()));
                throw new AssertionError(reportType + " should require a date range");
            } catch (LocalizedValidationException e) {
                assertEquals(reportType, "reports.error.dateRangeRequired", e.getErrorCode());
            }
        }
    }

    /**
     * Quantities are read by spreadsheets, so they must not pick up the host's
     * decimal comma. Fails with {@code 12,50} if the formatter loses its root
     * locale.
     */
    @Test
    public void quantities_useADotDecimal_underADecimalCommaDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            InventoryItem reagent = item(1L, "R1", "Reagent", "Cartridge");
            when(inventoryItemService.getAllActive()).thenReturn(List.of(reagent));
            when(inventoryLotService.getAll()).thenReturn(List.of(lot(reagent, "L1", 12.5, null)));

            ReportTable table = reportService.generateReport(request("STOCK_ON_HAND", null, null, List.of()));

            assertEquals("12.50", cell(table, 0, 5));
        } finally {
            Locale.setDefault(original);
        }
    }

    /** A lot that went off earlier today is expired, not due this week. */
    @Test
    public void expiring_aLotThatWentOffEarlierToday_isExpiredNotThisWeek() {
        InventoryItem reagent = item(1L, "R1", "Reagent", "Cartridge");
        InventoryLot expiredToday = lot(reagent, "L1", 4, LocalDate.now());
        expiredToday.setExpirationDate(new Timestamp(System.currentTimeMillis() - 60_000));
        when(inventoryItemService.getAllActive()).thenReturn(List.of(reagent));
        when(inventoryLotService.getAll()).thenReturn(List.of(expiredToday));

        ReportTable table = reportService.generateReport(request("EXPIRING", null, null, List.of()));

        assertEquals("Expired", cell(table, 0, 7));
    }

    @Test
    public void expiring_bucketsSitOnTheSevenAndThirtyDayBoundaries() {
        InventoryItem reagent = item(1L, "R1", "Reagent", "Cartridge");
        when(inventoryItemService.getAllActive()).thenReturn(List.of(reagent));
        when(inventoryLotService.getAll()).thenReturn(List.of(lot(reagent, "L7", 1, LocalDate.now().plusDays(8)),
                lot(reagent, "L30", 1, LocalDate.now().plusDays(31))));

        ReportTable table = reportService.generateReport(request("EXPIRING", null, null, List.of()));

        // Expiry sits at midnight, so "in 8 days" is 7 whole days away.
        assertEquals("This week", cell(table, 0, 7));
        assertEquals("This month", cell(table, 1, 7));
    }

    /**
     * Tags reach the report canonicalized on write, but the endpoint is reachable
     * directly, so a differently-cased tag must not silently return nothing.
     */
    @Test
    public void tagFilter_ignoresCase() {
        InventoryItem reagent = item(1L, "R1", "Reagent", "Cartridge");
        when(inventoryItemService.getAllActive()).thenReturn(List.of(reagent));
        when(inventoryLotService.getAll()).thenReturn(List.of(lot(reagent, "L1", 5, null)));

        ReportTable table = reportService.generateReport(request("STOCK_ON_HAND", null, null, List.of("cartridge")));

        assertEquals(2, table.getRows().size());
        assertEquals("R1", cell(table, 0, 0));
    }

    @Test
    public void tagFilter_excludesAnItemCarryingNoneOfTheSelectedTags() {
        InventoryItem reagent = item(1L, "R1", "Reagent", "Cartridge");
        when(inventoryItemService.getAllActive()).thenReturn(List.of(reagent));
        when(inventoryLotService.getAll()).thenReturn(List.of(lot(reagent, "L1", 5, null)));

        ReportTable table = reportService.generateReport(request("STOCK_ON_HAND", null, null, List.of("Consumable")));

        assertTrue("the item is filtered out, so the report is empty: " + table.getRows(), table.getRows().isEmpty());
    }

    /**
     * An empty report must be empty. A totals row over no rows reads as a result
     * and leaves the screen unable to tell "nothing matched" from "one row".
     */
    @Test
    public void anEmptyReportCarriesNoTotalsRow() {
        when(inventoryItemService.getAllActive()).thenReturn(List.of());
        when(inventoryLotService.getAll()).thenReturn(List.of());

        ReportTable table = reportService.generateReport(request("STOCK_ON_HAND", null, null, List.of()));

        assertTrue("no rows at all, not a lone TOTAL: " + table.getRows(), table.getRows().isEmpty());
    }

    @Test
    public void tagsAreListedAlphabeticallyInTheirOwnColumn() {
        InventoryItem reagent = item(1L, "R1", "Reagent", "TB", "Cartridge");
        when(inventoryItemService.getAllActive()).thenReturn(List.of(reagent));
        when(inventoryLotService.getAll()).thenReturn(List.of(lot(reagent, "L1", 5, null)));

        ReportTable table = reportService.generateReport(request("STOCK_ON_HAND", null, null, List.of()));

        assertEquals("Cartridge, TB", cell(table, 0, 2));
    }

    /**
     * With a date, the quantity must come from the replayed log rather than the
     * lot's current figure, and a lot the log does not reach must be left out
     * rather than reported as zero.
     */
    @Test
    public void stockOnHand_withADate_readsTheReplayedLogNotTheLotsOwnQuantity() {
        InventoryItem reagent = item(1L, "R1", "Reagent", "Cartridge");
        InventoryLot replayed = lot(reagent, "L1", 99, null);
        InventoryLot unknown = lot(reagent, "L2", 50, null);
        when(inventoryItemService.getAllActive()).thenReturn(List.of(reagent));
        when(inventoryLotService.getAll()).thenReturn(List.of(replayed, unknown));
        when(inventoryTransactionService.getQuantityOnHandAsOf(any())).thenReturn(Map.of(replayed.getId(), 7.0));

        ReportTable table = reportService.generateReport(
                request("STOCK_ON_HAND", null, Timestamp.valueOf(LocalDate.now().atStartOfDay()), List.of()));

        assertEquals("the replayed lot plus the TOTAL row", 2, table.getRows().size());
        assertEquals("L1", cell(table, 0, 3));
        assertEquals("7", cell(table, 0, 5));
        assertEquals("TOTAL (1 lots)", cell(table, 1, 0));
    }

    @Test
    public void numericColumnsAreDeclaredPerReport_soXlsxTypesQuantitiesNotCodes() {
        InventoryItem reagent = item(1L, "R1", "Reagent", "Cartridge");
        when(inventoryItemService.getAllActive()).thenReturn(List.of(reagent));
        when(inventoryLotService.getAll()).thenReturn(List.of(lot(reagent, "L1", 5, null)));

        ReportTable table = reportService.generateReport(request("STOCK_ON_HAND", null, null, List.of()));

        assertNumericColumns(table, Set.of(5));
    }

    private void assertNumericColumns(ReportTable table, Set<Integer> expected) {
        for (int col = 0; col < table.getHeaders().size(); col++) {
            assertEquals(table.getHeaders().get(col), expected.contains(col), table.isNumericColumn(col));
        }
    }
}
