package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResultPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTestPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerMappingConfirmationServiceTest {

    private static final String BINDING_FINGERPRINT = "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String RECOGNITION_FINGERPRINT = "sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
    private static final String PROFILE_FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Mock
    private AnalyzerMappingConfirmationDAO confirmationDAO;

    @Mock
    private AuditTrailService auditTrailService;

    @Mock
    private SystemUserService systemUserService;

    @Mock
    private AnalyzerMappingCatalogService mappingCatalogService;

    private AnalyzerMappingConfirmationService service;

    @Before
    public void setUp() {
        service = new AnalyzerMappingConfirmationServiceImpl(confirmationDAO, auditTrailService, systemUserService,
                mappingCatalogService);
        SystemUser actor = new SystemUser();
        actor.setId("17");
        actor.setFirstName("Ada");
        actor.setLastName("Lovelace");
        actor.setLoginName("ada");
        when(systemUserService.getUserById("17")).thenReturn(actor);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(
                List.of(new AnalyzerMappingCatalogService.TestOption("1", "Mock Test", "MOCK", List.of("1234-5"))));
        when(mappingCatalogService.getActiveResultOptions("1"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("11", "DETECTED", "Detected")));
        when(confirmationDAO.insert(any(AnalyzerMappingConfirmation.class))).thenAnswer(invocation -> {
            AnalyzerMappingConfirmation confirmation = invocation.getArgument(0);
            confirmation.setId("71");
            return confirmation.getId();
        });
        when(auditTrailService.saveNewHistory(any(AnalyzerMappingConfirmation.class), eq("17"),
                eq("analyzer_site_binding_confirmation"))).thenReturn("91");
    }

    @Test
    public void confirmsTheExactImmutableCandidateAndAuditsTheActor() {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmationRequest request = exactRequest();

        AnalyzerMappingConfirmationView confirmed = service.confirm(candidate, RECOGNITION_FINGERPRINT, request, "17");

        ArgumentCaptor<AnalyzerMappingConfirmation> saved = ArgumentCaptor.forClass(AnalyzerMappingConfirmation.class);
        verify(confirmationDAO).insert(saved.capture());
        assertSame(candidate.revision(), saved.getValue().getSiteBindingRevision());
        assertEquals("site.mock-analyzer", saved.getValue().getProfileId());
        assertEquals(2, saved.getValue().getProfileRevision());
        assertEquals(PROFILE_FINGERPRINT, saved.getValue().getProfileRevisionFingerprint());
        assertEquals(BINDING_FINGERPRINT, saved.getValue().getBindingFingerprint());
        assertEquals(RECOGNITION_FINGERPRINT, saved.getValue().getRecognitionFingerprint());
        assertEquals("91", saved.getValue().getAuditEventId());
        assertEquals("17", saved.getValue().getConfirmedBy());
        assertEquals(AnalyzerMappingConfirmationView.State.CURRENT, confirmed.state());
        assertEquals("17", confirmed.confirmedBy());
        assertEquals("Ada Lovelace", confirmed.confirmedByDisplayName());
        assertEquals(request.confirmedRows(), confirmed.confirmedRows());
        assertEquals(request.excludedRows(), confirmed.excludedRows());
        verify(auditTrailService).saveNewHistory(saved.getValue(), "17", "analyzer_site_binding_confirmation");
    }

    @Test
    public void disabledHistoryLeavesTheSavedConfirmationStaleWithoutASecondWrite() {
        when(auditTrailService.saveNewHistory(any(AnalyzerMappingConfirmation.class), eq("17"),
                eq("analyzer_site_binding_confirmation"))).thenReturn(null);

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
    public void rejectsAStaleBindingOrRecognitionFingerprintBeforeWriting() {
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
        AnalyzerMappingTest pending = test(base.revision(), "PENDING", AnalyzerMappingState.UNRESOLVED);
        AnalyzerMappingSnapshot candidate = new AnalyzerMappingSnapshot(base.binding(), base.revision(),
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
        when(confirmationDAO.findByRevisionId("61")).thenReturn(Optional.of(written.getValue()));
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
    public void reportsAFormerConfirmationAsStaleForANewBindingRevision() throws Exception {
        AnalyzerMappingSnapshot former = completeCandidate("60",
                "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        AnalyzerMappingConfirmation stored = storedConfirmation(former, exactRequest());
        when(confirmationDAO.findLatestByBindingId("51")).thenReturn(Optional.of(stored));

        AnalyzerMappingConfirmationView status = service.getStatus(completeCandidate("61", BINDING_FINGERPRINT),
                RECOGNITION_FINGERPRINT);

        assertEquals(AnalyzerMappingConfirmationView.State.STALE, status.state());
        assertEquals(former.revision().getBindingFingerprint(), status.bindingFingerprint());
        assertEquals(exactRequest().confirmedRows(), status.confirmedRows());
    }

    @Test
    public void reportsAConfirmationAsStaleWhenControlRecognitionChanges() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation stored = storedConfirmation(candidate, exactRequest());
        when(confirmationDAO.findLatestByBindingId("51")).thenReturn(Optional.of(stored));
        when(confirmationDAO.findByRevisionId("61")).thenReturn(Optional.of(stored));

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
        when(confirmationDAO.findLatestByBindingId("51")).thenReturn(Optional.of(stored));

        AnalyzerMappingConfirmationView status = service.getStatus(candidate, RECOGNITION_FINGERPRINT);

        assertEquals(AnalyzerMappingConfirmationView.State.STALE, status.state());
    }

    @Test
    public void confirmationMustMatchTheExactPinnedProfileIdentity() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation stored = storedConfirmation(candidate, exactRequest());
        stored.setProfileId("another.profile");
        stored.setProfileRevision(3);
        when(confirmationDAO.findByRevisionId("61")).thenReturn(Optional.of(stored));

        assertEquals(Optional.empty(), service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).currentConfirmation());
        assertFalse(service.hasMatchingConfirmation(candidate, RECOGNITION_FINGERPRINT));
    }

    @Test
    public void confirmationRequiresItsDurableActorAndTime() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation stored = storedConfirmation(candidate, exactRequest());
        stored.setConfirmedBy(null);
        stored.setConfirmedAt(null);
        when(confirmationDAO.findByRevisionId("61")).thenReturn(Optional.of(stored));

        assertEquals(Optional.empty(), service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).currentConfirmation());
        assertFalse(service.hasMatchingConfirmation(candidate, RECOGNITION_FINGERPRINT));
    }

    @Test
    public void reportsAConfirmationAsStaleWhenItsCatalogBindingIsNoLongerCurrent() throws Exception {
        AnalyzerMappingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerMappingConfirmation stored = storedConfirmation(candidate, exactRequest());
        when(confirmationDAO.findLatestByBindingId("51")).thenReturn(Optional.of(stored));
        when(confirmationDAO.findByRevisionId("61")).thenReturn(Optional.of(stored));
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
        when(confirmationDAO.findLatestByBindingId("51"))
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
        when(confirmationDAO.findByRevisionId("61")).thenReturn(Optional.of(stored));
        when(confirmationDAO.findLatestByBindingId("51")).thenReturn(Optional.of(stored));

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
        when(confirmationDAO.findByRevisionId("61")).thenReturn(Optional.of(stored));
        when(confirmationDAO.findLatestByBindingId("51")).thenReturn(Optional.of(stored));

        assertEquals(Optional.empty(), service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).currentConfirmation());
        assertFalse(service.hasMatchingConfirmation(candidate, RECOGNITION_FINGERPRINT));
        AnalyzerMappingConfirmationView status = service.getStatus(candidate, RECOGNITION_FINGERPRINT);
        assertEquals(AnalyzerMappingConfirmationView.State.STALE, status.state());
        assertEquals(List.of(), status.confirmedRows());
    }

    @Test
    public void rejectsCatalogBindingsThatAreNoLongerCurrentBeforeWriting() {
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.confirm(completeCandidate("61", BINDING_FINGERPRINT), RECOGNITION_FINGERPRINT,
                        exactRequest(), "17"));

        verify(confirmationDAO, never()).insert(any());
        verify(auditTrailService, never()).saveNewHistory(any(), any(), any());
    }

    private static AnalyzerMappingConfirmationRequest exactRequest() {
        return new AnalyzerMappingConfirmationRequest(BINDING_FINGERPRINT, RECOGNITION_FINGERPRINT,
                List.of(new AnalyzerMappingSourceRow("RAW-A", null), new AnalyzerMappingSourceRow("RAW-A", "Detected")),
                List.of(new AnalyzerMappingSourceRow("RAW-B", null), new AnalyzerMappingSourceRow("RAW-B", "Invalid")));
    }

    private static AnalyzerMappingSnapshot completeCandidate(String revisionId, String fingerprint) {
        AnalyzerProfileBinding profile = new AnalyzerProfileBinding();
        profile.setId("41");
        profile.setProfileId("site.mock-analyzer");
        profile.setProfileRevision(2);
        profile.setProfileFingerprint(PROFILE_FINGERPRINT);

        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId("51");
        binding.setProfileBinding(profile);

        AnalyzerMapping revision = new AnalyzerMapping();
        revision.setId(revisionId);
        revision.setSiteBinding(binding);
        revision.setRevisionNumber("60".equals(revisionId) ? 3 : 4);
        revision.setBindingFingerprint(fingerprint);

        AnalyzerMappingTest bound = test(revision, "RAW-A", AnalyzerMappingState.BOUND);
        AnalyzerMappingTest excluded = test(revision, "RAW-B", AnalyzerMappingState.EXCLUDED);
        AnalyzerMappingResult boundResult = result(revision, "RAW-A", "Detected", AnalyzerMappingState.BOUND);
        AnalyzerMappingResult excludedResult = result(revision, "RAW-B", "Invalid", AnalyzerMappingState.EXCLUDED);
        return new AnalyzerMappingSnapshot(binding, revision, List.of(bound, excluded),
                List.of(boundResult, excludedResult));
    }

    private static AnalyzerMappingTest test(AnalyzerMapping revision, String sourceRowKey, AnalyzerMappingState state) {
        AnalyzerMappingTest test = new AnalyzerMappingTest();
        test.setId(new AnalyzerMappingTestPK(revision.getId(), sourceRowKey));
        test.setSiteBindingRevision(revision);
        test.setMappingState(state);
        test.setTestId(state == AnalyzerMappingState.BOUND ? "1" : null);
        return test;
    }

    private static AnalyzerMappingResult result(AnalyzerMapping revision, String sourceRowKey, String rawValue,
            AnalyzerMappingState state) {
        AnalyzerMappingResult result = new AnalyzerMappingResult();
        result.setId(new AnalyzerMappingResultPK(revision.getId(), sourceRowKey, rawValue));
        result.setSiteBindingRevision(revision);
        result.setMappingState(state);
        result.setTestResultId(state == AnalyzerMappingState.BOUND ? "11" : null);
        return result;
    }

    private static AnalyzerMappingConfirmation storedConfirmation(AnalyzerMappingSnapshot snapshot,
            AnalyzerMappingConfirmationRequest request) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AnalyzerMappingConfirmation confirmation = new AnalyzerMappingConfirmation();
        confirmation.setId("70");
        confirmation.setSiteBindingRevision(snapshot.revision());
        confirmation.setProfileId("site.mock-analyzer");
        confirmation.setProfileRevision(2);
        confirmation.setProfileRevisionFingerprint(PROFILE_FINGERPRINT);
        confirmation.setBindingFingerprint(snapshot.revision().getBindingFingerprint());
        confirmation.setRecognitionFingerprint(RECOGNITION_FINGERPRINT);
        confirmation.setAuditEventId("90");
        confirmation.setConfirmedRowsJson(mapper.writeValueAsString(request.confirmedRows()));
        confirmation.setExcludedRowsJson(mapper.writeValueAsString(request.excludedRows()));
        confirmation.setConfirmedBy("16");
        confirmation.setConfirmedAt(Timestamp.from(Instant.parse("2026-08-22T10:00:00Z")));
        return confirmation;
    }
}
