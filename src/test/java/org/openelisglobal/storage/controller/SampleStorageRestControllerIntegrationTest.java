package org.openelisglobal.storage.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Integration test for SampleStorageRestController.getSamples() endpoint.
 * 
 * CRITICAL: This test verifies that the API endpoint returns samples with
 * complete hierarchical paths WITHOUT lazy loading exceptions. This catches the
 * issue where controllers access entity relationships outside transaction
 * boundaries.
 * 
 * This test should FAIL if: 1. Service layer doesn't eagerly fetch all
 * relationships 2. Controller tries to access relationships after transaction
 * closes 3. LazyInitializationException occurs during JSON serialization
 * 
 * Following OpenELIS test patterns: extends BaseWebContextSensitiveTest to load
 * full Spring context and hit real database with proper transaction management.
 */
public class SampleStorageRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @After
    public void tearDown() throws Exception {
        // Clean up any test data created during this test
        cleanStorageTestData();
    }

    /**
     * Clean up storage-related test data to ensure tests don't pollute the
     * database. Preserves fixture data (IDs 1-999) but deletes test-created data.
     */
    private void cleanStorageTestData() {
        try {
            // Delete in correct order to respect foreign key constraints
            jdbcTemplate.execute("DELETE FROM sample_storage_assignment WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM sample_storage_movement WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_position WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_rack WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_shelf WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_device WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM sample WHERE id >= 10000");
        } catch (Exception e) {
            // Ignore cleanup errors - data may not exist
        }
    }

    /**
     * Create a complete storage hierarchy with sample assignments for testing. This
     * creates: Room -> Device -> Shelf -> Rack -> Position -> Sample Assignment
     * Uses JDBC directly to avoid REST API validation issues in tests.
     */
    private void createTestStorageHierarchyWithSamples() throws Exception {
        // Use unique IDs based on timestamp to avoid conflicts
        long timestamp = System.currentTimeMillis() % 9000; // Last 4 digits
        int baseId = 1000 + (int) timestamp;

        // Create room directly via JDBC to avoid validation issues
        // Note: Using actual column names from schema (last_updated not lastupdated)
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                baseId, "Test Integration Room", "TEST-INT-ROOM-" + timestamp, true, 1);
        Integer roomId = baseId;

        // Create device directly via JDBC
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                baseId, "Test Freezer", "TEST-FREEZER-" + timestamp, "freezer", roomId, true, 1);
        Integer deviceId = baseId;

        // Create shelf directly via JDBC
        jdbcTemplate.update(
                "INSERT INTO storage_shelf (id, label, parent_device_id, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                baseId, "Test Shelf", deviceId, true, 1);
        Integer shelfId = baseId;

        // Create rack directly via JDBC
        jdbcTemplate.update(
                "INSERT INTO storage_rack (id, label, rows, columns, parent_shelf_id, active, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                baseId, "Test Rack", 4, 6, shelfId, true, 1);
        Integer rackId = baseId;

        // Create position directly via JDBC
        jdbcTemplate.update(
                "INSERT INTO storage_position (id, coordinate, parent_rack_id, occupied, sys_user_id, last_updated, fhir_uuid) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                baseId, "A1", rackId, false, 1);
        Integer positionId = baseId;

        // Create sample with unique ID
        int sampleId = 10000 + (int) timestamp;
        jdbcTemplate.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                sampleId, "TEST-SAMPLE-" + timestamp);

        // Assign sample to position
        String assignmentResponse = mockMvc
                .perform(post("/rest/storage/samples/assign").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"sampleId\":\"%d\",\"positionId\":\"%d\",\"notes\":\"Integration test assignment\"}",
                                sampleId, positionId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        assertNotNull("Assignment should succeed", assignmentResponse);
    }

    /**
     * CRITICAL TEST: Verify GET /rest/storage/samples returns samples with complete
     * hierarchical paths WITHOUT lazy loading exceptions.
     * 
     * This test will FAIL if: - Service layer doesn't eagerly fetch all
     * relationships - Controller accesses relationships after transaction closes -
     * LazyInitializationException occurs during JSON serialization
     */
    @Test
    public void testGetSamples_ReturnsCompleteData_NoLazyInitializationException() throws Exception {
        // Setup: Create test data with full hierarchy
        cleanStorageTestData();
        createTestStorageHierarchyWithSamples();
        // When: Call GET /rest/storage/samples
        MvcResult result = mockMvc.perform(get("/rest/storage/samples")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Then: Response should be valid JSON array
        String responseContent = result.getResponse().getContentAsString();
        assertNotNull("Response should not be null", responseContent);
        assertFalse("Response should not be empty", responseContent.trim().isEmpty());

        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());

        // Verify at least one sample is returned
        assertTrue("Response should contain at least one sample", responseJson.size() > 0);

        // Verify each sample has required fields
        JsonNode firstSample = responseJson.get(0);
        assertNotNull("First sample should not be null", firstSample);
        assertTrue("Sample should have 'id' field", firstSample.has("id"));
        assertTrue("Sample should have 'sampleId' field", firstSample.has("sampleId"));
        assertTrue("Sample should have 'location' field", firstSample.has("location"));

        // CRITICAL: Verify hierarchical path is complete (contains ">")
        String location = firstSample.get("location").asText();
        assertNotNull("Location should not be null", location);
        assertFalse("Location should not be empty", location.trim().isEmpty());
        assertTrue("Location should contain hierarchical separator '>'", location.contains(">"));

        // Verify location contains all hierarchy levels (Room > Device > Shelf > Rack >
        // Position)
        // This ensures the full hierarchy was loaded and path was built correctly
        assertTrue("Location should contain room name",
                location.contains("Test Integration Room") || location.contains("Room"));
        assertTrue("Location should contain device name",
                location.contains("Test Freezer") || location.contains("Freezer"));
        assertTrue("Location should contain shelf label",
                location.contains("Test Shelf") || location.contains("Shelf"));
        assertTrue("Location should contain rack label", location.contains("Test Rack") || location.contains("Rack"));
        assertTrue("Location should contain position coordinate", location.contains("A1"));

        // Verify no exceptions occurred (status is 200, not 500)
        assertEquals("Response status should be 200", 200, result.getResponse().getStatus());
    }

    /**
     * Verify GET /rest/storage/samples returns correct data structure for all
     * samples.
     */
    @Test
    public void testGetSamples_ReturnsCorrectDataStructure() throws Exception {
        // Setup: Create test data with full hierarchy
        cleanStorageTestData();
        createTestStorageHierarchyWithSamples();
        // When: Call GET /rest/storage/samples
        MvcResult result = mockMvc.perform(get("/rest/storage/samples")).andExpect(status().isOk()).andReturn();

        // Then: Parse and verify response structure
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        // Verify all samples have required fields
        for (JsonNode sample : responseJson) {
            assertTrue("Sample should have 'id' field", sample.has("id"));
            assertTrue("Sample should have 'sampleId' field", sample.has("sampleId"));
            assertTrue("Sample should have 'type' field", sample.has("type"));
            assertTrue("Sample should have 'status' field", sample.has("status"));
            assertTrue("Sample should have 'location' field", sample.has("location"));
            assertTrue("Sample should have 'assignedBy' field", sample.has("assignedBy"));
            assertTrue("Sample should have 'date' field", sample.has("date"));

            // Verify location is not null or empty
            String location = sample.get("location").asText();
            assertNotNull("Location should not be null", location);
            assertFalse("Location should not be empty", location.trim().isEmpty());
        }
    }

    /**
     * Verify GET /rest/storage/samples?countOnly=true returns metrics correctly.
     */
    @Test
    public void testGetSamples_CountOnly_ReturnsMetrics() throws Exception {
        // Setup: Create test data with full hierarchy
        cleanStorageTestData();
        createTestStorageHierarchyWithSamples();
        // When: Call GET /rest/storage/samples?countOnly=true
        MvcResult result = mockMvc.perform(get("/rest/storage/samples?countOnly=true")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Then: Response should contain metrics
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());
        assertTrue("Response should contain metrics", responseJson.size() > 0);

        JsonNode metrics = responseJson.get(0);
        assertTrue("Metrics should have 'totalSamples' field", metrics.has("totalSamples"));
        assertTrue("Metrics should have 'active' field", metrics.has("active"));
        assertTrue("Metrics should have 'disposed' field", metrics.has("disposed"));
        assertTrue("Metrics should have 'storageLocations' field", metrics.has("storageLocations"));

        // Verify counts are non-negative
        assertTrue("totalSamples should be >= 0", metrics.get("totalSamples").asInt() >= 0);
    }
}
