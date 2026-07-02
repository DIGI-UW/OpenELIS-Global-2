package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCriticalCommunicationDAO;
import org.openelisglobal.microbiology.form.MicroCaseReadinessForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;

@RunWith(MockitoJUnitRunner.class)
public class MicroReportReleaseServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    @Mock
    private MicroCaseReadinessService readinessService;

    @Mock
    private MicroCaseStateService stateService;

    @Mock
    private MicroCriticalCommunicationDAO communicationDAO;

    private MicroReportReleaseService service;

    @Before
    public void setUp() {
        service = new MicroReportReleaseServiceImpl(caseDAO, activityDAO, readinessService, stateService,
                communicationDAO);
    }

    @Test
    public void finalReleaseIsBlockedUntilReadinessPasses() {
        MicroCaseReadinessForm readiness = new MicroCaseReadinessForm();
        readiness.caseId = "case-1";
        readiness.finalReleaseReady = false;
        readiness.blockers.add("AST_REVIEW_REQUIRED");
        when(readinessService.getReadiness("case-1")).thenReturn(readiness);

        try {
            service.releaseFinal("case-1", "1");
            fail("Expected final release to be blocked");
        } catch (IllegalStateException expected) {
            assertEquals("Final release is blocked: AST_REVIEW_REQUIRED", expected.getMessage());
        }
    }

    @Test
    public void finalReleaseUpdatesCaseAndRecordsHistory() {
        MicroCase transitionedCase = new MicroCase();
        transitionedCase.setId("case-1");
        transitionedCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        MicroCaseReadinessForm readiness = new MicroCaseReadinessForm();
        readiness.caseId = "case-1";
        readiness.finalReleaseReady = true;
        when(readinessService.getReadiness("case-1")).thenReturn(readiness);
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(stateService.advanceStage("case-1", MicroCaseStage.FINAL_RELEASED, "1", "Final report released"))
                .thenReturn(transitionedCase);
        when(caseDAO.update(transitionedCase)).thenReturn(transitionedCase);

        MicroCase released = service.releaseFinal("case-1", "1");

        assertEquals(MicroCaseFinalReleaseState.FINAL_RELEASED.name(), released.getFinalReleaseState());
        assertEquals(MicroCaseStage.FINAL_RELEASED.name(), released.getStage());
        verify(stateService).advanceStage("case-1", MicroCaseStage.FINAL_RELEASED, "1", "Final report released");
        verify(activityDAO).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void finalReleaseIsBlockedByOpenCriticalFollowUp() {
        MicroCaseReadinessForm readiness = new MicroCaseReadinessForm();
        readiness.caseId = "case-1";
        readiness.finalReleaseReady = true;
        MicroCriticalCommunication communication = new MicroCriticalCommunication();
        communication.setFollowUpNeeded(Boolean.TRUE);
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.OPEN.name());
        when(readinessService.getReadiness("case-1")).thenReturn(readiness);
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of(communication));

        try {
            service.releaseFinal("case-1", "1");
            fail("Expected final release to be blocked by critical follow-up");
        } catch (IllegalStateException expected) {
            assertEquals("Final release is blocked: CRITICAL_FOLLOW_UP_REQUIRED", expected.getMessage());
        }
    }

    @Test
    public void finalReleaseUsesCaseStateTransitionGuard() {
        MicroCaseReadinessForm readiness = new MicroCaseReadinessForm();
        readiness.caseId = "case-1";
        readiness.finalReleaseReady = true;
        when(readinessService.getReadiness("case-1")).thenReturn(readiness);
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(stateService.advanceStage("case-1", MicroCaseStage.FINAL_RELEASED, "1", "Final report released"))
                .thenThrow(new IllegalArgumentException("Invalid microbiology case stage transition"));

        try {
            service.releaseFinal("case-1", "1");
            fail("Expected final release to use the case transition guard");
        } catch (IllegalArgumentException expected) {
            assertEquals("Invalid microbiology case stage transition", expected.getMessage());
        }

        verify(caseDAO, org.mockito.Mockito.never()).update(any());
    }
}
