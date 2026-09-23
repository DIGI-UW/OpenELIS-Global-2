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
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingConfirmationDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResultPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTestPK;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerSiteBindingConfirmationServiceTest {

    private static final String BINDING_FINGERPRINT = "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String RECOGNITION_FINGERPRINT = "sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
    private static final String PROFILE_FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Mock
    private AnalyzerSiteBindingConfirmationDAO confirmationDAO;

    @Mock
    private AuditTrailService auditTrailService;

    @Mock
    private SystemUserService systemUserService;

    @Mock
    private AnalyzerMappingCatalogService mappingCatalogService;

    private AnalyzerSiteBindingConfirmationService service;

    @Before
    public void setUp() {
        service = new AnalyzerSiteBindingConfirmationServiceImpl(confirmationDAO, auditTrailService, systemUserService,
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
        when(confirmationDAO.insert(any(AnalyzerSiteBindingConfirmation.class))).thenAnswer(invocation -> {
            AnalyzerSiteBindingConfirmation confirmation = invocation.getArgument(0);
            confirmation.setId("71");
            return confirmation.getId();
        });
        when(auditTrailService.saveNewHistory(any(AnalyzerSiteBindingConfirmation.class), eq("17"),
                eq("analyzer_site_binding_confirmation"))).thenReturn("91");
    }

    @Test
    public void confirmsTheExactImmutableCandidateAndAuditsTheActor() {
        AnalyzerSiteBindingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerSiteBindingConfirmationRequest request = exactRequest();

        AnalyzerSiteBindingConfirmationView confirmed = service.confirm(candidate, RECOGNITION_FINGERPRINT, request,
                "17");

        ArgumentCaptor<AnalyzerSiteBindingConfirmation> saved = ArgumentCaptor
                .forClass(AnalyzerSiteBindingConfirmation.class);
        verify(confirmationDAO).insert(saved.capture());
        assertSame(candidate.revision(), saved.getValue().getSiteBindingRevision());
        assertEquals("site.mock-analyzer", saved.getValue().getProfileId());
        assertEquals(2, saved.getValue().getProfileRevision());
        assertEquals(PROFILE_FINGERPRINT, saved.getValue().getProfileRevisionFingerprint());
        assertEquals(BINDING_FINGERPRINT, saved.getValue().getBindingFingerprint());
        assertEquals(RECOGNITION_FINGERPRINT, saved.getValue().getRecognitionFingerprint());
        assertEquals("91", saved.getValue().getAuditEventId());
        assertEquals("17", saved.getValue().getConfirmedBy());
        assertEquals(AnalyzerSiteBindingConfirmationView.State.CURRENT, confirmed.state());
        assertEquals("17", confirmed.confirmedBy());
        assertEquals("Ada Lovelace", confirmed.confirmedByDisplayName());
        assertEquals(request.confirmedRows(), confirmed.confirmedRows());
        assertEquals(request.excludedRows(), confirmed.excludedRows());
        verify(auditTrailService).saveNewHistory(saved.getValue(), "17", "analyzer_site_binding_confirmation");
    }

    @Test
    public void disabledHistoryLeavesTheSavedConfirmationStaleWithoutASecondWrite() {
        when(auditTrailService.saveNewHistory(any(AnalyzerSiteBindingConfirmation.class), eq("17"),
                eq("analyzer_site_binding_confirmation"))).thenReturn(null);

        AnalyzerSiteBindingConfirmationView confirmed = service.confirm(completeCandidate("61", BINDING_FINGERPRINT),
                RECOGNITION_FINGERPRINT, exactRequest(), "17");

        ArgumentCaptor<AnalyzerSiteBindingConfirmation> saved = ArgumentCaptor
                .forClass(AnalyzerSiteBindingConfirmation.class);
        verify(confirmationDAO).insert(saved.capture());
        assertNull(saved.getValue().getAuditEventId());
        verify(confirmationDAO, never()).update(any(AnalyzerSiteBindingConfirmation.class));
        assertEquals(AnalyzerSiteBindingConfirmationView.State.STALE, confirmed.state());
    }

    @Test
    public void rejectsAStaleBindingOrRecognitionFingerprintBeforeWriting() {
        AnalyzerSiteBindingConfirmationRequest stale = new AnalyzerSiteBindingConfirmationRequest(
                "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd", RECOGNITION_FINGERPRINT,
                exactRequest().confirmedRows(), exactRequest().excludedRows());

        assertThrows(IllegalArgumentException.class, () -> service.confirm(completeCandidate("61", BINDING_FINGERPRINT),
                RECOGNITION_FINGERPRINT, stale, "17"));
        verify(confirmationDAO, never()).insert(any());
    }

    @Test
    public void rejectsUnresolvedOrOmittedSourceRowsBeforeWriting() {
        AnalyzerSiteBindingSnapshot unresolved = completeCandidate("61", BINDING_FINGERPRINT);
        unresolved.tests().get(0).setMappingState(AnalyzerSiteBindingMappingState.UNRESOLVED);
        AnalyzerSiteBindingConfirmationRequest omitted = new AnalyzerSiteBindingConfirmationRequest(BINDING_FINGERPRINT,
                RECOGNITION_FINGERPRINT, List.of(new AnalyzerSiteBindingSourceRow("RAW-A", "Detected")),
                exactRequest().excludedRows());

        assertThrows(IllegalArgumentException.class,
                () -> service.confirm(unresolved, RECOGNITION_FINGERPRINT, exactRequest(), "17"));
        assertThrows(IllegalArgumentException.class, () -> service.confirm(completeCandidate("61", BINDING_FINGERPRINT),
                RECOGNITION_FINGERPRINT, omitted, "17"));
        verify(confirmationDAO, never()).insert(any());
    }

    @Test
    public void reportsAFormerConfirmationAsStaleForANewBindingRevision() throws Exception {
        AnalyzerSiteBindingSnapshot former = completeCandidate("60",
                "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        AnalyzerSiteBindingConfirmation stored = storedConfirmation(former, exactRequest());
        when(confirmationDAO.findLatestByBindingId("51")).thenReturn(Optional.of(stored));

        AnalyzerSiteBindingConfirmationView status = service.getStatus(completeCandidate("61", BINDING_FINGERPRINT),
                RECOGNITION_FINGERPRINT);

        assertEquals(AnalyzerSiteBindingConfirmationView.State.STALE, status.state());
        assertEquals(former.revision().getBindingFingerprint(), status.bindingFingerprint());
        assertEquals(exactRequest().confirmedRows(), status.confirmedRows());
    }

    @Test
    public void reportsAConfirmationAsStaleWhenControlRecognitionChanges() throws Exception {
        AnalyzerSiteBindingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerSiteBindingConfirmation stored = storedConfirmation(candidate, exactRequest());
        when(confirmationDAO.findLatestByBindingId("51")).thenReturn(Optional.of(stored));
        when(confirmationDAO.findByRevisionId("61")).thenReturn(Optional.of(stored));

        AnalyzerSiteBindingConfirmationView status = service.getStatus(candidate,
                "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd");
        AnalyzerSiteBindingVerificationAssessment assessment = service.assessCurrent(candidate,
                "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd");

        assertEquals(AnalyzerSiteBindingConfirmationView.State.STALE, status.state());
        assertEquals(RECOGNITION_FINGERPRINT, status.recognitionFingerprint());
        assertTrue(assessment.mappingsCurrent());
        assertFalse(assessment.recognitionCurrent());
        assertEquals(Optional.empty(), assessment.currentConfirmation());
    }

    @Test
    public void reportsHistoricalConfirmationWithoutDurableEvidenceAsStale() throws Exception {
        AnalyzerSiteBindingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerSiteBindingConfirmation stored = storedConfirmation(candidate, exactRequest());
        stored.setProfileRevisionFingerprint(null);
        stored.setAuditEventId(null);
        when(confirmationDAO.findLatestByBindingId("51")).thenReturn(Optional.of(stored));

        AnalyzerSiteBindingConfirmationView status = service.getStatus(candidate, RECOGNITION_FINGERPRINT);

        assertEquals(AnalyzerSiteBindingConfirmationView.State.STALE, status.state());
    }

    @Test
    public void confirmationMustMatchTheExactPinnedProfileIdentity() throws Exception {
        AnalyzerSiteBindingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerSiteBindingConfirmation stored = storedConfirmation(candidate, exactRequest());
        stored.setProfileId("another.profile");
        stored.setProfileRevision(3);
        when(confirmationDAO.findByRevisionId("61")).thenReturn(Optional.of(stored));

        assertEquals(Optional.empty(), service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).currentConfirmation());
    }

    @Test
    public void confirmationRequiresItsDurableActorAndTime() throws Exception {
        AnalyzerSiteBindingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerSiteBindingConfirmation stored = storedConfirmation(candidate, exactRequest());
        stored.setConfirmedBy(null);
        stored.setConfirmedAt(null);
        when(confirmationDAO.findByRevisionId("61")).thenReturn(Optional.of(stored));

        assertEquals(Optional.empty(), service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).currentConfirmation());
    }

    @Test
    public void reportsAConfirmationAsStaleWhenItsCatalogBindingIsNoLongerCurrent() throws Exception {
        AnalyzerSiteBindingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerSiteBindingConfirmation stored = storedConfirmation(candidate, exactRequest());
        when(confirmationDAO.findLatestByBindingId("51")).thenReturn(Optional.of(stored));
        when(confirmationDAO.findByRevisionId("61")).thenReturn(Optional.of(stored));
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(List.of());

        AnalyzerSiteBindingConfirmationView status = service.getStatus(candidate, RECOGNITION_FINGERPRINT);
        AnalyzerSiteBindingVerificationAssessment assessment = service.assessCurrent(candidate,
                RECOGNITION_FINGERPRINT);

        assertEquals(AnalyzerSiteBindingConfirmationView.State.STALE, status.state());
        assertFalse(assessment.mappingsCurrent());
        assertTrue(assessment.recognitionCurrent());
        assertEquals(Optional.empty(), assessment.currentConfirmation());
    }

    @Test
    public void reportsAConfirmationAsStaleWhenItsResultOptionMovesToAnotherTest() throws Exception {
        AnalyzerSiteBindingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        when(confirmationDAO.findLatestByBindingId("51"))
                .thenReturn(Optional.of(storedConfirmation(candidate, exactRequest())));
        when(mappingCatalogService.getActiveResultOptions("1")).thenReturn(List.of());

        AnalyzerSiteBindingConfirmationView status = service.getStatus(candidate, RECOGNITION_FINGERPRINT);

        assertEquals(AnalyzerSiteBindingConfirmationView.State.STALE, status.state());
    }

    @Test
    public void exactSavedRowsAreRequiredForAConfirmationToRemainCurrent() throws Exception {
        AnalyzerSiteBindingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerSiteBindingConfirmation stored = storedConfirmation(candidate, exactRequest());
        stored.setConfirmedRowsJson(
                new ObjectMapper().writeValueAsString(List.of(new AnalyzerSiteBindingSourceRow("RAW-A", null))));
        when(confirmationDAO.findByRevisionId("61")).thenReturn(Optional.of(stored));
        when(confirmationDAO.findLatestByBindingId("51")).thenReturn(Optional.of(stored));

        assertEquals(Optional.empty(), service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).currentConfirmation());
        assertEquals(AnalyzerSiteBindingConfirmationView.State.STALE,
                service.getStatus(candidate, RECOGNITION_FINGERPRINT).state());
    }

    @Test
    public void malformedSavedRowsAreStaleInsteadOfBreakingVerificationStatus() throws Exception {
        AnalyzerSiteBindingSnapshot candidate = completeCandidate("61", BINDING_FINGERPRINT);
        AnalyzerSiteBindingConfirmation stored = storedConfirmation(candidate, exactRequest());
        stored.setConfirmedRowsJson("not-json");
        when(confirmationDAO.findByRevisionId("61")).thenReturn(Optional.of(stored));
        when(confirmationDAO.findLatestByBindingId("51")).thenReturn(Optional.of(stored));

        assertEquals(Optional.empty(), service.assessCurrent(candidate, RECOGNITION_FINGERPRINT).currentConfirmation());
        AnalyzerSiteBindingConfirmationView status = service.getStatus(candidate, RECOGNITION_FINGERPRINT);
        assertEquals(AnalyzerSiteBindingConfirmationView.State.STALE, status.state());
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

    private static AnalyzerSiteBindingConfirmationRequest exactRequest() {
        return new AnalyzerSiteBindingConfirmationRequest(BINDING_FINGERPRINT, RECOGNITION_FINGERPRINT,
                List.of(new AnalyzerSiteBindingSourceRow("RAW-A", null),
                        new AnalyzerSiteBindingSourceRow("RAW-A", "Detected")),
                List.of(new AnalyzerSiteBindingSourceRow("RAW-B", null),
                        new AnalyzerSiteBindingSourceRow("RAW-B", "Invalid")));
    }

    private static AnalyzerSiteBindingSnapshot completeCandidate(String revisionId, String fingerprint) {
        AnalyzerProfileBinding profile = new AnalyzerProfileBinding();
        profile.setId("41");
        profile.setProfileId("site.mock-analyzer");
        profile.setProfileRevision(2);
        profile.setProfileFingerprint(PROFILE_FINGERPRINT);

        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId("51");
        binding.setProfileBinding(profile);

        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId(revisionId);
        revision.setSiteBinding(binding);
        revision.setRevisionNumber("60".equals(revisionId) ? 3 : 4);
        revision.setBindingFingerprint(fingerprint);

        AnalyzerSiteBindingTest bound = test(revision, "RAW-A", AnalyzerSiteBindingMappingState.BOUND);
        AnalyzerSiteBindingTest excluded = test(revision, "RAW-B", AnalyzerSiteBindingMappingState.EXCLUDED);
        AnalyzerSiteBindingResult boundResult = result(revision, "RAW-A", "Detected",
                AnalyzerSiteBindingMappingState.BOUND);
        AnalyzerSiteBindingResult excludedResult = result(revision, "RAW-B", "Invalid",
                AnalyzerSiteBindingMappingState.EXCLUDED);
        return new AnalyzerSiteBindingSnapshot(binding, revision, List.of(bound, excluded),
                List.of(boundResult, excludedResult));
    }

    private static AnalyzerSiteBindingTest test(AnalyzerSiteBindingRevision revision, String sourceRowKey,
            AnalyzerSiteBindingMappingState state) {
        AnalyzerSiteBindingTest test = new AnalyzerSiteBindingTest();
        test.setId(new AnalyzerSiteBindingTestPK(revision.getId(), sourceRowKey));
        test.setSiteBindingRevision(revision);
        test.setMappingState(state);
        test.setTestId(state == AnalyzerSiteBindingMappingState.BOUND ? "1" : null);
        return test;
    }

    private static AnalyzerSiteBindingResult result(AnalyzerSiteBindingRevision revision, String sourceRowKey,
            String rawValue, AnalyzerSiteBindingMappingState state) {
        AnalyzerSiteBindingResult result = new AnalyzerSiteBindingResult();
        result.setId(new AnalyzerSiteBindingResultPK(revision.getId(), sourceRowKey, rawValue));
        result.setSiteBindingRevision(revision);
        result.setMappingState(state);
        result.setTestResultId(state == AnalyzerSiteBindingMappingState.BOUND ? "11" : null);
        return result;
    }

    private static AnalyzerSiteBindingConfirmation storedConfirmation(AnalyzerSiteBindingSnapshot snapshot,
            AnalyzerSiteBindingConfirmationRequest request) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AnalyzerSiteBindingConfirmation confirmation = new AnalyzerSiteBindingConfirmation();
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
