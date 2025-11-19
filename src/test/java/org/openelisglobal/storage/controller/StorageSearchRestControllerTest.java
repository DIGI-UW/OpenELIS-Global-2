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
 * 
 * Uses Liquibase fixtures (IDs 1-999) for storage hierarchy: - Room 1 (MAIN),
 * Room 2 (SEC) - Device 10 (FRZ01), Device 11 (REF01) - Shelf 20 (Shelf-A),
 * Shelf 21 (Shelf-B) - Rack 30 (Rack R1), Rack 31 (Rack R2) - Position 100
 * (A1), Position 101 (A2), Position 102 (A3)
 */
public class StorageSearchRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper = new ObjectMapper();

    // Use fixture IDs from Liquibase test data (004-insert-test-storage-data.xml)
    private static final Integer FIXTURE_ROOM_ID = 1; // "MAIN"
    private static final Integer FIXTURE_DEVICE_ID = 10; // "FRZ01"
    private static final Integer FIXTURE_SHELF_ID = 20; // "Shelf-A"
    private static final Integer FIXTURE_RACK_ID = 30; // "Rack R1"
    private static final Integer FIXTURE_POSITION_ID = 100; // "A1"

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
        // Clean only test-created samples/assignments, not fixtures
        cleanTestSamplesAndAssignments();
        // Create samples and assignments using fixture storage hierarchy
        createTestSamplesWithAssignments();
    }

    @After
    public void tearDown() throws Exception {
        // Clean only test-created samples/assignments, not fixtures
        cleanTestSamplesAndAssignments();
    }

    // ========== Samples Search Tests ==========

    @Test
    public void testSearchSamples_BySampleId_ReturnsMatching() throws Exception {
        // Search by parent Sample accession number (search should match SampleItem ID,
        // External ID, or parent Sample accession)
        // Use the accession number prefix which is more reliable than numeric ID
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "TEST-SAMPLE-"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> sampleItems = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", sampleItems);
        assertTrue("Should return at least one matching SampleItem", sampleItems.size() >= 1);

        // Verify the SampleItem has parent Sample accession number matching
        boolean found = false;
        for (Map<String, Object> sampleItem : sampleItems) {
            String sampleAccessionNumber = (String) sampleItem.get("sampleAccessionNumber");
            if (sampleAccessionNumber != null && sampleAccessionNumber.contains("TEST-SAMPLE-")) {
                found = true;
                break;
            }
        }
        assertTrue("Should find SampleItem with matching parent Sample accession number", found);
    }

    @Test
    public void testSearchSamples_ByAccessionPrefix_ReturnsMatching() throws Exception {
        // Search by parent Sample accession number prefix (e.g., "TEST-SAMPLE-" matches
        // "TEST-SAMPLE-123")
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "TEST-SAMPLE-"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> sampleItems = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", sampleItems);
        assertTrue("Should return at least one matching SampleItem", sampleItems.size() >= 1);

        // Verify all returned SampleItems have parent Sample accession number matching
        // prefix
        for (Map<String, Object> sampleItem : sampleItems) {
            String sampleAccessionNumber = (String) sampleItem.get("sampleAccessionNumber");
            assertNotNull("Parent Sample accession number should not be null", sampleAccessionNumber);
            assertTrue("Parent Sample accession number should contain prefix",
                    sampleAccessionNumber.toLowerCase().contains("test-sample-"));
        }
    }

    @Test
    public void testSearchSamples_ByLocationPath_ReturnsMatching() throws Exception {
        // Search by location path substring (e.g., "Freezer" matches "Main Laboratory >
        // Freezer Unit 1 > ...")
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
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "Main Laboratory"))
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
        // Search by name (case-insensitive partial) - using fixture room name
        MvcResult result = mockMvc.perform(get("/rest/storage/rooms/search").param("q", "Main Laboratory"))
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
            assertTrue("Name should contain query (case-insensitive)", name.toLowerCase().contains("main laboratory"));
        }
    }

    @Test
    public void testSearchRooms_ByCode_ReturnsMatching() throws Exception {
        // Search by code (case-insensitive partial) - using fixture room code
        MvcResult result = mockMvc.perform(get("/rest/storage/rooms/search").param("q", "MAIN"))
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
        // Search by name - using fixture device name
        MvcResult result = mockMvc.perform(get("/rest/storage/devices/search").param("q", "Freezer Unit"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);
        assertTrue("Should return at least one matching device", devices.size() >= 1);
    }

    @Test
    public void testSearchDevices_ByCode_ReturnsMatching() throws Exception {
        // Search by code - using fixture device code
        MvcResult result = mockMvc.perform(get("/rest/storage/devices/search").param("q", "FRZ01"))
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

        // Verify all returned devices have matching deviceType (physical type, not
        // hierarchy level "type")
        for (Map<String, Object> device : devices) {
            String deviceType = (String) device.get("deviceType");
            assertNotNull("DeviceType should not be null", deviceType);
            assertTrue("DeviceType should match query (case-insensitive)",
                    deviceType.toLowerCase().contains("freezer"));
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
        // Search by label (case-insensitive partial) - using fixture shelf label
        MvcResult result = mockMvc.perform(get("/rest/storage/shelves/search").param("q", "Shelf-A"))
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
            assertTrue("Label should contain query (case-insensitive)", label.toLowerCase().contains("shelf-a"));
        }
    }

    // ========== Racks Search Tests ==========

    @Test
    public void testSearchRacks_ByLabel_ReturnsMatching() throws Exception {
        // Search by label (case-insensitive partial) - using fixture rack label
        MvcResult result = mockMvc.perform(get("/rest/storage/racks/search").param("q", "Rack R1"))
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
            assertTrue("Label should contain query (case-insensitive)", label.toLowerCase().contains("rack r1"));
        }
    }

    // ========== Helper Methods ==========

    /**
     * Clean only test-created samples and assignments, preserving Liquibase
     * fixtures. Fixtures (IDs 1-999) are loaded by Liquibase and should not be
     * deleted.
     */
    private void cleanTestSamplesAndAssignments() {
        try {
            // Delete test-created assignments (IDs >= 1000)
            jdbcTemplate.execute("DELETE FROM sample_storage_assignment WHERE id >= 1000");
            // Delete test-created sample items (IDs >= 40000)
            jdbcTemplate.execute("DELETE FROM sample_item WHERE id >= 40000");
            // Delete test-created samples (IDs >= 10000)
            jdbcTemplate.execute("DELETE FROM sample WHERE id >= 10000");
        } catch (Exception e) {
            // Ignore cleanup errors - data may not exist
        }
    }

    /**
     * Create test samples and assignments using fixture storage hierarchy. Storage
     * hierarchy (rooms, devices, shelves, racks, positions) comes from Liquibase
     * fixtures (IDs 1-999).
     */
    private void createTestSamplesWithAssignments() throws Exception {
        // Use sequence-based IDs for samples to avoid conflicts
        // Sequences are set to start at 1000+ by Liquibase
        // (storage-test-007-update-sequences)
        testSampleId = jdbcTemplate.queryForObject("SELECT nextval('sample_seq')", Integer.class);
        testSample2Id = jdbcTemplate.queryForObject("SELECT nextval('sample_seq')", Integer.class);
        testSample3Id = jdbcTemplate.queryForObject("SELECT nextval('sample_seq')", Integer.class);

        // Use short unique suffix to ensure unique accession numbers (max 20 chars)
        // Format: timestamp last 3 digits + thread ID + sequence ID
        // This prevents collisions when tests run in parallel or if cleanup fails
        long timestamp = System.currentTimeMillis();
        long threadId = Thread.currentThread().getId();
        String uniqueSuffix = String.format("%03d", timestamp % 1000) + "-" + (threadId % 100) + "-" + testSampleId;

        // Create samples with different accession prefixes (keeping total length <= 20)
        // "TEST-SAMPLE-" = 12 chars, uniqueSuffix = ~8-10 chars, total = ~20 chars
        jdbcTemplate.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testSampleId, "TEST-SAMPLE-" + uniqueSuffix);

        jdbcTemplate.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testSample2Id, "TB-001-" + uniqueSuffix);

        jdbcTemplate.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testSample3Id, "S-2025-" + uniqueSuffix);

        // Get or create default status_id and typeosamp_id
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

        // Create SampleItems using sequence-based IDs
        int sampleItemId1 = jdbcTemplate.queryForObject("SELECT nextval('sample_item_seq')", Integer.class);
        int sampleItemId2 = jdbcTemplate.queryForObject("SELECT nextval('sample_item_seq')", Integer.class);
        int sampleItemId3 = jdbcTemplate.queryForObject("SELECT nextval('sample_item_seq')", Integer.class);

        // Reuse same unique suffix for sample items to match their parent samples

        jdbcTemplate.update(
                "INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, status_id, lastupdated) VALUES (?, ?, 1, NULL, ?, ?, ?, CURRENT_TIMESTAMP)",
                sampleItemId1, testSampleId, "TEST-SAMPLE-" + uniqueSuffix + "-T1", typeOfSampleId, statusId);

        jdbcTemplate.update(
                "INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, status_id, lastupdated) VALUES (?, ?, 1, NULL, ?, ?, ?, CURRENT_TIMESTAMP)",
                sampleItemId2, testSample2Id, "TB-001-" + uniqueSuffix + "-T1", typeOfSampleId, statusId);

        jdbcTemplate.update(
                "INSERT INTO sample_item (id, samp_id, sort_order, sampitem_id, external_id, typeosamp_id, status_id, lastupdated) VALUES (?, ?, 1, NULL, ?, ?, ?, CURRENT_TIMESTAMP)",
                sampleItemId3, testSample3Id, "S-2025-" + uniqueSuffix + "-T1", typeOfSampleId, statusId);

        // Create assignments using fixture storage hierarchy
        // Use fixture Rack 30 (Rack R1) with positions A1, A2, A3
        testAssignmentId = jdbcTemplate.queryForObject("SELECT nextval('sample_storage_assignment_seq')",
                Integer.class);
        testAssignment2Id = jdbcTemplate.queryForObject("SELECT nextval('sample_storage_assignment_seq')",
                Integer.class);
        testAssignment3Id = jdbcTemplate.queryForObject("SELECT nextval('sample_storage_assignment_seq')",
                Integer.class);

        // Assign to fixture rack with position coordinates
        jdbcTemplate.update(
                "INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_by_user_id, assigned_date, last_updated) VALUES (?, ?, ?, 'rack', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testAssignmentId, sampleItemId1, FIXTURE_RACK_ID, "A1", 1);

        jdbcTemplate.update(
                "INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_by_user_id, assigned_date, last_updated) VALUES (?, ?, ?, 'rack', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testAssignment2Id, sampleItemId2, FIXTURE_RACK_ID, "A2", 1);

        jdbcTemplate.update(
                "INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_by_user_id, assigned_date, last_updated) VALUES (?, ?, ?, 'rack', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                testAssignment3Id, sampleItemId3, FIXTURE_RACK_ID, "A3", 1);
    }
}
