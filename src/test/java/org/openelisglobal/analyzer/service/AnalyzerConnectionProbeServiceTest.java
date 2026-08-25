package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerConnectionProbeServiceTest {

    private static final String ANALYZER_ID = "oe-analyzer-42";
    private static final String CONNECTION_ID = "bridge-connection-7f3c";
    private static final String PROFILE_ID = "fixture.synthetic-socket";
    private static final int PROFILE_REVISION = 2;
    private static final String PROFILE_FINGERPRINT = "sha256:" + "1".repeat(64);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private BridgeAnalyzerConnectionClient bridgeClient;

    private AnalyzerConnectionProbeService service;
    private Analyzer analyzer;

    @Before
    public void setUp() {
        service = new AnalyzerConnectionProbeService(analyzerService, bridgeClient, () -> "probe-fixture-004");
        analyzer = analyzer();
    }

    @Test
    public void probesTheExactSavedBridgeConnectionRevision() throws Exception {
        ObjectNode connection = fixture("analyzer-connection.json");
        ObjectNode evidence = fixture("connection-probe-result.json");
        when(analyzerService.getWithType(ANALYZER_ID)).thenReturn(Optional.of(analyzer));
        when(bridgeClient.getConnection(CONNECTION_ID)).thenReturn(connection);
        when(bridgeClient.probe(CONNECTION_ID, 4, "probe-fixture-004")).thenReturn(evidence);

        AnalyzerConnectionProbeView result = service.probe(ANALYZER_ID);

        assertEquals("probe-fixture-004", result.requestId());
        assertEquals(CONNECTION_ID, result.connectionId());
        assertEquals(PROFILE_ID, result.profileRef().profileId());
        assertEquals(PROFILE_REVISION, result.profileRef().revision());
        assertEquals(PROFILE_FINGERPRINT, result.profileRef().fingerprint());
        assertEquals(4, result.configRevision());
        assertEquals("SUCCEEDED", result.status());
        verify(bridgeClient).probe(CONNECTION_ID, 4, "probe-fixture-004");
    }

    @Test
    public void rejectsAConnectionOwnedByAnotherOpenElisAnalyzer() throws Exception {
        ObjectNode connection = fixture("analyzer-connection.json");
        connection.put("clientAnalyzerId", "another-analyzer");
        when(analyzerService.getWithType(ANALYZER_ID)).thenReturn(Optional.of(analyzer));
        when(bridgeClient.getConnection(CONNECTION_ID)).thenReturn(connection);

        AnalyzerConnectionProbeException exception = assertThrows(AnalyzerConnectionProbeException.class,
                () -> service.probe(ANALYZER_ID));

        assertEquals("analyzer.testConnection.bridge.connectionMismatch", exception.messageKey());
        verify(bridgeClient, never()).probe(CONNECTION_ID, 4, "probe-fixture-004");
    }

    @Test
    public void rejectsEvidenceForAStaleSavedRevision() throws Exception {
        ObjectNode evidence = fixture("connection-probe-result.json");
        evidence.put("configRevision", 3);
        when(analyzerService.getWithType(ANALYZER_ID)).thenReturn(Optional.of(analyzer));
        when(bridgeClient.getConnection(CONNECTION_ID)).thenReturn(fixture("analyzer-connection.json"));
        when(bridgeClient.probe(CONNECTION_ID, 4, "probe-fixture-004")).thenReturn(evidence);

        AnalyzerConnectionProbeException exception = assertThrows(AnalyzerConnectionProbeException.class,
                () -> service.probe(ANALYZER_ID));

        assertEquals("analyzer.testConnection.bridge.staleEvidence", exception.messageKey());
    }

    @Test
    public void doesNotProbeAnUnknownAnalyzer() {
        when(analyzerService.getWithType(ANALYZER_ID)).thenReturn(Optional.empty());

        AnalyzerConnectionProbeException exception = assertThrows(AnalyzerConnectionProbeException.class,
                () -> service.probe(ANALYZER_ID));

        assertEquals("analyzer.testConnection.analyzerNotFound", exception.messageKey());
        verify(bridgeClient, never()).getConnection(CONNECTION_ID);
    }

    private static Analyzer analyzer() {
        AnalyzerProfileBinding profile = new AnalyzerProfileBinding();
        profile.setProfileId(PROFILE_ID);
        profile.setProfileRevision(PROFILE_REVISION);
        profile.setProfileFingerprint(PROFILE_FINGERPRINT);
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setProfileBinding(profile);
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setSiteBinding(binding);
        Analyzer analyzer = new Analyzer();
        analyzer.setId(ANALYZER_ID);
        analyzer.setBridgeConnectionId(CONNECTION_ID);
        analyzer.setSiteBindingRevision(revision);
        return analyzer;
    }

    private static ObjectNode fixture(String name) throws Exception {
        String json = Files.readString(
                Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1", "fixtures", name));
        return (ObjectNode) JSON.readTree(json);
    }
}
