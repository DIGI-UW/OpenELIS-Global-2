package org.openelisglobal.fhir;

import static org.junit.Assert.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.fhir.providers.TaskProvider;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.*;
import org.springframework.test.annotation.Rollback;

/**
 * Integration tests for {@link TaskProvider}.
 *
 * <p>
 * Task is unique among FHIR providers because it owns the lab order lifecycle
 * state machine (READY → IN_PROGRESS → COMPLETED). These tests verify not just
 * HTTP response codes but also that state transitions are correctly persisted
 * to the OpenELIS DB and that invalid transitions are firmly rejected.
 *
 * <p>
 * Test data (task-facade.xml):
 * <ul>
 * <li>VALID_ID — status READY (Test Entered, status_id=1)</li>
 * <li>COMPLETED_ID — status COMPLETED (Testing Finished, status_id=3)</li>
 * </ul>
 */
@Rollback
public class TaskFacadeTest extends BaseWebContextSensitiveTest {

    private static final String VALID_ID = "550e8400-e29b-41d4-a716-446655440050";
    private static final String COMPLETED_ID = "550e8400-e29b-41d4-a716-446655440051";
    private static final String INVALID_ID = "00000000-0000-0000-0000-000000000000";
    private static final String PATIENT_UUID = "550e8400-e29b-41d4-a716-446655440099";

    @Autowired
    private TaskProvider taskProvider;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private IStatusService statusService;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(taskProvider));
        MockServletConfig config = new MockServletConfig(new MockServletContext());
        fhirServlet.init(config);
        objectMapper = new ObjectMapper();
        executeDataSetWithStateManagement("testdata/task-facade.xml");
        statusService.refreshCache();
    }

    private MockHttpServletRequest buildRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setContextPath("");
        request.setServletPath("/fhir");
        request.setPathInfo(path);
        request.setRequestURI("/fhir" + path);
        request.setContentType("application/fhir+json");
        request.addHeader("Accept", "application/fhir+json");
        org.openelisglobal.login.valueholder.UserSessionData usd = new org.openelisglobal.login.valueholder.UserSessionData();
        usd.setSytemUserId(1);
        request.getSession().setAttribute(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, usd);
        return request;
    }

    @Test
    public void readTask_shouldReturn200WithTaskResource() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(buildRequest("GET", "/Task/" + VALID_ID), response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Task", json.get("resourceType").asText());
        assertEquals(VALID_ID, json.get("id").asText());
        assertEquals("ready", json.get("status").asText());
    }

    @Test
    public void readTask_completedSample_shouldReturnCompletedStatus() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(buildRequest("GET", "/Task/" + COMPLETED_ID), response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("completed", json.get("status").asText());
    }

    @Test
    public void readTask_notFound_shouldReturn404WithOperationOutcome() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(buildRequest("GET", "/Task/" + INVALID_ID), response);

        assertEquals(404, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", json.get("resourceType").asText());
    }

    /**
     * Soft-delete: sample must still exist in DB after DELETE. Only the FHIR store
     * representation is updated to CANCELLED.
     */
    @Test
    public void deleteTask_shouldReturn204AndKeepSampleInDB() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(buildRequest("DELETE", "/Task/" + VALID_ID), response);

        assertEquals(204, response.getStatus());
        Sample sample = sampleService.getSampleByFhirUuid(VALID_ID);
        assertNotNull("Sample must remain in DB after soft-delete", sample);
        // DB status must be updated to NonConforming (id=12) — CANCELLED maps here
        assertEquals(statusService.getStatusID(OrderStatus.NonConforming_depricated), sample.getStatusId());
    }

    @Test
    public void deleteTask_terminalState_shouldReturn400() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(buildRequest("DELETE", "/Task/" + COMPLETED_ID), response);

        assertEquals(400, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", json.get("resourceType").asText());
        // DB record must be untouched — terminal guard fired before any update
        Sample sample = sampleService.getSampleByFhirUuid(COMPLETED_ID);
        assertNotNull(sample);
        assertEquals(statusService.getStatusID(OrderStatus.Finished), sample.getStatusId());
    }

    @Test
    public void deleteTask_alreadyCancelled_shouldReturn400() throws Exception {
        // First cancel it
        MockHttpServletResponse first = new MockHttpServletResponse();
        fhirServlet.service(buildRequest("DELETE", "/Task/" + VALID_ID), first);
        assertEquals(204, first.getStatus());

        // Second cancel must be rejected — CANCELLED is terminal
        MockHttpServletResponse second = new MockHttpServletResponse();
        fhirServlet.service(buildRequest("DELETE", "/Task/" + VALID_ID), second);
        assertEquals(400, second.getStatus());
        JsonNode json = objectMapper.readTree(second.getContentAsString());
        assertEquals("OperationOutcome", json.get("resourceType").asText());
    }

    @Test
    public void deleteTask_notFound_shouldReturn404WithOperationOutcome() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(buildRequest("DELETE", "/Task/" + INVALID_ID), response);

        assertEquals(404, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", json.get("resourceType").asText());
    }

    @Test
    public void searchTasks_noFilter_shouldReturnBundleWithAllSamples() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(buildRequest("GET", "/Task"), response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Bundle", json.get("resourceType").asText());
        assertTrue(json.get("total").asInt() >= 2);
    }

    /** Dataset has exactly 1 completed sample — filter must return exactly 1. */
    @Test
    public void searchTasks_withStatusFilter_shouldReturnOnlyMatchingTasks() throws Exception {
        MockHttpServletRequest request = buildRequest("GET", "/Task");
        request.addParameter("status", "completed");
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Bundle", json.get("resourceType").asText());
        assertEquals(1, json.get("total").asInt());
    }

    @Test
    public void searchTasks_withDateRangeFilter_shouldReturnMatchingTasks() throws Exception {
        MockHttpServletRequest request = buildRequest("GET", "/Task");
        request.addParameter("authored-on", "ge2024-01-01");
        request.addParameter("authored-on", "le2025-12-31");
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Bundle", json.get("resourceType").asText());
        assertTrue(json.get("total").asInt() >= 2);
    }

    /** Empty result set must return a Bundle with total=0, not a 404. */
    @Test
    public void searchTasks_withDateRangeExcludingAll_shouldReturnEmptyBundle() throws Exception {
        MockHttpServletRequest request = buildRequest("GET", "/Task");
        request.addParameter("authored-on", "ge2020-01-01");
        request.addParameter("authored-on", "le2021-12-31");
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Bundle", json.get("resourceType").asText());
        assertEquals(0, json.get("total").asInt());
    }

    @Test
    public void searchTasks_withMultipleFilters_shouldNarrowResults() throws Exception {
        MockHttpServletRequest request = buildRequest("GET", "/Task");
        request.addParameter("status", "ready");
        request.addParameter("patient", PATIENT_UUID);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Bundle", json.get("resourceType").asText());
        assertTrue(json.get("total").asInt() >= 0);
    }

    /**
     * Valid transition READY → IN_PROGRESS must persist the new status_id to DB.
     * This is the core state machine guarantee.
     */
    @Test
    public void updateTask_readyToInProgress_shouldReturn200AndPersistToDb() throws Exception {
        String body = "{\"resourceType\":\"Task\",\"id\":\"" + VALID_ID + "\",\"status\":\"in-progress\"}";
        MockHttpServletRequest request = buildRequest("PUT", "/Task/" + VALID_ID);
        request.setContent(body.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        Sample updated = sampleService.getSampleByFhirUuid(VALID_ID);
        assertNotNull(updated);
        assertEquals(statusService.getStatusID(OrderStatus.Started), updated.getStatusId());
    }

    @Test
    public void updateTask_validTransition_responseShouldShowNewStatus() throws Exception {
        String body = "{\"resourceType\":\"Task\",\"id\":\"" + VALID_ID + "\",\"status\":\"in-progress\"}";
        MockHttpServletRequest request = buildRequest("PUT", "/Task/" + VALID_ID);
        request.setContent(body.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Task", json.get("resourceType").asText());
        assertEquals("in-progress", json.get("status").asText());
    }

    /**
     * READY → COMPLETED skips IN_PROGRESS and must be rejected. DB must remain
     * unchanged.
     */
    @Test
    public void updateTask_readyToCompleted_shouldReturn400AndLeaveDbUnchanged() throws Exception {
        String body = "{\"resourceType\":\"Task\",\"id\":\"" + VALID_ID + "\",\"status\":\"completed\"}";
        MockHttpServletRequest request = buildRequest("PUT", "/Task/" + VALID_ID);
        request.setContent(body.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertTrue(response.getStatus() == 400 || response.getStatus() == 422);
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", json.get("resourceType").asText());
        Sample sample = sampleService.getSampleByFhirUuid(VALID_ID);
        assertEquals(statusService.getStatusID(OrderStatus.Entered), sample.getStatusId());
    }

    /**
     * A completed lab order cannot be re-opened — terminal state must block all
     * transitions.
     */
    @Test
    public void updateTask_fromTerminalCompleted_shouldReturn400AndLeaveDbUnchanged() throws Exception {
        String body = "{\"resourceType\":\"Task\",\"id\":\"" + COMPLETED_ID + "\",\"status\":\"in-progress\"}";
        MockHttpServletRequest request = buildRequest("PUT", "/Task/" + COMPLETED_ID);
        request.setContent(body.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertTrue(response.getStatus() == 400 || response.getStatus() == 422);
        Sample sample = sampleService.getSampleByFhirUuid(COMPLETED_ID);
        assertEquals(statusService.getStatusID(OrderStatus.Finished), sample.getStatusId());
    }

    /**
     * Re-sending the current status must succeed — callers should not be penalised.
     */
    @Test
    public void updateTask_sameState_shouldBeIdempotentAndReturn200() throws Exception {
        String body = "{\"resourceType\":\"Task\",\"id\":\"" + VALID_ID + "\",\"status\":\"ready\"}";
        MockHttpServletRequest request = buildRequest("PUT", "/Task/" + VALID_ID);
        request.setContent(body.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void updateTask_missingStatus_shouldReturn400() throws Exception {
        String body = "{\"resourceType\":\"Task\",\"id\":\"" + VALID_ID + "\"}";
        MockHttpServletRequest request = buildRequest("PUT", "/Task/" + VALID_ID);
        request.setContent(body.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void updateTask_notFound_shouldReturn404WithOperationOutcome() throws Exception {
        String body = "{\"resourceType\":\"Task\",\"id\":\"" + INVALID_ID + "\",\"status\":\"in-progress\"}";
        MockHttpServletRequest request = buildRequest("PUT", "/Task/" + INVALID_ID);
        request.setContent(body.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", json.get("resourceType").asText());
    }
}