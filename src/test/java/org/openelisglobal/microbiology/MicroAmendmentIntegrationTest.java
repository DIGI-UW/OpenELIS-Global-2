package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures.ReferenceData;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroCaseAmendmentService;
import org.openelisglobal.microbiology.service.MicroCaseAnalysisService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroIdentificationHistoryService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.service.MicroReportReleaseService;
import org.openelisglobal.microbiology.service.MicroReportVersionService;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationEvent;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroReportVersion;
import org.openelisglobal.microbiology.valueholder.MicroReportVersionType;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class MicroAmendmentIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private AnalyteService analyteService;

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private MicroCaseAnalysisService caseAnalysisService;

    @Autowired
    private TestAnalyteService testAnalyteService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private MicroIsolateService isolateService;

    @Autowired
    private MicroAstService astService;

    @Autowired
    private MicroReportReleaseService reportReleaseService;

    @Autowired
    private MicroCaseAmendmentService amendmentService;

    @Autowired
    private MicroReportVersionService reportVersionService;

    @Autowired
    private MicroIdentificationHistoryService identificationHistoryService;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    public void amendmentAppendsIdentificationAndReportHistoryThenRelocks() {
        FinalCase fixture = createFinalCase();

        amendmentService.openAmendment(fixture.caseId(), "Correct identification", fixture.userId());
        try {
            amendmentService.openAmendment(fixture.caseId(), "Open a second amendment", fixture.userId());
            fail("Expected only one open amendment per case");
        } catch (RuntimeException expected) {
            assertEquals("AMENDMENT_ALREADY_OPEN", expected.getMessage());
        }

        isolateService.updateIdentification(fixture.isolateId(), null, "Klebsiella pneumoniae",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                "Confirmatory identification corrected the organism", fixture.userId());
        reportReleaseService.releaseAmended(fixture.caseId(), fixture.userId());

        List<MicroReportVersion> versions = reportVersionService.getVersions(fixture.caseId());
        assertEquals(2, versions.size());
        assertEquals(Integer.valueOf(1), versions.get(0).getVersionNumber());
        assertEquals(MicroReportVersionType.FINAL.name(), versions.get(0).getReleaseType());
        assertTrue(versions.get(0).getContent().contains("Escherichia coli"));
        assertEquals(Integer.valueOf(2), versions.get(1).getVersionNumber());
        assertEquals(MicroReportVersionType.AMENDED.name(), versions.get(1).getReleaseType());
        assertEquals(versions.get(0).getId(), versions.get(1).getCorrectsVersionId());
        assertTrue(versions.get(1).getContent().contains("Klebsiella pneumoniae"));
        assertTrue(reportVersionService.getSourcesForCase(fixture.caseId()).size() >= 2);

        List<MicroIsolateIdentificationEvent> history = identificationHistoryService.getHistory(fixture.isolateId());
        assertEquals(1, history.size());
        assertEquals(fixture.originalOrganismText(), history.get(0).getPreviousOrganismText());
        assertEquals("Klebsiella pneumoniae", history.get(0).getNewOrganismText());
        assertEquals("Confirmatory identification corrected the organism", history.get(0).getReason());

        MicroCase relocked = caseService.getCase(fixture.caseId());
        assertEquals(MicroCaseStage.FINAL_RELEASED.name(), relocked.getStage());
        assertEquals(MicroCaseFinalReleaseState.FINAL_RELEASED.name(), relocked.getFinalReleaseState());
        assertNull(amendmentService.getOpenAmendment(fixture.caseId()));
    }

    @Test
    public void outerFailureRollsBackTheEntireAmendmentOpenTransaction() {
        FinalCase fixture = createFinalCase();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        try {
            transaction.executeWithoutResult(status -> {
                amendmentService.openAmendment(fixture.caseId(), "Must roll back", fixture.userId());
                throw new IllegalStateException("FORCED_ROLLBACK");
            });
            fail("Expected the transaction to roll back");
        } catch (IllegalStateException expected) {
            assertEquals("FORCED_ROLLBACK", expected.getMessage());
        }

        assertTrue(amendmentService.getHistory(fixture.caseId()).isEmpty());
        assertEquals(1, reportVersionService.getVersions(fixture.caseId()).size());
        MicroCase unchanged = caseService.getCase(fixture.caseId());
        assertEquals(MicroCaseStage.FINAL_RELEASED.name(), unchanged.getStage());
        assertEquals(MicroCaseFinalReleaseState.FINAL_RELEASED.name(), unchanged.getFinalReleaseState());
    }

    private FinalCase createFinalCase() {
        String userId = fixtures.defaultUserId();
        fixtures.ensureRequiredWorkflowStatuses();
        String methodId = fixtures.createMethodId();
        ReferenceData referenceData = fixtures.createReferenceData(methodId);
        SampleItem sampleItem = fixtures.createSampleWithSampleItem("OGC782M8A");
        sampleItem.setTypeOfSample(typeOfSampleService.getAllTypeOfSamples().stream()
                .filter(type -> type.getLocalization() != null).findFirst()
                .orElseThrow(() -> new IllegalStateException("A configured specimen type is required")));
        sampleItem.setSysUserId(userId);
        sampleItemService.update(sampleItem);
        MicroCase microCase = caseService.createOrGetCase(sampleItem.getId(), MicroWorkflowType.BACTERIOLOGY, methodId,
                userId);
        linkReportableAnalysis(microCase, sampleItem, referenceData, userId);

        MicroIsolate isolate = isolateService.createIsolate(microCase.getId(), "ISO-1", null,
                referenceData.organism().getDisplayName(), MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, userId);
        MicroAstRun run = astService.startRun(isolate.getId(), referenceData.panel().getId(),
                referenceData.standard().getId(), userId);
        astService.recordReading(run.getId(), referenceData.antibiotic().getId(), MicroAstMethod.MIC,
                new BigDecimal("4"), userId);
        astService.reviewRun(run.getId(), userId);
        reportReleaseService.releaseFinal(microCase.getId(), userId);
        return new FinalCase(microCase.getId(), isolate.getId(), referenceData.organism().getDisplayName(), userId);
    }

    private void linkReportableAnalysis(MicroCase microCase, SampleItem sampleItem, ReferenceData referenceData,
            String userId) {
        org.openelisglobal.test.valueholder.Test test = fixtures.createCatalogTest();
        Analysis analysis = new Analysis();
        analysis.setSampleItem(sampleItem);
        analysis.setTest(test);
        analysis.setAnalysisType("MANUAL");
        analysis.setIsReportable(IActionConstants.YES);
        analysis.setRevision("0");
        analysis.setStartedDate(Timestamp.from(Instant.now()));
        analysis.setStatusId(fixtures.ensureAnalysisNotStartedStatus());
        analysis.setFhirUuid(UUID.randomUUID());
        analysis.setSysUserId(userId);
        analysisService.insert(analysis);

        String analyteSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Analyte analyte = new Analyte();
        analyte.setAnalyteName("Microbiology report " + analyteSuffix);
        analyte.setLocalAbbreviation("M" + analyteSuffix);
        analyte.setIsActive(IActionConstants.YES);
        analyte.setSysUserId(userId);
        analyteService.insert(analyte);

        TestAnalyte testAnalyte = new TestAnalyte();
        testAnalyte.setTest(test);
        testAnalyte.setAnalyte(analyte);
        testAnalyte.setSortOrder("0");
        testAnalyte.setIsReportable(IActionConstants.YES);
        testAnalyte.setSysUserId(userId);
        testAnalyteService.insert(testAnalyte);

        TestResult testResult = new TestResult();
        testResult.setTest(test);
        testResult.setTestResultType(ResultType.REMARK.getCharacterValue());
        testResult.setIsActive(true);
        testResult.setSortOrder("0");
        testResult.setSysUserId(userId);
        testResultService.insert(testResult);

        referenceData.cultureSetup().setReportableTestAnalyteId(testAnalyte.getId());
        caseAnalysisService.linkAnalysis(microCase, analysis, referenceData.cultureSetup());
    }

    private record FinalCase(String caseId, String isolateId, String originalOrganismText, String userId) {
    }
}
