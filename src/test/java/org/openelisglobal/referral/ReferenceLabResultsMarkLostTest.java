package org.openelisglobal.referral;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
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
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkFlowServiceImpl;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkFlowServiceImpl.ReferralResultsImportObjects;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.referral.dao.ReferralStatusHistoryDAO;
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
 * End-to-end coverage for "mark referral as lost": flips ReferralStatus to
 * CANCELLED (FHIR-aligned terminal), writes a status-history row, and pushes
 * the row out of Outstanding into History with outcome "Lost".
 *
 * <p>
 * Also asserts the post-transition side effects: FHIR cancellation push to the
 * receiving lab, local Analysis cancellation, and a guard on the FHIR
 * result-import path that refuses to apply incoming results to a lost referral.
 */
public class ReferenceLabResultsMarkLostTest extends BaseWebContextSensitiveTest {

    private static final String REFERRAL_ID = "1";
    private static final String LINKED_ANALYSIS_ID = "1";
    private static final String ACTOR_USER_ID = "1";
    private static final String REASON = "Courier confirmed loss in transit";

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
    // unqualified bean name; this qualifier targets the @Service impl directly
    // so {@link #fhirResultImport_throwsExplicitLostGuardException} can drive
    // the real method.
    @Autowired
    @Qualifier("fhirReferralServiceImpl")
    private FhirReferralService realFhirReferralService;

    private FhirReferralService fhirReferralServiceMock;
    private Object originalFhirReferralService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
        fhirReferralServiceMock = Mockito.mock(FhirReferralService.class);
        originalFhirReferralService = ReflectionTestUtils.getField(referralService, "fhirReferralService");
        ReflectionTestUtils.setField(referralService, "fhirReferralService", fhirReferralServiceMock);
    }

    @After
    public void restoreFhirReferralService() {
        ReflectionTestUtils.setField(referralService, "fhirReferralService", originalFhirReferralService);
    }

    @Test
    public void markReferralAsLost_flipsStatusToCancelledAndStampsLostMetadata() {
        referralService.markReferralAsLost(REFERRAL_ID, REASON, ACTOR_USER_ID);

        Referral fresh = referralService.getReferralById(REFERRAL_ID);
        Assert.assertEquals(ReferralStatus.CANCELLED, fresh.getStatus());
        Assert.assertTrue(Boolean.TRUE.equals(fresh.getLostStatus()));
        Assert.assertEquals(REASON, fresh.getLostReason());
        Assert.assertNotNull(fresh.getLostDate());
    }

    @Test
    public void markReferralAsLost_appendsStatusHistoryRowWithLostReason() {
        int before = statusHistoryDAO.findByReferralIdOrderedByChangedAt(REFERRAL_ID).size();

        referralService.markReferralAsLost(REFERRAL_ID, REASON, ACTOR_USER_ID);

        List<ReferralStatusHistory> history = statusHistoryDAO.findByReferralIdOrderedByChangedAt(REFERRAL_ID);
        Assert.assertEquals(before + 1, history.size());
        ReferralStatusHistory latest = history.get(history.size() - 1);
        Assert.assertEquals(ReferralStatus.DRAFT, latest.getFromStatus());
        Assert.assertEquals(ReferralStatus.CANCELLED, latest.getToStatus());
        Assert.assertEquals(ACTOR_USER_ID, latest.getChangedByUserId());
        Assert.assertEquals("Marked lost: " + REASON, latest.getNotes());
    }

    @Test
    public void markReferralAsLost_movesRowFromOutstandingToHistoryWithLostOutcome() {
        referralService.dispatchReferral(REFERRAL_ID, java.sql.Timestamp.valueOf("2026-05-15 10:30:00"), ACTOR_USER_ID,
                null);
        Assert.assertTrue("precondition: dispatched referral must be in Outstanding",
                idsIn(DashboardView.OUTSTANDING).contains(REFERRAL_ID));

        referralService.markReferralAsLost(REFERRAL_ID, REASON, ACTOR_USER_ID);

        Assert.assertFalse("lost referral must drop out of Outstanding",
                idsIn(DashboardView.OUTSTANDING).contains(REFERRAL_ID));
        ReferenceLabReferralDTO inHistory = referenceLabResultsService.getDashboardReferrals(DashboardView.HISTORY)
                .stream().filter(r -> REFERRAL_ID.equals(r.getId())).findFirst().orElse(null);
        Assert.assertNotNull("lost referral must surface in History", inHistory);
        Assert.assertEquals("Lost", inHistory.getOutcome());
        Assert.assertNotNull("closedDate must be populated from lostDate", inHistory.getClosedDate());
    }

    @Test
    public void markReferralAsLost_isIdempotent() {
        referralService.markReferralAsLost(REFERRAL_ID, REASON, ACTOR_USER_ID);
        int afterFirst = statusHistoryDAO.findByReferralIdOrderedByChangedAt(REFERRAL_ID).size();

        referralService.markReferralAsLost(REFERRAL_ID, "second call", ACTOR_USER_ID);

        Assert.assertEquals("second mark-lost on already-lost referral must not append another history row", afterFirst,
                statusHistoryDAO.findByReferralIdOrderedByChangedAt(REFERRAL_ID).size());
        Referral fresh = referralService.getReferralById(REFERRAL_ID);
        Assert.assertEquals("reason must remain the original", REASON, fresh.getLostReason());
    }

    @Test
    public void markReferralAsLost_publishesFhirCancellationCarryingTheReason() {
        referralService.markReferralAsLost(REFERRAL_ID, REASON, ACTOR_USER_ID);

        // The mock's recorded invocations should include some call whose arguments
        // carry the user-supplied reason — that's how the implementer signals to the
        // receiving lab that this referral is cancelled and why.
        boolean fhirCalledWithReason = Mockito.mockingDetails(fhirReferralServiceMock).getInvocations().stream()
                .flatMap(inv -> Arrays.stream(inv.getArguments())).anyMatch(arg -> REASON.equals(arg));
        Assert.assertTrue(
                "FhirReferralService must receive a publish invocation carrying the lost reason so it can land in Task.note",
                fhirCalledWithReason);
    }

    @Test
    public void markReferralAsLost_bestEffortFhirFailureStillCommitsLocalLost() throws Exception {
        // Symmetric to the reject best-effort test: an unchecked RuntimeException
        // out of the FHIR publish (unreachable store / HTTP 500) must not roll back
        // the local lost-flag write or the linked-analysis cancellation.
        Analysis preLost = analysisService.get(LINKED_ANALYSIS_ID);
        preLost.setReferredOut(true);
        preLost.setSysUserId(ACTOR_USER_ID);
        analysisService.update(preLost);
        Mockito.doThrow(new RuntimeException("simulated FHIR store outage (HTTP 500)")).when(fhirReferralServiceMock)
                .publishReferralLost(Mockito.any(Referral.class), Mockito.anyString(), Mockito.anyString());

        // Must NOT propagate — the catch swallows the publish failure.
        referralService.markReferralAsLost(REFERRAL_ID, REASON, ACTOR_USER_ID);

        Referral fresh = referralService.getReferralById(REFERRAL_ID);
        Assert.assertEquals("status must persist as CANCELLED despite the FHIR publish failure",
                ReferralStatus.CANCELLED, fresh.getStatus());
        Assert.assertTrue("lostStatus must persist despite the FHIR publish failure",
                Boolean.TRUE.equals(fresh.getLostStatus()));
        Assert.assertEquals("lost reason must persist despite the FHIR publish failure", REASON, fresh.getLostReason());

        Analysis freshAnalysis = analysisService.get(LINKED_ANALYSIS_ID);
        String canceledStatusId = statusService.getStatusID(AnalysisStatus.Canceled);
        Assert.assertEquals("linked analysis must still be cancelled despite the FHIR publish failure",
                canceledStatusId, freshAnalysis.getStatusId());
        Assert.assertFalse("referred_out must be cleared despite the FHIR publish failure",
                freshAnalysis.isReferredOut());

        Mockito.verify(fhirReferralServiceMock).publishReferralLost(Mockito.any(Referral.class), Mockito.anyString(),
                Mockito.anyString());
    }

    @Test
    public void markReferralAsLost_realTransactionalPublishFailureStillCommitsLocalLost() throws Exception {
        // TRUE regression guard for the rollback-only bug on the lost path. Wires the
        // REAL @Transactional publish bean (not a proxy-less Mockito mock) and forces
        // its FHIR-store read to throw, reproducing the production boundary: the
        // publish runs inside markReferralAsLost's transaction. At REQUIRED propagation
        // the inner throw marks the SHARED tx rollback-only -> commit fails with
        // UnexpectedRollbackException (test fails). At REQUIRES_NEW only the inner tx
        // is poisoned, the caller's catch swallows it, and the local lost write commits
        // (test passes).
        Analysis preLost = analysisService.get(LINKED_ANALYSIS_ID);
        preLost.setReferredOut(true);
        preLost.setFhirUuid(UUID.randomUUID());
        preLost.setSysUserId(ACTOR_USER_ID);
        analysisService.update(preLost);

        Object realTarget = AopTestUtils.getTargetObject(realFhirReferralService);
        FhirPersistanceService originalPersistence = (FhirPersistanceService) ReflectionTestUtils.getField(realTarget,
                "fhirPersistanceService");
        FhirPersistanceService failingPersistence = Mockito.mock(FhirPersistanceService.class);
        Mockito.when(failingPersistence.getServiceRequestByAnalysisUuid(Mockito.anyString()))
                .thenThrow(new RuntimeException("simulated FHIR store outage (connection refused)"));
        ReflectionTestUtils.setField(realTarget, "fhirPersistanceService", failingPersistence);

        ReflectionTestUtils.setField(referralService, "fhirReferralService", realFhirReferralService);

        try {
            // Must NOT propagate: the real publish throws inside its own REQUIRES_NEW
            // tx, the caller's catch swallows it, and the outer tx commits cleanly.
            referralService.markReferralAsLost(REFERRAL_ID, REASON, ACTOR_USER_ID);

            Referral fresh = referralService.getReferralById(REFERRAL_ID);
            Assert.assertEquals("status must commit as CANCELLED even though the real transactional FHIR publish threw",
                    ReferralStatus.CANCELLED, fresh.getStatus());
            Assert.assertTrue("lostStatus must commit despite the publish failure",
                    Boolean.TRUE.equals(fresh.getLostStatus()));
            Assert.assertEquals("lost reason must commit despite the publish failure", REASON, fresh.getLostReason());

            Analysis freshAnalysis = analysisService.get(LINKED_ANALYSIS_ID);
            String canceledStatusId = statusService.getStatusID(AnalysisStatus.Canceled);
            Assert.assertEquals("linked analysis must commit cancelled despite the publish failure", canceledStatusId,
                    freshAnalysis.getStatusId());
            Assert.assertFalse("referred_out must commit cleared despite the publish failure",
                    freshAnalysis.isReferredOut());

            Mockito.verify(failingPersistence).getServiceRequestByAnalysisUuid(Mockito.anyString());
        } finally {
            ReflectionTestUtils.setField(realTarget, "fhirPersistanceService", originalPersistence);
        }
    }

    @Test
    public void markReferralAsLost_cancelsLinkedAnalysisAndClearsReferredOutFlag() {
        Analysis preLost = analysisService.get(LINKED_ANALYSIS_ID);
        preLost.setReferredOut(true);
        preLost.setSysUserId(ACTOR_USER_ID);
        analysisService.update(preLost);

        referralService.markReferralAsLost(REFERRAL_ID, REASON, ACTOR_USER_ID);

        Analysis fresh = analysisService.get(LINKED_ANALYSIS_ID);
        String canceledStatusId = statusService.getStatusID(AnalysisStatus.Canceled);
        Assert.assertNotNull("AnalysisStatus.Canceled must be a configured status", canceledStatusId);
        Assert.assertEquals("linked analysis must be cancelled when its referral is lost", canceledStatusId,
                fresh.getStatusId());
        Assert.assertFalse("referred_out must be cleared so the analysis can be re-referred via a new referral",
                fresh.isReferredOut());
    }

    @Test
    public void fhirResultImport_throwsExplicitLostGuardException() throws Exception {
        // Pin a fhir_uuid on the linked analysis so setReferralResult's SR-by-uuid
        // lookup resolves; otherwise the call throws for an unrelated reason and
        // we can't verify the guard.
        UUID analysisFhirUuid = UUID.randomUUID();
        Analysis analysis = analysisService.get(LINKED_ANALYSIS_ID);
        analysis.setFhirUuid(analysisFhirUuid);
        analysis.setSysUserId(ACTOR_USER_ID);
        analysisService.update(analysis);

        referralService.markReferralAsLost(REFERRAL_ID, REASON, ACTOR_USER_ID);

        // ReferralResultsImportObjects is a non-static inner of
        // FhirApiWorkFlowServiceImpl,
        // so its synthetic constructor takes the outer instance as the first arg.
        Constructor<ReferralResultsImportObjects> ctor = ReferralResultsImportObjects.class
                .getDeclaredConstructor(FhirApiWorkFlowServiceImpl.class);
        ctor.setAccessible(true);
        ReferralResultsImportObjects imp = ctor.newInstance(new FhirApiWorkFlowServiceImpl());
        ServiceRequest sr = new ServiceRequest();
        sr.setId(analysisFhirUuid.toString());
        imp.originalReferralObjects.serviceRequests = List.of(sr);
        imp.originalReferralObjects.task = new Task();

        RuntimeException thrown = null;
        try {
            realFhirReferralService.setReferralResult(imp);
        } catch (RuntimeException e) {
            thrown = e;
        }
        Assert.assertNotNull(
                "setReferralResult must refuse to apply results for a lost referral — expected RuntimeException, got none",
                thrown);
        String msg = thrown.getMessage() == null ? "" : thrown.getMessage().toLowerCase();
        Assert.assertTrue(
                "refusal exception's message must reference \"lost\" so the cause is observable in logs (got: "
                        + thrown.getMessage() + ")",
                msg.contains("lost"));
    }

    private List<String> idsIn(DashboardView view) {
        return referenceLabResultsService.getDashboardReferrals(view).stream().map(ReferenceLabReferralDTO::getId)
                .toList();
    }
}
