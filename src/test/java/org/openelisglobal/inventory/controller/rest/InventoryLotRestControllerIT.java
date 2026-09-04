package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.service.SampleStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * OGC-657 regression test: the app's real Jackson {@code ObjectMapper} bean
 * (registered in AppConfig with {@code Hibernate5JakartaModule}) silently drops
 * any entity field annotated {@code @jakarta.persistence.Transient} — treating
 * the JPA annotation as an implicit {@code @JsonIgnore}. InventoryLot .location
 * was declared that way and never appeared in JSON responses even when
 * correctly populated (fixed by using the {@code transient} keyword instead).
 * This test drives the actual Spring MVC stack (real DispatcherServlet, real
 * message converters) via MockMvc, not a hand-built ObjectMapper, so it
 * exercises the exact code path that broke.
 */
public class InventoryLotRestControllerIT extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleStorageService sampleStorageService;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        executeDataSetWithStateManagement("testdata/inventory-lot-storage-test-data.xml");
        // Defensive: sample_storage_assignment/movement aren't part of the fixture
        // above (they're populated by individual test methods), so guarantee a
        // clean slate here too, not just in @After — keeps test methods
        // order-independent.
        cleanRowsInCurrentConnection(new String[] { "sample_storage_movement", "sample_storage_assignment" });
    }

    @After
    public void cleanUp() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "sample_storage_movement", "sample_storage_assignment" });
    }

    @Test
    public void getById_includesPopulatedLocationField_whenLotIsAssigned() throws Exception {
        sampleStorageService.assignInventoryLotWithLocation("7000", "7000", "room", null, "testing", "1");

        MvcResult result = mockMvc.perform(get("/rest/inventory/lots/7000")).andExpect(status().isOk()).andReturn();

        JsonNode lot = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue("Response should have a 'location' field", lot.has("location"));
        assertFalse("location should not be null", lot.get("location").isNull());
        assertEquals("OGC657 Test Room", lot.get("location").get("hierarchicalPath").asText());
    }

    @Test
    public void getById_hasNoLocationHierarchicalPath_whenLotIsUnassigned() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/inventory/lots/7000")).andExpect(status().isOk()).andReturn();

        JsonNode lot = objectMapper.readTree(result.getResponse().getContentAsString());
        // In production, Include(NON_NULL) means the field is entirely absent when
        // unassigned; this test's Spring context doesn't apply that same Jackson
        // config, so the field may come back as an explicit null instead — assert on
        // the functional behavior (no hierarchical path leaks) rather than on
        // presence/absence of the key itself.
        JsonNode location = lot.get("location");
        boolean hasPath = location != null && !location.isNull() && location.has("hierarchicalPath");
        assertFalse("Unassigned lot should have no location hierarchicalPath", hasPath);
    }

    @Test
    public void getAll_bulkAttachesLocationsWithoutNPlusOneErrors() throws Exception {
        sampleStorageService.assignInventoryLotWithLocation("7000", "7000", "room", null, null, "1");
        sampleStorageService.assignInventoryLotWithLocation("7001", "7000", "device", null, null, "1");

        MvcResult result = mockMvc.perform(get("/rest/inventory/lots")).andExpect(status().isOk()).andReturn();

        JsonNode lots = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue("Response should be an array", lots.isArray());

        boolean sawLot7000 = false;
        boolean sawLot7001 = false;
        for (JsonNode lot : lots) {
            if ("OGC657-LOT-001".equals(lot.get("lotNumber").asText())) {
                sawLot7000 = true;
                assertEquals("OGC657 Test Room", lot.get("location").get("hierarchicalPath").asText());
            }
            if ("OGC657-LOT-002".equals(lot.get("lotNumber").asText())) {
                sawLot7001 = true;
                assertEquals("OGC657 Test Room > OGC657 Test Freezer",
                        lot.get("location").get("hierarchicalPath").asText());
            }
        }
        assertTrue("Should have seen lot OGC657-LOT-001 with its location", sawLot7000);
        assertTrue("Should have seen lot OGC657-LOT-002 with its location", sawLot7001);
    }

    // ==========================================================================
    // Full REST round-trip through InventoryLotStorageRestController, driving
    // the exact same POST bodies the frontend sends.
    // ==========================================================================

    private String assignBody(String lotId, String locationId, String locationType, String positionCoordinate,
            String notes) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("inventoryLotId", lotId);
        body.put("locationId", locationId);
        body.put("locationType", locationType);
        body.put("positionCoordinate", positionCoordinate);
        body.put("notes", notes);
        return objectMapper.writeValueAsString(body);
    }

    private String moveBody(String lotId, String locationId, String locationType, String positionCoordinate,
            String reason, String notes) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("inventoryLotId", lotId);
        body.put("locationId", locationId);
        body.put("locationType", locationType);
        body.put("positionCoordinate", positionCoordinate);
        body.put("reason", reason);
        body.put("notes", notes);
        return objectMapper.writeValueAsString(body);
    }

    @Test
    public void postAssign_createsAssignmentAndIsVisibleOnGetById() throws Exception {
        mockMvc.perform(post("/rest/storage/inventory-lots/assign").contentType(MediaType.APPLICATION_JSON)
                .content(assignBody("7000", "7000", "room", null, "via REST"))).andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/rest/inventory/lots/7000")).andExpect(status().isOk()).andReturn();
        JsonNode lot = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("OGC657 Test Room", lot.get("location").get("hierarchicalPath").asText());
    }

    @Test
    public void postAssign_returnsBadRequest_whenAlreadyAssigned() throws Exception {
        mockMvc.perform(post("/rest/storage/inventory-lots/assign").contentType(MediaType.APPLICATION_JSON)
                .content(assignBody("7000", "7000", "room", null, null))).andExpect(status().isCreated());

        mockMvc.perform(post("/rest/storage/inventory-lots/assign").contentType(MediaType.APPLICATION_JSON)
                .content(assignBody("7000", "7000", "room", null, null))).andExpect(status().isBadRequest());
    }

    @Test
    public void postAssign_returnsBadRequest_whenLocationTypeMissing() throws Exception {
        mockMvc.perform(post("/rest/storage/inventory-lots/assign").contentType(MediaType.APPLICATION_JSON)
                .content(assignBody("7000", "7000", null, null, null))).andExpect(status().isBadRequest());
    }

    @Test
    public void postAssign_returnsBadRequest_whenInventoryLotIdMissing() throws Exception {
        mockMvc.perform(post("/rest/storage/inventory-lots/assign").contentType(MediaType.APPLICATION_JSON)
                .content(assignBody(null, "7000", "room", null, null))).andExpect(status().isBadRequest());
    }

    @Test
    public void postMove_updatesLocationAndAppearsInMovementsAudit() throws Exception {
        mockMvc.perform(post("/rest/storage/inventory-lots/assign").contentType(MediaType.APPLICATION_JSON)
                .content(assignBody("7000", "7000", "room", null, null))).andExpect(status().isCreated());

        mockMvc.perform(post("/rest/storage/inventory-lots/move").contentType(MediaType.APPLICATION_JSON)
                .content(moveBody("7000", "7000", "device", null, "relocate", null))).andExpect(status().isOk());

        MvcResult lotResult = mockMvc.perform(get("/rest/inventory/lots/7000")).andExpect(status().isOk()).andReturn();
        JsonNode lot = objectMapper.readTree(lotResult.getResponse().getContentAsString());
        assertEquals("OGC657 Test Room > OGC657 Test Freezer", lot.get("location").get("hierarchicalPath").asText());

        MvcResult movementsResult = mockMvc.perform(get("/rest/storage/inventory-lots/7000/movements"))
                .andExpect(status().isOk()).andReturn();
        JsonNode movements = objectMapper.readTree(movementsResult.getResponse().getContentAsString());
        assertTrue("Should be an array of movements", movements.isArray());
        assertEquals("One movement for the assign, one for the move", 2, movements.size());
    }

    @Test
    public void fullRestRoundTrip_assignMoveAndDashboardListingStayInSync() throws Exception {
        // Assign both lots (mirrors two rows on the Inventory dashboard).
        mockMvc.perform(post("/rest/storage/inventory-lots/assign").contentType(MediaType.APPLICATION_JSON)
                .content(assignBody("7000", "7000", "room", null, null))).andExpect(status().isCreated());
        mockMvc.perform(post("/rest/storage/inventory-lots/assign").contentType(MediaType.APPLICATION_JSON)
                .content(assignBody("7001", "7000", "room", null, null))).andExpect(status().isCreated());

        // Move only the first lot.
        mockMvc.perform(post("/rest/storage/inventory-lots/move").contentType(MediaType.APPLICATION_JSON)
                .content(moveBody("7000", "7000", "device", null, "moved", null))).andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/rest/inventory/lots")).andExpect(status().isOk()).andReturn();
        JsonNode lots = objectMapper.readTree(listResult.getResponse().getContentAsString());

        boolean lot1Correct = false;
        boolean lot2Correct = false;
        for (JsonNode lot : lots) {
            if ("OGC657-LOT-001".equals(lot.get("lotNumber").asText())) {
                lot1Correct = "OGC657 Test Room > OGC657 Test Freezer"
                        .equals(lot.get("location").get("hierarchicalPath").asText());
            }
            if ("OGC657-LOT-002".equals(lot.get("lotNumber").asText())) {
                lot2Correct = "OGC657 Test Room".equals(lot.get("location").get("hierarchicalPath").asText());
            }
        }
        assertTrue("Moved lot should show its new location on the dashboard listing", lot1Correct);
        assertTrue("Untouched lot should still show its original location", lot2Correct);
    }
}
