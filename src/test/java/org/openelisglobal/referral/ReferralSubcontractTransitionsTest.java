package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.referral.dao.ReferralStatusHistoryDAO;
import org.openelisglobal.referral.fhir.service.FhirReferralService;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration coverage for the FHIR Task-aligned ReferralStatus state machine:
 * happy-path transitions, illegal-transition rejection, missing required-field
 * rejection, terminal COMPLETED, and the no-subcontract no-op (covers the FHIR
 * auto-trigger edge case where results return on a historical referral).
 *
 * <p>
 * Fixture loads referral id=1 with a subcontract at DRAFT and referral id=2
 * with no subcontract row.
 */
public class ReferralSubcontractTransitionsTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReferralService referralService;

    @Autowired
    private ReferralStatusHistoryDAO statusHistoryDAO;

    // Swap the live FhirReferralService for a mock so dispatch tests can assert
    // whether/how often referAnalysisesToOrganization fires without touching the
    // FHIR store. Restored in @After.
    private FhirReferralService fhirReferralServiceMock;
    private Object originalFhirReferralServiceOnReferralService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
        // referral.xml seeds referral_status_history id=100; advance the sequence
        // past it so lifecycle inserts don't collide on the PK when this class runs
        // before other tests have already bumped the sequence.
        resyncSequence("clinlims.referral_status_history_seq", "clinlims.referral_status_history");
        fhirReferralServiceMock = Mockito.mock(FhirReferralService.class);
        originalFhirReferralServiceOnReferralService = ReflectionTestUtils.getField(referralService,
                "fhirReferralService");
        ReflectionTestUtils.setField(referralService, "fhirReferralService", fhirReferralServiceMock);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(referralService, "fhirReferralService",
                originalFhirReferralServiceOnReferralService);
    }

    @Test
    @Transactional
    public void dispatch_advancesStatusAndWritesHistory() {
        Timestamp handoff = Timestamp.valueOf("2026-05-15 10:30:00");
        Timestamp beforeDispatch = new Timestamp(System.currentTimeMillis());
        referralService.dispatchReferral("1", handoff, "42", "courier ABC pickup");
        Timestamp afterDispatch = new Timestamp(System.currentTimeMillis());

        Referral fresh = referralService.getReferralById("1");
        assertEquals(ReferralStatus.REQUESTED, fresh.getStatus());
        assertEquals(handoff, fresh.getSubcontract().getHandoffDatetime());

        List<ReferralStatusHistory> history = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1");
        assertEquals(2, history.size()); // fixture's null->DRAFT + this DRAFT->REQUESTED
        ReferralStatusHistory latest = history.get(1);
        assertEquals(ReferralStatus.DRAFT, latest.getFromStatus());
        assertEquals(ReferralStatus.REQUESTED, latest.getToStatus());
        assertEquals("42", latest.getChangedByUserId());
        assertEquals("courier ABC pickup", latest.getNotes());
        Timestamp changedAt = latest.getChangedAt();
        assertNotNull(changedAt);
        assertFalse("changedAt should be >= beforeDispatch", changedAt.before(beforeDispatch));
        assertFalse("changedAt should be <= afterDispatch", changedAt.after(afterDispatch));
    }

    @Test
    @Transactional
    public void dispatch_withoutHandoffDatetime_throwsAndPersistsNothing() {
        long before = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1").size();

        assertThrows(IllegalArgumentException.class,
                () -> referralService.dispatchReferral("1", null, "42", "missing handoff"));

        Referral fresh = referralService.getReferralById("1");
        assertEquals(ReferralStatus.DRAFT, fresh.getStatus());
        assertEquals(before, statusHistoryDAO.findByReferralIdOrderedByChangedAt("1").size());
    }

    @Test
    @Transactional
    public void illegalTransition_fromDraftDirectlyToReceived_throws() {
        assertThrows(IllegalStateException.class,
                () -> referralService.markReferralReceived("1", "42", "operator skipped dispatch"));

        Referral fresh = referralService.getReferralById("1");
        assertEquals(ReferralStatus.DRAFT, fresh.getStatus());
    }

    // No @Transactional: markReferral* use Propagation.REQUIRES_NEW (to isolate
    // the transition guard's IllegalStateException from auto-trigger callers'
    // transactions). A test-tx would suspend on every inner call and the inner
    // would read pre-outer DB state. The @Before fixture reload handles cleanup
    // between tests.
    @Test
    public void fullLifecycle_writesOneHistoryRowPerTransition() {
        Timestamp handoff = Timestamp.valueOf("2026-05-15 10:30:00");
        referralService.dispatchReferral("1", handoff, "42", "dispatched");
        referralService.markReferralReceived("1", "42", "lab confirmed");
        referralService.markReferralCompleted("1", "1", "FHIR result import");

        Referral fresh = referralService.getReferralById("1");
        assertEquals(ReferralStatus.COMPLETED, fresh.getStatus());

        List<ReferralStatusHistory> history = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1");
        // fixture initial null->DRAFT plus 3 lifecycle transitions
        assertEquals(4, history.size());
        assertNull(history.get(0).getFromStatus());
        assertEquals(ReferralStatus.DRAFT, history.get(0).getToStatus());
        assertEquals(ReferralStatus.DRAFT, history.get(1).getFromStatus());
        assertEquals(ReferralStatus.REQUESTED, history.get(1).getToStatus());
        assertEquals(ReferralStatus.REQUESTED, history.get(2).getFromStatus());
        assertEquals(ReferralStatus.RECEIVED, history.get(2).getToStatus());
        assertEquals(ReferralStatus.RECEIVED, history.get(3).getFromStatus());
        assertEquals(ReferralStatus.COMPLETED, history.get(3).getToStatus());
        assertEquals("1", history.get(3).getChangedByUserId()); // system actor for auto-trigger
        assertEquals("FHIR result import", history.get(3).getNotes());
    }

    @Test
    public void completedIsTerminal_furtherTransitionsRejected() {
        referralService.dispatchReferral("1", Timestamp.valueOf("2026-05-15 10:30:00"), "42", null);
        referralService.markReferralReceived("1", "42", null);
        referralService.markReferralCompleted("1", "42", null);

        assertThrows(IllegalStateException.class,
                () -> referralService.markReferralReceived("1", "42", "after complete"));
        assertThrows(IllegalStateException.class,
                () -> referralService.markReferralCompleted("1", "42", "double complete"));
    }

    @Test
    @Transactional
    public void noSubcontract_transitionIsNoop_noHistoryWritten() {
        // Referral id=2 has no subcontract row (pre-S-14 historical case).
        long before = statusHistoryDAO.findByReferralIdOrderedByChangedAt("2").size();

        // Must not throw — the FHIR auto-trigger relies on this no-op behavior so
        // result imports against legacy referrals don't break.
        referralService.markReferralCompleted("2", "1", "FHIR result import");

        assertEquals(before, statusHistoryDAO.findByReferralIdOrderedByChangedAt("2").size());
        Referral fresh = referralService.getReferralById("2");
        assertNull(fresh.getSubcontract());
    }

    @Test
    public void unknownReferral_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> referralService.markReferralReceived("9999", "42", "no such referral"));
    }

    // -- DRAFT -> REQUESTED is when the receiving lab needs FHIR --

    @Test
    @Transactional
    public void dispatch_invokesFhirReferAnalysisesToOrganization() throws Exception {
        Timestamp handoff = Timestamp.valueOf("2026-05-15 10:30:00");
        referralService.dispatchReferral("1", handoff, "42", "courier ABC pickup");

        // FHIR push fires exactly once on DRAFT -> REQUESTED, with the referral
        // whose status just transitioned.
        ArgumentCaptor<Referral> referralCaptor = ArgumentCaptor.forClass(Referral.class);
        verify(fhirReferralServiceMock, times(1)).referAnalysisesToOrganization(referralCaptor.capture());
        Referral pushed = referralCaptor.getValue();
        assertEquals("1", pushed.getId());
        assertEquals(ReferralStatus.REQUESTED, pushed.getStatus());
        assertEquals(handoff, pushed.getSubcontract().getHandoffDatetime());
    }

    @Test
    @Transactional
    public void dispatch_logsAndContinues_whenFhirPushFails() throws Exception {
        doThrow(new FhirLocalPersistingException("FHIR store unreachable")).when(fhirReferralServiceMock)
                .referAnalysisesToOrganization(any(Referral.class));
        Timestamp handoff = Timestamp.valueOf("2026-05-15 10:30:00");

        // FHIR outage must NOT abort the DB transition — operator gets a stable
        // REQUESTED state to retry FHIR pushing against, history row is written,
        // no exception escapes to the caller.
        referralService.dispatchReferral("1", handoff, "42", "courier ABC pickup");

        Referral fresh = referralService.getReferralById("1");
        assertEquals(ReferralStatus.REQUESTED, fresh.getStatus());
        assertEquals(handoff, fresh.getSubcontract().getHandoffDatetime());
        List<ReferralStatusHistory> history = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1");
        assertEquals(2, history.size()); // fixture's null->DRAFT + DRAFT->REQUESTED
        ReferralStatusHistory latest = history.get(1);
        assertEquals(ReferralStatus.DRAFT, latest.getFromStatus());
        assertEquals(ReferralStatus.REQUESTED, latest.getToStatus());
    }

    @Test
    public void nonDispatchTransitions_doNotInvokeFhirPush() throws Exception {
        // Only the DRAFT -> REQUESTED leg pushes to FHIR. RECEIVED and COMPLETED
        // must not push. Reset the mock between calls to count cleanly.
        referralService.dispatchReferral("1", Timestamp.valueOf("2026-05-15 10:30:00"), "42", null);
        verify(fhirReferralServiceMock, times(1)).referAnalysisesToOrganization(any(Referral.class));
        Mockito.reset(fhirReferralServiceMock);

        referralService.markReferralReceived("1", "42", "received at lab");
        referralService.markReferralCompleted("1", "1", "results back");

        verify(fhirReferralServiceMock, never()).referAnalysisesToOrganization(any(Referral.class));
    }

    // -- OGC-799 Manual Entry path: completes the referral, sets manually_entered,
    // and publishes the result resources back to the FHIR store --

    @Test
    public void manualEntryCompletion_jumpsFromRequestedDirectlyToCompleted_whenPeerNeverAcknowledged()
            throws Exception {
        // FR-OUTSTANDING-005: phone/fax results from a non-OpenELIS reference lab.
        // Peer never sent RECEIVED/IN_PROGRESS acks, so the local referral is stuck
        // at REQUESTED. Manual Entry must still complete it.
        Timestamp handoff = Timestamp.valueOf("2026-05-15 10:30:00");
        referralService.dispatchReferral("1", handoff, "42", null);
        Mockito.reset(fhirReferralServiceMock);

        referralService.markReferralCompletedFromManualEntry("1", "55");

        Referral fresh = referralService.getReferralById("1");
        assertEquals("REQUESTED → COMPLETED direct jump must succeed for manual entry", ReferralStatus.COMPLETED,
                fresh.getStatus());
        assertTrue("manually_entered must flip true on direct-jump completion",
                Boolean.TRUE.equals(fresh.getManuallyEntered()));
        List<ReferralStatusHistory> history = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1");
        // fixture null->DRAFT + DRAFT->REQUESTED + REQUESTED->COMPLETED (no synthetic
        // RECEIVED)
        assertEquals("history must not invent a synthetic RECEIVED row", 3, history.size());
        ReferralStatusHistory completion = history.get(2);
        assertEquals("from_status must reflect the actual prior state", ReferralStatus.REQUESTED,
                completion.getFromStatus());
        assertEquals(ReferralStatus.COMPLETED, completion.getToStatus());
        assertEquals("Manually entered at Result Entry", completion.getNotes());
        verify(fhirReferralServiceMock, times(1)).publishManualEntryCompletion(any(Referral.class), anyString());
    }

    @Test
    public void manualEntryCompletion_advancesReferralAndPublishesToFhir() throws Exception {
        referralService.dispatchReferral("1", Timestamp.valueOf("2026-05-15 10:30:00"), "42", null);
        referralService.markReferralReceived("1", "42", "received at peer");
        Mockito.reset(fhirReferralServiceMock);

        referralService.markReferralCompletedFromManualEntry("1", "55");

        Referral fresh = referralService.getReferralById("1");
        assertEquals(ReferralStatus.COMPLETED, fresh.getStatus());
        assertTrue("manually_entered must flip true after manual-entry completion",
                Boolean.TRUE.equals(fresh.getManuallyEntered()));
        List<ReferralStatusHistory> history = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1");
        // fixture null->DRAFT + DRAFT->REQUESTED + REQUESTED->RECEIVED +
        // RECEIVED->COMPLETED
        assertEquals(4, history.size());
        ReferralStatusHistory completion = history.get(3);
        assertEquals(ReferralStatus.RECEIVED, completion.getFromStatus());
        assertEquals(ReferralStatus.COMPLETED, completion.getToStatus());
        assertEquals("55", completion.getChangedByUserId());
        assertEquals("Manually entered at Result Entry", completion.getNotes());

        ArgumentCaptor<Referral> referralCaptor = ArgumentCaptor.forClass(Referral.class);
        ArgumentCaptor<String> actorCaptor = ArgumentCaptor.forClass(String.class);
        verify(fhirReferralServiceMock, times(1)).publishManualEntryCompletion(referralCaptor.capture(),
                actorCaptor.capture());
        assertEquals("FHIR publish must receive the just-completed referral", "1", referralCaptor.getValue().getId());
        assertEquals(ReferralStatus.COMPLETED, referralCaptor.getValue().getStatus());
        assertTrue(Boolean.TRUE.equals(referralCaptor.getValue().getManuallyEntered()));
        assertEquals("FHIR publish must carry the actor userId from the hook", "55", actorCaptor.getValue());
    }

    @Test
    public void manualEntryCompletion_logsAndContinues_whenFhirPushFails() throws Exception {
        referralService.dispatchReferral("1", Timestamp.valueOf("2026-05-15 10:30:00"), "42", null);
        referralService.markReferralReceived("1", "42", "received at peer");
        doThrow(new FhirLocalPersistingException("FHIR store unreachable")).when(fhirReferralServiceMock)
                .publishManualEntryCompletion(any(Referral.class), anyString());

        // No exception should escape — the local DB transition must be durable even
        // when the peer-sync push fails.
        referralService.markReferralCompletedFromManualEntry("1", "55");

        Referral fresh = referralService.getReferralById("1");
        assertEquals("local status flips to COMPLETED even when FHIR push fails", ReferralStatus.COMPLETED,
                fresh.getStatus());
        assertTrue("manually_entered must still be true after FHIR-failure path",
                Boolean.TRUE.equals(fresh.getManuallyEntered()));
        // History row was written by the transition guard before the FHIR push attempt.
        List<ReferralStatusHistory> history = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1");
        assertEquals(ReferralStatus.COMPLETED, history.get(history.size() - 1).getToStatus());
    }

    @Test
    public void manualEntryCompletion_isNoOp_whenAlreadyCompleted() throws Exception {
        referralService.dispatchReferral("1", Timestamp.valueOf("2026-05-15 10:30:00"), "42", null);
        referralService.markReferralReceived("1", "42", null);
        referralService.markReferralCompleted("1", "1", "FHIR result import");
        // Counter from the earlier two dispatch + complete legs; we care that the
        // manual-entry call doesn't add another invocation.
        Mockito.reset(fhirReferralServiceMock);
        long historyBefore = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1").size();

        // Should silently no-op — never re-invokes publishManualEntryCompletion and
        // never writes another history row.
        referralService.markReferralCompletedFromManualEntry("1", "55");

        Referral fresh = referralService.getReferralById("1");
        assertEquals(ReferralStatus.COMPLETED, fresh.getStatus());
        // manually_entered stays whatever it was (false here, since prior completion
        // came through the FHIR-result-import path, not Manual Entry).
        assertFalse("manually_entered must NOT be retroactively set on already-COMPLETED row",
                Boolean.TRUE.equals(fresh.getManuallyEntered()));
        assertEquals(historyBefore, statusHistoryDAO.findByReferralIdOrderedByChangedAt("1").size());
        verify(fhirReferralServiceMock, never()).publishManualEntryCompletion(any(Referral.class), anyString());
    }

    @Test
    @Transactional
    public void manualEntryCompletion_noop_whenNoSubcontract() throws Exception {
        // Referral id=2 has no subcontract row (pre-S-14 legacy). transition() bails
        // early without changing status; the manuallyEntered flag must NOT be set
        // (and the FHIR publish must NOT fire) because the status never advanced.
        Referral before = referralService.getReferralById("2");
        ReferralStatus statusBefore = before.getStatus();
        assertFalse(Boolean.TRUE.equals(before.getManuallyEntered()));

        referralService.markReferralCompletedFromManualEntry("2", "55");

        Referral fresh = referralService.getReferralById("2");
        assertEquals("status must remain unchanged when subcontract is absent", statusBefore, fresh.getStatus());
        assertFalse("manually_entered must NOT flip when transition is no-op",
                Boolean.TRUE.equals(fresh.getManuallyEntered()));
        verify(fhirReferralServiceMock, never()).publishManualEntryCompletion(any(Referral.class), anyString());
    }

    @Test
    @Transactional
    public void getSubcontractStatusHistory_returnsRowsForReferral() {
        List<ReferralStatusHistory> history = referralService.getSubcontractStatusHistory("1");
        assertEquals(1, history.size());
        assertEquals("100", history.get(0).getId());
        assertEquals(ReferralStatus.DRAFT, history.get(0).getToStatus());
        assertTrue(referralService.getSubcontractStatusHistory("9999").isEmpty());
    }
}
