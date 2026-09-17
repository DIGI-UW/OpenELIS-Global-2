package org.openelisglobal.referral;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.referral.dao.ReferralStatusHistoryDAO;
import org.openelisglobal.referral.dto.ReferenceLabMetricsDTO;
import org.openelisglobal.referral.dto.ReferenceLabReferralDTO;
import org.openelisglobal.referral.fhir.service.FhirReferralService;
import org.openelisglobal.referral.service.ReferenceLabResultsService;
import org.openelisglobal.referral.service.ReferenceLabResultsService.DashboardView;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * End-to-end coverage for the OGC-803 Accept (reconcile-ack) and OGC-804 Reject
 * reception actions, plus regression assertions pinning the unchanged bucketing
 * of Outstanding / manual-entry / Mark-Lost rows.
 *
 * <p>
 * Referral 1 in the fixture is DRAFT with a subcontract row; the tests drive it
 * to COMPLETED (the Returned state) before exercising Accept/Reject.
 */
public class ReferenceLabResultsAcceptRejectTest extends BaseWebContextSensitiveTest {

    private static final String REFERRAL_ID = "1";
    private static final String LINKED_ANALYSIS_ID = "1";
    private static final String ACTOR_USER_ID = "1";

    @Autowired
    private ReferenceLabResultsService referenceLabResultsService;

    @Autowired
    private ReferralService referralService;

    @Autowired
    private ReferralStatusHistoryDAO statusHistoryDAO;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    // AppTestConfig publishes a Mockito-mocked FhirReferralService under the
    // unqualified bean name; this qualifier targets the real @Service impl (with
    // its live @Transactional proxy) so the rollback-only regression test can
    // exercise the REAL transactional publish boundary — not a mock that has no
    // proxy and can never mark the caller's transaction rollback-only.
    @Autowired
    @Qualifier("fhirReferralServiceImpl")
    private FhirReferralService realFhirReferralService;

    private FhirReferralService fhirReferralServiceMock;
    private Object originalFhirReferralService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
        // referral.xml seeds referral_status_history id=100; advance the sequence
        // past it so lifecycle inserts don't collide on the PK when this class runs
        // before other tests have already bumped the sequence.
        resyncSequence("clinlims.referral_status_history_seq", "clinlims.referral_status_history");
        // Mock FHIR so the best-effort publish calls don't hit a real store.
        fhirReferralServiceMock = Mockito.mock(FhirReferralService.class);
        originalFhirReferralService = ReflectionTestUtils.getField(referralService, "fhirReferralService");
        ReflectionTestUtils.setField(referralService, "fhirReferralService", fhirReferralServiceMock);
        // Drive referral 1 (DRAFT + subcontract) to the Returned state: COMPLETED.
        referralService.dispatchReferral(REFERRAL_ID, java.sql.Timestamp.valueOf("2026-05-15 10:30:00"), ACTOR_USER_ID,
                null);
        referralService.markReferralCompleted(REFERRAL_ID, ACTOR_USER_ID, "Result returned");
    }

    @After
    public void restoreFhirReferralService() {
        ReflectionTestUtils.setField(referralService, "fhirReferralService", originalFhirReferralService);
    }

    // ---- OGC-803 Accept ------------------------------------------------------

    @Test
    public void accept_setsReconciledFieldsAndKeepsStatusCompleted() {
        Assert.assertEquals("precondition: referral must be COMPLETED before accept", ReferralStatus.COMPLETED,
                referralService.getReferralById(REFERRAL_ID).getStatus());

        referralService.markReferralReconciled(REFERRAL_ID, ACTOR_USER_ID);

        Referral fresh = referralService.getReferralById(REFERRAL_ID);
        Assert.assertEquals("status must stay COMPLETED (Accept is an ack, not a transition)", ReferralStatus.COMPLETED,
                fresh.getStatus());
        Assert.assertTrue("reconciled flag must be set", Boolean.TRUE.equals(fresh.getReconciled()));
        Assert.assertEquals("reconciled_by must be the actor", ACTOR_USER_ID, fresh.getReconciledBy());
        Assert.assertNotNull("reconciled_at must be stamped", fresh.getReconciledAt());
    }

    @Test
    public void accept_appendsSameStatusHistoryNoteRow() {
        int before = statusHistoryDAO.findByReferralIdOrderedByChangedAt(REFERRAL_ID).size();

        referralService.markReferralReconciled(REFERRAL_ID, ACTOR_USER_ID);

        List<ReferralStatusHistory> history = statusHistoryDAO.findByReferralIdOrderedByChangedAt(REFERRAL_ID);
        Assert.assertEquals("exactly one history row must be appended", before + 1, history.size());
        ReferralStatusHistory latest = history.get(history.size() - 1);
        Assert.assertEquals(ReferralStatus.COMPLETED, latest.getFromStatus());
        Assert.assertEquals(ReferralStatus.COMPLETED, latest.getToStatus());
        Assert.assertEquals(ACTOR_USER_ID, latest.getChangedByUserId());
        Assert.assertEquals(
                "REFERRAL_RESULT_RECEIVED {\"analysisId\":\"" + LINKED_ANALYSIS_ID + "\",\"source\":\"fhir\"}",
                latest.getNotes());
    }

    @Test
    public void accept_movesRowFromReturnedToHistoryWithReconciledOutcome() {
        Assert.assertTrue("precondition: COMPLETED referral must be in Returned",
                idsIn(DashboardView.RETURNED).contains(REFERRAL_ID));

        referralService.markReferralReconciled(REFERRAL_ID, ACTOR_USER_ID);

        Assert.assertFalse("reconciled referral must drop out of Returned",
                idsIn(DashboardView.RETURNED).contains(REFERRAL_ID));
        ReferenceLabReferralDTO inHistory = referenceLabResultsService.getDashboardReferrals(DashboardView.HISTORY)
                .stream().filter(r -> REFERRAL_ID.equals(r.getId())).findFirst().orElse(null);
        Assert.assertNotNull("reconciled referral must surface in History", inHistory);
        Assert.assertEquals("Reconciled", inHistory.getOutcome());
        Assert.assertNotNull("closedDate must be populated from reconciled_at", inHistory.getClosedDate());
    }

    @Test
    public void accept_updatesMetricsReturnedDownReconciledTodayUp() {
        ReferenceLabMetricsDTO before = referenceLabResultsService.getDashboardMetrics();

        referralService.markReferralReconciled(REFERRAL_ID, ACTOR_USER_ID);

        ReferenceLabMetricsDTO after = referenceLabResultsService.getDashboardMetrics();
        Assert.assertEquals("returned tile must drop by one", before.getReturned() - 1, after.getReturned());
        Assert.assertEquals("reconciledToday tile must rise by one", before.getReconciledToday() + 1,
                after.getReconciledToday());
    }

    @Test
    public void accept_isIdempotent() {
        referralService.markReferralReconciled(REFERRAL_ID, ACTOR_USER_ID);
        int afterFirst = statusHistoryDAO.findByReferralIdOrderedByChangedAt(REFERRAL_ID).size();

        referralService.markReferralReconciled(REFERRAL_ID, ACTOR_USER_ID);

        Assert.assertEquals("second accept on already-reconciled referral must not append another history row",
                afterFirst, statusHistoryDAO.findByReferralIdOrderedByChangedAt(REFERRAL_ID).size());
    }

    // ---- OGC-804 Reject ------------------------------------------------------

    @Test
    public void reject_transitionsCompletedToRejectedAndPersistsReason() {
        referralService.markReferralRejected(REFERRAL_ID, "hemolyzed", "Sample was hemolyzed on arrival",
                ACTOR_USER_ID);

        Referral fresh = referralService.getReferralById(REFERRAL_ID);
        Assert.assertEquals(ReferralStatus.REJECTED, fresh.getStatus());
        Assert.assertEquals("hemolyzed", fresh.getRejectReasonCode());
        Assert.assertEquals("Sample was hemolyzed on arrival", fresh.getRejectReasonText());
    }

    @Test
    public void reject_appendsCompletedToRejectedHistoryRow() {
        int before = statusHistoryDAO.findByReferralIdOrderedByChangedAt(REFERRAL_ID).size();

        referralService.markReferralRejected(REFERRAL_ID, "clotted", "Specimen clotted", ACTOR_USER_ID);

        List<ReferralStatusHistory> history = statusHistoryDAO.findByReferralIdOrderedByChangedAt(REFERRAL_ID);
        Assert.assertEquals(before + 1, history.size());
        ReferralStatusHistory latest = history.get(history.size() - 1);
        Assert.assertEquals(ReferralStatus.COMPLETED, latest.getFromStatus());
        Assert.assertEquals(ReferralStatus.REJECTED, latest.getToStatus());
        Assert.assertEquals("REFERRAL_RESULT_REJECTED [clotted] Specimen clotted", latest.getNotes());
    }

    @Test
    public void reject_closesLinkedAnalysisToRejectedByReferenceLab() {
        String rejectedStatusId = statusService.getStatusID(AnalysisStatus.RejectedByReferenceLab);
        Assert.assertNotNull("RejectedByReferenceLab must be a configured status", rejectedStatusId);
        Assert.assertNotEquals("RejectedByReferenceLab must resolve to a real id, not the -1 sentinel", "-1",
                rejectedStatusId);

        referralService.markReferralRejected(REFERRAL_ID, "mislabeled", "Wrong patient label", ACTOR_USER_ID);

        Analysis fresh = analysisService.get(LINKED_ANALYSIS_ID);
        Assert.assertEquals("linked analysis must be closed to RejectedByReferenceLab", rejectedStatusId,
                fresh.getStatusId());
        Assert.assertFalse("referred_out must be cleared so the test can be re-referred", fresh.isReferredOut());
    }

    @Test
    public void reject_movesRowFromReturnedToHistoryWithRejectedOutcome() {
        Assert.assertTrue("precondition: COMPLETED referral must be in Returned",
                idsIn(DashboardView.RETURNED).contains(REFERRAL_ID));

        referralService.markReferralRejected(REFERRAL_ID, "other", "Cannot use this result", ACTOR_USER_ID);

        Assert.assertFalse("rejected referral must drop out of Returned",
                idsIn(DashboardView.RETURNED).contains(REFERRAL_ID));
        ReferenceLabReferralDTO inHistory = referenceLabResultsService.getDashboardReferrals(DashboardView.HISTORY)
                .stream().filter(r -> REFERRAL_ID.equals(r.getId())).findFirst().orElse(null);
        Assert.assertNotNull("rejected referral must surface in History", inHistory);
        Assert.assertEquals("Rejected", inHistory.getOutcome());
    }

    @Test
    public void reject_bestEffortFhirFailureStillCommitsLocalRejection() throws Exception {
        // Pin the best-effort contract: when the FHIR publish blows up with an
        // unchecked RuntimeException (unreachable store / HTTP 500), the local DB
        // rejection must still commit — the bug was the publish exception bubbling
        // out of markReferralRejected and rolling back the whole transaction.
        String rejectedStatusId = statusService.getStatusID(AnalysisStatus.RejectedByReferenceLab);
        Assert.assertNotEquals("RejectedByReferenceLab must resolve to a real id, not the -1 sentinel", "-1",
                rejectedStatusId);
        Mockito.doThrow(new RuntimeException("simulated FHIR store outage (HTTP 500)")).when(fhirReferralServiceMock)
                .publishReferralRejected(Mockito.any(Referral.class), Mockito.anyString(), Mockito.anyString());

        // Must NOT propagate — the catch swallows the publish failure.
        referralService.markReferralRejected(REFERRAL_ID, "hemolyzed", "Sample was hemolyzed on arrival",
                ACTOR_USER_ID);

        Referral fresh = referralService.getReferralById(REFERRAL_ID);
        Assert.assertEquals("status must persist as REJECTED despite the FHIR publish failure", ReferralStatus.REJECTED,
                fresh.getStatus());
        Assert.assertEquals("reject reason code must persist despite the FHIR publish failure", "hemolyzed",
                fresh.getRejectReasonCode());
        Assert.assertEquals("reject reason text must persist despite the FHIR publish failure",
                "Sample was hemolyzed on arrival", fresh.getRejectReasonText());

        Analysis freshAnalysis = analysisService.get(LINKED_ANALYSIS_ID);
        Assert.assertEquals(
                "linked analysis must still be closed to RejectedByReferenceLab despite the FHIR publish failure",
                rejectedStatusId, freshAnalysis.getStatusId());
        Assert.assertFalse("referred_out must be cleared despite the FHIR publish failure",
                freshAnalysis.isReferredOut());

        // Confirm the publish was actually attempted (so the test exercises the
        // catch, not a path that skipped publishing).
        Mockito.verify(fhirReferralServiceMock).publishReferralRejected(Mockito.any(Referral.class),
                Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void reject_realTransactionalPublishFailureStillCommitsLocalRejection() throws Exception {
        // TRUE regression guard for the rollback-only bug. The earlier best-effort
        // test stubs fhirReferralService with a plain Mockito mock — a mock has NO
        // @Transactional proxy, so a throw from it never marks the caller's
        // transaction rollback-only; it passed even while production 500'd. This
        // test wires the REAL @Transactional publish bean and forces its FHIR-store
        // read to throw, reproducing the exact production boundary: the publish runs
        // inside markReferralRejected's transaction. With the publish at REQUIRED
        // propagation the inner throw marks the SHARED tx rollback-only and commit
        // dies with UnexpectedRollbackException (this test fails). With the publish
        // at REQUIRES_NEW only the suspended inner tx is poisoned, the caller's catch
        // swallows the failure, and the local rejection commits (this test passes).
        String rejectedStatusId = statusService.getStatusID(AnalysisStatus.RejectedByReferenceLab);
        Assert.assertNotEquals("RejectedByReferenceLab must resolve to a real id, not the -1 sentinel", "-1",
                rejectedStatusId);

        // Give the referral's analysis a fhir_uuid so the publish does NOT early-return
        // (it skips when referral/analysis fhir_uuid is null) and actually reaches the
        // FHIR-store read we are about to break. Committed in its own tx (the test
        // class
        // runs NOT_SUPPORTED) so the REQUIRES_NEW re-fetch sees it.
        Analysis analysis = analysisService.get(LINKED_ANALYSIS_ID);
        analysis.setFhirUuid(UUID.randomUUID());
        analysis.setSysUserId(ACTOR_USER_ID);
        analysisService.update(analysis);

        // Break the FIRST FHIR-store call the publish makes
        // (getServiceRequestByAnalysisUuid),
        // mimicking an unreachable store throwing a raw HAPI RuntimeException. Inject
        // onto
        // the real bean's target (unwrap the @Transactional proxy) and restore in
        // finally so
        // the shared singleton is not polluted for other tests.
        Object realTarget = AopTestUtils.getTargetObject(realFhirReferralService);
        FhirPersistanceService originalPersistence = (FhirPersistanceService) ReflectionTestUtils.getField(realTarget,
                "fhirPersistanceService");
        FhirPersistanceService failingPersistence = Mockito.mock(FhirPersistanceService.class);
        Mockito.when(failingPersistence.getServiceRequestByAnalysisUuid(Mockito.anyString()))
                .thenThrow(new RuntimeException("simulated FHIR store outage (connection refused)"));
        ReflectionTestUtils.setField(realTarget, "fhirPersistanceService", failingPersistence);

        // Point referralService at the REAL transactional publish instead of the
        // happy-path mock installed in init().
        ReflectionTestUtils.setField(referralService, "fhirReferralService", realFhirReferralService);

        try {
            // Must NOT propagate: the real publish throws inside its own REQUIRES_NEW
            // tx, the caller's catch swallows it, and the outer tx commits cleanly.
            referralService.markReferralRejected(REFERRAL_ID, "hemolyzed", "Sample was hemolyzed on arrival",
                    ACTOR_USER_ID);

            Referral fresh = referralService.getReferralById(REFERRAL_ID);
            Assert.assertEquals("status must commit as REJECTED even though the real transactional FHIR publish threw",
                    ReferralStatus.REJECTED, fresh.getStatus());
            Assert.assertEquals("reject reason code must commit despite the publish failure", "hemolyzed",
                    fresh.getRejectReasonCode());
            Assert.assertEquals("reject reason text must commit despite the publish failure",
                    "Sample was hemolyzed on arrival", fresh.getRejectReasonText());

            Analysis freshAnalysis = analysisService.get(LINKED_ANALYSIS_ID);
            Assert.assertEquals(
                    "linked analysis must commit closed to RejectedByReferenceLab despite the publish failure",
                    rejectedStatusId, freshAnalysis.getStatusId());
            Assert.assertFalse("referred_out must commit cleared despite the publish failure",
                    freshAnalysis.isReferredOut());

            // Prove the failing store call was actually reached — i.e. the test exercised
            // the real publish body, not an early-return skip path.
            Mockito.verify(failingPersistence).getServiceRequestByAnalysisUuid(Mockito.anyString());
        } finally {
            ReflectionTestUtils.setField(realTarget, "fhirPersistanceService", originalPersistence);
        }
    }

    @Test
    public void reject_refusesWhenAlreadyReconciled() {
        referralService.markReferralReconciled(REFERRAL_ID, ACTOR_USER_ID);

        IllegalStateException thrown = null;
        try {
            referralService.markReferralRejected(REFERRAL_ID, "other", "too late", ACTOR_USER_ID);
        } catch (IllegalStateException e) {
            thrown = e;
        }
        Assert.assertNotNull("rejecting a reconciled referral must throw IllegalStateException", thrown);
        Referral fresh = referralService.getReferralById(REFERRAL_ID);
        Assert.assertEquals("status must remain COMPLETED after a refused reject", ReferralStatus.COMPLETED,
                fresh.getStatus());
        Assert.assertNull("reject reason must not be persisted on a refused reject", fresh.getRejectReasonText());
    }

    // ---- canTransitionTo guard ----------------------------------------------

    @Test
    public void guard_completedToRejectedAllowedOthersTerminal() {
        Assert.assertTrue("COMPLETED -> REJECTED must be allowed",
                ReferralStatus.COMPLETED.canTransitionTo(ReferralStatus.REJECTED));
        Assert.assertFalse("COMPLETED -> RECEIVED must be illegal",
                ReferralStatus.COMPLETED.canTransitionTo(ReferralStatus.RECEIVED));
        Assert.assertFalse("COMPLETED -> IN_PROGRESS must be illegal",
                ReferralStatus.COMPLETED.canTransitionTo(ReferralStatus.IN_PROGRESS));
        Assert.assertFalse("COMPLETED -> COMPLETED must be illegal",
                ReferralStatus.COMPLETED.canTransitionTo(ReferralStatus.COMPLETED));
        Assert.assertFalse("REJECTED must stay terminal",
                ReferralStatus.REJECTED.canTransitionTo(ReferralStatus.COMPLETED));
        Assert.assertFalse("CANCELLED must stay terminal",
                ReferralStatus.CANCELLED.canTransitionTo(ReferralStatus.REJECTED));
    }

    // ---- regression: bucketing of untouched COMPLETED rows -------------------

    @Test
    public void regression_unactionedCompletedStaysInReturnedNotHistory() {
        // No accept/reject performed — the row must stay in Returned, not leak into
        // History, and the Returned metric must still count it.
        Assert.assertTrue("untouched COMPLETED referral must remain in Returned",
                idsIn(DashboardView.RETURNED).contains(REFERRAL_ID));
        Assert.assertFalse("untouched COMPLETED referral must NOT appear in History",
                idsIn(DashboardView.HISTORY).contains(REFERRAL_ID));
        Assert.assertEquals("reconciledToday must be zero with no accepts", 0L,
                referenceLabResultsService.getDashboardMetrics().getReconciledToday());
    }

    private List<String> idsIn(DashboardView view) {
        return referenceLabResultsService.getDashboardReferrals(view).stream().map(ReferenceLabReferralDTO::getId)
                .toList();
    }
}
