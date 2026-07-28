package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
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

/**
 * Final release is gated by {@link MicroCaseReadinessService} (isolate + AST
 * review + critical follow-up state), not by {@code MicroCase.stage}. Nothing
 * in the isolate/AST flow advances {@code stage} through the intermediate
 * culture stages, so requiring a specific source stage here (as a prior
 * revision did via {@code MicroCaseStateService.advanceStage}) made a
 * readiness-eligible case unreleasable. {@code releaseFinal} now sets
 * {@code stage} directly, consistent with {@code releasePreliminary}.
 */
@RunWith(MockitoJUnitRunner.class)
public class MicroReportReleaseServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    @Mock
    private MicroCaseReadinessService readinessService;

    @Mock
    private MicroCriticalCommunicationDAO communicationDAO;

    @Mock
    private MicroReportProjectionService reportProjectionService;

    private MicroReportReleaseService service;

    @Before
    public void setUp() {
        service = new MicroReportReleaseServiceImpl(caseDAO, activityDAO, readinessService, communicationDAO,
                reportProjectionService);
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
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage(MicroCaseStage.SETUP_RECORDED.name());
        MicroCaseReadinessForm readiness = new MicroCaseReadinessForm();
        readiness.caseId = "case-1";
        readiness.finalReleaseReady = true;
        when(readinessService.getReadiness("case-1")).thenReturn(readiness);
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseDAO.update(microCase)).thenReturn(microCase);
        when(reportProjectionService.releaseFinal("case-1", "1"))
                .thenReturn(new MicroReportProjectionResult("Isolate 1: Escherichia coli", true, List.of("11")));

        MicroCase released = service.releaseFinal("case-1", "1");

        assertEquals(MicroCaseFinalReleaseState.FINAL_RELEASED.name(), released.getFinalReleaseState());
        assertEquals(MicroCaseStage.FINAL_RELEASED.name(), released.getStage());
        assertNotNull(released.getClosedAt());
        assertEquals("1", released.getClosedBy());
        verify(caseDAO).update(microCase);
        verify(activityDAO).insert(org.mockito.ArgumentMatchers.any());
    }

    /**
     * Regression for the bug this revision fixes: a case can be readiness-eligible
     * (isolate present, AST reviewed, no open critical follow-up) while its raw
     * {@code stage} is still an early culture stage, because nothing in the
     * isolate/AST flow advances it. Final release must not require a specific
     * source stage.
     */
    @Test
    public void finalReleaseSucceedsRegardlessOfCurrentStageOnceReadinessPasses() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage(MicroCaseStage.SETUP_RECORDED.name());
        MicroCaseReadinessForm readiness = new MicroCaseReadinessForm();
        readiness.caseId = "case-1";
        readiness.finalReleaseReady = true;
        when(readinessService.getReadiness("case-1")).thenReturn(readiness);
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseDAO.update(microCase)).thenReturn(microCase);
        when(reportProjectionService.releaseFinal("case-1", "1"))
                .thenReturn(new MicroReportProjectionResult("Isolate 1: Escherichia coli", true, List.of("11")));

        MicroCase released = service.releaseFinal("case-1", "1");

        assertEquals(MicroCaseStage.FINAL_RELEASED.name(), released.getStage());
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

}
