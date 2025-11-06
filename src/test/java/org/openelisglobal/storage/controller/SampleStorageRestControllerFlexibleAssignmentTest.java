package org.openelisglobal.storage.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.form.SampleAssignmentForm;
import org.openelisglobal.storage.form.SampleMovementForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for flexible assignment REST endpoints - Simplified
 * approach: - locationType: 'device', 'shelf', or 'rack' only (no 'position'
 * entity) - positionCoordinate: optional text field for any location_type - No
 * backward compatibility with positionId Following TDD: Write tests BEFORE
 * implementation
 */
public class SampleStorageRestControllerFlexibleAssignmentTest extends BaseWebContextSensitiveTest {

    private static final Logger logger = LoggerFactory
            .getLogger(SampleStorageRestControllerFlexibleAssignmentTest.class);

    @Autowired
    private DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanStorageTestData();
    }

    @After
    public void tearDown() throws Exception {
        cleanStorageTestData();
    }

    private void cleanStorageTestData() {
        try {
            jdbcTemplate.execute("DELETE FROM sample_storage_movement WHERE id::integer >= 1000");
            jdbcTemplate.execute("DELETE FROM sample_storage_assignment WHERE id::integer >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_rack WHERE id::integer >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_shelf WHERE id::integer >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_device WHERE id::integer >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE id::integer >= 1000");
        } catch (Exception e) {
            logger.warn("Failed to clean storage test data: " + e.getMessage());
        }
    }

    private String createSampleAndGetId() throws Exception {
        // Create a test sample
        String sampleResponse = mockMvc
                .perform(post("/rest/samples").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessionNumber\":\"TEST-" + System.currentTimeMillis() + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(sampleResponse).get("id").asText();
    }

    private String createRoomAndGetId(String name, String code) throws Exception {
        org.openelisglobal.storage.form.StorageRoomForm form = new org.openelisglobal.storage.form.StorageRoomForm();
        form.setName(name);
        form.setCode(code);
        form.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
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
        return objectMapper.readTree(response).get("id").asText();
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
        return objectMapper.readTree(response).get("id").asText();
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
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    public void testAssignSample_WithLocationIdAndType_Returns201() throws Exception {
        // Setup
        String sampleId = createSampleAndGetId();
        String roomId = createRoomAndGetId("Test Room", "TEST-ROOM-" + System.currentTimeMillis());
        String deviceId = createDeviceAndGetId("Test Device", "TEST-DEV-" + System.currentTimeMillis(), "freezer",
                roomId);

        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleId(sampleId);
        form.setLocationId(deviceId);
        form.setLocationType("device");
        form.setNotes("Test assignment");

        // Execute & Verify
        mockMvc.perform(post("/rest/storage/samples/assign").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignmentId").exists()).andExpect(jsonPath("$.hierarchicalPath").exists());
    }

    @Test
    public void testAssignSample_WithLocationIdAndType_DeviceLevel_Valid() throws Exception {
        // Setup
        String sampleId = createSampleAndGetId();
        String roomId = createRoomAndGetId("Main Lab", "MAIN-" + System.currentTimeMillis());
        String deviceId = createDeviceAndGetId("Freezer 1", "FRZ1-" + System.currentTimeMillis(), "freezer", roomId);

        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleId(sampleId);
        form.setLocationId(deviceId);
        form.setLocationType("device");
        form.setPositionCoordinate("A5");
        form.setNotes("Test assignment");

        // Execute
        String response = mockMvc
                .perform(post("/rest/storage/samples/assign").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        // Verify
        com.fasterxml.jackson.databind.JsonNode json = objectMapper.readTree(response);
        assertNotNull(json.get("assignmentId"));
        assertTrue(json.get("hierarchicalPath").asText().contains("Main Lab"));
        assertTrue(json.get("hierarchicalPath").asText().contains("Freezer 1"));
        assertTrue(json.get("hierarchicalPath").asText().contains("A5"));
    }

    @Test
    public void testAssignSample_WithLocationIdAndType_WithCoordinate_Valid() throws Exception {
        // Setup
        String sampleId = createSampleAndGetId();
        String roomId = createRoomAndGetId("Test Room", "TEST-ROOM-" + System.currentTimeMillis());
        String deviceId = createDeviceAndGetId("Test Device", "TEST-DEV-" + System.currentTimeMillis(), "freezer",
                roomId);
        String shelfId = createShelfAndGetId("Shelf-A", deviceId);
        String rackId = createRackAndGetId("Rack-1", 8, 12, shelfId);

        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleId(sampleId);
        form.setLocationId(rackId);
        form.setLocationType("rack");
        form.setPositionCoordinate("B3");
        form.setNotes("Test assignment with coordinate");

        // Execute
        String response = mockMvc
                .perform(post("/rest/storage/samples/assign").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        // Verify
        com.fasterxml.jackson.databind.JsonNode json = objectMapper.readTree(response);
        assertNotNull(json.get("assignmentId"));
        assertTrue(json.get("hierarchicalPath").asText().contains("Rack-1"));
        assertTrue(json.get("hierarchicalPath").asText().contains("B3"));
    }

    @Test
    public void testAssignSample_MissingLocationIdOrType_Returns400() throws Exception {
        // Setup - missing locationId
        String sampleId = createSampleAndGetId();
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleId(sampleId);
        form.setLocationType("device");
        form.setNotes("Test");

        // Execute & Verify
        mockMvc.perform(post("/rest/storage/samples/assign").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form))).andExpect(status().isBadRequest());

        // Setup - missing locationType
        form.setLocationId("10");
        form.setLocationType(null);

        // Execute & Verify
        mockMvc.perform(post("/rest/storage/samples/assign").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form))).andExpect(status().isBadRequest());
    }

    @Test
    public void testMoveSample_WithLocationIdAndType_Returns200() throws Exception {
        // Setup - create assignment first
        String sampleId = createSampleAndGetId();
        String roomId = createRoomAndGetId("Test Room", "TEST-ROOM-" + System.currentTimeMillis());
        String deviceId = createDeviceAndGetId("Test Device", "TEST-DEV-" + System.currentTimeMillis(), "freezer",
                roomId);
        String shelfId = createShelfAndGetId("Shelf-A", deviceId);

        // Assign to device
        SampleAssignmentForm assignForm = new SampleAssignmentForm();
        assignForm.setSampleId(sampleId);
        assignForm.setLocationId(deviceId);
        assignForm.setLocationType("device");
        assignForm.setNotes("Initial assignment");

        mockMvc.perform(post("/rest/storage/samples/assign").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignForm))).andExpect(status().isCreated());

        // Move to shelf
        SampleMovementForm moveForm = new SampleMovementForm();
        moveForm.setSampleId(sampleId);
        moveForm.setLocationId(shelfId);
        moveForm.setLocationType("shelf");
        moveForm.setReason("Moving to shelf");

        // Execute & Verify
        // This test verifies that findBySampleId correctly handles String-to-numeric
        // conversion
        // The sampleId is a String, but the database column is numeric
        mockMvc.perform(post("/rest/storage/samples/move").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(moveForm))).andExpect(status().isOk())
                .andExpect(jsonPath("$.movementId").exists());
    }

    /**
     * Test: Move sample with numeric sample ID (String format) - verifies
     * String-to-numeric conversion This test specifically covers the type mismatch
     * fix for findBySampleId
     */
    @Test
    public void testMoveSample_WithNumericSampleIdString_HandlesTypeConversion() throws Exception {
        // Setup - create assignment first
        String sampleId = createSampleAndGetId();
        // Verify sampleId is a String (even though it represents a number)
        assertNotNull("Sample ID should not be null", sampleId);

        String roomId = createRoomAndGetId("Test Room", "TEST-ROOM-" + System.currentTimeMillis());
        String deviceId = createDeviceAndGetId("Test Device", "TEST-DEV-" + System.currentTimeMillis(), "freezer",
                roomId);
        String shelfId = createShelfAndGetId("Shelf-A", deviceId);

        // Assign to device
        SampleAssignmentForm assignForm = new SampleAssignmentForm();
        assignForm.setSampleId(sampleId);
        assignForm.setLocationId(deviceId);
        assignForm.setLocationType("device");
        assignForm.setNotes("Initial assignment");

        mockMvc.perform(post("/rest/storage/samples/assign").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignForm))).andExpect(status().isCreated());

        // Move to shelf - this will call findBySampleId with String sampleId
        // The DAO should convert it to Integer for the database query
        SampleMovementForm moveForm = new SampleMovementForm();
        moveForm.setSampleId(sampleId); // String, but represents numeric ID
        moveForm.setLocationId(shelfId);
        moveForm.setLocationType("shelf");
        moveForm.setReason("Moving to shelf");

        // Execute & Verify - should succeed despite String-to-numeric conversion
        mockMvc.perform(post("/rest/storage/samples/move").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(moveForm))).andExpect(status().isOk())
                .andExpect(jsonPath("$.movementId").exists());
    }

    /**
     * Integration test: Move sample from device to rack - simulates browser
     * scenario This test verifies the complete end-to-end flow including: 1.
     * Creating a sample 2. Assigning it to a device 3. Moving it to a rack 4.
     * Verifying the movement record is created correctly with flexible assignment
     * fields
     */
    @Test
    public void testMoveSample_DeviceToRack_EndToEnd() throws Exception {
        // Setup - create sample and locations
        String sampleId = createSampleAndGetId();
        String roomId = createRoomAndGetId("Test Room", "TEST-ROOM-" + System.currentTimeMillis());
        String deviceId = createDeviceAndGetId("Test Device", "TEST-DEV-" + System.currentTimeMillis(), "freezer",
                roomId);
        String shelfId = createShelfAndGetId("Shelf-A", deviceId);
        String rackId = createRackAndGetId("Rack-1", 8, 12, shelfId);

        // Step 1: Assign sample to device
        SampleAssignmentForm assignForm = new SampleAssignmentForm();
        assignForm.setSampleId(sampleId);
        assignForm.setLocationId(deviceId);
        assignForm.setLocationType("device");
        assignForm.setNotes("Initial assignment to device");

        String assignResponse = mockMvc
                .perform(post("/rest/storage/samples/assign").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignForm)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.assignmentId").exists()).andReturn()
                .getResponse().getContentAsString();

        // Step 2: Move sample to rack
        SampleMovementForm moveForm = new SampleMovementForm();
        moveForm.setSampleId(sampleId);
        moveForm.setLocationId(rackId);
        moveForm.setLocationType("rack");
        moveForm.setReason("Moving to rack");

        String moveResponse = mockMvc
                .perform(post("/rest/storage/samples/move").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveForm)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.movementId").exists()).andReturn().getResponse()
                .getContentAsString();

        // Step 3: Verify movement record in database
        com.fasterxml.jackson.databind.JsonNode moveResponseJson = objectMapper.readTree(moveResponse);
        String movementId = moveResponseJson.get("movementId").asText();

        // Query database to verify movement record
        Map<String, Object> movementRecord = jdbcTemplate
                .queryForMap("SELECT * FROM sample_storage_movement WHERE id = ?", Integer.parseInt(movementId));

        // Verify previous location (device)
        assertNotNull("Movement record should exist", movementRecord);
        assertEquals("Previous location ID should match device", Integer.parseInt(deviceId),
                ((Number) movementRecord.get("previous_location_id")).intValue());
        assertEquals("Previous location type should be 'device'", "device",
                movementRecord.get("previous_location_type"));
        assertNull("Previous position coordinate should be null", movementRecord.get("previous_position_coordinate"));

        // Verify new location (rack)
        assertEquals("New location ID should match rack", Integer.parseInt(rackId),
                ((Number) movementRecord.get("new_location_id")).intValue());
        assertEquals("New location type should be 'rack'", "rack", movementRecord.get("new_location_type"));
        assertNull("New position coordinate should be null", movementRecord.get("new_position_coordinate"));

        // Verify assignment was updated
        Map<String, Object> assignmentRecord = jdbcTemplate
                .queryForMap("SELECT * FROM sample_storage_assignment WHERE sample_id = ?", Integer.parseInt(sampleId));
        assertEquals("Assignment location ID should be updated to rack", Integer.parseInt(rackId),
                ((Number) assignmentRecord.get("location_id")).intValue());
        assertEquals("Assignment location type should be updated to 'rack'", "rack",
                assignmentRecord.get("location_type"));
    }

    @Test
    public void testMoveSample_WithLocationIdAndType_DeviceToRack_Valid() throws Exception {
        // Setup
        String sampleId = createSampleAndGetId();
        String roomId = createRoomAndGetId("Main Lab", "MAIN-" + System.currentTimeMillis());
        String deviceId = createDeviceAndGetId("Freezer 1", "FRZ1-" + System.currentTimeMillis(), "freezer", roomId);
        String shelfId = createShelfAndGetId("Shelf-A", deviceId);
        String rackId = createRackAndGetId("Rack-1", 8, 12, shelfId);

        // Assign to device
        SampleAssignmentForm assignForm = new SampleAssignmentForm();
        assignForm.setSampleId(sampleId);
        assignForm.setLocationId(deviceId);
        assignForm.setLocationType("device");
        assignForm.setNotes("Initial assignment");

        mockMvc.perform(post("/rest/storage/samples/assign").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignForm))).andExpect(status().isCreated());

        // Move to rack with coordinate
        SampleMovementForm moveForm = new SampleMovementForm();
        moveForm.setSampleId(sampleId);
        moveForm.setLocationId(rackId);
        moveForm.setLocationType("rack");
        moveForm.setPositionCoordinate("C7");
        moveForm.setReason("Moving to rack");

        // Execute
        String response = mockMvc
                .perform(post("/rest/storage/samples/move").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveForm)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Verify
        com.fasterxml.jackson.databind.JsonNode json = objectMapper.readTree(response);
        assertNotNull(json.get("movementId"));
        assertNotNull(json.get("newHierarchicalPath"));
        assertTrue(json.get("newHierarchicalPath").asText().contains("Rack-1"));
        assertTrue(json.get("newHierarchicalPath").asText().contains("C7"));
    }
}
