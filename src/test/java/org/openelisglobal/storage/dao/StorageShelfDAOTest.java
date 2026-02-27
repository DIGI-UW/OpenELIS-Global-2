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
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DAO tests for StorageShelfDAO
 * 
 * Tests all HQL queries to ensure they compile and execute correctly. This
 * catches HQL property reference errors (e.g., non-existent properties).
 * 
 * Uses BaseWebContextSensitiveTest (legacy pattern) since project doesn't use
 * Spring Boot. Reference: Testing Roadmap > BaseWebContextSensitiveTest (Legacy
 * Integration)
 */
public class StorageShelfDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StorageShelfDAO storageShelfDAO;

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    @Autowired
    private StorageRoomDAO storageRoomDAO;

    private JdbcTemplate jdbcTemplate;

    private StorageRoom testRoom;
    private StorageDevice testDevice;
    private StorageShelf testShelf1;
    private StorageShelf testShelf2;
    private Integer testRoomId;
    private Integer testDeviceId;
    private Integer testShelf1Id;
    private Integer testShelf2Id;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanTestData();

        // Create test room - use high IDs (9100+) to avoid conflicts with other tests
        testRoomId = 9100;
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testRoomId, "Test Room", "TSTRM-SHF", true, 1);
        testRoom = storageRoomDAO.get(testRoomId).orElse(null);

        // Create test device
        testDeviceId = 9101;
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testDeviceId, "Test Device", "TSTDEV-SHF", "freezer", testRoomId, true, 1);
        testDevice = storageDeviceDAO.get(testDeviceId).orElse(null);

        // Create test shelves
        testShelf1Id = 9102;
        jdbcTemplate.update(
                "INSERT INTO storage_shelf (id, label, code, parent_device_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testShelf1Id, "Shelf A", "SHELF-A", testDeviceId, true, 1);
        testShelf1 = storageShelfDAO.get(testShelf1Id).orElse(null);

        testShelf2Id = 9103;
        jdbcTemplate.update(
                "INSERT INTO storage_shelf (id, label, code, parent_device_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                testShelf2Id, "Shelf B", "SHELF-B", testDeviceId, true, 1);
        testShelf2 = storageShelfDAO.get(testShelf2Id).orElse(null);
    }

    @After
    public void tearDown() throws Exception {
        cleanTestData();
    }

    private void cleanTestData() {
        try {
            // Delete in FK order: shelf -> device -> room
            // Use both code and id to handle leftover records from previous test runs
            jdbcTemplate
                    .execute("DELETE FROM storage_shelf WHERE code IN ('SHELF-A', 'SHELF-B') OR id IN (9102, 9103)");
            jdbcTemplate.execute("DELETE FROM storage_device WHERE code = 'TSTDEV-SHF' OR id = 9101");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE code = 'TSTRM-SHF' OR id = 9100");
        } catch (Exception e) {
            // Ignore cleanup errors - next test run will retry
        }
    }

    /**
     * Test: findByLabel - validates HQL query compiles and executes This test would
     * fail if HQL references non-existent properties
     */
    @Test
    public void testFindByLabel_WithValidLabel_ReturnsShelf() {
        // Act: Execute HQL query - this will fail if HQL references non-existent
        // properties
        StorageShelf result = storageShelfDAO.findByLabel("Shelf A");

        // Assert
        assertNotNull("Shelf should be found", result);
        assertEquals("Label should match", "Shelf A", result.getLabel());
        assertEquals("Code should match", "SHELF-A", result.getCode());
    }

    /**
     * Test: findByLabel - not found returns null
     */
    @Test
    public void testFindByLabel_WithInvalidLabel_ReturnsNull() {
        // Act
        StorageShelf result = storageShelfDAO.findByLabel("NONEXISTENT");

        // Assert
        assertNull("Shelf should not be found", result);
    }

    /**
     * Test: findByParentDeviceId - validates HQL query compiles and executes
     */
    @Test
    public void testFindByParentDeviceId_WithValidDevice_ReturnsShelves() {
        // Act: Execute HQL query
        List<StorageShelf> results = storageShelfDAO.findByParentDeviceId(testDevice.getId());

        // Assert
        assertEquals("Should return two shelves", 2, results.size());
        assertTrue("Should contain shelf 1", results.stream().anyMatch(s -> s.getLabel().equals("Shelf A")));
        assertTrue("Should contain shelf 2", results.stream().anyMatch(s -> s.getLabel().equals("Shelf B")));
    }

    /**
     * Test: findByLabelAndParentDevice - validates HQL query compiles and executes
     */
    @Test
    public void testFindByLabelAndParentDevice_WithValidParams_ReturnsShelf() {
        // Act: Execute HQL query
        StorageShelf result = storageShelfDAO.findByLabelAndParentDevice("Shelf A", testDevice);

        // Assert
        assertNotNull("Shelf should be found", result);
        assertEquals("Label should match", "Shelf A", result.getLabel());
    }

    /**
     * Test: countByDeviceId - validates HQL query compiles and executes
     */
    @Test
    public void testCountByDeviceId_WithValidDevice_ReturnsCount() {
        // Act: Execute HQL query
        int count = storageShelfDAO.countByDeviceId(testDevice.getId());

        // Assert
        assertEquals("Should return count of 2", 2, count);
    }

    /**
     * Test: getAll - validates HQL query compiles and executes
     */
    @Test
    public void testGetAll_ReturnsAllShelves() {
        // Act: Execute HQL query
        List<StorageShelf> results = storageShelfDAO.getAll();

        // Assert
        assertTrue("Should return at least 2 shelves", results.size() >= 2);
        assertTrue("Should contain shelf 1", results.stream().anyMatch(s -> s.getLabel().equals("Shelf A")));
        assertTrue("Should contain shelf 2", results.stream().anyMatch(s -> s.getLabel().equals("Shelf B")));
    }
}
