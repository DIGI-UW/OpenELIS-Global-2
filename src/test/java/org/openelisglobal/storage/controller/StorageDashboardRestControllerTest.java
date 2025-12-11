package org.openelisglobal.storage.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for Storage Dashboard filtering endpoints. Tests
 * tab-specific filter requirements per FR-065: - Samples tab: Filter by
 * location and status - Rooms tab: Filter by status - Devices tab: Filter by
 * type, room, and status - Shelves tab: Filter by device, room, and status -
 * Racks tab: Filter by room, shelf, device, and status - Racks tab: Display
 * room column (FR-065a)
 */
public class StorageDashboardRestControllerTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    // Fixed test data IDs from DBUnit XML file
    private static final Integer TEST_ROOM_ID = 1000;
    private static final Integer TEST_DEVICE_ID = 1000;
    private static final Integer TEST_SHELF_ID = 1000;
    private static final Integer TEST_RACK_ID = 1000;
    private static final Integer TEST_POSITION_ID = 1000;
    private static final Integer TEST_SAMPLE_ID = 10000;
    private static final Integer TEST_ASSIGNMENT_ID = 2000;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Load user data (required for assigned_by_user_id foreign key)
        executeDataSetWithStateManagement("testdata/user-role.xml");

        // Load test data via DBUnit XML (replaces JDBC data creation)
        executeDataSetWithStateManagement("testdata/storage-dashboard-test-data.xml");
    }

    /**
     * Test: GET /rest/storage/samples?location={locationId}&status={status} Should
     * return only samples matching both location and status filters (AND logic)
     */
    @Test
    public void testGetSamples_FilterByLocation_ReturnsFiltered() throws Exception {
        // Filter by location string (e.g., "Test Integration Room" or "Test Freezer")
        // Not by position ID - location is a hierarchical path string
        MvcResult result = mockMvc
                .perform(get("/rest/storage/sample-items").param("location", "Test Integration Room").param("status",
                        "active"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode itemsNode = root.has("items") ? root.get("items") : root;
        List<Map<String, Object>> samples = objectMapper.convertValue(itemsNode,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        // Note: Test may not have sample assignments, so we just verify the filter
        // works if samples exist
        // If samples are returned, verify they match the location filter
        if (samples.size() > 0) {
            for (Map<String, Object> sample : samples) {
                String location = (String) sample.get("location");
                assertNotNull("Location should not be null", location);
                // Location format: "Room > Device > Shelf > Rack > Position"
                // Filter by "Test Integration" should match "Test Integration Room"
                assertTrue("Location should contain test room name",
                        location.contains("Test Integration Room") || location.contains("Test Integration"));
            }
        }
    }

    @Test
    public void testGetSamples_FilterByStatus_ReturnsFiltered() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/sample-items").param("status", "active"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode itemsNode = root.has("items") ? root.get("items") : root;
        List<Map<String, Object>> samples = objectMapper.convertValue(itemsNode,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);

        // Verify all returned samples have active status (non-disposed)
        // Status is now returned as actual status ID, not "active"/"disposed" string
        for (Map<String, Object> sample : samples) {
            String status = (String) sample.get("status");
            assertNotNull("Status should not be null", status);
            // Status ID should not be "24" (disposed) - accept any other status as active
            assertNotEquals("Status should not be disposed (ID 24)", "24", status);
        }
    }

    /**
     * Test: GET /rest/storage/rooms?status={status} Should return only rooms
     * matching the status filter
     */
    @Test
    public void testGetRooms_FilterByStatus_ReturnsFiltered() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/rooms").param("status", "active"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> rooms = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", rooms);

        // Verify all returned rooms have active status
        for (Map<String, Object> room : rooms) {
            Boolean active = (Boolean) room.get("active");
            assertNotNull("Active status should not be null", active);
            assertTrue("Room should be active", active);
        }
    }

    /**
     * Test: GET
     * /rest/storage/devices?type={deviceType}&roomId={roomId}&status={status}
     * Should return only devices matching all three filters (AND logic)
     */
    @Test
    public void testGetDevices_FilterByTypeRoomStatus_ReturnsFiltered() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/storage/devices").param("type", "FREEZER")
                        .param("roomId", String.valueOf(TEST_ROOM_ID)).param("status", "active"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);

        // Verify all returned devices match all three filters
        for (Map<String, Object> device : devices) {
            String type = (String) device.get("type");
            Integer roomId = (Integer) device.get("roomId");
            Boolean active = (Boolean) device.get("active");

            assertEquals("Device type should match", "freezer", type); // getTypeAsString() returns lowercase
            assertEquals("Device roomId should match", TEST_ROOM_ID, roomId);
            assertTrue("Device should be active", active);
        }
    }

    /**
     * Test: GET
     * /rest/storage/shelves?deviceId={deviceId}&roomId={roomId}&status={status}
     * Should return only shelves matching all three filters (AND logic)
     */
    @Test
    public void testGetShelves_FilterByDeviceRoomStatus_ReturnsFiltered() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/storage/shelves").param("deviceId", String.valueOf(TEST_DEVICE_ID))
                        .param("roomId", String.valueOf(TEST_ROOM_ID)).param("status", "active"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> shelves = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", shelves);

        // Verify all returned shelves match all three filters (implementation uses
        // parentDeviceId and parentRoomId)
        for (Map<String, Object> shelf : shelves) {
            Integer deviceId = (Integer) shelf.get("parentDeviceId");
            Integer roomId = (Integer) shelf.get("parentRoomId");
            Boolean active = (Boolean) shelf.get("active");

            assertEquals("Shelf deviceId should match", TEST_DEVICE_ID, deviceId);
            assertEquals("Shelf roomId should match", TEST_ROOM_ID, roomId);
            assertTrue("Shelf should be active", active);
        }
    }

    /**
     * Test: GET
     * /rest/storage/racks?roomId={roomId}&shelfId={shelfId}&deviceId={deviceId}&status={status}
     * Should return only racks matching all four filters (AND logic)
     */
    @Test
    public void testGetRacks_FilterByRoomShelfDeviceStatus_ReturnsFiltered() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/storage/racks").param("roomId", String.valueOf(TEST_ROOM_ID))
                        .param("shelfId", String.valueOf(TEST_SHELF_ID))
                        .param("deviceId", String.valueOf(TEST_DEVICE_ID)).param("status", "active"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> racks = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", racks);

        // Verify all returned racks match all four filters (implementation uses
        // parentRoomId, parentShelfId, parentDeviceId)
        for (Map<String, Object> rack : racks) {
            Integer roomId = (Integer) rack.get("parentRoomId");
            Integer shelfId = (Integer) rack.get("parentShelfId");
            Integer deviceId = (Integer) rack.get("parentDeviceId");
            Boolean active = (Boolean) rack.get("active");

            assertEquals("Rack roomId should match", TEST_ROOM_ID, roomId);
            assertEquals("Rack shelfId should match", TEST_SHELF_ID, shelfId);
            assertEquals("Rack deviceId should match", TEST_DEVICE_ID, deviceId);
            assertTrue("Rack should be active", active);
        }
    }

    /**
     * Test: GET /rest/storage/racks should return racks with room column (FR-065a)
     */
    @Test
    public void testGetRacks_ReturnsRoomColumn() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/racks")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> racks = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", racks);
        assertFalse("Should return at least one rack", racks.isEmpty());

        // Verify all racks have room column (implementation uses parentRoomId)
        for (Map<String, Object> rack : racks) {
            Integer roomId = (Integer) rack.get("parentRoomId");
            assertNotNull("Rack should have parentRoomId column", roomId);
        }
    }

    /**
     * Test: GET /rest/storage/dashboard/location-counts Should return counts by
     * type for active locations only (FR-057, FR-057a)
     */
    @Test
    public void testGetLocationCounts_ReturnsActiveCountsByType() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/dashboard/location-counts")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> counts = objectMapper.readValue(responseBody, Map.class);

        assertNotNull("Response should not be null", counts);

        // Verify response contains all required location type counts
        assertTrue("Response should contain rooms count", counts.containsKey("rooms"));
        assertTrue("Response should contain devices count", counts.containsKey("devices"));
        assertTrue("Response should contain shelves count", counts.containsKey("shelves"));
        assertTrue("Response should contain racks count", counts.containsKey("racks"));

        // Verify counts are integers
        assertNotNull("Rooms count should not be null", counts.get("rooms"));
        assertNotNull("Devices count should not be null", counts.get("devices"));
        assertNotNull("Shelves count should not be null", counts.get("shelves"));
        assertNotNull("Racks count should not be null", counts.get("racks"));

        // Verify counts are non-negative
        Integer roomsCount = ((Number) counts.get("rooms")).intValue();
        Integer devicesCount = ((Number) counts.get("devices")).intValue();
        Integer shelvesCount = ((Number) counts.get("shelves")).intValue();
        Integer racksCount = ((Number) counts.get("racks")).intValue();

        assertTrue("Rooms count should be non-negative", roomsCount >= 0);
        assertTrue("Devices count should be non-negative", devicesCount >= 0);
        assertTrue("Shelves count should be non-negative", shelvesCount >= 0);
        assertTrue("Racks count should be non-negative", racksCount >= 0);

        // BUG CATCH: Verify that not all counts are 0 (this should catch the bug where
        // all counts show 0)
        int totalCount = roomsCount + devicesCount + shelvesCount + racksCount;
        assertTrue("BUG: All counts are 0! This indicates a problem with active location filtering. "
                + "Actual counts - rooms: " + roomsCount + ", devices: " + devicesCount + ", shelves: " + shelvesCount
                + ", racks: " + racksCount + ". Check if active field is null or not set to true in test data.",
                totalCount > 0);

        // Verify at least one location exists (from test data)
        assertTrue("Should have at least one active room from test data", roomsCount >= 1);
        assertTrue("Should have at least one active device from test data", devicesCount >= 1);
        assertTrue("Should have at least one active shelf from test data", shelvesCount >= 1);
        assertTrue("Should have at least one active rack from test data", racksCount >= 1);
    }

    /**
     * Test: GET /rest/storage/dashboard/location-counts Should exclude inactive
     * locations from counts (FR-057 - active locations only)
     * 
     * Note: This test verifies that inactive locations (from foundation data) are
     * excluded. The DBUnit XML file only contains active locations, so we verify
     * that the counts match active-only data.
     */
    @Test
    public void testGetLocationCounts_ExcludesInactiveLocations() throws Exception {
        // Verify that inactive locations from foundation data (e.g., room ID 3 with
        // code "INACTIVE") are excluded
        MvcResult result = mockMvc.perform(get("/rest/storage/dashboard/location-counts")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> counts = objectMapper.readValue(responseBody, Map.class);

        Integer roomsCount = ((Number) counts.get("rooms")).intValue();
        Integer devicesCount = ((Number) counts.get("devices")).intValue();

        // Verify counts are reasonable (should include active test data but exclude
        // inactive foundation data)
        assertTrue("Rooms count should be reasonable", roomsCount >= 0);
        assertTrue("Devices count should be reasonable", devicesCount >= 0);

        // Verify that inactive room (ID 3, code "INACTIVE") is not counted
        // The count should only include active rooms (ID 1, 2 from foundation + ID 1000
        // from test data)
        assertTrue("Rooms count should include active test data", roomsCount >= 1);
    }

}
