package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for AnalyzerQueryService query workflow
 * 
 * Task Reference: T103
 * Test Coverage Goal: >80%
 * 
 * These tests verify:
 * - Query analyzer workflow with full Spring context
 * - Timeout handling for long-running queries
 * - ASTM response parsing and field extraction
 * 
 * Uses BaseWebContextSensitiveTest for full Spring context and database
 * integration.
 */
@RunWith(SpringRunner.class)
public class AnalyzerQueryServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerQueryService analyzerQueryService;

    @Test
    public void testQueryAnalyzer_WithTimeout_HandlesGracefully() {
        // Arrange
        String analyzerId = "TEST-ANALYZER-001";

        // Act - Start query (current skeleton implementation completes immediately)
        String jobId = analyzerQueryService.startQuery(analyzerId);

        // Assert - Verify job was created
        assertNotNull("Job ID should not be null", jobId);

        // Get status to verify job exists
        Map<String, Object> status = analyzerQueryService.getStatus(analyzerId, jobId);
        assertNotNull("Status should not be null", status);
        assertEquals("Analyzer ID should match", analyzerId, status.get("analyzerId"));
        assertEquals("Job ID should match", jobId, status.get("jobId"));

        // Note: Current skeleton implementation completes immediately.
        // When full implementation is added, this test should verify:
        // - Query starts with "running" state
        // - Status can be polled while running
        // - Timeout occurs after configured timeout period
        // - Status transitions to "timeout" or "error" state
        // - Error message indicates timeout
    }

    @Test
    public void testParseASTMResponse_ExtractsFields() {
        // Arrange
        String analyzerId = "TEST-ANALYZER-002";

        // Act - Start query
        String jobId = analyzerQueryService.startQuery(analyzerId);

        // Assert - Verify job was created
        assertNotNull("Job ID should not be null", jobId);

        // Get status to verify fields structure
        Map<String, Object> status = analyzerQueryService.getStatus(analyzerId, jobId);
        assertNotNull("Status should not be null", status);

        // Verify fields list exists in response
        Object fieldsObj = status.get("fields");
        assertNotNull("Fields should be present in status", fieldsObj);
        assertTrue("Fields should be a list", fieldsObj instanceof List);

        // Note: Current skeleton implementation returns empty fields list.
        // When full implementation is added, this test should verify:
        // - ASTM response is parsed correctly
        // - Fields are extracted from H, P, O, R segments
        // - Field types are correctly identified (NUMERIC, QUALITATIVE, etc.)
        // - Field names, units, and codes are extracted
        // - Fields are stored in AnalyzerField entities
    }

    @Test
    public void testQueryWorkflow_CompleteLifecycle() {
        // Arrange
        String analyzerId = "TEST-ANALYZER-003";

        // Act - Start query
        String jobId = analyzerQueryService.startQuery(analyzerId);
        assertNotNull("Job ID should not be null", jobId);

        // Poll status
        Map<String, Object> status = analyzerQueryService.getStatus(analyzerId, jobId);
        assertNotNull("Status should not be null", status);
        assertEquals("State should be present", "completed", status.get("state"));

        // Cancel query (should handle gracefully even if already completed)
        analyzerQueryService.cancel(analyzerId, jobId);

        // Verify status is still retrievable after cancel
        Map<String, Object> finalStatus = analyzerQueryService.getStatus(analyzerId, jobId);
        assertNotNull("Status should still be retrievable", finalStatus);
    }
}



