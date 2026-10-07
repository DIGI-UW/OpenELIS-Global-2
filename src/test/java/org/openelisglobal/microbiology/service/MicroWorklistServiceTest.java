package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO;
import org.openelisglobal.microbiology.dao.MicroCaseSpecimenDAO;
import org.openelisglobal.microbiology.dao.MicroCriticalCommunicationDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.dao.MicroPatientOriginDAO;
import org.openelisglobal.microbiology.dao.MicroReviewedAstWorklistQuery;
import org.openelisglobal.microbiology.dao.MicroReviewedAstWorklistRow;
import org.openelisglobal.microbiology.dao.MicroWorklistContextDAO;
import org.openelisglobal.microbiology.form.MicroWorklistActivityContext;
import org.openelisglobal.microbiology.form.MicroWorklistPageForm;
import org.openelisglobal.microbiology.form.MicroWorklistQueryForm;
import org.openelisglobal.microbiology.form.MicroWorklistRecentActivityContext;
import org.openelisglobal.microbiology.form.MicroWorklistRowForm;
import org.openelisglobal.microbiology.form.MicroWorklistSpecimenContext;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.microbiology.valueholder.MicroCaseSpecimen;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroPatientOrigin;

@RunWith(MockitoJUnitRunner.class)
public class MicroWorklistServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseOrderDetailDAO caseOrderDetailDAO;

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

    @Mock
    private MicroPatientOriginDAO patientOriginDAO;

    @Mock
    private MicroOrganismDAO organismDAO;

    @Mock
    private MicroCaseSpecimenDAO specimenDAO;
    @Mock
    private MicrobiologyCaseAccessService accessService;
    private final java.util.Map<String, MicroCaseSpecimen> membersByCase = new java.util.LinkedHashMap<>();
    @Mock
    private org.openelisglobal.test.service.TestSectionService testSectionService;
    private MicroWorklistService service;

    @Before
    public void setUp() {
        when(contextDAO.getSpecimenContexts(anyList())).thenReturn(List.of());
        when(contextDAO.getLatestActivityContexts(anyList())).thenReturn(List.of());
        when(contextDAO.getRecentActivityContexts(anyList(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of());
        when(panelDAO.getByIds(anyList())).thenReturn(List.of());
        when(caseOrderDetailDAO.getByCaseIds(anyList())).thenReturn(List.of());
        when(accessService.getWorklistAccess("7")).thenReturn(new MicrobiologyWorklistAccess(true, java.util.Set.of()));
        when(specimenDAO.getByCaseIds(anyList())).thenAnswer(invocation ->
                invocation.<List<String>>getArgument(0).stream().map(membersByCase::get)
                        .filter(java.util.Objects::nonNull).toList());
        service = new MicroWorklistServiceImpl(caseDAO, caseOrderDetailDAO, isolateDAO, astRunDAO, communicationDAO,
                contextDAO, panelDAO, patientOriginDAO, organismDAO, specimenDAO, accessService, testSectionService);
    }

    @Test
    public void labUnitOptionsRespectAccessEvenWhenNoRowsMatch() {
        var allowed = new org.openelisglobal.test.valueholder.TestSection();
        allowed.setId("unit-1");
        allowed.setTestSectionName("Bacteriology bench");
        var denied = new org.openelisglobal.test.valueholder.TestSection();
        denied.setId("unit-2");
        denied.setTestSectionName("Other bench");
        when(testSectionService.getAllTestSections()).thenReturn(List.of(allowed, denied));
        when(accessService.getWorklistAccess("7"))
                .thenReturn(new MicrobiologyWorklistAccess(false, java.util.Set.of("unit-1")));
        MicroWorklistQueryForm query = new MicroWorklistQueryForm();
        var page = service.getWorklistPage(query, "7");
        assertEquals(1, page.labUnits.size());
        assertEquals("unit-1", page.labUnits.get(0).id);
        assertEquals("Bacteriology bench", page.labUnits.get(0).label);
        assertEquals(0, page.total);
        query.grain = "ast";
        query.status = "reviewed";
        var reviewed = service.getWorklistPage(query, "7");
        assertEquals(1, reviewed.labUnits.size());
        assertEquals("unit-1", reviewed.labUnits.get(0).id);
    }

    @Test
    public void enrichesBothGrainsWithBoundedAuthoritativeContext() {
        MicroCase microCase = microCase("case-1", "sample-1", "unit-1", MicroCaseStage.REVIEW_READY, "ROUTINE");
        MicroIsolate isolate = significantIsolate("isolate-1");
        isolate.setCaseId("case-1");
        isolate.setSourceSampleItemId(membersByCase.get("case-1").getSampleItemId());
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        run.setIsolateId("isolate-1");
        run.setPanelId("panel-1");
        run.setStatus(MicroAstRunStatus.REVIEWED.name());
        run.setAnalyzerExpertFlags("ESBL|MDR");
        run.setAnalyzerCompletedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        MicroAstPanel panel = new MicroAstPanel();
        panel.setId("panel-1");
        panel.setName("Gram negative standard");
        java.sql.Timestamp lastActivityAt = java.sql.Timestamp.valueOf("2026-08-06 09:15:00");
        when(caseDAO.getOpenCases(true, java.util.Set.of())).thenReturn(List.of(microCase));
        when(astRunDAO.getReviewedWorklistPage(any(MicroReviewedAstWorklistQuery.class)))
                .thenReturn(List.of(new MicroReviewedAstWorklistRow(microCase, isolate, run)));
        when(astRunDAO.countReviewedWorklist(any(MicroReviewedAstWorklistQuery.class))).thenReturn(1L);
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1"))).thenReturn(List.of(run));
        when(communicationDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of());
        when(contextDAO.getSpecimenContexts(List.of("sample-1"))).thenReturn(List.of(
                new MicroWorklistSpecimenContext("sample-1", "LAB-1001", "Mendez, Olivia", "Blood", null, "blood")));
        when(contextDAO.getLatestActivityContexts(List.of("case-1"))).thenReturn(
                List.of(new MicroWorklistActivityContext("case-1", lastActivityAt, "7", "Olivia", "Mendez")));
        when(contextDAO.getRecentActivityContexts(List.of("case-1"), 25))
                .thenReturn(List.of(new MicroWorklistRecentActivityContext("case-1", lastActivityAt, "7", "Olivia",
                        "Mendez", "AST_REVIEWED", "AST run reviewed")));
        when(panelDAO.getByIds(List.of("panel-1"))).thenReturn(List.of(panel));

        MicroWorklistQueryForm cultureQuery = new MicroWorklistQueryForm();
        cultureQuery.q = "Mendez";
        MicroWorklistPageForm culturePage = service.getWorklistPage(cultureQuery, "7");
        MicroWorklistRowForm cultureRow = culturePage.rows.get(0);
        MicroWorklistQueryForm astQuery = new MicroWorklistQueryForm();
        astQuery.grain = "ast";
        astQuery.status = "reviewed";
        astQuery.q = "Gram negative";
        MicroWorklistRowForm astRow = service.getWorklistPage(astQuery, "7").rows.get(0);

        assertEquals("LAB-1001", cultureRow.accessionNumber);
        assertEquals("Mendez, Olivia", cultureRow.patientDisplay);
        assertEquals("Blood", cultureRow.specimenDisplay);
        assertEquals(lastActivityAt, cultureRow.lastActivityAt);
        assertEquals("Olivia Mendez", cultureRow.lastActivityBy);
        assertEquals("Gram negative standard", astRow.panelName);
        assertEquals("LAB-1001", astRow.accessionNumber);
        assertEquals("Mendez, Olivia", astRow.patientDisplay);
        assertEquals(1, culturePage.summary.resistanceHits.get("ESBL").intValue());
        assertEquals(1, culturePage.summary.resistanceHits.get("MDR").intValue());
        assertEquals(1, culturePage.recentActivity.size());
        assertEquals("LAB-1001", culturePage.recentActivity.get(0).accessionNumber);
        assertEquals("Olivia Mendez", culturePage.recentActivity.get(0).performedByDisplay);
        verify(contextDAO, org.mockito.Mockito.times(2)).getSpecimenContexts(List.of("sample-1"));
        verify(contextDAO, org.mockito.Mockito.times(2)).getLatestActivityContexts(List.of("case-1"));
        verify(contextDAO, org.mockito.Mockito.times(2)).getRecentActivityContexts(List.of("case-1"), 25);
        verify(panelDAO, org.mockito.Mockito.times(2)).getByIds(List.of("panel-1"));
    }

    @Test
    public void worklistPrioritizesAstReviewBeforeSetupAndShowsSiblings() {
        MicroCase astCase = microCase("case-ast", "sample-1", "unit-1", MicroCaseStage.SETUP_RECORDED, "ROUTINE");
        MicroCase setupCase = microCase("case-setup", "sample-2", "unit-1", MicroCaseStage.RECEIVED, "ROUTINE");
        MicroCase siblingCase = microCase("case-tb", "sample-1", "unit-2", MicroCaseStage.RECEIVED, "ROUTINE");
        MicroIsolate isolate = significantIsolate("iso-1");
        isolate.setCaseId("case-ast");
        isolate.setSourceSampleItemId(membersByCase.get("case-ast").getSampleItemId());
        MicroAstRun run = new MicroAstRun();
        run.setIsolateId("iso-1");
        run.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        when(caseDAO.getOpenCases(true, java.util.Set.of())).thenReturn(List.of(setupCase, astCase, siblingCase));
        when(isolateDAO.getByCaseIds(List.of("case-setup", "case-ast", "case-tb"))).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateIds(List.of("iso-1"))).thenReturn(List.of(run));
        when(communicationDAO.getByCaseIds(List.of("case-setup", "case-ast", "case-tb"))).thenReturn(List.of());

        List<MicroWorklistRowForm> rows = service.getWorklistPage(new MicroWorklistQueryForm(), "7").rows;

        assertEquals("case-ast", rows.get(0).caseId);
        assertEquals("AST_REVIEW", rows.get(0).dueAction);
        assertEquals("HIGH", rows.get(0).urgency);
        assertTrue(rows.get(0).relatedCaseIds.contains("case-tb"));
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
        MicroCase microCase = microCase("case-1", "sample-1", "unit-1", MicroCaseStage.SETUP_RECORDED, "ROUTINE");
        MicroCriticalCommunication communication = new MicroCriticalCommunication();
        communication.setCaseId("case-1");
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.OPEN.name());
        communication.setFollowUpNeeded(true);
        when(caseDAO.getOpenCases(true, java.util.Set.of())).thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of());
        when(communicationDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(communication));

        MicroWorklistRowForm row = service.getWorklistPage(new MicroWorklistQueryForm(), "7").rows.get(0);

        assertEquals("HIGH", row.urgency);
        assertTrue(row.hasOpenCriticalCommunication);
    }

    @Test
    public void positiveSignalHasItsOwnSummaryAndSubcultureAction() {
        MicroCase positive = microCase("case-positive", "sample-1", "unit-1", MicroCaseStage.POSITIVE_SIGNAL, "STAT");
        when(caseDAO.getOpenCases(true, java.util.Set.of())).thenReturn(List.of(positive));
        when(isolateDAO.getByCaseIds(List.of("case-positive"))).thenReturn(List.of());
        when(communicationDAO.getByCaseIds(List.of("case-positive"))).thenReturn(List.of());

        MicroWorklistPageForm page = service.getWorklistPage(new MicroWorklistQueryForm(), "7");

        assertEquals(1, page.summary.positiveSignals);
        assertEquals("SUBCULTURE_GRAM_STAIN", page.rows.get(0).dueAction);
        MicroWorklistQueryForm positiveQuery = new MicroWorklistQueryForm();
        positiveQuery.status = "positive";
        assertEquals(1, service.getWorklistPage(positiveQuery, "7").total);
    }

    @Test
    public void incubatingCaseUsesStageFallbackWhenTimingIsUnavailable() {
        MicroCase incubating = microCase("case-1", "sample-1", "unit-1", MicroCaseStage.INCUBATING, "ROUTINE");
        when(caseDAO.getOpenCases(true, java.util.Set.of())).thenReturn(List.of(incubating));
        when(isolateDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of());
        when(communicationDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of());

        MicroWorklistRowForm row = service.getWorklistPage(new MicroWorklistQueryForm(), "7").rows.get(0);

        assertEquals("INCUBATING", row.dueAction);
    }

    @Test
    public void worklistFiltersSearchesAndPaginatesOnTheServer() {
        MicroCase bacteriology = microCase("case-bac", "sample-1", "unit-1", MicroCaseStage.RECEIVED, "ROUTINE");
        MicroCase tb = microCase("case-tb", "sample-2", "unit-2", MicroCaseStage.RECEIVED, "ROUTINE");
        when(caseDAO.getOpenCases(true, java.util.Set.of())).thenReturn(List.of(bacteriology, tb));
        when(isolateDAO.getByCaseIds(List.of("case-bac", "case-tb"))).thenReturn(List.of());
        when(communicationDAO.getByCaseIds(List.of("case-bac", "case-tb"))).thenReturn(List.of());

        MicroWorklistQueryForm query = new MicroWorklistQueryForm();
        query.testSectionId = "unit-1";
        query.q = "sample-1";
        query.sort = null;
        query.page = 1;
        query.pageSize = 1;

        MicroWorklistPageForm page = service.getWorklistPage(query, "7");

        assertEquals(1, page.total);
        assertEquals(1, page.page);
        assertEquals(1, page.rows.size());
        assertEquals("case-bac", page.rows.get(0).caseId);
    }

    @Test
    public void allSentinelsPreserveTheUnfilteredCanonicalWorklist() {
        MicroCase bacteriology = microCase("case-bac", "sample-1", "unit-1", MicroCaseStage.RECEIVED, "ROUTINE");
        when(caseDAO.getOpenCases(true, java.util.Set.of())).thenReturn(List.of(bacteriology));
        when(isolateDAO.getByCaseIds(List.of("case-bac"))).thenReturn(List.of());
        when(communicationDAO.getByCaseIds(List.of("case-bac"))).thenReturn(List.of());

        MicroWorklistQueryForm query = new MicroWorklistQueryForm();
        query.testSectionId = "ALL";
        query.stage = "ALL";
        query.urgency = "ALL";
        query.due = "ALL";

        MicroWorklistPageForm page = service.getWorklistPage(query, "7");

        assertEquals(1, page.total);
        assertEquals("case-bac", page.rows.get(0).caseId);
    }

    @Test
    public void worklistSummarizesActionQueuesIndependentlyOfStageAndDueFilters() {
        MicroCase incubating = microCase("case-incubating", "sample-1", "unit-1", MicroCaseStage.INCUBATING, "ROUTINE");
        MicroCase growth = microCase("case-growth", "sample-2", "unit-1", MicroCaseStage.GROWTH_DETECTED, "ROUTINE");
        MicroCase astReview = microCase("case-ast", "sample-3", "unit-1", MicroCaseStage.AST_IN_PROGRESS, "ROUTINE");
        MicroCase readyForReview = microCase("case-ready", "sample-4", "unit-1", MicroCaseStage.REVIEW_READY,
                "ROUTINE");
        MicroIsolate astIsolate = significantIsolate("iso-ast");
        astIsolate.setCaseId("case-ast");
        astIsolate.setSourceSampleItemId(membersByCase.get("case-ast").getSampleItemId());
        MicroAstRun astRun = new MicroAstRun();
        astRun.setIsolateId("iso-ast");
        astRun.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        MicroIsolate reviewedIsolate = new MicroIsolate();
        reviewedIsolate.setId("iso-reviewed");
        reviewedIsolate.setCaseId("case-ready");
        reviewedIsolate.setSourceSampleItemId(membersByCase.get("case-ready").getSampleItemId());
        reviewedIsolate.setSignificance(MicroIsolateSignificance.NORMAL_FLORA.name());

        when(caseDAO.getOpenCases(true, java.util.Set.of()))
                .thenReturn(List.of(incubating, growth, astReview, readyForReview));
        when(isolateDAO.getByCaseIds(List.of("case-incubating", "case-growth", "case-ast", "case-ready")))
                .thenReturn(List.of(astIsolate, reviewedIsolate));
        when(astRunDAO.getByIsolateIds(List.of("iso-ast", "iso-reviewed"))).thenReturn(List.of(astRun));
        when(communicationDAO.getByCaseIds(List.of("case-incubating", "case-growth", "case-ast", "case-ready")))
                .thenReturn(List.of());

        MicroWorklistQueryForm query = new MicroWorklistQueryForm();
        query.stage = MicroCaseStage.INCUBATING.name();
        MicroWorklistPageForm page = service.getWorklistPage(query, "7");

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
        MicroCase microCase = microCase("case-ast", "sample-1", "unit-1", MicroCaseStage.AST_IN_PROGRESS, "STAT");
        MicroIsolate isolate = significantIsolate("iso-1");
        isolate.setCaseId("case-ast");
        isolate.setSourceSampleItemId(membersByCase.get("case-ast").getSampleItemId());
        isolate.setIsolateLabel("Isolate 1");
        isolate.setPreliminaryOrganismText("E. coli");
        MicroAstRun awaiting = astRun("run-awaiting", "iso-1", MicroAstRunStatus.AWAITING_RESULTS);
        MicroAstRun resultsIn = astRun("run-results", "iso-1", MicroAstRunStatus.RESULTS_IN);
        resultsIn.setPanelId("panel-1");

        when(caseDAO.getOpenCases(true, java.util.Set.of())).thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseIds(List.of("case-ast"))).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateIds(List.of("iso-1"))).thenReturn(List.of(awaiting, resultsIn));
        when(communicationDAO.getByCaseIds(List.of("case-ast"))).thenReturn(List.of());

        MicroWorklistQueryForm query = new MicroWorklistQueryForm();
        query.grain = "ast";
        query.status = "results-in";
        MicroWorklistPageForm page = service.getWorklistPage(query, "7");

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

    @Test
    public void significantIsolateWithOnlyReviewedRunsAdvancesToCaseReview() {
        MicroCase microCase = microCase("case-ast", "sample-1", "unit-1", MicroCaseStage.REVIEW_READY, "ROUTINE");
        MicroIsolate isolate = significantIsolate("iso-1");
        isolate.setCaseId("case-ast");
        isolate.setSourceSampleItemId(membersByCase.get("case-ast").getSampleItemId());
        MicroAstRun reviewed = astRun("run-reviewed", "iso-1", MicroAstRunStatus.REVIEWED);
        when(caseDAO.getOpenCases(true, java.util.Set.of())).thenReturn(List.of(microCase));
        when(isolateDAO.getByCaseIds(List.of("case-ast"))).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateIds(List.of("iso-1"))).thenReturn(List.of(reviewed));
        when(communicationDAO.getByCaseIds(List.of("case-ast"))).thenReturn(List.of());

        List<MicroWorklistRowForm> rows = service.getWorklistPage(new MicroWorklistQueryForm(), "7").rows;

        assertEquals("case-ast", rows.get(0).caseId);
        assertEquals("CASE_REVIEW", rows.get(0).dueAction);
    }

    @Test
    public void reviewedRunsLeaveDefaultActionQueueAndRemainInReviewedView() {
        MicroCase microCase = microCase("case-ast", "sample-1", "unit-1", MicroCaseStage.REVIEW_READY, "ROUTINE");
        MicroIsolate isolate = significantIsolate("iso-1");
        isolate.setCaseId("case-ast");
        isolate.setSourceSampleItemId(membersByCase.get("case-ast").getSampleItemId());
        MicroAstRun inProgress = astRun("run-active", "iso-1", MicroAstRunStatus.IN_PROGRESS);
        MicroAstRun reviewed = astRun("run-reviewed", "iso-1", MicroAstRunStatus.REVIEWED);

        when(caseDAO.getOpenCases(true, java.util.Set.of())).thenReturn(List.of(microCase));
        when(astRunDAO.getReviewedWorklistPage(any(MicroReviewedAstWorklistQuery.class)))
                .thenReturn(List.of(new MicroReviewedAstWorklistRow(microCase, isolate, reviewed)));
        when(astRunDAO.countReviewedWorklist(any(MicroReviewedAstWorklistQuery.class))).thenReturn(1L);
        when(isolateDAO.getByCaseIds(List.of("case-ast"))).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateIds(List.of("iso-1"))).thenReturn(List.of(inProgress, reviewed));
        when(communicationDAO.getByCaseIds(List.of("case-ast"))).thenReturn(List.of());

        MicroWorklistQueryForm activeQuery = new MicroWorklistQueryForm();
        activeQuery.grain = "ast";
        MicroWorklistPageForm activePage = service.getWorklistPage(activeQuery, "7");
        MicroWorklistQueryForm reviewedQuery = new MicroWorklistQueryForm();
        reviewedQuery.grain = "ast";
        reviewedQuery.status = "reviewed";
        MicroWorklistPageForm reviewedPage = service.getWorklistPage(reviewedQuery, "7");

        assertEquals(1, activePage.total);
        assertEquals("run-active", activePage.rows.get(0).astRunId);
        assertEquals(1, activePage.summary.astInQueue);
        assertEquals(1, reviewedPage.total);
        assertEquals("run-reviewed", reviewedPage.rows.get(0).astRunId);
        assertEquals(MicroAstRunStatus.REVIEWED.name(), reviewedPage.rows.get(0).astStatus);
        assertEquals("VIEW", reviewedPage.rows.get(0).dueAction);
    }

    @Test
    public void reviewedViewOffersTheSurveillanceFiltersItsRowsSupport() {
        MicroCase microCase = microCase("case-ast", "sample-1", "unit-1", MicroCaseStage.REVIEW_READY, "ROUTINE");
        MicroIsolate isolate = significantIsolate("iso-1");
        isolate.setCaseId("case-ast");
        isolate.setSourceSampleItemId(membersByCase.get("case-ast").getSampleItemId());
        isolate.setOrganismId("org-1");
        MicroAstRun reviewed = astRun("run-reviewed", "iso-1", MicroAstRunStatus.REVIEWED);
        MicroOrganism organism = new MicroOrganism();
        organism.setId("org-1");
        organism.setDisplayName("Escherichia coli (UAT)");
        when(astRunDAO.getReviewedWorklistPage(any(MicroReviewedAstWorklistQuery.class)))
                .thenReturn(List.of(new MicroReviewedAstWorklistRow(microCase, isolate, reviewed)));
        when(astRunDAO.countReviewedWorklist(any(MicroReviewedAstWorklistQuery.class))).thenReturn(1L);
        when(contextDAO.getSpecimenContexts(List.of("sample-1"))).thenReturn(List
                .of(new MicroWorklistSpecimenContext("sample-1", "LAB-1001", "Mendez, Olivia", "Blood", null, "7")));
        when(organismDAO.getByIds(List.of("org-1"))).thenReturn(List.of(organism));

        MicroWorklistQueryForm reviewedQuery = new MicroWorklistQueryForm();
        reviewedQuery.grain = "ast";
        reviewedQuery.status = "reviewed";
        MicroWorklistPageForm page = service.getWorklistPage(reviewedQuery, "7");

        assertEquals("Escherichia coli (UAT)", page.rows.get(0).organismDisplay);
        assertEquals(1, page.filterOptions.specimenTypes.size());
        assertEquals("Blood", page.filterOptions.specimenTypes.get(0).label);
        assertEquals(1, page.filterOptions.organisms.size());
        assertEquals("Escherichia coli (UAT)", page.filterOptions.organisms.get(0).label);
    }

    @Test
    public void reviewedViewIncludesReviewedRunsFromReleasedCases() {
        MicroCase releasedCase = microCase("case-released", "sample-1", "unit-1", MicroCaseStage.REVIEW_READY,
                "ROUTINE");
        releasedCase.setClosedAt(Timestamp.valueOf("2026-08-18 10:00:00"));
        MicroIsolate isolate = significantIsolate("iso-1");
        isolate.setCaseId("case-released");
        isolate.setSourceSampleItemId(membersByCase.get("case-released").getSampleItemId());
        MicroAstRun reviewed = astRun("run-reviewed", "iso-1", MicroAstRunStatus.REVIEWED);

        when(astRunDAO.getReviewedWorklistPage(any(MicroReviewedAstWorklistQuery.class)))
                .thenReturn(List.of(new MicroReviewedAstWorklistRow(releasedCase, isolate, reviewed)));
        when(astRunDAO.countReviewedWorklist(any(MicroReviewedAstWorklistQuery.class))).thenReturn(1L);

        MicroWorklistQueryForm reviewedQuery = new MicroWorklistQueryForm();
        reviewedQuery.grain = "ast";
        reviewedQuery.status = "reviewed";

        MicroWorklistPageForm reviewedPage = service.getWorklistPage(reviewedQuery, "7");

        assertEquals(1, reviewedPage.total);
        assertEquals("case-released", reviewedPage.rows.get(0).caseId);
        assertEquals("run-reviewed", reviewedPage.rows.get(0).astRunId);
        assertEquals("VIEW", reviewedPage.rows.get(0).dueAction);
        verify(caseDAO, never()).getOpenCases(true, java.util.Set.of());
    }

    @Test
    public void reviewedHistoryIsFilteredAndPagedBeforeRelatedContextLoads() {
        MicroCase releasedCase = microCase("case-released", "sample-1", "unit-1", MicroCaseStage.REVIEW_READY, "STAT");
        releasedCase.setClosedAt(Timestamp.valueOf("2026-08-18 10:00:00"));
        MicroIsolate isolate = significantIsolate("iso-1");
        isolate.setCaseId("case-released");
        isolate.setSourceSampleItemId(membersByCase.get("case-released").getSampleItemId());
        MicroAstRun reviewed = astRun("run-reviewed", "iso-1", MicroAstRunStatus.REVIEWED);

        when(astRunDAO.getReviewedWorklistPage(any(MicroReviewedAstWorklistQuery.class)))
                .thenReturn(List.of(new MicroReviewedAstWorklistRow(releasedCase, isolate, reviewed)));
        when(astRunDAO.countReviewedWorklist(any(MicroReviewedAstWorklistQuery.class))).thenReturn(245L);

        MicroWorklistQueryForm query = new MicroWorklistQueryForm();
        query.grain = "ast";
        query.status = "reviewed";
        query.testSectionId = "unit-1";
        query.urgency = "HIGH";
        query.q = "LAB-1001";
        query.sort = "newest";
        query.page = 3;
        query.pageSize = 10;

        MicroWorklistPageForm page = service.getWorklistPage(query, "7");

        assertEquals(245, page.total);
        assertEquals(1, page.rows.size());
        verify(astRunDAO).getReviewedWorklistPage(org.mockito.ArgumentMatchers
                .argThat(reviewedQuery -> reviewedQuery.offset() == 20 && reviewedQuery.limit() == 10
                        && "unit-1".equals(reviewedQuery.testSectionId()) && "HIGH".equals(reviewedQuery.urgency())
                        && "LAB-1001".equals(reviewedQuery.search()) && "newest".equals(reviewedQuery.sort())));
        verify(caseOrderDetailDAO).getByCaseIds(List.of("case-released"));
        verify(caseDAO, never()).getOpenCases(true, java.util.Set.of());
        verify(isolateDAO, never()).getByCaseIds(anyList());
        verify(astRunDAO, never()).getByIsolateIds(anyList());
    }

    @Test
    public void astWorklistAppliesStructuredSurveillanceFiltersAndReturnsReusableOptions() {
        MicroCase includedCase = microCase("case-included", "sample-1", "unit-1", MicroCaseStage.AST_IN_PROGRESS,
                "ROUTINE");
        MicroCase excludedCase = microCase("case-excluded", "sample-2", "unit-1", MicroCaseStage.AST_IN_PROGRESS,
                "ROUTINE");
        MicroIsolate included = significantIsolate("isolate-1");
        included.setCaseId("case-included");
        included.setSourceSampleItemId(membersByCase.get("case-included").getSampleItemId());
        included.setOrganismId("organism-1");
        included.setPreliminaryOrganismText("E. coli");
        MicroIsolate excluded = significantIsolate("isolate-2");
        excluded.setCaseId("case-excluded");
        excluded.setSourceSampleItemId(membersByCase.get("case-excluded").getSampleItemId());
        excluded.setOrganismId("organism-2");
        excluded.setPreliminaryOrganismText("S. aureus");
        excluded.setSignificance(MicroIsolateSignificance.CONTAMINANT.name());
        MicroAstRun includedRun = astRun("run-1", "isolate-1", MicroAstRunStatus.RESULTS_IN);
        MicroAstRun excludedRun = astRun("run-2", "isolate-2", MicroAstRunStatus.RESULTS_IN);
        MicroCaseOrderDetail includedDetail = orderDetail("case-included", "INPATIENT");
        MicroCaseOrderDetail excludedDetail = orderDetail("case-excluded", "OUTPATIENT");

        when(caseDAO.getOpenCases(true, java.util.Set.of())).thenReturn(List.of(includedCase, excludedCase));
        when(isolateDAO.getByCaseIds(List.of("case-included", "case-excluded")))
                .thenReturn(List.of(included, excluded));
        when(astRunDAO.getByIsolateIds(List.of("isolate-1", "isolate-2")))
                .thenReturn(List.of(includedRun, excludedRun));
        when(communicationDAO.getByCaseIds(List.of("case-included", "case-excluded"))).thenReturn(List.of());
        when(contextDAO.getSpecimenContexts(List.of("sample-1", "sample-2"))).thenReturn(List.of(
                new MicroWorklistSpecimenContext("sample-1", "LAB-001", "Ada Lovelace", "Blood",
                        java.sql.Timestamp.valueOf("2026-07-12 09:00:00"), "blood"),
                new MicroWorklistSpecimenContext("sample-2", "LAB-002", "Grace Hopper", "Urine",
                        java.sql.Timestamp.valueOf("2026-06-30 09:00:00"), "urine")));
        when(caseOrderDetailDAO.getByCaseIds(List.of("case-included", "case-excluded")))
                .thenReturn(List.of(includedDetail, excludedDetail));
        when(patientOriginDAO.getByCodes(List.of("INPATIENT", "OUTPATIENT"))).thenReturn(
                List.of(patientOrigin("INPATIENT", "Inpatient"), patientOrigin("OUTPATIENT", "Outpatient")));

        MicroWorklistQueryForm query = new MicroWorklistQueryForm();
        query.grain = "ast";
        query.from = "2026-07-01";
        query.to = "2026-07-31";
        query.specimen = List.of("blood");
        query.origin = List.of("INPATIENT");
        query.organism = List.of("organism-1");
        query.significance = List.of(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name());

        MicroWorklistPageForm page = service.getWorklistPage(query, "7");

        assertEquals(1, page.total);
        assertEquals("run-1", page.rows.get(0).astRunId);
        assertEquals("blood", page.rows.get(0).specimenTypeId);
        assertEquals("INPATIENT", page.rows.get(0).patientOrigin);
        assertEquals("organism-1", page.rows.get(0).organismId);
        assertEquals(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name(), page.rows.get(0).isolateSignificance);
        assertEquals(List.of("blood", "urine"),
                page.filterOptions.specimenTypes.stream().map(option -> option.id).toList());
        assertEquals(List.of("organism-1", "organism-2"),
                page.filterOptions.organisms.stream().map(option -> option.id).toList());
        assertEquals(List.of("Inpatient", "Outpatient"),
                page.filterOptions.patientOrigins.stream().map(option -> option.label).toList());
    }

    @Test
    public void groupedCaseKeepsOneDistinctCultureRowPerMemberSpecimen() {
        MicroCase microCase = microCase("case-1", "item-1", "unit-1", MicroCaseStage.INCUBATING, "ROUTINE");
        MicroCaseSpecimen second = new MicroCaseSpecimen();
        second.setCaseId("case-1");
        second.setSampleItemId("item-2");
        when(caseDAO.getOpenCases(true, java.util.Set.of())).thenReturn(List.of(microCase));
        when(specimenDAO.getByCaseIds(List.of("case-1"))).thenReturn(List.of(membersByCase.get("case-1"), second));

        MicroWorklistPageForm page = service.getWorklistPage(new MicroWorklistQueryForm(), "7");

        assertEquals(2, page.total);
        assertEquals(java.util.Set.of("item-1", "item-2"),
                page.rows.stream().map(row -> row.sampleItemId).collect(java.util.stream.Collectors.toSet()));
        assertEquals(java.util.Set.of("case-1:item-1", "case-1:item-2"),
                page.rows.stream().map(row -> row.rowId).collect(java.util.stream.Collectors.toSet()));
        assertTrue(page.rows.stream().allMatch(row -> "case-1".equals(row.caseId)));
        verify(contextDAO).getSpecimenContexts(List.of("item-1", "item-2"));
    }

    @Test
    public void unitRestrictedWorklistPassesOnlyTheActorsPermittedUnitsToTheQuery() {
        when(accessService.getWorklistAccess("7"))
                .thenReturn(new MicrobiologyWorklistAccess(false, java.util.Set.of("unit-2")));
        MicroCase microCase = microCase("case-2", "item-2", "unit-2", MicroCaseStage.INCUBATING, "ROUTINE");
        when(caseDAO.getOpenCases(false, java.util.Set.of("unit-2"))).thenReturn(List.of(microCase));

        MicroWorklistPageForm page = service.getWorklistPage(new MicroWorklistQueryForm(), "7");

        assertEquals(1, page.total);
        assertEquals("unit-2", page.rows.get(0).testSectionId);
        assertEquals(1, page.summary.totalPending);
        verify(caseDAO).getOpenCases(false, java.util.Set.of("unit-2"));
        verify(caseDAO, never()).getOpenCases(true, java.util.Set.of());
        verify(contextDAO).getLatestActivityContexts(List.of("case-2"));
    }

    private MicroAstRun astRun(String id, String isolateId, MicroAstRunStatus status) {
        MicroAstRun run = new MicroAstRun();
        run.setId(id);
        run.setIsolateId(isolateId);
        run.setStatus(status.name());
        return run;
    }

    private MicroCase microCase(String id, String sampleItemId, String unitId, MicroCaseStage stage, String priority) {
        MicroCase microCase = new MicroCase();
        microCase.setId(id);
        microCase.setSampleId(sampleItemId);
        microCase.setTestSectionId(unitId);
        MicroCaseSpecimen member = new MicroCaseSpecimen();
        member.setCaseId(id);
        member.setSampleItemId(sampleItemId);
        membersByCase.put(id, member);
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

    private MicroCaseOrderDetail orderDetail(String caseId, String patientOrigin) {
        MicroCaseOrderDetail detail = new MicroCaseOrderDetail();
        detail.setCaseId(caseId);
        detail.setPatientOrigin(patientOrigin);
        return detail;
    }

    private MicroPatientOrigin patientOrigin(String code, String displayName) {
        MicroPatientOrigin origin = new MicroPatientOrigin();
        origin.setCode(code);
        origin.setDisplayName(displayName);
        return origin;
    }
}
