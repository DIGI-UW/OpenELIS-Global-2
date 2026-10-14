package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCriticalCommunicationDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.form.MicroCaseReadinessForm;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;

@RunWith(MockitoJUnitRunner.class)
public class MicroCaseReadinessServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Mock
    private MicroAstRunDAO astRunDAO;

    @Mock
    private MicroCriticalCommunicationDAO communicationDAO;

    private MicroCaseReadinessService service;
    private org.openelisglobal.microbiology.valueholder.MicroCaseInoculation culture;
    @Mock
    private MicroAstReportChoiceService choices;

    @Before
    public void setUp() {
        service = new MicroCaseReadinessServiceImpl(caseDAO, isolateDAO, astRunDAO, communicationDAO);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "isolateWorkspace",
                org.mockito.Mockito.mock(MicroIsolateWorkspaceServiceImpl.class));
        var cultures = org.mockito.Mockito.mock(org.openelisglobal.microbiology.dao.MicroCaseInoculationDAO.class);
        culture = new org.openelisglobal.microbiology.valueholder.MicroCaseInoculation();
        culture.setOutcome("GROWTH");
        culture.setOutcomeAt(new java.sql.Timestamp(1000));
        when(cultures.getRows("case-1")).thenReturn(List.of(culture));
        var dst = org.mockito.Mockito.mock(MicroDstService.class);
        when(dst.evaluate("case-1")).thenReturn(new org.openelisglobal.microbiology.form.MicroDstForm());
        org.springframework.test.util.ReflectionTestUtils.setField(service, "cultures", cultures);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "dst", dst);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "choices", choices);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "requirements",
                org.mockito.Mockito.mock(MicroCaseFinalRequirementsService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "links",
                org.mockito.Mockito.mock(org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "referrals",
                org.mockito.Mockito.mock(org.openelisglobal.referral.dao.ReferralDAO.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "statuses",
                org.mockito.Mockito.mock(org.openelisglobal.common.services.IStatusService.class));
    }

    @Test
    public void missingIsolateBlocksFinalRelease() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getWorkspace("case-1")).thenReturn(List.of());

        MicroCaseReadinessForm readiness = service.getReadiness("case-1");

        assertFalse(readiness.finalReleaseReady);
        assertTrue(readiness.blockers.contains("ISOLATE_REQUIRED"));
    }

    @Test
    public void unreviewedAstBlocksFinalRelease() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroIsolate isolate = significantIsolate();
        MicroAstRun run = new MicroAstRun();
        run.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getWorkspace("case-1")).thenReturn(List.of(isolate));
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(run));

        MicroCaseReadinessForm readiness = service.getReadiness("case-1");

        assertFalse(readiness.finalReleaseReady);
        assertTrue(readiness.blockers.contains("AST_REVIEW_REQUIRED"));
    }

    @Test
    public void reviewedRunWithoutDrugSelectionDoesNotAllowFinalRelease() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroIsolate isolate = significantIsolate();
        MicroAstRun reviewed = new MicroAstRun();
        reviewed.setId("run-1");
        reviewed.setStatus(MicroAstRunStatus.REVIEWED.name());
        reviewed.setReportable(true);
        MicroAstRun repeat = new MicroAstRun();
        repeat.setId("run-2");
        repeat.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getWorkspace("case-1")).thenReturn(List.of(isolate));
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(reviewed, repeat));

        MicroCaseReadinessForm readiness = service.getReadiness("case-1");

        assertFalse(readiness.finalReleaseReady);
        assertTrue(readiness.blockers.contains("AST_REVIEW_REQUIRED"));
    }

    @Test
    public void reviewedAstAllowsFinalRelease() {
        when(choices.getChoices("iso-1")).thenReturn(List.of(new org.openelisglobal.microbiology.valueholder.MicroAstReportChoice()));
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroIsolate isolate = significantIsolate();
        MicroAstRun run = new MicroAstRun();
        run.setStatus(MicroAstRunStatus.REVIEWED.name());
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getWorkspace("case-1")).thenReturn(List.of(isolate));
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(run));

        MicroCaseReadinessForm readiness = service.getReadiness("case-1");

        assertTrue(readiness.finalReleaseReady);
        assertTrue(readiness.blockers.isEmpty());
    }

    @Test
    public void multipleReviewedAttemptsRequireIndividualDrugSelections() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroIsolate isolate = significantIsolate();
        MicroAstRun original = reviewedRun(false);
        MicroAstRun repeat = reviewedRun(false);
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getWorkspace("case-1")).thenReturn(List.of(isolate));
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(original, repeat));

        MicroCaseReadinessForm blocked = service.getReadiness("case-1");

        assertFalse(blocked.finalReleaseReady);
        assertTrue(blocked.blockers.contains("AST_REVIEW_REQUIRED"));

        when(choices.getChoices("iso-1"))
                .thenReturn(List.of(new org.openelisglobal.microbiology.valueholder.MicroAstReportChoice()));
        MicroCaseReadinessForm ready = service.getReadiness("case-1");
        assertTrue(ready.finalReleaseReady);
    }

    @Test
    public void noGrowthReadyAllowsFinalReleaseWithoutAnIsolate() {
        culture.setOutcome("NO_GROWTH");
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage(MicroCaseStage.NO_GROWTH_READY.name());
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getWorkspace("case-1")).thenReturn(List.of());
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());

        MicroCaseReadinessForm readiness = service.getReadiness("case-1");

        assertTrue(readiness.finalReleaseReady);
        assertTrue(readiness.blockers.isEmpty());
    }

    @Test
    public void openCriticalFollowUpBlocksFinalRelease() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroIsolate isolate = significantIsolate();
        MicroAstRun run = new MicroAstRun();
        run.setStatus(MicroAstRunStatus.REVIEWED.name());
        MicroCriticalCommunication communication = new MicroCriticalCommunication();
        communication.setFollowUpNeeded(Boolean.TRUE);
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.OPEN.name());
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getWorkspace("case-1")).thenReturn(List.of(isolate));
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of(communication));
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(run));

        MicroCaseReadinessForm readiness = service.getReadiness("case-1");

        assertFalse(readiness.finalReleaseReady);
        assertTrue(readiness.blockers.contains("CRITICAL_FOLLOW_UP_REQUIRED"));
    }

    @Test
    public void astProgressIncludesAwaitingSetupAndPendingIdentification() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroIsolate completed = significantIsolate();
        MicroIsolate awaitingSetup = significantIsolate();
        awaitingSetup.setId("iso-2");
        MicroIsolate pendingIdentification = new MicroIsolate();
        pendingIdentification.setId("iso-3");
        pendingIdentification.setSignificance(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name());
        MicroAstRun reviewed = reviewedRun(true);
        MicroAstRun invalidated = new MicroAstRun();
        invalidated.setStatus(MicroAstRunStatus.INVALIDATED.name());
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getWorkspace("case-1")).thenReturn(List.of(completed, awaitingSetup, pendingIdentification));
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(reviewed, invalidated));
        when(astRunDAO.getByIsolateId("iso-2")).thenReturn(List.of());
        when(astRunDAO.getByIsolateId("iso-3")).thenReturn(List.of());

        MicroCaseReadinessForm readiness = service.getReadiness("case-1");

        assertEquals(1, readiness.astRunsComplete);
        assertEquals(1, readiness.astRunsTotal);
        assertEquals(1, readiness.significantIsolatesAwaitingAstSetup);
        assertEquals(1, readiness.isolatesPendingIdentification);
        assertTrue(readiness.blockers.contains("ISOLATE_IDENTIFICATION_REQUIRED"));
    }

    @Test
    public void failedQualityControlBlocksEvenANonSignificantIsolate() {
        var c = new MicroCase();
        c.setId("case-1");
        var isolate = significantIsolate();
        isolate.setSignificance("UNKNOWN");
        var run = new MicroAstRun();
        run.setStatus("QC_FAILED");
        run.setQcState("FAILED");
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(c));
        when(isolateDAO.getWorkspace("case-1")).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(run));
        var readiness = service.getReadiness("case-1");
        assertFalse(readiness.finalReleaseReady);
        assertTrue(readiness.blockers.contains("AST_REVIEW_REQUIRED"));
    }

    private MicroIsolate significantIsolate() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setSignificance(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name());
        isolate.setOrganismId("organism-1");
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.CONFIRMED.name());
        return isolate;
    }

    private MicroAstRun reviewedRun(boolean reportable) {
        MicroAstRun run = new MicroAstRun();
        run.setStatus(MicroAstRunStatus.REVIEWED.name());
        run.setReportable(reportable);
        return run;
    }
}
