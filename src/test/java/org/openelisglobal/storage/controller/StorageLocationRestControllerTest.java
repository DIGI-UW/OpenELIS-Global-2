package org.openelisglobal.storage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.form.StorageDeviceForm;
import org.openelisglobal.storage.form.StoragePositionForm;
import org.openelisglobal.storage.form.StorageRackForm;
import org.openelisglobal.storage.form.StorageRoomForm;
import org.openelisglobal.storage.form.StorageShelfForm;
import org.openelisglobal.storage.service.StorageLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for StorageLocationRestController - Room CRUD operations
 * Following TDD approach: Write tests BEFORE implementation Tests based on
 * contracts/storage-api.json specification
 */
public class StorageLocationRestControllerTest extends BaseWebContextSensitiveTest {

    private static final Logger logger = LoggerFactory.getLogger(StorageLocationRestControllerTest.class);

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        // Clean up storage tables before each test to ensure atomicity
        // Note: This preserves fixture data loaded by Liquibase (IDs 1-999), but cleans
        // test-created data
        cleanStorageTestData();
    }

    @After
    public void tearDown() throws Exception {
        // Clean up any test data created during this test
        cleanStorageTestData();
    }

    /**
     * Clean up storage-related test data to ensure tests don't pollute the
     * database. This method deletes test-created entities but preserves fixture
     * data. Fixture data has IDs 1-999, so we delete IDs >= 1000 or entities with
     * TEST- prefix codes.
     */
    private void cleanStorageTestData() {
        try {
            // Delete test-created data (IDs >= 1000 or codes/names starting with TEST-)
            // This preserves fixture data loaded by Liquibase (IDs 1-999)
            // IDs are stored as VARCHAR, so we compare as strings
            jdbcTemplate.execute("DELETE FROM sample_storage_movement WHERE id::integer >= 1000 OR id LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM sample_storage_assignment WHERE id::integer >= 1000 OR id LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_position WHERE id::integer >= 1000 OR coordinate LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_rack WHERE id::integer >= 1000 OR label LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_shelf WHERE id::integer >= 1000 OR label LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_device WHERE id::integer >= 1000 OR code LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE id::integer >= 1000 OR code LIKE 'TEST-%'");
        } catch (Exception e) {
            // Log but don't fail - cleanup is best effort
            logger.warn("Failed to clean storage test data: " + e.getMessage());
        }
    }

    /**
     * T027: Test creating a room with valid input returns HTTP 201 Created
     * Contract: POST /rest/storage/rooms with valid JSON → 201 + room ID
     */
    @Test
    public void testCreateRoom_ValidInput_Returns201() throws Exception {
        // Given: Valid room form data
        StorageRoomForm roomForm = new StorageRoomForm();
        roomForm.setName("Main Laboratory");
        roomForm.setCode("TEST-ROOM-" + System.currentTimeMillis()); // Unique code to avoid fixture conflicts
        roomForm.setDescription("Primary laboratory room");
        roomForm.setActive(true);

        String requestBody = objectMapper.writeValueAsString(roomForm);

        // When: POST to /rest/storage/rooms
        // Then: Expect 201 Created with room ID in response
        mockMvc.perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").value(roomForm.getCode()))
                .andExpect(jsonPath("$.name").value("Main Laboratory")).andExpect(jsonPath("$.fhirUuid").exists());
    }

    /**
     * T027: Test getting all rooms returns HTTP 200 with list Contract: GET
     * /rest/storage/rooms → 200 + array of rooms
     */
    @Test
    public void testGetRooms_ReturnsAllRooms() throws Exception {
        // Given: At least one room exists in database
        // (Created via testCreateRoom or test setup)

        // When: GET /rest/storage/rooms
        // Then: Expect 200 OK with array of rooms
        mockMvc.perform(get("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * T027: Test getting room by ID returns HTTP 200 with room data Contract: GET
     * /rest/storage/rooms/{id} → 200 + room object
     */
    @Test
    public void testGetRoomById_ValidId_ReturnsRoom() throws Exception {
        // Given: Create a room to retrieve
        StorageRoomForm roomForm = new StorageRoomForm();
        roomForm.setName("Test Room for GET");
        roomForm.setCode("TEST-GET-" + System.currentTimeMillis()); // Unique code
        roomForm.setActive(true);

        String requestBody = objectMapper.writeValueAsString(roomForm);

        // Create room and extract ID from response
        String response = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String roomId = objectMapper.readTree(response).get("id").asText();

        // When: GET /rest/storage/rooms/{id}
        // Then: Expect 200 OK with room data
        mockMvc.perform(get("/rest/storage/rooms/" + roomId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(roomId))
                .andExpect(jsonPath("$.code").value(roomForm.getCode()));
    }

    /**
     * T027: Test deleting room with children returns HTTP 409 Conflict Contract:
     * DELETE /rest/storage/rooms/{id} with children → 409 Validation: Cannot delete
     * room with active child devices
     */
    @Test
    public void testDeleteRoom_WithChildren_Returns409() throws Exception {
        // Given: Create room with child device
        StorageRoomForm roomForm = new StorageRoomForm();
        roomForm.setName("Room With Device");
        roomForm.setCode("ROOM-DEV-" + System.currentTimeMillis()); // Unique code
        roomForm.setActive(true);

        String roomRequestBody = objectMapper.writeValueAsString(roomForm);

        String roomResponse = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON).content(roomRequestBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String roomId = objectMapper.readTree(roomResponse).get("id").asText();

        // Create child device to ensure room has children
        StorageDeviceForm deviceForm = new StorageDeviceForm();
        deviceForm.setName("Test Device");
        deviceForm.setCode("TEST-DEV-" + System.currentTimeMillis());
        deviceForm.setType("freezer");
        deviceForm.setParentRoomId(roomId);
        deviceForm.setActive(true);

        mockMvc.perform(post("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deviceForm))).andExpect(status().isCreated());

        // When: DELETE /rest/storage/rooms/{id}
        // Then: Expect 409 Conflict (cannot delete room with children)
        mockMvc.perform(delete("/rest/storage/rooms/" + roomId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    /**
     * T027: Test updating room with valid data returns HTTP 200 Contract: PUT
     * /rest/storage/rooms/{id} with valid JSON → 200
     */
    @Test
    public void testUpdateRoom_ValidInput_Returns200() throws Exception {
        // Given: Create room to update
        StorageRoomForm roomForm = new StorageRoomForm();
        roomForm.setName("Original Name");
        roomForm.setCode("ORIG");
        roomForm.setActive(true);

        String createRequestBody = objectMapper.writeValueAsString(roomForm);

        String createResponse = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON).content(createRequestBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String roomId = objectMapper.readTree(createResponse).get("id").asText();

        // Modify room data
        roomForm.setName("Updated Name");
        roomForm.setDescription("Updated description");
        String updateRequestBody = objectMapper.writeValueAsString(roomForm);

        // When: PUT /rest/storage/rooms/{id}
        // Then: Expect 200 OK with updated data
        mockMvc.perform(
                put("/rest/storage/rooms/" + roomId).contentType(MediaType.APPLICATION_JSON).content(updateRequestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(roomId))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    /**
     * T027: Test creating room with duplicate code returns HTTP 400 Contract: POST
     * /rest/storage/rooms with duplicate code → 400 Validation: Room code must be
     * unique globally
     */
    @Test
    public void testCreateRoom_DuplicateCode_Returns400() throws Exception {
        // Given: Create first room with code "DUP-CODE"
        StorageRoomForm roomForm1 = new StorageRoomForm();
        roomForm1.setName("First Room");
        roomForm1.setCode("DUP-CODE");
        roomForm1.setActive(true);

        String requestBody1 = objectMapper.writeValueAsString(roomForm1);

        mockMvc.perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON).content(requestBody1))
                .andExpect(status().isCreated());

        // Given: Create second room with same code "DUP-CODE"
        StorageRoomForm roomForm2 = new StorageRoomForm();
        roomForm2.setName("Second Room");
        roomForm2.setCode("DUP-CODE"); // Duplicate code
        roomForm2.setActive(true);

        String requestBody2 = objectMapper.writeValueAsString(roomForm2);

        // When: POST second room with duplicate code
        // Then: Expect 400 Bad Request
        mockMvc.perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON).content(requestBody2))
                .andExpect(status().isBadRequest());
    }

    /**
     * T027: Test creating room with missing required fields returns HTTP 400
     * Contract: POST /rest/storage/rooms with invalid data → 400 Validation: Name
     * and code are required fields
     */
    @Test
    public void testCreateRoom_MissingRequiredFields_Returns400() throws Exception {
        // Given: Room form with missing name
        StorageRoomForm roomForm = new StorageRoomForm();
        roomForm.setCode("MISSING-NAME");
        // Name is null - should fail validation

        String requestBody = objectMapper.writeValueAsString(roomForm);

        // When: POST with missing required field
        // Then: Expect 400 Bad Request
        mockMvc.perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ========== T028: Device CRUD Tests ==========

    /**
     * T028: Test creating device with valid input returns HTTP 201 Contract: POST
     * /rest/storage/devices with valid JSON → 201 + device ID
     */
    @Test
    public void testCreateDevice_ValidInput_Returns201() throws Exception {
        // Given: Create parent room first
        StorageRoomForm roomForm = new StorageRoomForm();
        roomForm.setName("Device Test Room");
        roomForm.setCode("DEV-ROOM");
        roomForm.setActive(true);

        String roomResponse = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomForm)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String roomId = objectMapper.readTree(roomResponse).get("id").asText();

        // Given: Valid device form data
        StorageDeviceForm deviceForm = new StorageDeviceForm();
        deviceForm.setName("Freezer Unit 1");
        deviceForm.setCode("TEST-FRZ-" + System.currentTimeMillis()); // Unique code to avoid fixture conflicts
        deviceForm.setType("freezer");
        deviceForm.setTemperatureSetting(-80.0);
        deviceForm.setParentRoomId(roomId);
        deviceForm.setActive(true);

        String requestBody = objectMapper.writeValueAsString(deviceForm);

        // When: POST to /rest/storage/devices
        // Then: Expect 201 Created with device ID
        mockMvc.perform(post("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").value(deviceForm.getCode()))
                .andExpect(jsonPath("$.name").value("Freezer Unit 1")).andExpect(jsonPath("$.type").value("freezer"))
                .andExpect(jsonPath("$.fhirUuid").exists());
    }

    /**
     * T028: Test getting devices filtered by room ID returns only devices in that
     * room Contract: GET /rest/storage/devices?roomId={id} → 200 + filtered array
     */
    @Test
    public void testGetDevices_FilterByRoomId_ReturnsFiltered() throws Exception {
        // Given: Create room with device
        StorageRoomForm roomForm = new StorageRoomForm();
        roomForm.setName("Filter Test Room");
        roomForm.setCode("FILTER-ROOM-" + System.currentTimeMillis()); // Unique code
        roomForm.setActive(true);

        String roomResponse = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomForm)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String roomId = objectMapper.readTree(roomResponse).get("id").asText();

        // Create device in room
        StorageDeviceForm deviceForm = new StorageDeviceForm();
        deviceForm.setName("Test Device");
        deviceForm.setCode("TEST-DEV");
        deviceForm.setType("refrigerator");
        deviceForm.setParentRoomId(roomId);
        deviceForm.setActive(true);

        mockMvc.perform(post("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deviceForm))).andExpect(status().isCreated());

        // When: GET devices filtered by room ID
        // Then: Expect 200 OK with array containing only devices in that room
        mockMvc.perform(get("/rest/storage/devices").param("roomId", roomId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].parentRoomId").value(roomId));
    }

    /**
     * T028: Test creating device with duplicate code in same room returns HTTP 400
     * Contract: POST /rest/storage/devices with duplicate code → 400 Validation:
     * Device code must be unique within parent room
     */
    @Test
    public void testCreateDevice_DuplicateCode_Returns400() throws Exception {
        // Given: Create room
        StorageRoomForm roomForm = new StorageRoomForm();
        roomForm.setName("Duplicate Device Test Room");
        roomForm.setCode("DUP-DEV-ROOM");
        roomForm.setActive(true);

        String roomResponse = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomForm)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String roomId = objectMapper.readTree(roomResponse).get("id").asText();

        // Given: Create first device with code "DUP-DEV"
        StorageDeviceForm deviceForm1 = new StorageDeviceForm();
        deviceForm1.setName("First Device");
        deviceForm1.setCode("DUP-DEV");
        deviceForm1.setType("freezer");
        deviceForm1.setParentRoomId(roomId);
        deviceForm1.setActive(true);

        mockMvc.perform(post("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deviceForm1))).andExpect(status().isCreated());

        // Given: Create second device with same code in same room
        StorageDeviceForm deviceForm2 = new StorageDeviceForm();
        deviceForm2.setName("Second Device");
        deviceForm2.setCode("DUP-DEV"); // Duplicate code
        deviceForm2.setType("refrigerator");
        deviceForm2.setParentRoomId(roomId); // Same room
        deviceForm2.setActive(true);

        // When: POST second device with duplicate code
        // Then: Expect 400 Bad Request
        mockMvc.perform(post("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deviceForm2))).andExpect(status().isBadRequest());
    }

    /**
     * T028: Test creating device with invalid type returns HTTP 400 Contract: POST
     * /rest/storage/devices with invalid enum → 400 Validation: Type must be one
     * of: freezer, refrigerator, cabinet, other
     */
    @Test
    public void testCreateDevice_InvalidType_Returns400() throws Exception {
        // Given: Create room
        StorageRoomForm roomForm = new StorageRoomForm();
        roomForm.setName("Invalid Type Test Room");
        roomForm.setCode("INVALID-TYPE-ROOM");
        roomForm.setActive(true);

        String roomResponse = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomForm)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String roomId = objectMapper.readTree(roomResponse).get("id").asText();

        // Given: Device form with invalid type
        StorageDeviceForm deviceForm = new StorageDeviceForm();
        deviceForm.setName("Invalid Device");
        deviceForm.setCode("INVALID-DEV");
        deviceForm.setType("invalid_type"); // Invalid enum value
        deviceForm.setParentRoomId(roomId);
        deviceForm.setActive(true);

        // When: POST device with invalid type
        // Then: Expect 400 Bad Request
        mockMvc.perform(post("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deviceForm))).andExpect(status().isBadRequest());
    }

    // ========== T029: Shelf, Rack, Position CRUD Tests ==========

    /**
     * T029: Test creating shelf with valid input returns HTTP 201 Contract: POST
     * /rest/storage/shelves with valid JSON → 201 + shelf ID
     */
    @Test
    public void testCreateShelf_ValidInput_Returns201() throws Exception {
        // Given: Create room and device hierarchy
        String roomId = createRoomAndGetId("Shelf Test Room", "SHELF-ROOM");
        String deviceId = createDeviceAndGetId("Test Device", "SHELF-DEV", "freezer", roomId);

        // Given: Valid shelf form data
        StorageShelfForm shelfForm = new StorageShelfForm();
        shelfForm.setLabel("Shelf-A");
        shelfForm.setCapacityLimit(50);
        shelfForm.setParentDeviceId(deviceId);
        shelfForm.setActive(true);

        // When: POST to /rest/storage/shelves
        // Then: Expect 201 Created
        mockMvc.perform(post("/rest/storage/shelves").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shelfForm))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists()).andExpect(jsonPath("$.label").value("Shelf-A"))
                .andExpect(jsonPath("$.fhirUuid").exists());
    }

    /**
     * T029: Test creating rack with grid dimensions returns HTTP 201 Contract: POST
     * /rest/storage/racks with valid JSON → 201 + rack ID
     */
    @Test
    public void testCreateRack_WithGridDimensions_Returns201() throws Exception {
        // Given: Create hierarchy
        String roomId = createRoomAndGetId("Rack Test Room", "RACK-ROOM");
        String deviceId = createDeviceAndGetId("Test Device", "RACK-DEV", "refrigerator", roomId);
        String shelfId = createShelfAndGetId("Shelf-1", deviceId);

        // Given: Valid rack form with grid
        StorageRackForm rackForm = new StorageRackForm();
        rackForm.setLabel("Rack R1");
        rackForm.setRows(8);
        rackForm.setColumns(12);
        rackForm.setPositionSchemaHint("A1");
        rackForm.setParentShelfId(shelfId);
        rackForm.setActive(true);

        // When: POST to /rest/storage/racks
        // Then: Expect 201 Created with grid dimensions
        mockMvc.perform(post("/rest/storage/racks").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rackForm))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists()).andExpect(jsonPath("$.label").value("Rack R1"))
                .andExpect(jsonPath("$.rows").value(8)).andExpect(jsonPath("$.columns").value(12))
                .andExpect(jsonPath("$.fhirUuid").exists());
    }

    /**
     * T029: Test creating position with coordinate returns HTTP 201 Contract: POST
     * /rest/storage/positions with valid JSON → 201 + position ID
     */
    @Test
    public void testCreatePosition_ValidCoordinate_Returns201() throws Exception {
        // Given: Create full hierarchy
        String roomId = createRoomAndGetId("Position Test Room", "POS-ROOM");
        String deviceId = createDeviceAndGetId("Test Device", "POS-DEV", "freezer", roomId);
        String shelfId = createShelfAndGetId("Shelf-1", deviceId);
        String rackId = createRackAndGetId("Rack R1", 8, 12, shelfId);

        // Given: Valid position form
        StoragePositionForm positionForm = new StoragePositionForm();
        positionForm.setCoordinate("A5");
        positionForm.setRowIndex(1);
        positionForm.setColumnIndex(5);
        positionForm.setParentRackId(rackId);
        positionForm.setOccupied(false);

        // When: POST to /rest/storage/positions
        // Then: Expect 201 Created
        mockMvc.perform(post("/rest/storage/positions").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(positionForm))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists()).andExpect(jsonPath("$.coordinate").value("A5"))
                .andExpect(jsonPath("$.occupied").value(false)).andExpect(jsonPath("$.fhirUuid").exists());
    }

    /**
     * T029: Test creating position with duplicate coordinate succeeds (flexible
     * storage) Per FR-014: Allow duplicate coordinates within same rack Contract:
     * POST /rest/storage/positions with duplicate coordinate → 201
     */
    @Test
    public void testCreatePosition_DuplicateCoordinate_Returns201() throws Exception {
        // Given: Create rack
        String roomId = createRoomAndGetId("Duplicate Position Room", "DUP-POS-ROOM");
        String deviceId = createDeviceAndGetId("Test Device", "DUP-POS-DEV", "cabinet", roomId);
        String shelfId = createShelfAndGetId("Shelf-1", deviceId);
        String rackId = createRackAndGetId("Rack R1", 0, 0, shelfId); // No grid

        // Given: Create first position with coordinate "RED-01"
        StoragePositionForm positionForm1 = new StoragePositionForm();
        positionForm1.setCoordinate("RED-01");
        positionForm1.setParentRackId(rackId);

        mockMvc.perform(post("/rest/storage/positions").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(positionForm1))).andExpect(status().isCreated());

        // Given: Create second position with same coordinate "RED-01"
        StoragePositionForm positionForm2 = new StoragePositionForm();
        positionForm2.setCoordinate("RED-01"); // Duplicate coordinate
        positionForm2.setParentRackId(rackId);

        // When: POST second position with duplicate coordinate
        // Then: Expect 201 Created (duplicates allowed per FR-014)
        mockMvc.perform(post("/rest/storage/positions").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(positionForm2))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.coordinate").value("RED-01"));
    }

    /**
     * T029: Test getting positions filtered by rack ID and occupancy Contract: GET
     * /rest/storage/positions?rackId={id}&occupied=false → 200 + filtered array
     */
    @Test
    public void testGetPositions_FilterByRackAndOccupancy_ReturnsFiltered() throws Exception {
        // Given: Create rack with positions
        String roomId = createRoomAndGetId("Filter Position Room", "FILTER-POS-ROOM");
        String deviceId = createDeviceAndGetId("Test Device", "FILTER-POS-DEV", "freezer", roomId);
        String shelfId = createShelfAndGetId("Shelf-1", deviceId);
        String rackId = createRackAndGetId("Rack R1", 8, 12, shelfId);

        // Create occupied position
        StoragePositionForm occupiedPosition = new StoragePositionForm();
        occupiedPosition.setCoordinate("A1");
        occupiedPosition.setParentRackId(rackId);
        occupiedPosition.setOccupied(true);

        mockMvc.perform(post("/rest/storage/positions").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(occupiedPosition))).andExpect(status().isCreated());

        // Create unoccupied position
        StoragePositionForm unoccupiedPosition = new StoragePositionForm();
        unoccupiedPosition.setCoordinate("A2");
        unoccupiedPosition.setParentRackId(rackId);
        unoccupiedPosition.setOccupied(false);

        mockMvc.perform(post("/rest/storage/positions").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unoccupiedPosition))).andExpect(status().isCreated());

        // When: GET unoccupied positions in rack
        // Then: Expect only unoccupied positions returned
        mockMvc.perform(get("/rest/storage/positions").param("rackId", rackId).param("occupied", "false")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].occupied").value(false));
    }

    // ========== Helper Methods for Test Setup ==========

    private String createRoomAndGetId(String name, String code) throws Exception {
        StorageRoomForm roomForm = new StorageRoomForm();
        roomForm.setName(name);
        // Ensure unique code to avoid conflicts with fixture data
        String uniqueCode = code + "-" + System.currentTimeMillis();
        roomForm.setCode(uniqueCode);
        roomForm.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomForm)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    private String createDeviceAndGetId(String name, String code, String type, String roomId) throws Exception {
        StorageDeviceForm deviceForm = new StorageDeviceForm();
        deviceForm.setName(name);
        deviceForm.setCode(code);
        deviceForm.setType(type);
        deviceForm.setParentRoomId(roomId);
        deviceForm.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deviceForm)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    private String createShelfAndGetId(String label, String deviceId) throws Exception {
        StorageShelfForm shelfForm = new StorageShelfForm();
        shelfForm.setLabel(label);
        shelfForm.setParentDeviceId(deviceId);
        shelfForm.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/shelves").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shelfForm)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    private String createRackAndGetId(String label, int rows, int columns, String shelfId) throws Exception {
        StorageRackForm rackForm = new StorageRackForm();
        rackForm.setLabel(label);
        rackForm.setRows(rows);
        rackForm.setColumns(columns);
        rackForm.setParentShelfId(shelfId);
        rackForm.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/racks").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rackForm)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }
}
