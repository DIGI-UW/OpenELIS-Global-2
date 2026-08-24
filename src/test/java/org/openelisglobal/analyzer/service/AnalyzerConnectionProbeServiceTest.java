package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerConnectionProbeServiceTest {

    private static final String ANALYZER_ID = "77";
    private static final String PROFILE_ID = "genexpert-astm";
    private static final int PROFILE_REVISION = 1;
    private static final String FINGERPRINT = "sha256:" + "6".repeat(64);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private BridgeRegistrationService registrationService;

    @Mock
    private BridgeHttpClient bridgeHttpClient;

    private AnalyzerConnectionProbeService service;
    private Analyzer analyzer;

    @Before
    public void setUp() {
        service = new AnalyzerConnectionProbeService(analyzerService, registrationService, bridgeHttpClient,
                "https://bridge.example/");
        analyzer = new Analyzer();
        analyzer.setId(ANALYZER_ID);
    }

    @Test
    public void probesTheExactInactiveDraftWithoutSynchronizingRuntime() throws Exception {
        ObjectNode candidate = probeCandidate(FINGERPRINT);
        when(analyzerService.getWithType(ANALYZER_ID)).thenReturn(Optional.of(analyzer));
        when(registrationService.buildProbeRegistration(analyzer)).thenReturn(candidate);
        when(bridgeHttpClient.post(eq("https://bridge.example/api/analyzers/77/probe"),
                eq(JSON.writeValueAsString(candidate)), eq(Duration.ofSeconds(10))))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, probeFixture()));

        AnalyzerConnectionProbeView result = service.probe(ANALYZER_ID);

        assertEquals("1.0", result.schemaVersion());
        assertEquals(ANALYZER_ID, result.analyzerId());
        assertEquals(PROFILE_ID, result.profileRef().profileId());
        assertEquals(PROFILE_REVISION, result.profileRef().revision());
        assertEquals(FINGERPRINT, result.desiredStateFingerprint());
        assertEquals("TIMEOUT", result.outcome());
        assertTrue(result.resultsOnlyAvailable());
        verify(registrationService, never()).synchronize();
    }

    @Test
    public void rejectsProbeEvidenceForAStaleDraftCandidate() throws Exception {
        when(analyzerService.getWithType(ANALYZER_ID)).thenReturn(Optional.of(analyzer));
        when(registrationService.buildProbeRegistration(analyzer))
                .thenReturn(probeCandidate("sha256:" + "7".repeat(64)));
        when(bridgeHttpClient.post(any(String.class), any(String.class), any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, probeFixture()));

        AnalyzerConnectionProbeException exception = assertThrows(AnalyzerConnectionProbeException.class,
                () -> service.probe(ANALYZER_ID));

        assertEquals("analyzer.testConnection.bridge.staleEvidence", exception.messageKey());
    }

    @Test
    public void doesNotProbeAnUnknownAnalyzer() throws Exception {
        when(analyzerService.getWithType(ANALYZER_ID)).thenReturn(Optional.empty());

        AnalyzerConnectionProbeException exception = assertThrows(AnalyzerConnectionProbeException.class,
                () -> service.probe(ANALYZER_ID));

        assertEquals("analyzer.testConnection.analyzerNotFound", exception.messageKey());
        verify(bridgeHttpClient, never()).post(any(String.class), any(), any(Duration.class));
    }

    @Test
    public void rejectsMalformedProbeEvidence() throws Exception {
        when(analyzerService.getWithType(ANALYZER_ID)).thenReturn(Optional.of(analyzer));
        when(registrationService.buildProbeRegistration(analyzer)).thenReturn(probeCandidate(FINGERPRINT));
        when(bridgeHttpClient.post(any(String.class), any(String.class), any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, "{\"schemaVersion\":\"1.0\"}"));

        AnalyzerConnectionProbeException exception = assertThrows(AnalyzerConnectionProbeException.class,
                () -> service.probe(ANALYZER_ID));

        assertEquals("analyzer.testConnection.bridge.invalidEvidence", exception.messageKey());
    }

    @Test
    public void reportsMissingBridgeConfigurationBeforeReadingTheDraft() throws Exception {
        service = new AnalyzerConnectionProbeService(analyzerService, registrationService, bridgeHttpClient, " ");

        AnalyzerConnectionProbeException exception = assertThrows(AnalyzerConnectionProbeException.class,
                () -> service.probe(ANALYZER_ID));

        assertEquals("analyzer.testConnection.bridge.notConfigured", exception.messageKey());
        verify(analyzerService, never()).getWithType(any(String.class));
        verify(bridgeHttpClient, never()).post(any(String.class), any(), any(Duration.class));
    }

    private static ObjectNode probeCandidate(String fingerprint) {
        ObjectNode candidate = JSON.createObjectNode();
        candidate.put("desiredStatus", "INACTIVE");
        candidate.put("desiredStateFingerprint", fingerprint);
        candidate.putObject("profileRef").put("profileId", PROFILE_ID).put("revision", PROFILE_REVISION);
        return candidate;
    }

    private static String probeFixture() throws Exception {
        return Files.readString(Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1", "fixtures",
                "connection-probe-result.json"));
    }
}
