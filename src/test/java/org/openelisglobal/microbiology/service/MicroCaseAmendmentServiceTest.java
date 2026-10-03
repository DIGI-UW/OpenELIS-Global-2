package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
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
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAmendmentDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendmentStatus;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;

@RunWith(MockitoJUnitRunner.class)
public class MicroCaseAmendmentServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseAmendmentDAO amendmentDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    @Mock
    private MicroReportVersionService reportVersionService;

    @Mock
    private MicroAstRunDAO astRunDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Mock
    private MicroIdentificationHistoryService identificationHistoryService;

    private MicroCaseAmendmentService service;

    @Before
    public void setUp() {
        service = new MicroCaseAmendmentServiceImpl(caseDAO, amendmentDAO, activityDAO, reportVersionService, astRunDAO,
                isolateDAO, identificationHistoryService);
    }

    @Test
    public void openAmendmentCapturesBaselineAndUnlocksFinalCase() {
        MicroCase microCase = finalCase();
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(amendmentDAO.getOpenByCaseId("case-1")).thenReturn(null);
        when(amendmentDAO.getNextSequence("case-1")).thenReturn(2);
        when(caseDAO.update(microCase)).thenReturn(microCase);

        MicroCaseAmendment amendment = service.openAmendment("case-1", "Correct organism identification", "9");

        assertEquals("case-1", amendment.getCaseId());
        assertEquals(Integer.valueOf(2), amendment.getSequenceNumber());
        assertEquals(MicroCaseAmendmentStatus.OPEN.name(), amendment.getStatus());
        assertEquals("Correct organism identification", amendment.getReason());
        assertEquals("9", amendment.getOpenedBy());
        assertNotNull(amendment.getOpenedAt());
        assertEquals(MicroCaseStage.AMENDED.name(), microCase.getStage());
        assertEquals(MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name(), microCase.getFinalReleaseState());
        verify(reportVersionService).ensureFinalBaseline(microCase);
        verify(amendmentDAO).insert(amendment);
        verify(caseDAO).update(microCase);
        verify(activityDAO).insert(any(MicroCaseActivity.class));
    }

    @Test
    public void openAmendmentRejectsNonFinalCase() {
        MicroCase microCase = finalCase();
        microCase.setStage(MicroCaseStage.REVIEW_READY.name());
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.NOT_READY.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));

        try {
            service.openAmendment("case-1", "Correction", "9");
            fail("Expected amendment to require final release");
        } catch (IllegalStateException expected) {
            assertEquals("AMENDMENT_REQUIRES_FINAL_RELEASE", expected.getMessage());
        }

        verify(amendmentDAO, never()).insert(any(MicroCaseAmendment.class));
    }

    @Test
    public void openAmendmentRequiresReason() {
        try {
            service.openAmendment("case-1", "  ", "9");
            fail("Expected amendment reason to be required");
        } catch (IllegalArgumentException expected) {
            assertEquals("AMENDMENT_REASON_REQUIRED", expected.getMessage());
        }
    }

    @Test
    public void openAmendmentRejectsSecondOpenAmendment() {
        when(caseDAO.get("case-1")).thenReturn(Optional.of(finalCase()));
        MicroCaseAmendment existing = new MicroCaseAmendment();
        existing.setId("amendment-1");
        existing.setCaseId("case-1");
        existing.setStatus(MicroCaseAmendmentStatus.OPEN.name());
        when(amendmentDAO.getOpenByCaseId("case-1")).thenReturn(existing);

        try {
            service.openAmendment("case-1", "Another correction", "9");
            fail("Expected only one open amendment");
        } catch (IllegalStateException expected) {
            assertEquals("AMENDMENT_ALREADY_OPEN", expected.getMessage());
        }
    }

    @Test
    public void completeAmendmentClosesLifecycleAndRelocksCase() {
        MicroCase microCase = finalCase();
        microCase.setStage(MicroCaseStage.AMENDED.name());
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name());
        MicroCaseAmendment amendment = openAmendment();
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(amendmentDAO.getOpenByCaseId("case-1")).thenReturn(amendment);
        when(amendmentDAO.update(amendment)).thenReturn(amendment);
        when(caseDAO.update(microCase)).thenReturn(microCase);
        MicroReportProjectionResult projection = new MicroReportProjectionResult("Isolate A: Klebsiella pneumoniae",
                true, List.of("202"));

        MicroCaseAmendment completed = service.completeAmendment("case-1", projection, "9");

        assertEquals(MicroCaseAmendmentStatus.RELEASED.name(), completed.getStatus());
        assertEquals("9", completed.getClosedBy());
        assertNotNull(completed.getClosedAt());
        assertEquals(MicroCaseStage.FINAL_RELEASED.name(), microCase.getStage());
        assertEquals(MicroCaseFinalReleaseState.FINAL_RELEASED.name(), microCase.getFinalReleaseState());
        verify(reportVersionService).recordAmendedFinal(amendment, projection, "9");
    }

    @Test
    public void cancelAmendmentRevertsDraftIdentificationAndRelocksWithoutPublishing() {
        MicroCase microCase = finalCase();
        microCase.setStage(MicroCaseStage.AMENDED.name());
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name());
        MicroCaseAmendment amendment = openAmendment();
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(amendmentDAO.getOpenByCaseId("case-1")).thenReturn(amendment);
        when(amendmentDAO.update(amendment)).thenReturn(amendment);
        when(caseDAO.update(microCase)).thenReturn(microCase);
        MicroAstRun source = new MicroAstRun();
        source.setId("run-1");
        source.setStatus(MicroAstRunStatus.REVIEWED.name());
        source.setReportable(false);
        MicroAstRun repeat = new MicroAstRun();
        repeat.setId("run-2");
        repeat.setSourceRunId("run-1");
        repeat.setReportable(true);
        when(astRunDAO.getByAmendmentId("amendment-2")).thenReturn(List.of(repeat));
        when(astRunDAO.get("run-1")).thenReturn(Optional.of(source));
        MicroIsolate amendmentIsolate = new MicroIsolate();
        amendmentIsolate.setId("isolate-2");
        amendmentIsolate.setCaseId("case-1");
        amendmentIsolate.setAmendmentId("amendment-2");
        when(isolateDAO.getByAmendmentId("amendment-2")).thenReturn(List.of(amendmentIsolate));

        MicroCaseAmendment cancelled = service.cancelAmendment("case-1", "Correction no longer required", "9");

        assertEquals(MicroCaseAmendmentStatus.CANCELLED.name(), cancelled.getStatus());
        assertEquals("Correction no longer required", cancelled.getClosingReason());
        assertEquals(MicroCaseStage.FINAL_RELEASED.name(), microCase.getStage());
        assertEquals(true, source.isReportable());
        assertEquals(false, repeat.isReportable());
        assertEquals(MicroAstRunStatus.CANCELLED.name(), repeat.getStatus());
        assertNotNull(amendmentIsolate.getCancelledAt());
        verify(astRunDAO).update(source);
        verify(astRunDAO).update(repeat);
        verify(isolateDAO).update(amendmentIsolate);
        verify(identificationHistoryService).revertAmendment("amendment-2", "Correction no longer required", "9");
        verify(reportVersionService, never()).recordAmendedFinal(any(MicroCaseAmendment.class),
                any(MicroReportProjectionResult.class), any(String.class));
    }

    private MicroCase finalCase() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());
        microCase.setClosedBy("7");
        microCase.setClosedAt(MicroCaseServiceImpl.now());
        return microCase;
    }

    private MicroCaseAmendment openAmendment() {
        MicroCaseAmendment amendment = new MicroCaseAmendment();
        amendment.setId("amendment-2");
        amendment.setCaseId("case-1");
        amendment.setSequenceNumber(2);
        amendment.setStatus(MicroCaseAmendmentStatus.OPEN.name());
        amendment.setReason("Correct organism identification");
        amendment.setOpenedBy("8");
        amendment.setOpenedAt(MicroCaseServiceImpl.now());
        return amendment;
    }
}
