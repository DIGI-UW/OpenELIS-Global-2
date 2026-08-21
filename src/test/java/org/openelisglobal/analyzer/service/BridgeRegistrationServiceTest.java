package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerConnectionRole;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerTransportMode;
import org.openelisglobal.analyzer.valueholder.CommunicationMode;

@RunWith(MockitoJUnitRunner.class)
public class BridgeRegistrationServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-20T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private BridgeHttpClient bridgeHttpClient;

    private BridgeRegistrationService service;
    private Analyzer fileAnalyzer;
    private Analyzer astmAnalyzer;

    @Before
    public void setUp() {
        service = new BridgeRegistrationService(analyzerService, bridgeHttpClient, "https://bridge.example", CLOCK);

        fileAnalyzer = pinnedAnalyzer("42", "FluoroCycler Lab 1", "FILE", "fluorocycler-xt", 1,
                "sha256:458386c72b12603cb3e0abdb75673a6ed4561f25545ca569a5797dba233cf3a6");
        fileAnalyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        fileAnalyzer.setTransportMode(AnalyzerTransportMode.FILE);
        fileAnalyzer.setConnectionRole(AnalyzerConnectionRole.RECEIVER);
        fileAnalyzer.setImportDirectory("/data/analyzer-imports/fluoro/incoming");
        fileAnalyzer.setFilePattern("copied-pattern-must-not-cross");
        fileAnalyzer.setColumnMappings(java.util.Map.of("copied", "mapping"));
        fileAnalyzer.setFileFormat("COPIED");

        astmAnalyzer = pinnedAnalyzer("77", "GeneXpert Lab 1", "ASTM", "genexpert-astm", 1,
                "sha256:26b5f09e129dae6c5e0b1284eef10350f3dcd3f660413096045247f2470f32f1");
        astmAnalyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        astmAnalyzer.setTransportMode(AnalyzerTransportMode.TCP);
        astmAnalyzer.setConnectionRole(AnalyzerConnectionRole.RECEIVER);
        astmAnalyzer.setIpAddress("10.42.20.10");
        astmAnalyzer.setPort(9600);
        astmAnalyzer.setCommunicationMode(CommunicationMode.BOTH);
    }

    @Test
    public void buildsDeterministicVersionedStateFromPinsAndSiteValuesOnly() throws Exception {
        when(analyzerService.getAllWithTypes()).thenReturn(List.of(astmAnalyzer, fileAnalyzer));

        ObjectNode initial = service.buildDesiredState();

        assertConforms(initial);
        assertEquals("1.0", initial.path("schemaVersion").asText());
        assertEquals(List.of("42", "77"), fieldNames(initial.path("analyzers")));

        JsonNode file = initial.path("analyzers").path("42");
        assertEquals("fluorocycler-xt", file.path("profileRef").path("profileId").asText());
        assertEquals(1, file.path("profileRef").path("revision").asInt());
        assertEquals("FILE", file.path("protocol").asText());
        assertEquals("FILE", file.path("connection").path("mode").asText());
        assertEquals("RECEIVER", file.path("connection").path("role").asText());
        assertEquals(fileAnalyzer.getImportDirectory(),
                file.path("connection").path("settings").path("directory").asText());
        assertFalse(file.has("filePattern"));
        assertFalse(file.has("columnMappings"));
        assertFalse(file.has("testCodeLoinc"));
        assertFalse(file.has("qcRules"));
        assertFalse(file.has("controlLots"));

        JsonNode astm = initial.path("analyzers").path("77");
        assertEquals("genexpert-astm", astm.path("profileRef").path("profileId").asText());
        assertEquals("TCP", astm.path("connection").path("mode").asText());
        assertEquals("RECEIVER", astm.path("connection").path("role").asText());
        assertEquals("10.42.20.10", astm.path("connection").path("settings").path("remoteHost").asText());
        assertEquals(9600, astm.path("connection").path("settings").path("remotePort").asInt());
        assertTrue(astm.path("desiredStateFingerprint").asText().matches("sha256:[0-9a-f]{64}"));

        String firstState = initial.path("desiredStateRevision").asText();
        String firstFileFingerprint = file.path("desiredStateFingerprint").asText();
        fileAnalyzer.setFilePattern("another-copied-value");
        ObjectNode copiedProfileChange = service.buildDesiredState();
        assertEquals(firstState, copiedProfileChange.path("desiredStateRevision").asText());
        assertEquals(firstFileFingerprint,
                copiedProfileChange.path("analyzers").path("42").path("desiredStateFingerprint").asText());

        fileAnalyzer.setImportDirectory("/data/analyzer-imports/fluoro-2/incoming");
        ObjectNode siteChange = service.buildDesiredState();
        assertNotEquals(firstState, siteChange.path("desiredStateRevision").asText());
        assertNotEquals(firstFileFingerprint,
                siteChange.path("analyzers").path("42").path("desiredStateFingerprint").asText());
    }

    @Test
    public void activeAnalyzerWithoutAnImmutablePinBlocksThePushInsteadOfBeingInferred() throws Exception {
        Analyzer unpinned = new Analyzer();
        unpinned.setId("99");
        unpinned.setName("Unpinned analyzer");
        unpinned.setType("ASTM");
        unpinned.setIpAddress("10.42.20.99");
        unpinned.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        when(analyzerService.getAllWithTypes()).thenReturn(List.of(unpinned));

        BridgeRegistrationException exception = assertThrows(BridgeRegistrationException.class,
                () -> service.synchronize());

        assertTrue(exception.getMessage().contains("99"));
        assertTrue(exception.getMessage().contains("profile pin"));
        verify(bridgeHttpClient, never()).put(anyString(), anyString(), any(Duration.class));
    }

    @Test
    public void incompleteInactiveSetupIsNotRuntimeStateButIncompleteActiveSetupIsRejected() {
        Analyzer setup = pinnedAnalyzer("88", "Setup analyzer", "ASTM", "genexpert-astm", 1,
                "sha256:26b5f09e129dae6c5e0b1284eef10350f3dcd3f660413096045247f2470f32f1");
        setup.setStatus(Analyzer.AnalyzerStatus.SETUP);
        when(analyzerService.getAllWithTypes()).thenReturn(List.of(setup));

        assertTrue(service.buildDesiredState().path("analyzers").isEmpty());

        setup.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        assertThrows(BridgeRegistrationException.class, () -> service.buildDesiredState());
    }

    @Test
    public void completeSetupEnablesBridgeRuntimeWithoutActivatingTheOpenElisAnalyzer() {
        astmAnalyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
        when(analyzerService.getAllWithTypes()).thenReturn(List.of(astmAnalyzer));

        JsonNode registration = service.buildDesiredState().path("analyzers").path("77");

        assertEquals("ACTIVE", registration.path("desiredStatus").asText());
        assertEquals(Analyzer.AnalyzerStatus.SETUP, astmAnalyzer.getStatus());

        astmAnalyzer.setStatus(Analyzer.AnalyzerStatus.INACTIVE);
        registration = service.buildDesiredState().path("analyzers").path("77");
        assertEquals("INACTIVE", registration.path("desiredStatus").asText());
    }

    @Test
    public void activeSocketAnalyzerRequiresAnExplicitProfileDerivedTransportAndRole() {
        astmAnalyzer.setTransportMode(null);
        when(analyzerService.getAllWithTypes()).thenReturn(List.of(astmAnalyzer));
        assertThrows(BridgeRegistrationException.class, () -> service.buildDesiredState());

        astmAnalyzer.setTransportMode(AnalyzerTransportMode.TCP);
        astmAnalyzer.setConnectionRole(null);
        assertThrows(BridgeRegistrationException.class, () -> service.buildDesiredState());
    }

    @Test
    public void acceptsOnlyAnExactAcknowledgementForEveryDesiredAnalyzer() throws Exception {
        when(analyzerService.getAllWithTypes()).thenReturn(List.of(fileAnalyzer, astmAnalyzer));
        when(bridgeHttpClient.put(anyString(), anyString(), any(Duration.class))).thenAnswer(invocation -> {
            JsonNode request = JSON.readTree((String) invocation.getArgument(1));
            return new BridgeHttpClient.BridgeResponse(200, acknowledgement(request, null).toString());
        });

        BridgeRegistrationResult result = service.synchronize();

        assertTrue(result.complete());
        assertTrue(result.acknowledged("42"));
        assertTrue(result.acknowledged("77"));
    }

    @Test
    public void rejectsAResponseThatAcknowledgesAStaleFingerprint() throws Exception {
        when(analyzerService.getAllWithTypes()).thenReturn(List.of(fileAnalyzer));
        when(bridgeHttpClient.put(anyString(), anyString(), any(Duration.class))).thenAnswer(invocation -> {
            JsonNode request = JSON.readTree((String) invocation.getArgument(1));
            return new BridgeHttpClient.BridgeResponse(200, acknowledgement(request, "42").toString());
        });

        BridgeRegistrationResult result = service.synchronize();

        assertFalse(result.complete());
        assertFalse(result.acknowledged("42"));
        assertTrue(result.failure().contains("42"));
    }

    private static Analyzer pinnedAnalyzer(String id, String name, String protocol, String profileId, int revision,
            String profileFingerprint) {
        AnalyzerProfileBinding profileBinding = new AnalyzerProfileBinding();
        profileBinding.setProfileId(profileId);
        profileBinding.setProfileRevision(revision);
        profileBinding.setProfileFingerprint(profileFingerprint);
        AnalyzerSiteBinding siteBinding = new AnalyzerSiteBinding();
        siteBinding.setProfileBinding(profileBinding);
        AnalyzerSiteBindingRevision bindingRevision = new AnalyzerSiteBindingRevision();
        bindingRevision.setSiteBinding(siteBinding);

        Analyzer analyzer = new Analyzer();
        analyzer.setId(id);
        analyzer.setName(name);
        analyzer.setType(protocol);
        analyzer.setSiteBindingRevision(bindingRevision);
        return analyzer;
    }

    private static ObjectNode acknowledgement(JsonNode request, String staleAnalyzerId) {
        ObjectNode response = JSON.createObjectNode();
        response.put("schemaVersion", "1.0");
        response.put("appliedStateRevision", request.path("desiredStateRevision").asText());
        ObjectNode counts = response.putObject("counts");
        counts.put("total", request.path("analyzers").size());
        counts.put("added", request.path("analyzers").size());
        counts.put("updated", 0);
        counts.put("removed", 0);
        counts.put("unchanged", 0);
        counts.put("rejected", 0);
        ObjectNode registrations = response.putObject("registrations");
        request.path("analyzers").fields().forEachRemaining(entry -> {
            ObjectNode result = registrations.putObject(entry.getKey());
            result.set("profileRef", entry.getValue().path("profileRef").deepCopy());
            String fingerprint = entry.getValue().path("desiredStateFingerprint").asText();
            result.put("desiredStateFingerprint",
                    entry.getKey().equals(staleAnalyzerId) ? "sha256:" + "f".repeat(64) : fingerprint);
            result.put("status", "APPLIED");
        });
        response.putArray("errors");
        return response;
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        Iterator<String> fields = node.fieldNames();
        fields.forEachRemaining(names::add);
        return names;
    }

    private static void assertConforms(JsonNode request) throws Exception {
        Path schemaPath = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1",
                "registration-sync.schema.json");
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(JSON.readTree(Files.readString(schemaPath)));
        assertTrue(schema.validate(request).toString(), schema.validate(request).isEmpty());
    }
}
