package org.openelisglobal.storage.dao;

import static org.junit.Assert.*;

import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DAO tests for StorageDeviceDAO
 * 
 * Tests all HQL queries to ensure they compile and execute correctly. This
 * catches HQL property reference errors (e.g., non-existent properties).
 * 
 * Uses BaseWebContextSensitiveTest (legacy pattern) since project doesn't use
 * Spring Boot. Reference: Testing Roadmap > BaseWebContextSensitiveTest (Legacy
 * Integration)
 */
public class StorageDeviceDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    @Autowired
    private StorageRoomDAO storageRoomDAO;

    private JdbcTemplate jdbcTemplate;

    private StorageRoom testRoom;
    private StorageDevice testDevice1;
    private StorageDevice testDevice2;
    private Integer testRoomId;
    private Integer testDevice1Id;
    private Integer testDevice2Id;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanTestData();

        // Create test room
        testRoomId = 2000;
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testRoomId, "Test Room", "TSTRM-DEV", true, 1);
        testRoom = storageRoomDAO.get(testRoomId).orElse(null);

        // Create test devices
        testDevice1Id = 2001;
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testDevice1Id, "Test Device 1", "TEST-DEV1", "freezer", testRoomId, true, 1);
        testDevice1 = storageDeviceDAO.get(testDevice1Id).orElse(null);

        testDevice2Id = 2002;
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testDevice2Id, "Test Device 2", "TEST-DEV2", "refrigerator", testRoomId, true, 1);
        testDevice2 = storageDeviceDAO.get(testDevice2Id).orElse(null);
    }

    @After
    public void tearDown() throws Exception {
        cleanTestData();
    }

    private void cleanTestData() {
        try {
            // Delete by code to handle leftover records from previous test runs
            // (ensures unique constraint violations don't occur)
            jdbcTemplate.execute(
                    "DELETE FROM storage_device WHERE code IN ('TEST-DEV1', 'TEST-DEV2') OR id IN (2001, 2002)");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE code = 'TSTRM-DEV' OR id = 2000");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Test: findByCode - validates HQL query compiles and executes This test would
     * fail if HQL references non-existent properties (e.g., shortCode)
     */
    @Test
    public void testFindByCode_WithValidCode_ReturnsDevice() {
        // Act: Execute HQL query - this will fail if HQL references non-existent
        // properties
        StorageDevice result = storageDeviceDAO.findByCode("TEST-DEV1");

        // Assert
        assertNotNull("Device should be found", result);
        assertEquals("Code should match", "TEST-DEV1", result.getCode());
        assertEquals("Name should match", "Test Device 1", result.getName());
    }

    /**
     * Test: findByCode - not found returns null
     */
    @Test
    public void testFindByCode_WithInvalidCode_ReturnsNull() {
        // Act
        StorageDevice result = storageDeviceDAO.findByCode("NONEXISTENT");

        // Assert
        assertNull("Device should not be found", result);
    }

    /**
     * Test: findByParentRoomId - validates HQL query compiles and executes
     */
    @Test
    public void testFindByParentRoomId_WithValidRoom_ReturnsDevices() {
        // Act: Execute HQL query
        List<StorageDevice> results = storageDeviceDAO.findByParentRoomId(testRoom.getId());

        // Assert
        assertEquals("Should return two devices", 2, results.size());
        assertTrue("Should contain device 1", results.stream().anyMatch(d -> d.getCode().equals("TEST-DEV1")));
        assertTrue("Should contain device 2", results.stream().anyMatch(d -> d.getCode().equals("TEST-DEV2")));
    }

    /**
     * Test: findByParentRoomIdAndCode - validates HQL query compiles and executes
     */
    @Test
    public void testFindByParentRoomIdAndCode_WithValidParams_ReturnsDevice() {
        // Act: Execute HQL query
        StorageDevice result = storageDeviceDAO.findByParentRoomIdAndCode(testRoom.getId(), "TEST-DEV1");

        // Assert
        assertNotNull("Device should be found", result);
        assertEquals("Code should match", "TEST-DEV1", result.getCode());
    }

    /**
     * Test: findByCodeAndParentRoom - validates HQL query compiles and executes
     */
    @Test
    public void testFindByCodeAndParentRoom_WithValidParams_ReturnsDevice() {
        // Act: Execute HQL query
        StorageDevice result = storageDeviceDAO.findByCodeAndParentRoom("TEST-DEV1", testRoom);

        // Assert
        assertNotNull("Device should be found", result);
        assertEquals("Code should match", "TEST-DEV1", result.getCode());
    }

    /**
     * Test: countByRoomId - validates HQL query compiles and executes
     */
    @Test
    public void testCountByRoomId_WithValidRoom_ReturnsCount() {
        // Act: Execute HQL query
        int count = storageDeviceDAO.countByRoomId(testRoom.getId());

        // Assert
        assertEquals("Should return count of 2", 2, count);
    }

    /**
     * Test: getAll - validates HQL query compiles and executes
     */
    @Test
    public void testGetAll_ReturnsAllDevices() {
        // Act: Execute HQL query
        List<StorageDevice> results = storageDeviceDAO.getAll();

        // Assert
        assertTrue("Should return at least 2 devices", results.size() >= 2);
        assertTrue("Should contain device 1", results.stream().anyMatch(d -> d.getCode().equals("TEST-DEV1")));
        assertTrue("Should contain device 2", results.stream().anyMatch(d -> d.getCode().equals("TEST-DEV2")));
    }
}
