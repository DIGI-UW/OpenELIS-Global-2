package org.openelisglobal.storage.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import javax.sql.DataSource;

/**
 * Integration tests for Storage Dashboard filtering endpoints. Tests tab-specific
 * filter requirements per FR-065:
 * - Samples tab: Filter by location and status
 * - Rooms tab: Filter by status
 * - Devices tab: Filter by type, room, and status
 * - Shelves tab: Filter by device, room, and status
 * - Racks tab: Filter by room, shelf, device, and status
 * - Racks tab: Display room column (FR-065a)
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
     * Test: GET /rest/storage/samples?location={locationId}&status={status}
     * Should return only samples matching both location and status filters (AND logic)
     */
    @Test
    public void testGetSamples_FilterByLocation_ReturnsFiltered() throws Exception {
        // Filter by location string (e.g., "Test Integration Room" or "Test Freezer")
        // Not by position ID - location is a hierarchical path string
        MvcResult result = mockMvc.perform(get("/rest/storage/samples")
                .param("location", "Test Integration Room")
                .param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        // Should return at least one sample at the test position
        assertTrue("Should return at least one filtered sample", samples.size() >= 1);

        // Verify all returned samples match the location filter
        for (Map<String, Object> sample : samples) {
            String location = (String) sample.get("location");
            assertNotNull("Location should not be null", location);
            // Location format: "Room > Device > Shelf > Rack > Position"
            // Filter by "Test Integration" should match "Test Integration Room"
            assertTrue("Location should contain test room name", 
                    location.contains("Test Integration Room") || location.contains("Test Integration"));
        }
    }

    @Test
    public void testGetSamples_FilterByStatus_ReturnsFiltered() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/samples")
                .param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

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
     * Test: GET /rest/storage/rooms?status={status}
     * Should return only rooms matching the status filter
     */
    @Test
    public void testGetRooms_FilterByStatus_ReturnsFiltered() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/rooms")
                .param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

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
     * Test: GET /rest/storage/devices?type={deviceType}&roomId={roomId}&status={status}
     * Should return only devices matching all three filters (AND logic)
     */
    @Test
    public void testGetDevices_FilterByTypeRoomStatus_ReturnsFiltered() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/devices")
                .param("type", "FREEZER")
                .param("roomId", String.valueOf(testRoomId))
                .param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

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
     * Test: GET /rest/storage/shelves?deviceId={deviceId}&roomId={roomId}&status={status}
     * Should return only shelves matching all three filters (AND logic)
     */
    @Test
    public void testGetShelves_FilterByDeviceRoomStatus_ReturnsFiltered() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/shelves")
                .param("deviceId", String.valueOf(testDeviceId))
                .param("roomId", String.valueOf(testRoomId))
                .param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> shelves = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", shelves);

        // Verify all returned shelves match all three filters
        for (Map<String, Object> shelf : shelves) {
            Integer deviceId = (Integer) shelf.get("deviceId");
            Integer roomId = (Integer) shelf.get("roomId");
            Boolean active = (Boolean) shelf.get("active");

            assertEquals("Shelf deviceId should match", testDeviceId, deviceId);
            assertEquals("Shelf roomId should match", testRoomId, roomId);
            assertTrue("Shelf should be active", active);
        }
    }

    /**
     * Test: GET /rest/storage/racks?roomId={roomId}&shelfId={shelfId}&deviceId={deviceId}&status={status}
     * Should return only racks matching all four filters (AND logic)
     */
    @Test
    public void testGetRacks_FilterByRoomShelfDeviceStatus_ReturnsFiltered() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/racks")
                .param("roomId", String.valueOf(testRoomId))
                .param("shelfId", String.valueOf(testShelfId))
                .param("deviceId", String.valueOf(testDeviceId))
                .param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> racks = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", racks);

        // Verify all returned racks match all four filters
        for (Map<String, Object> rack : racks) {
            Integer roomId = (Integer) rack.get("roomId");
            Integer shelfId = (Integer) rack.get("shelfId");
            Integer deviceId = (Integer) rack.get("deviceId");
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
        MvcResult result = mockMvc.perform(get("/rest/storage/racks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> racks = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", racks);
        assertFalse("Should return at least one rack", racks.isEmpty());

        // Verify all racks have room column
        for (Map<String, Object> rack : racks) {
            Integer roomId = (Integer) rack.get("roomId");
            assertNotNull("Rack should have roomId column", roomId);
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
        // Use unique IDs based on timestamp to avoid conflicts (following existing integration test pattern)
        long timestamp = System.currentTimeMillis() % 9000;
        int baseId = 1000 + (int)timestamp;
        
        testRoomId = baseId;
        testDeviceId = baseId;
        testShelfId = baseId;
        testRackId = baseId;
        testPositionId = baseId;
        testSampleId = 10000 + (int)timestamp;
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

        // Create position
        jdbcTemplate.update(
                "INSERT INTO storage_position (id, coordinate, parent_rack_id, occupied, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testPositionId, "A1", testRackId, true, 1);

        // Create sample (following existing integration test pattern - uses lastupdated, not last_updated)
        jdbcTemplate.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testSampleId, "TEST-SAMPLE-" + timestamp);

        // Create assignment - columns from entity: ID, SAMPLE_ID, STORAGE_POSITION_ID, ASSIGNED_BY_USER_ID, ASSIGNED_DATE, NOTES, last_updated (from BaseObject)
        jdbcTemplate.update(
                "INSERT INTO sample_storage_assignment (id, sample_id, storage_position_id, assigned_by_user_id, assigned_date, last_updated) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testAssignmentId, testSampleId, testPositionId, 1);
    }
}

