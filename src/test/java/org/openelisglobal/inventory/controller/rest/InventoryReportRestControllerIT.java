package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
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
 * The Reports tab ({@code InventoryReports.jsx}) called
 * {@code /rest/inventory/reports/generate}, which never existed on the backend
 * — every "Generate" click 404'd. Covers the new endpoint end-to-end: one
 * report type across all 3 export formats to prove the writer pipeline works,
 * plus the validation error paths (unknown reportType/exportFormat, missing
 * date range for date-scoped report types).
 */
public class InventoryReportRestControllerIT extends BaseWebContextSensitiveTest {

    private static final String CODE_PREFIX = "RPTTEST_";

    @Autowired
    private javax.sql.DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbc;
    private MockHttpSession mockSession;

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
        assertTrue(csv.contains(CODE_PREFIX + "REAGENT"));
        assertTrue(csv.contains("25")); // total quantity across lots
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
        assertTrue(body.length > 0);
        // .xlsx is a zip archive
        assertEquals("PK", new String(body, 0, 2, java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test
    public void generate_unknownReportType_returnsBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/rest/inventory/reports/generate").session(mockSession).param("reportType", "NOT_A_REPORT")
                        .param("exportFormat", "CSV").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    public void generate_unknownExportFormat_returnsBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/rest/inventory/reports/generate").session(mockSession).param("reportType", "STOCK_LEVELS")
                        .param("exportFormat", "WORD").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    public void generate_usageTrendsWithoutDateRange_returnsBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/rest/inventory/reports/generate").session(mockSession).param("reportType", "USAGE_TRENDS")
                        .param("exportFormat", "CSV").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
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
    public void generate_invalidDateFormat_returnsBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(post("/rest/inventory/reports/generate").session(mockSession)
                .param("reportType", "USAGE_TRENDS").param("exportFormat", "CSV").param("startDate", "not-a-date")
                .param("endDate", "2030-01-01").contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();

        assertEquals(400, result.getResponse().getStatus());
    }
}
