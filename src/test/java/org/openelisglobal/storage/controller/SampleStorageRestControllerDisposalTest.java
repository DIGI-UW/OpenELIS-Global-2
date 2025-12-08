package org.openelisglobal.storage.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
 * Integration tests for SampleStorageRestController disposal endpoint (OGC-73)
 * Tests that disposal REST API correctly sets numeric status ID and returns
 * proper responses
 */
public class SampleStorageRestControllerDisposalTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Reference data (status_of_sample) is managed by Liquibase and preserved
        // by cleanRowsInCurrentConnection (reference tables are not truncated)
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanStorageTestData();
        // Ensure SampleDisposed status exists (insert if missing, e.g., if
        // status_of_sample was truncated)
        Integer disposedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM status_of_sample WHERE name = 'SampleDisposed' AND status_type = 'SAMPLE'",
                Integer.class);
        if (disposedCount == null || disposedCount.intValue() == 0) {
            jdbcTemplate.update(
                    "INSERT INTO status_of_sample (id, description, code, status_type, lastupdated, name, display_key, is_active) "
                            + "VALUES (24, 'Sample has been physically disposed', 1, 'SAMPLE', CURRENT_TIMESTAMP, 'SampleDisposed', 'status.sample.disposed', 'Y') "
                            + "ON CONFLICT (id) DO UPDATE SET name = 'SampleDisposed'");
        }
    }

    @After
    public void tearDown() throws Exception {
        cleanStorageTestData();
    }

    /**
     * Clean up storage-related test data to ensure tests don't pollute the
     * database.
     */
    private void cleanStorageTestData() {
        try {
            jdbcTemplate.execute("DELETE FROM sample_storage_movement WHERE id >= 10000");
            jdbcTemplate.execute("DELETE FROM sample_storage_assignment WHERE id >= 10000");
            jdbcTemplate.execute("DELETE FROM sample_item WHERE id >= 10000");
            jdbcTemplate.execute("DELETE FROM sample WHERE id >= 10000");
        } catch (Exception e) {
            // Ignore cleanup errors - data may not exist
        }
    }

    /**
     * Create a test sample item for disposal testing. Returns the external_id which
     * is used to identify the sample item (resolveSampleItem only accepts accession
     * numbers or external IDs, not numeric IDs).
     */
    private String createTestSampleItem() throws Exception {
        // Create a sample first
        int sampleId = 10000;
        String accessionNumber = "TD" + (System.currentTimeMillis() % 10000);
        jdbcTemplate.update(
                "INSERT INTO sample (id, accession_number, received_date, entered_date, lastupdated, sys_user_id) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)",
                sampleId, accessionNumber, 1);

        // Create sample item with external_id
        int sampleItemId = 10000;
        String externalId = "EXT-" + System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO sample_item (id, samp_id, sort_order, status_id, external_id, lastupdated) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                sampleItemId, sampleId, 1, 1, externalId);

        // Return external_id for use with resolveSampleItem
        return externalId;
    }

    /**
     * Helper to get the numeric sample_item.id from the external_id. Used for
     * database verification queries.
     */
    private int getSampleItemNumericId(String externalId) {
        return jdbcTemplate.queryForObject("SELECT id FROM sample_item WHERE external_id = ?", Integer.class,
                externalId);
    }

    @Test
    public void testDisposeSampleItem_Returns200OnSuccess() throws Exception {
        // Arrange
        String sampleItemExternalId = createTestSampleItem();
        int numericId = getSampleItemNumericId(sampleItemExternalId);
        String requestBody = "{\"sampleItemId\":\"" + sampleItemExternalId
                + "\",\"reason\":\"expired\",\"method\":\"autoclave\",\"notes\":\"Test disposal\"}";

        // Act & Assert
        MvcResult result = this.mockMvc
                .perform(post("/rest/storage/sample-items/dispose").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sampleItemId").value(String.valueOf(numericId)))
                .andExpect(jsonPath("$.status").value("disposed")).andReturn();

        // Verify status was set correctly in database
        String actualStatusId = jdbcTemplate.queryForObject("SELECT status_id FROM sample_item WHERE id = ?",
                String.class, numericId);
        assertEquals("Status ID should be set to disposed status ID (24)", "24", actualStatusId);
    }

    @Test
    public void testDisposeSampleItem_Returns400ForInvalidSample() throws Exception {
        // Arrange - use a non-existent external ID
        String requestBody = "{\"sampleItemId\":\"NONEXISTENT-99999\",\"reason\":\"expired\",\"method\":\"autoclave\"}";

        // Act & Assert
        MvcResult result = this.mockMvc.perform(
                post("/rest/storage/sample-items/dispose").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue("Error message should contain 'not found'", responseBody.contains("not found"));
    }

    @Test
    public void testDisposeSampleItem_Returns400ForAlreadyDisposed() throws Exception {
        // Arrange
        String sampleItemExternalId = createTestSampleItem();
        int numericId = getSampleItemNumericId(sampleItemExternalId);
        // Set status to disposed (ID 24)
        jdbcTemplate.update("UPDATE sample_item SET status_id = '24' WHERE id = ?", numericId);
        String requestBody = "{\"sampleItemId\":\"" + sampleItemExternalId
                + "\",\"reason\":\"expired\",\"method\":\"autoclave\"}";

        // Act & Assert
        MvcResult result = this.mockMvc.perform(
                post("/rest/storage/sample-items/dispose").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue("Error message should contain 'already disposed'", responseBody.contains("already disposed"));
    }

    @Test
    public void testDisposeSampleItem_Returns400ForMissingReason() throws Exception {
        // Arrange
        String sampleItemExternalId = createTestSampleItem();
        String requestBody = "{\"sampleItemId\":\"" + sampleItemExternalId + "\",\"method\":\"autoclave\"}";

        // Act & Assert
        MvcResult result = this.mockMvc.perform(
                post("/rest/storage/sample-items/dispose").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue("Error message should contain 'reason'", responseBody.contains("reason"));
    }

    @Test
    public void testDisposeSampleItem_Returns400ForMissingMethod() throws Exception {
        // Arrange
        String sampleItemExternalId = createTestSampleItem();
        String requestBody = "{\"sampleItemId\":\"" + sampleItemExternalId + "\",\"reason\":\"expired\"}";

        // Act & Assert
        MvcResult result = this.mockMvc.perform(
                post("/rest/storage/sample-items/dispose").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue("Error message should contain 'method'", responseBody.contains("method"));
    }

    @Test
    public void testDisposeSampleItem_ClearsStorageAssignment() throws Exception {
        // Arrange
        String sampleItemExternalId = createTestSampleItem();
        int numericId = getSampleItemNumericId(sampleItemExternalId);

        // Create a storage assignment
        jdbcTemplate.update(
                "INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, assigned_by_user_id, assigned_date, last_updated) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                10000, numericId, 10, "device", 1);

        String requestBody = "{\"sampleItemId\":\"" + sampleItemExternalId
                + "\",\"reason\":\"expired\",\"method\":\"autoclave\"}";

        // Act
        this.mockMvc.perform(
                post("/rest/storage/sample-items/dispose").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk());

        // Assert - assignment should be deleted
        Integer assignmentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sample_storage_assignment WHERE sample_item_id = ?", Integer.class, numericId);
        assertEquals("Assignment should be deleted after disposal", Integer.valueOf(0), assignmentCount);
    }

    /**
     * OGC-73: Test that disposal creates movement audit record with all required
     * fields persisted correctly. Per FR-043, FR-051g, FR-051h: System MUST record
     * audit trail with reason, method, notes, previous location, user, timestamp.
     * Per Constitution Section V (TDD): Integration tests MUST verify persistence
     * of all fields.
     */
    @Test
    public void testDisposeSampleItem_PersistsMovementAuditRecord() throws Exception {
        // Arrange
        String sampleItemExternalId = createTestSampleItem();
        int numericId = getSampleItemNumericId(sampleItemExternalId);
        Integer previousLocationId = 10;
        String previousLocationType = "device";
        String previousPositionCoordinate = "A5";
        String disposalReason = "expired";
        String disposalMethod = "autoclave";
        String disposalNotes = "Sample expired after 6 months storage";

        // Create a storage assignment (required to have previous location for movement
        // record)
        jdbcTemplate.update(
                "INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, position_coordinate, assigned_by_user_id, assigned_date, last_updated) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                10000, numericId, previousLocationId, previousLocationType, previousPositionCoordinate, 1);

        String requestBody = String.format(
                "{\"sampleItemId\":\"%s\",\"reason\":\"%s\",\"method\":\"%s\",\"notes\":\"%s\"}", sampleItemExternalId,
                disposalReason, disposalMethod, disposalNotes);

        // Act
        this.mockMvc
                .perform(post("/rest/storage/sample-items/dispose").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sampleItemId").value(String.valueOf(numericId)))
                .andExpect(jsonPath("$.status").value("disposed")).andExpect(jsonPath("$.reason").value(disposalReason))
                .andExpect(jsonPath("$.method").value(disposalMethod));

        // Assert - Verify movement record was created in database
        Integer movementCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sample_storage_movement WHERE sample_item_id = ?", Integer.class, numericId);
        assertEquals("Movement record should be created for disposal", Integer.valueOf(1), movementCount);

        // Assert - Verify all movement record fields are persisted correctly
        // Previous location fields
        Integer actualPreviousLocationId = jdbcTemplate.queryForObject(
                "SELECT previous_location_id FROM sample_storage_movement WHERE sample_item_id = ?", Integer.class,
                numericId);
        assertEquals("Previous location ID should be recorded", previousLocationId, actualPreviousLocationId);

        String actualPreviousLocationType = jdbcTemplate.queryForObject(
                "SELECT previous_location_type FROM sample_storage_movement WHERE sample_item_id = ?", String.class,
                numericId);
        assertEquals("Previous location type should be recorded", previousLocationType, actualPreviousLocationType);

        String actualPreviousPositionCoordinate = jdbcTemplate.queryForObject(
                "SELECT previous_position_coordinate FROM sample_storage_movement WHERE sample_item_id = ?",
                String.class, numericId);
        assertEquals("Previous position coordinate should be recorded", previousPositionCoordinate,
                actualPreviousPositionCoordinate);

        // New location fields (should be NULL for disposal)
        Integer actualNewLocationId = jdbcTemplate.queryForObject(
                "SELECT new_location_id FROM sample_storage_movement WHERE sample_item_id = ?", Integer.class,
                numericId);
        assertNull("New location ID should be NULL for disposal", actualNewLocationId);

        String actualNewLocationType = jdbcTemplate.queryForObject(
                "SELECT new_location_type FROM sample_storage_movement WHERE sample_item_id = ?", String.class,
                numericId);
        assertNull("New location type should be NULL for disposal", actualNewLocationType);

        // User and timestamp fields
        Integer actualMovedByUserId = jdbcTemplate.queryForObject(
                "SELECT moved_by_user_id FROM sample_storage_movement WHERE sample_item_id = ?", Integer.class,
                numericId);
        assertEquals("Moved by user ID should be set (default system user)", Integer.valueOf(1), actualMovedByUserId);

        java.sql.Timestamp actualMovementDate = jdbcTemplate.queryForObject(
                "SELECT movement_date FROM sample_storage_movement WHERE sample_item_id = ?", java.sql.Timestamp.class,
                numericId);
        assertNotNull("Movement date should be set", actualMovementDate);
        // Verify movement date is recent (within last 5 seconds)
        long timeDiff = Math.abs(System.currentTimeMillis() - actualMovementDate.getTime());
        assertTrue("Movement date should be recent (within 5 seconds)", timeDiff < 5000);

        // Reason field - should contain formatted disposal information
        String actualReason = jdbcTemplate.queryForObject(
                "SELECT reason FROM sample_storage_movement WHERE sample_item_id = ?", String.class, numericId);
        assertNotNull("Reason should not be null", actualReason);
        assertTrue("Reason should contain disposal reason", actualReason.contains("Disposal: " + disposalReason));
        assertTrue("Reason should contain disposal method", actualReason.contains("Method: " + disposalMethod));
        assertTrue("Reason should contain disposal notes", actualReason.contains("Notes: " + disposalNotes));
        // Verify exact format: "Disposal: {reason} | Method: {method} | Notes: {notes}"
        String expectedReasonFormat = "Disposal: " + disposalReason + " | Method: " + disposalMethod + " | Notes: "
                + disposalNotes;
        assertEquals("Reason should match expected format", expectedReasonFormat, actualReason);
    }

    /**
     * OGC-73: Test that disposal creates movement audit record even when notes are
     * null. Per FR-051h: Notes field is optional.
     */
    @Test
    public void testDisposeSampleItem_PersistsMovementRecordWithoutNotes() throws Exception {
        // Arrange
        String sampleItemExternalId = createTestSampleItem();
        int numericId = getSampleItemNumericId(sampleItemExternalId);
        Integer previousLocationId = 10;
        String previousLocationType = "device";
        String disposalReason = "contaminated";
        String disposalMethod = "incineration";
        // Notes is null/omitted

        // Create a storage assignment
        jdbcTemplate.update(
                "INSERT INTO sample_storage_assignment (id, sample_item_id, location_id, location_type, assigned_by_user_id, assigned_date, last_updated) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                10000, numericId, previousLocationId, previousLocationType, 1);

        String requestBody = String.format("{\"sampleItemId\":\"%s\",\"reason\":\"%s\",\"method\":\"%s\"}",
                sampleItemExternalId, disposalReason, disposalMethod);

        // Act
        this.mockMvc.perform(
                post("/rest/storage/sample-items/dispose").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk());

        // Assert - Verify movement record was created
        Integer movementCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sample_storage_movement WHERE sample_item_id = ?", Integer.class, numericId);
        assertEquals("Movement record should be created for disposal", Integer.valueOf(1), movementCount);

        // Assert - Verify reason format when notes are null (should not include " |
        // Notes: ")
        String actualReason = jdbcTemplate.queryForObject(
                "SELECT reason FROM sample_storage_movement WHERE sample_item_id = ?", String.class, numericId);
        assertNotNull("Reason should not be null", actualReason);
        assertTrue("Reason should contain disposal reason", actualReason.contains("Disposal: " + disposalReason));
        assertTrue("Reason should contain disposal method", actualReason.contains("Method: " + disposalMethod));
        // When notes is null, format should be: "Disposal: {reason} | Method: {method}"
        String expectedReasonFormat = "Disposal: " + disposalReason + " | Method: " + disposalMethod;
        assertEquals("Reason should match expected format (without notes)", expectedReasonFormat, actualReason);
    }
}
