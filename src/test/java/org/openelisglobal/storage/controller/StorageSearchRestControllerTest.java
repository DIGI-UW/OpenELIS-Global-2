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
 * Integration tests for Storage Search REST endpoints. Tests tab-specific
 * search functionality per FR-064 and FR-064a (Phase 3.1 in plan.md): - Samples
 * tab: Search by sample ID, accession prefix, location path (OR logic) - Rooms
 * tab: Search by name and code - Devices tab: Search by name, code, and type -
 * Shelves tab: Search by label - Racks tab: Search by label
 * 
 * All searches use case-insensitive partial/substring matching.
 */
public class StorageSearchRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper = new ObjectMapper();

    private Integer testRoomId;
    private Integer testRoom2Id;
    private Integer testDeviceId;
    private Integer testDevice2Id;
    private Integer testShelfId;
    private Integer testShelf2Id;
    private Integer testRackId;
    private Integer testRack2Id;
    private Integer testPositionId;
    private Integer testSampleId;
    private Integer testSample2Id;
    private Integer testSample3Id;
    private Integer testAssignmentId;
    private Integer testAssignment2Id;
    private Integer testAssignment3Id;

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

    // ========== Samples Search Tests ==========

    @Test
    public void testSearchSamples_BySampleId_ReturnsMatching() throws Exception {
        // Search by exact sample ID
        String sampleId = String.valueOf(testSampleId);
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", sampleId))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertTrue("Should return at least one matching sample", samples.size() >= 1);

        // Verify the sample ID matches (handle both Integer and String IDs)
        boolean found = false;
        for (Map<String, Object> sample : samples) {
            Object idObj = sample.get("id");
            if (idObj != null) {
                Integer id = idObj instanceof Integer ? (Integer) idObj : Integer.parseInt(String.valueOf(idObj));
                if (id.equals(testSampleId)) {
                    found = true;
                    break;
                }
            }
        }
        assertTrue("Should find sample with matching ID", found);
    }

    @Test
    public void testSearchSamples_ByAccessionPrefix_ReturnsMatching() throws Exception {
        // Search by accession prefix (e.g., "TEST-SAMPLE-" matches "TEST-SAMPLE-123")
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "TEST-SAMPLE-"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertTrue("Should return at least one matching sample", samples.size() >= 1);

        // Verify all returned samples have accession prefix matching
        for (Map<String, Object> sample : samples) {
            String type = (String) sample.get("type");
            assertNotNull("Type (accession number) should not be null", type);
            assertTrue("Accession number should contain prefix", type.toLowerCase().contains("test-sample-"));
        }
    }

    @Test
    public void testSearchSamples_ByLocationPath_ReturnsMatching() throws Exception {
        // Search by location path substring (e.g., "Freezer" matches "Test Integration
        // Room > Test Freezer > ...")
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "Freezer"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertTrue("Should return at least one matching sample", samples.size() >= 1);

        // Verify all returned samples have location path containing "Freezer"
        for (Map<String, Object> sample : samples) {
            String location = (String) sample.get("location");
            assertNotNull("Location should not be null", location);
            assertTrue("Location should contain 'Freezer' (case-insensitive)",
                    location.toLowerCase().contains("freezer"));
        }
    }

    @Test
    public void testSearchSamples_CombinedFields_OR_Logic() throws Exception {
        // Search should match ANY of the three fields (sample ID, accession prefix,
        // location path)
        // Test with a query that matches location path but not ID or accession
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "Test Integration"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertTrue("Should return at least one matching sample via location path", samples.size() >= 1);
    }

    @Test
    public void testSearchSamples_CaseInsensitive() throws Exception {
        // "freezer" should match "Freezer Unit 1" (case-insensitive)
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "freezer"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        // Should find samples even with lowercase query
        assertTrue("Should return at least one matching sample (case-insensitive)", samples.size() >= 1);
    }

    @Test
    public void testSearchSamples_PartialMatch() throws Exception {
        // "TEST-SAMP" should match "TEST-SAMPLE-123" (partial substring)
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "TEST-SAMP"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertTrue("Should return at least one matching sample (partial match)", samples.size() >= 1);
    }

    @Test
    public void testSearchSamples_EmptyQuery_ReturnsAll() throws Exception {
        // Empty search should return all samples
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", ""))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        // Should return all samples (at least the ones we created)
        assertTrue("Should return all samples when query is empty", samples.size() >= 1);
    }

    @Test
    public void testSearchSamples_NoMatches_ReturnsEmpty() throws Exception {
        // Query that matches nothing should return empty array
        MvcResult result = mockMvc
                .perform(get("/rest/storage/samples/search").param("q", "NONEXISTENT-SAMPLE-ID-999999"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertEquals("Should return empty array when no matches", 0, samples.size());
    }

    // ========== Rooms Search Tests ==========

    @Test
    public void testSearchRooms_ByName_ReturnsMatching() throws Exception {
        // Search by name (case-insensitive partial)
        MvcResult result = mockMvc.perform(get("/rest/storage/rooms/search").param("q", "Test Integration"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> rooms = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", rooms);
        assertTrue("Should return at least one matching room", rooms.size() >= 1);

        // Verify all returned rooms have name containing query
        for (Map<String, Object> room : rooms) {
            String name = (String) room.get("name");
            assertNotNull("Name should not be null", name);
            assertTrue("Name should contain query (case-insensitive)", name.toLowerCase().contains("test integration"));
        }
    }

    @Test
    public void testSearchRooms_ByCode_ReturnsMatching() throws Exception {
        // Search by code (case-insensitive partial)
        MvcResult result = mockMvc.perform(get("/rest/storage/rooms/search").param("q", "TEST-INT"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> rooms = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", rooms);
        assertTrue("Should return at least one matching room", rooms.size() >= 1);
    }

    @Test
    public void testSearchRooms_CombinedFields_OR_Logic() throws Exception {
        // Search should match name OR code
        // Query "ROOM" should match both name and code
        MvcResult result = mockMvc.perform(get("/rest/storage/rooms/search").param("q", "ROOM"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> rooms = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", rooms);
        assertTrue("Should return at least one matching room (name or code)", rooms.size() >= 1);
    }

    // ========== Devices Search Tests ==========

    @Test
    public void testSearchDevices_ByName_ReturnsMatching() throws Exception {
        // Search by name
        MvcResult result = mockMvc.perform(get("/rest/storage/devices/search").param("q", "Test Freezer"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);
        assertTrue("Should return at least one matching device", devices.size() >= 1);
    }

    @Test
    public void testSearchDevices_ByCode_ReturnsMatching() throws Exception {
        // Search by code
        MvcResult result = mockMvc.perform(get("/rest/storage/devices/search").param("q", "TEST-FREEZER"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);
        assertTrue("Should return at least one matching device", devices.size() >= 1);
    }

    @Test
    public void testSearchDevices_ByType_ReturnsMatching() throws Exception {
        // Search by type (freezer, refrigerator, etc.)
        MvcResult result = mockMvc.perform(get("/rest/storage/devices/search").param("q", "freezer"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);
        assertTrue("Should return at least one matching device", devices.size() >= 1);

        // Verify all returned devices have matching type
        for (Map<String, Object> device : devices) {
            String type = (String) device.get("type");
            assertNotNull("Type should not be null", type);
            assertTrue("Type should match query (case-insensitive)", type.toLowerCase().contains("freezer"));
        }
    }

    @Test
    public void testSearchDevices_CombinedFields_OR_Logic() throws Exception {
        // Search should match name OR code OR type
        // Query "freezer" should match type
        MvcResult result = mockMvc.perform(get("/rest/storage/devices/search").param("q", "freezer"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);
        assertTrue("Should return at least one matching device (name or code or type)", devices.size() >= 1);
    }

    // ========== Shelves Search Tests ==========

    @Test
    public void testSearchShelves_ByLabel_ReturnsMatching() throws Exception {
        // Search by label (case-insensitive partial)
        MvcResult result = mockMvc.perform(get("/rest/storage/shelves/search").param("q", "Test Shelf"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> shelves = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", shelves);
        assertTrue("Should return at least one matching shelf", shelves.size() >= 1);

        // Verify all returned shelves have label containing query
        for (Map<String, Object> shelf : shelves) {
            String label = (String) shelf.get("label");
            assertNotNull("Label should not be null", label);
            assertTrue("Label should contain query (case-insensitive)", label.toLowerCase().contains("test shelf"));
        }
    }

    // ========== Racks Search Tests ==========

    @Test
    public void testSearchRacks_ByLabel_ReturnsMatching() throws Exception {
        // Search by label (case-insensitive partial)
        MvcResult result = mockMvc.perform(get("/rest/storage/racks/search").param("q", "Test Rack"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> racks = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", racks);
        assertTrue("Should return at least one matching rack", racks.size() >= 1);

        // Verify all returned racks have label containing query
        for (Map<String, Object> rack : racks) {
            String label = (String) rack.get("label");
            assertNotNull("Label should not be null", label);
            assertTrue("Label should contain query (case-insensitive)", label.toLowerCase().contains("test rack"));
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
        // Use unique IDs based on timestamp to avoid conflicts
        long timestamp = System.currentTimeMillis() % 9000;
        int baseId = 1000 + (int) timestamp;

        testRoomId = baseId;
        testRoom2Id = baseId + 100;
        testDeviceId = baseId;
        testDevice2Id = baseId + 100;
        testShelfId = baseId;
        testShelf2Id = baseId + 100;
        testRackId = baseId;
        testRack2Id = baseId + 100;
        testPositionId = baseId;
        testSampleId = 10000 + (int) timestamp;
        testSample2Id = 10000 + (int) timestamp + 1;
        testSample3Id = 10000 + (int) timestamp + 2;
        testAssignmentId = baseId + 1000;
        testAssignment2Id = baseId + 1001;
        testAssignment3Id = baseId + 1002;

        // Create first room
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testRoomId, "Test Integration Room", "TEST-INT-ROOM-" + timestamp, true, 1);

        // Create second room for variety
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testRoom2Id, "Secondary Test Room", "SECOND-ROOM-" + timestamp, true, 1);

        // Create first device (freezer)
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testDeviceId, "Test Freezer", "TEST-FREEZER-" + timestamp, "freezer", testRoomId, true, 1);

        // Create second device (refrigerator)
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testDevice2Id, "Test Refrigerator", "TEST-REFRIG-" + timestamp, "refrigerator", testRoomId, true, 1);

        // Create first shelf
        jdbcTemplate.update(
                "INSERT INTO storage_shelf (id, label, parent_device_id, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testShelfId, "Test Shelf", testDeviceId, true, 1);

        // Create second shelf
        jdbcTemplate.update(
                "INSERT INTO storage_shelf (id, label, parent_device_id, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testShelf2Id, "Secondary Shelf", testDevice2Id, true, 1);

        // Create first rack
        jdbcTemplate.update(
                "INSERT INTO storage_rack (id, label, rows, columns, parent_shelf_id, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testRackId, "Test Rack", 9, 9, testShelfId, true, 1);

        // Create second rack
        jdbcTemplate.update(
                "INSERT INTO storage_rack (id, label, rows, columns, parent_shelf_id, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testRack2Id, "Secondary Rack", 8, 8, testShelf2Id, true, 1);

        // Create position
        jdbcTemplate.update(
                "INSERT INTO storage_position (id, coordinate, parent_rack_id, occupied, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testPositionId, "A1", testRackId, true, 1);

        // Create samples with different accession prefixes
        jdbcTemplate.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testSampleId, "TEST-SAMPLE-" + timestamp);

        jdbcTemplate.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testSample2Id, "TB-001-" + timestamp);

        jdbcTemplate.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testSample3Id, "S-2025-" + timestamp);

        // Create assignments
        jdbcTemplate.update(
                "INSERT INTO sample_storage_assignment (id, sample_id, storage_position_id, assigned_by_user_id, assigned_date, last_updated) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testAssignmentId, testSampleId, testPositionId, 1);

        // Assign second sample to same position (will test different accession prefix)
        jdbcTemplate.update(
                "INSERT INTO sample_storage_assignment (id, sample_id, storage_position_id, assigned_by_user_id, assigned_date, last_updated) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testAssignment2Id, testSample2Id, testPositionId, 1);

        // Assign third sample to same position (will test different accession prefix)
        jdbcTemplate.update(
                "INSERT INTO sample_storage_assignment (id, sample_id, storage_position_id, assigned_by_user_id, assigned_date, last_updated) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testAssignment3Id, testSample3Id, testPositionId, 1);
    }
}
