package org.openelisglobal.storage.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.service.LabelManagementService;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for LabelManagementRestController Following TDD approach:
 * Write tests BEFORE implementation Tests cover short code updates, label
 * generation, and print history
 */
public class LabelManagementRestControllerTest extends BaseWebContextSensitiveTest {

    private static final Logger logger = LoggerFactory.getLogger(LabelManagementRestControllerTest.class);

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LabelManagementService labelManagementService;

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Load test data that includes status_of_sample records needed by
        // BarcodeLabelMaker static initializer
        // This must be done BEFORE any code that references BarcodeLabelMaker
        executeDataSetWithStateManagement("testdata/status-of-sample.xml");

        // CRITICAL: Force StatusService to build its maps by calling getStatusID()
        // This ensures the @PostConstruct method has run and maps are populated
        // BEFORE BarcodeLabelMaker's static initializer runs (when new
        // BarcodeLabelMaker() is called)
        org.openelisglobal.common.services.IStatusService statusService = org.openelisglobal.spring.util.SpringContext
                .getBean(org.openelisglobal.common.services.IStatusService.class);
        String statusId = statusService
                .getStatusID(org.openelisglobal.common.services.StatusService.SampleStatus.Entered);
        // Verify we got a valid ID (not "-1" which means not found)
        if ("-1".equals(statusId)) {
            throw new IllegalStateException(
                    "SampleStatus.Entered not found in database. Test data may not be loaded correctly.");
        }

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanStorageTestData();
    }

    @After
    public void tearDown() throws Exception {
        cleanStorageTestData();
    }

    /**
     * Clean up storage-related test data
     */
    private void cleanStorageTestData() {
        try {
            jdbcTemplate.execute("DELETE FROM storage_location_print_history WHERE location_id::integer >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_box WHERE id::integer >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_rack WHERE id::integer >= 1000 OR label LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_shelf WHERE id::integer >= 1000 OR label LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_device WHERE id::integer >= 1000 OR code LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE id::integer >= 1000 OR code LIKE 'TEST-%'");
        } catch (Exception e) {
            logger.warn("Failed to clean storage test data: " + e.getMessage());
        }
    }

    /**
     * Helper: Create a test device and return its ID
     */
    private String createTestDevice() throws Exception {
        // Create a test room first (following pattern from other storage tests)
        long timestamp = System.currentTimeMillis() % 9000;
        Integer roomId = 1000 + (int) timestamp;
        // Room code must be ≤10 chars: "TESTROOM" + 2 digits = 9 chars
        String roomCode = "TESTROOM" + (timestamp % 100);
        jdbcTemplate.update("INSERT INTO storage_room (id, name, code, active, sys_user_id, last_updated, fhir_uuid) "
                + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid()) " + "ON CONFLICT (id) DO NOTHING",
                roomId, "Test Room", roomCode, true, 1);

        // Create device with proper room relationship (code must be ≤10 chars)
        // Use unique code per test run to avoid conflicts: "FRZ01" + 2 digits = 7 chars
        // max
        String deviceCode = "FRZ01" + String.format("%02d", timestamp % 100);
        StorageDevice device = new StorageDevice();
        device.setCode(deviceCode);
        device.setName("Test Device");
        device.setActive(true);

        Integer deviceId = 2000 + (int) timestamp;
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, 'freezer', ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid()) "
                        + "ON CONFLICT (id) DO NOTHING",
                deviceId, device.getName(), device.getCode(), roomId, device.getActive(), 1);
        // Get the actual ID (may be different if conflict occurred)
        Integer id = jdbcTemplate.queryForObject("SELECT id FROM storage_device WHERE code = ?", Integer.class,
                device.getCode());
        return String.valueOf(id);
    }

    /**
     * Helper: Create a test device with NULL code to test validation Note: Since
     * codes must always be ≤10 chars and NOT NULL, we can't create devices with
     * invalid codes. This helper creates a device with a valid code but tests can
     * manually set code to NULL via UPDATE to test validation scenarios.
     */
    private String createTestDeviceWithoutShortCode() throws Exception {
        // Create a test room first
        long timestamp = System.currentTimeMillis() % 9000;
        Integer roomId = 1000 + (int) timestamp;
        // Room code must be ≤10 chars: "TESTROOM" + 2 digits = 9 chars
        String roomCode = "TESTROOM" + (timestamp % 100);
        jdbcTemplate.update("INSERT INTO storage_room (id, name, code, active, sys_user_id, last_updated, fhir_uuid) "
                + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid()) " + "ON CONFLICT (id) DO NOTHING",
                roomId, "Test Room No SC", roomCode, true, 1);

        // Create device with empty code to test validation
        // Note: We can't set code to NULL due to NOT NULL constraint, but we can test
        // empty string
        // The validation checks for null or empty, so empty string will trigger the
        // error
        String deviceCode = ""; // Empty string to test validation
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (nextval('storage_device_seq'), ?, ?, 'freezer', ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                "Test Device No SC", deviceCode, roomId, true, 1);
        Integer id = jdbcTemplate.queryForObject(
                "SELECT id FROM storage_device WHERE name = ? AND code = ? ORDER BY id LIMIT 1", Integer.class,
                "Test Device No SC", deviceCode);

        return String.valueOf(id);
    }

    /**
     * Helper: Create a test device with code ≤10 chars (should work)
     */
    private String createTestDeviceWithCodeLeq10Chars() throws Exception {
        // Create a test room first
        long timestamp = System.currentTimeMillis() % 9000;
        Integer roomId = 1000 + (int) timestamp;
        // Room code must be ≤10 chars: "TESTROOM" + 2 digits = 9 chars
        String roomCode = "TESTROOM" + (timestamp % 100);
        jdbcTemplate.update("INSERT INTO storage_room (id, name, code, active, sys_user_id, last_updated, fhir_uuid) "
                + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid()) " + "ON CONFLICT (id) DO NOTHING",
                roomId, "Test Room Short", roomCode, true, 1);

        // Create device with code ≤10 chars (should work - code will be used for
        // labels)
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (nextval('storage_device_seq'), ?, ?, 'freezer', ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                "Test Device Short", "TEST-DEV01", roomId, true, 1);
        Integer id = jdbcTemplate.queryForObject("SELECT id FROM storage_device WHERE code = ?", Integer.class,
                "TEST-DEV01");
        return String.valueOf(id);
    }

    /**
     * T285: Test POST /rest/storage/{type}/{id}/print-label generates PDF Expected:
     * 200 OK with PDF content type (code from entity)
     */
    @Test
    public void testPostPrintLabelEndpoint_GeneratesPdf_Returns200() throws Exception {
        // Given: Test device exists with code set in entity
        String deviceId = createTestDevice();

        // Verify device was created correctly with code, parentRoom
        StorageDevice device = storageDeviceDAO.get(Integer.parseInt(deviceId)).orElse(null);
        assertNotNull("Device should exist", device);
        assertNotNull("Device should have code", device.getCode());
        assertNotNull("Device should have parentRoom", device.getParentRoom());
        assertNotNull("ParentRoom should have code", device.getParentRoom().getCode());
        assertTrue("Device code should start with FRZ01", device.getCode().startsWith("FRZ01"));

        // When: POST /rest/storage/device/{id}/print-label (uses code from entity)
        // Then: Expect 200 OK with PDF content
        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }

    /**
     * T285: Test POST /rest/storage/{type}/{id}/print-label with missing code
     * Expected: 400 Bad Request with error message
     */
    @Test
    public void testPrintValidationChecksCodeExists_MissingCode_Returns400() throws Exception {
        // Given: Test device exists with NULL code (simulating missing code scenario)
        String deviceId = createTestDeviceWithoutShortCode();

        // When: POST /rest/storage/device/{id}/print-label
        // Then: Expect 400 Bad Request with error message about missing code
        MvcResult result = mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label"))
                .andExpect(status().isBadRequest()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists()).andReturn();

        // Verify error message contains "code"
        String errorMessage = objectMapper.readTree(result.getResponse().getContentAsString()).get("error").asText();
        assertTrue("Error message should mention code", errorMessage.toLowerCase().contains("code"));
    }

    /**
     * T285: Test POST /rest/storage/{type}/{id}/print-label with code ≤10 chars
     * Expected: 200 OK with PDF (code will be used for labels)
     */
    @Test
    public void testPostPrintLabelEndpoint_CodeLeq10Chars_GeneratesPdf_Returns200() throws Exception {
        // Given: Test device exists with code ≤10 chars
        String deviceId = createTestDeviceWithCodeLeq10Chars();

        // When: POST /rest/storage/device/{id}/print-label (code ≤10 chars)
        // Then: Expect 200 OK with PDF content (code will be used for labels)
        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }

    /**
     * T285: Test error response if code is missing Expected: JSON error response
     * with specific message
     */
    @Test
    public void testErrorResponseIfCodeMissing_ReturnsJsonError() throws Exception {
        // Given: Test device exists with NULL code (simulating missing code scenario)
        String deviceId = createTestDeviceWithoutShortCode();

        // When: POST /rest/storage/device/{id}/print-label
        // Then: Expect JSON error response (not PDF)
        MvcResult result = mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label"))
                .andExpect(status().isBadRequest()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Verify error message contains "code"
        String errorMessage = objectMapper.readTree(result.getResponse().getContentAsString()).get("error").asText();
        assertTrue("Error message should mention code", errorMessage.toLowerCase().contains("code"));
    }

    /**
     * T285: Test POST /rest/storage/{type}/{id}/print-label tracks print history
     * Expected: Print history is recorded after label generation
     */
    @Test
    public void testPrintHistoryTracking_AfterLabelGeneration_Recorded() throws Exception {
        // Given: Test device exists with code
        String deviceId = createTestDevice();

        // When: POST /rest/storage/device/{id}/print-label
        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk());

        // Then: Print history record exists in database
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM storage_location_print_history WHERE location_id::integer = ? AND location_type = 'device'",
                Integer.class, Integer.parseInt(deviceId));
        assertNotNull("Print history should be recorded", count);
        assertTrue("Print history count should be > 0", count > 0);
    }

    /**
     * Test: POST /rest/storage/{type}/{id}/print-label with invalid type Expected:
     * 400 Bad Request
     */
    @Test
    public void testPostPrintLabelEndpoint_InvalidType_Returns400() throws Exception {
        // Given: Invalid location type
        String deviceId = createTestDevice();

        // When: POST /rest/storage/room/{id}/print-label (room not supported)
        // Then: Expect 400 Bad Request
        mockMvc.perform(post("/rest/storage/room/" + deviceId + "/print-label")).andExpect(status().isBadRequest());
    }

    /**
     * Test: GET /rest/storage/{type}/{id}/print-history returns history Expected:
     * 200 OK with list of print records
     */
    @Test
    public void testGetPrintHistory_ReturnsHistory_Returns200() throws Exception {
        // Given: Test device exists with print history
        String deviceId = createTestDevice();
        // Create print history record
        jdbcTemplate.update(
                "INSERT INTO storage_location_print_history (id, location_type, location_id, location_code, printed_by, printed_date, print_count) "
                        + "VALUES (gen_random_uuid(), 'device', ?, 'FRZ01', '1', CURRENT_TIMESTAMP, 1)",
                Integer.parseInt(deviceId));

        // When: GET /rest/storage/device/{id}/print-history
        // Then: Expect 200 OK with print history list
        mockMvc.perform(get("/rest/storage/device/" + deviceId + "/print-history")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * T285: Test PDF generation with code from entity Expected: PDF is generated
     * using code from entity Note: This test verifies the endpoint works, actual
     * dimension testing is in service layer
     */
    @Test
    public void testPdfGenerationWithCode_FromEntity() throws Exception {
        // Given: Test device exists with code
        String deviceId = createTestDevice();

        // Verify device has code
        StorageDevice device = storageDeviceDAO.get(Integer.parseInt(deviceId)).orElse(null);
        assertNotNull("Device should exist", device);
        assertNotNull("Device should have code", device.getCode());
        assertTrue("Device code should start with FRZ01", device.getCode().startsWith("FRZ01"));

        // When: POST /rest/storage/device/{id}/print-label
        // Then: PDF is generated using code from entity
        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }
}
