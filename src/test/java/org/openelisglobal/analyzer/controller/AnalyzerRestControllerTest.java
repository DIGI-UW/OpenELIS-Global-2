package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
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

    @Mock
    private AnalyzerQueryService analyzerQueryService;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        // Get controller from application context and inject mock service
        AnalyzerRestController controller = webApplicationContext.getBean(AnalyzerRestController.class);
        ReflectionTestUtils.setField(controller, "analyzerQueryService", analyzerQueryService);
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

        // Act & Assert: Test connection endpoint should work and return expected fields
        // Note: success will be false since there's no actual analyzer running
        mockMvc.perform(post("/rest/analyzer/analyzers/" + analyzerId + "/test-connection")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists()).andExpect(jsonPath("$.analyzerId").value(analyzerId))
                .andExpect(jsonPath("$.ipAddress").value("192.168.1.100")).andExpect(jsonPath("$.port").value(5000));
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
     * Test: POST /rest/analyzer/analyzers/{id}/delete soft deletes analyzer Task
     * Reference: T054
     *
     * Note: Uses POST /delete endpoint (not HTTP DELETE) — the duplicate
     * 
     * @DeleteMapping was removed per PR review. The POST endpoint has 90-day soft
     *                delete logic which is the canonical behavior used by the UI.
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

        // Act & Assert: POST delete endpoint should soft delete analyzer
        mockMvc.perform(
                post("/rest/analyzer/analyzers/" + analyzerId + "/delete").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    /**
     * Test: POST /rest/analyzer/analyzers/{id}/query starts query job
     * 
     * This test verifies the controller endpoint correctly delegates to
     * AnalyzerQueryService and returns the expected HTTP response.
     * 
     * Task Reference: T106 - Query endpoint verification
     * 
     * Note: This test mocks AnalyzerQueryService to avoid real TCP connections.
     * Real end-to-end testing with the mock server should be done in Cypress E2E
     * tests.
     */
    @Test
    public void testQueryAnalyzer_StartsQueryJob() throws Exception {
        // Arrange: Create analyzer first
        String uniqueName = "TEST-Query-Test-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName
                + "\",\"analyzerType\":\"Hematology Analyzer\",\"ipAddress\":\"172.20.1.100\","
                + "\"port\":5000,\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String analyzerId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        analyzerId = analyzerId.substring(0, analyzerId.indexOf("\""));

        // Mock service to return job ID
        String mockJobId = "test-job-id-123";
        when(analyzerQueryService.startQuery(analyzerId)).thenReturn(mockJobId);

        // Act & Assert: POST query endpoint should return 202 Accepted with job ID
        mockMvc.perform(
                post("/rest/analyzer/analyzers/" + analyzerId + "/query").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.jobId").value(mockJobId))
                .andExpect(jsonPath("$.analyzerId").value(analyzerId)).andExpect(jsonPath("$.status").value("started"));
    }

    /**
     * Test: GET /rest/analyzer/analyzers/{id}/query/{jobId}/status returns query
     * status
     * 
     * This test verifies the controller endpoint correctly delegates to
     * AnalyzerQueryService and returns the expected HTTP response.
     * 
     * Task Reference: T106 - Query status endpoint verification
     * 
     * Note: This test mocks AnalyzerQueryService to avoid real TCP connections.
     * Real end-to-end testing with the mock server should be done in Cypress E2E
     * tests.
     */
    @Test
    public void testGetQueryStatus_ReturnsStatus() throws Exception {
        // Arrange: Create analyzer first
        String uniqueName = "TEST-Query-Status-Test-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName
                + "\",\"analyzerType\":\"Hematology Analyzer\",\"ipAddress\":\"172.20.1.100\","
                + "\"port\":5000,\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String analyzerId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        analyzerId = analyzerId.substring(0, analyzerId.indexOf("\""));

        String jobId = "test-job-id-456";

        // Mock service to return status
        Map<String, Object> mockStatus = new HashMap<>();
        mockStatus.put("analyzerId", analyzerId);
        mockStatus.put("jobId", jobId);
        mockStatus.put("state", "completed");
        mockStatus.put("progress", 100);
        mockStatus.put("createdAt", System.currentTimeMillis());

        when(analyzerQueryService.getStatus(analyzerId, jobId)).thenReturn(mockStatus);

        // Act & Assert: GET status endpoint should return 200 OK with status
        mockMvc.perform(get("/rest/analyzer/analyzers/" + analyzerId + "/query/" + jobId + "/status")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.analyzerId").value(analyzerId)).andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.state").value("completed")).andExpect(jsonPath("$.progress").value(100));
    }

    /**
     * Test: GET /rest/analyzer/analyzers/{id}/query/{jobId}/status returns 404 for
     * non-existent job
     * 
     * Task Reference: T106 - Query status endpoint error handling
     */
    @Test
    public void testGetQueryStatus_WithInvalidJobId_ReturnsNotFound() throws Exception {
        // Arrange: Create analyzer first
        String uniqueName = "TEST-Query-Status-NotFound-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName
                + "\",\"analyzerType\":\"Hematology Analyzer\",\"ipAddress\":\"172.20.1.100\","
                + "\"port\":5000,\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String analyzerId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        analyzerId = analyzerId.substring(0, analyzerId.indexOf("\""));

        String invalidJobId = "invalid-job-id";

        // Mock service to return null (job not found)
        when(analyzerQueryService.getStatus(analyzerId, invalidJobId)).thenReturn(null);

        // Act & Assert: GET status endpoint should return 404 Not Found
        mockMvc.perform(get("/rest/analyzer/analyzers/" + analyzerId + "/query/" + invalidJobId + "/status")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
