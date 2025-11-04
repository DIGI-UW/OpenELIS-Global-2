package org.openelisglobal.storage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.form.SampleAssignmentForm;
import org.openelisglobal.storage.service.SampleStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for SampleStorageRestController - Sample Assignment
 * Following TDD: Write tests BEFORE implementation Tests based on
 * contracts/storage-api.json
 */
public class SampleStorageRestControllerTest extends BaseWebContextSensitiveTest {

    private static final Logger logger = LoggerFactory.getLogger(SampleStorageRestControllerTest.class);

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        // Clean up test-created data before each test
        cleanStorageTestData();
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
            // Delete test-created data (IDs >= 1000 or codes/names starting with TEST-)
            // Preserves fixture data (IDs 1-999)
            jdbcTemplate.execute("DELETE FROM sample_storage_movement WHERE id::integer >= 1000 OR id LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM sample_storage_assignment WHERE id::integer >= 1000 OR id LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_position WHERE id::integer >= 1000 OR coordinate LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_rack WHERE id::integer >= 1000 OR label LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_shelf WHERE id::integer >= 1000 OR label LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_device WHERE id::integer >= 1000 OR code LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE id::integer >= 1000 OR code LIKE 'TEST-%'");
        } catch (Exception e) {
            logger.warn("Failed to clean storage test data: " + e.getMessage());
        }
    }

    /**
     * T042: Test assigning sample to valid position returns HTTP 201 Contract: POST
     * /rest/storage/samples/assign → 201 + assignment details
     */
    @Test
    public void testAssignSample_ValidInput_Returns201() throws Exception {
        // Given: Create a sample dynamically to ensure it exists
        String sampleId = createSampleAndGetId();

        // Create a position in the test hierarchy
        String roomId = createRoomAndGetId("Test Room", "TEST-ROOM-" + System.currentTimeMillis());
        String deviceId = createDeviceAndGetId("Test Device", "TEST-DEV-" + System.currentTimeMillis(), "freezer",
                roomId);
        String shelfId = createShelfAndGetId("Shelf-1", deviceId);
        String rackId = createRackAndGetId("Rack-1", 8, 12, shelfId);

        // Create an unoccupied position
        org.openelisglobal.storage.form.StoragePositionForm positionForm = new org.openelisglobal.storage.form.StoragePositionForm();
        positionForm.setCoordinate("A1");
        positionForm.setParentRackId(rackId);
        positionForm.setOccupied(false);

        String positionResponse = mockMvc
                .perform(post("/rest/storage/positions").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(positionForm)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String positionId = objectMapper.readTree(positionResponse).get("id").asInt() + "";

        // Given: Valid assignment form
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleId(sampleId);
        form.setPositionId(positionId);
        form.setNotes("Initial storage assignment");

        String requestBody = objectMapper.writeValueAsString(form);

        // When: POST to /rest/storage/samples/assign
        // Then: Expect 201 Created with hierarchical path
        mockMvc.perform(
                post("/rest/storage/samples/assign").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.assignmentId").exists())
                .andExpect(jsonPath("$.hierarchicalPath").exists()).andExpect(jsonPath("$.assignedDate").exists());
    }

    // Helper method to create a sample and get its ID
    private String createSampleAndGetId() {
        // Insert a sample directly via JDBC to ensure it exists
        // Use a numeric ID starting from 10000 to avoid conflicts with fixtures
        Long timestamp = System.currentTimeMillis();
        String sampleId = String.valueOf(10000 + (timestamp % 9000)); // Use last 4 digits of timestamp
        String accessionNumber = "ACC-" + timestamp;
        jdbcTemplate.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                Integer.parseInt(sampleId), accessionNumber);
        return sampleId;
    }

    // Helper methods to create entities and get their IDs
    private String createRoomAndGetId(String name, String code) throws Exception {
        org.openelisglobal.storage.form.StorageRoomForm form = new org.openelisglobal.storage.form.StorageRoomForm();
        form.setName(name);
        form.setCode(code);
        form.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asInt() + "";
    }

    private String createDeviceAndGetId(String name, String code, String type, String roomId) throws Exception {
        org.openelisglobal.storage.form.StorageDeviceForm form = new org.openelisglobal.storage.form.StorageDeviceForm();
        form.setName(name);
        form.setCode(code);
        form.setType(type);
        form.setParentRoomId(roomId);
        form.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asInt() + "";
    }

    private String createShelfAndGetId(String label, String deviceId) throws Exception {
        org.openelisglobal.storage.form.StorageShelfForm form = new org.openelisglobal.storage.form.StorageShelfForm();
        form.setLabel(label);
        form.setParentDeviceId(deviceId);
        form.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/shelves").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asInt() + "";
    }

    private String createRackAndGetId(String label, int rows, int columns, String shelfId) throws Exception {
        org.openelisglobal.storage.form.StorageRackForm form = new org.openelisglobal.storage.form.StorageRackForm();
        form.setLabel(label);
        form.setRows(rows);
        form.setColumns(columns);
        form.setParentShelfId(shelfId);
        form.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/racks").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asInt() + "";
    }

    /**
     * T042: Test assigning sample to occupied position returns HTTP 400 Validation:
     * Cannot assign to already-occupied position
     */
    @Test
    public void testAssignSample_OccupiedPosition_Returns400() throws Exception {
        // Given: Assignment to occupied position
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleId("sample-new");
        form.setPositionId("position-occupied"); // Already occupied
        form.setNotes("Should fail");

        String requestBody = objectMapper.writeValueAsString(form);

        // When: POST assignment to occupied position
        // Then: Expect 400 Bad Request
        mockMvc.perform(
                post("/rest/storage/samples/assign").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    /**
     * T042: Test assigning sample to inactive location returns HTTP 400 Validation:
     * Cannot assign to inactive storage location
     */
    @Test
    public void testAssignSample_InactiveLocation_Returns400() throws Exception {
        // Given: Assignment to position in inactive hierarchy
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleId("sample-456");
        form.setPositionId("position-inactive"); // In inactive location
        form.setNotes("Should fail");

        String requestBody = objectMapper.writeValueAsString(form);

        // When: POST assignment to inactive location
        // Then: Expect 400 Bad Request
        mockMvc.perform(
                post("/rest/storage/samples/assign").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    /**
     * T042: Test assigning sample with missing required fields returns HTTP 400
     * Validation: sampleId and positionId are required
     */
    @Test
    public void testAssignSample_MissingRequiredFields_Returns400() throws Exception {
        // Given: Form with missing sampleId
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setPositionId("123");
        // sampleId is null

        String requestBody = objectMapper.writeValueAsString(form);

        // When: POST with missing required field
        // Then: Expect 400 Bad Request
        mockMvc.perform(
                post("/rest/storage/samples/assign").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
