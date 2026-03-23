package org.openelisglobal.storage.dao;

import static org.junit.Assert.*;

import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DAO tests for StorageRackDAO
 * 
 * Tests all HQL queries to ensure they compile and execute correctly. This
 * catches HQL property reference errors (e.g., non-existent properties).
 * 
 * Uses BaseWebContextSensitiveTest (legacy pattern) since project doesn't use
 * Spring Boot. Reference: Testing Roadmap > BaseWebContextSensitiveTest (Legacy
 * Integration)
 */
public class StorageRackDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StorageRackDAO storageRackDAO;

    @Autowired
    private StorageShelfDAO storageShelfDAO;

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    @Autowired
    private StorageRoomDAO storageRoomDAO;

    private JdbcTemplate jdbcTemplate;

    private StorageRoom testRoom;
    private StorageDevice testDevice;
    private StorageShelf testShelf;
    private StorageRack testRack1;
    private StorageRack testRack2;
    private Integer testRoomId;
    private Integer testDeviceId;
    private Integer testShelfId;
    private Integer testRack1Id;
    private Integer testRack2Id;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanTestData();

        // Create test room
        testRoomId = 2200;
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testRoomId, "Test Room", "TSTRM-RACK", true, 1);
        testRoom = storageRoomDAO.get(testRoomId).orElse(null);

        // Create test device
        testDeviceId = 2201;
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testDeviceId, "Test Device", "TEST-DEV", "freezer", testRoomId, true, 1);
        testDevice = storageDeviceDAO.get(testDeviceId).orElse(null);

        // Create test shelf
        testShelfId = 2202;
        jdbcTemplate.update(
                "INSERT INTO storage_shelf (id, label, code, parent_device_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testShelfId, "Test Shelf", "TEST-SHELF", testDeviceId, true, 1);
        testShelf = storageShelfDAO.get(testShelfId).orElse(null);

        // Create test racks (Note: racks no longer have rows/columns - use code column)
        testRack1Id = 2203;
        jdbcTemplate.update(
                "INSERT INTO storage_rack (id, label, code, parent_shelf_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testRack1Id, "Rack 1", "RACK-1", testShelfId, true, 1);
        testRack1 = storageRackDAO.get(testRack1Id).orElse(null);

        testRack2Id = 2204;
        jdbcTemplate.update(
                "INSERT INTO storage_rack (id, label, code, parent_shelf_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testRack2Id, "Rack 2", "RACK-2", testShelfId, true, 1);
        testRack2 = storageRackDAO.get(testRack2Id).orElse(null);
    }

    @After
    public void tearDown() throws Exception {
        cleanTestData();
    }

    private void cleanTestData() {
        try {
            // Delete by code to handle leftover records from previous test runs
            // (ensures unique constraint violations don't occur)
            jdbcTemplate.execute("DELETE FROM storage_rack WHERE code IN ('RACK-1', 'RACK-2') OR id IN (2203, 2204)");
            jdbcTemplate.execute("DELETE FROM storage_shelf WHERE code = 'TEST-SHELF' OR id = 2202");
            jdbcTemplate.execute("DELETE FROM storage_device WHERE code = 'TEST-DEV' OR id = 2201");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE code = 'TSTRM-RACK' OR id = 2200");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Test: findByLabel - validates HQL query compiles and executes This test would
     * fail if HQL references non-existent properties
     */
    @Test
    public void testFindByLabel_WithValidLabel_ReturnsRack() {
        // Act: Execute HQL query - this will fail if HQL references non-existent
        // properties
        StorageRack result = storageRackDAO.findByLabel("Rack 1");

        // Assert
        assertNotNull("Rack should be found", result);
        assertEquals("Label should match", "Rack 1", result.getLabel());
        assertEquals("Short code should match", "RACK-1", result.getCode());
    }

    /**
     * Test: findByLabel - not found returns null
     */
    @Test
    public void testFindByLabel_WithInvalidLabel_ReturnsNull() {
        // Act
        StorageRack result = storageRackDAO.findByLabel("NONEXISTENT");

        // Assert
        assertNull("Rack should not be found", result);
    }

    /**
     * Test: findByParentShelfId - validates HQL query compiles and executes
     */
    @Test
    public void testFindByParentShelfId_WithValidShelf_ReturnsRacks() {
        // Act: Execute HQL query
        List<StorageRack> results = storageRackDAO.findByParentShelfId(testShelf.getId());

        // Assert
        assertEquals("Should return two racks", 2, results.size());
        assertTrue("Should contain rack 1", results.stream().anyMatch(r -> r.getLabel().equals("Rack 1")));
        assertTrue("Should contain rack 2", results.stream().anyMatch(r -> r.getLabel().equals("Rack 2")));
    }

    /**
     * Test: findByLabelAndParentShelf - validates HQL query compiles and executes
     */
    @Test
    public void testFindByLabelAndParentShelf_WithValidParams_ReturnsRack() {
        // Act: Execute HQL query
        StorageRack result = storageRackDAO.findByLabelAndParentShelf("Rack 1", testShelf);

        // Assert
        assertNotNull("Rack should be found", result);
        assertEquals("Label should match", "Rack 1", result.getLabel());
    }

    /**
     * Test: countByShelfId - validates HQL query compiles and executes
     */
    @Test
    public void testCountByShelfId_WithValidShelf_ReturnsCount() {
        // Act: Execute HQL query
        int count = storageRackDAO.countByShelfId(testShelf.getId());

        // Assert
        assertEquals("Should return count of 2", 2, count);
    }

    /**
     * Test: getAll - validates HQL query compiles and executes
     */
    @Test
    public void testGetAll_ReturnsAllRacks() {
        // Act: Execute HQL query
        List<StorageRack> results = storageRackDAO.getAll();

        // Assert
        assertTrue("Should return at least 2 racks", results.size() >= 2);
        assertTrue("Should contain rack 1", results.stream().anyMatch(r -> r.getLabel().equals("Rack 1")));
        assertTrue("Should contain rack 2", results.stream().anyMatch(r -> r.getLabel().equals("Rack 2")));
    }
}
