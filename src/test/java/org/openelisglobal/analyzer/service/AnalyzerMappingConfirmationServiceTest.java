package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerMappingConfirmationDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResultPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTestPK;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.terminology.Coding;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerMappingConfirmationServiceTest {

    private static final String BINDING_FINGERPRINT = "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String RECOGNITION_FINGERPRINT = "sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
    private static final String PROFILE_FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String EDITED_FINGERPRINT = "sha256:" + "e".repeat(64);

    @Mock
    private AnalyzerMappingConfirmationDAO confirmationDAO;

    @Mock
    private AuditTrailService auditTrailService;

    @Mock
    private SystemUserService systemUserService;

    @Mock
    private AnalyzerMappingCatalogService mappingCatalogService;

    @Mock
    private BridgeProfileCatalogService profileCatalogService;

    @Mock
    private BridgeAnalyzerConnectionClient connectionClient;

    private AnalyzerMappingConfirmationService service;

    @Before
    public void setUp() {
        service = new AnalyzerMappingConfirmationServiceImpl(confirmationDAO, auditTrailService, systemUserService,
                mappingCatalogService, profileCatalogService, connectionClient);
        lenient().when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(PROFILE_FINGERPRINT, "copies/mL", "DETECTED", "MOCK"));
        SystemUser actor = new SystemUser();
        actor.setId("17");
        actor.setFirstName("Ada");
        actor.setLastName("Lovelace");
        actor.setLoginName("ada");
        when(systemUserService.getUserById("17")).thenReturn(actor);
        when(mappingCatalogService.searchActiveTests(null))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.TestOption("1", "Mock Test", "MOCK",
                        List.of(Coding.loinc("1234-5")))));
        when(mappingCatalogService.getActiveResultOptions("1")).thenReturn(
                List.of(new AnalyzerMappingCatalogService.ResultOption("11", "DETECTED", "Detected", List.of())));
        when(confirmationDAO.insert(any(AnalyzerMappingConfirmation.class))).thenAnswer(invocation -> {
            AnalyzerMappingConfirmation confirmation = invocation.getArgument(0);
            confirmation.setId("71");
            return confirmation.getId();
        });
        when(auditTrailService.saveNewHistory(any(AnalyzerMappingConfirmation.class), eq("17"),
                eq("analyzer_mapping_confirmation"))).thenReturn("91");
    }

    @Test
    public void confirmsTheExactImmutableCandidateAndAuditsTheActor() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmationRequest request = exactRequest();

        AnalyzerMappingConfirmationView confirmed = service.confirm(candidate, RECOGNITION_FINGERPRINT, request, "17");

        ArgumentCaptor<AnalyzerMappingConfirmation> saved = ArgumentCaptor.forClass(AnalyzerMappingConfirmation.class);
        verify(confirmationDAO).insert(saved.capture());
        assertSame(candidate.mapping(), saved.getValue().getMapping());
        assertEquals("site.mock-analyzer", saved.getValue().getProfileId());
        assertEquals(2, saved.getValue().getProfileRevision());
        assertEquals(PROFILE_FINGERPRINT, saved.getValue().getProfileRevisionFingerprint());
        assertEquals(BINDING_FINGERPRINT, saved.getValue().getMappingFingerprint());
        assertEquals(RECOGNITION_FINGERPRINT, saved.getValue().getRecognitionFingerprint());
        assertEquals("91", saved.getValue().getAuditEventId());
        assertEquals("17", saved.getValue().getConfirmedBy());
        assertEquals(AnalyzerMappingConfirmationView.State.CURRENT, confirmed.state());
        assertEquals("17", confirmed.confirmedBy());
        assertEquals("Ada Lovelace", confirmed.confirmedByDisplayName());
        assertEquals(request.confirmedRows(), confirmed.confirmedRows());
        assertEquals(request.excludedRows(), confirmed.excludedRows());
        verify(auditTrailService).saveNewHistory(saved.getValue(), "17", "analyzer_mapping_confirmation");
    }

    @Test
    public void disabledHistoryLeavesTheSavedConfirmationStaleWithoutASecondWrite() {
        when(auditTrailService.saveNewHistory(any(AnalyzerMappingConfirmation.class), eq("17"),
                eq("analyzer_mapping_confirmation"))).thenReturn(null);

        AnalyzerMappingConfirmationView confirmed = service.confirm(completeCandidate("61", BINDING_FINGERPRINT),
                RECOGNITION_FINGERPRINT, exactRequest(), "17");

        ArgumentCaptor<AnalyzerMappingConfirmation> saved = ArgumentCaptor
                .forClass(AnalyzerMappingConfirmation.class);
        verify(confirmationDAO).insert(saved.capture());
        assertNull(saved.getValue().getAuditEventId());
        verify(confirmationDAO, never()).update(any(AnalyzerMappingConfirmation.class));
        assertEquals(AnalyzerMappingConfirmationView.State.STALE, confirmed.state());
    }

    @Test
    public void rejectsAStaleMappingOrRecognitionFingerprintBeforeWriting() {
        AnalyzerMappingConfirmationRequest stale = new AnalyzerMappingConfirmationRequest(
                "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd", RECOGNITION_FINGERPRINT,
                exactRequest().confirmedRows(), exactRequest().excludedRows());

        assertThrows(IllegalArgumentException.class, () -> service.confirm(completeCandidate("61", BINDING_FINGERPRINT),
                RECOGNITION_FINGERPRINT, stale, "17"));
        verify(confirmationDAO, never()).insert(any());
    }

    @Test
    public void confirmsResolvedDecisionsWhileOtherRowsRemainUnresolved() throws Exception {
        AnalyzerMappingSnapshot base = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingTest pending = test(base.mapping(), "PENDING", AnalyzerMappingState.UNRESOLVED);
        AnalyzerMappingSnapshot candidate = new AnalyzerMappingSnapshot(base.mapping(),
                List.of(base.tests().get(0), base.tests().get(1), pending), base.results());

        AnalyzerMappingConfirmationView view = service.confirm(candidate, RECOGNITION_FINGERPRINT, exactRequest(),
                "17");

        assertEquals(AnalyzerMappingConfirmationView.State.CURRENT, view.state());
        assertEquals(exactRequest().confirmedRows(), view.confirmedRows());
        assertEquals(exactRequest().excludedRows(), view.excludedRows());
        assertEquals(AnalyzerMappingState.UNRESOLVED, pending.getMappingState());
        ArgumentCaptor<AnalyzerMappingConfirmation> written = ArgumentCaptor
                .forClass(AnalyzerMappingConfirmation.class);
        verify(confirmationDAO).insert(written.capture());
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(written.getValue()));
        assertTrue(service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).mappingsCurrent());
    }

    @Test
    public void rejectsFalselyConfirmedUnresolvedRowsOrOmittedBoundRowsBeforeWriting() {
        AnalyzerMappingSnapshot unresolved = completeCandidate("61", BINDING_FINGERPRINT);
        unresolved.tests().get(0).setMappingState(AnalyzerMappingState.UNRESOLVED);
        AnalyzerMappingConfirmationRequest omitted = new AnalyzerMappingConfirmationRequest(BINDING_FINGERPRINT,
                RECOGNITION_FINGERPRINT, List.of(new AnalyzerMappingSourceRow("RAW-A", "Detected")),
                exactRequest().excludedRows());

        assertThrows(IllegalArgumentException.class,
                () -> service.confirm(unresolved, RECOGNITION_FINGERPRINT, exactRequest(), "17"));
        assertThrows(IllegalArgumentException.class, () -> service.confirm(completeCandidate("61", BINDING_FINGERPRINT),
                RECOGNITION_FINGERPRINT, omitted, "17"));
        verify(confirmationDAO, never()).insert(any());
    }

    @Test
    public void reportsAFormerConfirmationAsStaleForANewMappingRevision() throws Exception {
        AnalyzerMappingSnapshot former = completeCandidate("60",
                "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        AnalyzerMappingConfirmation stored = storedConfirmation(former, exactRequest());
        when(confirmationDAO.findLatestByAnalyzerId("42")).thenReturn(Optional.of(stored));

        AnalyzerMappingConfirmationView status = service.getStatus(completeCandidate("61", BINDING_FINGERPRINT),
                RECOGNITION_FINGERPRINT);

        assertEquals(AnalyzerMappingConfirmationView.State.STALE, status.state());
        assertEquals(former.mapping().getMappingFingerprint(), status.mappingFingerprint());
        assertEquals(exactRequest().confirmedRows(), status.confirmedRows());
    }

    @Test
    public void reportsAConfirmationAsStaleWhenControlRecognitionChanges() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation stored = storedConfirmation(candidate, exactRequest());
        when(confirmationDAO.findLatestByAnalyzerId("42")).thenReturn(Optional.of(stored));
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(stored));

        AnalyzerMappingConfirmationView status = service.getStatus(candidate,
                "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd");
        AnalyzerMappingVerificationAssessment assessment = service.assessCurrent(candidate,
                "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd");

        assertEquals(AnalyzerMappingConfirmationView.State.STALE, status.state());
        assertEquals(RECOGNITION_FINGERPRINT, status.recognitionFingerprint());
        assertTrue(assessment.mappingsCurrent());
        assertFalse(assessment.recognitionCurrent());
        assertEquals(Optional.empty(), assessment.currentConfirmation());
        assertFalse(service.hasMatchingConfirmation(candidate, "sha256:" + "d".repeat(64)));
    }

    @Test
    public void reportsHistoricalConfirmationWithoutDurableEvidenceAsStale() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation stored = storedConfirmation(candidate, exactRequest());
        stored.setProfileRevisionFingerprint(null);
        stored.setAuditEventId(null);
        when(confirmationDAO.findLatestByAnalyzerId("42")).thenReturn(Optional.of(stored));

        AnalyzerMappingConfirmationView status = service.getStatus(candidate, RECOGNITION_FINGERPRINT);

        assertEquals(AnalyzerMappingConfirmationView.State.STALE, status.state());
    }

    @Test
    public void confirmationMustMatchTheExactPinnedProfileIdentity() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation stored = storedConfirmation(candidate, exactRequest());
        stored.setProfileId("another.profile");
        stored.setProfileRevision(3);
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(stored));

        assertEquals(Optional.empty(), service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).currentConfirmation());
        assertFalse(service.hasMatchingConfirmation(candidate, RECOGNITION_FINGERPRINT));
    }

    @Test
    public void confirmationRequiresItsDurableActorAndTime() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation stored = storedConfirmation(candidate, exactRequest());
        stored.setConfirmedBy(null);
        stored.setConfirmedAt(null);
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(stored));

        assertEquals(Optional.empty(), service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).currentConfirmation());
        assertFalse(service.hasMatchingConfirmation(candidate, RECOGNITION_FINGERPRINT));
    }

    @Test
    public void reportsAConfirmationAsStaleWhenItsCatalogChoicesAreNoLongerCurrent() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation stored = storedConfirmation(candidate, exactRequest());
        when(confirmationDAO.findLatestByAnalyzerId("42")).thenReturn(Optional.of(stored));
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(stored));
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(List.of());

        AnalyzerMappingConfirmationView status = service.getStatus(candidate, RECOGNITION_FINGERPRINT);
        AnalyzerMappingVerificationAssessment assessment = service.assessCurrent(candidate, RECOGNITION_FINGERPRINT);

        assertEquals(AnalyzerMappingConfirmationView.State.STALE, status.state());
        assertFalse(assessment.mappingsCurrent());
        assertTrue(assessment.recognitionCurrent());
        assertEquals(Optional.empty(), assessment.currentConfirmation());
        assertTrue(service.hasMatchingConfirmation(candidate, RECOGNITION_FINGERPRINT));
    }

    @Test
    public void reportsAConfirmationAsStaleWhenItsResultOptionMovesToAnotherTest() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        when(confirmationDAO.findLatestByAnalyzerId("42"))
                .thenReturn(Optional.of(storedConfirmation(candidate, exactRequest())));
        when(mappingCatalogService.getActiveResultOptions("1")).thenReturn(List.of());

        AnalyzerMappingConfirmationView status = service.getStatus(candidate, RECOGNITION_FINGERPRINT);

        assertEquals(AnalyzerMappingConfirmationView.State.STALE, status.state());
    }

    @Test
    public void exactSavedRowsAreRequiredForAConfirmationToRemainCurrent() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation stored = storedConfirmation(candidate, exactRequest());
        stored.setConfirmedRowsJson(
                new ObjectMapper().writeValueAsString(List.of(new AnalyzerMappingSourceRow("RAW-A", null))));
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(stored));
        when(confirmationDAO.findLatestByAnalyzerId("42")).thenReturn(Optional.of(stored));

        assertEquals(Optional.empty(), service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).currentConfirmation());
        assertFalse(service.hasMatchingConfirmation(candidate, RECOGNITION_FINGERPRINT));
        assertEquals(AnalyzerMappingConfirmationView.State.STALE,
                service.getStatus(candidate, RECOGNITION_FINGERPRINT).state());
    }

    @Test
    public void malformedSavedRowsAreStaleInsteadOfBreakingVerificationStatus() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation stored = storedConfirmation(candidate, exactRequest());
        stored.setConfirmedRowsJson("not-json");
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(stored));
        when(confirmationDAO.findLatestByAnalyzerId("42")).thenReturn(Optional.of(stored));

        assertEquals(Optional.empty(), service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).currentConfirmation());
        assertFalse(service.hasMatchingConfirmation(candidate, RECOGNITION_FINGERPRINT));
        AnalyzerMappingConfirmationView status = service.getStatus(candidate, RECOGNITION_FINGERPRINT);
        assertEquals(AnalyzerMappingConfirmationView.State.STALE, status.state());
        assertEquals(List.of(), status.confirmedRows());
    }

    @Test
    public void rejectsCatalogChoicesThatAreNoLongerCurrentBeforeWriting() {
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.confirm(completeCandidate("61", BINDING_FINGERPRINT), RECOGNITION_FINGERPRINT,
                        exactRequest(), "17"));

        verify(confirmationDAO, never()).insert(any());
        verify(auditTrailService, never()).saveNewHistory(any(), any(), any());
    }

    @Test
    public void aConfirmationKeepsHowTheServedProfileReadsEachRowAndTheType() {
        AnalyzerMappingConfirmation saved = confirmed();

        assertEquals(PROFILE_FINGERPRINT, saved.getReadingsFingerprint());
        assertTrue(saved.getRowReadingsJson().contains("RAW-A"));
        assertTrue(saved.getTypeReadingJson().contains("identifier_pattern"));
    }

    @Test
    public void aDeliveryReadWithTheConfirmedContentChangesNothingAndAsksTheBridgeNothing() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));

        AnalyzerConfirmationReading reading = service.readingOnDelivery(candidate, "site.mock-analyzer", 2,
                PROFILE_FINGERPRINT);

        assertFalse(reading.typeChanged());
        assertTrue(reading.changedRows().isEmpty());
        verify(profileCatalogService, never()).getProfile("site.mock-analyzer", 2);
    }

    @Test
    public void anEditThatReadsEveryRowAlikeChangesNothingAndRemembersTheNewContent() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "copies/mL", "DETECTED", "MOCK"));

        AnalyzerConfirmationReading reading = service.readingOnDelivery(candidate, "site.mock-analyzer", 2,
                EDITED_FINGERPRINT);

        assertFalse(reading.typeChanged());
        assertTrue(reading.changedRows().isEmpty());
        assertEquals(EDITED_FINGERPRINT, saved.getReadingsFingerprint());
        verify(confirmationDAO).update(saved);
    }

    @Test
    public void anOlderQueuedRevisionIsComparedWithTheConfirmedReadingBeforeItIsHeld() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        BridgeProfileCatalog.ProfileRevision older = served(EDITED_FINGERPRINT, "copies/mL", "DETECTED", "MOCK");
        ((com.fasterxml.jackson.databind.node.ObjectNode) older.profile().path("catalog")).put("revision", 1);
        when(profileCatalogService.getProfile("site.mock-analyzer", 1)).thenReturn(older);

        AnalyzerConfirmationReading queued = service.readingOnDelivery(candidate, "site.mock-analyzer", 1,
                EDITED_FINGERPRINT);

        assertTrue("an older equivalent reading does not require operator recovery", queued.isUnchanged());
        assertTrue(queued.arrival().forRecord("RAW-A", null).contains("copies/mL"));
        verify(profileCatalogService).getProfile("site.mock-analyzer", 1);
        verify(profileCatalogService, never()).getProfile("site.mock-analyzer", 2);
    }

    @Test
    public void aQueuedDeliveryCannotBorrowReadingsFromANewerEditOfItsRevision() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        String newerFingerprint = "sha256:" + "f".repeat(64);
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(newerFingerprint, "copies/mL", "DETECTED", "MOCK"));

        AnalyzerConfirmationReading queued = service.readingOnDelivery(candidate, "site.mock-analyzer", 2,
                EDITED_FINGERPRINT);

        assertTrue(queued.typeChanged());
        assertNull(queued.arrival());
        assertEquals(PROFILE_FINGERPRINT, saved.getReadingsFingerprint());
        verify(confirmationDAO, never()).update(any());

        saved.setReadingsFingerprint(null);
        saved.setTypeReadingJson(null);
        saved.setRowReadingsJson(null);
        assertTrue(service.readingOnDelivery(candidate, "site.mock-analyzer", 2, EDITED_FINGERPRINT).typeChanged());
        assertNull("a mismatched source cannot establish a legacy baseline", saved.getReadingsFingerprint());
        verify(confirmationDAO, never()).update(any());
    }

    @Test
    public void changingTheFileProtocolFormatRequiresReconfirmationAndPreventsRecovery() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        BridgeProfileCatalog.ProfileRevision csv = served(PROFILE_FINGERPRINT, "copies/mL", "DETECTED", "MOCK");
        ((com.fasterxml.jackson.databind.node.ObjectNode) csv.profile()).putObject("protocol").put("name", "FILE")
                .put("format", "CSV");
        when(profileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(csv);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        when(confirmationDAO.findLatestByAnalyzerId("42")).thenReturn(Optional.of(saved));
        BridgeProfileCatalog.ProfileRevision xlsx = served(EDITED_FINGERPRINT, "copies/mL", "DETECTED", "MOCK");
        ((com.fasterxml.jackson.databind.node.ObjectNode) xlsx.profile()).putObject("protocol").put("name", "FILE")
                .put("format", "XLSX");
        when(profileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(xlsx);

        AnalyzerConfirmationReading changed = service.readingOnDelivery(candidate, "site.mock-analyzer", 2,
                EDITED_FINGERPRINT);

        assertTrue(changed.typeChanged());
        assertEquals(PROFILE_FINGERPRINT, saved.getReadingsFingerprint());
        assertFalse(service.readsAsConfirmed("61", "RAW-A", null, changed.arrival().forRecord("RAW-A", null)));
        assertFalse(service.standsAsServed("61", xlsx));
        assertEquals(AnalyzerMappingConfirmationView.State.STALE,
                service.getStatus(candidate, RECOGNITION_FINGERPRINT).state());
    }

    @Test
    public void aUnitChangeHoldsOnlyThatTest() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "IU/mL", "DETECTED", "MOCK"));

        AnalyzerConfirmationReading reading = service.readingOnDelivery(candidate, "site.mock-analyzer", 2,
                EDITED_FINGERPRINT);

        assertFalse(reading.typeChanged());
        assertEquals(java.util.Set.of(new AnalyzerMappingSourceRow("RAW-A", null, "")), reading.changedRows());
    }

    @Test
    public void aTranslationChangeHoldsOnlyThatValue() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "copies/mL", "POSITIVE", "MOCK"));

        AnalyzerConfirmationReading reading = service.readingOnDelivery(candidate, "site.mock-analyzer", 2,
                EDITED_FINGERPRINT);

        assertEquals(java.util.Set.of(new AnalyzerMappingSourceRow("RAW-A", "Detected", "")), reading.changedRows());
    }

    @Test
    public void aRoutingChangeHoldsTheWholeAnalyzer() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "copies/mL", "DETECTED", "OTHER"));

        assertTrue(service.readingOnDelivery(candidate, "site.mock-analyzer", 2, EDITED_FINGERPRINT).typeChanged());
    }

    @Test
    public void aConfirmationFromBeforeReadingsGainsThemOnItsNextDelivery() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation legacy = storedConfirmation(candidate, exactRequest());
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(legacy));

        AnalyzerConfirmationReading reading = service.readingOnDelivery(candidate, "site.mock-analyzer", 2,
                PROFILE_FINGERPRINT);

        assertFalse(reading.typeChanged());
        assertEquals(PROFILE_FINGERPRINT, legacy.getReadingsFingerprint());
        assertTrue(legacy.getRowReadingsJson().contains("RAW-A"));
        verify(confirmationDAO).update(legacy);
    }

    @Test
    public void aLegacyConfirmationStartsTrackingReadingsWithoutAWholeFileFingerprintGate() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation legacy = storedConfirmation(candidate, exactRequest());
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(legacy));
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "copies/mL", "DETECTED", "MOCK"));

        AnalyzerConfirmationReading first = service.readingOnDelivery(candidate, "site.mock-analyzer", 2,
                EDITED_FINGERPRINT);

        assertTrue(first.isUnchanged());
        assertEquals(EDITED_FINGERPRINT, legacy.getReadingsFingerprint());
        assertTrue(legacy.getRowReadingsJson().contains("RAW-A"));
        verify(confirmationDAO).update(legacy);

        // Once readings exist, a later unit change still holds just the affected test.
        String laterFingerprint = "sha256:" + "f".repeat(64);
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(laterFingerprint, "IU/mL", "DETECTED", "MOCK"));
        AnalyzerConfirmationReading changed = service.readingOnDelivery(candidate, "site.mock-analyzer", 2,
                laterFingerprint);
        assertFalse(changed.typeChanged());
        assertEquals(java.util.Set.of(new AnalyzerMappingSourceRow("RAW-A", null, "")), changed.changedRows());
        assertEquals(EDITED_FINGERPRINT, legacy.getReadingsFingerprint());
    }

    @Test
    public void aLegacyConfirmationIsNotInvalidatedByAChangedServedFingerprint() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation legacy = storedConfirmation(candidate, exactRequest());
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(legacy));
        when(confirmationDAO.findLatestByAnalyzerId("42")).thenReturn(Optional.of(legacy));
        BridgeProfileCatalog.ProfileRevision edited = served(EDITED_FINGERPRINT, "copies/mL", "DETECTED", "MOCK");
        when(profileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(edited);

        AnalyzerMappingConfirmationView status = service.getStatus(candidate, RECOGNITION_FINGERPRINT);
        assertEquals(AnalyzerMappingConfirmationView.State.CURRENT, status.state());
        assertFalse(status.typeReadingChanged());
        assertTrue(status.rowsReadDifferently().isEmpty());
        assertTrue(service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).currentConfirmation().isPresent());
        assertTrue(service.standsAsServed("61", edited));
        assertNull("read-only checks must not manufacture historical readings", legacy.getReadingsFingerprint());
        verify(confirmationDAO, never()).update(any());
    }

    @Test
    public void confirmingAgainAfterAChangeRecordsTheNewReadingsAndWhoConfirmedThem() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "IU/mL", "DETECTED", "MOCK"));

        AnalyzerMappingConfirmationView again = service.confirm(candidate, RECOGNITION_FINGERPRINT, exactRequest(),
                "18");

        assertEquals(AnalyzerMappingConfirmationView.State.CURRENT, again.state());
        assertEquals(EDITED_FINGERPRINT, saved.getReadingsFingerprint());
        assertTrue(saved.getRowReadingsJson().contains("IU/mL"));
        assertEquals("18", saved.getConfirmedBy());
        verify(auditTrailService).saveHistory(eq(saved), any(AnalyzerMappingConfirmation.class), eq("18"), eq("U"),
                eq("analyzer_mapping_confirmation"));
    }

    @Test
    public void verificationIsStaleWhileAConfirmedRowReadsDifferently() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findLatestByAnalyzerId("42")).thenReturn(Optional.of(saved));
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "IU/mL", "DETECTED", "MOCK"));

        AnalyzerMappingConfirmationView status = service.getStatus(candidate, RECOGNITION_FINGERPRINT);
        assertEquals(AnalyzerMappingConfirmationView.State.STALE, status.state());
        assertFalse(status.typeReadingChanged());
        assertEquals(List.of(new AnalyzerMappingSourceRow("RAW-A", null, "")), status.rowsReadDifferently());
    }

    @Test
    public void verificationSaysWhenTheWholeTypeReadsDifferently() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findLatestByAnalyzerId("42")).thenReturn(Optional.of(saved));
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "copies/mL", "DETECTED", "OTHER"));

        AnalyzerMappingConfirmationView status = service.getStatus(candidate, RECOGNITION_FINGERPRINT);

        assertEquals(AnalyzerMappingConfirmationView.State.STALE, status.state());
        assertTrue(status.typeReadingChanged());
    }

    @Test
    public void aDeliveryThatDoesNotSayWhatItWasReadWithIsComparedWithTheServedProfile() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "IU/mL", "DETECTED", "MOCK"));

        AnalyzerConfirmationReading reading = service.readingOnDelivery(candidate, "site.mock-analyzer", 2, null);

        assertEquals(java.util.Set.of(new AnalyzerMappingSourceRow("RAW-A", null, "")), reading.changedRows());
        assertNull("how it arrived is not known", reading.arrival());
    }

    @Test
    public void aHeldRecordReadsAsConfirmedOnlyWhenItArrivedReadingTheConfirmedWay() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        String arrivedAsConfirmed = service.readingOnDelivery(candidate, "site.mock-analyzer", 2, PROFILE_FINGERPRINT)
                .arrival().forRecord("RAW-A", null);
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "IU/mL", "DETECTED", "MOCK"));
        String arrivedInIu = service.readingOnDelivery(candidate, "site.mock-analyzer", 2, EDITED_FINGERPRINT).arrival()
                .forRecord("RAW-A", null);
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "copies/mL", "DETECTED", "OTHER"));
        String arrivedRoutedElsewhere = service
                .readingOnDelivery(candidate, "site.mock-analyzer", 2, EDITED_FINGERPRINT).arrival()
                .forRecord("RAW-A", null);

        assertTrue(service.readsAsConfirmed("61", "RAW-A", null, arrivedAsConfirmed));
        assertFalse("parsed in another unit", service.readsAsConfirmed("61", "RAW-A", null, arrivedInIu));
        assertTrue("routing does not change a parsed result",
                service.readsAsConfirmed("61", "RAW-A", null, arrivedRoutedElsewhere));
        assertFalse(service.readsAsConfirmed("61", "RAW-A", null, null));
    }

    @Test
    public void recoveryRejectsAnArrivalValueMissingFromTheCurrentConfirmation() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        BridgeProfileCatalog.ProfileRevision arrival = served(EDITED_FINGERPRINT, "copies/mL", "DETECTED", "MOCK");
        com.fasterxml.jackson.databind.node.ObjectNode test = (com.fasterxml.jackson.databind.node.ObjectNode) arrival
                .profile().path("default_test_mappings").get(0);
        test.withArray("values").add("Removed");
        when(profileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(arrival);
        String reading = service.readingOnDelivery(candidate, "site.mock-analyzer", 2, EDITED_FINGERPRINT).arrival()
                .forRecord("RAW-A", null);

        assertFalse(service.readsAsConfirmed("61", "RAW-A", null, reading));
    }

    @Test
    public void recoveryRequiresEveryArrivalSpellingButAllowsNewConfirmedValues() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        String reading = service.readingOnDelivery(candidate, "site.mock-analyzer", 2, PROFILE_FINGERPRINT).arrival()
                .forRecord("RAW-A", null);
        ObjectMapper json = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode withMissingSpelling = (com.fasterxml.jackson.databind.node.ObjectNode) json
                .readTree(reading);
        withMissingSpelling.withArray("rows").addObject().put("value", "REMOVED SPELLING").putArray("reading")
                .add("Detected");
        assertFalse(service.readsAsConfirmed("61", "RAW-A", null, withMissingSpelling.toString()));

        com.fasterxml.jackson.databind.node.ObjectNode beforeValuesWereAdded = (com.fasterxml.jackson.databind.node.ObjectNode) json
                .readTree(reading);
        com.fasterxml.jackson.databind.node.ArrayNode rows = beforeValuesWereAdded.putArray("rows");
        json.readTree(reading).path("rows").forEach(row -> {
            if (!row.has("value")) {
                rows.add(row);
            }
        });
        assertTrue(service.readsAsConfirmed("61", "RAW-A", null, beforeValuesWereAdded.toString()));
    }

    @Test
    public void aConfirmationStandsOnlyAgainstAServedProfileThatReadsAsConfirmed() {
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));

        assertTrue(service.standsAsServed("61", served(PROFILE_FINGERPRINT, "copies/mL", "DETECTED", "MOCK")));
        assertFalse(service.standsAsServed("61", served(EDITED_FINGERPRINT, "IU/mL", "DETECTED", "MOCK")));
        assertFalse("the Bridge could not serve it", service.standsAsServed("61", null));
    }

    @Test
    public void aConfirmationTakenWhileTheBridgeCannotServeTheProfileIsSavedWithoutReadings() {
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenThrow(new BridgeProfileCatalogException("Bridge catalog unreachable"));

        AnalyzerMappingConfirmationView confirmed = service.confirm(completeCandidate("61", BINDING_FINGERPRINT),
                RECOGNITION_FINGERPRINT, exactRequest(), "17");

        ArgumentCaptor<AnalyzerMappingConfirmation> saved = ArgumentCaptor.forClass(AnalyzerMappingConfirmation.class);
        verify(confirmationDAO).insert(saved.capture());
        assertEquals(AnalyzerMappingConfirmationView.State.CURRENT, confirmed.state());
        assertNull(saved.getValue().getReadingsFingerprint());
        assertNull(saved.getValue().getRowReadingsJson());
    }

    @Test
    public void unreadableLegacyDeliveryIsHeldUntilASemanticBaselineCanBeEstablished() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        saved.setReadingsFingerprint(null);
        saved.setTypeReadingJson(null);
        saved.setRowReadingsJson(null);
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenThrow(new BridgeProfileCatalogException("offline"));
        assertTrue(service.readingOnDelivery(candidate, "site.mock-analyzer", 2, PROFILE_FINGERPRINT).typeChanged());
        assertTrue(service.readingOnDelivery(candidate, "site.mock-analyzer", 2, EDITED_FINGERPRINT).typeChanged());
        verify(confirmationDAO, never()).update(any());
        org.mockito.Mockito.doReturn(served(EDITED_FINGERPRINT, "copies/mL", "DETECTED", "MOCK"))
                .when(profileCatalogService).getProfile("site.mock-analyzer", 2);
        assertTrue(service.readingOnDelivery(candidate, "site.mock-analyzer", 2, EDITED_FINGERPRINT).isUnchanged());
    }

    @Test
    public void reconfirmationFailsWithoutChangingTheSavedConfirmationWhenProfileIsUnavailable() {
        AnalyzerMappingConfirmation saved = confirmed();
        Timestamp before = saved.getConfirmedAt();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenThrow(new BridgeProfileCatalogException("offline"));
        assertThrows(AnalyzerRequestException.class, () -> service.confirm(completeCandidate("61", BINDING_FINGERPRINT),
                RECOGNITION_FINGERPRINT, exactRequest(), "17"));
        assertEquals(before, saved.getConfirmedAt());
        assertEquals(PROFILE_FINGERPRINT, saved.getReadingsFingerprint());
        verify(confirmationDAO, never()).update(any());
        verify(auditTrailService, never()).saveNewHistory(any(), any(), any());
    }

    @Test
    public void undeclaredCodeRecoveryRequiresExplicitConfirmationAndMatchingParsing() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        String arrival = service.readingOnDelivery(candidate, "site.mock-analyzer", 2, PROFILE_FINGERPRINT).arrival()
                .forRecord("RSVX", null);
        assertFalse(service.readsAsConfirmed("61", "RSVX", null, arrival));
        var rows = new ObjectMapper().readTree(saved.getConfirmedRowsJson());
        ((com.fasterxml.jackson.databind.node.ArrayNode) rows).addObject().put("sourceRowKey", "RSVX")
                .putNull("rawValue").put("subIdentity", "");
        saved.setConfirmedRowsJson(rows.toString());
        assertTrue(service.readsAsConfirmed("61", "RSVX", null, arrival));
        saved.setExcludedRowsJson(saved.getConfirmedRowsJson());
        saved.setConfirmedRowsJson("[]");
        assertTrue("an explicit exclusion must also let an undeclared held row leave review",
                service.readsAsConfirmed("61", "RSVX", null, arrival));
        assertFalse("declared records cannot recover from missing arrival readings",
                service.readsAsConfirmed("61", "RAW-A", null, arrival));
        saved.setTypeReadingJson("{\"configDefaults.numberFormat\":\",\"}");
        assertFalse(service.readsAsConfirmed("61", "RSVX", null, arrival));
    }

    @Test
    public void anUnavailableIndividualConnectionDoesNotBreakTheTypeCatalog() {
        AnalyzerMappingConfirmation saved = confirmed();
        saved.getMapping().getAnalyzer().setBridgeConnectionId("missing-connection");
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        BridgeProfileCatalog.ProfileRevision profile = served(PROFILE_FINGERPRINT, "copies/mL", "DETECTED", "MOCK");
        assertFalse(service.standsAsServed("61", profile));
        when(connectionClient.getConnection("missing-connection"))
                .thenThrow(new BridgeAnalyzerConnectionException("connection unavailable"));
        assertFalse(service.standsAsServed("61", profile));
    }

    @Test
    public void changingInstrumentCodeOverridesRequiresReconfirmationEvenWhenProfileIsUnchanged() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        candidate.mapping().getAnalyzer().setBridgeConnectionId("bridge-42");
        var connection = new ObjectMapper().createObjectNode();
        connection.putObject("values").putObject("codeOverrides").put("RAW-A", "INST-1");
        when(connectionClient.getConnection("bridge-42")).thenReturn(connection);
        service.confirm(candidate, RECOGNITION_FINGERPRINT, exactRequest(), "17");
        ArgumentCaptor<AnalyzerMappingConfirmation> capture = ArgumentCaptor
                .forClass(AnalyzerMappingConfirmation.class);
        verify(confirmationDAO).insert(capture.capture());
        AnalyzerMappingConfirmation saved = capture.getValue();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        when(confirmationDAO.findLatestByAnalyzerId("42")).thenReturn(Optional.of(saved));
        assertTrue(service.readingOnDelivery(candidate, "site.mock-analyzer", 2, PROFILE_FINGERPRINT).isUnchanged());
        connection.with("values").with("codeOverrides").put("RAW-A", "INST-2");
        assertTrue(service.readingOnDelivery(candidate, "site.mock-analyzer", 2, PROFILE_FINGERPRINT).typeChanged());
        assertEquals(AnalyzerMappingConfirmationView.State.STALE,
                service.getStatus(candidate, RECOGNITION_FINGERPRINT).state());
        assertFalse(service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).recognitionCurrent());
        service.confirm(candidate, RECOGNITION_FINGERPRINT, exactRequest(), "17");
        assertTrue(service.readingOnDelivery(candidate, "site.mock-analyzer", 2, PROFILE_FINGERPRINT).isUnchanged());
        connection.with("values").put("host", "renamed-host");
        assertEquals(AnalyzerMappingConfirmationView.State.CURRENT,
                service.getStatus(candidate, RECOGNITION_FINGERPRINT).state());
        connection.with("values").with("codeOverrides").removeAll();
        assertTrue("removing an override also changes interpretation",
                service.readingOnDelivery(candidate, "site.mock-analyzer", 2, PROFILE_FINGERPRINT).typeChanged());
    }

    @Test
    public void changingConnectionNumberFormatRequiresReconfirmationAndBlocksOldRecovery() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        candidate.mapping().getAnalyzer().setBridgeConnectionId("bridge-42");
        var connection = new ObjectMapper().createObjectNode();
        connection.putObject("values").put("numberFormat", ".");
        when(connectionClient.getConnection("bridge-42")).thenReturn(connection);
        service.confirm(candidate, RECOGNITION_FINGERPRINT, exactRequest(), "17");
        ArgumentCaptor<AnalyzerMappingConfirmation> capture = ArgumentCaptor
                .forClass(AnalyzerMappingConfirmation.class);
        verify(confirmationDAO).insert(capture.capture());
        AnalyzerMappingConfirmation saved = capture.getValue();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        when(confirmationDAO.findLatestByAnalyzerId("42")).thenReturn(Optional.of(saved));
        String arrival = service.readingOnDelivery(candidate, "site.mock-analyzer", 2, PROFILE_FINGERPRINT).arrival()
                .forRecord("RAW-A", null);
        assertTrue(service.readsAsConfirmed("61", "RAW-A", null, arrival));

        connection.with("values").put("numberFormat", ",");
        assertTrue(service.readingOnDelivery(candidate, "site.mock-analyzer", 2, PROFILE_FINGERPRINT).typeChanged());
        assertEquals(AnalyzerMappingConfirmationView.State.STALE,
                service.getStatus(candidate, RECOGNITION_FINGERPRINT).state());
        assertFalse(service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).recognitionCurrent());
        service.confirm(candidate, RECOGNITION_FINGERPRINT, exactRequest(), "17");
        assertTrue(service.readingOnDelivery(candidate, "site.mock-analyzer", 2, PROFILE_FINGERPRINT).isUnchanged());
        assertFalse("results parsed with the old decimal separator cannot recover under the new one",
                service.readsAsConfirmed("61", "RAW-A", null, arrival));

        connection.with("values").put("host", "renamed-host");
        assertEquals(AnalyzerMappingConfirmationView.State.CURRENT,
                service.getStatus(candidate, RECOGNITION_FINGERPRINT).state());
        connection.with("values").remove("numberFormat");
        assertTrue("restoring the profile default also requires reconfirmation",
                service.readingOnDelivery(candidate, "site.mock-analyzer", 2, PROFILE_FINGERPRINT).typeChanged());
    }

    /** A first confirmation of the complete candidate, as saved. */
    private AnalyzerMappingConfirmation confirmed() {
        service.confirm(completeCandidate("61", BINDING_FINGERPRINT), RECOGNITION_FINGERPRINT, exactRequest(), "17");
        ArgumentCaptor<AnalyzerMappingConfirmation> saved = ArgumentCaptor.forClass(AnalyzerMappingConfirmation.class);
        verify(confirmationDAO).insert(saved.capture());
        org.mockito.Mockito.clearInvocations(confirmationDAO, profileCatalogService, auditTrailService);
        return saved.getValue();
    }

    /** The profile the Bridge serves for site.mock-analyzer revision 2. */
    private static BridgeProfileCatalog.ProfileRevision served(String fingerprint, String unit, String detectedFrom,
            String identifierPattern) {
        ObjectMapper json = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode profile = json.createObjectNode();
        profile.putObject("profileMeta").put("id", "site.mock-analyzer").put("displayName", "Mock analyzer");
        profile.putObject("catalog").put("revision", 2).put("revisionFingerprint", fingerprint).put("source", "SITE")
                .put("status", "ACTIVE");
        profile.putObject("protocol").put("name", "ASTM");
        profile.put("identifier_pattern", identifierPattern);
        com.fasterxml.jackson.databind.node.ArrayNode tests = profile.putArray("default_test_mappings");
        com.fasterxml.jackson.databind.node.ObjectNode rawA = tests.addObject().put("test_code", "RAW-A")
                .put("loinc", "1234-5").put("result_type", "qualitative").put("unit", unit);
        rawA.putArray("values").add("Detected");
        rawA.putObject("translations").putArray("Detected").add(detectedFrom);
        tests.addObject().put("test_code", "RAW-B").put("loinc", "1234-6").putArray("values").add("Invalid");
        return new BridgeProfileCatalog.ProfileRevision(profile, json.createObjectNode());
    }

    private static AnalyzerMappingConfirmationRequest exactRequest() {
        return new AnalyzerMappingConfirmationRequest(BINDING_FINGERPRINT, RECOGNITION_FINGERPRINT,
                List.of(new AnalyzerMappingSourceRow("RAW-A", null), new AnalyzerMappingSourceRow("RAW-A", "Detected")),
                List.of(new AnalyzerMappingSourceRow("RAW-B", null), new AnalyzerMappingSourceRow("RAW-B", "Invalid")));
    }

    private static AnalyzerMappingSnapshot completeCandidate(String revisionId, String fingerprint) {
        Analyzer analyzer = new Analyzer();
        analyzer.setId("42");

        AnalyzerMapping revision = new AnalyzerMapping();
        revision.setId(revisionId);
        revision.setAnalyzer(analyzer);
        revision.setRevisionNumber("60".equals(revisionId) ? 3 : 4);
        revision.setProfileId("site.mock-analyzer");
        revision.setProfileRevision(2);
        revision.setProfileFingerprint(PROFILE_FINGERPRINT);
        revision.setMappingFingerprint(fingerprint);

        AnalyzerMappingTest bound = test(revision, "RAW-A", AnalyzerMappingState.BOUND);
        AnalyzerMappingTest excluded = test(revision, "RAW-B", AnalyzerMappingState.EXCLUDED);
        AnalyzerMappingResult boundResult = result(revision, "RAW-A", "Detected", AnalyzerMappingState.BOUND);
        AnalyzerMappingResult excludedResult = result(revision, "RAW-B", "Invalid", AnalyzerMappingState.EXCLUDED);
        return new AnalyzerMappingSnapshot(revision, List.of(bound, excluded), List.of(boundResult, excludedResult));
    }

    private static AnalyzerMappingTest test(AnalyzerMapping revision, String sourceRowKey, AnalyzerMappingState state) {
        AnalyzerMappingTest test = new AnalyzerMappingTest();
        test.setId(new AnalyzerMappingTestPK(revision.getId(), sourceRowKey));
        test.setMapping(revision);
        test.setMappingState(state);
        test.setTestId(state == AnalyzerMappingState.BOUND ? "1" : null);
        return test;
    }

    private static AnalyzerMappingResult result(AnalyzerMapping revision, String sourceRowKey, String rawValue,
            AnalyzerMappingState state) {
        AnalyzerMappingResult result = new AnalyzerMappingResult();
        result.setId(new AnalyzerMappingResultPK(revision.getId(), sourceRowKey, rawValue));
        result.setMapping(revision);
        result.setMappingState(state);
        result.setTestResultId(state == AnalyzerMappingState.BOUND ? "11" : null);
        return result;
    }

    private static AnalyzerMappingConfirmation storedConfirmation(AnalyzerMappingSnapshot snapshot,
            AnalyzerMappingConfirmationRequest request) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AnalyzerMappingConfirmation confirmation = new AnalyzerMappingConfirmation();
        confirmation.setId("70");
        confirmation.setMapping(snapshot.mapping());
        confirmation.setProfileId("site.mock-analyzer");
        confirmation.setProfileRevision(2);
        confirmation.setProfileRevisionFingerprint(PROFILE_FINGERPRINT);
        confirmation.setMappingFingerprint(snapshot.mapping().getMappingFingerprint());
        confirmation.setRecognitionFingerprint(RECOGNITION_FINGERPRINT);
        confirmation.setAuditEventId("90");
        confirmation.setConfirmedRowsJson(mapper.writeValueAsString(request.confirmedRows()));
        confirmation.setExcludedRowsJson(mapper.writeValueAsString(request.excludedRows()));
        confirmation.setConfirmedBy("16");
        confirmation.setConfirmedAt(Timestamp.from(Instant.parse("2026-08-22T10:00:00Z")));
        return confirmation;
    }
}
