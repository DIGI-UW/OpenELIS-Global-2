package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.AnalyzerTestProfileCatalog;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationCandidate;
import org.openelisglobal.analyzer.valueholder.AnalyzerConnectionRole;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerTransportMode;
import org.openelisglobal.analyzer.valueholder.CommunicationMode;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerActivationServiceTest {

    private static final String ANALYZER_ID = "77";
    private static final String ACTOR = "17";
    private static final String RECOGNITION_FINGERPRINT = "sha256:" + "c".repeat(64);
    private static final String REGISTRATION_FINGERPRINT = "sha256:" + "d".repeat(64);
    private static final Instant ACTIVATED_AT = Instant.parse("2026-08-23T20:00:00Z");
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
    private BridgeRegistrationService registrationService;

    @Mock
    private AnalyzerActivationCandidateFactory candidateFactory;

    @Mock
    private AnalyzerActivationCandidateService candidateService;

    private AnalyzerActivationService service;
    private Analyzer analyzer;
    private AnalyzerSiteBindingSnapshot snapshot;
    private AnalyzerSiteBindingConfirmation confirmation;
    private ObjectNode registration;
    private AnalyzerActivationDocuments documents;
    private AnalyzerActivationCandidate retained;

    @Before
    public void setUp() {
        analyzer = analyzer();
        snapshot = snapshot();
        analyzer.setSiteBindingRevision(snapshot.revision());
        confirmation = confirmation(snapshot.revision());
        registration = registration();
        documents = new AnalyzerActivationDocuments(JSON.createObjectNode().put("oeAnalyzerId", ANALYZER_ID),
                registration);
        retained = new AnalyzerActivationCandidate();

        when(analyzerService.getWithType(ANALYZER_ID)).thenReturn(Optional.of(analyzer));
        when(profileCatalogService.getProfile(AnalyzerTestProfileCatalog.PROFILE_ID,
                AnalyzerTestProfileCatalog.PROFILE_REVISION)).thenReturn(profileRevision());
        when(siteBindingService.findByRevisionId(snapshot.revision().getId())).thenReturn(Optional.of(snapshot));
        when(confirmationService.findCurrent(snapshot, RECOGNITION_FINGERPRINT)).thenReturn(Optional.of(confirmation));
        TestSection activeUnit = new TestSection();
        activeUnit.setId("4");
        activeUnit.setIsActive("Y");
        when(testSectionService.get("4")).thenReturn(activeUnit);
        when(registrationService.buildActivationRegistration(analyzer)).thenReturn(registration);

        service = new AnalyzerActivationServiceImpl(analyzerService, profileCatalogService, siteBindingService,
                confirmationService, testSectionService, registrationService, candidateFactory, candidateService,
                Clock.fixed(ACTIVATED_AT, ZoneOffset.UTC));
    }

    @Test
    public void promotesOnlyAfterTheExactCandidateIsAcknowledgedAndRetained() {
        BridgeRegisteredCandidate acknowledgement = acknowledgeCandidate();
        when(candidateService.retain(analyzer, snapshot.revision(), confirmation, documents, ACTOR))
                .thenReturn(retained);

        AnalyzerActivationResult result = service.activate(ANALYZER_ID, ACTOR);

        assertTrue(result.activated());
        assertTrue(result.blockers().isEmpty());
        assertEquals(Analyzer.AnalyzerStatus.ACTIVE, analyzer.getStatus());
        assertTrue(analyzer.isActive());
        assertEquals(retained, analyzer.getActiveCandidate());
        assertEquals(ACTIVATED_AT, analyzer.getLastActivatedDate().toInstant());
        assertEquals(ACTOR, analyzer.getSysUserId());

        InOrder order = inOrder(registrationService, candidateFactory, candidateService, analyzerService);
        order.verify(registrationService).synchronizeCandidate(ANALYZER_ID, registration);
        order.verify(candidateFactory).create(analyzer, snapshot, confirmation, registration, acknowledgement);
        order.verify(candidateService).retain(analyzer, snapshot.revision(), confirmation, documents, ACTOR);
        order.verify(analyzerService).update(analyzer);
    }

    @Test
    public void leavesTheAnalyzerUnchangedWhenBridgeDoesNotAcknowledgeTheExactCandidate() {
        when(registrationService.synchronizeCandidate(ANALYZER_ID, registration))
                .thenReturn(new BridgeRegistrationResult(false, Set.of(), "Bridge unavailable"));

        AnalyzerActivationResult result = service.activate(ANALYZER_ID, ACTOR);

        assertFalse(result.activated());
        assertEquals(List.of("analyzer.activation.blocker.bridgeAcknowledgement"),
                result.blockers().stream().map(AnalyzerActivationBlocker::code).toList());
        assertEquals(Analyzer.AnalyzerStatus.VALIDATION, analyzer.getStatus());
        assertFalse(analyzer.isActive());
        assertNull(analyzer.getActiveCandidate());
        assertNull(analyzer.getLastActivatedDate());
        verify(candidateFactory, never()).create(any(), any(), any(), any(), any());
        verify(candidateService, never()).retain(any(), any(), any(), any(), any());
        verify(analyzerService, never()).update(any(Analyzer.class));
        verify(registrationService, never()).synchronize();
    }

    @Test
    public void readinessValidatesTheDraftWithoutSynchronizingOrPersisting() {
        AnalyzerActivationResult result = service.readiness(ANALYZER_ID);

        assertTrue(result.ready());
        assertFalse(result.activated());
        assertTrue(result.blockers().isEmpty());
        verify(registrationService, never()).synchronizeCandidate(any(), any());
        verify(registrationService, never()).synchronize();
        verify(candidateFactory, never()).create(any(), any(), any(), any(), any());
        verify(candidateService, never()).retain(any(), any(), any(), any(), any());
        verify(analyzerService, never()).update(any(Analyzer.class));
        assertUnchanged();
    }

    @Test
    public void profileMustDeclareTheSelectedTransportBeforeActivation() {
        BridgeProfileCatalog.ProfileRevision revision = profileRevision();
        ObjectNode serialOnly = revision.profile().deepCopy();
        serialOnly.putArray("transport").add("RS-232");
        when(profileCatalogService.getProfile(AnalyzerTestProfileCatalog.PROFILE_ID,
                AnalyzerTestProfileCatalog.PROFILE_REVISION))
                .thenReturn(new BridgeProfileCatalog.ProfileRevision(serialOnly, revision.publication(),
                        revision.controlRecognitionSummary()));

        AnalyzerActivationResult result = service.readiness(ANALYZER_ID);

        assertEquals(List.of("analyzer.activation.blocker.transport"),
                result.blockers().stream().map(AnalyzerActivationBlocker::code).toList());
        verify(registrationService, never()).buildActivationRegistration(analyzer);
        verify(registrationService, never()).synchronizeCandidate(any(), any());
    }

    @Test
    public void twoWayDataFlowRequiresThePinnedProfilesExplicitCapability() {
        analyzer.setCommunicationMode(CommunicationMode.BOTH);

        AnalyzerActivationResult result = service.readiness(ANALYZER_ID);

        assertEquals(List.of("analyzer.activation.blocker.dataFlow"),
                result.blockers().stream().map(AnalyzerActivationBlocker::code).toList());
        verify(registrationService, never()).buildActivationRegistration(analyzer);
        verify(registrationService, never()).synchronizeCandidate(any(), any());
    }

    @Test
    public void restoresBridgeDesiredStateWhenTheAcknowledgedCandidateCannotBeRetained() {
        acknowledgeCandidate();
        when(candidateService.retain(analyzer, snapshot.revision(), confirmation, documents, ACTOR))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, () -> service.activate(ANALYZER_ID, ACTOR));

        verify(registrationService).synchronize();
        assertUnchanged();
    }

    @Test
    public void restoresTheAnalyzerAndBridgeWhenFinalPromotionCannotBePersisted() {
        acknowledgeCandidate();
        when(candidateService.retain(analyzer, snapshot.revision(), confirmation, documents, ACTOR))
                .thenReturn(retained);
        doThrow(new IllegalStateException("database unavailable")).when(analyzerService).update(analyzer);

        assertThrows(IllegalStateException.class, () -> service.activate(ANALYZER_ID, ACTOR));

        verify(registrationService).synchronize();
        assertUnchanged();
    }

    private BridgeRegisteredCandidate acknowledgeCandidate() {
        BridgeRegisteredCandidate acknowledgement = acknowledgement();
        when(registrationService.synchronizeCandidate(ANALYZER_ID, registration)).thenReturn(
                new BridgeRegistrationResult(true, Set.of(ANALYZER_ID), null, Map.of(ANALYZER_ID, acknowledgement)));
        when(candidateFactory.create(analyzer, snapshot, confirmation, registration, acknowledgement))
                .thenReturn(documents);
        return acknowledgement;
    }

    private void assertUnchanged() {
        assertEquals(Analyzer.AnalyzerStatus.VALIDATION, analyzer.getStatus());
        assertFalse(analyzer.isActive());
        assertNull(analyzer.getActiveCandidate());
        assertNull(analyzer.getLastActivatedDate());
        assertNull(analyzer.getSysUserId());
    }

    private static Analyzer analyzer() {
        Analyzer analyzer = new Analyzer();
        analyzer.setId(ANALYZER_ID);
        analyzer.setName("Lab analyzer");
        analyzer.setStatus(Analyzer.AnalyzerStatus.VALIDATION);
        analyzer.setActive(false);
        analyzer.setTestUnitIds(List.of("4"));
        analyzer.setType("ASTM");
        analyzer.setTransportMode(AnalyzerTransportMode.TCP);
        analyzer.setConnectionRole(AnalyzerConnectionRole.RECEIVER);
        analyzer.setCommunicationMode(CommunicationMode.ANALYZER_INITIATED);
        return analyzer;
    }

    private static AnalyzerSiteBindingSnapshot snapshot() {
        AnalyzerProfileBinding profile = new AnalyzerProfileBinding();
        profile.setId("21");
        profile.setProfileId(AnalyzerTestProfileCatalog.PROFILE_ID);
        profile.setProfileRevision(AnalyzerTestProfileCatalog.PROFILE_REVISION);
        profile.setProfileFingerprint(AnalyzerTestProfileCatalog.PROFILE_FINGERPRINT);
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId("31");
        binding.setProfileBinding(profile);
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId("41");
        revision.setSiteBinding(binding);
        revision.setBindingFingerprint("sha256:" + "b".repeat(64));
        return new AnalyzerSiteBindingSnapshot(binding, revision, List.of(), List.of());
    }

    private static AnalyzerSiteBindingConfirmation confirmation(AnalyzerSiteBindingRevision revision) {
        AnalyzerSiteBindingConfirmation confirmation = new AnalyzerSiteBindingConfirmation();
        confirmation.setId("51");
        confirmation.setSiteBindingRevision(revision);
        confirmation.setRecognitionFingerprint(RECOGNITION_FINGERPRINT);
        return confirmation;
    }

    private static BridgeProfileCatalog.ProfileRevision profileRevision() {
        BridgeProfileCatalog.ProfileRevision profile = AnalyzerTestProfileCatalog.catalog().profiles().get(0);
        BridgeProfileCatalog.ControlRecognitionSummary recognition = new BridgeProfileCatalog.ControlRecognitionSummary(
                RECOGNITION_FINGERPRINT, "NONE", "No automated control recognition", true, List.of());
        return new BridgeProfileCatalog.ProfileRevision(profile.profile(), profile.publication(), recognition);
    }

    private static ObjectNode registration() {
        return JSON.createObjectNode().put("desiredStateFingerprint", REGISTRATION_FINGERPRINT);
    }

    private static BridgeRegisteredCandidate acknowledgement() {
        return new BridgeRegisteredCandidate(ANALYZER_ID, AnalyzerTestProfileCatalog.PROFILE_ID,
                AnalyzerTestProfileCatalog.PROFILE_REVISION, REGISTRATION_FINGERPRINT);
    }
}
