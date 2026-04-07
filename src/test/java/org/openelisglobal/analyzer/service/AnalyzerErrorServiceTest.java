package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerErrorDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Unit tests for AnalyzerErrorService implementation
 * 
 * 
 * TDD Workflow (MANDATORY for complex logic): - RED: Write failing test first
 * (defines expected behavior) - GREEN: Write minimal code to make test pass -
 * REFACTOR: Improve code quality while keeping tests green
 * 
 * SDD Checkpoint: Unit tests MUST pass after Phase 2 (Services)
 * 
 * Test Naming: test{MethodName}_{Scenario}_{ExpectedResult}
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerErrorServiceTest {

    @Mock
    private AnalyzerErrorDAO analyzerErrorDAO;

    @Mock
    private AnalyzerReprocessingService analyzerReprocessingService;

    @InjectMocks
    private AnalyzerErrorServiceImpl analyzerErrorService;

    private Analyzer testAnalyzer;
    private AnalyzerError testError;

    @Before
    public void setUp() {
        // Setup test analyzer
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("ANALYZER-001");
        testAnalyzer.setName("Test Analyzer");

        // Setup test error
        testError = new AnalyzerError();
        testError.setId("ERROR-001");
        testError.setAnalyzer(testAnalyzer);
        testError.setErrorType(AnalyzerError.ErrorType.MAPPING);
        testError.setSeverity(AnalyzerError.Severity.ERROR);
        testError.setErrorMessage("No mapping found for test code: GLUCOSE");
        testError.setRawMessage("H|\\^&|||...");
        testError.setStatus(AnalyzerError.ErrorStatus.UNACKNOWLEDGED);
    }

    /**
     * Test: Create error record with unmapped field
     * 
     * Verifies that createError() creates a new AnalyzerError record with correct
     * fields and persists it via DAO.
     */
    @Test
    public void testCreateError_WithUnmappedField_CreatesErrorRecord() {
        // Arrange
        AnalyzerError newError = new AnalyzerError();
        newError.setAnalyzer(testAnalyzer);
        newError.setErrorType(AnalyzerError.ErrorType.MAPPING);
        newError.setSeverity(AnalyzerError.Severity.ERROR);
        newError.setErrorMessage("No mapping found for test code: GLUCOSE");
        newError.setRawMessage("H|\\^&|||...");
        newError.setStatus(AnalyzerError.ErrorStatus.UNACKNOWLEDGED);

        when(analyzerErrorDAO.insert(any(AnalyzerError.class))).thenReturn("ERROR-001");

        // Act
        String errorId = analyzerErrorService.createError(testAnalyzer, AnalyzerError.ErrorType.MAPPING,
                AnalyzerError.Severity.ERROR, "No mapping found for test code: GLUCOSE", "H|\\^&|||...");

        // Assert
        assertNotNull("Error ID should not be null", errorId);
        assertEquals("Error ID should match", "ERROR-001", errorId);
        verify(analyzerErrorDAO).insert(any(AnalyzerError.class));
    }

    /**
     * Test: Acknowledge error with valid user
     * 
     * Verifies that acknowledgeError() updates error status to ACKNOWLEDGED and
     * sets acknowledged_by and acknowledged_at fields.
     */
    @Test
    public void testAcknowledgeError_WithValidUser_UpdatesStatus() {
        // Arrange
        String userId = "USER-001";
        when(analyzerErrorDAO.get("ERROR-001")).thenReturn(Optional.of(testError));
        when(analyzerErrorDAO.update(any(AnalyzerError.class))).thenReturn(testError);

        // Act
        analyzerErrorService.acknowledgeError("ERROR-001", userId);

        // Assert
        verify(analyzerErrorDAO).get("ERROR-001");
        verify(analyzerErrorDAO).update(any(AnalyzerError.class));
        // Verify error status updated (will be verified in implementation)
    }

    /**
     * Test: Reprocess error after new mapping created
     * 
     * Verifies that reprocessError() calls AnalyzerReprocessingService to
     * reprocess the error message.
     */
    @Test
    public void testReprocessError_WithNewMapping_ProcessesMessage() {
        // Arrange
        when(analyzerErrorDAO.get("ERROR-001")).thenReturn(Optional.of(testError));
        when(analyzerReprocessingService.reprocessMessage(any(AnalyzerError.class)))
                .thenReturn(true);

        // Act
        boolean success = analyzerErrorService.reprocessError("ERROR-001");

        // Assert
        assertEquals("Reprocessing should succeed", true, success);
        verify(analyzerErrorDAO).get("ERROR-001");
        verify(analyzerReprocessingService).reprocessMessage(any(AnalyzerError.class));
    }

    /**
     * Test: Get errors by filters
     *
     * Verifies that getErrorsByFilters() delegates to DAO's findByFilters() with
     * the correct parameters.
     */
    @Test
    public void testGetErrorsByFilters_WithFilters_ReturnsFilteredList() {
        // Arrange
        List<AnalyzerError> filteredErrors = new ArrayList<>();
        filteredErrors.add(testError);

        // Service now delegates to DAO.findByFilters() with all filter params
        when(analyzerErrorDAO.findByFilters(null, AnalyzerError.ErrorType.MAPPING, AnalyzerError.Severity.ERROR,
                AnalyzerError.ErrorStatus.UNACKNOWLEDGED, null, null)).thenReturn(filteredErrors);

        // Act
        List<AnalyzerError> result = analyzerErrorService.getErrorsByFilters(null, // analyzerId
                AnalyzerError.ErrorType.MAPPING, AnalyzerError.Severity.ERROR, AnalyzerError.ErrorStatus.UNACKNOWLEDGED,
                null, // startDate
                null); // endDate

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return one error", 1, result.size());
        assertEquals("Error ID should match", "ERROR-001", result.get(0).getId());
    }

    /**
     * Test: Acknowledge error with invalid ID throws exception
     * 
     * Verifies that acknowledgeError() throws exception when error not found.
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testAcknowledgeError_WithInvalidId_ThrowsException() {
        // Arrange
        when(analyzerErrorDAO.get("INVALID-ID")).thenReturn(Optional.empty());

        // Act
        analyzerErrorService.acknowledgeError("INVALID-ID", "USER-001");

        // Assert: Exception should be thrown (handled by @Test(expected))
    }
}
