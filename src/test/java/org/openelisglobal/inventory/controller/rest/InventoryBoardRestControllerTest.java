package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The board over HTTP. The service tests prove the arithmetic; this proves the
 * response a browser actually receives — in particular that the five
 * {@code LocalDate} fields serialise as plain ISO strings rather than as nested
 * objects or epoch arrays, which is the failure mode a service-level test
 * cannot see.
 */
public class InventoryBoardRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String CODE = "BOARDTEST_CARTRIDGE";
    private static final String LOT_PREFIX = "BOARDTEST-LOT";

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private ObjectMapper objectMapper;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        objectMapper = new ObjectMapper();
        cleanup();

        // Prefix-scoped rows of our own rather than the shared fixture, so other
        // suites' inventory data cannot change what this asserts.
        jdbc.update("INSERT INTO clinlims.inventory_item (id, fhir_uuid, code, name, item_type, units,"
                + " low_stock_threshold, lead_time_days, is_active, last_updated)"
                + " VALUES (nextval('clinlims.inventory_item_seq'), gen_random_uuid(), ?,"
                + " 'Board Test Cartridge', 'CARTRIDGE', 'tests', 10, 14, 'Y', NOW())", CODE);
        Long itemId = jdbc.queryForObject("SELECT id FROM clinlims.inventory_item WHERE code = ?", Long.class, CODE);

        jdbc.update("INSERT INTO clinlims.inventory_lot (id, fhir_uuid, inventory_item_id, lot_number,"
                + " expiration_date, receipt_date, initial_quantity, current_quantity, qc_status, status, version,"
                + " last_updated) VALUES (nextval('clinlims.inventory_lot_seq'), gen_random_uuid(), ?, ?,"
                + " NOW() + INTERVAL '1 year', NOW(), 60, 60, 'PASSED', 'ACTIVE', 0, NOW())", itemId,
                LOT_PREFIX + "-1");
        Long lotId = jdbc.queryForObject("SELECT id FROM clinlims.inventory_lot WHERE lot_number = ?", Long.class,
                LOT_PREFIX + "-1");

        for (int daysAgo = 1; daysAgo <= 28; daysAgo++) {
            jdbc.update("INSERT INTO clinlims.inventory_usage (id, inventory_item_id, lot_id, quantity_used,"
                    + " usage_date, performed_by_user, last_updated)"
                    + " VALUES (nextval('clinlims.inventory_usage_seq'), ?, ?, 2, NOW() - (? * INTERVAL '1 day'),"
                    + " 1, NOW())", itemId, lotId, daysAgo);
        }
    }

    @After
    public void tearDown() {
        cleanup();
    }

    /**
     * Never TRUNCATE: inventory_item and its children are shared with other suites'
     * fixtures.
     */
    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.inventory_usage WHERE lot_id IN"
                + " (SELECT id FROM clinlims.inventory_lot WHERE lot_number LIKE ?)", LOT_PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.inventory_lot WHERE lot_number LIKE ?", LOT_PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.inventory_item WHERE code = ?", CODE);
    }

    private JsonNode boardRow() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/inventory/board")).andExpect(status().isOk()).andReturn();
        JsonNode rows = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue("the board response must be a JSON array", rows.isArray());
        for (JsonNode row : rows) {
            if (CODE.equals(row.path("code").asText())) {
                return row;
            }
        }
        throw new AssertionError("seeded item " + CODE + " missing from the board response");
    }

    @Test
    public void theEndpointIsMappedAndReturnsTheSeededItem() throws Exception {
        JsonNode row = boardRow();

        assertEquals("Board Test Cartridge", row.path("name").asText());
        assertEquals("CARTRIDGE", row.path("itemType").asText());
        assertEquals("tests", row.path("units").asText());
        assertEquals(60.0, row.path("onHand").asDouble(), 0.0001);
        assertEquals(10, row.path("lowStockThreshold").asInt());
    }

    @Test
    public void datesSerialiseAsPlainIsoStringsNotObjectsOrArrays() throws Exception {
        JsonNode row = boardRow();
        LocalDate today = LocalDate.now();

        for (String field : new String[] { "runOutEarly", "runOutLate", "orderByDate", "basisDate" }) {
            JsonNode value = row.path(field);
            assertTrue(field + " must be present", !value.isMissingNode());
            assertTrue(field + " must serialise as a string, got " + value.getNodeType() + ": " + value,
                    value.isTextual());
            // Parses, and therefore round-trips for the frontend.
            assertNotNull(field + " must be an ISO date", LocalDate.parse(value.asText()));
        }

        // 60 on hand at a steady 2 a day, with a 14-day lead time set on the item.
        assertEquals(today.plusDays(30).toString(), row.path("runOutEarly").asText());
        assertEquals(today.plusDays(30).toString(), row.path("runOutLate").asText());
        assertEquals(today.plusDays(16).toString(), row.path("orderByDate").asText());
        assertEquals(today.minusDays(1).toString(), row.path("basisDate").asText());
    }

    @Test
    public void enumsAndFlagsSerialiseAsTheirNamesSoTheFrontendCanSwitchOnThem() throws Exception {
        JsonNode row = boardRow();

        assertEquals("ADEQUATE", row.path("status").asText());
        assertEquals("SET", row.path("leadTimeTier").asText());
        assertEquals(14, row.path("leadTimeDays").asInt());
        assertTrue("stale must be a boolean, not a string", row.path("stale").isBoolean());
        assertEquals(false, row.path("stale").asBoolean());
        assertEquals(2.0, row.path("medianDailyUse").asDouble(), 0.0001);
    }

    /**
     * A date the board cannot stand behind must reach the caller as nothing at all.
     * Absent and null both read as falsy in the frontend; a string would not.
     */
    private void assertNoDate(JsonNode row, String field) {
        JsonNode value = row.path(field);
        assertTrue(field + " must carry no date without history, got " + value,
                value.isNull() || value.isMissingNode());
    }

    @Test
    public void aRowWithNoProjectionCarriesNoDatesRatherThanInventingThem() throws Exception {
        jdbc.update("DELETE FROM clinlims.inventory_usage WHERE lot_id IN"
                + " (SELECT id FROM clinlims.inventory_lot WHERE lot_number LIKE ?)", LOT_PREFIX + "%");

        JsonNode row = boardRow();

        assertEquals("BUILDING_DATA", row.path("status").asText());
        assertNoDate(row, "runOutEarly");
        assertNoDate(row, "runOutLate");
        assertNoDate(row, "orderByDate");
        assertNoDate(row, "basisDate");
        assertTrue("an item with no usage at all is stale by definition", row.path("stale").asBoolean());
    }
}
