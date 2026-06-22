package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.openelisglobal.referral.valueholder.SubcontractStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration coverage for the S-14 / OGC-624 FR-02 strict-linear state
 * machine: happy-path transitions, illegal-transition rejection, missing
 * required-field rejection, terminal CLOSED, and the no-subcontract no-op
 * (covers the FHIR auto-trigger edge case where results return on a historical
 * referral that predates S-14).
 *
 * <p>
 * Fixture loads referral id=1 with a subcontract at DRAFT and referral id=2
 * with no subcontract row at all.
 */
public class ReferralSubcontractTransitionsTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReferralService referralService;

    @Autowired
    private ReferralStatusHistoryDAO statusHistoryDAO;

    // Swap the live FhirReferralService for a mock so dispatch tests can assert
    // whether/how often referAnalysisesToOrganization fires without touching the
    // FHIR store. Restored in @After. Matches the ReflectionTestUtils pattern
    // used in ReferralSubcontractDispatchIntegrationTest.
    private FhirReferralService fhirReferralServiceMock;
    private Object originalFhirReferralServiceOnReferralService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
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
        referralService.dispatchSubcontract("1", handoff, "42", "courier ABC pickup");
        Timestamp afterDispatch = new Timestamp(System.currentTimeMillis());

        Referral fresh = referralService.getReferralById("1");
        assertEquals(SubcontractStatus.DISPATCHED, fresh.getSubcontract().getSubcontractStatus());
        assertEquals(handoff, fresh.getSubcontract().getHandoffDatetime());

        List<ReferralStatusHistory> history = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1");
        assertEquals(2, history.size()); // fixture's null->DRAFT + this DRAFT->DISPATCHED
        ReferralStatusHistory latest = history.get(1);
        assertEquals(SubcontractStatus.DRAFT, latest.getFromStatus());
        assertEquals(SubcontractStatus.DISPATCHED, latest.getToStatus());
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
                () -> referralService.dispatchSubcontract("1", null, "42", "missing handoff"));

        Referral fresh = referralService.getReferralById("1");
        assertEquals(SubcontractStatus.DRAFT, fresh.getSubcontract().getSubcontractStatus());
        assertEquals(before, statusHistoryDAO.findByReferralIdOrderedByChangedAt("1").size());
    }

    @Test
    @Transactional
    public void illegalTransition_fromDraftDirectlyToReceived_throws() {
        assertThrows(IllegalStateException.class,
                () -> referralService.markSubcontractReceived("1", "42", "operator skipped dispatch"));

        Referral fresh = referralService.getReferralById("1");
        assertEquals(SubcontractStatus.DRAFT, fresh.getSubcontract().getSubcontractStatus());
    }

    // No @Transactional: markSubcontractReceived/ResultsReturned use
    // Propagation.REQUIRES_NEW (to isolate the strict-linear guard's
    // IllegalStateException from auto-trigger callers' transactions). A test-tx
    // would suspend on every inner call and the inner would read pre-outer DB
    // state. The @Before fixture reload handles cleanup between tests.
    @Test
    public void fullLifecycle_writesOneHistoryRowPerTransition() {
        Timestamp handoff = Timestamp.valueOf("2026-05-15 10:30:00");
        referralService.dispatchSubcontract("1", handoff, "42", "dispatched");
        referralService.markSubcontractReceived("1", "42", "lab confirmed");
        referralService.markSubcontractResultsReturned("1", "1", "FHIR result import");
        referralService.closeSubcontract("1", "42", "validation complete");

        Referral fresh = referralService.getReferralById("1");
        assertEquals(SubcontractStatus.CLOSED, fresh.getSubcontract().getSubcontractStatus());

        List<ReferralStatusHistory> history = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1");
        // fixture initial null->DRAFT plus 4 lifecycle transitions
        assertEquals(5, history.size());
        assertNull(history.get(0).getFromStatus());
        assertEquals(SubcontractStatus.DRAFT, history.get(0).getToStatus());
        assertEquals(SubcontractStatus.DRAFT, history.get(1).getFromStatus());
        assertEquals(SubcontractStatus.DISPATCHED, history.get(1).getToStatus());
        assertEquals(SubcontractStatus.DISPATCHED, history.get(2).getFromStatus());
        assertEquals(SubcontractStatus.RECEIVED, history.get(2).getToStatus());
        assertEquals(SubcontractStatus.RECEIVED, history.get(3).getFromStatus());
        assertEquals(SubcontractStatus.RESULTS_RETURNED, history.get(3).getToStatus());
        assertEquals("1", history.get(3).getChangedByUserId()); // system actor for auto-trigger
        assertEquals("FHIR result import", history.get(3).getNotes());
        assertEquals(SubcontractStatus.RESULTS_RETURNED, history.get(4).getFromStatus());
        assertEquals(SubcontractStatus.CLOSED, history.get(4).getToStatus());
    }

    @Test
    public void closeIsTerminal_furtherTransitionsRejected() {
        referralService.dispatchSubcontract("1", Timestamp.valueOf("2026-05-15 10:30:00"), "42", null);
        referralService.markSubcontractReceived("1", "42", null);
        referralService.markSubcontractResultsReturned("1", "42", null);
        referralService.closeSubcontract("1", "42", null);

        assertThrows(IllegalStateException.class,
                () -> referralService.markSubcontractReceived("1", "42", "after close"));
        assertThrows(IllegalStateException.class, () -> referralService.closeSubcontract("1", "42", "double close"));
    }

    @Test
    @Transactional
    public void noSubcontract_transitionIsNoop_noHistoryWritten() {
        // Referral id=2 has no subcontract row (pre-S-14 historical case).
        long before = statusHistoryDAO.findByReferralIdOrderedByChangedAt("2").size();

        // Must not throw — the FHIR auto-trigger relies on this no-op behavior so
        // result imports against legacy referrals don't break.
        referralService.markSubcontractResultsReturned("2", "1", "FHIR result import");

        assertEquals(before, statusHistoryDAO.findByReferralIdOrderedByChangedAt("2").size());
        Referral fresh = referralService.getReferralById("2");
        assertNull(fresh.getSubcontract());
    }

    @Test
    public void unknownReferral_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> referralService.markSubcontractReceived("9999", "42", "no such referral"));
    }

    // -- S-14 / OGC-624: DRAFT -> DISPATCHED is when the receiving lab needs FHIR
    // --

    @Test
    @Transactional
    public void dispatch_invokesFhirReferAnalysisesToOrganization() throws Exception {
        Timestamp handoff = Timestamp.valueOf("2026-05-15 10:30:00");
        referralService.dispatchSubcontract("1", handoff, "42", "courier ABC pickup");

        // FHIR push fires exactly once on DRAFT -> DISPATCHED, with the referral
        // whose subcontract just transitioned.
        ArgumentCaptor<Referral> referralCaptor = ArgumentCaptor.forClass(Referral.class);
        verify(fhirReferralServiceMock, times(1)).referAnalysisesToOrganization(referralCaptor.capture());
        Referral pushed = referralCaptor.getValue();
        assertEquals("1", pushed.getId());
        assertEquals(SubcontractStatus.DISPATCHED, pushed.getSubcontract().getSubcontractStatus());
        assertEquals(handoff, pushed.getSubcontract().getHandoffDatetime());
    }

    @Test
    @Transactional
    public void dispatch_logsAndContinues_whenFhirPushFails() throws Exception {
        doThrow(new FhirLocalPersistingException("FHIR store unreachable")).when(fhirReferralServiceMock)
                .referAnalysisesToOrganization(any(Referral.class));
        Timestamp handoff = Timestamp.valueOf("2026-05-15 10:30:00");

        // FHIR outage must NOT abort the DB transition — operator gets a stable
        // DISPATCHED state to retry FHIR pushing against, history row is written,
        // no exception escapes to the caller.
        referralService.dispatchSubcontract("1", handoff, "42", "courier ABC pickup");

        Referral fresh = referralService.getReferralById("1");
        assertEquals(SubcontractStatus.DISPATCHED, fresh.getSubcontract().getSubcontractStatus());
        assertEquals(handoff, fresh.getSubcontract().getHandoffDatetime());
        List<ReferralStatusHistory> history = statusHistoryDAO.findByReferralIdOrderedByChangedAt("1");
        assertEquals(2, history.size()); // fixture's null->DRAFT + DRAFT->DISPATCHED
        ReferralStatusHistory latest = history.get(1);
        assertEquals(SubcontractStatus.DRAFT, latest.getFromStatus());
        assertEquals(SubcontractStatus.DISPATCHED, latest.getToStatus());
    }

    @Test
    public void nonDispatchTransitions_doNotInvokeFhirPush() throws Exception {
        // Only the DRAFT -> DISPATCHED leg pushes to FHIR. RECEIVED, RESULTS_RETURNED,
        // and CLOSED must not push. Reset the mock between calls to count cleanly.
        referralService.dispatchSubcontract("1", Timestamp.valueOf("2026-05-15 10:30:00"), "42", null);
        verify(fhirReferralServiceMock, times(1)).referAnalysisesToOrganization(any(Referral.class));
        Mockito.reset(fhirReferralServiceMock);

        referralService.markSubcontractReceived("1", "42", "received at lab");
        referralService.markSubcontractResultsReturned("1", "1", "results back");
        referralService.closeSubcontract("1", "42", "validation complete");

        verify(fhirReferralServiceMock, never()).referAnalysisesToOrganization(any(Referral.class));
    }

    @Test
    @Transactional
    public void getSubcontractStatusHistory_returnsRowsForReferral() {
        List<ReferralStatusHistory> history = referralService.getSubcontractStatusHistory("1");
        assertEquals(1, history.size());
        assertEquals("100", history.get(0).getId());
        assertEquals(SubcontractStatus.DRAFT, history.get(0).getToStatus());
        assertTrue(referralService.getSubcontractStatusHistory("9999").isEmpty());
    }
}
