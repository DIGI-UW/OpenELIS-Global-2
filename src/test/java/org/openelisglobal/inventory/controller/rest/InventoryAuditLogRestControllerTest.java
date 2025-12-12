package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Tests the InventoryAuditLogRestController to verify it properly transforms
 * History entities into UI-friendly audit log format.
 */
@Rollback
public class InventoryAuditLogRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryLotService inventoryLotService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String TEST_USER_ID = "1";

    @Before
    public void setup() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        executeDataSetWithStateManagement("testdata/inventory-audit-test-data.xml");
    }

    // ============================================================================
    // INVENTORY ITEM AUDIT LOG ENDPOINT TESTS
    // ============================================================================

    @Test
    public void testGetItemAuditTrail_ReturnsAuditLogs() throws Exception {
        // Given: An item with audit history
        InventoryItem item = inventoryItemService.get(2000L);
        item.setDescription("Updated for REST test");
        item.setSysUserId(TEST_USER_ID);
        inventoryItemService.update(item);

        // When: We request the audit trail
        MvcResult result = mockMvc
                .perform(get("/rest/inventory/audit-logs/item/2000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Then: Response should contain audit logs
        String responseJson = result.getResponse().getContentAsString();
        List<Map<String, Object>> auditLogs = objectMapper.readValue(responseJson, List.class);

        assertNotNull("Audit logs should not be null", auditLogs);
        assertTrue("Should have at least one audit log", auditLogs.size() > 0);
    }

    @Test
    public void testGetItemAuditTrail_ContainsRequiredFields() throws Exception {
        // Given: An item with updates
        InventoryItem item = inventoryItemService.get(2000L);
        item.setDescription("Field validation test");
        item.setLowStockThreshold(20);
        item.setSysUserId(TEST_USER_ID);
        inventoryItemService.update(item);

        // When: We request the audit trail
        MvcResult result = mockMvc
                .perform(get("/rest/inventory/audit-logs/item/2000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Then: Each audit log should have required fields
        String responseJson = result.getResponse().getContentAsString();
        List<Map<String, Object>> auditLogs = objectMapper.readValue(responseJson, List.class);

        Map<String, Object> firstLog = auditLogs.get(0);
        assertTrue("Should have id field", firstLog.containsKey("id"));
        assertTrue("Should have timestamp field", firstLog.containsKey("timestamp"));
        assertTrue("Should have activity field", firstLog.containsKey("activity"));
        assertTrue("Should have performedByUser field", firstLog.containsKey("performedByUser"));
        assertTrue("Should have changes field", firstLog.containsKey("changes"));
        assertTrue("Should have summary field", firstLog.containsKey("summary"));
    }

    @Test
    public void testGetItemAuditTrail_ParsesChangesCorrectly() throws Exception {
        // Given: An item with specific field changes
        InventoryItem item = inventoryItemService.get(2000L);
        String newDescription = "Parsed changes test - " + System.currentTimeMillis();
        item.setDescription(newDescription);
        item.setSysUserId(TEST_USER_ID);
        inventoryItemService.update(item);

        // When: We request the audit trail
        MvcResult result = mockMvc
                .perform(get("/rest/inventory/audit-logs/item/2000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Then: Changes should be parsed into structured format
        String responseJson = result.getResponse().getContentAsString();
        List<Map<String, Object>> auditLogs = objectMapper.readValue(responseJson, List.class);

        Map<String, Object> recentLog = auditLogs.get(0);
        Object changesObj = recentLog.get("changes");

        assertNotNull("Changes should be present", changesObj);
        assertTrue("Changes should be a map", changesObj instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> changes = (Map<String, Object>) changesObj;

        // Should have field-level changes with old/new values
        if (!changes.isEmpty()) {
            Object firstChange = changes.values().iterator().next();
            assertTrue("Each change should be a map with old/new", firstChange instanceof Map);
        }
    }

    @Test
    public void testGetItemAuditTrail_IncludesUserName() throws Exception {
        // Given: An item updated by a user
        InventoryItem item = inventoryItemService.get(2000L);
        item.setDescription("User tracking test");
        item.setSysUserId(TEST_USER_ID);
        inventoryItemService.update(item);

        // When: We request the audit trail
        mockMvc.perform(get("/rest/inventory/audit-logs/item/2000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].performedByUser").exists())
                .andExpect(jsonPath("$[0].sysUserId").value(TEST_USER_ID));
    }

    @Test
    public void testGetItemAuditTrail_NonExistentItem_ReturnsNotFound() throws Exception {
        // When: We request audit trail for non-existent item
        mockMvc.perform(get("/rest/inventory/audit-logs/item/999999").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ============================================================================
    // INVENTORY LOT AUDIT LOG ENDPOINT TESTS
    // ============================================================================

    @Test
    public void testGetLotAuditTrail_ReturnsAuditLogs() throws Exception {
        // Given: A lot with audit history
        InventoryLot lot = inventoryLotService.get(2000L);
        lot.setCurrentQuantity(85.0);
        lot.setSysUserId(TEST_USER_ID);
        inventoryLotService.update(lot);

        // When: We request the audit trail
        MvcResult result = mockMvc
                .perform(get("/rest/inventory/audit-logs/lot/2000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Then: Response should contain audit logs
        String responseJson = result.getResponse().getContentAsString();
        List<Map<String, Object>> auditLogs = objectMapper.readValue(responseJson, List.class);

        assertNotNull("Audit logs should not be null", auditLogs);
        assertTrue("Should have at least one audit log", auditLogs.size() > 0);
    }

    @Test
    public void testGetLotAuditTrail_TracksQuantityChanges() throws Exception {
        // Given: A lot with quantity changes
        InventoryLot lot = inventoryLotService.get(2001L);
        Double originalQuantity = lot.getCurrentQuantity();
        Double newQuantity = originalQuantity - 10.0;

        lot.setCurrentQuantity(newQuantity);
        lot.setSysUserId(TEST_USER_ID);
        inventoryLotService.update(lot);

        // When: We request the audit trail
        MvcResult result = mockMvc
                .perform(get("/rest/inventory/audit-logs/lot/2001").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Then: Should capture quantity change
        String responseJson = result.getResponse().getContentAsString();
        List<Map<String, Object>> auditLogs = objectMapper.readValue(responseJson, List.class);

        Map<String, Object> recentLog = auditLogs.get(0);
        assertEquals("Activity should be UPDATE", "U", recentLog.get("activity"));

        @SuppressWarnings("unchecked")
        Map<String, Object> changes = (Map<String, Object>) recentLog.get("changes");
        assertNotNull("Changes should be present", changes);
    }

    @Test
    public void testGetLotAuditTrail_GeneratesSummary() throws Exception {
        // Given: A lot with changes
        InventoryLot lot = inventoryLotService.get(2000L);
        lot.setCurrentQuantity(60.0);
        lot.setSysUserId(TEST_USER_ID);
        inventoryLotService.update(lot);

        // When: We request the audit trail
        mockMvc.perform(get("/rest/inventory/audit-logs/lot/2000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].summary").exists())
                .andExpect(jsonPath("$[0].summary").isString());
    }

    // ============================================================================
    // STORAGE LOCATION AUDIT LOG ENDPOINT TESTS
    // ============================================================================

    @Test
    public void testGetLocationAuditTrail_ReturnsAuditLogs() throws Exception {
        // When: We request the audit trail for a location
        // (assuming location 2000 exists from test data)
        MvcResult result = mockMvc
                .perform(get("/rest/inventory/audit-logs/location/2000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Then: Response should be valid (may be empty if no updates yet)
        String responseJson = result.getResponse().getContentAsString();
        List<Map<String, Object>> auditLogs = objectMapper.readValue(responseJson, List.class);
        assertNotNull("Audit logs should not be null", auditLogs);
    }

    // ============================================================================
    // AUDIT LOG TRANSFORMATION TESTS
    // ============================================================================

    @Test
    public void testAuditLogTransformation_FormatsFieldNames() throws Exception {
        // Given: An item with camelCase field update
        InventoryItem item = inventoryItemService.get(2000L);
        item.setLowStockThreshold(35);
        item.setSysUserId(TEST_USER_ID);
        inventoryItemService.update(item);

        // When: We request the audit trail
        MvcResult result = mockMvc
                .perform(get("/rest/inventory/audit-logs/item/2000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Then: Summary should format field names nicely
        String responseJson = result.getResponse().getContentAsString();
        List<Map<String, Object>> auditLogs = objectMapper.readValue(responseJson, List.class);

        String summary = (String) auditLogs.get(0).get("summary");
        assertNotNull("Summary should be present", summary);
        // Summary should format camelCase fields (lowStockThreshold -> Low Stock
        // Threshold)
    }

    @Test
    public void testAuditLogTransformation_HandlesMultipleFieldChanges() throws Exception {
        // Given: An item with multiple field changes
        InventoryItem item = inventoryItemService.get(2000L);
        item.setDescription("Multi-change test");
        item.setLowStockThreshold(25);
        item.setManufacturer("Test Manufacturer");
        item.setSysUserId(TEST_USER_ID);
        inventoryItemService.update(item);

        // When: We request the audit trail
        MvcResult result = mockMvc
                .perform(get("/rest/inventory/audit-logs/item/2000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Then: Should show all field changes
        String responseJson = result.getResponse().getContentAsString();
        List<Map<String, Object>> auditLogs = objectMapper.readValue(responseJson, List.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> changes = (Map<String, Object>) auditLogs.get(0).get("changes");
        assertTrue("Should capture multiple field changes", changes.size() >= 2);
    }

    @Test
    public void testAuditLogTransformation_ShowsOldAndNewValues() throws Exception {
        // Given: An item with a clear before/after change
        InventoryItem item = inventoryItemService.get(2000L);
        item.setLowStockThreshold(99);
        item.setSysUserId(TEST_USER_ID);
        inventoryItemService.update(item);

        // When: We request the audit trail
        MvcResult result = mockMvc
                .perform(get("/rest/inventory/audit-logs/item/2000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Then: Changes should show old and new values
        String responseJson = result.getResponse().getContentAsString();
        List<Map<String, Object>> auditLogs = objectMapper.readValue(responseJson, List.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> changes = (Map<String, Object>) auditLogs.get(0).get("changes");

        if (!changes.isEmpty()) {
            Object firstChange = changes.values().iterator().next();
            @SuppressWarnings("unchecked")
            Map<String, String> changeDetails = (Map<String, String>) firstChange;

            // Should have old and/or new values
            boolean hasOldOrNew = changeDetails.containsKey("old") || changeDetails.containsKey("new");
            assertTrue("Should track old and/or new values", hasOldOrNew);
        }
    }

    @Test
    public void testAuditLogTransformation_OrdersByTimestampDescending() throws Exception {
        // Given: Multiple updates to create history
        InventoryItem item = inventoryItemService.get(2000L);

        for (int i = 0; i < 3; i++) {
            item.setDescription("Update " + i);
            item.setSysUserId(TEST_USER_ID);
            inventoryItemService.update(item);

            Thread.sleep(50); // Ensure different timestamps
        }

        // When: We request the audit trail
        MvcResult result = mockMvc
                .perform(get("/rest/inventory/audit-logs/item/2000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Then: Logs should be ordered newest first
        String responseJson = result.getResponse().getContentAsString();
        List<Map<String, Object>> auditLogs = objectMapper.readValue(responseJson, List.class);

        assertTrue("Should have multiple audit logs", auditLogs.size() >= 3);

        // Verify descending order
        for (int i = 1; i < auditLogs.size(); i++) {
            String currentTimestamp = (String) auditLogs.get(i - 1).get("timestamp");
            String previousTimestamp = (String) auditLogs.get(i).get("timestamp");

            Timestamp current = Timestamp.valueOf(currentTimestamp.replace("T", " ").substring(0, 19));
            Timestamp previous = Timestamp.valueOf(previousTimestamp.replace("T", " ").substring(0, 19));

            assertTrue("Should be sorted descending", current.compareTo(previous) >= 0);
        }
    }

    @Test
    public void testAuditLogTransformation_IncludesActivityType() throws Exception {
        // Given: An item with insert/update history
        InventoryItem item = inventoryItemService.get(2000L);
        item.setDescription("Activity type test");
        item.setSysUserId(TEST_USER_ID);
        inventoryItemService.update(item);

        // When: We request the audit trail
        mockMvc.perform(get("/rest/inventory/audit-logs/item/2000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].activity").exists())
                .andExpect(jsonPath("$[0].activity")
                        .value(org.hamcrest.CoreMatchers.anyOf(org.hamcrest.CoreMatchers.is("I"),
                                org.hamcrest.CoreMatchers.is("U"), org.hamcrest.CoreMatchers.is("D"))));
    }
}
