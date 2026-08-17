package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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

    @Mock
    private MicroReportVersionService reportVersionService;

    @Mock
    private MicroCaseAmendmentService amendmentService;

    private MicroReportReleaseService service;

    @Before
    public void setUp() {
        service = new MicroReportReleaseServiceImpl(caseDAO, activityDAO, readinessService, communicationDAO,
                reportProjectionService, reportVersionService, amendmentService);
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
        verify(reportProjectionService, never()).releaseFinal(anyString(), anyString());
        verify(caseDAO, never()).update(any(MicroCase.class));
        verify(reportVersionService, never()).recordInitialFinal(anyString(), any(MicroReportProjectionResult.class),
                anyString());
        verify(activityDAO, never()).insert(any());
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
        verify(reportVersionService).recordInitialFinal(org.mockito.ArgumentMatchers.eq("case-1"),
                org.mockito.ArgumentMatchers.any(MicroReportProjectionResult.class),
                org.mockito.ArgumentMatchers.eq("1"));
        verify(activityDAO).insert(org.mockito.ArgumentMatchers.any());
    }

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
        verify(reportProjectionService, never()).releaseFinal(anyString(), anyString());
        verify(caseDAO, never()).update(any(MicroCase.class));
        verify(reportVersionService, never()).recordInitialFinal(anyString(), any(MicroReportProjectionResult.class),
                anyString());
        verify(activityDAO, never()).insert(any());
    }

    @Test
    public void projectionFailureDoesNotPartiallyReleaseTheCase() {
        MicroCaseReadinessForm readiness = new MicroCaseReadinessForm();
        readiness.caseId = "case-1";
        readiness.finalReleaseReady = true;
        when(readinessService.getReadiness("case-1")).thenReturn(readiness);
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(reportProjectionService.releaseFinal("case-1", "42"))
                .thenThrow(new IllegalStateException("REPORT_MAPPING_REQUIRED"));

        try {
            service.releaseFinal("case-1", "42");
            fail("Expected final release to fail before changing case state");
        } catch (IllegalStateException expected) {
            assertEquals("REPORT_MAPPING_REQUIRED", expected.getMessage());
        }

        verify(caseDAO, never()).update(any(MicroCase.class));
        verify(reportVersionService, never()).recordInitialFinal(anyString(), any(MicroReportProjectionResult.class),
                anyString());
        verify(activityDAO, never()).insert(any());
    }

}
