package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for AnalyzerRestController against the real Spring context,
 * real services, and real DB (via JdbcTemplate for setup + verification). No
 * mocks are injected into Spring-managed controllers by this class.
 *
 * <p>
 * Note on the audit trail: the global test config in
 * {@code AppTestConfig#auditTrailService} provides a Mockito mock of
 * {@link org.openelisglobal.audittrail.dao.AuditTrailService}. That mock
 * silently no-ops {@code saveHistory} calls, which means any integration test
 * that doesn't explicitly replace it is NOT exercising the audit path. The
 * {@link #testDeleteAnalyzer_FreshDbRow_PersistsWithoutAuditError()} regression
 * test installs a real {@link AuditTrailServiceImpl} for the duration of that
 * single method via try/finally to actually catch audit-trail regressions.
 *
 * <p>
 * Query/status endpoints that delegate to AnalyzerQueryService are NOT covered
 * here — those paths require a live TCP analyzer and are exercised by
 * AnalyzerQueryServiceIntegrationTest and related service-layer integration
 * tests running against the openelis-analyzer-mock container.
 */
public class AnalyzerRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;
    private String testIp;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        AnalyzerTestCleanup.clean(jdbcTemplate);
        testIp = AnalyzerTestCleanup.uniqueIp();
    }

    @After
    public void tearDown() {
        AnalyzerTestCleanup.clean(jdbcTemplate);
    }

    /**
     * Test: GET /rest/analyzer/analyzers returns list of analyzers
     */
    @Test
    public void testGetAnalyzers_ReturnsList() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.analyzers").isArray());
    }

    /**
     * Test: GET /rest/analyzer/analyzers includes the qcRules + controlLots fields
     * per analyzer (consumed by the bridge bootstrap to populate its registry). The
     * fields must always be present (possibly empty) so the bridge can rely on the
     * contract.
     */
    @Test
    public void testGetAnalyzers_IncludesQcRulesAndControlLotsFields() throws Exception {
        // Create one analyzer so the response is non-empty
        String uniqueName = "TEST-Fields-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName + "\",\"analyzerType\":\"Chemistry Analyzer\",\"ipAddress\":\""
                + testIp + "\"," + "\"port\":5000,\"testUnitIds\":[]}";
        mockMvc.perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated());

        // Both fields must appear on every analyzer entry in the response.
        // qcRules is always emitted (existing FR-15 contract); controlLots is
        // emitted whenever QCControlLotService is wired AND the analyzer.id is
        // numeric — both true in the standard webapp deployment.
        mockMvc.perform(get("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.analyzers").isArray()).andExpect(jsonPath("$.analyzers[0].qcRules").exists())
                .andExpect(jsonPath("$.analyzers[0].qcRules").isArray())
                .andExpect(jsonPath("$.analyzers[0].controlLots").exists())
                .andExpect(jsonPath("$.analyzers[0].controlLots").isArray());
    }

    /**
     * Test: POST /rest/analyzer/analyzers creates analyzer with valid data
     */
    @Test
    public void testCreateAnalyzer_WithValidData_ReturnsCreated() throws Exception {
        // Arrange: Create analyzer form JSON
        String uniqueName = "TEST-Analyzer-" + System.currentTimeMillis();
        String requestBody = "{\"name\":\"" + uniqueName + "\",\"analyzerType\":\"Chemistry Analyzer\",\"ipAddress\":\""
                + testIp + "\"," + "\"port\":5000,\"testUnitIds\":[]}";

        // Act & Assert: Endpoint should create analyzer
        mockMvc.perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(uniqueName));
    }

    /**
     * Test: POST /rest/analyzer/analyzers/{id}/test-connection tests connection
     */
    @Test
    public void testTestConnection_WithValidConfig_ReturnsSuccess() throws Exception {
        // Arrange: Create analyzer first
        String uniqueName = "TEST-Connection-Test-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName + "\",\"analyzerType\":\"Chemistry Analyzer\",\"ipAddress\":\""
                + testIp + "\"," + "\"port\":5000,\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andReturn(); // Don't assert status yet - we'll check it

        int status = createResult.getResponse().getStatus();
        String responseBody = createResult.getResponse().getContentAsString();

        // Assert creation succeeded
        assertEquals("Analyzer creation should succeed", 201, status);

        // Extract analyzer ID from response (simplified parsing)
        String analyzerId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        analyzerId = analyzerId.substring(0, analyzerId.indexOf("\""));

        // Act & Assert: Test connection endpoint should work and return expected fields
        // Note: success will be false since there's no actual analyzer running
        mockMvc.perform(post("/rest/analyzer/analyzers/" + analyzerId + "/test-connection")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists()).andExpect(jsonPath("$.analyzerId").value(analyzerId))
                .andExpect(jsonPath("$.ipAddress").value(testIp)).andExpect(jsonPath("$.port").value(5000));
    }

    /**
     * Test: GET /rest/analyzer/analyzers/{id} returns analyzer by ID
     */
    @Test
    public void testGetAnalyzer_WithValidId_ReturnsAnalyzer() throws Exception {
        // Arrange: Create analyzer first
        String uniqueName = "TEST-Get-Test-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName + "\",\"analyzerType\":\"Chemistry Analyzer\",\"ipAddress\":\""
                + testIp + "\"," + "\"port\":5000,\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                new TypeReference<Map<String, Object>>() {
                });
        String analyzerId = String.valueOf(responseMap.get("id"));

        // Act & Assert: GET endpoint should return analyzer
        mockMvc.perform(get("/rest/analyzer/analyzers/" + analyzerId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(analyzerId))
                .andExpect(jsonPath("$.name").value(uniqueName));
    }

    /**
     * Test: GET /rest/analyzer/analyzers/{id} returns 404 for non-existent analyzer
     */
    @Test
    public void testGetAnalyzer_WithInvalidId_ReturnsNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/rest/analyzer/analyzers/99999").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * Test: PUT /rest/analyzer/analyzers/{id} updates analyzer
     */
    @Test
    public void testUpdateAnalyzer_WithValidData_ReturnsUpdated() throws Exception {
        // Arrange: Create analyzer first
        String uniqueName = "TEST-Update-Test-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName + "\",\"analyzerType\":\"Chemistry Analyzer\",\"ipAddress\":\""
                + testIp + "\"," + "\"port\":5000,\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                new TypeReference<Map<String, Object>>() {
                });
        String analyzerId = String.valueOf(responseMap.get("id"));

        // Update analyzer
        String updateBody = "{\"name\":\"Updated Name\",\"analyzerType\":\"Hematology Analyzer\",\"active\":false}";

        // Act & Assert: PUT endpoint should update analyzer
        mockMvc.perform(put("/rest/analyzer/analyzers/" + analyzerId).contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(analyzerId))
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    /**
     * Test: POST /rest/analyzer/analyzers/{id}/delete soft-deletes an analyzer that
     * was just created in the same test.
     *
     * <p>
     * This covers the happy path where the analyzer is still live in the Hibernate
     * L1 cache from the prior create call. See
     * {@link #testDeleteAnalyzer_FreshDbRow_PersistsWithoutAuditError()} for the
     * production-realistic path where the analyzer exists in the DB from a prior
     * HTTP session.
     */
    @Test
    public void testDeleteAnalyzer_WithValidId_ReturnsNoContent() throws Exception {
        // Arrange: Create analyzer first
        String uniqueName = "TEST-Delete-Test-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName + "\",\"analyzerType\":\"Chemistry Analyzer\",\"ipAddress\":\""
                + testIp + "\"," + "\"port\":5000,\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                new TypeReference<Map<String, Object>>() {
                });
        String analyzerId = String.valueOf(responseMap.get("id"));

        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        mockMvc.perform(post("/rest/analyzer/analyzers/" + analyzerId + "/delete")
                .contentType(MediaType.APPLICATION_JSON).sessionAttr(IActionConstants.USER_SESSION_DATA, usd))
                .andExpect(status().isOk()).andExpect(jsonPath("$.deleted").value(true));
    }

    /**
     * Regression test for the audit-trail sysUserId bug fixed in v6.1.
     *
     * <p>
     * Production scenario: an analyzer already exists in the DB from a prior HTTP
     * session. An admin presses "Delete" in the UI. The controller fetches the
     * analyzer fresh from the DB, so every transient field (including
     * {@code sysUserId} on
     * {@link org.openelisglobal.common.valueholder.BaseObject}) is {@code null}.
     * Without an explicit {@code setSysUserId} before
     * {@code analyzerService.update(...)}, the real audit-trail writer throws
     * {@code LIMSRuntimeException: System User ID is null} and the delete 500s.
     *
     * <p>
     * Two things are needed to make this reproducible in-process:
     * <ol>
     * <li>Insert the analyzer row directly via {@link JdbcTemplate} so Hibernate's
     * L1 cache never sees it — otherwise a prior inline create would leave the
     * transient {@code sysUserId} populated in memory and hide the bug.</li>
     * <li>Temporarily swap the Mockito-mocked {@code AuditTrailService} (installed
     * by {@code AppTestConfig}) for a real {@link AuditTrailServiceImpl} — without
     * this, {@code saveHistory} is a no-op and the null-user check never runs. The
     * mock is restored in a finally block so other tests in this class are
     * unaffected.</li>
     * </ol>
     */
    @Test
    public void testDeleteAnalyzer_FreshDbRow_PersistsWithoutAuditError() throws Exception {
        // Install a real AuditTrailService on the analyzerService for this test
        // only. AppTestConfig#auditTrailService provides a Mockito mock that
        // silently swallows saveHistory calls; we need the real writer here so
        // the null-sysUserId path actually throws when the bug is present.
        Object analyzerServiceTarget = AopTestUtils.getUltimateTargetObject(analyzerService);
        Object originalAuditTrailService = ReflectionTestUtils.getField(analyzerServiceTarget, "auditTrailService");
        AuditTrailServiceImpl realAuditTrailService = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(realAuditTrailService, "referenceTablesService", referenceTablesService);
        ReflectionTestUtils.setField(realAuditTrailService, "historyService", historyService);
        ReflectionTestUtils.setField(analyzerServiceTarget, "auditTrailService", realAuditTrailService);

        try {
            // Guard: restore shared seed data that prior test classes may have
            // truncated (SystemAuditTrailIntegrationTest truncates reference_tables
            // and system_user via cleanRowsInCurrentConnection). Without these rows,
            // the real AuditTrailServiceImpl can't resolve the "analyzer" reference
            // table or the sys_user FK when writing history.
            jdbcTemplate.update(
                    "INSERT INTO clinlims.reference_tables " + "(id, name, keep_history, is_hl7_encoded, lastupdated) "
                            + "VALUES (182, 'analyzer', 'Y', 'N', now()) ON CONFLICT (id) DO NOTHING");
            jdbcTemplate.update("INSERT INTO clinlims.system_user "
                    + "(id, login_name, last_name, first_name, is_active, is_employee, external_id, lastupdated) "
                    + "VALUES (1, 'admin', 'admin', 'admin', 'Y', 'Y', 'admin', now()) ON CONFLICT (id) DO NOTHING");

            // Arrange: insert analyzer row directly via JDBC, bypassing Hibernate.
            // analyzer_type_id is intentionally NULL — the delete path doesn't
            // care about the type, and the test DB doesn't seed analyzer_type.
            String uniqueName = "TEST-DeleteFreshDb-" + System.currentTimeMillis();
            Integer analyzerId = jdbcTemplate.queryForObject("SELECT nextval('analyzer_seq')", Integer.class);
            jdbcTemplate.update("INSERT INTO clinlims.analyzer (id, name, is_active, status, last_updated) "
                    + "VALUES (?, ?, true, 'ACTIVE', now())", analyzerId, uniqueName);

            // Act: hit the delete endpoint exactly as the UI does — the session
            // attribute carries the logged-in user.
            UserSessionData usd = new UserSessionData();
            usd.setSytemUserId(1);
            mockMvc.perform(post("/rest/analyzer/analyzers/" + analyzerId + "/delete")
                    .contentType(MediaType.APPLICATION_JSON).sessionAttr(IActionConstants.USER_SESSION_DATA, usd))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.deleted").value(true));

            // Assert: soft-delete landed in the DB. If the sysUserId bug fires,
            // the real audit writer throws LIMSRuntimeException before the update
            // commits, the controller's catch block returns 500, and the status
            // assertion above fails.
            Map<String, Object> row = jdbcTemplate
                    .queryForMap("SELECT status, is_active FROM clinlims.analyzer WHERE id = ?", analyzerId);
            assertEquals("DELETED", row.get("status"));
            assertEquals(Boolean.FALSE, row.get("is_active"));

            // Assert: a history row landed, attributed to the session user.
            // This verifies the audit trail actually ran (not just that the
            // delete didn't throw).
            List<Map<String, Object>> historyRows = jdbcTemplate.queryForList(
                    "SELECT sys_user_id, activity FROM clinlims.history WHERE reference_id = ? AND reference_table ="
                            + " (SELECT id FROM clinlims.reference_tables WHERE name = 'analyzer')",
                    analyzerId);
            assertFalse("Expected at least one audit-trail history row for the deleted analyzer",
                    historyRows.isEmpty());
            boolean attributedToUser1 = historyRows.stream()
                    .anyMatch(r -> "1".equals(String.valueOf(r.get("sys_user_id"))));
            assertTrue("Expected a history row attributed to sys_user_id=1, got: " + historyRows, attributedToUser1);
        } finally {
            // Restore the mocked AuditTrailService so sibling tests see the
            // default (mocked) behavior.
            ReflectionTestUtils.setField(analyzerServiceTarget, "auditTrailService", originalAuditTrailService);
        }
    }

    /**
     * Test: GET /rest/analyzer/analyzers includes pluginLoaded field in each entry.
     * For test-created analyzers (no matching plugin JAR), pluginLoaded should be
     * false.
     *
     * Verifies R1 fix: pluginLoaded field is always present in analyzer responses.
     */
    @Test
    public void testGetAnalyzers_ResponseIncludesPluginLoadedField() throws Exception {
        // Arrange: Create a test analyzer
        String uniqueName = "TEST-PluginLoaded-List-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName + "\",\"analyzerType\":\"Chemistry Analyzer\",\"ipAddress\":\""
                + testIp + "\"," + "\"port\":5000,\"testUnitIds\":[]}";

        mockMvc.perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated());

        // Act
        MvcResult listResult = mockMvc.perform(get("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert: Each entry should have a pluginLoaded field
        String responseBody = listResult.getResponse().getContentAsString();
        Map<String, Object> envelope = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {
        });
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> analyzers = (List<Map<String, Object>>) envelope.get("analyzers");
        assertNotNull("Response should contain analyzers array", analyzers);
        assertFalse("Response should contain at least one analyzer", analyzers.isEmpty());
        for (Map<String, Object> analyzerMap : analyzers) {
            assertTrue("Each analyzer should have pluginLoaded field", analyzerMap.containsKey("pluginLoaded"));
        }
    }

    /**
     * Test: GET /rest/analyzer/analyzers/{id} includes pluginLoaded=false when no
     * plugin JAR is loaded for the analyzer.
     *
     * Verifies R1 fix: pluginLoaded is false (not missing or error) for analyzers
     * without a loaded plugin.
     */
    @Test
    public void testGetAnalyzer_PluginLoadedFalse_WhenNoMatchingPlugin() throws Exception {
        // Arrange: Create analyzer (no real plugin JAR loaded for "Chemistry Analyzer")
        String uniqueName = "TEST-PluginLoaded-False-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName + "\",\"analyzerType\":\"Chemistry Analyzer\",\"ipAddress\":\""
                + testIp + "\"," + "\"port\":5000,\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                new TypeReference<Map<String, Object>>() {
                });
        String analyzerId = String.valueOf(responseMap.get("id"));

        // Act & Assert: pluginLoaded should be false (no plugin JAR loaded)
        mockMvc.perform(get("/rest/analyzer/analyzers/" + analyzerId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pluginLoaded").value(false));
    }

    /**
     * Test: POST /rest/analyzer/analyzers with non-numeric pluginTypeId should not
     * throw NumberFormatException.
     *
     * Regression test for "For input string: generic-astm" bug: the frontend
     * fallback list used hardcoded string IDs like "generic-astm" instead of
     * database numeric IDs. The backend should gracefully resolve these by name
     * rather than crashing with a 500.
     */
    @Test
    public void testCreateAnalyzer_WithNonNumericPluginTypeId_ReturnsCreated() throws Exception {
        String uniqueName = "TEST-NonNumericPlugin-" + System.currentTimeMillis();
        String requestBody = "{\"name\":\"" + uniqueName + "\",\"analyzerType\":\"MOLECULAR\","
                + "\"pluginTypeId\":\"generic-astm\"," + "\"ipAddress\":\"" + testIp + "\",\"port\":1200}";

        // Should return 201 (gracefully ignoring unresolvable pluginTypeId)
        // instead of 500 NumberFormatException
        mockMvc.perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(uniqueName));
    }

    /**
     * Test: PUT /rest/analyzer/analyzers/{id} with non-numeric pluginTypeId should
     * not throw NumberFormatException.
     *
     * Same regression test as above, but for the update path.
     */
    @Test
    public void testUpdateAnalyzer_WithNonNumericPluginTypeId_ReturnsOk() throws Exception {
        // Arrange: Create analyzer first (without pluginTypeId)
        String uniqueName = "TEST-NonNumericPluginUpdate-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName + "\",\"analyzerType\":\"MOLECULAR\"," + "\"ipAddress\":\""
                + testIp + "\",\"port\":1200}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                new TypeReference<Map<String, Object>>() {
                });
        String analyzerId = String.valueOf(responseMap.get("id"));

        // Act: Update with non-numeric pluginTypeId "generic-astm"
        String updateBody = "{\"pluginTypeId\":\"generic-astm\"}";

        // Should return 200 (gracefully resolving by name) instead of 500
        mockMvc.perform(put("/rest/analyzer/analyzers/" + analyzerId).contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(analyzerId));
    }

    /**
     * Test: POST /rest/analyzer/analyzers/{id}/test-connection with HL7 protocol
     * returns real connectivity result (not hardcoded success)
     */
    @Test
    public void testTestConnection_WithHl7Protocol_ReturnsExpectedFields() throws Exception {
        String uniqueName = "TEST-HL7-Connection-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName + "\",\"analyzerType\":\"HEMATOLOGY\",\"ipAddress\":\"" + testIp
                + "\"," + "\"port\":5380,\"protocolVersion\":\"HL7_V2_3_1\","
                + "\"communicationMode\":\"ANALYZER_INITIATED\",\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andReturn();
        assertEquals("HL7 analyzer creation should succeed", 201, createResult.getResponse().getStatus());

        String responseBody = createResult.getResponse().getContentAsString();
        String analyzerId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        analyzerId = analyzerId.substring(0, analyzerId.indexOf("\""));

        // Test connection: should return real result (not hardcoded success)
        mockMvc.perform(post("/rest/analyzer/analyzers/" + analyzerId + "/test-connection")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists()).andExpect(jsonPath("$.analyzerId").value(analyzerId))
                .andExpect(jsonPath("$.ipAddress").value(testIp)).andExpect(jsonPath("$.port").value(5380))
                .andExpect(jsonPath("$.communicationMode").value("ANALYZER_INITIATED"))
                .andExpect(jsonPath("$.protocol").value("HL7_V2_3_1"))
                // Must have TCP reachability info (not just hardcoded success)
                .andExpect(jsonPath("$.tcpReachable").exists()).andExpect(jsonPath("$.message").exists());
    }

    /**
     * Test: POST /rest/analyzer/analyzers/{id}/test-connection with HL7 protocol
     * and no IP/port returns proper error
     */
    @Test
    public void testTestConnection_WithHl7Protocol_NoIpPort_ReturnsConfigError() throws Exception {
        String uniqueName = "TEST-HL7-NoIP-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName
                + "\",\"analyzerType\":\"HEMATOLOGY\",\"protocolVersion\":\"HL7_V2_3_1\",\"testUnitIds\":[]}";

        MvcResult createResult = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andReturn();
        assertEquals(201, createResult.getResponse().getStatus());

        String responseBody = createResult.getResponse().getContentAsString();
        String analyzerId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        analyzerId = analyzerId.substring(0, analyzerId.indexOf("\""));

        // Test connection without IP/port should fail with config error
        mockMvc.perform(post("/rest/analyzer/analyzers/" + analyzerId + "/test-connection")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.message").exists());
    }

    /**
     * Test: CommunicationMode is persisted and returned in analyzer response
     */
    @Test
    public void testCreateAnalyzer_WithCommunicationMode_PersistsMode() throws Exception {
        String uniqueName = "TEST-CommMode-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName
                + "\",\"analyzerType\":\"CHEMISTRY\",\"ipAddress\":\"192.168.1.50\","
                + "\"port\":6001,\"protocolVersion\":\"HL7_V2_3_1\","
                + "\"communicationMode\":\"ANALYZER_INITIATED\",\"testUnitIds\":[]}";

        MvcResult result = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String analyzerId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        analyzerId = analyzerId.substring(0, analyzerId.indexOf("\""));

        // Verify GET returns communicationMode
        mockMvc.perform(get("/rest/analyzer/analyzers/" + analyzerId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.communicationMode").value("ANALYZER_INITIATED"))
                .andExpect(jsonPath("$.effectiveCommunicationMode").value("ANALYZER_INITIATED"));
    }

    /**
     * Test: Null communicationMode defaults to ANALYZER_INITIATED in effective mode
     */
    @Test
    public void testCreateAnalyzer_WithNullCommunicationMode_DefaultsToAnalyzerInitiated() throws Exception {
        String uniqueName = "TEST-NullCommMode-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName + "\",\"analyzerType\":\"MOLECULAR\",\"testUnitIds\":[]}";

        MvcResult result = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String analyzerId = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6);
        analyzerId = analyzerId.substring(0, analyzerId.indexOf("\""));

        // communicationMode should be null (not set), but effective should default
        mockMvc.perform(get("/rest/analyzer/analyzers/" + analyzerId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveCommunicationMode").value("ANALYZER_INITIATED"));
    }

    // === OGC-526: Discovered sources endpoint tests ===

    @Test
    public void testDiscoveredSources_CreatesStubAnalyzer() throws Exception {
        String body = "{\"sourceId\":\"" + AnalyzerTestCleanup.uniqueSourceId()
                + "\",\"protocol\":\"ASTM\",\"transport\":\"TCP\",\"protocolHint\":\"GENEXPERT\"}";

        MvcResult result = mockMvc
                .perform(
                        post("/rest/analyzer/discovered-sources").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();

        Map<String, Object> response = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertNotNull("Should return analyzerId", response.get("analyzerId"));
        assertEquals("PENDING_REGISTRATION", response.get("status"));
        assertEquals(false, response.get("alreadyExists"));
    }

    @Test
    public void testDiscoveredSources_CreatesStubAnalyzerWithFhirUuid() throws Exception {
        String sourceId = "10.0.0.52";
        String body = "{\"sourceId\":\"" + sourceId
                + "\",\"protocol\":\"ASTM\",\"transport\":\"TCP\",\"protocolHint\":\"GENEXPERT\"}";

        MvcResult result = mockMvc
                .perform(
                        post("/rest/analyzer/discovered-sources").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();

        Map<String, Object> response = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        String analyzerId = String.valueOf(response.get("analyzerId"));

        String fhirUuid = jdbcTemplate.queryForObject("SELECT fhir_uuid::text FROM analyzer WHERE id = ?", String.class,
                Integer.valueOf(analyzerId));
        assertNotNull("Discovered-source stub should persist a fhir_uuid", fhirUuid);
        assertFalse("Discovered-source stub fhir_uuid should not be blank", fhirUuid.trim().isEmpty());
    }

    @Test
    public void testDiscoveredSources_IdempotentOnDuplicateSourceId() throws Exception {
        String srcId = AnalyzerTestCleanup.uniqueSourceId();
        String body = "{\"sourceId\":\"" + srcId + "\",\"protocol\":\"HL7\",\"transport\":\"MLLP\"}";

        // First call — creates stub
        MvcResult first = mockMvc
                .perform(
                        post("/rest/analyzer/discovered-sources").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();

        Map<String, Object> firstResponse = objectMapper.readValue(first.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        String firstId = String.valueOf(firstResponse.get("analyzerId"));

        // Second call — returns existing stub
        MvcResult second = mockMvc
                .perform(
                        post("/rest/analyzer/discovered-sources").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();

        Map<String, Object> secondResponse = objectMapper.readValue(second.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals("Same analyzer ID on duplicate", firstId, String.valueOf(secondResponse.get("analyzerId")));
        assertEquals(true, secondResponse.get("alreadyExists"));
    }

    @Test
    public void testDiscoveredSources_MissingSourceId_Returns400() throws Exception {
        String body = "{\"protocol\":\"ASTM\",\"transport\":\"TCP\"}";

        mockMvc.perform(post("/rest/analyzer/discovered-sources").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
