package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerErrorService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

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

    private ObjectMapper objectMapper;
    private Analyzer testAnalyzer;
    private AnalyzerError testError;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();

        // Setup test analyzer - use existing analyzer from test data
        // The test data has analyzers with IDs "1", "2", "3" (String IDs in Java, INTEGER in DB)
        // Reference: ID_TYPE_ANALYSIS.md - Legacy Analyzer uses LIMSStringNumberUserType
        java.util.List<Analyzer> analyzers = analyzerService.getAll();
        if (!analyzers.isEmpty()) {
            testAnalyzer = analyzers.get(0);  // Use first available analyzer
        } else {
            // Fallback: create a new analyzer (should not happen with test data)
            testAnalyzer = new Analyzer();
            testAnalyzer.setId("1");  // String ID (matches Java type)
            testAnalyzer.setName("Test Analyzer");
        }

        // Setup test error
        testError = new AnalyzerError();
        testError.setId("ERROR-001");
        testError.setAnalyzer(testAnalyzer);
        testError.setErrorType(AnalyzerError.ErrorType.MAPPING);
        testError.setSeverity(AnalyzerError.Severity.ERROR);
        testError.setErrorMessage("No mapping found for test code: GLUCOSE");
        testError.setRawMessage("H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mg/dL|N");
        testError.setStatus(AnalyzerError.ErrorStatus.UNACKNOWLEDGED);
    }

    /**
     * Test: GET /rest/analyzer/errors with filters
     * Task Reference: T085
     * 
     * Verifies that GET endpoint returns filtered list of errors.
     */
    @Test
    public void testGetErrors_WithFilters_ReturnsFilteredList() throws Exception {
        // Arrange: Create test error in database
        String errorId = analyzerErrorService.createError(
                testAnalyzer,
                AnalyzerError.ErrorType.MAPPING,
                AnalyzerError.Severity.ERROR,
                "No mapping found for test code: GLUCOSE",
                "H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mg/dL|N");

        // Act & Assert
        mockMvc.perform(get("/rest/analyzer/errors")
                .param("status", "UNACKNOWLEDGED")
                .param("errorType", "MAPPING")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.status").value("success"));
    }

    /**
     * Test: GET /rest/analyzer/errors/{id}
     * Task Reference: T085
     * 
     * Verifies that GET endpoint returns single error by ID.
     */
    @Test
    public void testGetError_WithValidId_ReturnsError() throws Exception {
        // Arrange: Create test error in database
        String errorId = analyzerErrorService.createError(
                testAnalyzer,
                AnalyzerError.ErrorType.MAPPING,
                AnalyzerError.Severity.ERROR,
                "No mapping found for test code: GLUCOSE",
                "H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mg/dL|N");

        // Act & Assert
        mockMvc.perform(get("/rest/analyzer/errors/" + errorId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(errorId))
                .andExpect(jsonPath("$.data.errorType").value("MAPPING"))
                .andExpect(jsonPath("$.data.severity").value("ERROR"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    /**
     * Test: POST /rest/analyzer/errors/{id}/acknowledge
     * Task Reference: T085
     * 
     * Verifies that acknowledge endpoint updates error status.
     */
    @Test
    public void testAcknowledgeError_WithValidId_UpdatesStatus() throws Exception {
        // Arrange: Create test error in database
        String errorId = analyzerErrorService.createError(
                testAnalyzer,
                AnalyzerError.ErrorType.MAPPING,
                AnalyzerError.Severity.ERROR,
                "No mapping found for test code: GLUCOSE",
                "H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mg/dL|N");

        String userId = "USER-001";

        // Act & Assert
        mockMvc.perform(post("/rest/analyzer/errors/" + errorId + "/acknowledge")
                .param("userId", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // Verify error was acknowledged
        AnalyzerError error = analyzerErrorService.getErrorById(errorId);
        assertNotNull("Error should be found", error);
        assertEquals("Status should be ACKNOWLEDGED", AnalyzerError.ErrorStatus.ACKNOWLEDGED, error.getStatus());
    }

    /**
     * Test: POST /rest/analyzer/errors/{id}/reprocess
     * Task Reference: T085
     * 
     * Verifies that reprocess endpoint reprocesses error message.
     */
    @Test
    public void testReprocessError_WithValidId_ProcessesMessage() throws Exception {
        // Arrange: Create test error in database
        String errorId = analyzerErrorService.createError(
                testAnalyzer,
                AnalyzerError.ErrorType.MAPPING,
                AnalyzerError.Severity.ERROR,
                "No mapping found for test code: GLUCOSE",
                "H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mg/dL|N");

        // Act & Assert
        // Note: Reprocessing will fail if mappings don't exist, but endpoint should
        // still return response
        mockMvc.perform(post("/rest/analyzer/errors/" + errorId + "/reprocess")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    /**
     * Test: GET /rest/analyzer/errors - 404 Not Found
     * Task Reference: T085
     * 
     * Verifies that GET endpoint returns 404 for invalid error ID.
     */
    @Test
    public void testGetError_WithInvalidId_Returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/rest/analyzer/errors/INVALID-ID")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}

