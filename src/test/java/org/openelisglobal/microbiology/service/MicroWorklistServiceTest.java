package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
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
        MicroAstRun run = new MicroAstRun();
        run.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        when(caseDAO.getOpenCases()).thenReturn(List.of(setupCase, astCase, siblingCase));
        when(caseDAO.getBySampleItem("sample-1")).thenReturn(List.of(astCase, siblingCase));
        when(caseDAO.getBySampleItem("sample-2")).thenReturn(List.of(setupCase));
        when(isolateDAO.getByCaseId("case-ast")).thenReturn(List.of(isolate));
        when(isolateDAO.getByCaseId("case-setup")).thenReturn(List.of());
        when(isolateDAO.getByCaseId("case-tb")).thenReturn(List.of());
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(run));
        when(communicationDAO.getByCaseId("case-ast")).thenReturn(List.of());
        when(communicationDAO.getByCaseId("case-setup")).thenReturn(List.of());
        when(communicationDAO.getByCaseId("case-tb")).thenReturn(List.of());

        List<MicroWorklistRowForm> rows = service.getWorklistPage(new MicroWorklistQueryForm()).rows;

        assertEquals("case-ast", rows.get(0).caseId);
        assertEquals("AST_REVIEW", rows.get(0).dueAction);
        assertEquals("HIGH", rows.get(0).urgency);
        assertTrue(rows.get(0).siblingWorkflows.contains(MicroWorkflowType.MYCOBACTERIOLOGY_TB.name()));
        assertEquals("SETUP", rows.get(1).dueAction);
    }

    @Test
    public void openCriticalCommunicationRaisesUrgency() {
        MicroCase microCase = microCase("case-1", "sample-1", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.SETUP_RECORDED, "ROUTINE");
        MicroCriticalCommunication communication = new MicroCriticalCommunication();
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.OPEN.name());
        communication.setFollowUpNeeded(true);
        when(caseDAO.getOpenCases()).thenReturn(List.of(microCase));
        when(caseDAO.getBySampleItem("sample-1")).thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of(communication));

        MicroWorklistRowForm row = service.getWorklistPage(new MicroWorklistQueryForm()).rows.get(0);

        assertEquals("HIGH", row.urgency);
        assertTrue(row.hasOpenCriticalCommunication);
    }

    @Test
    public void reviewedAstAdvancesSignificantIsolateToCaseReview() {
        MicroCase microCase = microCase("case-1", "sample-1", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.REVIEW_READY, "ROUTINE");
        MicroIsolate isolate = significantIsolate("iso-1");
        MicroAstRun reviewed = new MicroAstRun();
        reviewed.setStatus(MicroAstRunStatus.REVIEWED.name());
        when(caseDAO.getOpenCases()).thenReturn(List.of(microCase));
        when(caseDAO.getBySampleItem("sample-1")).thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(reviewed));
        when(communicationDAO.getByCaseId("case-1")).thenReturn(List.of());

        MicroWorklistRowForm row = service.getWorklistPage(new MicroWorklistQueryForm()).rows.get(0);

        assertEquals("CASE_REVIEW", row.dueAction);
    }

    @Test
    public void worklistFiltersSearchesAndPaginatesOnTheServer() {
        MicroCase bacteriology = microCase("case-bac", "sample-1", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.RECEIVED, "ROUTINE");
        MicroCase tb = microCase("case-tb", "sample-2", MicroWorkflowType.MYCOBACTERIOLOGY_TB, MicroCaseStage.RECEIVED,
                "ROUTINE");
        when(caseDAO.getOpenCases()).thenReturn(List.of(bacteriology, tb));
        when(caseDAO.getBySampleItem("sample-1")).thenReturn(List.of(bacteriology));
        when(caseDAO.getBySampleItem("sample-2")).thenReturn(List.of(tb));
        when(isolateDAO.getByCaseId("case-bac")).thenReturn(List.of());
        when(isolateDAO.getByCaseId("case-tb")).thenReturn(List.of());
        when(communicationDAO.getByCaseId("case-bac")).thenReturn(List.of());
        when(communicationDAO.getByCaseId("case-tb")).thenReturn(List.of());

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
        MicroAstRun astRun = new MicroAstRun();
        astRun.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        MicroIsolate reviewedIsolate = new MicroIsolate();
        reviewedIsolate.setId("iso-reviewed");
        reviewedIsolate.setSignificance(MicroIsolateSignificance.NORMAL_FLORA.name());

        when(caseDAO.getOpenCases()).thenReturn(List.of(incubating, growth, astReview, readyForReview));
        for (MicroCase microCase : List.of(incubating, growth, astReview, readyForReview)) {
            when(caseDAO.getBySampleItem(microCase.getSampleItemId())).thenReturn(List.of(microCase));
        }
        when(isolateDAO.getByCaseId("case-incubating")).thenReturn(List.of());
        when(isolateDAO.getByCaseId("case-growth")).thenReturn(List.of());
        when(isolateDAO.getByCaseId("case-ast")).thenReturn(List.of(astIsolate));
        when(isolateDAO.getByCaseId("case-ready")).thenReturn(List.of(reviewedIsolate));
        when(astRunDAO.getByIsolateId("iso-ast")).thenReturn(List.of(astRun));
        when(astRunDAO.getByIsolateId("iso-reviewed")).thenReturn(List.of());
        when(communicationDAO.getByCaseId("case-incubating")).thenReturn(List.of());
        when(communicationDAO.getByCaseId("case-growth")).thenReturn(List.of());
        when(communicationDAO.getByCaseId("case-ast")).thenReturn(List.of());
        when(communicationDAO.getByCaseId("case-ready")).thenReturn(List.of());

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
