package org.openelisglobal.storage.service;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for LabelManagementService that verify label generation
 * through the service layer (not REST endpoints). These tests validate: - PDF
 * generation using shortCode from entity - Validation when shortCode is missing
 * - Print history tracking
 * 
 * Following OpenELIS test patterns: extends BaseWebContextSensitiveTest to load
 * full Spring context and hit real database with proper transaction management.
 */
public class LabelManagementServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final Logger logger = LoggerFactory.getLogger(LabelManagementServiceIntegrationTest.class);

    @Autowired
    private LabelManagementService labelManagementService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        // Clean up storage tables before each test to ensure atomicity
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
            // Also clean up by short_code patterns used in tests (TEST- prefix)
            jdbcTemplate.execute(
                    "DELETE FROM storage_location_print_history WHERE location_id::integer >= 1000 OR location_id LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_position WHERE id::integer >= 1000 OR coordinate LIKE 'TEST-%'");
            jdbcTemplate.execute(
                    "DELETE FROM storage_rack WHERE id::integer >= 1000 OR label LIKE 'TEST-%' OR code LIKE 'TEST-%'");
            jdbcTemplate.execute(
                    "DELETE FROM storage_shelf WHERE id::integer >= 1000 OR label LIKE 'TEST-%' OR code LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_device WHERE id::integer >= 1000 OR code LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE id::integer >= 1000 OR code LIKE 'TEST-%'");
        } catch (Exception e) {
            // Log but don't fail - cleanup is best effort
            logger.warn("Failed to clean storage test data: " + e.getMessage());
        }
    }

    /**
     * Helper: Create a test device with shortCode and return it
     */
    private StorageDevice createTestDeviceWithShortCode() {
        // Given: Get a room from fixtures to use as parent
        StorageRoom parentRoom = storageLocationService.getRooms().get(0);

        // Given: Create device with code (using test-specific prefix, ≤10 chars)
        StorageDevice device = new StorageDevice();
        device.setCode("TEST-FRZ01"); // 9 chars
        device.setName("Test Device Label 01");
        device.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        device.setParentRoom(parentRoom);
        device.setActive(true);
        device.setSysUserIdValue(1); // Required field

        // When: Insert device through service layer
        Integer deviceId = storageLocationService.insert(device);
        assertNotNull("Device should be created", deviceId);

        // Return the device
        return (StorageDevice) storageLocationService.get(deviceId, StorageDevice.class);
    }

    /**
     * Helper: Create a test device without code (empty/null) to test validation
     * Note: Since codes are now always required and ≤10 chars, we test with
     * empty/null code
     */
    private StorageDevice createTestDeviceWithoutShortCode() {
        // Given: Get a room from fixtures to use as parent
        StorageRoom parentRoom = storageLocationService.getRooms().get(0);

        // Given: Create device with empty code (to test validation)
        StorageDevice device = new StorageDevice();
        device.setCode(""); // Empty code - should fail validation
        device.setName("Test Device No Code");
        device.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        device.setParentRoom(parentRoom);
        device.setActive(true);
        device.setSysUserIdValue(1); // Required field

        // Note: We can't actually persist this, so we'll test the validation in the
        // generateLabel test
        return device;
    }

    /**
     * Test generating label for device with shortCode through service layer
     * Expected: PDF is generated successfully using shortCode from entity
     */
    @Test
    public void testGenerateLabel_DeviceWithShortCode_GeneratesPdf() {
        // Given: Device with shortCode
        StorageDevice device = createTestDeviceWithShortCode();
        assertNotNull("Device should have shortCode", device.getCode());
        assertEquals("Device should have expected shortCode", "TEST-FRZ01", device.getCode());

        // When: Generate label through service layer
        ByteArrayOutputStream pdf = labelManagementService.generateLabel(device);

        // Then: PDF should be generated (non-empty)
        assertNotNull("PDF should not be null", pdf);
        assertTrue("PDF should have content", pdf.size() > 0);
    }

    /**
     * Test generating label for device with code ≤10 chars and no shortCode -
     * should use code Expected: PDF is generated successfully using code (since
     * code ≤10 chars)
     */
    @Test
    public void testGenerateLabel_DeviceWithCodeLeq10Chars_NoShortCode_UsesCode() {
        // Given: Get a room from fixtures to use as parent
        StorageRoom parentRoom = storageLocationService.getRooms().get(0);

        // Given: Create device with code ≤10 chars and no shortCode
        StorageDevice device = new StorageDevice();
        device.setCode("TEST-DEV01"); // 10 chars
        device.setName("Test Device 01");
        device.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        device.setParentRoom(parentRoom);
        device.setActive(true);
        device.setSysUserIdValue(1); // Required field
        // shortCode is null - should be allowed since code ≤10 chars

        // When: Insert device through service layer
        Integer deviceId = storageLocationService.insert(device);
        assertNotNull("Device should be created", deviceId);

        // Given: Retrieve device
        StorageDevice retrieved = (StorageDevice) storageLocationService.get(deviceId, StorageDevice.class);
        assertNotNull("Device should have code", retrieved.getCode());
        assertTrue("Device code should be ≤10 chars", retrieved.getCode().length() <= 10);
        assertEquals("Device code should match", "TEST-DEV01", retrieved.getCode());

        // When: Generate label through service layer (should use code since ≤10 chars)
        ByteArrayOutputStream pdf = labelManagementService.generateLabel(retrieved);

        // Then: PDF should be generated (non-empty)
        assertNotNull("PDF should not be null", pdf);
        assertTrue("PDF should have content", pdf.size() > 0);
    }

    /**
     * Test generating label for device with missing/empty code throws exception
     * Expected: IllegalArgumentException is thrown when code is missing or empty
     * Note: Since codes are now always ≤10 chars and required, we test with empty
     * code
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGenerateLabel_DeviceWithCodeGt10Chars_NoShortCode_ThrowsException() {
        // Given: Device with empty code (codes are now always required and ≤10 chars)
        StorageDevice device = createTestDeviceWithoutShortCode();
        // Verify device has empty code
        assertTrue("Device should have empty code", device.getCode() != null && device.getCode().trim().isEmpty());

        // When: Generate label through service layer
        // Then: Should throw IllegalArgumentException (code is required)
        labelManagementService.generateLabel(device);
    }

    /**
     * Test generating label for shelf with shortCode through service layer
     * Expected: PDF is generated successfully using shortCode from entity
     */
    @Test
    public void testGenerateLabel_ShelfWithShortCode_GeneratesPdf() {
        // Given: Get a device from fixtures to use as parent
        StorageDevice parentDevice = storageLocationService.getAllDevices().get(0);

        // Given: Create shelf with shortCode (using test-specific prefix)
        StorageShelf shelf = new StorageShelf();
        shelf.setLabel("TEST-SHELF-LBL01");
        shelf.setParentDevice(parentDevice);
        shelf.setCode("TEST-SHA01");
        shelf.setActive(true);
        shelf.setSysUserIdValue(1); // Required field

        // When: Insert shelf through service layer
        Integer shelfId = storageLocationService.insert(shelf);
        assertNotNull("Shelf should be created", shelfId);

        // Given: Retrieve shelf
        StorageShelf retrieved = (StorageShelf) storageLocationService.get(shelfId, StorageShelf.class);
        assertNotNull("Shelf should have shortCode", retrieved.getCode());

        // When: Generate label through service layer
        ByteArrayOutputStream pdf = labelManagementService.generateLabel(retrieved);

        // Then: PDF should be generated (non-empty)
        assertNotNull("PDF should not be null", pdf);
        assertTrue("PDF should have content", pdf.size() > 0);
    }

    /**
     * Test generating label for rack with shortCode through service layer Expected:
     * PDF is generated successfully using shortCode from entity
     */
    @Test
    public void testGenerateLabel_RackWithShortCode_GeneratesPdf() {
        // Given: Get a shelf from fixtures to use as parent
        StorageShelf parentShelf = storageLocationService.getAllShelves().get(0);

        // Given: Create rack with shortCode (using test-specific prefix)
        StorageRack rack = new StorageRack();
        rack.setLabel("TEST-RACK-LBL01");
        rack.setParentShelf(parentShelf);
        rack.setRows(8);
        rack.setColumns(12);
        rack.setCode("TEST-RKR01");
        rack.setActive(true);
        rack.setSysUserIdValue(1); // Required field

        // When: Insert rack through service layer
        Integer rackId = storageLocationService.insert(rack);
        assertNotNull("Rack should be created", rackId);

        // Given: Retrieve rack
        StorageRack retrieved = (StorageRack) storageLocationService.get(rackId, StorageRack.class);
        assertNotNull("Rack should have shortCode", retrieved.getCode());

        // When: Generate label through service layer
        ByteArrayOutputStream pdf = labelManagementService.generateLabel(retrieved);

        // Then: PDF should be generated (non-empty)
        assertNotNull("PDF should not be null", pdf);
        assertTrue("PDF should have content", pdf.size() > 0);
    }

    /**
     * Test validateCodeExists returns true when shortCode exists Expected: Returns
     * true for device with shortCode
     */
    @Test
    public void testValidateShortCodeExists_DeviceWithShortCode_ReturnsTrue() {
        // Given: Device with shortCode
        StorageDevice device = createTestDeviceWithShortCode();
        String deviceId = String.valueOf(device.getId());

        // When: Validate shortCode exists
        boolean exists = labelManagementService.validateCodeExists(deviceId, "device");

        // Then: Should return true
        assertTrue("Short code should exist", exists);
    }

    /**
     * Test validateCodeExists returns true when code ≤10 chars (even without
     * shortCode) Expected: Returns true for device with code ≤10 chars (code can be
     * used for labels)
     */
    @Test
    public void testValidateShortCodeExists_DeviceWithCodeLeq10Chars_ReturnsTrue() {
        // Given: Get a room from fixtures to use as parent
        StorageRoom parentRoom = storageLocationService.getRooms().get(0);

        // Given: Create device with code ≤10 chars and no shortCode
        StorageDevice device = new StorageDevice();
        device.setCode("TEST-DEV01"); // 10 chars
        device.setName("Test Device 01");
        device.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        device.setParentRoom(parentRoom);
        device.setActive(true);
        device.setSysUserIdValue(1); // Required field
        // shortCode is null - should be allowed since code ≤10 chars

        // When: Insert device through service layer
        Integer deviceId = storageLocationService.insert(device);
        assertNotNull("Device should be created", deviceId);

        // When: Validate shortCode exists (should return true since code ≤10 chars)
        boolean exists = labelManagementService.validateCodeExists(String.valueOf(deviceId), "device");

        // Then: Should return true (code ≤10 chars can be used for labels)
        assertTrue("Should return true when code ≤10 chars (even without shortCode)", exists);
    }

    /**
     * Test validateCodeExists returns false when device doesn't exist Expected:
     * Returns false for non-existent device ID Note: Since database requires NOT
     * NULL shortCode and service auto-generates it, we can't test a device without
     * shortCode. Instead, we test with a non-existent device ID.
     */
    @Test
    public void testValidateShortCodeExists_DeviceDoesNotExist_ReturnsFalse() {
        // Given: Non-existent device ID
        String nonExistentDeviceId = "999999";

        // When: Validate shortCode exists
        boolean exists = labelManagementService.validateCodeExists(nonExistentDeviceId, "device");

        // Then: Should return false
        assertFalse("Short code should not exist for non-existent device", exists);
    }

    /**
     * Test trackPrintHistory records print history in database Expected: Print
     * history record is created in storage_location_print_history table
     */
    @Test
    public void testTrackPrintHistory_RecordsInDatabase() {
        // Given: Device with shortCode
        StorageDevice device = createTestDeviceWithShortCode();
        String deviceId = String.valueOf(device.getId());
        String shortCode = device.getCode();
        String userId = "1"; // Test user ID

        // When: Track print history
        labelManagementService.trackPrintHistory(deviceId, "device", shortCode, userId);

        // Then: Print history record should exist in database
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM storage_location_print_history WHERE location_id::integer = ? AND location_type = 'device'",
                Integer.class, Integer.parseInt(deviceId));
        assertNotNull("Print history count should not be null", count);
        assertTrue("Print history should be recorded", count > 0);

        // Verify the record details
        String recordedCode = jdbcTemplate.queryForObject(
                "SELECT location_code FROM storage_location_print_history WHERE location_id::integer = ? AND location_type = 'device' ORDER BY printed_date DESC LIMIT 1",
                String.class, Integer.parseInt(deviceId));
        assertEquals("Code should match", shortCode, recordedCode);
    }

    /**
     * Test full workflow: generate label and track print history Expected: PDF is
     * generated and print history is recorded
     */
    @Test
    public void testGenerateLabelAndTrackHistory_FullWorkflow() {
        // Given: Device with shortCode
        StorageDevice device = createTestDeviceWithShortCode();
        String deviceId = String.valueOf(device.getId());
        String userId = "1"; // Test user ID

        // When: Generate label
        ByteArrayOutputStream pdf = labelManagementService.generateLabel(device);
        assertNotNull("PDF should be generated", pdf);
        assertTrue("PDF should have content", pdf.size() > 0);

        // When: Track print history
        labelManagementService.trackPrintHistory(deviceId, "device", device.getCode(), userId);

        // Then: Print history should be recorded
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM storage_location_print_history WHERE location_id::integer = ? AND location_type = 'device'",
                Integer.class, Integer.parseInt(deviceId));
        assertTrue("Print history should be recorded", count > 0);
    }
}
