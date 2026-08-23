package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerConnectionProbeServiceTest {

    private static final String ANALYZER_ID = "77";
    private static final String PROFILE_ID = "genexpert-astm";
    private static final int PROFILE_REVISION = 1;
    private static final String FINGERPRINT = "sha256:" + "6".repeat(64);

    @Mock
    private BridgeRegistrationService registrationService;

    @Mock
    private BridgeHttpClient bridgeHttpClient;

    private AnalyzerConnectionProbeService service;

    @Before
    public void setUp() {
        service = new AnalyzerConnectionProbeService(registrationService, bridgeHttpClient, "https://bridge.example/");
    }

    @Test
    public void probesOnlyTheExactRegisteredAnalyzerWithoutCallerSuppliedSettings() throws Exception {
        when(registrationService.synchronize()).thenReturn(synchronizedCandidate(FINGERPRINT));
        when(bridgeHttpClient.post(eq("https://bridge.example/api/analyzers/77/probe"), isNull(),
                eq(Duration.ofSeconds(10)))).thenReturn(new BridgeHttpClient.BridgeResponse(200, probeFixture()));

        AnalyzerConnectionProbeView result = service.probe(ANALYZER_ID);

        assertEquals("1.0", result.schemaVersion());
        assertEquals(ANALYZER_ID, result.analyzerId());
        assertEquals(PROFILE_ID, result.profileRef().profileId());
        assertEquals(PROFILE_REVISION, result.profileRef().revision());
        assertEquals(FINGERPRINT, result.desiredStateFingerprint());
        assertEquals("TIMEOUT", result.outcome());
        assertTrue(result.resultsOnlyAvailable());
    }

    @Test
    public void rejectsProbeEvidenceForAStaleRegisteredCandidate() throws Exception {
        when(registrationService.synchronize()).thenReturn(synchronizedCandidate("sha256:" + "7".repeat(64)));
        when(bridgeHttpClient.post(any(String.class), isNull(), any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, probeFixture()));

        AnalyzerConnectionProbeException exception = assertThrows(AnalyzerConnectionProbeException.class,
                () -> service.probe(ANALYZER_ID));

        assertEquals("analyzer.testConnection.bridge.staleEvidence", exception.messageKey());
    }

    @Test
    public void doesNotProbeWhenTheCurrentCandidateWasNotAcknowledged() throws Exception {
        when(registrationService.synchronize())
                .thenReturn(new BridgeRegistrationResult(false, Set.of(), "Bridge unavailable", Map.of()));

        AnalyzerConnectionProbeException exception = assertThrows(AnalyzerConnectionProbeException.class,
                () -> service.probe(ANALYZER_ID));

        assertEquals("analyzer.testConnection.bridge.notSynchronized", exception.messageKey());
        verify(bridgeHttpClient, never()).post(any(String.class), any(), any(Duration.class));
    }

    @Test
    public void rejectsMalformedProbeEvidence() throws Exception {
        when(registrationService.synchronize()).thenReturn(synchronizedCandidate(FINGERPRINT));
        when(bridgeHttpClient.post(any(String.class), isNull(), any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, "{\"schemaVersion\":\"1.0\"}"));

        AnalyzerConnectionProbeException exception = assertThrows(AnalyzerConnectionProbeException.class,
                () -> service.probe(ANALYZER_ID));

        assertEquals("analyzer.testConnection.bridge.invalidEvidence", exception.messageKey());
    }

    @Test
    public void reportsMissingBridgeConfigurationBeforeAttemptingSynchronization() throws Exception {
        service = new AnalyzerConnectionProbeService(registrationService, bridgeHttpClient, " ");

        AnalyzerConnectionProbeException exception = assertThrows(AnalyzerConnectionProbeException.class,
                () -> service.probe(ANALYZER_ID));

        assertEquals("analyzer.testConnection.bridge.notConfigured", exception.messageKey());
        verify(registrationService, never()).synchronize();
        verify(bridgeHttpClient, never()).post(any(String.class), any(), any(Duration.class));
    }

    private static BridgeRegistrationResult synchronizedCandidate(String fingerprint) {
        BridgeRegisteredCandidate candidate = new BridgeRegisteredCandidate(ANALYZER_ID, PROFILE_ID, PROFILE_REVISION,
                fingerprint);
        return new BridgeRegistrationResult(true, Set.of(ANALYZER_ID), null, Map.of(ANALYZER_ID, candidate));
    }

    private static String probeFixture() throws Exception {
        return Files.readString(Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1", "fixtures",
                "connection-probe-result.json"));
    }
}
