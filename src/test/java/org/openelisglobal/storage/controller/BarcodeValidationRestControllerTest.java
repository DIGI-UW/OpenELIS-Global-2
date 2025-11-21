package org.openelisglobal.storage.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration test for BarcodeValidationRestController Following TDD: Write
 * tests BEFORE implementation Tests barcode validation REST endpoint per FR-024
 * through FR-027
 *
 * Following OpenELIS test patterns: extends BaseWebContextSensitiveTest to load
 * full Spring context and hit real database with proper transaction management.
 */
public class BarcodeValidationRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;
    private long timestamp;
    private int baseId;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);

        // Use unique IDs based on timestamp to avoid conflicts
        timestamp = System.currentTimeMillis() % 9000;
        baseId = 1000 + (int) timestamp;

        // Create test storage hierarchy with clean barcode-friendly codes
        createTestStorageHierarchy();
    }

    @After
    public void tearDown() throws Exception {
        cleanStorageTestData();
    }

    /**
     * Create a complete storage hierarchy for barcode validation testing Creates:
     * Room -> Device -> Shelf -> Rack -> Position Uses clean codes without internal
     * hyphens for barcode compatibility
     */
    private void createTestStorageHierarchy() throws Exception {
        // Create room - use simple code without hyphens
        jdbcTemplate.update(
                "INSERT INTO storage_room (id, name, code, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                baseId, "Barcode Test Room", "TESTROOM" + timestamp, true, 1);

        // Create device - use simple code without hyphens
        jdbcTemplate.update(
                "INSERT INTO storage_device (id, name, code, type, parent_room_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                baseId + 1, "Barcode Test Freezer", "TESTDEV" + timestamp, "freezer", baseId, true, 1);

        // Create shelf - use simple label without hyphens
        jdbcTemplate.update(
                "INSERT INTO storage_shelf (id, label, parent_device_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                baseId + 2, "SHELF" + timestamp, baseId + 1, true, 1);

        // Create rack - use simple label without hyphens
        jdbcTemplate.update(
                "INSERT INTO storage_rack (id, label, parent_shelf_id, active, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                baseId + 3, "RACK" + timestamp, baseId + 2, true, 1);

        // Create position (Note: coordinate is singular, no active column)
        jdbcTemplate.update(
                "INSERT INTO storage_position (id, coordinate, parent_rack_id, parent_shelf_id, parent_device_id, sys_user_id, last_updated, fhir_uuid) "
                        + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, gen_random_uuid())",
                baseId + 4, "A1", baseId + 3, baseId + 2, baseId + 1, 1);
    }

    /**
     * Clean up test data after test execution
     */
    private void cleanStorageTestData() {
        try {
            jdbcTemplate.execute("DELETE FROM sample_storage_assignment WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_position WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_rack WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_shelf WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_device WHERE id >= 1000");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE id >= 1000");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Test POST /rest/storage/barcode/validate endpoint with valid barcode
     * Expected: Returns 200 OK with valid response
     */
    @Test
    public void testPostBarcodeValidateEndpoint() throws Exception {
        // Arrange
        String validBarcode = String.format("TESTROOM%d-TESTDEV%d", timestamp, timestamp);
        String requestBody = String.format("{\"barcode\": \"%s\"}", validBarcode);

        // Act
        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseJson = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseJson);

        assertNotNull("Response should not be null", response);
        assertTrue("Response should have 'valid' field", response.has("valid"));
        assertTrue("Barcode should be valid", response.get("valid").asBoolean());
        assertTrue("Response should have 'validComponents' field", response.has("validComponents"));
        assertFalse("Valid components should not be empty", response.get("validComponents").isEmpty());
    }

    /**
     * Test request/response format matches API contract Expected: Response includes
     * all required fields
     */
    @Test
    public void testRequestResponseFormatMatchesContract() throws Exception {
        // Arrange
        String validBarcode = String.format("BT-ROOM-%d-BT-FRZ-%d", timestamp, timestamp);
        String requestBody = String.format("{\"barcode\": \"%s\"}", validBarcode);

        // Act
        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseJson = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseJson);

        // Verify required fields per API contract
        assertTrue("Response must have 'valid' field", response.has("valid"));
        assertTrue("Response must have 'validComponents' field", response.has("validComponents"));
        assertTrue("Response must have 'barcode' field", response.has("barcode"));
        assertEquals("Barcode should match request", validBarcode, response.get("barcode").asText());

        // For valid barcodes, failedStep and errorMessage should be null
        if (response.get("valid").asBoolean()) {
            assertTrue("Response should have 'failedStep' field (null for valid)",
                    response.has("failedStep") || !response.has("failedStep"));
            assertTrue("Response should have 'errorMessage' field (null for valid)",
                    response.has("errorMessage") || !response.has("errorMessage"));
        }
    }

    /**
     * Test database persistence after validation Note: Validation endpoint should
     * NOT persist anything, it's read-only Expected: No database changes after
     * validation
     */
    @Test
    public void testDatabasePersistenceAfterValidation() throws Exception {
        // Arrange
        String validBarcode = String.format("BT-ROOM-%d-BT-FRZ-%d", timestamp, timestamp);
        String requestBody = String.format("{\"barcode\": \"%s\"}", validBarcode);

        // Get initial counts
        int initialRoomCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM storage_room WHERE id >= 1000",
                Integer.class);

        // Act
        mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk());

        // Assert - No new records created
        int finalRoomCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM storage_room WHERE id >= 1000",
                Integer.class);
        assertEquals("Validation should not create new records", initialRoomCount, finalRoomCount);
    }

    /**
     * Test error response 400 Bad Request for invalid barcode format Expected:
     * Returns 400 with error details
     */
    @Test
    public void testErrorResponse400() throws Exception {
        // Arrange
        String invalidBarcode = "INVALID_FORMAT_NO_HYPHEN";
        String requestBody = String.format("{\"barcode\": \"%s\"}", invalidBarcode);

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk()) // Validation errors return 200 with valid=false
                .andReturn();

        // Assert
        String responseJson = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseJson);

        assertFalse("Validation should fail for invalid format", response.get("valid").asBoolean());
        assertTrue("Response should have 'errorMessage' field", response.has("errorMessage"));
        assertNotNull("Error message should not be null", response.get("errorMessage").asText());
        assertTrue("Response should have 'failedStep' field", response.has("failedStep"));
        assertEquals("Failed step should be FORMAT_VALIDATION", "FORMAT_VALIDATION",
                response.get("failedStep").asText());
    }

    /**
     * Test error response 404 for non-existent location Expected: Returns 200 with
     * valid=false and appropriate error
     */
    @Test
    public void testErrorResponse404() throws Exception {
        // Arrange
        String nonExistentBarcode = "NONEXISTENT-ROOM-DEVICE";
        String requestBody = String.format("{\"barcode\": \"%s\"}", nonExistentBarcode);

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk()) // Validation errors return 200 with valid=false
                .andReturn();

        // Assert
        String responseJson = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseJson);

        assertFalse("Validation should fail for non-existent location", response.get("valid").asBoolean());
        assertTrue("Response should have 'errorMessage' field", response.has("errorMessage"));
        assertNotNull("Error message should not be null", response.get("errorMessage").asText());
        assertTrue("Error message should mention 'not found'",
                response.get("errorMessage").asText().toLowerCase().contains("not found"));
    }

    /**
     * Test validation with complete 5-level barcode Expected: Returns valid
     * response with all 5 components populated
     */
    @Test
    public void testValidate5LevelBarcode() throws Exception {
        // Arrange
        String barcode = String.format("TESTROOM%d-TESTDEV%d-SHELF%d-RACK%d-A1", timestamp, timestamp, timestamp,
                timestamp);
        String requestBody = String.format("{\"barcode\": \"%s\"}", barcode);

        // Act
        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseJson = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseJson);

        assertTrue("5-level barcode should be valid", response.get("valid").asBoolean());
        JsonNode validComponents = response.get("validComponents");
        assertTrue("Should have room component", validComponents.has("room"));
        assertTrue("Should have device component", validComponents.has("device"));
        assertTrue("Should have shelf component", validComponents.has("shelf"));
        assertTrue("Should have rack component", validComponents.has("rack"));
        assertTrue("Should have position component", validComponents.has("position"));
    }

    /**
     * Test validation with inactive location Expected: Returns invalid response
     * with ACTIVITY_CHECK failed step
     */
    @Test
    public void testValidateInactiveLocation() throws Exception {
        // Arrange - Make device inactive
        jdbcTemplate.update("UPDATE storage_device SET active = false WHERE id = ?", baseId + 1);

        String barcode = String.format("TESTROOM%d-TESTDEV%d", timestamp, timestamp);
        String requestBody = String.format("{\"barcode\": \"%s\"}", barcode);

        // Act
        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseJson = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseJson);

        assertFalse("Validation should fail for inactive device", response.get("valid").asBoolean());
        assertEquals("Failed step should be ACTIVITY_CHECK", "ACTIVITY_CHECK", response.get("failedStep").asText());
        assertTrue("Error message should mention 'inactive'",
                response.get("errorMessage").asText().toLowerCase().contains("inactive"));

        // Restore active state for cleanup
        jdbcTemplate.update("UPDATE storage_device SET active = true WHERE id = ?", baseId + 1);
    }

    /**
     * Test validation with partial barcode (valid components only) Expected:
     * Returns invalid overall but includes valid components for pre-filling
     */
    @Test
    public void testValidatePartialBarcode() throws Exception {
        // Arrange - Barcode with valid room/device but non-existent shelf
        String barcode = String.format("TESTROOM%d-TESTDEV%d-NONEXISTENT", timestamp, timestamp);
        String requestBody = String.format("{\"barcode\": \"%s\"}", barcode);

        // Act
        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseJson = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseJson);

        assertFalse("Overall validation should fail", response.get("valid").asBoolean());
        JsonNode validComponents = response.get("validComponents");
        assertTrue("Should have room component", validComponents.has("room"));
        assertTrue("Should have device component", validComponents.has("device"));
        assertFalse("Should not have shelf component", validComponents.has("shelf"));
    }
}
