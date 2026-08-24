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
                .andExpect(jsonPath("$.analyzerId").value("77"))
                .andExpect(jsonPath("$.profileRef.profileId").value("genexpert-astm"))
                .andExpect(jsonPath("$.dataFlow").value("TWO_WAY"))
                .andExpect(jsonPath("$.outcome").value("TIMEOUT"))
                .andExpect(jsonPath("$.configureEndpoint.host").value("bridge.lab.example"))
                .andExpect(jsonPath("$.resultsOnlyAvailable").value(true));
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
        return new AnalyzerConnectionProbeView("1.0", "77",
                new AnalyzerConnectionProbeView.ProfileRef("genexpert-astm", 1), "sha256:" + "6".repeat(64),
                new AnalyzerConnectionProbeView.Connection("TCP", "RECEIVER"), "TWO_WAY", "TIMEOUT",
                new AnalyzerConnectionProbeView.ConfigureEndpoint("NETWORK", "bridge.lab.example", 12001, null, null),
                true,
                List.of(new AnalyzerConnectionProbeView.Check("LISTENER", "PASSED", "listener.ready", 3, Map.of())));
    }
}
