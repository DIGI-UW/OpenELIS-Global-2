package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationCandidate;
import org.openelisglobal.analyzer.valueholder.AnalyzerConnectionRole;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerTransportMode;
import org.openelisglobal.analyzer.valueholder.CommunicationMode;

@RunWith(MockitoJUnitRunner.class)
public class BridgeRegistrationExactCandidateTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-23T20:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private BridgeHttpClient bridgeHttpClient;

    private BridgeRegistrationService service;

    @Before
    public void setUp() {
        service = new BridgeRegistrationService(analyzerService, bridgeHttpClient, "https://bridge.example", CLOCK);
    }

    @Test
    public void activeRuntimeUsesThePromotedCandidateInsteadOfMutableDraftFields() throws Exception {
        Analyzer analyzer = analyzer("42", Analyzer.AnalyzerStatus.ACTIVE);
        ObjectNode retainedRegistration = registration("Retained analyzer", "192.0.2.10", 5000);
        analyzer.setActiveCandidate(candidate(analyzer, retainedRegistration));
        analyzer.setName("Unverified draft name");
        analyzer.setIpAddress("192.0.2.99");
        analyzer.setPort(5999);
        when(analyzerService.getAllWithTypes()).thenReturn(List.of(analyzer));

        ObjectNode desiredState = service.buildDesiredState();

        assertEquals(retainedRegistration, desiredState.path("analyzers").path("42"));
    }

    @Test
    public void nonActiveDraftIsNeverPublishedAsBridgeRuntime() {
        Analyzer analyzer = analyzer("42", Analyzer.AnalyzerStatus.SETUP);
        when(analyzerService.getAllWithTypes()).thenReturn(List.of(analyzer));

        assertTrue(service.buildDesiredState().path("analyzers").isEmpty());
    }

    @Test
    public void activeAnalyzerWithoutAPromotedCandidateFailsClosed() {
        Analyzer analyzer = analyzer("42", Analyzer.AnalyzerStatus.ACTIVE);
        when(analyzerService.getAllWithTypes()).thenReturn(List.of(analyzer));

        BridgeRegistrationException exception = assertThrows(BridgeRegistrationException.class,
                () -> service.buildDesiredState());

        assertTrue(exception.getMessage().contains("42"));
        assertTrue(exception.getMessage().contains("activation candidate"));
    }

    private static Analyzer analyzer(String id, Analyzer.AnalyzerStatus status) {
        AnalyzerProfileBinding profile = new AnalyzerProfileBinding();
        profile.setProfileId("site.mock");
        profile.setProfileRevision(2);
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setProfileBinding(profile);
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setSiteBinding(binding);

        Analyzer analyzer = new Analyzer();
        analyzer.setId(id);
        analyzer.setName("Mutable analyzer");
        analyzer.setType("ASTM");
        analyzer.setStatus(status);
        analyzer.setSiteBindingRevision(revision);
        analyzer.setTransportMode(AnalyzerTransportMode.TCP);
        analyzer.setConnectionRole(AnalyzerConnectionRole.INITIATOR);
        analyzer.setCommunicationMode(CommunicationMode.ANALYZER_INITIATED);
        analyzer.setIpAddress("192.0.2.20");
        analyzer.setPort(5100);
        return analyzer;
    }

    private static AnalyzerActivationCandidate candidate(Analyzer analyzer, ObjectNode registration) throws Exception {
        String fingerprint = registration.path("desiredStateFingerprint").asText();
        ObjectNode candidateDocument = JSON.createObjectNode();
        candidateDocument.put("desiredRegistrationFingerprint", fingerprint);

        AnalyzerActivationCandidate candidate = new AnalyzerActivationCandidate();
        candidate.setId("81");
        candidate.setAnalyzer(analyzer);
        candidate.setCandidateDocumentJson(candidateDocument.toString());
        candidate.setBridgeRegistrationJson(registration.toString());
        candidate.setDesiredStateFingerprint(fingerprint);
        return candidate;
    }

    private static ObjectNode registration(String name, String host, int port) throws Exception {
        ObjectNode registration = JSON.createObjectNode();
        registration.put("sourceId", host);
        registration.put("name", name);
        ObjectNode profileRef = registration.putObject("profileRef");
        profileRef.put("profileId", "site.mock");
        profileRef.put("revision", 2);
        registration.put("protocol", "ASTM");
        registration.put("dataFlow", "RESULTS_ONLY");
        registration.put("desiredStatus", "ACTIVE");
        ObjectNode connection = registration.putObject("connection");
        connection.put("mode", "TCP");
        connection.put("role", "INITIATOR");
        ObjectNode settings = connection.putObject("settings");
        settings.put("remoteHost", host);
        settings.put("remotePort", port);
        registration.put("desiredStateFingerprint", fingerprint(registration));
        return registration;
    }

    private static String fingerprint(ObjectNode value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = JSON.writeValueAsString(value).getBytes(StandardCharsets.UTF_8);
        return "sha256:" + HexFormat.of().formatHex(digest.digest(bytes));
    }
}
