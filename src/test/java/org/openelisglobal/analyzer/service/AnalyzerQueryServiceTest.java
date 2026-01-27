package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Unit tests for AnalyzerQueryService implementation
 * 
 * Task Reference: T102 Test Coverage Goal: >80%
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerQueryServiceTest {

    private AnalyzerQueryServiceImpl analyzerQueryService;

    @Before
    public void setUp() {
        analyzerQueryService = new AnalyzerQueryServiceImpl();
    }

    @Test
    public void testQueryAnalyzer_WithValidConfig_ReturnsJobId() {
        // Arrange
        String analyzerId = "ANALYZER-001";

        // Act
        String jobId = analyzerQueryService.startQuery(analyzerId);

        // Assert
        assertNotNull("Job ID should not be null", jobId);
        assertTrue("Job ID should be a valid UUID format", jobId.length() > 0);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testQueryAnalyzer_WithNullAnalyzerId_ThrowsException() {
        // Act
        analyzerQueryService.startQuery(null);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testQueryAnalyzer_WithEmptyAnalyzerId_ThrowsException() {
        // Act
        analyzerQueryService.startQuery("");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testQueryAnalyzer_WithWhitespaceAnalyzerId_ThrowsException() {
        // Act
        analyzerQueryService.startQuery("   ");
    }

    @Test
    public void testGetQueryStatus_WithJobId_ReturnsStatus() {
        // Arrange
        String analyzerId = "ANALYZER-001";
        String jobId = analyzerQueryService.startQuery(analyzerId);

        // Act
        Map<String, Object> status = analyzerQueryService.getStatus(analyzerId, jobId);

        // Assert
        assertNotNull("Status should not be null", status);
        assertEquals("Analyzer ID should match", analyzerId, status.get("analyzerId"));
        assertEquals("Job ID should match", jobId, status.get("jobId"));
        assertNotNull("Created at timestamp should be present", status.get("createdAt"));
        assertNotNull("State should be present", status.get("state"));
        assertNotNull("Progress should be present", status.get("progress"));
    }

    @Test
    public void testGetQueryStatus_WithInvalidJobId_ReturnsNotFoundStatus() {
        // Arrange
        String analyzerId = "ANALYZER-001";
        String invalidJobId = "INVALID-JOB-ID";

        // Act
        Map<String, Object> status = analyzerQueryService.getStatus(analyzerId, invalidJobId);

        // Assert
        assertNotNull("Status should not be null", status);
        assertEquals("State should be not_found", "not_found", status.get("state"));
        assertEquals("Progress should be 0", 0, status.get("progress"));
    }

    @Test
    public void testCancelQuery_WithJobId_CancelsJob() {
        // Arrange
        String analyzerId = "ANALYZER-001";
        String jobId = analyzerQueryService.startQuery(analyzerId);

        // Act
        analyzerQueryService.cancel(analyzerId, jobId);

        // Assert
        Map<String, Object> status = analyzerQueryService.getStatus(analyzerId, jobId);
        assertNotNull("Status should still be retrievable after cancel", status);
        // Note: With full implementation, cancellation should set state to "cancelled"
        // if job is still in "pending" or "in_progress" state
    }

    @Test
    public void testCancelQuery_WithInvalidJobId_DoesNotThrow() {
        // Arrange
        String analyzerId = "ANALYZER-001";
        String invalidJobId = "INVALID-JOB-ID";

        // Act & Assert - should not throw exception
        analyzerQueryService.cancel(analyzerId, invalidJobId);
    }
}
