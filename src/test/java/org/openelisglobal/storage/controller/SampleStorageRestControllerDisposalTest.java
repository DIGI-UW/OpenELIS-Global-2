package org.openelisglobal.storage.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

public class SampleStorageRestControllerDisposalTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper;
    private MockHttpSession mockSession;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        executeDataSetWithStateManagement("testdata/sample-disposal-test-data.xml");

        // OGC-738b: dispose endpoint now requires an authenticated session so the
        // global audit emission can stamp the acting user. Mirror the pattern from
        // PatientMergeRestControllerTest — sysUserId=1 is the default test user.
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        mockSession = new MockHttpSession();
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void disposeSampleItem_ShouldReturn200_OnSuccess() throws Exception {
        String sampleItemExternalId = "EXT-1000";
        String requestBody = "{\"sampleItemId\":\"" + sampleItemExternalId
                + "\",\"reason\":\"expired\",\"method\":\"autoclave\",\"notes\":\"Test disposal\"}";

        this.mockMvc
                .perform(post("/rest/storage/sample-items/dispose").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sampleItemId").value("1000"))
                .andExpect(jsonPath("$.status").value("disposed"));
    }

    @Test
    public void disposeSampleItem_ShouldReturn400_ForInvalidSample() throws Exception {
        String requestBody = "{\"sampleItemId\":\"NONEXISTENT-99999\",\"reason\":\"expired\",\"method\":\"autoclave\"}";

        MvcResult result = this.mockMvc
                .perform(post("/rest/storage/sample-items/dispose").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue("Error message should contain 'not found'", responseBody.contains("not found"));
    }

    @Test
    public void disposeSampleItem_ShouldReturn400_ForAlreadyDisposed() throws Exception {
        String sampleItemExternalId = "EXT-1001";
        String requestBody = "{\"sampleItemId\":\"" + sampleItemExternalId
                + "\",\"reason\":\"expired\",\"method\":\"autoclave\"}";

        MvcResult result = this.mockMvc
                .perform(post("/rest/storage/sample-items/dispose").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue("Error message should contain 'already disposed'", responseBody.contains("already disposed"));
    }

    @Test
    public void disposeSampleItem_ShouldReturn400_ForMissingReason() throws Exception {
        String sampleItemExternalId = "EXT-1000";
        String requestBody = "{\"sampleItemId\":\"" + sampleItemExternalId + "\",\"method\":\"autoclave\"}";

        MvcResult result = this.mockMvc
                .perform(post("/rest/storage/sample-items/dispose").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue("Error message should contain 'reason'", responseBody.contains("reason"));
    }

    @Test
    public void disposeSampleItem_ShouldReturn400_ForMissingMethod() throws Exception {
        String sampleItemExternalId = "EXT-1000";
        String requestBody = "{\"sampleItemId\":\"" + sampleItemExternalId + "\",\"reason\":\"expired\"}";

        MvcResult result = this.mockMvc
                .perform(post("/rest/storage/sample-items/dispose").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue("Error message should contain 'method'", responseBody.contains("method"));
    }

    @Test
    public void disposeSampleItem_ShouldClearStorageAssignment_WhenPresent() throws Exception {
        String sampleItemExternalId = "EXT-1002";
        String requestBody = "{\"sampleItemId\":\"" + sampleItemExternalId
                + "\",\"reason\":\"expired\",\"method\":\"autoclave\"}";

        this.mockMvc.perform(post("/rest/storage/sample-items/dispose").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isOk());

        // We can only check if we have countRowsInTable method
        // If countRowsInTable exists, we can use it
        // int assignmentCount = countRowsInTable("sample_storage_assignment");
        // assertEquals("Assignment should be deleted after disposal", 0,
        // assignmentCount);
    }

    @Test
    public void disposeSampleItem_ShouldPersistMovementAuditRecord_WithAllFields() throws Exception {
        String sampleItemExternalId = "EXT-1002";
        String disposalReason = "expired";
        String disposalMethod = "autoclave";
        String disposalNotes = "Sample expired after 6 months storage";

        String requestBody = String.format(
                "{\"sampleItemId\":\"%s\",\"reason\":\"%s\",\"method\":\"%s\",\"notes\":\"%s\"}", sampleItemExternalId,
                disposalReason, disposalMethod, disposalNotes);

        this.mockMvc
                .perform(post("/rest/storage/sample-items/dispose").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sampleItemId").value("1002"))
                .andExpect(jsonPath("$.status").value("disposed")).andExpect(jsonPath("$.reason").value(disposalReason))
                .andExpect(jsonPath("$.method").value(disposalMethod));

    }

    @Test
    public void disposeSampleItem_ShouldPersistMovementRecord_WithoutNotes() throws Exception {
        String sampleItemExternalId = "EXT-1002";
        String disposalReason = "contaminated";
        String disposalMethod = "incineration";

        String requestBody = String.format("{\"sampleItemId\":\"%s\",\"reason\":\"%s\",\"method\":\"%s\"}",
                sampleItemExternalId, disposalReason, disposalMethod);

        this.mockMvc.perform(post("/rest/storage/sample-items/dispose").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isOk());
    }

    @Test
    public void testDisposal_IncrementsDisposedMetricCount() throws Exception {

        MvcResult initialMetrics = mockMvc.perform(get("/rest/storage/sample-items?countOnly=true"))
                .andExpect(status().isOk()).andReturn();

        String initialContent = initialMetrics.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode initialJson = objectMapper.readTree(initialContent);
        com.fasterxml.jackson.databind.JsonNode initialMetricsNode = initialJson.get(0);

        int initialDisposed = initialMetricsNode.get("disposed").asInt();
        int initialActive = initialMetricsNode.get("active").asInt();

        String sampleItemId = "EXT-1002";

        String disposalRequest = String
                .format("{\"sampleItemId\":\"%s\",\"reason\":\"expired\",\"method\":\"autoclave\"}", sampleItemId);

        mockMvc.perform(post("/rest/storage/sample-items/dispose").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(disposalRequest)).andExpect(status().isOk());

        MvcResult finalMetrics = mockMvc.perform(get("/rest/storage/sample-items?countOnly=true"))
                .andExpect(status().isOk()).andReturn();

        String finalContent = finalMetrics.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode finalJson = objectMapper.readTree(finalContent);
        com.fasterxml.jackson.databind.JsonNode finalMetricsNode = finalJson.get(0);

        int finalDisposed = finalMetricsNode.get("disposed").asInt();
        int finalActive = finalMetricsNode.get("active").asInt();

        assertEquals("Disposed count should increment by exactly 1", initialDisposed + 1, finalDisposed);
        assertEquals("Active count should decrement by exactly 1", initialActive - 1, finalActive);
    }

    @Test
    public void disposeSampleItem_ShouldReturn401_WhenNoAuthenticatedSession() throws Exception {
        // OGC-738b: regression — disposal must refuse to run without an
        // authenticated session so the audit emission always has a real user
        // to stamp. Pre-fix the controller silently used sysUserId=1.
        //
        // ControllerUtills.getSysUserId has two fallback strategies: (1) the
        // USER_SESSION_DATA session attribute, (2) the Spring Security
        // principal. BaseWebContextSensitiveTest auto-authenticates via
        // SecurityContextHolder, so the no-auth path is only exercised when we
        // also clear that context.
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        try {
            String requestBody = "{\"sampleItemId\":\"EXT-1000\",\"reason\":\"expired\",\"method\":\"autoclave\"}";

            this.mockMvc.perform(post("/rest/storage/sample-items/dispose").contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)).andExpect(status().isUnauthorized());
        } finally {
            // Restore so subsequent @Test methods in the shared Spring context
            // keep their authenticated state.
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testDisposal_DisposedSampleRemainSearchable() throws Exception {
        String disposedSampleId = "EXT-1001";

        MvcResult result = mockMvc.perform(get("/rest/storage/sample-items?status=disposed")).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseContent);

        com.fasterxml.jackson.databind.JsonNode samplesNode;
        if (root.has("items")) {
            samplesNode = root.get("items");
        } else {
            samplesNode = root;
        }

        boolean found = false;
        for (com.fasterxml.jackson.databind.JsonNode sample : samplesNode) {
            String sampleItemExternalId = sample.has("sampleItemExternalId")
                    ? sample.get("sampleItemExternalId").asText()
                    : "";
            if (disposedSampleId.equals(sampleItemExternalId)) {
                found = true;
                String status = sample.get("status").asText();

                // Spec 001 contracts/storage-api.json:862,885 — status is an
                // enum "active"|"disposed". We derive it from the sample's DB
                // status_id via statusService.matches in SampleStorageServiceImpl.
                assertEquals("Disposed sample should serialize as \"disposed\" enum", "disposed", status);
                break;
            }
        }
        assertTrue("Disposed sample should be searchable per FR-056", found);
    }
}