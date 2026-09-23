package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Covers the four period reports end to end over HTTP: what each one returns
 * for a known date range, the tag filter, the half-open date bounds, the
 * on-screen preview, all three export formats, and the validation paths.
 *
 * <p>
 * Stock is received through {@code /rest/inventory/management/receive} rather
 * than written with SQL, so the reports are read against rows the product
 * itself produced. The one exception is back-dating, which no endpoint offers.
 */
public class InventoryReportRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String CODE_PREFIX = "RPTTEST-";

    @Autowired
    private javax.sql.DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbc;
    private MockHttpSession mockSession;
    private long itemId;
    private long lotId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbc = new JdbcTemplate(dataSource);
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        mockSession = new MockHttpSession();
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        cleanup();
        createItemAndLot();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.inventory_usage WHERE inventory_item_id IN "
                + "(SELECT id FROM clinlims.inventory_item WHERE code LIKE ?)", CODE_PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.inventory_transaction WHERE lot_id IN "
                + "(SELECT l.id FROM clinlims.inventory_lot l JOIN clinlims.inventory_item i "
                + "ON i.id = l.inventory_item_id WHERE i.code LIKE ?)", CODE_PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.inventory_lot WHERE inventory_item_id IN "
                + "(SELECT id FROM clinlims.inventory_item WHERE code LIKE ?)", CODE_PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.inventory_item_tag WHERE item_id IN "
                + "(SELECT id FROM clinlims.inventory_item WHERE code LIKE ?)", CODE_PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.inventory_item WHERE code LIKE ?", CODE_PREFIX + "%");
    }

    /**
     * The lot is created through {@code POST /rest/inventory/lots}, which records
     * no transaction. That is deliberate: it is the fixture the as-of replay has to
     * cope with, and it keeps the receipts written by later tests countable.
     */
    private void createItemAndLot() throws Exception {
        itemId = createItem(CODE_PREFIX + "REAGENT", CODE_PREFIX + "Reagent", List.of("Cartridge"));

        HashMap<String, Object> lot = new HashMap<>();
        HashMap<String, Object> lotItem = new HashMap<>();
        lotItem.put("id", itemId);
        lot.put("inventoryItem", lotItem);
        lot.put("lotNumber", CODE_PREFIX + "LOT1");
        lot.put("initialQuantity", 25);
        lot.put("currentQuantity", 25);
        MvcResult lotResult = mockMvc.perform(post("/rest/inventory/lots").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(lot))).andReturn();
        assertEquals("test setup: lot creation failed - " + lotResult.getResponse().getContentAsString(), 201,
                lotResult.getResponse().getStatus());
        lotId = objectMapper.readTree(lotResult.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createItem(String code, String name, List<String> tags) throws Exception {
        HashMap<String, Object> item = new HashMap<>();
        item.put("code", code);
        item.put("name", name);
        item.put("itemType", "REAGENT");
        item.put("units", "mL");
        item.put("tags", tags);
        MvcResult result = mockMvc.perform(post("/rest/inventory/items").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(item))).andReturn();
        assertEquals("test setup: item creation failed - " + result.getResponse().getContentAsString(), 201,
                result.getResponse().getStatus());
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    /** Receives through the product's own endpoint, so a RECEIPT row is written. */
    private long receive(long forItemId, String lotNumber, double quantity) throws Exception {
        HashMap<String, Object> lot = new HashMap<>();
        HashMap<String, Object> lotItem = new HashMap<>();
        lotItem.put("id", forItemId);
        lot.put("inventoryItem", lotItem);
        lot.put("lotNumber", lotNumber);
        lot.put("initialQuantity", quantity);
        lot.put("currentQuantity", quantity);
        MvcResult result = mockMvc
                .perform(post("/rest/inventory/management/receive").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(lot)))
                .andReturn();
        assertEquals("test setup: receive failed - " + result.getResponse().getContentAsString(), 201,
                result.getResponse().getStatus());
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void recordTransactionAt(Timestamp transactionDate) {
        jdbc.update(
                "INSERT INTO clinlims.inventory_transaction (id, lot_id, transaction_type, quantity_change,"
                        + " quantity_after, transaction_date, performed_by_user)"
                        + " VALUES (nextval('clinlims.inventory_transaction_seq'), ?, 'CONSUMPTION', -5, 20, ?, 1)",
                lotId, transactionDate);
    }

    private void backdate(String table, String dateColumn, Timestamp when) {
        jdbc.update("UPDATE clinlims." + table + " SET " + dateColumn + " = ? WHERE lot_id = ?", when, lotId);
    }

    private MvcResult generate(String reportType, String format, String... extraParams) throws Exception {
        var request = post("/rest/inventory/reports/generate").session(mockSession).param("reportType", reportType)
                .param("exportFormat", format).contentType(MediaType.APPLICATION_JSON).content("{}");
        for (int i = 0; i < extraParams.length; i += 2) {
            request = request.param(extraParams[i], extraParams[i + 1]);
        }
        return mockMvc.perform(request).andReturn();
    }

    private MvcResult preview(String reportType, String... extraParams) throws Exception {
        var request = post("/rest/inventory/reports/preview").session(mockSession).param("reportType", reportType)
                .contentType(MediaType.APPLICATION_JSON).content("{}");
        for (int i = 0; i < extraParams.length; i += 2) {
            request = request.param(extraParams[i], extraParams[i + 1]);
        }
        return mockMvc.perform(request).andReturn();
    }

    private String csvOf(MvcResult result) throws Exception {
        assertEquals("expected 200 but body was " + result.getResponse().getContentAsString(), 200,
                result.getResponse().getStatus());
        return result.getResponse().getContentAsString();
    }

    /**
     * This fixture's own data rows. These reports cover the whole instance and the
     * other inventory test classes leave rows behind in the same database, so a
     * count of every row in the report is a count of whatever else ran first.
     */
    private List<String> dataRowsOf(String csv) {
        List<String> rows = new ArrayList<>();
        String[] lines = csv.split("\r?\n");
        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].isBlank() && !lines[i].startsWith("TOTAL") && lines[i].contains(CODE_PREFIX)) {
                rows.add(lines[i]);
            }
        }
        return rows;
    }

    private String totalRowOf(String csv) {
        for (String line : csv.split("\r?\n")) {
            if (line.startsWith("TOTAL")) {
                return line;
            }
        }
        return "";
    }

    /**
     * The totals row sums the whole report, foreign rows included, so it is checked
     * for shape and a lower bound rather than an exact figure. The exact arithmetic
     * is pinned on controlled data in {@code InventoryReportServiceTest}.
     */
    private void assertTotalsRow(String csv, String unitLabel, int column, double atLeast) {
        String total = totalRowOf(csv);
        assertTrue("a totals row is present and counts " + unitLabel + ": " + total,
                total.matches("TOTAL \\(\\d+ " + unitLabel + "\\).*"));
        String[] cells = total.split(",", -1);
        assertTrue("totals row carries a summed quantity in column " + column + ": " + total,
                cells.length > column && !cells[column].isBlank());
        assertTrue("total " + cells[column] + " should be at least " + atLeast,
                Double.parseDouble(cells[column]) >= atLeast);
    }

    // --- RECEIVED ---

    @Test
    public void received_showsWhatArrivedInThePeriod_withItsLotAndQuantity() throws Exception {
        passQc(receive(itemId, CODE_PREFIX + "DELIVERY1", 40));
        // Consumption writes a transaction to the same table in the same period, and
        // this report must not count it as an arrival.
        consume(itemId, 6);
        LocalDate today = LocalDate.now();

        String csv = csvOf(generate("RECEIVED", "CSV", "startDate", today.toString(), "endDate", today.toString()));

        List<String> rows = dataRowsOf(csv);
        assertEquals("exactly the one receipt made in the period: " + csv, 1, rows.size());
        assertTrue(rows.get(0), rows.get(0).contains(CODE_PREFIX + "DELIVERY1"));
        assertTrue(rows.get(0), rows.get(0).contains(CODE_PREFIX + "REAGENT"));
        assertTrue("quantity received: " + rows.get(0), rows.get(0).contains(",40,"));
        assertTotalsRow(csv, "receipts", 5, 40);
    }

    /**
     * The setUp lot was created without a receipt, and a period report must not
     * invent one for it. This is the difference between "what arrived" and "what
     * exists".
     */
    @Test
    public void received_omitsStockThatWasNeverRecordedAsReceived() throws Exception {
        LocalDate today = LocalDate.now();

        String csv = csvOf(generate("RECEIVED", "CSV", "startDate", today.toString(), "endDate", today.toString()));

        assertFalse("the transaction-less setUp lot has no receipt to report: " + csv,
                csv.contains(CODE_PREFIX + "LOT1"));
        assertTrue(csv, dataRowsOf(csv).isEmpty());
    }

    @Test
    public void received_requiresADateRange() throws Exception {
        MvcResult result = generate("RECEIVED", "CSV");

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("reports.error.dateRangeRequired", errorCodeOf(result));
    }

    // --- CONSUMED ---

    @Test
    public void consumed_aggregatesPerItemOverThePeriod() throws Exception {
        passQc(receive(itemId, CODE_PREFIX + "DELIVERY2", 50));
        consume(itemId, 6);
        consume(itemId, 4);
        LocalDate today = LocalDate.now();

        String csv = csvOf(generate("CONSUMED", "CSV", "startDate", today.toString(), "endDate", today.toString()));

        List<String> rows = dataRowsOf(csv);
        assertEquals("one row per item, not per usage event: " + csv, 1, rows.size());
        assertTrue(rows.get(0), rows.get(0).startsWith(CODE_PREFIX + "REAGENT," + CODE_PREFIX + "Reagent,Cartridge,"));
        assertTrue("10 used across 2 events, averaging 5: " + rows.get(0), rows.get(0).contains(",10,2,5,"));
        assertTotalsRow(csv, "items", 3, 10);
    }

    @Test
    public void consumed_requiresADateRange() throws Exception {
        MvcResult result = generate("CONSUMED", "CSV");

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("reports.error.dateRangeRequired", errorCodeOf(result));
    }

    // --- STOCK ON HAND ---

    @Test
    public void stockOnHand_withNoDate_reportsEveryLotHoldingStockNow() throws Exception {
        receive(itemId, CODE_PREFIX + "DELIVERY3", 12);

        String csv = csvOf(generate("STOCK_ON_HAND", "CSV"));

        List<String> rows = dataRowsOf(csv);
        assertEquals("both the setUp lot and the received one: " + csv, 2, rows.size());
        assertTrue(csv, csv.contains(CODE_PREFIX + "LOT1"));
        assertTrue(csv, csv.contains(CODE_PREFIX + "DELIVERY3"));
        assertTotalsRow(csv, "lots", 5, 37);
    }

    /**
     * The quantity at a past date comes from the transaction log, not the lot's
     * current figure. Receiving 12 today must not change what yesterday's report
     * says.
     */
    @Test
    public void stockOnHand_asOfAPastDate_replaysTheTransactionLog() throws Exception {
        long receivedLotId = receive(itemId, CODE_PREFIX + "DELIVERY4", 12);
        passQc(receivedLotId);
        LocalDate today = LocalDate.now();
        // Move that receipt two days back so "as of yesterday" has something to find.
        jdbc.update("UPDATE clinlims.inventory_transaction SET transaction_date = ? WHERE lot_id = ?",
                Timestamp.valueOf(today.minusDays(2).atTime(9, 0)), receivedLotId);
        // Then consume from it today, which the past-date report must not see.
        consume(itemId, 5);

        String yesterday = csvOf(generate("STOCK_ON_HAND", "CSV", "endDate", today.minusDays(1).toString()));

        List<String> rows = dataRowsOf(yesterday);
        assertEquals("only the lot that had a transaction before yesterday ended: " + yesterday, 1, rows.size());
        assertTrue(rows.get(0), rows.get(0).contains(CODE_PREFIX + "DELIVERY4"));
        assertTrue("the full 12, before today's consumption: " + rows.get(0), rows.get(0).contains(",12,"));
        assertFalse("the transaction-less lot cannot be replayed: " + yesterday,
                yesterday.contains(CODE_PREFIX + "LOT1"));
    }

    // --- EXPIRING ---

    @Test
    public void expiring_listsLotsExpiringInTheWindowSoonestFirst() throws Exception {
        long soonId = receive(itemId, CODE_PREFIX + "SOON", 5);
        long laterId = receive(itemId, CODE_PREFIX + "LATER", 5);
        LocalDate today = LocalDate.now();
        setExpiry(laterId, today.plusDays(20));
        setExpiry(soonId, today.plusDays(3));

        String csv = csvOf(
                generate("EXPIRING", "CSV", "startDate", today.toString(), "endDate", today.plusDays(30).toString()));

        List<String> rows = dataRowsOf(csv);
        assertEquals("both lots fall in the window: " + csv, 2, rows.size());
        assertTrue("soonest first: " + rows.get(0), rows.get(0).contains(CODE_PREFIX + "SOON"));
        assertTrue(rows.get(1), rows.get(1).contains(CODE_PREFIX + "LATER"));
        assertTrue("3 days out is this week: " + rows.get(0), rows.get(0).contains("This week"));
        assertTrue("20 days out is this month: " + rows.get(1), rows.get(1).contains("This month"));
    }

    @Test
    public void expiring_omitsLotsExpiringAfterTheWindow() throws Exception {
        long farOffId = receive(itemId, CODE_PREFIX + "FAROFF", 5);
        LocalDate today = LocalDate.now();
        setExpiry(farOffId, today.plusDays(90));

        String csv = csvOf(
                generate("EXPIRING", "CSV", "startDate", today.toString(), "endDate", today.plusDays(30).toString()));

        assertFalse("90 days out is outside a 30-day window: " + csv, csv.contains(CODE_PREFIX + "FAROFF"));
    }

    // --- the tag filter ---

    @Test
    public void tagFilter_keepsOnlyItemsCarryingOneOfTheSelectedTags() throws Exception {
        long gloveItemId = createItem(CODE_PREFIX + "GLOVE", CODE_PREFIX + "Gloves", List.of("Consumable"));
        receive(gloveItemId, CODE_PREFIX + "GLOVEBOX", 7);
        receive(itemId, CODE_PREFIX + "DELIVERY5", 9);
        LocalDate today = LocalDate.now();

        String consumables = csvOf(generate("RECEIVED", "CSV", "startDate", today.toString(), "endDate",
                today.toString(), "tags", "Consumable"));

        assertTrue("the gloves carry Consumable: " + consumables, consumables.contains(CODE_PREFIX + "GLOVEBOX"));
        assertFalse("the reagent carries Cartridge, not Consumable: " + consumables,
                consumables.contains(CODE_PREFIX + "DELIVERY5"));
        assertEquals(1, dataRowsOf(consumables).size());
    }

    @Test
    public void tagFilter_matchesAnySelectedTag_notAllOfThem() throws Exception {
        long gloveItemId = createItem(CODE_PREFIX + "GLOVE", CODE_PREFIX + "Gloves", List.of("Consumable"));
        receive(gloveItemId, CODE_PREFIX + "GLOVEBOX", 7);
        receive(itemId, CODE_PREFIX + "DELIVERY6", 9);
        LocalDate today = LocalDate.now();

        String both = csvOf(generate("RECEIVED", "CSV", "startDate", today.toString(), "endDate", today.toString(),
                "tags", "Consumable", "tags", "Cartridge"));

        assertEquals("an item matching either tag is in: " + both, 2, dataRowsOf(both).size());
    }

    @Test
    public void noTagFilter_meansEveryItem_notItemsWithoutTags() throws Exception {
        long gloveItemId = createItem(CODE_PREFIX + "GLOVE", CODE_PREFIX + "Gloves", List.of("Consumable"));
        receive(gloveItemId, CODE_PREFIX + "GLOVEBOX", 7);
        receive(itemId, CODE_PREFIX + "DELIVERY7", 9);
        LocalDate today = LocalDate.now();

        String all = csvOf(generate("RECEIVED", "CSV", "startDate", today.toString(), "endDate", today.toString()));

        assertEquals(2, dataRowsOf(all).size());
    }

    // --- date bounds ---

    @Test
    public void endDateCoversTheWholeDayItNames() throws Exception {
        receive(itemId, CODE_PREFIX + "LATEDELIVERY", 3);
        LocalDate today = LocalDate.now();
        jdbc.update(
                "UPDATE clinlims.inventory_transaction SET transaction_date = ? WHERE lot_id IN"
                        + " (SELECT id FROM clinlims.inventory_lot WHERE lot_number = ?)",
                Timestamp.valueOf(today.atTime(23, 59, 59, 999_999_000)), CODE_PREFIX + "LATEDELIVERY");

        String csv = csvOf(generate("RECEIVED", "CSV", "startDate", today.toString(), "endDate", today.toString()));

        assertTrue("a receipt in the final microsecond of the end day is still in the range: " + csv,
                csv.contains(CODE_PREFIX + "LATEDELIVERY"));
    }

    @Test
    public void theDayAfterTheEndDateIsOutsideTheRange() throws Exception {
        receive(itemId, CODE_PREFIX + "TOMORROW", 3);
        LocalDate today = LocalDate.now();
        jdbc.update(
                "UPDATE clinlims.inventory_transaction SET transaction_date = ? WHERE lot_id IN"
                        + " (SELECT id FROM clinlims.inventory_lot WHERE lot_number = ?)",
                Timestamp.valueOf(today.plusDays(1).atStartOfDay()), CODE_PREFIX + "TOMORROW");

        String csv = csvOf(generate("RECEIVED", "CSV", "startDate", today.toString(), "endDate", today.toString()));

        assertFalse("next-day midnight is outside a half-open range: " + csv, csv.contains(CODE_PREFIX + "TOMORROW"));
    }

    @Test
    public void consumedHonoursTheSameEndDayAsReceived() throws Exception {
        passQc(receive(itemId, CODE_PREFIX + "DELIVERY8", 20));
        consume(itemId, 4);
        LocalDate today = LocalDate.now();
        jdbc.update("UPDATE clinlims.inventory_usage SET usage_date = ? WHERE inventory_item_id = ?",
                Timestamp.valueOf(today.atTime(23, 59, 59, 999_999_000)), itemId);

        String csv = csvOf(generate("CONSUMED", "CSV", "startDate", today.toString(), "endDate", today.toString()));

        assertTrue("usage in the final microsecond of the end day is in the range: " + csv,
                csv.contains(CODE_PREFIX + "REAGENT"));
    }

    // --- the on-screen preview ---

    @Test
    public void preview_returnsTheSameTableAsJsonSoTheScreenCanShowIt() throws Exception {
        receive(itemId, CODE_PREFIX + "DELIVERY9", 15);
        LocalDate today = LocalDate.now();

        MvcResult result = preview("RECEIVED", "startDate", today.toString(), "endDate", today.toString());

        assertEquals(200, result.getResponse().getStatus());
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("Received", body.path("title").asText());
        assertEquals("Date Received", body.path("headers").get(0).asText());
        assertEquals("Quantity Received", body.path("headers").get(5).asText());
        JsonNode ownRows = objectMapper.createArrayNode();
        body.path("rows").forEach(row -> {
            if (row.toString().contains(CODE_PREFIX)) {
                ((com.fasterxml.jackson.databind.node.ArrayNode) ownRows).add(row);
            }
        });
        assertEquals("the one receipt this test made: " + body.path("rows"), 1, ownRows.size());
        assertEquals(CODE_PREFIX + "DELIVERY9", ownRows.get(0).get(4).asText());
        assertEquals("15", ownRows.get(0).get(5).asText());
    }

    @Test
    public void preview_reportsAValidationFailureAsBadRequest() throws Exception {
        MvcResult result = preview("CONSUMED");

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("reports.error.dateRangeRequired", errorCodeOf(result));
    }

    // --- export formats ---

    @Test
    public void stockOnHandCsv_carriesTheItemAndItsTags() throws Exception {
        String csv = csvOf(generate("STOCK_ON_HAND", "CSV"));

        assertTrue(csv, csv.startsWith("Item Code,Item Name,Tags,Lot Number,Expiration Date,Quantity,Location,Status"));
        assertTrue(csv, csv.contains(
                CODE_PREFIX + "REAGENT," + CODE_PREFIX + "Reagent,Cartridge," + CODE_PREFIX + "LOT1,,25,Unassigned"));
    }

    @Test
    public void stockOnHandPdf_returnsPdfBytes() throws Exception {
        MvcResult result = generate("STOCK_ON_HAND", "PDF");

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("application/pdf", result.getResponse().getContentType());
        byte[] body = result.getResponse().getContentAsByteArray();
        assertEquals("%PDF", new String(body, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test
    public void stockOnHandExcel_typesTheQuantityAsANumber() throws Exception {
        MvcResult result = generate("STOCK_ON_HAND", "EXCEL");

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                result.getResponse().getContentType());
        try (Workbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row itemRow = null;
            for (Row row : sheet) {
                if (row.getCell(0) != null && (CODE_PREFIX + "REAGENT").equals(row.getCell(0).getStringCellValue())) {
                    itemRow = row;
                }
            }
            assertTrue("item row present in sheet", itemRow != null);
            assertEquals(25.0, itemRow.getCell(5).getNumericCellValue(), 0.0);
            assertEquals("Cartridge", itemRow.getCell(2).getStringCellValue());
        }
    }

    // --- validation ---

    @Test
    public void generate_unknownReportType_returnsBadRequest() throws Exception {
        MvcResult result = generate("NOT_A_REPORT", "CSV");

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("reports.error.unknownReportType", errorCodeOf(result));
    }

    /** The three the recut dropped must not linger as working report types. */
    @Test
    public void generate_aRetiredReportType_isNoLongerAReportType() throws Exception {
        for (String retired : List.of("LOW_STOCK", "LOT_TRACEABILITY", "TRANSACTION_HISTORY")) {
            MvcResult result = generate(retired, "CSV", "startDate", "2020-01-01", "endDate", "2030-01-01");

            assertEquals(retired + " should no longer be served", 400, result.getResponse().getStatus());
            assertEquals(retired, "reports.error.unknownReportType", errorCodeOf(result));
        }
    }

    @Test
    public void generate_unknownExportFormat_returnsBadRequest() throws Exception {
        MvcResult result = generate("STOCK_ON_HAND", "WORD");

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("reports.error.unknownExportFormat", errorCodeOf(result));
    }

    @Test
    public void generate_invalidDateFormat_returnsBadRequest() throws Exception {
        MvcResult result = generate("CONSUMED", "CSV", "startDate", "not-a-date", "endDate", "2030-01-01");

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("reports.error.invalidDate", errorCodeOf(result));
    }

    private void setExpiry(long forLotId, LocalDate expiry) {
        jdbc.update("UPDATE clinlims.inventory_lot SET expiration_date = ? WHERE id = ?",
                Timestamp.valueOf(expiry.atStartOfDay()), forLotId);
    }

    /**
     * A received lot lands QC-PENDING, and consumption takes only PASSED stock, so
     * anything meaning to consume has to clear QC first — through the endpoint the
     * QC dialog uses, not a SQL update.
     */
    private void passQc(long forLotId) throws Exception {
        HashMap<String, Object> body = new HashMap<>();
        body.put("qcStatus", "PASSED");
        MvcResult result = mockMvc
                .perform(put("/rest/inventory/lots/" + forLotId + "/qc-status").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andReturn();
        assertEquals("test setup: QC pass failed - " + result.getResponse().getContentAsString(), 200,
                result.getResponse().getStatus());
    }

    private void consume(long forItemId, double quantity) throws Exception {
        HashMap<String, Object> body = new HashMap<>();
        body.put("itemId", String.valueOf(forItemId));
        body.put("quantity", quantity);
        MvcResult result = mockMvc
                .perform(post("/rest/inventory/management/consume").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andReturn();
        assertEquals("test setup: consume failed - " + result.getResponse().getContentAsString(), 200,
                result.getResponse().getStatus());
    }

    private String errorCodeOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("errorCode").asText(null);
    }
}
