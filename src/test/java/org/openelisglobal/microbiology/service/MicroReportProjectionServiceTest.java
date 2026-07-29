package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;

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
    private MicroOrganismDAO organismDAO;

    @Mock
    private MicroAntibioticDAO antibioticDAO;

    @Mock
    private AnalysisService analysisService;

    @Mock
    private TestAnalyteService testAnalyteService;

    @Mock
    private ResultService resultService;

    @Mock
    private IStatusService statusService;

    private MicroReportProjectionService service;

    @Before
    public void setUp() {
        service = new MicroReportProjectionServiceImpl(caseDAO, caseAnalysisDAO, isolateDAO, astRunDAO, readingDAO,
                organismDAO, antibioticDAO, analysisService, testAnalyteService, resultService, statusService);
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
        when(readingDAO.getByRunId("run-1")).thenReturn(List.of(resistant, susceptible));
        when(organismDAO.get("org-1")).thenReturn(Optional.of(organism("org-1", "Escherichia coli")));
        when(antibioticDAO.get("amp")).thenReturn(Optional.of(antibiotic("amp", "Ampicillin")));
        when(antibioticDAO.get("cip")).thenReturn(Optional.of(antibiotic("cip", "Ciprofloxacin")));
        when(analysisService.get("42")).thenReturn(analysis);
        when(testAnalyteService.get("17")).thenReturn(testAnalyte);
        when(resultService.insert(any(Result.class))).thenAnswer(invocation -> {
            Result result = invocation.getArgument(0);
            result.setId("201");
            return "201";
        });
        when(statusService.getStatusID(AnalysisStatus.Finalized)).thenReturn("6");

        MicroReportProjectionResult result = service.releaseFinal("case-1", "9");

        assertEquals("Isolate A: Escherichia coli (Ampicillin R, Ciprofloxacin S)", result.getContent());
        assertEquals(List.of("201"), result.getProjectedResultIds());
        ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(resultService).insert(resultCaptor.capture());
        assertEquals("Y", resultCaptor.getValue().getIsReportable());
        assertEquals("A", resultCaptor.getValue().getResultType());
        assertEquals("9", resultCaptor.getValue().getSysUserId());
        assertEquals("201", link.getProjectedResultId());
        verify(caseAnalysisDAO).update(link);
        verify(analysis).setStatusId("6");
        verify(analysisService).update(analysis);
    }

    @Test
    public void preliminaryReleaseKeepsTheCaseUsableWhenAStandardMappingIsNotConfigured() {
        MicroCase microCase = microCase("case-1", MicroCaseStage.NO_GROWTH_READY);
        MicroCaseAnalysis link = link("case-1", "42", null);
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of(link));

        MicroReportProjectionResult result = service.releasePreliminary("case-1", "9");

        assertEquals("No growth", result.getContent());
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
