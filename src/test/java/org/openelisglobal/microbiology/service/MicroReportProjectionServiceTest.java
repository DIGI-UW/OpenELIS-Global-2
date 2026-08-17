package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstReadingDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;

@RunWith(MockitoJUnitRunner.class)
public class MicroReportProjectionServiceTest {

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseAnalysisDAO caseAnalysisDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Mock
    private MicroAstRunDAO astRunDAO;

    @Mock
    private MicroAstReadingDAO readingDAO;

    @Mock
    private MicroAstRunAntibioticDAO runAntibioticDAO;

    @Mock
    private MicroOrganismDAO organismDAO;

    @Mock
    private MicroAntibioticDAO antibioticDAO;

    @Mock
    private AnalysisService analysisService;

    @Mock
    private TestAnalyteService testAnalyteService;

    @Mock
    private TestResultService testResultService;

    @Mock
    private ResultService resultService;

    @Mock
    private IStatusService statusService;

    private MicroReportProjectionService service;

    @Before
    public void setUp() {
        service = new MicroReportProjectionServiceImpl(caseDAO, caseAnalysisDAO, isolateDAO, astRunDAO, readingDAO,
                runAntibioticDAO, organismDAO, antibioticDAO, analysisService, testAnalyteService, testResultService,
                resultService, statusService);
    }

    @Test
    public void finalProjectionWritesReviewedSirContentToStandardResultAndFinalizesAnalysis() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.REVIEW_READY);
        MicroCaseAnalysis link = link("case-1", "42", "17");
        Analysis analysis = mock(Analysis.class);
        org.openelisglobal.test.valueholder.Test analysisTest = new org.openelisglobal.test.valueholder.Test();
        analysisTest.setId("test-1");
        when(analysis.getTest()).thenReturn(analysisTest);
        TestAnalyte testAnalyte = reportableTestAnalyte("17", "test-1");
        MicroIsolate isolate = isolate("iso-1");
        MicroAstRun run = reviewedRun("run-1", "iso-1");
        MicroAstReading resistant = reading("amp", MicroAstInterpretation.RESISTANT);
        MicroAstReading susceptible = reading("cip", MicroAstInterpretation.SUSCEPTIBLE);

        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of(link));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(run));
        when(runAntibioticDAO.getByRunId("run-1"))
                .thenReturn(List.of(ordered("run-1", "cip", 1), ordered("run-1", "amp", 2)));
        when(readingDAO.getByRunId("run-1")).thenReturn(List.of(resistant, susceptible));
        when(organismDAO.get("org-1")).thenReturn(Optional.of(organism("org-1", "Escherichia coli")));
        when(antibioticDAO.get("amp")).thenReturn(Optional.of(antibiotic("amp", "Ampicillin")));
        when(antibioticDAO.get("cip")).thenReturn(Optional.of(antibiotic("cip", "Ciprofloxacin")));
        when(analysisService.get("42")).thenReturn(analysis);
        when(testAnalyteService.get("17")).thenReturn(testAnalyte);
        TestResult reportTestResult = reportTestResult(analysisTest);
        when(testResultService.getAllActiveTestResultsPerTest(analysisTest)).thenReturn(List.of(reportTestResult));
        when(resultService.insert(any(Result.class))).thenAnswer(invocation -> {
            Result result = invocation.getArgument(0);
            result.setId("201");
            return "201";
        });
        when(statusService.getStatusID(AnalysisStatus.Finalized)).thenReturn("6");

        MicroReportProjectionResult result = service.releaseFinal("case-1", "9");

        assertEquals("Isolate A: Escherichia coli; Ciprofloxacin S, Ampicillin R", result.getContent());
        assertEquals(List.of("201"), result.getProjectedResultIds());
        ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(resultService).insert(resultCaptor.capture());
        assertEquals("Y", resultCaptor.getValue().getIsReportable());
        assertEquals("R", resultCaptor.getValue().getResultType());
        assertEquals(reportTestResult, resultCaptor.getValue().getTestResult());
        assertEquals("9", resultCaptor.getValue().getSysUserId());
        assertEquals("201", link.getProjectedResultId());
        verify(caseAnalysisDAO).update(link);
        verify(analysis).setStatusId("6");
        verify(analysisService).update(analysis);
    }

    @Test
    public void noGrowthPreviewDoesNotPersistAndFinalReleaseCreatesOneStandardResult() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.NO_GROWTH_READY);
        MicroCaseAnalysis link = link("case-1", "42", "17");
        Analysis analysis = mock(Analysis.class);
        org.openelisglobal.test.valueholder.Test analysisTest = new org.openelisglobal.test.valueholder.Test();
        analysisTest.setId("test-1");
        when(analysis.getTest()).thenReturn(analysisTest);
        TestAnalyte testAnalyte = reportableTestAnalyte("17", "test-1");
        TestResult reportTestResult = reportTestResult(analysisTest);

        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of(link));
        when(analysisService.get("42")).thenReturn(analysis);
        when(testAnalyteService.get("17")).thenReturn(testAnalyte);
        when(testResultService.getAllActiveTestResultsPerTest(analysisTest)).thenReturn(List.of(reportTestResult));
        when(resultService.insert(any(Result.class))).thenAnswer(invocation -> {
            Result result = invocation.getArgument(0);
            result.setId("201");
            return "201";
        });
        when(statusService.getStatusID(AnalysisStatus.Finalized)).thenReturn("6");

        MicroReportProjectionResult preview = service.preview("case-1");

        assertEquals("No growth", preview.getContent());
        assertTrue(preview.getProjectedResultIds().isEmpty());
        verify(resultService, never()).insert(any(Result.class));
        verify(resultService, never()).update(any(Result.class));

        MicroReportProjectionResult released = service.releaseFinal("case-1", "9");

        assertEquals("No growth", released.getContent());
        assertEquals(List.of("201"), released.getProjectedResultIds());
        ArgumentCaptor<Result> result = ArgumentCaptor.forClass(Result.class);
        verify(resultService, times(1)).insert(result.capture());
        assertEquals("No growth", result.getValue().getValue());
        assertEquals("Y", result.getValue().getIsReportable());
        assertEquals("9", result.getValue().getSysUserId());
        verify(analysis).setStatusId("6");
        verify(analysis).setReleasedDate(any(Timestamp.class));
        verify(analysis).setSysUserId("9");
        verify(analysisService).update(analysis);
    }

    @Test
    public void preliminaryReleaseRejectsNoGrowthWithoutPersistingAPatientResult() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.NO_GROWTH_READY);
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));

        try {
            service.releasePreliminary("case-1", "9");
            fail("Expected no-growth reporting to require authorized final release");
        } catch (IllegalStateException expected) {
            assertEquals("FINAL_NEGATIVE_RELEASE_REQUIRED", expected.getMessage());
        }
        verify(resultService, never()).insert(any(Result.class));
        verify(resultService, never()).update(any(Result.class));
    }

    @Test
    public void amendedProjectionCreatesNewAnalysisRevisionAndPreservesPriorResult() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.AMENDED);
        MicroCaseAnalysis link = link("case-1", "42", "17");
        link.setProjectedResultId("201");
        org.openelisglobal.test.valueholder.Test analysisTest = new org.openelisglobal.test.valueholder.Test();
        analysisTest.setId("test-1");
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("sample-item-1");
        Analysis original = new Analysis();
        original.setId("42");
        original.setTest(analysisTest);
        original.setSampleItem(sampleItem);
        original.setRevision("0");
        Analysis revised = mock(Analysis.class);
        TestAnalyte testAnalyte = reportableTestAnalyte("17", "test-1");
        TestResult reportTestResult = reportTestResult(analysisTest);

        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of(link));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate("iso-1")));
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(reviewedRun("run-1", "iso-1")));
        when(runAntibioticDAO.getByRunId("run-1")).thenReturn(List.of(ordered("run-1", "amp", 1)));
        when(readingDAO.getByRunId("run-1")).thenReturn(List.of(reading("amp", MicroAstInterpretation.RESISTANT)));
        when(organismDAO.get("org-1")).thenReturn(Optional.of(organism("org-1", "Klebsiella pneumoniae")));
        when(antibioticDAO.get("amp")).thenReturn(Optional.of(antibiotic("amp", "Ampicillin")));
        when(analysisService.get("42")).thenReturn(original);
        when(analysisService.buildAnalysis(analysisTest, sampleItem)).thenReturn(revised);
        when(analysisService.insert(revised)).thenReturn("43");
        when(revised.getId()).thenReturn("43");
        when(revised.getTest()).thenReturn(analysisTest);
        when(analysisService.get("43")).thenReturn(revised);
        when(testAnalyteService.get("17")).thenReturn(testAnalyte);
        when(testResultService.getAllActiveTestResultsPerTest(analysisTest)).thenReturn(List.of(reportTestResult));
        when(resultService.insert(any(Result.class))).thenAnswer(invocation -> {
            Result result = invocation.getArgument(0);
            result.setId("202");
            return "202";
        });
        when(statusService.getStatusID(AnalysisStatus.Finalized)).thenReturn("6");

        MicroReportProjectionResult result = service.releaseAmended("case-1", "9");

        assertEquals(List.of("202"), result.getProjectedResultIds());
        verify(revised).setRevision("1");
        assertEquals("43", link.getAnalysisId());
        assertEquals("202", link.getProjectedResultId());
        verify(resultService, never()).update(any(Result.class));
        verify(caseAnalysisDAO).update(link);
    }

    @Test
    public void preliminaryReleaseKeepsTheCaseUsableWhenAStandardMappingIsNotConfigured() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.REVIEW_READY);
        MicroCaseAnalysis link = link("case-1", "42", null);
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of(link));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate("iso-1")));
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of());
        when(organismDAO.get("org-1")).thenReturn(Optional.of(organism("org-1", "Escherichia coli")));

        MicroReportProjectionResult result = service.releasePreliminary("case-1", "9");

        assertEquals("Isolate A: Escherichia coli", result.getContent());
        assertFalse(result.isMappingConfigured());
        assertTrue(result.getProjectedResultIds().isEmpty());
    }

    @Test
    public void finalReleaseNamesTheMissingProjectionMapping() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.NO_GROWTH_READY);
        MicroCaseAnalysis link = link("case-1", "42", null);
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of(link));

        try {
            service.releaseFinal("case-1", "9");
            fail("Expected final release to require a configured report mapping");
        } catch (IllegalStateException expected) {
            assertEquals("REPORT_MAPPING_REQUIRED", expected.getMessage());
        }
    }

    @Test
    public void finalReleaseRequiresAnActiveRemarkResultDefinitionForPatientHistory() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.NO_GROWTH_READY);
        MicroCaseAnalysis link = link("case-1", "42", "17");
        Analysis analysis = mock(Analysis.class);
        org.openelisglobal.test.valueholder.Test analysisTest = new org.openelisglobal.test.valueholder.Test();
        analysisTest.setId("test-1");
        when(analysis.getTest()).thenReturn(analysisTest);
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of(link));
        when(analysisService.get("42")).thenReturn(analysis);
        when(testResultService.getAllActiveTestResultsPerTest(analysisTest)).thenReturn(List.of());

        try {
            service.releaseFinal("case-1", "9");
            fail("Expected final release to require a remark result definition");
        } catch (IllegalStateException expected) {
            assertEquals("REPORT_MAPPING_REQUIRED", expected.getMessage());
        }
    }

    @Test
    public void preliminaryReleaseNamesMissingReportableContent() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.RECEIVED);
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of());

        try {
            service.releasePreliminary("case-1", "9");
            fail("Expected preliminary release to require reportable content");
        } catch (IllegalStateException expected) {
            assertEquals("REPORTABLE_CONTENT_REQUIRED", expected.getMessage());
        }
    }

    @Test
    public void preliminaryReleaseProjectsGramStainBeforeOrganismIdentification() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.IDENTIFICATION);
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setIsolateLabel("Isolate A");
        isolate.setGramStain("Gram negative rods");
        isolate.setColonyMorphology("Lactose fermenting colonies");
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.PRELIMINARY.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));

        MicroReportProjectionResult result = service.releasePreliminary("case-1", "9");

        assertEquals("Isolate A: Gram stain: Gram negative rods; Colony morphology: Lactose fermenting colonies",
                result.getContent());
        assertTrue(result.hasReportableContent());
    }

    @Test
    public void multipleReviewedAttemptsRequireOneSelectionAndProjectOnlyThatRun() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.REVIEW_READY);
        MicroIsolate isolate = isolate("iso-1");
        MicroAstRun original = reviewedRun("run-1", "iso-1");
        MicroAstRun repeat = reviewedRun("run-2", "iso-1");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(original, repeat));
        when(organismDAO.get("org-1")).thenReturn(Optional.of(organism("org-1", "Escherichia coli")));

        MicroReportProjectionResult unresolvedPreview = service.preview("case-1");

        assertFalse(unresolvedPreview.hasReportableContent());
        assertEquals("", unresolvedPreview.getContent());

        try {
            service.releaseFinal("case-1", "9");
            fail("Expected an explicit reportable AST attempt before release");
        } catch (IllegalStateException expected) {
            assertEquals("REPORTABLE_AST_RUN_REQUIRED", expected.getMessage());
        }

        repeat.setReportable(true);
        when(runAntibioticDAO.getByRunId("run-2")).thenReturn(List.of(ordered("run-2", "cip", 1)));
        when(readingDAO.getByRunId("run-2")).thenReturn(List.of(reading("cip", MicroAstInterpretation.SUSCEPTIBLE)));
        when(antibioticDAO.get("cip")).thenReturn(Optional.of(antibiotic("cip", "Ciprofloxacin")));

        MicroReportProjectionResult projection = service.preview("case-1");

        assertEquals("Isolate A: Escherichia coli; Ciprofloxacin S", projection.getContent());
        verify(readingDAO, never()).getByRunId("run-1");
    }

    @Test
    public void finalizedLegacyCaseWithAmbiguousRunsRecoversReleasedResultContent() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.FINAL_RELEASED);
        MicroCaseAnalysis link = link("case-1", "42", "17");
        link.setProjectedResultId("201");
        MicroIsolate isolate = isolate("iso-1");
        MicroAstRun original = reviewedRun("run-1", "iso-1");
        MicroAstRun repeat = reviewedRun("run-2", "iso-1");
        Result releasedResult = new Result();
        releasedResult.setId("201");
        releasedResult.setValue("Isolate A: Escherichia coli; Ciprofloxacin S");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of(link));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(original, repeat));
        when(organismDAO.get("org-1")).thenReturn(Optional.of(organism("org-1", "Escherichia coli")));
        when(resultService.getResultById("201")).thenReturn(releasedResult);

        MicroReportProjectionResult projection = service.preview("case-1");

        assertEquals("Isolate A: Escherichia coli; Ciprofloxacin S", projection.getContent());
        assertEquals(List.of("201"), projection.getProjectedResultIds());
    }

    @Test
    public void finalizedLegacyCaseRejectsConflictingReleasedResultContent() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.FINAL_RELEASED);
        MicroCaseAnalysis firstLink = link("case-1", "42", "17");
        firstLink.setProjectedResultId("201");
        MicroCaseAnalysis secondLink = link("case-1", "43", "18");
        secondLink.setProjectedResultId("202");
        MicroIsolate isolate = isolate("iso-1");
        Result firstResult = new Result();
        firstResult.setId("201");
        firstResult.setValue("Isolate A: Escherichia coli; Ciprofloxacin S");
        Result secondResult = new Result();
        secondResult.setId("202");
        secondResult.setValue("Isolate A: Escherichia coli; Ciprofloxacin R");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of(firstLink, secondLink));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateId("iso-1"))
                .thenReturn(List.of(reviewedRun("run-1", "iso-1"), reviewedRun("run-2", "iso-1")));
        when(organismDAO.get("org-1")).thenReturn(Optional.of(organism("org-1", "Escherichia coli")));
        when(resultService.getResultById("201")).thenReturn(firstResult);
        when(resultService.getResultById("202")).thenReturn(secondResult);

        try {
            service.preview("case-1");
            fail("Expected conflicting released baselines to require manual resolution");
        } catch (IllegalStateException expected) {
            assertEquals("FINAL_REPORT_BASELINE_AMBIGUOUS", expected.getMessage());
        }
    }

    @Test
    public void previewUsesTheLatestReadingInTheSnapshottedOrderAndExcludesUnorderedRows() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.REVIEW_READY);
        MicroIsolate isolate = isolate("iso-1");
        MicroAstRun run = reviewedRun("run-1", "iso-1");
        MicroAstReading oldAmp = reading("amp", MicroAstInterpretation.RESISTANT);
        oldAmp.setId("reading-1");
        oldAmp.setCreatedAt(Timestamp.valueOf("2026-08-06 08:00:00"));
        MicroAstReading currentAmp = reading("amp", MicroAstInterpretation.SUSCEPTIBLE);
        currentAmp.setId("reading-2");
        currentAmp.setCreatedAt(Timestamp.valueOf("2026-08-06 09:00:00"));
        MicroAstReading unordered = reading("cip", MicroAstInterpretation.RESISTANT);
        unordered.setId("reading-3");
        unordered.setCreatedAt(Timestamp.valueOf("2026-08-06 09:30:00"));
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of());
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of(isolate));
        when(astRunDAO.getByIsolateId("iso-1")).thenReturn(List.of(run));
        when(runAntibioticDAO.getByRunId("run-1")).thenReturn(List.of(ordered("run-1", "amp", 1)));
        when(readingDAO.getByRunId("run-1")).thenReturn(List.of(oldAmp, unordered, currentAmp));
        when(organismDAO.get("org-1")).thenReturn(Optional.of(organism("org-1", "Escherichia coli")));
        when(antibioticDAO.get("amp")).thenReturn(Optional.of(antibiotic("amp", "Ampicillin")));

        MicroReportProjectionResult projection = service.preview("case-1");

        assertEquals("Isolate A: Escherichia coli; Ampicillin S", projection.getContent());
        verify(antibioticDAO, never()).get("cip");
    }

    @Test
    public void previewReturnsExistingProjectedResultIdsForCriticalCommunicationTargeting() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.PRELIM_RELEASED);
        MicroCaseAnalysis link = link("case-1", "42", "17");
        link.setProjectedResultId("201");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of(link));
        when(isolateDAO.getByCaseId("case-1")).thenReturn(List.of());

        MicroReportProjectionResult result = service.preview("case-1");

        assertEquals(List.of("201"), result.getProjectedResultIds());
    }

    private MicroCase microCase(String id, MicroCaseStage stage) {
        MicroCase microCase = new MicroCase();
        microCase.setId(id);
        microCase.setStage(stage.name());
        return microCase;
    }

    private MicroCaseAnalysis link(String caseId, String analysisId, String reportableTestAnalyteId) {
        MicroCaseAnalysis link = new MicroCaseAnalysis();
        link.setCaseId(caseId);
        link.setAnalysisId(analysisId);
        link.setReportableTestAnalyteId(reportableTestAnalyteId);
        return link;
    }

    private TestAnalyte reportableTestAnalyte(String id, String testId) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(testId);
        Analyte analyte = new Analyte();
        analyte.setId("5");
        TestAnalyte testAnalyte = new TestAnalyte();
        testAnalyte.setId(id);
        testAnalyte.setTest(test);
        testAnalyte.setAnalyte(analyte);
        testAnalyte.setIsReportable("Y");
        return testAnalyte;
    }

    private TestResult reportTestResult(org.openelisglobal.test.valueholder.Test test) {
        TestResult testResult = new TestResult();
        testResult.setId("test-result-1");
        testResult.setTest(test);
        testResult.setTestResultType("R");
        testResult.setIsActive(true);
        return testResult;
    }

    private MicroIsolate isolate(String id) {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId(id);
        isolate.setIsolateLabel("Isolate A");
        isolate.setOrganismId("org-1");
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.CONFIRMED.name());
        return isolate;
    }

    private MicroAstRun reviewedRun(String id, String isolateId) {
        MicroAstRun run = new MicroAstRun();
        run.setId(id);
        run.setIsolateId(isolateId);
        run.setStatus(MicroAstRunStatus.REVIEWED.name());
        return run;
    }

    private MicroAstReading reading(String antibioticId, MicroAstInterpretation interpretation) {
        MicroAstReading reading = new MicroAstReading();
        reading.setAntibioticId(antibioticId);
        reading.setInterpretation(interpretation.name());
        return reading;
    }

    private MicroAstRunAntibiotic ordered(String runId, String antibioticId, int displayOrder) {
        MicroAstRunAntibiotic ordered = new MicroAstRunAntibiotic();
        ordered.setAstRunId(runId);
        ordered.setAntibioticId(antibioticId);
        ordered.setDisplayOrder(displayOrder);
        return ordered;
    }

    private MicroOrganism organism(String id, String displayName) {
        MicroOrganism organism = new MicroOrganism();
        organism.setId(id);
        organism.setDisplayName(displayName);
        return organism;
    }

    private MicroAntibiotic antibiotic(String id, String displayName) {
        MicroAntibiotic antibiotic = new MicroAntibiotic();
        antibiotic.setId(id);
        antibiotic.setDisplayName(displayName);
        return antibiotic;
    }
}
