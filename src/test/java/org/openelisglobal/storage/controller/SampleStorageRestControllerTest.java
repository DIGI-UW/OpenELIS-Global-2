package org.openelisglobal.storage.controller;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.service.SampleStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests for SampleStorageRestController - Sample Assignment
 * Following TDD: Write tests BEFORE implementation Tests based on
 * contracts/storage-api.json
 */
public class SampleStorageRestControllerTest extends BaseWebContextSensitiveTest {

    private static final Logger logger = LoggerFactory.getLogger(SampleStorageRestControllerTest.class);

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        // Clean up test-created data before each test
        cleanStorageTestData();
    }

    @After
    public void tearDown() throws Exception {
        // Clean up any test data created during this test
        cleanStorageTestData();
    }

    /**
     * Clean up storage-related test data to ensure tests don't pollute the
     * database. Preserves fixture data (IDs 1-999) but deletes test-created data.
     */
    private void cleanStorageTestData() {
        try {
            // Delete test-created data (IDs >= 1000 or codes/names starting with TEST-)
            // Preserves fixture data (IDs 1-999)
            jdbcTemplate.execute("DELETE FROM sample_storage_movement WHERE id::integer >= 1000 OR id LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM sample_storage_assignment WHERE id::integer >= 1000 OR id LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_position WHERE id::integer >= 1000 OR coordinate LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_rack WHERE id::integer >= 1000 OR label LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_shelf WHERE id::integer >= 1000 OR label LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_device WHERE id::integer >= 1000 OR code LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM storage_room WHERE id::integer >= 1000 OR code LIKE 'TEST-%'");
        } catch (Exception e) {
            logger.warn("Failed to clean storage test data: " + e.getMessage());
        }
    }

    // OLD TESTS REMOVED: These tested deprecated position-based assignment
    // New flexible assignment tests are in
    // SampleStorageRestControllerFlexibleAssignmentTest.java

    // Helper method to create a sample and get its ID
    private String createSampleAndGetId() {
        // Insert a sample directly via JDBC to ensure it exists
        // Use a numeric ID starting from 10000 to avoid conflicts with fixtures
        Long timestamp = System.currentTimeMillis();
        String sampleId = String.valueOf(10000 + (timestamp % 9000)); // Use last 4 digits of timestamp
        String accessionNumber = "ACC-" + timestamp;
        jdbcTemplate.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                Integer.parseInt(sampleId), accessionNumber);
        return sampleId;
    }

    // Helper methods to create entities and get their IDs
    private String createRoomAndGetId(String name, String code) throws Exception {
        org.openelisglobal.storage.form.StorageRoomForm form = new org.openelisglobal.storage.form.StorageRoomForm();
        form.setName(name);
        form.setCode(code);
        form.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asInt() + "";
    }

    private String createDeviceAndGetId(String name, String code, String type, String roomId) throws Exception {
        org.openelisglobal.storage.form.StorageDeviceForm form = new org.openelisglobal.storage.form.StorageDeviceForm();
        form.setName(name);
        form.setCode(code);
        form.setType(type);
        form.setParentRoomId(roomId);
        form.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/devices").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asInt() + "";
    }

    private String createShelfAndGetId(String label, String deviceId) throws Exception {
        org.openelisglobal.storage.form.StorageShelfForm form = new org.openelisglobal.storage.form.StorageShelfForm();
        form.setLabel(label);
        form.setParentDeviceId(deviceId);
        form.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/shelves").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asInt() + "";
    }

    private String createRackAndGetId(String label, int rows, int columns, String shelfId) throws Exception {
        org.openelisglobal.storage.form.StorageRackForm form = new org.openelisglobal.storage.form.StorageRackForm();
        form.setLabel(label);
        form.setRows(rows);
        form.setColumns(columns);
        form.setParentShelfId(shelfId);
        form.setActive(true);

        String response = mockMvc
                .perform(post("/rest/storage/racks").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asInt() + "";
    }

    /**
     * Placeholder test to prevent initialization error. All actual tests are in
     * SampleStorageRestControllerFlexibleAssignmentTest.java
     */
    @org.junit.Test
    public void testPlaceholder() {
        // This test file is kept for reference but all tests moved to
        // SampleStorageRestControllerFlexibleAssignmentTest.java
        assertTrue(true);
    }

}
