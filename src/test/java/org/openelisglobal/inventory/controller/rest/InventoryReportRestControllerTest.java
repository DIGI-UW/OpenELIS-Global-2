package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
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
 * Covers {@code /rest/inventory/reports/generate} end to end: one report type
 * across all 3 export formats, the date bounds, and the validation error paths.
 */
public class InventoryReportRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String CODE_PREFIX = "RPTTEST-";

    @Autowired
    private javax.sql.DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbc;
    private MockHttpSession mockSession;
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
        jdbc.update("DELETE FROM clinlims.inventory_transaction WHERE lot_id IN "
                + "(SELECT l.id FROM clinlims.inventory_lot l JOIN clinlims.inventory_item i "
                + "ON i.id = l.inventory_item_id WHERE i.code LIKE ?)", CODE_PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.inventory_lot WHERE inventory_item_id IN "
                + "(SELECT id FROM clinlims.inventory_item WHERE code LIKE ?)", CODE_PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.inventory_item WHERE code LIKE ?", CODE_PREFIX + "%");
    }

    private void createItemAndLot() throws Exception {
        HashMap<String, Object> item = new HashMap<>();
        item.put("code", CODE_PREFIX + "REAGENT");
        item.put("name", CODE_PREFIX + "Reagent");
        item.put("itemType", "REAGENT");
        item.put("units", "mL");
        MvcResult itemResult = mockMvc.perform(post("/rest/inventory/items").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(item))).andReturn();
        assertEquals("test setup: item creation failed - " + itemResult.getResponse().getContentAsString(), 201,
                itemResult.getResponse().getStatus());
        long itemId = objectMapper.readTree(itemResult.getResponse().getContentAsString()).get("id").asLong();

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

    private void recordTransactionAt(Timestamp transactionDate) {
        jdbc.update(
                "INSERT INTO clinlims.inventory_transaction (id, lot_id, transaction_type, quantity_change,"
                        + " quantity_after, transaction_date, performed_by_user)"
                        + " VALUES (nextval('clinlims.inventory_transaction_seq'), ?, 'CONSUMPTION', -5, 20, ?, 1)",
                lotId, transactionDate);
    }

    @Test
    public void generate_stockLevelsCsv_returnsCsvWithItemData() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/rest/inventory/reports/generate").session(mockSession).param("reportType", "STOCK_LEVELS")
                        .param("exportFormat", "CSV").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentType().startsWith("text/csv"));
        String csv = result.getResponse().getContentAsString();
        // A freshly received lot is QC-PENDING and still counts as available.
        assertTrue(csv,
                csv.contains(CODE_PREFIX + "REAGENT," + CODE_PREFIX + "Reagent,REAGENT,,Unassigned,25,25,mL,Active"));
    }

    @Test
    public void generate_stockLevelsPdf_returnsPdfBytes() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/rest/inventory/reports/generate").session(mockSession).param("reportType", "STOCK_LEVELS")
                        .param("exportFormat", "PDF").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("application/pdf", result.getResponse().getContentType());
        byte[] body = result.getResponse().getContentAsByteArray();
        assertTrue(body.length > 0);
        assertEquals("%PDF", new String(body, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test
    public void generate_stockLevelsExcel_returnsXlsxBytes() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/rest/inventory/reports/generate").session(mockSession).param("reportType", "STOCK_LEVELS")
                        .param("exportFormat", "EXCEL").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                result.getResponse().getContentType());
        byte[] body = result.getResponse().getContentAsByteArray();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row itemRow = null;
            for (Row row : sheet) {
                if (row.getCell(0) != null && (CODE_PREFIX + "REAGENT").equals(row.getCell(0).getStringCellValue())) {
                    itemRow = row;
                }
            }
            assertTrue("item row present in sheet", itemRow != null);
            assertEquals(25.0, itemRow.getCell(5).getNumericCellValue(), 0.0);
            assertEquals(25.0, itemRow.getCell(6).getNumericCellValue(), 0.0);
            assertEquals("mL", itemRow.getCell(7).getStringCellValue());
        }
    }

    @Test
    public void generate_unknownReportType_returnsBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/rest/inventory/reports/generate").session(mockSession).param("reportType", "NOT_A_REPORT")
                        .param("exportFormat", "CSV").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("reports.error.unknownReportType", errorCodeOf(result));
    }

    @Test
    public void generate_unknownExportFormat_returnsBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/rest/inventory/reports/generate").session(mockSession).param("reportType", "STOCK_LEVELS")
                        .param("exportFormat", "WORD").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("reports.error.unknownExportFormat", errorCodeOf(result));
    }

    @Test
    public void generate_usageTrendsWithoutDateRange_returnsBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/rest/inventory/reports/generate").session(mockSession).param("reportType", "USAGE_TRENDS")
                        .param("exportFormat", "CSV").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("reports.error.dateRangeRequired", errorCodeOf(result));
    }

    @Test
    public void generate_transactionHistoryWithDateRange_returnsOk() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/rest/inventory/reports/generate").session(mockSession).param("reportType", "TRANSACTION_HISTORY")
                        .param("exportFormat", "CSV").param("startDate", "2020-01-01").param("endDate", "2030-01-01")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void generate_transactionHistorySameDayRange_includesTodaysLaterTransaction() throws Exception {
        LocalDate today = LocalDate.now();
        recordTransactionAt(Timestamp.valueOf(today.atTime(17, 45)));

        MvcResult result = mockMvc.perform(
                post("/rest/inventory/reports/generate").session(mockSession).param("reportType", "TRANSACTION_HISTORY")
                        .param("exportFormat", "CSV").param("startDate", today.toString())
                        .param("endDate", today.toString()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String csv = result.getResponse().getContentAsString();
        assertTrue("end date must cover the whole day, not stop at midnight: " + csv,
                csv.contains(CODE_PREFIX + "REAGENT"));
    }

    @Test
    public void generate_transactionHistory_excludesTheDayAfterTheEndDate() throws Exception {
        LocalDate today = LocalDate.now();
        recordTransactionAt(Timestamp.valueOf(today.plusDays(1).atStartOfDay()));

        MvcResult result = mockMvc.perform(
                post("/rest/inventory/reports/generate").session(mockSession).param("reportType", "TRANSACTION_HISTORY")
                        .param("exportFormat", "CSV").param("startDate", today.toString())
                        .param("endDate", today.toString()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String csv = result.getResponse().getContentAsString();
        assertFalse("next-day midnight is outside the range: " + csv, csv.contains(CODE_PREFIX + "REAGENT"));
    }

    @Test
    public void generate_invalidDateFormat_returnsBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(post("/rest/inventory/reports/generate").session(mockSession)
                .param("reportType", "USAGE_TRENDS").param("exportFormat", "CSV").param("startDate", "not-a-date")
                .param("endDate", "2030-01-01").contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertEquals("reports.error.invalidDate", errorCodeOf(result));
    }

    private String errorCodeOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("errorCode").asText(null);
    }
}
