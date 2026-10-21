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

    private AnalyzerMappingConfirmationService service;

    @Before
    public void setUp() {
        service = new AnalyzerMappingConfirmationServiceImpl(confirmationDAO, auditTrailService, systemUserService,
                mappingCatalogService, profileCatalogService);
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

        AnalyzerConfirmationReading reading = service.readingOnDelivery(candidate, PROFILE_FINGERPRINT);

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

        AnalyzerConfirmationReading reading = service.readingOnDelivery(candidate, EDITED_FINGERPRINT);

        assertFalse(reading.typeChanged());
        assertTrue(reading.changedRows().isEmpty());
        assertEquals(EDITED_FINGERPRINT, saved.getReadingsFingerprint());
        verify(confirmationDAO).update(saved);
    }

    @Test
    public void aUnitChangeHoldsOnlyThatTest() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "IU/mL", "DETECTED", "MOCK"));

        AnalyzerConfirmationReading reading = service.readingOnDelivery(candidate, EDITED_FINGERPRINT);

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

        AnalyzerConfirmationReading reading = service.readingOnDelivery(candidate, EDITED_FINGERPRINT);

        assertEquals(java.util.Set.of(new AnalyzerMappingSourceRow("RAW-A", "Detected", "")), reading.changedRows());
    }

    @Test
    public void aRoutingChangeHoldsTheWholeAnalyzer() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "copies/mL", "DETECTED", "OTHER"));

        assertTrue(service.readingOnDelivery(candidate, EDITED_FINGERPRINT).typeChanged());
    }

    @Test
    public void aConfirmationFromBeforeReadingsGainsThemWhileItsFileIsUnchanged() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation legacy = storedConfirmation(candidate, exactRequest());
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(legacy));

        AnalyzerConfirmationReading reading = service.readingOnDelivery(candidate, PROFILE_FINGERPRINT);

        assertFalse(reading.typeChanged());
        assertEquals(PROFILE_FINGERPRINT, legacy.getReadingsFingerprint());
        assertTrue(legacy.getRowReadingsJson().contains("RAW-A"));
        verify(confirmationDAO).update(legacy);
    }

    @Test
    public void aConfirmationFromBeforeReadingsHoldsTheAnalyzerOnceItsFileChanged() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        when(confirmationDAO.findByMappingId("61"))
                .thenReturn(Optional.of(storedConfirmation(candidate, exactRequest())));
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "copies/mL", "DETECTED", "MOCK"));

        assertTrue(service.readingOnDelivery(candidate, EDITED_FINGERPRINT).typeChanged());
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

        AnalyzerConfirmationReading reading = service.readingOnDelivery(candidate, null);

        assertEquals(java.util.Set.of(new AnalyzerMappingSourceRow("RAW-A", null, "")), reading.changedRows());
        assertNull("how it arrived is not known", reading.arrival());
    }

    @Test
    public void aHeldRecordReadsAsConfirmedOnlyWhenItArrivedReadingTheConfirmedWay() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation saved = confirmed();
        when(confirmationDAO.findByMappingId("61")).thenReturn(Optional.of(saved));
        String arrivedAsConfirmed = service.readingOnDelivery(candidate, PROFILE_FINGERPRINT).arrival()
                .forRecord("RAW-A", null);
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "IU/mL", "DETECTED", "MOCK"));
        String arrivedInIu = service.readingOnDelivery(candidate, EDITED_FINGERPRINT).arrival().forRecord("RAW-A",
                null);
        when(profileCatalogService.getProfile("site.mock-analyzer", 2))
                .thenReturn(served(EDITED_FINGERPRINT, "copies/mL", "DETECTED", "OTHER"));
        String arrivedRoutedElsewhere = service.readingOnDelivery(candidate, EDITED_FINGERPRINT).arrival()
                .forRecord("RAW-A", null);

        assertTrue(service.readsAsConfirmed("61", "RAW-A", null, arrivedAsConfirmed));
        assertFalse("parsed in another unit", service.readsAsConfirmed("61", "RAW-A", null, arrivedInIu));
        assertTrue("routing does not change a parsed result",
                service.readsAsConfirmed("61", "RAW-A", null, arrivedRoutedElsewhere));
        assertFalse(service.readsAsConfirmed("61", "RAW-A", null, null));
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
