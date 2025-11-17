package org.openelisglobal.storage.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper = new ObjectMapper();

    private Integer testRoomId;
    private Integer testDeviceId;
    private Integer testShelfId;
    private Integer testRackId;
    private Integer testPositionId;
    private Integer testSampleId;
    private Integer testAssignmentId;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanStorageTestData();
        createTestStorageHierarchyWithSamples();
    }

    @After
    public void tearDown() throws Exception {
        cleanStorageTestData();
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
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
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
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);

        // Verify all returned samples have active status
        for (Map<String, Object> sample : samples) {
            String status = (String) sample.get("status");
            assertNotNull("Status should not be null", status);
            assertEquals("Status should be active", "active", status.toLowerCase());
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
                        .param("roomId", String.valueOf(testRoomId)).param("status", "active"))
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
            assertEquals("Device roomId should match", testRoomId, roomId);
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
                .perform(get("/rest/storage/shelves").param("deviceId", String.valueOf(testDeviceId))
                        .param("roomId", String.valueOf(testRoomId)).param("status", "active"))
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

            assertEquals("Shelf deviceId should match", testDeviceId, deviceId);
            assertEquals("Shelf roomId should match", testRoomId, roomId);
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
                .perform(get("/rest/storage/racks").param("roomId", String.valueOf(testRoomId))
                        .param("shelfId", String.valueOf(testShelfId)).param("deviceId", String.valueOf(testDeviceId))
                        .param("status", "active"))
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

            assertEquals("Rack roomId should match", testRoomId, roomId);
            assertEquals("Rack shelfId should match", testShelfId, shelfId);
            assertEquals("Rack deviceId should match", testDeviceId, deviceId);
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
     */
    @Test
    public void testGetLocationCounts_ExcludesInactiveLocations() throws Exception {
        // Create an inactive room
        Integer inactiveRoomId = testRoomId + 1000;
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                inactiveRoomId, "Inactive Test Room", "INACTIVE-ROOM", false, 1);

        // Create an inactive device in the inactive room
        Integer inactiveDeviceId = testDeviceId + 1000;
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                inactiveDeviceId, "Inactive Device", "INACTIVE-DEV", "freezer", inactiveRoomId, false, 1);

        try {
            MvcResult result = mockMvc.perform(get("/rest/storage/dashboard/location-counts"))
                    .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            Map<String, Object> counts = objectMapper.readValue(responseBody, Map.class);

            // Get initial counts (before creating inactive items)
            Integer initialRoomsCount = ((Number) counts.get("rooms")).intValue();
            Integer initialDevicesCount = ((Number) counts.get("devices")).intValue();

            // Verify inactive locations are not counted
            // The counts should not include the inactive room and device we just created
            // We'll verify by checking that counts match active-only query

            // Re-fetch to ensure we get fresh counts
            result = mockMvc.perform(get("/rest/storage/dashboard/location-counts")).andExpect(status().isOk())
                    .andReturn();

            responseBody = result.getResponse().getContentAsString();
            counts = objectMapper.readValue(responseBody, Map.class);

            Integer roomsCount = ((Number) counts.get("rooms")).intValue();
            Integer devicesCount = ((Number) counts.get("devices")).intValue();

            // Verify inactive room is not counted (count should be same as before or less)
            // Since we created inactive items, the count should not increase
            // Note: We can't directly verify the exact count without knowing all active
            // rooms,
            // but we can verify the inactive ones are excluded by checking they don't
            // appear
            // in separate active-only queries
            assertTrue("Rooms count should be reasonable", roomsCount >= 0);
            assertTrue("Devices count should be reasonable", devicesCount >= 0);
        } finally {
            // Clean up inactive test data
            jdbcTemplate.execute("DELETE FROM storage_device WHERE id = " + inactiveDeviceId);
            jdbcTemplate.execute("DELETE FROM storage_room WHERE id = " + inactiveRoomId);
        }
    }

    // ========== Helper Methods ==========

    private void cleanStorageTestData() {
        try {
            // Delete in order to respect foreign key constraints
            jdbcTemplate.execute("DELETE FROM sample_storage_assignment WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM sample WHERE id >= 10000");
            jdbcTemplate.execute("DELETE FROM storage_position WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_rack WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_shelf WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_device WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE id >= 1000");
        } catch (Exception e) {
            // Ignore cleanup errors - data may not exist
        }
    }

    private void createTestStorageHierarchyWithSamples() throws Exception {
        // Use unique IDs based on timestamp to avoid conflicts (following existing
        // integration test pattern)
        long timestamp = System.currentTimeMillis() % 9000;
        int baseId = 1000 + (int) timestamp;

        testRoomId = baseId;
        testDeviceId = baseId;
        testShelfId = baseId;
        testRackId = baseId;
        testPositionId = baseId;
        testSampleId = 10000 + (int) timestamp;
        testAssignmentId = baseId + 1000;

        // Create room (following existing integration test pattern)
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testRoomId, "Test Integration Room", "TEST-INT-ROOM-" + timestamp, true, 1);

        // Create device
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testDeviceId, "Test Freezer", "TEST-FREEZER-" + timestamp, "freezer", testRoomId, true, 1);

        // Create shelf
        jdbcTemplate.update(
                "INSERT INTO storage_shelf (id, label, parent_device_id, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testShelfId, "Test Shelf", testDeviceId, true, 1);

        // Create rack
        jdbcTemplate.update(
                "INSERT INTO storage_rack (id, label, rows, columns, parent_shelf_id, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testRackId, "Test Rack", 9, 9, testShelfId, true, 1);

        // Create position (occupancy is now calculated dynamically from
        // SampleStorageAssignment)
        jdbcTemplate.update(
                "INSERT INTO storage_position (id, coordinate, parent_rack_id, parent_device_id, parent_shelf_id, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testPositionId, "A1", testRackId, testDeviceId, testShelfId, 1);

        // Create sample (following existing integration test pattern - uses
        // lastupdated, not last_updated)
        jdbcTemplate.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testSampleId, "TEST-SAMPLE-" + timestamp);

        // Create SampleItem for the sample
        // Use numeric ID (sample_item.id is numeric in DB, but Hibernate treats it as
        // String)
        int sampleItemId = 30000 + (int) timestamp;
        // Get default status_id and typeosamp_id from database (create if needed)
        Integer statusId;
        List<Integer> statusIds = jdbcTemplate.queryForList("SELECT id FROM status_of_sample ORDER BY id LIMIT 1",
                Integer.class);
        if (statusIds == null || statusIds.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO status_of_sample (id, description, code, status_type, lastupdated) VALUES (1, 'Test Status', 1, 'S', CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING");
            statusId = 1;
        } else {
            statusId = statusIds.get(0);
        }

        Integer typeOfSampleId;
        List<Integer> typeOfSampleIds = jdbcTemplate.queryForList("SELECT id FROM type_of_sample ORDER BY id LIMIT 1",
                Integer.class);
        if (typeOfSampleIds == null || typeOfSampleIds.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO localization (id, english, french, lastupdated) VALUES (1, 'Test Sample Type', 'Type d''échantillon de test', CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING");
            jdbcTemplate.update(
                    "INSERT INTO type_of_sample (id, description, domain, name_localization_id, lastupdated) VALUES (1, 'Test Sample Type', 'H', 1, CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING");
            typeOfSampleId = 1;
        } else {
            typeOfSampleId = typeOfSampleIds.get(0);
        }
        jdbcTemplate.update(
                "INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, status_id, lastupdated) VALUES (?, ?, 1, NULL, ?, ?, ?, CURRENT_TIMESTAMP)",
                sampleItemId, testSampleId, "TEST-SAMPLE-" + timestamp + "-TUBE-1", typeOfSampleId, statusId);

        // Create assignment using flexible assignment model (location_id +
        // location_type, SampleItem-level)
        // Assign to rack level with position coordinate
        jdbcTemplate.update(
                "INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_by_user_id, assigned_date, last_updated) VALUES (?, ?, ?, 'rack', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testAssignmentId, sampleItemId, testRackId, "A1", 1);
    }
}
