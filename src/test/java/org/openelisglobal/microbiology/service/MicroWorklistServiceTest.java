package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.openelisglobal.microbiology.form.MicroWorklistPageForm;
import org.openelisglobal.microbiology.form.MicroWorklistQueryForm;
import org.openelisglobal.microbiology.form.MicroWorklistRowForm;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;

@RunWith(MockitoJUnitRunner.class)
public class MicroWorklistServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Mock
    private MicroAstRunDAO astRunDAO;

    @Mock
    private MicroCriticalCommunicationDAO communicationDAO;

    private MicroWorklistService service;

    @Before
    public void setUp() {
        service = new MicroWorklistServiceImpl(caseDAO, isolateDAO, astRunDAO, communicationDAO);
    }

    @Test
    public void worklistPrioritizesAstReviewBeforeSetupAndShowsSiblings() {
        MicroCase astCase = microCase("case-ast", "sample-1", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.SETUP_RECORDED, "ROUTINE");
        MicroCase setupCase = microCase("case-setup", "sample-2", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.RECEIVED, "ROUTINE");
        MicroCase siblingCase = microCase("case-tb", "sample-1", MicroWorkflowType.MYCOBACTERIOLOGY_TB,
                MicroCaseStage.RECEIVED, "ROUTINE");
        MicroIsolate isolate = significantIsolate("iso-1");
        isolate.setCaseId("case-ast");
        MicroAstRun run = new MicroAstRun();
        run.setIsolateId("iso-1");
        run.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        when(caseDAO.getOpenCases()).thenReturn(List.of(setupCase, astCase, siblingCase));
        when(caseDAO.getBySampleItemIds(List.of("sample-2", "sample-1")))
                .thenReturn(List.of(setupCase, astCase, siblingCase));
        when(isolateDAO.getByCaseIds(List.of("case-setup", "case-ast", "case-tb"))).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateIds(List.of("iso-1"))).thenReturn(List.of(run));
        when(communicationDAO.getByCaseIds(List.of("case-setup", "case-ast", "case-tb"))).thenReturn(List.of());

        List<MicroWorklistRowForm> rows = service.getWorklistRows();

        assertEquals("case-ast", rows.get(0).caseId);
        assertEquals("AST_REVIEW", rows.get(0).dueAction);
        assertEquals("HIGH", rows.get(0).urgency);
        assertTrue(rows.get(0).siblingWorkflows.contains(MicroWorkflowType.MYCOBACTERIOLOGY_TB.name()));
        assertEquals("SETUP", rows.get(1).dueAction);
        verify(isolateDAO).getByCaseIds(List.of("case-setup", "case-ast", "case-tb"));
        verify(astRunDAO).getByIsolateIds(List.of("iso-1"));
        verify(communicationDAO).getByCaseIds(List.of("case-setup", "case-ast", "case-tb"));
        verify(caseDAO, never()).getBySampleItem(anyString());
        verify(isolateDAO, never()).getByCaseId(anyString());
        verify(astRunDAO, never()).getByIsolateId(anyString());
        verify(communicationDAO, never()).getByCaseId(anyString());
    }

    @Test
    public void openCriticalCommunicationRaisesUrgency() {
        MicroCase microCase = microCase("case-1", "sample-1", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.SETUP_RECORDED, "ROUTINE");
        MicroCriticalCommunication communication = new MicroCriticalCommunication();
        communication.setCaseId("case-1");
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.OPEN.name());
        communication.setFollowUpNeeded(true);
        when(caseDAO.getOpenCases()).thenReturn(List.of(microCase));
        when(caseDAO.getBySampleItemIds(List.of("sample-1"))).thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of());
        when(communicationDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(communication));

        MicroWorklistRowForm row = service.getWorklistRows().get(0);

        assertEquals("HIGH", row.urgency);
        assertTrue(row.hasOpenCriticalCommunication);
    }

    @Test
    public void unassignedCaseIsSurfacedAsTheFirstRequiredAction() {
        MicroCase unassigned = microCase("case-unassigned", "sample-1", MicroWorkflowType.UNASSIGNED,
                MicroCaseStage.RECEIVED, "ROUTINE");
        when(caseDAO.getOpenCases()).thenReturn(List.of(unassigned));
        when(caseDAO.getBySampleItemIds(List.of("sample-1"))).thenReturn(List.of(unassigned));
        when(isolateDAO.getByCaseIds(List.of("case-unassigned"))).thenReturn(List.of());
        when(communicationDAO.getByCaseIds(List.of("case-unassigned"))).thenReturn(List.of());

        MicroWorklistRowForm row = service.getWorklistRows().get(0);

        assertEquals("NEEDS_WORKFLOW", row.dueAction);
    }

    @Test
    public void worklistFiltersSearchesAndPaginatesOnTheServer() {
        MicroCase bacteriology = microCase("case-bac", "sample-1", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.RECEIVED, "ROUTINE");
        MicroCase tb = microCase("case-tb", "sample-2", MicroWorkflowType.MYCOBACTERIOLOGY_TB, MicroCaseStage.RECEIVED,
                "ROUTINE");
        when(caseDAO.getOpenCases()).thenReturn(List.of(bacteriology, tb));
        when(caseDAO.getBySampleItemIds(List.of("sample-1", "sample-2"))).thenReturn(List.of(bacteriology, tb));
        when(isolateDAO.getByCaseIds(List.of("case-bac", "case-tb"))).thenReturn(List.of());
        when(communicationDAO.getByCaseIds(List.of("case-bac", "case-tb"))).thenReturn(List.of());

        MicroWorklistQueryForm query = new MicroWorklistQueryForm();
        query.workflow = MicroWorkflowType.BACTERIOLOGY.name();
        query.q = "sample-1";
        query.sort = null;
        query.page = 1;
        query.pageSize = 1;

        MicroWorklistPageForm page = service.getWorklistPage(query);

        assertEquals(1, page.total);
        assertEquals(1, page.page);
        assertEquals(1, page.rows.size());
        assertEquals("case-bac", page.rows.get(0).caseId);
    }

    @Test
    public void allSentinelsPreserveTheUnfilteredCanonicalWorklist() {
        MicroCase bacteriology = microCase("case-bac", "sample-1", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.RECEIVED, "ROUTINE");
        when(caseDAO.getOpenCases()).thenReturn(List.of(bacteriology));
        when(caseDAO.getBySampleItemIds(List.of("sample-1"))).thenReturn(List.of(bacteriology));
        when(isolateDAO.getByCaseIds(List.of("case-bac"))).thenReturn(List.of());
        when(communicationDAO.getByCaseIds(List.of("case-bac"))).thenReturn(List.of());

        MicroWorklistQueryForm query = new MicroWorklistQueryForm();
        query.workflow = "ALL";
        query.stage = "ALL";
        query.urgency = "ALL";
        query.due = "ALL";

        MicroWorklistPageForm page = service.getWorklistPage(query);

        assertEquals(1, page.total);
        assertEquals("case-bac", page.rows.get(0).caseId);
    }

    @Test
    public void worklistSummarizesActionQueuesIndependentlyOfStageAndDueFilters() {
        MicroCase incubating = microCase("case-incubating", "sample-1", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.INCUBATING, "ROUTINE");
        MicroCase growth = microCase("case-growth", "sample-2", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.GROWTH_DETECTED, "ROUTINE");
        MicroCase astReview = microCase("case-ast", "sample-3", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.AST_IN_PROGRESS, "ROUTINE");
        MicroCase readyForReview = microCase("case-ready", "sample-4", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.REVIEW_READY, "ROUTINE");
        MicroIsolate astIsolate = significantIsolate("iso-ast");
        astIsolate.setCaseId("case-ast");
        MicroAstRun astRun = new MicroAstRun();
        astRun.setIsolateId("iso-ast");
        astRun.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        MicroIsolate reviewedIsolate = new MicroIsolate();
        reviewedIsolate.setId("iso-reviewed");
        reviewedIsolate.setCaseId("case-ready");
        reviewedIsolate.setSignificance(MicroIsolateSignificance.NORMAL_FLORA.name());

        when(caseDAO.getOpenCases()).thenReturn(List.of(incubating, growth, astReview, readyForReview));
        when(caseDAO.getBySampleItemIds(List.of("sample-1", "sample-2", "sample-3", "sample-4")))
                .thenReturn(List.of(incubating, growth, astReview, readyForReview));
        when(isolateDAO.getByCaseIds(List.of("case-incubating", "case-growth", "case-ast", "case-ready")))
                .thenReturn(List.of(astIsolate, reviewedIsolate));
        when(astRunDAO.getByIsolateIds(List.of("iso-ast", "iso-reviewed"))).thenReturn(List.of(astRun));
        when(communicationDAO.getByCaseIds(List.of("case-incubating", "case-growth", "case-ast", "case-ready")))
                .thenReturn(List.of());

        MicroWorklistQueryForm query = new MicroWorklistQueryForm();
        query.stage = MicroCaseStage.INCUBATING.name();
        MicroWorklistPageForm page = service.getWorklistPage(query);

        assertEquals(1, page.total);
        assertEquals(1, page.rows.size());
        assertEquals("case-incubating", page.rows.get(0).caseId);
        assertEquals(4, page.summary.totalPending);
        assertEquals(1, page.summary.incubating);
        assertEquals(1, page.summary.growthDetected);
        assertEquals(1, page.summary.needsAstReview);
        assertEquals(1, page.summary.readyForCaseReview);
    }

    private MicroCase microCase(String id, String sampleItemId, MicroWorkflowType workflowType, MicroCaseStage stage,
            String priority) {
        MicroCase microCase = new MicroCase();
        microCase.setId(id);
        microCase.setSampleItemId(sampleItemId);
        microCase.setWorkflowType(workflowType.name());
        microCase.setStage(stage.name());
        microCase.setPriority(priority);
        return microCase;
    }

    private MicroIsolate significantIsolate(String id) {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId(id);
        isolate.setSignificance(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name());
        return isolate;
    }
}
