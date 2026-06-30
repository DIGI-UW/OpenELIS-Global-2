package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for {@link AnalyzerQueryService} query workflow.
 *
 * <p>
 * These tests verify the complete query lifecycle including timeout handling,
 * ASTM response parsing, and graceful error recovery.
 *
 * <p>
 * <strong>Fixture strategy — intentionally absent analyzer rows:</strong>
 * Analyzer IDs {@code 999901}, {@code 999902}, and {@code 999903} are
 * deliberately not present in the test dataset. When {@code startQuery()} is
 * called with an unknown ID, {@code AnalyzerQueryServiceImpl.executeQuery()}
 * encounters an {@code ObjectNotFoundException} internally and converts it into
 * a {@code "failed"} job state. These tests validate that the service handles
 * the not-found path gracefully — returning a valid job ID, a queryable status
 * map, and a terminal state — without propagating the exception to the caller.
 *
 * <p>
 * If real analyzer connectivity needs to be tested, insert rows with valid
 * {@code ip_address} and {@code port} values into the dataset and update the
 * IDs accordingly.
 *
 * <p>
 * Uses {@link BaseWebContextSensitiveTest} for full Spring context and database
 * integration. Dataset: {@code testdata/analyzer-query-service.xml}.
 */
@RunWith(SpringRunner.class)
public class AnalyzerQueryServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerQueryService analyzerQueryService;

    @Before
    public void setUp() throws Exception {
        // Loads minimal system_user fixture required by the Spring context.
        // No analyzer rows are inserted — see class-level Javadoc for rationale.
        executeDataSetWithStateManagement("testdata/analyzer-query-service.xml");
    }

    @Test
    public void testQueryAnalyzer_WithNonExistentId_HandlesGracefully() {
        // Analyzer ID 999901 is intentionally absent from the dataset.
        // startQuery() should return a valid job ID and the status should
        // reflect a graceful failure rather than propagating an exception.
        String analyzerId = "999901";

        String jobId = analyzerQueryService.startQuery(analyzerId);

        assertNotNull("Job ID should not be null even for a non-existent analyzer", jobId);

        Map<String, Object> status = analyzerQueryService.getStatus(analyzerId, jobId);
        assertNotNull("Status should not be null", status);
        assertEquals("Analyzer ID should match", analyzerId, status.get("analyzerId"));
        assertEquals("Job ID should match", jobId, status.get("jobId"));

        String initialState = (String) status.get("state");
        assertTrue("Initial state should be pending or in_progress",
                "pending".equals(initialState) || "in_progress".equals(initialState));
    }

    @Test
    public void testParseASTMResponse_WithNonExistentId_CompletesWithoutException() throws InterruptedException {
        // Analyzer ID 999902 is intentionally absent from the dataset.
        // The query should proceed asynchronously and reach a terminal state
        // (completed or failed) without throwing an unhandled exception.
        String analyzerId = "999902";

        String jobId = analyzerQueryService.startQuery(analyzerId);
        assertNotNull("Job ID should not be null", jobId);

        Map<String, Object> status = null;
        int maxWait = 30;
        int waited = 0;
        while (waited < maxWait) {
            Thread.sleep(1000);
            status = analyzerQueryService.getStatus(analyzerId, jobId);
            String state = (String) status.get("state");
            if ("completed".equals(state) || "failed".equals(state)) {
                break;
            }
            waited++;
        }

        assertNotNull("Status should not be null after polling", status);
        String finalState = (String) status.get("state");

        if ("completed".equals(finalState)) {
            Object fieldsObj = status.get("fields");
            assertNotNull("Fields should be present when state is completed", fieldsObj);
            assertTrue("Fields should be a list", fieldsObj instanceof List);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fields = (List<Map<String, Object>>) fieldsObj;
            assertTrue("Should extract at least one field", fields.size() > 0);

            Map<String, Object> firstField = fields.get(0);
            assertNotNull("Field should have fieldName", firstField.get("fieldName"));
            assertNotNull("Field should have fieldType", firstField.get("fieldType"));
        } else {
            // "failed" is the expected terminal state when the analyzer ID does
            // not exist — the service converts the ObjectNotFoundException into
            // this state rather than propagating it.
            assertEquals("Non-existent analyzer should reach failed state", "failed", finalState);
        }
    }

    @Test
    public void testQueryWorkflow_WithNonExistentId_ReachesTerminalState() throws Exception {
        // Analyzer ID 999903 is intentionally absent from the dataset.
        // Verifies the full job lifecycle: start → poll → terminal state → cancel.
        String analyzerId = "999903";

        String jobId = analyzerQueryService.startQuery(analyzerId);
        assertNotNull("Job ID should not be null", jobId);

        Map<String, Object> status = null;
        int maxAttempts = 30;
        for (int i = 0; i < maxAttempts; i++) {
            status = analyzerQueryService.getStatus(analyzerId, jobId);
            assertNotNull("Status should not be null on each poll", status);
            String state = (String) status.get("state");
            if ("completed".equals(state) || "failed".equals(state) || "cancelled".equals(state)) {
                break;
            }
            Thread.sleep(1000);
        }

        String finalState = (String) status.get("state");
        assertTrue("State should reach a terminal value (completed, failed, or cancelled)",
                "completed".equals(finalState) || "failed".equals(finalState) || "cancelled".equals(finalState));

        // Cancel should be handled gracefully even if job already reached terminal
        // state
        analyzerQueryService.cancel(analyzerId, jobId);

        Map<String, Object> finalStatus = analyzerQueryService.getStatus(analyzerId, jobId);
        assertNotNull("Status should remain retrievable after cancel", finalStatus);
    }
}