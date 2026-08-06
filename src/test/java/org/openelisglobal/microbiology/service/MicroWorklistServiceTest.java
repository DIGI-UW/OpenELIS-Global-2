package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
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
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCriticalCommunicationDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroWorklistContextDAO;
import org.openelisglobal.microbiology.form.MicroWorklistActivityContext;
import org.openelisglobal.microbiology.form.MicroWorklistPageForm;
import org.openelisglobal.microbiology.form.MicroWorklistQueryForm;
import org.openelisglobal.microbiology.form.MicroWorklistRowForm;
import org.openelisglobal.microbiology.form.MicroWorklistSpecimenContext;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
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

    @Mock
    private MicroWorklistContextDAO contextDAO;

    @Mock
    private MicroAstPanelDAO panelDAO;

    private MicroWorklistService service;

    @Before
    public void setUp() {
        when(contextDAO.getSpecimenContexts(anyList())).thenReturn(List.of());
        when(contextDAO.getLatestActivityContexts(anyList())).thenReturn(List.of());
        when(panelDAO.getByIds(anyList())).thenReturn(List.of());
        service = new MicroWorklistServiceImpl(caseDAO, isolateDAO, astRunDAO, communicationDAO, contextDAO, panelDAO);
    }

    @Test
    public void enrichesBothGrainsWithBoundedAuthoritativeContext() {
        MicroCase microCase = microCase("case-1", "sample-1", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.REVIEW_READY, "ROUTINE");
        MicroIsolate isolate = significantIsolate("isolate-1");
        isolate.setCaseId("case-1");
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        run.setIsolateId("isolate-1");
        run.setPanelId("panel-1");
        run.setStatus(MicroAstRunStatus.REVIEWED.name());
        MicroAstPanel panel = new MicroAstPanel();
        panel.setId("panel-1");
        panel.setName("Gram negative standard");
        java.sql.Timestamp lastActivityAt = java.sql.Timestamp.valueOf("2026-08-06 09:15:00");
        when(caseDAO.getOpenCases()).thenReturn(List.of(microCase));
        when(caseDAO.getBySampleItemIds(List.of("sample-1"))).thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1"))).thenReturn(List.of(run));
        when(communicationDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of());
        when(contextDAO.getSpecimenContexts(List.of("sample-1"))).thenReturn(
                List.of(new MicroWorklistSpecimenContext("sample-1", "LAB-1001", "Mendez, Olivia", "Blood")));
        when(contextDAO.getLatestActivityContexts(List.of("case-1"))).thenReturn(
                List.of(new MicroWorklistActivityContext("case-1", lastActivityAt, "7", "Olivia", "Mendez")));
        when(panelDAO.getByIds(List.of("panel-1"))).thenReturn(List.of(panel));

        MicroWorklistQueryForm cultureQuery = new MicroWorklistQueryForm();
        cultureQuery.q = "Mendez";
        MicroWorklistRowForm cultureRow = service.getWorklistPage(cultureQuery).rows.get(0);
        MicroWorklistQueryForm astQuery = new MicroWorklistQueryForm();
        astQuery.grain = "ast";
        astQuery.q = "Gram negative";
        MicroWorklistRowForm astRow = service.getWorklistPage(astQuery).rows.get(0);

        assertEquals("LAB-1001", cultureRow.accessionNumber);
        assertEquals("Mendez, Olivia", cultureRow.patientDisplay);
        assertEquals("Blood", cultureRow.specimenDisplay);
        assertEquals(lastActivityAt, cultureRow.lastActivityAt);
        assertEquals("Olivia Mendez", cultureRow.lastActivityBy);
        assertEquals("Gram negative standard", astRow.panelName);
        assertEquals("LAB-1001", astRow.accessionNumber);
        assertEquals("Mendez, Olivia", astRow.patientDisplay);
        verify(contextDAO, org.mockito.Mockito.times(2)).getSpecimenContexts(List.of("sample-1"));
        verify(contextDAO, org.mockito.Mockito.times(2)).getLatestActivityContexts(List.of("case-1"));
        verify(panelDAO, org.mockito.Mockito.times(2)).getByIds(List.of("panel-1"));
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

        List<MicroWorklistRowForm> rows = service.getWorklistPage(new MicroWorklistQueryForm()).rows;

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

        MicroWorklistRowForm row = service.getWorklistPage(new MicroWorklistQueryForm()).rows.get(0);

        assertEquals("HIGH", row.urgency);
        assertTrue(row.hasOpenCriticalCommunication);
    }

    @Test
    public void positiveSignalHasItsOwnSummaryAndSubcultureAction() {
        MicroCase positive = microCase("case-positive", "sample-1", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.POSITIVE_SIGNAL, "STAT");
        when(caseDAO.getOpenCases()).thenReturn(List.of(positive));
        when(caseDAO.getBySampleItemIds(List.of("sample-1"))).thenReturn(List.of(positive));
        when(isolateDAO.getByCaseIds(List.of("case-positive"))).thenReturn(List.of());
        when(communicationDAO.getByCaseIds(List.of("case-positive"))).thenReturn(List.of());

        MicroWorklistPageForm page = service.getWorklistPage(new MicroWorklistQueryForm());

        assertEquals(1, page.summary.positiveSignals);
        assertEquals("SUBCULTURE_GRAM_STAIN", page.rows.get(0).dueAction);
        MicroWorklistQueryForm positiveQuery = new MicroWorklistQueryForm();
        positiveQuery.status = "positive";
        assertEquals(1, service.getWorklistPage(positiveQuery).total);
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

    @Test
    public void astGrainProjectsOneRowPerRunAndFiltersByResultsInStatus() {
        MicroCase microCase = microCase("case-ast", "sample-1", MicroWorkflowType.BACTERIOLOGY,
                MicroCaseStage.AST_IN_PROGRESS, "STAT");
        MicroIsolate isolate = significantIsolate("iso-1");
        isolate.setCaseId("case-ast");
        isolate.setIsolateLabel("Isolate 1");
        isolate.setPreliminaryOrganismText("E. coli");
        MicroAstRun awaiting = astRun("run-awaiting", "iso-1", MicroAstRunStatus.AWAITING_RESULTS);
        MicroAstRun resultsIn = astRun("run-results", "iso-1", MicroAstRunStatus.RESULTS_IN);
        resultsIn.setPanelId("panel-1");

        when(caseDAO.getOpenCases()).thenReturn(List.of(microCase));
        when(caseDAO.getBySampleItemIds(List.of("sample-1"))).thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseIds(List.of("case-ast"))).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateIds(List.of("iso-1"))).thenReturn(List.of(awaiting, resultsIn));
        when(communicationDAO.getByCaseIds(List.of("case-ast"))).thenReturn(List.of());

        MicroWorklistQueryForm query = new MicroWorklistQueryForm();
        query.grain = "ast";
        query.status = "results-in";
        MicroWorklistPageForm page = service.getWorklistPage(query);

        assertEquals(1, page.total);
        assertEquals(2, page.summary.astInQueue);
        assertEquals(1, page.summary.astAwaitingResults);
        assertEquals(1, page.summary.astResultsIn);
        assertEquals("run-results", page.rows.get(0).rowId);
        assertEquals("run-results", page.rows.get(0).astRunId);
        assertEquals("iso-1", page.rows.get(0).isolateId);
        assertEquals("Isolate 1", page.rows.get(0).isolateLabel);
        assertEquals("E. coli", page.rows.get(0).organismDisplay);
        assertEquals(MicroAstRunStatus.RESULTS_IN.name(), page.rows.get(0).astStatus);
    }

    private MicroAstRun astRun(String id, String isolateId, MicroAstRunStatus status) {
        MicroAstRun run = new MicroAstRun();
        run.setId(id);
        run.setIsolateId(isolateId);
        run.setStatus(status.name());
        return run;
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
