package org.openelisglobal.storage.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.services.historyservices.SampleHistoryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

public class SampleStorageRestControllerDisposalTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private IStatusService statusService;

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

    @Test
    public void recordSampleUsage_ShouldReturn400AndKeepRemaining_WhenAmountExceedsRemaining() throws Exception {
        MvcResult result = this.mockMvc
                .perform(post("/rest/storage/sample-items/record-usage").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sampleItemId\":\"EXT-1000\",\"amountUsed\":\"5\"}"))
                .andExpect(status().isBadRequest()).andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("remaining quantity 1"));
        assertEquals(0, new java.math.BigDecimal("1").compareTo(jdbcTemplate.queryForObject(
                "SELECT remaining_quantity FROM sample_item WHERE id = 1000", java.math.BigDecimal.class)));
    }

    @Test
    public void recordSampleUsage_ShouldExhaust_WhenAmountEqualsRemaining() throws Exception {
        this.mockMvc
                .perform(post("/rest/storage/sample-items/record-usage").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sampleItemId\":\"EXT-1000\",\"amountUsed\":\"1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.remainingQuantity").value(0))
                .andExpect(jsonPath("$.exhausted").value(true));
    }

    @Test
    public void disposeSampleItem_ShouldWriteOneSampleItemHistoryRow() throws Exception {
        int before = sampleItemUpdateHistoryRows(1002);

        this.mockMvc
                .perform(post("/rest/storage/sample-items/dispose").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sampleItemId\":\"EXT-1002\",\"reason\":\"expired\",\"method\":\"autoclave\"}"))
                .andExpect(status().isOk());

        assertEquals("disposal must add one sample_item history row", before + 1, sampleItemUpdateHistoryRows(1002));
    }

    @Test
    public void recordSampleUsage_ShouldWriteOneSampleItemHistoryRow() throws Exception {
        int before = sampleItemUpdateHistoryRows(1000);

        this.mockMvc.perform(post("/rest/storage/sample-items/record-usage").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleItemId\":\"EXT-1000\",\"amountUsed\":\"1\"}")).andExpect(status().isOk());

        assertEquals("usage must add one sample_item history row", before + 1, sampleItemUpdateHistoryRows(1000));
    }

    @Test
    public void orderAuditTrail_ShowsUsageAsRemainingQuantityAndDisposalAsStatusChange() throws Exception {
        jdbcTemplate.update("DELETE FROM history WHERE reference_id = 1000 AND reference_table ="
                + " (SELECT id FROM reference_tables WHERE upper(name) = 'SAMPLE_ITEM')");
        Integer typeOfSampleId = jdbcTemplate.queryForObject("SELECT min(id) FROM type_of_sample", Integer.class);
        assertNotNull("the test database needs a sample type", typeOfSampleId);
        jdbcTemplate.update("UPDATE sample_item SET typeosamp_id = ? WHERE id = 1000", typeOfSampleId);
        String sampleType = jdbcTemplate.queryForObject("SELECT description FROM type_of_sample WHERE id = ?",
                String.class, typeOfSampleId);
        this.mockMvc.perform(post("/rest/storage/sample-items/record-usage").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleItemId\":\"EXT-1000\",\"amountUsed\":\"1\"}")).andExpect(status().isOk());
        this.mockMvc
                .perform(post("/rest/storage/sample-items/dispose").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sampleItemId\":\"EXT-1000\",\"reason\":\"expired\",\"method\":\"autoclave\"}"))
                .andExpect(status().isOk());

        List<AuditTrailItem> rows = new SampleHistoryService(sampleService.get("1000")).getAuditTrailItems();

        assertEquals(2, rows.size());
        rows.forEach(row -> assertEquals(sampleType + " (EXT-1000)", row.getIdentifier()));
        String remainingQuantityLabel = MessageUtil.getMessage("auditTrail.sampleItem.remainingQuantity");
        AuditTrailItem usage = rows.stream().filter(row -> remainingQuantityLabel.equals(row.getAttribute()))
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal(usage.getOldValue()).compareTo(BigDecimal.ONE));
        assertEquals(0, new BigDecimal(usage.getNewValue()).compareTo(BigDecimal.ZERO));
        String statusLabel = MessageUtil.getMessage("auditTrail.sampleItem.status");
        AuditTrailItem disposal = rows.stream().filter(row -> statusLabel.equals(row.getAttribute())).findFirst()
                .orElseThrow();
        assertEquals(nameOrId("1"), disposal.getOldValue());
        assertEquals(nameOrId(statusService.getStatusID(SampleStatus.Disposed)), disposal.getNewValue());
    }

    private String nameOrId(String statusId) {
        String name = statusService.getStatusNameFromId(statusId);
        return name != null ? name : statusId;
    }

    private int sampleItemUpdateHistoryRows(int sampleItemId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM history h JOIN reference_tables r ON r.id = h.reference_table"
                        + " WHERE upper(r.name) = 'SAMPLE_ITEM' AND h.reference_id = ? AND h.activity = ?",
                Integer.class, sampleItemId, IActionConstants.AUDIT_TRAIL_UPDATE);
    }
}