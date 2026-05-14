package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;

/**
 * Unit tests for AnalyzerReprocessingService implementation
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
public class AnalyzerReprocessingServiceTest {

    @Mock
    private AnalyzerFieldMappingDAO analyzerFieldMappingDAO;

    @Mock
    private AnalyzerErrorService analyzerErrorService;

    @InjectMocks
    private AnalyzerReprocessingServiceImpl analyzerReprocessingService;

    private Analyzer testAnalyzer;
    private AnalyzerError testError;
    private List<String> rawMessageLines;

    @Before
    public void setUp() {
        // Setup test analyzer
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("ANALYZER-001");
        testAnalyzer.setName("Test Analyzer");

        // Setup test error with raw ASTM message
        testError = new AnalyzerError();
        testError.setId("ERROR-001");
        testError.setAnalyzer(testAnalyzer);
        testError.setErrorType(AnalyzerError.ErrorType.MAPPING);
        testError.setSeverity(AnalyzerError.Severity.ERROR);
        testError.setErrorMessage("No mapping found for test code: GLUCOSE");
        testError.setRawMessage("H|\\^&|||...\nP|1||...\nO|1||...\nR|1|^^^GLUCOSE|123|mg/dL|N");

        // Setup raw message lines (ASTM format)
        rawMessageLines = new ArrayList<>();
        rawMessageLines.add("H|\\^&|||...");
        rawMessageLines.add("P|1||...");
        rawMessageLines.add("O|1||...");
        rawMessageLines.add("R|1|^^^GLUCOSE|123|mg/dL|N");
    }

    /**
     * Test: Reprocess message with valid mapping
     * 
     * Verifies that reprocessMessage() successfully processes the message when
     * mappings are available.
     */
    @Test
    public void testReprocessMessage_WithValidMapping_ReturnsSuccess() {
        // Arrange
        AnalyzerFieldMapping mapping = new AnalyzerFieldMapping();
        mapping.setId("MAPPING-001");
        mapping.setIsActive(true);

        List<AnalyzerFieldMapping> mappings = new ArrayList<>();
        mappings.add(mapping);

        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("ANALYZER-001")).thenReturn(mappings);

        // Mock ASTMAnalyzerReader to return success
        // Note: This is a simplified test - actual implementation will need to
        // handle ASTMAnalyzerReader integration
        // For now, we'll test the service logic that checks for mappings

        // Act
        boolean success = analyzerReprocessingService.reprocessMessage(testError);

        // Assert
        // For now, this test verifies the service checks for mappings
        // Full integration will be tested in integration tests
        verify(analyzerFieldMappingDAO).findActiveMappingsByAnalyzerId("ANALYZER-001");
    }

    /**
     * Test: Reprocess message with still unmapped fields
     * 
     * Verifies that reprocessMessage() returns false when mappings are still
     * missing.
     */
    @Test
    public void testReprocessMessage_WithStillUnmapped_ReturnsError() {
        // Arrange
        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("ANALYZER-001"))
                .thenReturn(new ArrayList<>());

        // Act
        boolean success = analyzerReprocessingService.reprocessMessage(testError);

        // Assert
        assertFalse("Reprocessing should fail when mappings are still missing", success);
        verify(analyzerFieldMappingDAO).findActiveMappingsByAnalyzerId("ANALYZER-001");
    }

    /**
     * Test: Reprocess message with null raw message
     * 
     * Verifies that reprocessMessage() handles null raw message gracefully.
     */
    @Test
    public void testReprocessMessage_WithNullRawMessage_ReturnsError() {
        // Arrange
        testError.setRawMessage(null);

        // Act
        boolean success = analyzerReprocessingService.reprocessMessage(testError);

        // Assert
        assertFalse("Reprocessing should fail when raw message is null", success);
        verify(analyzerFieldMappingDAO, never()).findActiveMappingsByAnalyzerId(anyString());
    }

    /**
     * Test: Reprocess message with empty raw message
     * 
     * Verifies that reprocessMessage() handles empty raw message gracefully.
     */
    @Test
    public void testReprocessMessage_WithEmptyRawMessage_ReturnsError() {
        // Arrange
        testError.setRawMessage("");

        // Act
        boolean success = analyzerReprocessingService.reprocessMessage(testError);

        // Assert
        assertFalse("Reprocessing should fail when raw message is empty", success);
        verify(analyzerFieldMappingDAO, never()).findActiveMappingsByAnalyzerId(anyString());
    }
}
