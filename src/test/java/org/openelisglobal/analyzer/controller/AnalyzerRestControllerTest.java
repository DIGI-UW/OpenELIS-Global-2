package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for AnalyzerRestController Following TDD approach: Write
 * tests BEFORE implementation
 * 
 * Task Reference: T035 Test Coverage Goal: >80%
 */
public class AnalyzerRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        // Clean up analyzer test data before each test
        cleanAnalyzerTestData();
    }

    /**
     * Clean up analyzer-related test data Note: Must delete in order due to foreign
     * key constraints
     */
    private void cleanAnalyzerTestData() {
        try {
            // Delete test-created analyzer data in order (respecting foreign keys)
            jdbcTemplate.execute("DELETE FROM analyzer_field_mapping WHERE id LIKE 'TEST-%'");
            jdbcTemplate.execute("DELETE FROM analyzer_field WHERE id LIKE 'TEST-%'");
            // Delete analyzer_configuration first (references analyzer)
            // Use a subquery to safely delete configurations for test analyzers
            jdbcTemplate.execute("DELETE FROM analyzer_configuration "
                    + "WHERE analyzer_id IN (SELECT id FROM analyzer WHERE name LIKE 'TEST-%')");
            // Then delete analyzer (legacy table, clean by name pattern)
            jdbcTemplate.execute("DELETE FROM analyzer WHERE name LIKE 'TEST-%'");

            // Ensure analyzer sequence is synchronized with existing data
            // This prevents ID collisions when tests run together
            // Get max ID and set sequence to maxId+1 (so nextval returns maxId+1)
            Integer maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM analyzer", Integer.class);
            // Set sequence to maxId (next nextval() will return maxId+1)
            jdbcTemplate.execute("SELECT setval('analyzer_seq', " + maxId + ", true)");
        } catch (Exception e) {
            // Log but don't fail - cleanup is best effort
            // Note: logger is package-private in BaseWebContextSensitiveTest
            System.out.println("Failed to clean analyzer test data: " + e.getMessage());
        }
    }

    /**
     * Test: GET /rest/analyzer/analyzers returns list of analyzers Task Reference:
     * T035
     */
    @Test
    public void testGetAnalyzers_ReturnsList() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    /**
     * Test: POST /rest/analyzer/analyzers creates analyzer with valid data Task
     * Reference: T035
     */
    @Test
    public void testCreateAnalyzer_WithValidData_ReturnsCreated() throws Exception {
        // Arrange: Create analyzer form JSON
        String uniqueName = "TEST-Analyzer-" + System.currentTimeMillis();
        String requestBody = "{\"name\":\"" + uniqueName
                + "\",\"analyzerType\":\"Chemistry Analyzer\",\"ipAddress\":\"192.168.1.100\","
                + "\"port\":5000,\"testUnitIds\":[]}";

        // Act & Assert: Endpoint should create analyzer
        mockMvc.perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(uniqueName));
    }

    /**
     * Test: POST /rest/analyzer/analyzers/{id}/test-connection tests connection
     * Task Reference: T035
     */
    @Test
    public void testTestConnection_WithValidConfig_ReturnsSuccess() throws Exception {
        // Arrange: Create analyzer first
        String uniqueName = "TEST-Connection-Test-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName
                + "\",\"analyzerType\":\"Chemistry Analyzer\",\"ipAddress\":\"192.168.1.100\","
                + "\"port\":5000,\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andReturn(); // Don't assert status yet - we'll check it

        int status = createResult.getResponse().getStatus();
        String responseBody = createResult.getResponse().getContentAsString();

        // Debug: Print status and response if not 201
        if (status != 201) {
            System.out.println("DEBUG: Analyzer creation failed with status: " + status);
            System.out.println("DEBUG: Response body: " + responseBody);
            System.out.println("DEBUG: Request body: " + createBody);
        }

        // Assert creation succeeded
        assertEquals("Analyzer creation should succeed", 201, status);

        // Extract analyzer ID from response (simplified parsing)
        String analyzerId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        analyzerId = analyzerId.substring(0, analyzerId.indexOf("\""));

        // Act & Assert: Test connection endpoint should work
        mockMvc.perform(post("/rest/analyzer/analyzers/" + analyzerId + "/test-connection")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /**
     * Test: GET /rest/analyzer/analyzers/{id} returns analyzer by ID Task
     * Reference: T054
     */
    @Test
    public void testGetAnalyzer_WithValidId_ReturnsAnalyzer() throws Exception {
        // Arrange: Create analyzer first
        String uniqueName = "TEST-Get-Test-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName
                + "\",\"analyzerType\":\"Chemistry Analyzer\",\"ipAddress\":\"192.168.1.100\","
                + "\"port\":5000,\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String analyzerId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        analyzerId = analyzerId.substring(0, analyzerId.indexOf("\""));

        // Act & Assert: GET endpoint should return analyzer
        mockMvc.perform(get("/rest/analyzer/analyzers/" + analyzerId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(analyzerId))
                .andExpect(jsonPath("$.name").value(uniqueName));
    }

    /**
     * Test: GET /rest/analyzer/analyzers/{id} returns 404 for non-existent analyzer
     * Task Reference: T054
     */
    @Test
    public void testGetAnalyzer_WithInvalidId_ReturnsNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/rest/analyzer/analyzers/99999").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * Test: PUT /rest/analyzer/analyzers/{id} updates analyzer Task Reference: T054
     */
    @Test
    public void testUpdateAnalyzer_WithValidData_ReturnsUpdated() throws Exception {
        // Arrange: Create analyzer first
        String uniqueName = "TEST-Update-Test-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName
                + "\",\"analyzerType\":\"Chemistry Analyzer\",\"ipAddress\":\"192.168.1.100\","
                + "\"port\":5000,\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String analyzerId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        analyzerId = analyzerId.substring(0, analyzerId.indexOf("\""));

        // Update analyzer
        String updateBody = "{\"name\":\"Updated Name\",\"analyzerType\":\"Hematology Analyzer\",\"active\":false}";

        // Act & Assert: PUT endpoint should update analyzer
        mockMvc.perform(put("/rest/analyzer/analyzers/" + analyzerId).contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(analyzerId))
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    /**
     * Test: DELETE /rest/analyzer/analyzers/{id} soft deletes analyzer Task
     * Reference: T054
     */
    @Test
    public void testDeleteAnalyzer_WithValidId_ReturnsNoContent() throws Exception {
        // Arrange: Create analyzer first
        String uniqueName = "TEST-Delete-Test-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName
                + "\",\"analyzerType\":\"Chemistry Analyzer\",\"ipAddress\":\"192.168.1.100\","
                + "\"port\":5000,\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String analyzerId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        analyzerId = analyzerId.substring(0, analyzerId.indexOf("\""));

        // Act & Assert: DELETE endpoint should soft delete analyzer
        mockMvc.perform(delete("/rest/analyzer/analyzers/" + analyzerId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify analyzer is soft deleted (active=false)
        mockMvc.perform(get("/rest/analyzer/analyzers/" + analyzerId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }
}
