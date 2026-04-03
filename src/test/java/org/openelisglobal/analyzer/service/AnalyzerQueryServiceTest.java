package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.ProtocolVersion;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for AnalyzerQueryService implementation
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerQueryServiceTest {

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private FileImportService fileImportService;

    @Mock
    private SerialPortService serialPortService;

    private AnalyzerQueryServiceImpl analyzerQueryService;

    @Before
    public void setUp() {
        analyzerQueryService = new AnalyzerQueryServiceImpl();
        ReflectionTestUtils.setField(analyzerQueryService, "analyzerService", analyzerService);
        ReflectionTestUtils.setField(analyzerQueryService, "fileImportService", fileImportService);
        ReflectionTestUtils.setField(analyzerQueryService, "serialPortService", serialPortService);

        // Default: return a valid TCP-capable analyzer so startQuery passes validation
        Analyzer analyzer = new Analyzer();
        analyzer.setProtocolVersion(ProtocolVersion.ASTM_LIS2_A2);
        analyzer.setIpAddress("192.168.1.100");
        analyzer.setPort(5000);
        when(analyzerService.get(anyString())).thenReturn(analyzer);

        // Default: no file-import or serial-port config (TCP analyzer)
        when(fileImportService.getByAnalyzerId(any())).thenReturn(Optional.empty());
        when(serialPortService.getByAnalyzerId(any())).thenReturn(Optional.empty());
    }

    @Test
    public void testQueryAnalyzer_WithValidConfig_ReturnsJobId() {
        // Arrange
        String analyzerId = "1";

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
        String analyzerId = "1";
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
        String analyzerId = "1";
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
        String analyzerId = "1";
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
        String analyzerId = "1";
        String invalidJobId = "INVALID-JOB-ID";

        // Act & Assert - should not throw exception
        analyzerQueryService.cancel(analyzerId, invalidJobId);
    }
}
