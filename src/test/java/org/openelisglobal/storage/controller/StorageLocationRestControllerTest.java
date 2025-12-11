package org.openelisglobal.storage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.service.StorageLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Controller integration tests for Storage Location CRUD endpoints Tests for
 * OGC-68: Storage Location CRUD
 */
public class StorageLocationRestControllerTest extends BaseWebContextSensitiveTest {

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
        cleanTestData();
    }

    @After
    public void tearDown() throws Exception {
        cleanTestData();
    }

    private void cleanTestData() {
        try {
            jdbcTemplate.execute("DELETE FROM storage_device WHERE id >= 20000");
            jdbcTemplate.execute("DELETE FROM storage_shelf WHERE id >= 20000");
            jdbcTemplate.execute("DELETE FROM storage_rack WHERE id >= 20000");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE id >= 20000");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    // T007: Test DELETE /rest/storage/rooms/{id} endpoint
    @Test
    public void testDeleteRoom_WithValidId_Returns204() throws Exception {
        // Arrange - Create room with no children
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, fhir_uuid, sys_user_id, last_updated) VALUES (?, ?, ?, ?, gen_random_uuid(), ?, CURRENT_TIMESTAMP)",
                20000, "Empty Room", "EMPTY-ROOM", true, 1);

        // Act & Assert
        this.mockMvc.perform(delete("/rest/storage/rooms/20000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteRoom_WithChildDevices_Returns409() throws Exception {
        // Arrange - Create room with device
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, fhir_uuid, sys_user_id, last_updated) VALUES (?, ?, ?, ?, gen_random_uuid(), ?, CURRENT_TIMESTAMP)",
                20001, "Main Lab", "MAIN-LAB", true, 1);
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, fhir_uuid, sys_user_id, last_updated) VALUES (?, ?, ?, ?, ?, ?, gen_random_uuid(), ?, CURRENT_TIMESTAMP)",
                20000, "Test Device", "TEST-DEV", "freezer", 20001, true, 1);

        // Act & Assert
        this.mockMvc.perform(delete("/rest/storage/rooms/20001").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("REFERENTIAL_INTEGRITY_VIOLATION"))
                .andExpect(jsonPath("$.message").exists()).andExpect(jsonPath("$.dependentCount").value(1));
    }

    @Test
    public void testDeleteDevice_WithValidId_Returns204() throws Exception {
        // Arrange - Create room and device with no children
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, fhir_uuid, sys_user_id, last_updated) VALUES (?, ?, ?, ?, gen_random_uuid(), ?, CURRENT_TIMESTAMP)",
                20002, "Test Room", "TEST-ROOM", true, 1);
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, fhir_uuid, sys_user_id, last_updated) VALUES (?, ?, ?, ?, ?, ?, gen_random_uuid(), ?, CURRENT_TIMESTAMP)",
                20001, "Empty Device", "EMPTY-DEV", "freezer", 20002, true, 1);

        // Act & Assert
        this.mockMvc.perform(delete("/rest/storage/devices/20001").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteShelf_WithValidId_Returns204() throws Exception {
        // Arrange - Create hierarchy: Room -> Device -> Shelf (no racks)
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, fhir_uuid, sys_user_id, last_updated) VALUES (?, ?, ?, ?, gen_random_uuid(), ?, CURRENT_TIMESTAMP)",
                20003, "Test Room", "TEST-ROOM-2", true, 1);
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, fhir_uuid, sys_user_id, last_updated) VALUES (?, ?, ?, ?, ?, ?, gen_random_uuid(), ?, CURRENT_TIMESTAMP)",
                20002, "Test Device", "TEST-DEV-2", "freezer", 20003, true, 1);
        jdbcTemplate.update(
                "INSERT INTO storage_shelf (id, label, code, parent_device_id, active, fhir_uuid, sys_user_id, last_updated) VALUES (?, ?, ?, ?, ?, gen_random_uuid(), ?, CURRENT_TIMESTAMP)",
                20000, "Shelf-A", "SHELF-A", 20002, true, 1);

        // Act & Assert
        this.mockMvc.perform(delete("/rest/storage/shelves/20000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteRack_WithValidId_Returns204() throws Exception {
        // Arrange - Create hierarchy: Room -> Device -> Shelf -> Rack (no samples)
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, fhir_uuid, sys_user_id, last_updated) VALUES (?, ?, ?, ?, gen_random_uuid(), ?, CURRENT_TIMESTAMP)",
                20004, "Test Room", "TEST-ROOM-3", true, 1);
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, fhir_uuid, sys_user_id, last_updated) VALUES (?, ?, ?, ?, ?, ?, gen_random_uuid(), ?, CURRENT_TIMESTAMP)",
                20003, "Test Device", "TEST-DEV-3", "freezer", 20004, true, 1);
        jdbcTemplate.update(
                "INSERT INTO storage_shelf (id, label, code, parent_device_id, active, fhir_uuid, sys_user_id, last_updated) VALUES (?, ?, ?, ?, ?, gen_random_uuid(), ?, CURRENT_TIMESTAMP)",
                20001, "Shelf-B", "SHELF-B", 20003, true, 1);
        jdbcTemplate.update(
                "INSERT INTO storage_rack (id, label, code, parent_shelf_id, active, fhir_uuid, sys_user_id, last_updated) VALUES (?, ?, ?, ?, ?, gen_random_uuid(), ?, CURRENT_TIMESTAMP)",
                20000, "Rack-01", "RACK-01", 20001, true, 1);

        // Act & Assert
        this.mockMvc.perform(delete("/rest/storage/racks/20000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    // T008: Test POST /rest/storage/devices with connectivity fields
    @Test
    public void testCreateDevice_WithConnectivityFields_Returns201() throws Exception {
        // Arrange - Create parent room first
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, fhir_uuid, sys_user_id, last_updated) VALUES (?, ?, ?, ?, gen_random_uuid(), ?, CURRENT_TIMESTAMP)",
                20005, "IoT Room", "IOT-ROOM", true, 1);

        String deviceJson = "{" + "\"name\":\"IoT Freezer\"," + "\"code\":\"IOT-FRZ-01\"," + "\"type\":\"freezer\","
                + "\"parentRoomId\":20005," + "\"ipAddress\":\"192.168.1.100\"," + "\"port\":502,"
                + "\"communicationProtocol\":\"BACnet\"" + "}";

        // Act & Assert
        this.mockMvc.perform(post("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON).content(deviceJson))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ipAddress").value("192.168.1.100"))
                .andExpect(jsonPath("$.port").value(502))
                .andExpect(jsonPath("$.communicationProtocol").value("BACnet"));
    }

    // T008a: Test RBAC - These tests will be added after security configuration is
    // verified
    // For now, these tests are commented out until we confirm the security setup
    // @Test
    // public void testDeleteRoom_AsNonAdminUser_Returns403() throws Exception {
    // // Will be implemented after security setup confirmed
    // }
}
