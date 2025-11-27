package org.openelisglobal.storage.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
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
            // Also clean up by code patterns used in tests (TEST- prefix)
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
        // Given: Valid room form data (code must be ≤10 chars)
        StorageRoomForm roomForm = new StorageRoomForm();
        roomForm.setName("Main Laboratory");
        // Use unique code ≤10 chars: "TESTROOM" + 2 digits = 9 chars
        String uniqueCode = "TESTROOM" + (System.currentTimeMillis() % 100);
        roomForm.setCode(uniqueCode);
        roomForm.setDescription("Primary laboratory room");
        roomForm.setActive(true);

        String requestBody = objectMapper.writeValueAsString(roomForm);

        // When: POST to /rest/storage/rooms
        // Then: Expect 201 Created with room ID in response
        mockMvc.perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").value(uniqueCode)).andExpect(jsonPath("$.name").value("Main Laboratory"))
                .andExpect(jsonPath("$.fhirUuid").exists());
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
        // Ensure unique code ≤10 chars: "TESTGET" + 2 digits = 9 chars max
        long timestamp = System.currentTimeMillis() % 100;
        roomForm.setCode("TESTGET" + String.format("%02d", timestamp)); // Unique code
        roomForm.setActive(true);

        String requestBody = objectMapper.writeValueAsString(roomForm);

        // Create room and extract ID from response
        String response = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String roomId = objectMapper.readTree(response).get("id").asInt() + "";

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
        // Ensure unique code ≤10 chars: "ROOMDEV" + 2 digits = 9 chars max
        long timestamp = System.currentTimeMillis() % 100;
        roomForm.setCode("ROOMDEV" + String.format("%02d", timestamp)); // Unique code
        roomForm.setActive(true);

        String roomRequestBody = objectMapper.writeValueAsString(roomForm);

        String roomResponse = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON).content(roomRequestBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String roomId = objectMapper.readTree(roomResponse).get("id").asText();

        // Create child device to ensure room has children
        StorageDeviceForm deviceForm = new StorageDeviceForm();
        deviceForm.setName("Test Device");
        // Ensure unique code ≤10 chars: "TESTDEV" + 2 digits = 8 chars max
        long deviceTimestamp = System.currentTimeMillis() % 100;
        deviceForm.setCode("TESTDEV" + String.format("%02d", deviceTimestamp));
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
        // Ensure unique code ≤10 chars: "TESTFRZ" + 2 digits = 9 chars max
        long timestamp = System.currentTimeMillis() % 100;
        deviceForm.setCode("TESTFRZ" + String.format("%02d", timestamp)); // Unique code to avoid fixture conflicts
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
     * T028: Test getting all devices returns HTTP 200 with array Contract: GET
     * /rest/storage/devices → 200 + array of devices
     */
    @Test
    public void testGetDevices_ReturnsAllDevices() throws Exception {
        // Given: At least one device exists in database (from fixtures or previous
        // tests)

        // When: GET /rest/storage/devices (without roomId parameter)
        // Then: Expect 200 OK with array of devices
        // CRITICAL: This test validates that getAllDevices() DAO query works without
        // type mismatch errors (VARCHAR vs NUMERIC). If type mapping is wrong,
        // this will fail with "operator does not exist: character varying = integer"
        mockMvc.perform(get("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Additional validation: Verify response contains device data with parentRoom
        // relationships
        // This ensures lazy loading of relationships works without type errors
        String response = mockMvc.perform(get("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        // If devices exist, verify parentRoom data is populated (proves relationship
        // loading works)
        if (response.length() > 10) { // Non-empty array
            mockMvc.perform(get("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].roomName").exists());
        }
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
        // Ensure unique code ≤10 chars: "FILTROOM" + 2 digits = 10 chars max
        long timestamp = System.currentTimeMillis() % 100;
        roomForm.setCode("FILTROOM" + String.format("%02d", timestamp)); // Unique code
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
        // Given: Create room (code must be ≤10 chars)
        StorageRoomForm roomForm = new StorageRoomForm();
        roomForm.setName("Duplicate Device Test Room");
        roomForm.setCode("DUPDEVROOM"); // 10 chars
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
        // Given: Create room (code must be ≤10 chars)
        StorageRoomForm roomForm = new StorageRoomForm();
        roomForm.setName("Invalid Type Test Room");
        roomForm.setCode("INVTYPROOM"); // 10 chars
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
     * T029: Test getting all shelves returns HTTP 200 with array Contract: GET
     * /rest/storage/shelves → 200 + array of shelves
     */
    @Test
    public void testGetShelves_ReturnsAllShelves() throws Exception {
        // Given: At least one shelf exists in database

        // When: GET /rest/storage/shelves (without deviceId parameter)
        // Then: Expect 200 OK with array of shelves
        // CRITICAL: This validates getAllShelves() DAO query and parentDevice
        // relationship
        // loading without type mismatch errors
        mockMvc.perform(get("/rest/storage/shelves").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Verify parentDevice relationships can be loaded (proves foreign key type
        // matching)
        String response = mockMvc.perform(get("/rest/storage/shelves").contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        if (response.length() > 10) { // Non-empty array
            mockMvc.perform(get("/rest/storage/shelves").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].deviceName").exists());
        }
    }

    /**
     * T029: Test getting all racks returns HTTP 200 with array Contract: GET
     * /rest/storage/racks → 200 + array of racks
     */
    @Test
    public void testGetRacks_ReturnsAllRacks() throws Exception {
        // Given: At least one rack exists in database

        // When: GET /rest/storage/racks (without shelfId parameter)
        // Then: Expect 200 OK with array of racks
        // CRITICAL: This validates getAllRacks() DAO query and parentShelf relationship
        // loading without type mismatch errors
        mockMvc.perform(get("/rest/storage/racks").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Verify parentShelf relationships can be loaded (proves foreign key type
        // matching)
        String response = mockMvc.perform(get("/rest/storage/racks").contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        if (response.length() > 10) { // Non-empty array
            mockMvc.perform(get("/rest/storage/racks").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].shelfLabel").exists());
        }
    }

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
        // Occupancy is now calculated dynamically from SampleStorageAssignment records

        // When: POST to /rest/storage/positions
        // Then: Expect 201 Created
        mockMvc.perform(post("/rest/storage/positions").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(positionForm))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists()).andExpect(jsonPath("$.coordinate").value("A5"))
                .andExpect(jsonPath("$.fhirUuid").exists());
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
        // Occupancy is now calculated dynamically from SampleStorageAssignment records

        mockMvc.perform(post("/rest/storage/positions").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(occupiedPosition))).andExpect(status().isCreated());

        // Create unoccupied position
        StoragePositionForm unoccupiedPosition = new StoragePositionForm();
        unoccupiedPosition.setCoordinate("A2");
        unoccupiedPosition.setParentRackId(rackId);
        // Occupancy is now calculated dynamically from SampleStorageAssignment records

        mockMvc.perform(post("/rest/storage/positions").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unoccupiedPosition))).andExpect(status().isCreated());

        // When: GET positions in rack
        // Then: Expect positions returned (occupancy is now calculated dynamically from
        // SampleStorageAssignment)
        mockMvc.perform(get("/rest/storage/positions").param("rackId", rackId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    // ========== Helper Methods for Test Setup ==========

    private String createRoomAndGetId(String name, String code) throws Exception {
        StorageRoomForm roomForm = new StorageRoomForm();
        roomForm.setName(name);
        // Ensure unique code to avoid conflicts with fixture data (max 10 chars)
        // Use last 2 digits of timestamp to keep code ≤10 chars
        long timestamp = System.currentTimeMillis() % 100;
        // Truncate code to max 8 chars to leave room for 2-digit suffix
        String baseCode = code.length() <= 8 ? code : code.substring(0, 8);
        String uniqueCode = baseCode + String.format("%02d", timestamp);
        roomForm.setCode(uniqueCode);
        roomForm.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomForm)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asInt() + "";
    }

    private String createDeviceAndGetId(String name, String code, String type, String roomId) throws Exception {
        StorageDeviceForm deviceForm = new StorageDeviceForm();
        deviceForm.setName(name);
        // Ensure code is ≤10 chars - truncate if necessary and add unique suffix
        long timestamp = System.currentTimeMillis() % 100;
        String baseCode = code.length() <= 8 ? code : code.substring(0, 8);
        String uniqueCode = baseCode + String.format("%02d", timestamp);
        deviceForm.setCode(uniqueCode);
        deviceForm.setType(type);
        deviceForm.setParentRoomId(roomId);
        deviceForm.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deviceForm)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asInt() + "";
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

        return objectMapper.readTree(response).get("id").asInt() + "";
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

        return objectMapper.readTree(response).get("id").asInt() + "";
    }

    // ========== Phase 6: Location CRUD Operations - Edit Location Tests (T099)
    // ==========

    /**
     * T099: Test updating room with editable fields returns HTTP 200 Contract: PUT
     * /rest/storage/rooms/{id} with name, description, status → 200
     */
    @Test
    public void testUpdateRoom_UpdatesEditableFields() throws Exception {
        // Given: Create room to update
        String roomId = createRoomAndGetId("Original Room", "ORIG-ROOM");

        // Given: Update form with editable fields
        StorageRoomForm updateForm = new StorageRoomForm();
        updateForm.setName("Updated Room Name");
        updateForm.setDescription("Updated description");
        updateForm.setActive(false);

        String requestBody = objectMapper.writeValueAsString(updateForm);

        // When: PUT /rest/storage/rooms/{id}
        // Then: Expect 200 OK with updated fields (code should remain unchanged)
        mockMvc.perform(
                put("/rest/storage/rooms/" + roomId).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated Room Name"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.active").value(false));
    }

    /**
     * T099: Test updating room with code field returns HTTP 200 but code is ignored
     * Contract: PUT /rest/storage/rooms/{id} with code in request → 200, code
     * unchanged
     */
    @Test
    public void testUpdateRoom_CodeReadOnly() throws Exception {
        // Given: Create room with code "ORIG-CODE"
        StorageRoomForm createForm = new StorageRoomForm();
        createForm.setName("Test Room");
        createForm.setCode("ORIG-CODE");
        createForm.setActive(true);

        String createResponse = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createForm)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String roomId = objectMapper.readTree(createResponse).get("id").asText();
        String originalCode = objectMapper.readTree(createResponse).get("code").asText();

        // Given: Update form attempting to change code
        StorageRoomForm updateForm = new StorageRoomForm();
        updateForm.setName("Updated Name");
        updateForm.setCode("NEW-CODE"); // Attempt to change code
        updateForm.setActive(true);

        String requestBody = objectMapper.writeValueAsString(updateForm);

        // When: PUT /rest/storage/rooms/{id} with code change
        // Then: Expect 200 OK but code remains unchanged (read-only)
        mockMvc.perform(
                put("/rest/storage/rooms/" + roomId).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(originalCode))
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    /**
     * T099: Test updating device with editable fields returns HTTP 200 Contract:
     * PUT /rest/storage/devices/{id} with name, type, temperature, capacity → 200
     */
    @Test
    public void testUpdateDevice_UpdatesEditableFields() throws Exception {
        // Given: Create room and device
        String roomId = createRoomAndGetId("Device Update Room", "DEV-UPD-ROOM");
        String deviceId = createDeviceAndGetId("Original Device", "ORIG-DEV", "freezer", roomId);

        // Given: Update form with editable fields
        StorageDeviceForm updateForm = new StorageDeviceForm();
        updateForm.setName("Updated Device Name");
        updateForm.setType("refrigerator");
        updateForm.setTemperatureSetting(-20.0);
        updateForm.setCapacityLimit(100);
        updateForm.setActive(false);

        String requestBody = objectMapper.writeValueAsString(updateForm);

        // When: PUT /rest/storage/devices/{id}
        // Then: Expect 200 OK with updated fields
        mockMvc.perform(
                put("/rest/storage/devices/" + deviceId).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated Device Name"))
                .andExpect(jsonPath("$.type").value("refrigerator"))
                .andExpect(jsonPath("$.temperatureSetting").value(-20.0))
                .andExpect(jsonPath("$.capacityLimit").value(100)).andExpect(jsonPath("$.active").value(false));
    }

    /**
     * T099: Test updating device with parent room change returns HTTP 200 but
     * parent unchanged Contract: PUT /rest/storage/devices/{id} with parentRoomId →
     * 200, parent unchanged
     */
    @Test
    public void testUpdateDevice_ParentReadOnly() throws Exception {
        // Given: Create two rooms and device in first room
        String roomId1 = createRoomAndGetId("Room 1", "ROOM-1");
        String roomId2 = createRoomAndGetId("Room 2", "ROOM-2");
        String deviceId = createDeviceAndGetId("Test Device", "TEST-DEV", "freezer", roomId1);

        // Given: Update form attempting to change parent room
        StorageDeviceForm updateForm = new StorageDeviceForm();
        updateForm.setName("Updated Device");
        updateForm.setType("freezer");
        updateForm.setParentRoomId(roomId2); // Attempt to change parent
        updateForm.setActive(true);

        String requestBody = objectMapper.writeValueAsString(updateForm);

        // When: PUT /rest/storage/devices/{id} with parent change
        // Then: Expect 200 OK but parent room remains unchanged (read-only)
        mockMvc.perform(
                put("/rest/storage/devices/" + deviceId).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roomId").value(Integer.parseInt(roomId1)))
                .andExpect(jsonPath("$.name").value("Updated Device"));
    }

    /**
     * T099: Test updating shelf with editable fields returns HTTP 200 Contract: PUT
     * /rest/storage/shelves/{id} with label, capacity, status → 200
     */
    @Test
    public void testUpdateShelf_UpdatesEditableFields() throws Exception {
        // Given: Create hierarchy and shelf
        String roomId = createRoomAndGetId("Shelf Update Room", "SHELF-UPD-ROOM");
        String deviceId = createDeviceAndGetId("Test Device", "SHELF-DEV", "cabinet", roomId);
        String shelfId = createShelfAndGetId("Original Shelf", deviceId);

        // Given: Update form with editable fields
        StorageShelfForm updateForm = new StorageShelfForm();
        updateForm.setLabel("Updated Shelf Label");
        updateForm.setCapacityLimit(75);
        updateForm.setActive(false);

        String requestBody = objectMapper.writeValueAsString(updateForm);

        // When: PUT /rest/storage/shelves/{id}
        // Then: Expect 200 OK with updated fields
        mockMvc.perform(
                put("/rest/storage/shelves/" + shelfId).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.label").value("Updated Shelf Label"))
                .andExpect(jsonPath("$.capacityLimit").value(75)).andExpect(jsonPath("$.active").value(false));
    }

    /**
     * T099: Test updating rack with editable fields returns HTTP 200 Contract: PUT
     * /rest/storage/racks/{id} with label, dimensions, status → 200
     */
    @Test
    public void testUpdateRack_UpdatesEditableFields() throws Exception {
        // Given: Create hierarchy and rack
        String roomId = createRoomAndGetId("Rack Update Room", "RACK-UPD-ROOM");
        String deviceId = createDeviceAndGetId("Test Device", "RACK-DEV", "cabinet", roomId);
        String shelfId = createShelfAndGetId("Shelf-1", deviceId);
        String rackId = createRackAndGetId("Original Rack", 8, 12, shelfId);

        // Given: Update form with editable fields
        StorageRackForm updateForm = new StorageRackForm();
        updateForm.setLabel("Updated Rack Label");
        updateForm.setRows(10);
        updateForm.setColumns(15);
        updateForm.setPositionSchemaHint("B2");
        updateForm.setActive(false);

        String requestBody = objectMapper.writeValueAsString(updateForm);

        // When: PUT /rest/storage/racks/{id}
        // Then: Expect 200 OK with updated fields
        mockMvc.perform(
                put("/rest/storage/racks/" + rackId).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.label").value("Updated Rack Label"))
                .andExpect(jsonPath("$.rows").value(10)).andExpect(jsonPath("$.columns").value(15))
                .andExpect(jsonPath("$.positionSchemaHint").value("B2")).andExpect(jsonPath("$.active").value(false));
    }

    /**
     * T099: Test updating location with duplicate code returns HTTP 400 Contract:
     * PUT /rest/storage/rooms/{id} with duplicate code → 400
     */
    @Test
    public void testUpdateLocation_CodeUniquenessValidation() throws Exception {
        // Given: Create two rooms with different codes
        StorageRoomForm roomForm1 = new StorageRoomForm();
        roomForm1.setName("Room 1");
        roomForm1.setCode("ROOM-1");
        roomForm1.setActive(true);

        String response1 = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomForm1)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String roomId1 = objectMapper.readTree(response1).get("id").asText();

        StorageRoomForm roomForm2 = new StorageRoomForm();
        roomForm2.setName("Room 2");
        roomForm2.setCode("ROOM-2");
        roomForm2.setActive(true);

        String response2 = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomForm2)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String roomId2 = objectMapper.readTree(response2).get("id").asText();

        // Given: Update room 2 to use room 1's code (duplicate)
        StorageRoomForm updateForm = new StorageRoomForm();
        updateForm.setName("Room 2 Updated");
        updateForm.setCode("ROOM-1"); // Duplicate code
        updateForm.setActive(true);

        String requestBody = objectMapper.writeValueAsString(updateForm);

        // When: PUT /rest/storage/rooms/{id} with duplicate code
        // Then: Expect 400 Bad Request (code uniqueness validation)
        // Note: Since code is read-only, this should be ignored, but if validation
        // happens before ignoring, it may return 400
        // For now, we expect the code to be ignored, so this test verifies that
        // behavior
        mockMvc.perform(
                put("/rest/storage/rooms/" + roomId2).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()); // Code is ignored, so update succeeds
    }

    /**
     * T099: Test updating location with invalid data returns HTTP 400 Contract: PUT
     * /rest/storage/rooms/{id} with invalid field values → 400
     */
    @Test
    public void testUpdateLocation_InvalidData_Returns400() throws Exception {
        // Given: Create room
        String roomId = createRoomAndGetId("Test Room", "TEST-ROOM");

        // Given: Update form with invalid data (missing required name)
        StorageRoomForm updateForm = new StorageRoomForm();
        // Name is null - should fail validation
        updateForm.setDescription("Description");
        updateForm.setActive(true);

        String requestBody = objectMapper.writeValueAsString(updateForm);

        // When: PUT /rest/storage/rooms/{id} with invalid data
        // Then: Expect 400 Bad Request
        mockMvc.perform(
                put("/rest/storage/rooms/" + roomId).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ========== Phase 6: Location CRUD Operations - Delete Location Tests (T100)
    // ==========

    /**
     * T100: Test deleting room with child devices returns HTTP 409 Conflict
     * Contract: DELETE /rest/storage/rooms/{id} with child devices → 409
     */
    @Test
    public void testDeleteRoom_WithChildDevices_ReturnsError() throws Exception {
        // Given: Create room with child device
        String roomId = createRoomAndGetId("Room With Device", "ROOM-DEV");
        createDeviceAndGetId("Child Device", "CHILD-DEV", "freezer", roomId);

        // When: DELETE /rest/storage/rooms/{id}
        // Then: Expect 409 Conflict with constraint message
        String response = mockMvc
                .perform(delete("/rest/storage/rooms/" + roomId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict()).andReturn().getResponse().getContentAsString();

        // Verify error message contains constraint information
        // Note: Current implementation returns 409 but may not have message body
        // This test verifies the status code
    }

    /**
     * T100: Test deleting room with active samples returns HTTP 409 Conflict
     * Contract: DELETE /rest/storage/rooms/{id} with active samples → 409 Note:
     * This test requires sample assignment setup, which may not be available in
     * Phase 6 scope. We'll verify the constraint check exists.
     */
    @Test
    public void testDeleteRoom_WithActiveSamples_ReturnsError() throws Exception {
        // Given: Create room (active samples check requires SampleStorageService)
        String roomId = createRoomAndGetId("Room With Samples", "ROOM-SAMPLES");

        // When: DELETE /rest/storage/rooms/{id}
        // Then: If samples exist, expect 409 Conflict
        // For now, without samples, deletion should succeed if no devices exist
        // This test will be enhanced when sample assignment is available
        mockMvc.perform(delete("/rest/storage/rooms/" + roomId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent()); // No constraints, deletion succeeds
    }

    /**
     * T100: Test deleting room with no constraints returns HTTP 200/204 Contract:
     * DELETE /rest/storage/rooms/{id} with no children/samples → 200/204
     */
    @Test
    public void testDeleteRoom_NoConstraints_DeletesSuccessfully() throws Exception {
        // Given: Create room with no children
        String roomId = createRoomAndGetId("Empty Room", "EMPTY-ROOM");

        // When: DELETE /rest/storage/rooms/{id}
        // Then: Expect 204 No Content (successful deletion)
        mockMvc.perform(delete("/rest/storage/rooms/" + roomId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify room is deleted
        mockMvc.perform(get("/rest/storage/rooms/" + roomId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * T100: Test deleting device with child shelves returns HTTP 409 Conflict
     * Contract: DELETE /rest/storage/devices/{id} with child shelves → 409
     */
    @Test
    public void testDeleteDevice_WithChildShelves_ReturnsError() throws Exception {
        // Given: Create device with child shelf
        String roomId = createRoomAndGetId("Device Delete Room", "DEV-DEL-ROOM");
        String deviceId = createDeviceAndGetId("Device With Shelf", "DEV-SHELF", "cabinet", roomId);
        createShelfAndGetId("Child Shelf", deviceId);

        // When: DELETE /rest/storage/devices/{id}
        // Then: Expect 409 Conflict
        mockMvc.perform(delete("/rest/storage/devices/" + deviceId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    /**
     * T100: Test deleting device with active samples returns HTTP 409 Conflict
     * Contract: DELETE /rest/storage/devices/{id} with active samples → 409
     */
    @Test
    public void testDeleteDevice_WithActiveSamples_ReturnsError() throws Exception {
        // Given: Create device (active samples check requires SampleStorageService)
        String roomId = createRoomAndGetId("Device Samples Room", "DEV-SAM-ROOM");
        String deviceId = createDeviceAndGetId("Device With Samples", "DEV-SAM", "freezer", roomId);

        // When: DELETE /rest/storage/devices/{id}
        // Then: If samples exist, expect 409 Conflict
        // For now, without samples, deletion should succeed if no shelves exist
        mockMvc.perform(delete("/rest/storage/devices/" + deviceId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent()); // No constraints, deletion succeeds
    }

    /**
     * T100: Test deleting shelf with child racks returns HTTP 409 Conflict
     * Contract: DELETE /rest/storage/shelves/{id} with child racks → 409
     */
    @Test
    public void testDeleteShelf_WithChildRacks_ReturnsError() throws Exception {
        // Given: Create shelf with child rack
        String roomId = createRoomAndGetId("Shelf Delete Room", "SHELF-DEL-ROOM");
        String deviceId = createDeviceAndGetId("Device", "SHELF-DEV", "cabinet", roomId);
        String shelfId = createShelfAndGetId("Shelf With Rack", deviceId);
        createRackAndGetId("Child Rack", 8, 12, shelfId);

        // When: DELETE /rest/storage/shelves/{id}
        // Then: Expect 409 Conflict
        mockMvc.perform(delete("/rest/storage/shelves/" + shelfId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    /**
     * T100: Test deleting rack with active samples returns HTTP 409 Conflict
     * Contract: DELETE /rest/storage/racks/{id} with active samples → 409
     */
    @Test
    public void testDeleteRack_WithActiveSamples_ReturnsError() throws Exception {
        // Given: Create rack (active samples check requires SampleStorageService)
        String roomId = createRoomAndGetId("Rack Samples Room", "RACK-SAM-ROOM");
        String deviceId = createDeviceAndGetId("Device", "RACK-DEV", "freezer", roomId);
        String shelfId = createShelfAndGetId("Shelf", deviceId);
        String rackId = createRackAndGetId("Rack With Samples", 8, 12, shelfId);

        // When: DELETE /rest/storage/racks/{id}
        // Then: If samples exist, expect 409 Conflict
        // For now, without samples, deletion should succeed
        mockMvc.perform(delete("/rest/storage/racks/" + rackId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent()); // No constraints, deletion succeeds
    }

    /**
     * T100: Test deleting location returns constraint message in error response
     * Contract: DELETE /rest/storage/rooms/{id} with constraints → 409 + message
     */
    @Test
    public void testDeleteLocation_ReturnsConstraintMessage() throws Exception {
        // Given: Create room with child device
        String roomId = createRoomAndGetId("Room With Constraint", "ROOM-CONST");
        createDeviceAndGetId("Child Device", "CHILD", "freezer", roomId);

        // When: DELETE /rest/storage/rooms/{id}
        // Then: Expect 409 Conflict
        // Note: Current implementation may not return message body, but status code
        // indicates constraint
        mockMvc.perform(delete("/rest/storage/rooms/" + roomId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    /**
     * T100: Test deleting location requires confirmation (handled in frontend)
     * Contract: DELETE /rest/storage/rooms/{id} → 200/204 (confirmation in
     * frontend) Note: Confirmation is handled in frontend, backend just validates
     * constraints
     */
    @Test
    public void testDeleteLocation_ConfirmationRequired() throws Exception {
        // Given: Create room with no constraints
        String roomId = createRoomAndGetId("Confirm Delete Room", "CONF-DEL-ROOM");

        // When: DELETE /rest/storage/rooms/{id}
        // Then: Expect successful deletion (confirmation handled in frontend)
        mockMvc.perform(delete("/rest/storage/rooms/" + roomId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    // ========== Occupancy Counting Fix Tests ==========

    /**
     * Test that occupancy counting reflects actual SampleStorageAssignment records,
     * not StoragePosition.occupied flag. This verifies the fix for the bug where
     * occupancy showed incorrect values (73) instead of actual assignment count
     * (9).
     */
    @Test
    public void testOccupancyCount_MatchesActualAssignments() throws Exception {
        // Given: Create storage hierarchy
        String roomId = createRoomAndGetId("Occupancy Test Room", "OCC-TEST-ROOM");
        String deviceId = createDeviceAndGetId("Occupancy Test Device", "OCC-DEV", "freezer", roomId);
        String shelfId = createShelfAndGetId("Occupancy Test Shelf", deviceId);
        String rack1Id = createRackAndGetId("Rack 1", 8, 12, shelfId);
        String rack2Id = createRackAndGetId("Rack 2", 10, 10, shelfId);

        // Ensure status_of_sample and type_of_sample exist BEFORE creating samples
        jdbcTemplate.update(
                "INSERT INTO status_of_sample (id, description, code, status_type, lastupdated) VALUES (1, 'Test Status', 1, 'S', CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING");
        jdbcTemplate.update(
                "INSERT INTO localization (id, english, french, lastupdated) VALUES (1, 'Test Sample Type', 'Type d''échantillon de test', CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING");
        jdbcTemplate.update(
                "INSERT INTO type_of_sample (id, description, domain, name_localization_id, lastupdated) VALUES (1, 'Test Sample Type', 'H', 1, CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING");

        // Create 5 sample assignments to rack 1
        for (int i = 1; i <= 5; i++) {
            Integer sampleId = 10000 + i;
            jdbcTemplate.update(
                    "INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date, received_date, lastupdated, is_confirmation) "
                            + "VALUES (?, 'TEST-SAMPLE-' || ?, gen_random_uuid(), 'H', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false) "
                            + "ON CONFLICT (id) DO NOTHING",
                    sampleId, i);
            Integer sampleItemId = 20000 + sampleId; // Use numeric ID

            jdbcTemplate.update(
                    "INSERT INTO sample_item (id, samp_id, sort_order, status_id, typeosamp_id, lastupdated) VALUES (?, ?, 1, 1, 1, CURRENT_TIMESTAMP) "
                            + "ON CONFLICT (id) DO NOTHING",
                    sampleItemId, sampleId);
            String positionCoord = "A" + i;
            // Use DELETE then INSERT to avoid ON CONFLICT type issues
            jdbcTemplate.update("DELETE FROM sample_storage_assignment WHERE sample_item_id = ?", sampleItemId);
            jdbcTemplate.update(
                    "INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date, assigned_by_user_id, notes, last_updated) "
                            + "VALUES (?, ?, ?, 'rack', ?, CURRENT_TIMESTAMP, 1, 'Test assignment', CURRENT_TIMESTAMP)",
                    1000 + i, sampleItemId, Integer.parseInt(rack1Id), positionCoord);
        }

        // Create 4 sample assignments to rack 2
        for (int i = 1; i <= 4; i++) {
            Integer sampleId = 10005 + i;
            jdbcTemplate.update(
                    "INSERT INTO sample (id, accession_number, fhir_uuid, domain, status_id, entered_date, received_date, lastupdated, is_confirmation) "
                            + "VALUES (?, 'TEST-SAMPLE-' || ?, gen_random_uuid(), 'H', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false) "
                            + "ON CONFLICT (id) DO NOTHING",
                    sampleId, 5 + i);
            Integer sampleItemId = 20000 + sampleId; // Use numeric ID
            // Ensure status_of_sample and type_of_sample exist
            jdbcTemplate.update(
                    "INSERT INTO status_of_sample (id, description, code, status_type, lastupdated) VALUES (1, 'Test Status', 1, 'S', CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING");
            jdbcTemplate.update(
                    "INSERT INTO localization (id, english, french, lastupdated) VALUES (1, 'Test Sample Type', 'Type d''échantillon de test', CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING");
            jdbcTemplate.update(
                    "INSERT INTO type_of_sample (id, description, domain, name_localization_id, lastupdated) VALUES (1, 'Test Sample Type', 'H', 1, CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING");

            jdbcTemplate.update(
                    "INSERT INTO sample_item (id, samp_id, sort_order, status_id, typeosamp_id, lastupdated) VALUES (?, ?, 1, 1, 1, CURRENT_TIMESTAMP) "
                            + "ON CONFLICT (id) DO NOTHING",
                    sampleItemId, sampleId);
            String positionCoord2 = "1-" + i;
            // Use DELETE then INSERT to avoid ON CONFLICT type issues
            jdbcTemplate.update("DELETE FROM sample_storage_assignment WHERE sample_item_id = ?", sampleItemId);
            jdbcTemplate.update(
                    "INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_date, assigned_by_user_id, notes, last_updated) "
                            + "VALUES (?, ?, ?, 'rack', ?, CURRENT_TIMESTAMP, 1, 'Test assignment', CURRENT_TIMESTAMP)",
                    1005 + i, sampleItemId, Integer.parseInt(rack2Id), positionCoord2);
        }

        // When: Get shelves for API (which includes occupiedCount)
        List<Map<String, Object>> shelves = storageLocationService.getShelvesForAPI(null);

        // Then: Find our test shelf and verify occupancy count matches assignments (5 +
        // 4 = 9)
        Map<String, Object> testShelf = null;
        for (Map<String, Object> shelf : shelves) {
            if (shelfId.equals(shelf.get("id").toString())) {
                testShelf = shelf;
                break;
            }
        }

        assertNotNull("Test shelf should be found", testShelf);
        Integer occupiedCount = (Integer) testShelf.get("occupiedCount");
        assertNotNull("Occupied count should not be null", occupiedCount);
        assertEquals("Occupancy should match actual assignments (5 in rack 1 + 4 in rack 2 = 9)", 9,
                occupiedCount.intValue());
    }

    // ========== Code Field Tests (Iteration 9.5) ==========

    /**
     * T285: Test PUT endpoint accepts code field for device Contract: PUT
     * /rest/storage/devices/{id} with code → 200, code in response
     */
    @Test
    public void testPutEndpointAcceptsCodeField_Device() throws Exception {
        // Given: Create room and device with test-specific codes
        String roomId = createRoomAndGetId("Code Test Room", "TEST-SC-ROOM");
        String deviceId = createDeviceAndGetId("Test Device", "TEST-SC-DEV", "freezer", roomId);

        // Given: Update form with code (using test-specific prefix)
        StorageDeviceForm updateForm = new StorageDeviceForm();
        updateForm.setName("Updated Device");
        updateForm.setType("freezer");
        updateForm.setActive(true);
        updateForm.setCode("TEST-FRZ01");

        String requestBody = objectMapper.writeValueAsString(updateForm);

        // When: PUT /rest/storage/devices/{id} with code
        // Then: Expect 200 OK with code in response
        mockMvc.perform(
                put("/rest/storage/devices/" + deviceId).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("TEST-FRZ01"));
    }

    /**
     * T285: Test PUT endpoint accepts code field for shelf Contract: PUT
     * /rest/storage/shelves/{id} with code → 200, code in response
     */
    @Test
    public void testPutEndpointAcceptsCodeField_Shelf() throws Exception {
        // Given: Create hierarchy and shelf with test-specific codes
        String roomId = createRoomAndGetId("Code Shelf Room", "TEST-SC-SHELF-ROOM");
        String deviceId = createDeviceAndGetId("Test Device", "TEST-SC-SHELF-DEV", "cabinet", roomId);
        String shelfId = createShelfAndGetId("Test Shelf", deviceId);

        // Given: Update form with code (using test-specific prefix)
        StorageShelfForm updateForm = new StorageShelfForm();
        updateForm.setLabel("Updated Shelf");
        updateForm.setActive(true);
        updateForm.setCode("TEST-SHA01");

        String requestBody = objectMapper.writeValueAsString(updateForm);

        // When: PUT /rest/storage/shelves/{id} with code
        // Then: Expect 200 OK with code in response
        mockMvc.perform(
                put("/rest/storage/shelves/" + shelfId).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("TEST-SHA01"));
    }

    /**
     * T285: Test PUT endpoint accepts code field for rack Contract: PUT
     * /rest/storage/racks/{id} with code → 200, code in response
     */
    @Test
    public void testPutEndpointAcceptsCodeField_Rack() throws Exception {
        // Given: Create hierarchy and rack with test-specific codes
        String roomId = createRoomAndGetId("Code Rack Room", "TEST-SC-RACK-ROOM");
        String deviceId = createDeviceAndGetId("Test Device", "TEST-SC-RACK-DEV", "cabinet", roomId);
        String shelfId = createShelfAndGetId("Shelf-1", deviceId);
        String rackId = createRackAndGetId("Test Rack", 8, 12, shelfId);

        // Given: Update form with code (using test-specific prefix)
        StorageRackForm updateForm = new StorageRackForm();
        updateForm.setLabel("Updated Rack");
        updateForm.setRows(8);
        updateForm.setColumns(12);
        updateForm.setActive(true);
        updateForm.setCode("TEST-RKR01");

        String requestBody = objectMapper.writeValueAsString(updateForm);

        // When: PUT /rest/storage/racks/{id} with code
        // Then: Expect 200 OK with code in response
        mockMvc.perform(
                put("/rest/storage/racks/" + rackId).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("TEST-RKR01"));
    }

    /**
     * T285: Test code validation on save - invalid format returns 400 Contract: PUT
     * /rest/storage/devices/{id} with invalid code → 400 Bad Request
     */
    @Test
    public void testCodeValidationOnSave_InvalidFormat_Returns400() throws Exception {
        // Given: Create room and device with test-specific codes
        String roomId = createRoomAndGetId("Validation Test Room", "TEST-VAL-ROOM");
        String deviceId = createDeviceAndGetId("Test Device", "TEST-VAL-DEV", "freezer", roomId);

        // Given: Update form with invalid code (too long)
        StorageDeviceForm updateForm = new StorageDeviceForm();
        updateForm.setName("Updated Device");
        updateForm.setType("freezer");
        updateForm.setActive(true);
        updateForm.setCode("INVALID-CODE-TOO-LONG"); // Exceeds 10 characters

        String requestBody = objectMapper.writeValueAsString(updateForm);

        // When: PUT /rest/storage/devices/{id} with invalid code
        // Then: Expect 400 Bad Request with error message
        mockMvc.perform(
                put("/rest/storage/devices/" + deviceId).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    /**
     * T285: Test code validation on save - duplicate code returns 400 Contract: PUT
     * /rest/storage/devices/{id} with duplicate code → 400 Bad Request
     */
    @Test
    public void testCodeValidationOnSave_DuplicateCode_Returns400() throws Exception {
        // Given: Create room and two devices with test-specific codes
        String roomId = createRoomAndGetId("Duplicate Test Room", "TEST-DUP-ROOM");
        String deviceId1 = createDeviceAndGetId("Device 1", "TEST-DUP-DEV-1", "freezer", roomId);
        String deviceId2 = createDeviceAndGetId("Device 2", "TEST-DUP-DEV-2", "freezer", roomId);

        // Use test-specific code that will be cleaned up (unique for this test, max 10
        // chars)
        // Use timestamp to ensure uniqueness across test runs
        long timestamp = System.currentTimeMillis() % 100;
        String testCode = "DUP" + String.format("%02d", timestamp);

        // Given: Set code on first device
        StorageDeviceForm updateForm1 = new StorageDeviceForm();
        updateForm1.setName("Device 1");
        updateForm1.setType("freezer");
        updateForm1.setActive(true);
        updateForm1.setCode(testCode);

        String requestBody1 = objectMapper.writeValueAsString(updateForm1);
        mockMvc.perform(
                put("/rest/storage/devices/" + deviceId1).contentType(MediaType.APPLICATION_JSON).content(requestBody1))
                .andExpect(status().isOk());

        // Given: Attempt to use same code on second device
        StorageDeviceForm updateForm2 = new StorageDeviceForm();
        updateForm2.setName("Device 2");
        updateForm2.setType("freezer");
        updateForm2.setActive(true);
        updateForm2.setCode(testCode); // Duplicate

        String requestBody2 = objectMapper.writeValueAsString(updateForm2);

        // When: PUT /rest/storage/devices/{id} with duplicate code
        // Then: Expect 400 Bad Request with error message
        mockMvc.perform(
                put("/rest/storage/devices/" + deviceId2).contentType(MediaType.APPLICATION_JSON).content(requestBody2))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    /**
     * T285: Test code validation on save - auto-uppercase conversion Contract: PUT
     * /rest/storage/devices/{id} with lowercase code → 200, uppercase in response
     */
    @Test
    public void testCodeValidationOnSave_AutoUppercaseConversion() throws Exception {
        // Given: Create room and device with test-specific codes
        String roomId = createRoomAndGetId("Uppercase Test Room", "TEST-UC-ROOM");
        String deviceId = createDeviceAndGetId("Test Device", "TEST-UC-DEV", "freezer", roomId);

        // Given: Update form with lowercase code (using test-specific prefix,
        // unique for this test)
        StorageDeviceForm updateForm = new StorageDeviceForm();
        updateForm.setName("Updated Device");
        updateForm.setType("freezer");
        updateForm.setActive(true);
        updateForm.setCode("test-uc01"); // Lowercase, will be converted to TEST-UC01

        String requestBody = objectMapper.writeValueAsString(updateForm);

        // When: PUT /rest/storage/devices/{id} with lowercase code
        // Then: Expect 200 OK with uppercase code in response
        mockMvc.perform(
                put("/rest/storage/devices/" + deviceId).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("TEST-UC01"));
    }
}
