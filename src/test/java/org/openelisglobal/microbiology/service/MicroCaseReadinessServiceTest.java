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

    @Before
    public void setUp() {
        service = new MicroCaseReadinessServiceImpl(caseDAO, isolateDAO, astRunDAO, communicationDAO);
    }

    @Test
    public void missingIsolateBlocksFinalRelease() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of());

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
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(run));

        MicroCaseReadinessForm readiness = service.getReadiness("case-1");

        assertFalse(readiness.finalReleaseReady);
        assertTrue(readiness.blockers.contains("AST_REVIEW_REQUIRED"));
    }

    @Test
    public void reviewedAstAllowsFinalRelease() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroIsolate isolate = significantIsolate();
        MicroAstRun run = new MicroAstRun();
        run.setStatus(MicroAstRunStatus.REVIEWED.name());
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(run));

        MicroCaseReadinessForm readiness = service.getReadiness("case-1");

        assertTrue(readiness.finalReleaseReady);
        assertTrue(readiness.blockers.isEmpty());
    }

    @Test
    public void multipleReviewedAttemptsRequireExactlyOneReportableSelection() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        MicroIsolate isolate = significantIsolate();
        MicroAstRun original = reviewedRun(false);
        MicroAstRun repeat = reviewedRun(false);
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(original, repeat));

        MicroCaseReadinessForm blocked = service.getReadiness("case-1");

        assertFalse(blocked.finalReleaseReady);
        assertTrue(blocked.blockers.contains("REPORTABLE_AST_RUN_REQUIRED"));

        repeat.setReportable(true);
        MicroCaseReadinessForm ready = service.getReadiness("case-1");
        assertTrue(ready.finalReleaseReady);
    }

    @Test
    public void noGrowthReadyAllowsFinalReleaseWithoutAnIsolate() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage(MicroCaseStage.NO_GROWTH_READY.name());
        when(caseDAO.get("case-1")).thenReturn(java.util.Optional.of(microCase));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of());
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
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
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
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(completed, awaitingSetup, pendingIdentification));
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
