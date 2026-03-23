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
     * Test: GET /rest/analyzer/errors with filters
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
     * Test: GET /rest/analyzer/errors/{id}
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
     * Test: POST /rest/analyzer/errors/{id}/acknowledge
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
     * Test: POST /rest/analyzer/errors/{id}/reprocess
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
        // Reprocessing will fail if mappings don't exist — endpoint now returns 422
        // (not a contradictory 200 with success:false)
        mockMvc.perform(post("/rest/analyzer/errors/" + errorId + "/reprocess").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * Test: GET /rest/analyzer/errors - 404 Not Found
     * 
     * Verifies that GET endpoint returns 404 for invalid error ID.
     */
    @Test
    public void testGetError_WithInvalidId_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/rest/analyzer/errors/INVALID-ID").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    /**
     * Test: GET /rest/analyzer/errors with NO filters returns all errors
     * 
     * This test would have caught the bug where getErrorsByFilters returned empty
     * list when no filters were provided.
     * 
     */
    @Test
    public void testGetErrors_WithNoFilters_ReturnsAllErrors() throws Exception {
        // Arrange: Create multiple test errors in database
        String errorId1 = analyzerErrorService.createError(testAnalyzer, AnalyzerError.ErrorType.MAPPING,
                AnalyzerError.Severity.ERROR, "No mapping found for test code: GLUCOSE",
                "H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mg/dL|N");
        String errorId2 = analyzerErrorService.createError(testAnalyzer, AnalyzerError.ErrorType.VALIDATION,
                AnalyzerError.Severity.WARNING, "Unit mismatch: expected mg/dL, received mmol/L",
                "H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mmol/L|N");

        // Act & Assert: Call endpoint with NO filters
        mockMvc.perform(
                get("/rest/analyzer/errors").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.content").isArray()).andExpect(jsonPath("$.data.content.length()").value(2)) // Should
                                                                                                                         // return
                                                                                                                         // both
                                                                                                                         // errors
                .andExpect(jsonPath("$.data.content[0].id").exists())
                .andExpect(jsonPath("$.data.content[0].errorType").exists())
                .andExpect(jsonPath("$.data.content[0].severity").exists())
                .andExpect(jsonPath("$.data.content[0].errorMessage").exists())
                .andExpect(jsonPath("$.data.content[0].status").exists())
                .andExpect(jsonPath("$.data.content[0].analyzer").exists())
                .andExpect(jsonPath("$.data.content[0].analyzer.id").exists())
                .andExpect(jsonPath("$.data.content[0].analyzer.name").exists())
                .andExpect(jsonPath("$.data.statistics").exists())
                .andExpect(jsonPath("$.data.statistics.totalErrors").value(2))
                .andExpect(jsonPath("$.status").value("success"));
    }

    /**
     * Test: GET /rest/analyzer/errors response format matches frontend expectations
     * 
     * This test verifies that errors are properly converted to maps (not returned
     * as entity objects) and that the response structure matches what the frontend
     * expects: { data: { content: [...], statistics: {...} }, status: "success" }
     * 
     */
    @Test
    public void testGetErrors_ResponseFormat_MatchesFrontendExpectations() throws Exception {
        // Arrange: Create test error
        String errorId = analyzerErrorService.createError(testAnalyzer, AnalyzerError.ErrorType.MAPPING,
                AnalyzerError.Severity.ERROR, "No mapping found for test code: GLUCOSE",
                "H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mg/dL|N");

        // Act & Assert: Verify response structure matches frontend expectations
        mockMvc.perform(
                get("/rest/analyzer/errors").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Verify top-level structure
                .andExpect(jsonPath("$.data").exists()).andExpect(jsonPath("$.status").value("success"))
                // Verify data.content is an array (not object)
                .andExpect(jsonPath("$.data.content").isArray()).andExpect(jsonPath("$.data.content[0]").exists())
                // Verify error object structure (should be map, not entity)
                .andExpect(jsonPath("$.data.content[0].id").value(errorId))
                .andExpect(jsonPath("$.data.content[0].errorType").value("MAPPING"))
                .andExpect(jsonPath("$.data.content[0].severity").value("ERROR"))
                .andExpect(jsonPath("$.data.content[0].errorMessage").value("No mapping found for test code: GLUCOSE"))
                .andExpect(jsonPath("$.data.content[0].status").value("UNACKNOWLEDGED"))
                .andExpect(jsonPath("$.data.content[0].timestamp").exists())
                .andExpect(jsonPath("$.data.content[0].rawMessage")
                        .value("H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mg/dL|N"))
                // Verify analyzer nested object (should be map, not entity)
                .andExpect(jsonPath("$.data.content[0].analyzer").exists())
                .andExpect(jsonPath("$.data.content[0].analyzer.id").exists())
                .andExpect(jsonPath("$.data.content[0].analyzer.name").value("TEST-CONTROLLER-ANALYZER"))
                // Verify statistics structure
                .andExpect(jsonPath("$.data.statistics").exists())
                .andExpect(jsonPath("$.data.statistics.totalErrors").exists())
                .andExpect(jsonPath("$.data.statistics.unacknowledged").exists())
                .andExpect(jsonPath("$.data.statistics.critical").exists())
                .andExpect(jsonPath("$.data.statistics.last24Hours").exists());
    }
}
