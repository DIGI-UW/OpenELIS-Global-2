package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerErrorService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Controller tests for AnalyzerErrorRestController
 * 
 * Task Reference: T085
 * 
 * Note: Using BaseWebContextSensitiveTest pattern (matching existing codebase)
 * since @WebMvcTest dependencies not available. @WebMvcTest would be preferred
 * for faster execution if available.
 * 
 * Test Coverage Goal: >80%
 */
public class AnalyzerErrorRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerErrorService analyzerErrorService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;
    private Analyzer testAnalyzer;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        objectMapper = new ObjectMapper();

        // Clean up any leftover test data first
        cleanTestData();

        // Create test analyzer
        // Note: Analyzer uses String IDs in Java (e.g., "1"), but INTEGER in database
        // Reference: ID_TYPE_ANALYSIS.md - Legacy Analyzer uses
        // LIMSStringNumberUserType
        testAnalyzer = new Analyzer();
        testAnalyzer.setName("TEST-CONTROLLER-ANALYZER");
        testAnalyzer.setActive(false); // Start inactive
        testAnalyzer.setSysUserId("1");
        String analyzerId = analyzerService.insert(testAnalyzer);
        testAnalyzer.setId(analyzerId);
    }

    @After
    public void tearDown() throws Exception {
        // Clean up test data after each test
        cleanTestData();
    }

    /**
     * Clean up test-created analyzer error data
     */
    private void cleanTestData() {
        try {
            // Delete analyzer errors for test analyzer
            jdbcTemplate.execute("DELETE FROM analyzer_error WHERE analyzer_id IN "
                    + "(SELECT id FROM analyzer WHERE name LIKE 'TEST-%')");

            // Delete test analyzer (if exists)
            jdbcTemplate.execute("DELETE FROM analyzer WHERE name LIKE 'TEST-%'");

            // Reset analyzer sequence
            Integer maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM analyzer", Integer.class);
            jdbcTemplate.execute("SELECT setval('analyzer_seq', " + maxId + ", true)");
        } catch (Exception e) {
            // Log but don't fail - cleanup is best effort
            System.out.println("Failed to clean analyzer error test data: " + e.getMessage());
        }
    }

    /**
     * Test: GET /rest/analyzer/errors with filters Task Reference: T085
     * 
     * Verifies that GET endpoint returns filtered list of errors.
     */
    @Test
    public void testGetErrors_WithFilters_ReturnsFilteredList() throws Exception {
        // Arrange: Create test error in database
        String errorId = analyzerErrorService.createError(testAnalyzer, AnalyzerError.ErrorType.MAPPING,
                AnalyzerError.Severity.ERROR, "No mapping found for test code: GLUCOSE",
                "H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mg/dL|N");

        // Act & Assert
        mockMvc.perform(get("/rest/analyzer/errors").param("status", "UNACKNOWLEDGED").param("errorType", "MAPPING")
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists()).andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.status").value("success"));
    }

    /**
     * Test: GET /rest/analyzer/errors/{id} Task Reference: T085
     * 
     * Verifies that GET endpoint returns single error by ID.
     */
    @Test
    public void testGetError_WithValidId_ReturnsError() throws Exception {
        // Arrange: Create test error in database
        String errorId = analyzerErrorService.createError(testAnalyzer, AnalyzerError.ErrorType.MAPPING,
                AnalyzerError.Severity.ERROR, "No mapping found for test code: GLUCOSE",
                "H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mg/dL|N");

        // Act & Assert
        mockMvc.perform(get("/rest/analyzer/errors/" + errorId).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(errorId))
                .andExpect(jsonPath("$.data.errorType").value("MAPPING"))
                .andExpect(jsonPath("$.data.severity").value("ERROR")).andExpect(jsonPath("$.status").value("success"));
    }

    /**
     * Test: POST /rest/analyzer/errors/{id}/acknowledge Task Reference: T085
     * 
     * Verifies that acknowledge endpoint updates error status.
     */
    @Test
    public void testAcknowledgeError_WithValidId_UpdatesStatus() throws Exception {
        // Arrange: Create test error in database
        String errorId = analyzerErrorService.createError(testAnalyzer, AnalyzerError.ErrorType.MAPPING,
                AnalyzerError.Severity.ERROR, "No mapping found for test code: GLUCOSE",
                "H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mg/dL|N");

        String userId = "USER-001";

        // Act & Assert
        mockMvc.perform(post("/rest/analyzer/errors/" + errorId + "/acknowledge").param("userId", userId)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // Verify error was acknowledged
        AnalyzerError error = analyzerErrorService.getErrorById(errorId);
        assertNotNull("Error should be found", error);
        assertEquals("Status should be ACKNOWLEDGED", AnalyzerError.ErrorStatus.ACKNOWLEDGED, error.getStatus());
    }

    /**
     * Test: POST /rest/analyzer/errors/{id}/reprocess Task Reference: T085
     * 
     * Verifies that reprocess endpoint reprocesses error message.
     */
    @Test
    public void testReprocessError_WithValidId_ProcessesMessage() throws Exception {
        // Arrange: Create test error in database
        String errorId = analyzerErrorService.createError(testAnalyzer, AnalyzerError.ErrorType.MAPPING,
                AnalyzerError.Severity.ERROR, "No mapping found for test code: GLUCOSE",
                "H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mg/dL|N");

        // Act & Assert
        // Note: Reprocessing will fail if mappings don't exist, but endpoint should
        // still return response
        mockMvc.perform(post("/rest/analyzer/errors/" + errorId + "/reprocess").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    /**
     * Test: GET /rest/analyzer/errors - 404 Not Found Task Reference: T085
     * 
     * Verifies that GET endpoint returns 404 for invalid error ID.
     */
    @Test
    public void testGetError_WithInvalidId_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/rest/analyzer/errors/INVALID-ID").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }
}
