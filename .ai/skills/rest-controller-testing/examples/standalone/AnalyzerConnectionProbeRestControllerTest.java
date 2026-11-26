package org.openelisglobal.analyzer.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AnalyzerConnectionProbeException;
import org.openelisglobal.analyzer.service.AnalyzerConnectionProbeService;
import org.openelisglobal.analyzer.service.AnalyzerConnectionProbeView;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerConnectionProbeRestControllerTest {

    @Mock
    private AnalyzerConnectionProbeService service;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AnalyzerConnectionProbeRestController(service)).build();
    }

    @Test
    public void returnsStructuredEvidenceForTheRegisteredAnalyzer() throws Exception {
        when(service.probe("77")).thenReturn(evidence());

        mockMvc.perform(post("/rest/analyzer/analyzers/77/test-connection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("1.0"))
                .andExpect(jsonPath("$.requestId").value("probe-4"))
                .andExpect(jsonPath("$.connectionId").value("bridge-7"))
                .andExpect(jsonPath("$.profileRef.profileId").value("fixture.synthetic-socket"))
                .andExpect(jsonPath("$.configRevision").value(4))
                .andExpect(jsonPath("$.nonMutating").value(true))
                .andExpect(jsonPath("$.status").value("TIMEOUT"));
    }

    @Test
    public void reportsMissingBridgeConfigurationAsServiceUnavailable() throws Exception {
        when(service.probe("77"))
                .thenThrow(new AnalyzerConnectionProbeException("analyzer.testConnection.bridge.notConfigured"));

        mockMvc.perform(post("/rest/analyzer/analyzers/77/test-connection"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.messageKey").value("analyzer.testConnection.bridge.notConfigured"));
    }

    @Test
    public void reportsAnUnknownAnalyzerAsNotFound() throws Exception {
        when(service.probe("77"))
                .thenThrow(new AnalyzerConnectionProbeException("analyzer.testConnection.analyzerNotFound"));

        mockMvc.perform(post("/rest/analyzer/analyzers/77/test-connection"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messageKey").value("analyzer.testConnection.analyzerNotFound"));
    }

    private static AnalyzerConnectionProbeView evidence() {
        return new AnalyzerConnectionProbeView("1.0", "probe-4", "bridge-7",
                new AnalyzerConnectionProbeView.ProfileRef("fixture.synthetic-socket", 2, "sha256:" + "1".repeat(64)),
                4, "sha256:" + "2".repeat(64), true, "TIMEOUT", "2026-08-24T19:05:03Z", "2026-08-24T19:05:04Z",
                List.of(new AnalyzerConnectionProbeView.Check("listener-bind", "PASSED", "listener.ready", 3,
                        Map.of())));
    }
}
