package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * OGC-1187 — the batch test reassignment page used to call these two providers
 * with the literal string "null" on mount and both answered 500. An id that is
 * missing, blank or not a number is the caller's mistake and is answered as
 * one; an unknown numeric id is still an empty success.
 */
public class ProviderIdParameterIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String INTERNAL_ERROR = "Internal error";

    private AllTestsForSampleTypeProviderRestController testsForSampleType;
    private PendingAnalysisForTestProviderRestController pendingAnalysisForTest;

    /**
     * The providers resolve their services through SpringContext, which the web
     * context makes available, so they are built directly rather than autowired.
     */
    @Before
    public void createProviders() {
        testsForSampleType = new AllTestsForSampleTypeProviderRestController();
        pendingAnalysisForTest = new PendingAnalysisForTestProviderRestController();
    }

    @Test
    public void literalNullId_isAClientError_notAServerError() {
        ResponseEntity<Object> tests = testsForSampleType.processRequest("null");
        assertEquals(HttpStatus.BAD_REQUEST, tests.getStatusCode());
        assertTrue(String.valueOf(tests.getBody()).contains("numeric"));
        assertFalse(String.valueOf(tests.getBody()).contains(INTERNAL_ERROR));

        ResponseEntity<Object> pending = pendingAnalysisForTest.processRequest("null");
        assertEquals(HttpStatus.BAD_REQUEST, pending.getStatusCode());
        assertTrue(String.valueOf(pending.getBody()).contains("numeric"));
        assertFalse(String.valueOf(pending.getBody()).contains(INTERNAL_ERROR));
    }

    @Test
    public void nonNumericId_isAClientError() {
        assertEquals(HttpStatus.BAD_REQUEST, testsForSampleType.processRequest("abc").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, pendingAnalysisForTest.processRequest("abc").getStatusCode());
    }

    @Test
    public void blankId_isAClientError_thatNamesTheParameter() {
        ResponseEntity<Object> tests = testsForSampleType.processRequest("");
        assertEquals(HttpStatus.BAD_REQUEST, tests.getStatusCode());
        assertTrue(String.valueOf(tests.getBody()).contains("sampleTypeId"));
        assertFalse(String.valueOf(tests.getBody()).contains(INTERNAL_ERROR));

        ResponseEntity<Object> pending = pendingAnalysisForTest.processRequest(" ");
        assertEquals(HttpStatus.BAD_REQUEST, pending.getStatusCode());
        assertTrue(String.valueOf(pending.getBody()).contains("testId"));
        assertFalse(String.valueOf(pending.getBody()).contains(INTERNAL_ERROR));
    }

    @Test
    public void unknownNumericId_isStillAnEmptySuccess() {
        ResponseEntity<Object> tests = testsForSampleType.processRequest("99999");
        assertEquals(HttpStatus.OK, tests.getStatusCode());
        assertEquals(0, ((JSONArray) ((JSONObject) tests.getBody()).get("tests")).size());

        ResponseEntity<Object> pending = pendingAnalysisForTest.processRequest("99999");
        assertEquals(HttpStatus.OK, pending.getStatusCode());
        JSONObject body = (JSONObject) pending.getBody();
        for (String bucket : new String[] { "notStarted", "technicianRejection", "biologistRejection",
                "notValidated" }) {
            assertEquals(bucket + " is empty for an unknown test", 0, ((JSONArray) body.get(bucket)).size());
        }
    }
}
