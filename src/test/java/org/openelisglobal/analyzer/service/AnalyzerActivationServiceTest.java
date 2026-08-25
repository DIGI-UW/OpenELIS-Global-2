package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationRecord;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerActivationServiceTest {

    private static final String ANALYZER_ID = "oe-analyzer-42";
    private static final String CONNECTION_ID = "bridge-connection-7f3c";
    private static final String PROFILE_ID = "fixture.synthetic-socket";
    private static final int PROFILE_REVISION = 2;
    private static final String PROFILE_FINGERPRINT = "sha256:" + "1".repeat(64);
    private static final String RECOGNITION_FINGERPRINT = "sha256:" + "6".repeat(64);
    private static final String ACTOR = "17";
    private static final Instant ACTIVATED_AT = Instant.parse("2026-08-24T19:05:05Z");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private BridgeProfileCatalogService profileCatalogService;

    @Mock
    private AnalyzerSiteBindingService siteBindingService;

    @Mock
    private AnalyzerSiteBindingConfirmationService confirmationService;

    @Mock
    private TestSectionService testSectionService;

    @Mock
    private BridgeAnalyzerConnectionClient bridgeClient;

    @Mock
    private AnalyzerActivationRecordService activationRecordService;

    private AnalyzerActivationService service;
    private Analyzer analyzer;
    private AnalyzerSiteBindingSnapshot snapshot;
    private AnalyzerSiteBindingConfirmation confirmation;
    private ObjectNode connection;
    private ObjectNode activationAcknowledgement;
    private AnalyzerActivationRecord retained;

    @Before
    public void setUp() throws Exception {
        analyzer = analyzer();
        snapshot = snapshot();
        analyzer.setSiteBindingRevision(snapshot.revision());
        confirmation = confirmation(snapshot.revision());
        connection = fixture("analyzer-connection.json");
        connection.put("clientAnalyzerId", ANALYZER_ID);
        activationAcknowledgement = fixture("connection-activate-ack.json");
        retained = new AnalyzerActivationRecord();
        retained.setVerificationConfirmation(confirmation);

        when(analyzerService.getWithType(ANALYZER_ID)).thenReturn(Optional.of(analyzer));
        when(profileCatalogService.getProfile(PROFILE_ID, PROFILE_REVISION)).thenReturn(profileRevision());
        when(siteBindingService.findByRevisionId(snapshot.revision().getId())).thenReturn(Optional.of(snapshot));
        when(confirmationService.assessCurrent(snapshot, RECOGNITION_FINGERPRINT))
                .thenReturn(AnalyzerSiteBindingVerificationAssessment.current(confirmation));
        TestSection activeUnit = new TestSection();
        activeUnit.setId("4");
        activeUnit.setIsActive("Y");
        when(testSectionService.get("4")).thenReturn(activeUnit);
        when(bridgeClient.getConnection(CONNECTION_ID)).thenReturn(connection);

        service = new AnalyzerActivationServiceImpl(analyzerService, profileCatalogService, siteBindingService,
                confirmationService, testSectionService, bridgeClient, activationRecordService,
                Clock.fixed(ACTIVATED_AT, ZoneOffset.UTC), () -> "activate-fixture-004",
                () -> "deactivate-fixture-004");
    }

    @Test
    public void activatesOnlyTheExactSavedBridgeConnectionRevision() {
        when(bridgeClient.applyRuntimeCommand(CONNECTION_ID, 4, "ACTIVATE", "activate-fixture-004"))
                .thenReturn(activationAcknowledgement);
        when(activationRecordService.retain(analyzer, snapshot.revision(), confirmation, activationAcknowledgement,
                "ACTIVE", ACTOR)).thenReturn(retained);

        AnalyzerActivationResult result = service.activate(ANALYZER_ID, ACTOR);

        assertTrue(result.activated());
        assertEquals(Analyzer.AnalyzerStatus.ACTIVE, analyzer.getStatus());
        assertTrue(analyzer.isActive());
        assertEquals(retained, analyzer.getLatestActivationRecord());
        assertEquals(ACTIVATED_AT, analyzer.getLastActivatedDate().toInstant());
        InOrder order = inOrder(bridgeClient, activationRecordService, analyzerService);
        order.verify(bridgeClient).getConnection(CONNECTION_ID);
        order.verify(bridgeClient).applyRuntimeCommand(CONNECTION_ID, 4, "ACTIVATE", "activate-fixture-004");
        order.verify(activationRecordService).retain(analyzer, snapshot.revision(), confirmation,
                activationAcknowledgement, "ACTIVE", ACTOR);
        order.verify(analyzerService).update(analyzer);
    }

    @Test
    public void readinessReadsButDoesNotMutateTheSavedConnection() {
        AnalyzerActivationResult result = service.readiness(ANALYZER_ID);

        assertTrue(result.ready());
        assertFalse(result.activated());
        verify(bridgeClient).getConnection(CONNECTION_ID);
        verify(bridgeClient, never()).applyRuntimeCommand(any(), any(Integer.class), any(), any());
        verify(activationRecordService, never()).retain(any(), any(), any(), any(), any(), any());
        verify(analyzerService, never()).update(any(Analyzer.class));
        assertUnchanged();
    }

    @Test
    public void reportsEveryBridgeReadinessBlockerWithoutInterpretingProfileFields() {
        connection.with("readiness").put("ready", false);
        connection.with("readiness").putArray("blockers").addObject().put("key", "listener-port")
                .put("messageKey", "analyzer.connection.listenerPort.unavailable").putArray("fieldKeys")
                .add("listenerPort");

        AnalyzerActivationResult result = service.readiness(ANALYZER_ID);

        assertEquals(List.of("analyzer.connection.listenerPort.unavailable"),
                result.blockers().stream().map(AnalyzerActivationBlocker::code).toList());
        verify(bridgeClient, never()).applyRuntimeCommand(any(), any(Integer.class), any(), any());
    }

    @Test
    public void rejectsAConnectionOwnedByAnotherOpenElisAnalyzer() {
        connection.put("clientAnalyzerId", "another-analyzer");

        AnalyzerActivationResult result = service.readiness(ANALYZER_ID);

        assertEquals(List.of("analyzer.activation.blocker.connection"),
                result.blockers().stream().map(AnalyzerActivationBlocker::code).toList());
    }

    @Test
    public void requiresCurrentLocalMappingAndRecognitionVerification() {
        when(confirmationService.assessCurrent(snapshot, RECOGNITION_FINGERPRINT))
                .thenReturn(new AnalyzerSiteBindingVerificationAssessment(false, false, confirmation));

        AnalyzerActivationResult result = service.readiness(ANALYZER_ID);

        assertEquals(List.of("analyzer.activation.blocker.mappings", "analyzer.activation.blocker.recognition"),
                result.blockers().stream().map(AnalyzerActivationBlocker::code).toList());
        verify(bridgeClient, never()).applyRuntimeCommand(any(), any(Integer.class), any(), any());
    }

    @Test
    public void rejectedActivationAcknowledgementNeverChangesOpenElisState() {
        activationAcknowledgement.put("outcome", "REJECTED");
        activationAcknowledgement.put("actualRuntimeState", "INACTIVE");
        activationAcknowledgement.putArray("blockers").addObject().put("key", "listener-port").put("messageKey",
                "analyzer.connection.listenerPort.unavailable");
        when(bridgeClient.applyRuntimeCommand(CONNECTION_ID, 4, "ACTIVATE", "activate-fixture-004"))
                .thenReturn(activationAcknowledgement);

        AnalyzerActivationResult result = service.activate(ANALYZER_ID, ACTOR);

        assertFalse(result.activated());
        assertEquals(List.of("analyzer.connection.listenerPort.unavailable"),
                result.blockers().stream().map(AnalyzerActivationBlocker::code).toList());
        verify(activationRecordService, never()).retain(any(), any(), any(), any(), any(), any());
        verify(analyzerService, never()).update(any(Analyzer.class));
        assertUnchanged();
    }

    @Test
    public void compensatesOnlyANewlyAppliedActivationWhenLocalPersistenceFails() throws Exception {
        ObjectNode deactivationAcknowledgement = fixture("connection-deactivate-ack.json");
        when(bridgeClient.applyRuntimeCommand(CONNECTION_ID, 4, "ACTIVATE", "activate-fixture-004"))
                .thenReturn(activationAcknowledgement);
        when(activationRecordService.retain(analyzer, snapshot.revision(), confirmation, activationAcknowledgement,
                "ACTIVE", ACTOR)).thenReturn(retained);
        doThrow(new IllegalStateException("database unavailable")).when(analyzerService).update(analyzer);
        when(bridgeClient.applyRuntimeCommand(eq(CONNECTION_ID), eq(4), eq("DEACTIVATE"), any(String.class)))
                .thenReturn(deactivationAcknowledgement);

        assertThrows(IllegalStateException.class, () -> service.activate(ANALYZER_ID, ACTOR));

        verify(bridgeClient).applyRuntimeCommand(eq(CONNECTION_ID), eq(4), eq("DEACTIVATE"), any(String.class));
        assertUnchanged();
    }

    @Test
    public void deactivatesTheExactSavedBridgeConnectionRevision() throws Exception {
        ObjectNode deactivationAcknowledgement = fixture("connection-deactivate-ack.json");
        analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        analyzer.setActive(true);
        analyzer.setLatestActivationRecord(retained);
        when(bridgeClient.applyRuntimeCommand(CONNECTION_ID, 4, "DEACTIVATE", "deactivate-fixture-004"))
                .thenReturn(deactivationAcknowledgement);
        when(activationRecordService.retain(analyzer, snapshot.revision(), confirmation, deactivationAcknowledgement,
                "INACTIVE", ACTOR)).thenReturn(retained);

        AnalyzerDeactivationResult result = service.deactivate(ANALYZER_ID, ACTOR);

        assertTrue(result.deactivated());
        assertEquals(Analyzer.AnalyzerStatus.INACTIVE, analyzer.getStatus());
        assertFalse(analyzer.isActive());
        verify(bridgeClient).applyRuntimeCommand(CONNECTION_ID, 4, "DEACTIVATE", "deactivate-fixture-004");
        verify(activationRecordService).retain(analyzer, snapshot.revision(), confirmation, deactivationAcknowledgement,
                "INACTIVE", ACTOR);
        verify(analyzerService).update(analyzer);
    }

    private void assertUnchanged() {
        assertEquals(Analyzer.AnalyzerStatus.VALIDATION, analyzer.getStatus());
        assertFalse(analyzer.isActive());
        assertNull(analyzer.getLatestActivationRecord());
        assertNull(analyzer.getLastActivatedDate());
        assertNull(analyzer.getSysUserId());
    }

    private static Analyzer analyzer() {
        Analyzer analyzer = new Analyzer();
        analyzer.setId(ANALYZER_ID);
        analyzer.setName("Lab analyzer");
        analyzer.setBridgeConnectionId(CONNECTION_ID);
        analyzer.setStatus(Analyzer.AnalyzerStatus.VALIDATION);
        analyzer.setActive(false);
        analyzer.setTestUnitIds(List.of("4"));
        return analyzer;
    }

    private static AnalyzerSiteBindingSnapshot snapshot() {
        AnalyzerProfileBinding profile = new AnalyzerProfileBinding();
        profile.setId("21");
        profile.setProfileId(PROFILE_ID);
        profile.setProfileRevision(PROFILE_REVISION);
        profile.setProfileFingerprint(PROFILE_FINGERPRINT);
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId("31");
        binding.setProfileBinding(profile);
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId("41");
        revision.setSiteBinding(binding);
        revision.setBindingFingerprint("sha256:" + "5".repeat(64));
        return new AnalyzerSiteBindingSnapshot(binding, revision, List.of(), List.of());
    }

    private static AnalyzerSiteBindingConfirmation confirmation(AnalyzerSiteBindingRevision revision) {
        AnalyzerSiteBindingConfirmation confirmation = new AnalyzerSiteBindingConfirmation();
        confirmation.setId("51");
        confirmation.setSiteBindingRevision(revision);
        confirmation.setProfileId(PROFILE_ID);
        confirmation.setProfileRevision(PROFILE_REVISION);
        confirmation.setProfileRevisionFingerprint(PROFILE_FINGERPRINT);
        confirmation.setBindingFingerprint(revision.getBindingFingerprint());
        confirmation.setRecognitionFingerprint(RECOGNITION_FINGERPRINT);
        return confirmation;
    }

    private static BridgeProfileCatalog.ProfileRevision profileRevision() {
        ObjectNode profile = JSON.createObjectNode();
        profile.putObject("profileMeta").put("id", PROFILE_ID).put("displayName", "Synthetic socket analyzer");
        profile.putObject("catalog").put("revision", PROFILE_REVISION).put("revisionFingerprint", PROFILE_FINGERPRINT)
                .put("source", "TEST").put("status", "ACTIVE");
        profile.putObject("protocol").put("name", "ASTM");
        BridgeProfileCatalog.ControlRecognitionSummary recognition = new BridgeProfileCatalog.ControlRecognitionSummary(
                RECOGNITION_FINGERPRINT, "NONE", "No automated control recognition", true, List.of());
        return new BridgeProfileCatalog.ProfileRevision(profile, JSON.createObjectNode(), recognition);
    }

    private static ObjectNode fixture(String name) throws Exception {
        String json = Files.readString(
                Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1", "fixtures", name));
        return (ObjectNode) JSON.readTree(json);
    }
}
