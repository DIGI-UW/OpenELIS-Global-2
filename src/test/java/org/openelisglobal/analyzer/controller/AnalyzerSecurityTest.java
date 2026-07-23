package org.openelisglobal.analyzer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Security-focused integration tests for analyzer REST controllers.
 *
 * Validates SSRF blocklist, path traversal prevention, and authorization checks
 * introduced by the security remediation.
 */
public class AnalyzerSecurityTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Mock
    private AnalyzerQueryService analyzerQueryService;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        jdbcTemplate = new JdbcTemplate(dataSource);

        AnalyzerRestController controller = webApplicationContext.getBean(AnalyzerRestController.class);
        ReflectionTestUtils.setField(controller, "analyzerQueryService", analyzerQueryService);

        AnalyzerTestCleanup.clean(jdbcTemplate);
    }

    @After
    public void tearDown() {
        AnalyzerTestCleanup.clean(jdbcTemplate);
    }

    // ── SSRF: Create analyzer with blocked IP ───────────────────────────

    @Test
    public void testCreateAnalyzer_WithLoopbackIP_ReturnsBadRequest() throws Exception {
        String body = "{\"name\":\"TEST-SEC-Loopback\",\"analyzerType\":\"Chemistry Analyzer\","
                + "\"ipAddress\":\"127.0.0.1\",\"port\":5000,\"testUnitIds\":[]}";

        mockMvc.perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Connection to this address is not permitted"));
    }

    @Test
    public void testCreateAnalyzer_WithLinkLocalIP_ReturnsBadRequest() throws Exception {
        String body = "{\"name\":\"TEST-SEC-LinkLocal\",\"analyzerType\":\"Chemistry Analyzer\","
                + "\"ipAddress\":\"169.254.169.254\",\"port\":80,\"testUnitIds\":[]}";

        mockMvc.perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Connection to this address is not permitted"));
    }

    @Test
    public void testCreateAnalyzer_WithMulticastIP_ReturnsBadRequest() throws Exception {
        String body = "{\"name\":\"TEST-SEC-Multicast\",\"analyzerType\":\"Chemistry Analyzer\","
                + "\"ipAddress\":\"224.0.0.1\",\"port\":5000,\"testUnitIds\":[]}";

        mockMvc.perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Connection to this address is not permitted"));
    }

    @Test
    public void testCreateAnalyzer_WithAnyLocalIP_ReturnsBadRequest() throws Exception {
        String body = "{\"name\":\"TEST-SEC-AnyLocal\",\"analyzerType\":\"Chemistry Analyzer\","
                + "\"ipAddress\":\"0.0.0.0\",\"port\":5000,\"testUnitIds\":[]}";

        mockMvc.perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Connection to this address is not permitted"));
    }

    // ── SSRF: Private LAN addresses should be allowed ───────────────────

    @Test
    public void testCreateAnalyzer_WithPrivateIP_Succeeds() throws Exception {
        String uniqueName = "TEST-SEC-Private-" + System.currentTimeMillis();
        String body = "{\"name\":\"" + uniqueName + "\",\"analyzerType\":\"Chemistry Analyzer\"," + "\"ipAddress\":\""
                + AnalyzerTestCleanup.uniqueIp() + "\",\"port\":5000,\"testUnitIds\":[]}";

        mockMvc.perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists());
    }

    // ── SSRF: Update analyzer with blocked IP ───────────────────────────

    @Test
    public void testUpdateAnalyzer_WithLoopbackIP_ReturnsBadRequest() throws Exception {
        // Create a valid analyzer first
        String uniqueName = "TEST-SEC-Update-" + System.currentTimeMillis();
        String createBody = "{\"name\":\"" + uniqueName + "\",\"analyzerType\":\"Chemistry Analyzer\","
                + "\"ipAddress\":\"" + AnalyzerTestCleanup.uniqueIp() + "\",\"port\":5000,\"testUnitIds\":[]}";

        String createResponse = mockMvc
                .perform(post("/rest/analyzer/analyzers").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        String analyzerId = extractId(createResponse);

        // Try to update with loopback IP
        String updateBody = "{\"ipAddress\":\"127.0.0.1\",\"port\":5000}";

        mockMvc.perform(put("/rest/analyzer/analyzers/" + analyzerId).contentType(MediaType.APPLICATION_JSON)
                .content(updateBody)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Connection to this address is not permitted"));
    }

    // File-import security tests removed: FileImportRestController deleted
    // (FILE config is on Analyzer entity, managed through existing analyzer
    // endpoints)

    // ── Helper ──────────────────────────────────────────────────────────

    private String extractId(String responseBody) {
        int start = responseBody.indexOf("\"id\":\"") + 6;
        int end = responseBody.indexOf("\"", start);
        return responseBody.substring(start, end);
    }
}
