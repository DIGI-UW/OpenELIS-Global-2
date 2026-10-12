package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures.ReferenceData;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroBreakpointService;
import org.openelisglobal.microbiology.service.MicroCaseAnalysisService;
import org.openelisglobal.microbiology.service.MicroCaseStateService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.service.MicroLotSelection;
import org.openelisglobal.microbiology.service.MicroReagentLotService;
import org.openelisglobal.microbiology.service.MicroReportReleaseService;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.service.MicrobiologyUatScenarioService;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicrobiologyReferenceDataIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private MicrobiologyReferenceService referenceService;

    @Autowired
    private MicroBreakpointService breakpointService;

    @Autowired
    private MicrobiologyUatScenarioService uatScenarioService;

    @Autowired
    private MicroCaseAnalysisService caseAnalysisService;

    @Autowired
    private MicroCaseStateService caseStateService;

    @Autowired
    private MicroReagentLotService reagentLotService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private MicroIsolateService isolateService;

    @Autowired
    private org.openelisglobal.microbiology.service.MicroCaseService cases;

    @Autowired
    private MicroAstService astService;

    @Autowired
    private MicroReportReleaseService reportReleaseService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private TestService testService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private UserService userService;

    private String methodId;
    private ReferenceData referenceData;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        fixtures.ensureCultureAtmosphere();
        fixtures.ensureSampleEnteredStatus();
        fixtures.ensureAnalysisNotStartedStatus();
        fixtures.ensureAnalysisFinalizedStatus();
        methodId = fixtures.createMethodId();
        referenceData = fixtures.createReferenceData(methodId);
    }

    @Test
    public void reviewedScenarioCanReleaseWithoutBypassingFinalRequirements() {
        String actor = fixtures.defaultUserId();
        var scenario = uatScenarioService.provision(scenarioRequest("AST_REVIEWED"), actor);
        var corrected = referenceService.getActiveOrganisms().stream().filter(o -> "KPNUAT".equals(o.getWhonetCode()))
                .findFirst().orElseThrow();
        var panel = referenceService.getActiveAstPanels(null).stream()
                .filter(p -> p.getId().equals(scenario.astPanelId)).findFirst().orElseThrow();
        org.openelisglobal.microbiology.service.MicroAstPanelAssignmentService.requireCompatible(corrected, panel);
        var released = reportReleaseService.releaseFinal(scenario.caseId, actor);
        assertEquals("FINAL_RELEASED", released.getFinalReleaseState());
        assertTrue(resultService.getResultsByAnalysis(analysisService.get(scenario.analysisId)).isEmpty());
    }

    @Autowired
    private org.openelisglobal.microbiology.service.MicroCaseOrderDetailService orderDetails;

    @Test
    public void exportScenarioPreservesUnclassifiedCulturePurpose() {
        var scenario = uatScenarioService.provision(scenarioRequest("M4"), fixtures.defaultUserId());
        var detail = orderDetails.getOrderDetail(scenario.caseId);
        assertNotNull(detail.getPatientOrigin());
        assertNull(detail.getCulturePurpose());
    }

    @Test
    public void activeReferenceLookupsReturnTheFixtureData() {
        assertEquals(referenceData.organism().getDisplayName(),
                referenceService.getActiveOrganisms().stream()
                        .filter(organism -> organism.getId().equals(referenceData.organism().getId())).findFirst()
                        .orElseThrow().getDisplayName());
        assertEquals(referenceData.antibiotic().getDisplayName(),
                referenceService.getActiveAntibiotics().stream()
                        .filter(antibiotic -> antibiotic.getId().equals(referenceData.antibiotic().getId())).findFirst()
                        .orElseThrow().getDisplayName());
        assertEquals(referenceData.panel().getId(),
                referenceService.getActiveAstPanels("Enterobacterales").stream()
                        .filter(panel -> panel.getId().equals(referenceData.panel().getId())).findFirst().orElseThrow()
                        .getId());
    }

    @Test
    public void breakpointLookupReturnsBestRuleAndNullWhenMissing() {
        MicroBreakpointRule rule = breakpointService.findBreakpointRule(referenceData.standard().getId(),
                referenceData.organism().getId(), "Enterobacterales", referenceData.antibiotic().getId(), "MIC", null,
                "MIC");

        assertEquals(referenceData.rule().getId(), rule.getId());
        assertEquals(new BigDecimal("8.0000"), rule.getSusceptibleValue());
        assertNull(breakpointService.findBreakpointRule(referenceData.standard().getId(),
                referenceData.organism().getId(), "Enterobacterales", "missing", "MIC", null, "MIC"));
    }

    @Test
    public void uatScenariosReuseReferenceConfigurationAndGeneratedCaseIdentity() {
        MicrobiologyUatScenarioRequestForm firstRequest = scenarioRequest("MVP");
        MicrobiologyUatScenarioForm first = uatScenarioService.provision(firstRequest, fixtures.defaultUserId());
        MicrobiologyUatScenarioForm retry = uatScenarioService.provision(firstRequest, fixtures.defaultUserId());
        MicrobiologyUatScenarioForm second = uatScenarioService.provision(scenarioRequest("CASE"),
                fixtures.defaultUserId());

        assertEquals(first.caseId, retry.caseId);
        assertNotEquals(first.caseId, second.caseId);
        assertEquals(first.analysisId, retry.analysisId);
        assertNotNull(first.analysisId);
        assertNotNull(first.reportableTestAnalyteId);
        assertNotNull(first.patientId);
        assertNotNull(sampleHumanService.getPatientForSample(sampleService.get(first.sampleId)));
        assertEquals(first.patientId,
                sampleHumanService.getPatientForSample(sampleService.get(first.sampleId)).getId());
        assertEquals(first.analysisId, caseAnalysisService.getCaseAnalyses(first.caseId).get(0).getAnalysisId());
        assertEquals(first.reportableTestAnalyteId,
                caseAnalysisService.getCaseAnalyses(first.caseId).get(0).getReportableTestAnalyteId());
        assertEquals(1L, referenceService.getActiveAntibiotics().stream()
                .filter(antibiotic -> "CIPUAT".equals(antibiotic.getWhonetCode())).count());
        assertEquals(1L, referenceService.getActiveAntibiotics().stream()
                .filter(antibiotic -> "GENUAT".equals(antibiotic.getWhonetCode())).count());
        assertEquals(1L, referenceService.getActiveAstPanels(null).stream()
                .filter(panel -> "Gram negative AST panel (UAT)".equals(panel.getName())).count());
    }

    @Test
    public void uatLotSelectionsConsumeExactLotsAndRetainBenchProvenance() {
        String performedBy = fixtures.defaultUserId();
        MicrobiologyUatScenarioForm scenario = uatScenarioService.provision(scenarioRequest("MVP"), performedBy);
        var requirements = reagentLotService.getRequirements(scenario.caseId);
        assertEquals(2, requirements.size());
        var media = requirements.stream().filter(requirement -> "PRIMARY".equals(requirement.usageType)).findFirst()
                .orElseThrow();
        var astCard = requirements.stream().filter(requirement -> "SECONDARY".equals(requirement.usageType)).findFirst()
                .orElseThrow();
        var selectedMedia = media.lots.stream().filter(lot -> "UAT-MICRO-MEDIA-FEFO".equals(lot.lotNumber)).findFirst()
                .orElseThrow();
        var selectedCard = astCard.lots.stream().filter(lot -> "UAT-MICRO-CARD-FEFO".equals(lot.lotNumber)).findFirst()
                .orElseThrow();
        assertTrue(selectedMedia.available);
        assertTrue(selectedMedia.fefoRecommended);
        assertTrue(selectedCard.available);
        double mediaQuantity = selectedMedia.currentQuantity;
        double cardQuantity = selectedCard.currentQuantity;

        caseStateService.advanceStage(scenario.caseId, MicroCaseStage.SETUP_RECORDED, performedBy,
                "Service-created lot traceability integration",
                List.of(new MicroLotSelection(scenario.analysisId, media.linkId, selectedMedia.id),
                        new MicroLotSelection(scenario.analysisId, astCard.linkId, selectedCard.id)));

        var refreshed = reagentLotService.getRequirements(scenario.caseId);
        assertEquals(mediaQuantity - 1.0,
                refreshed.stream().filter(requirement -> media.linkId.equals(requirement.linkId)).findFirst()
                        .orElseThrow().lots.stream().filter(lot -> selectedMedia.id.equals(lot.id)).findFirst()
                        .orElseThrow().currentQuantity,
                0.001);
        assertEquals(cardQuantity - 1.0,
                refreshed.stream().filter(requirement -> astCard.linkId.equals(requirement.linkId)).findFirst()
                        .orElseThrow().lots.stream().filter(lot -> selectedCard.id.equals(lot.id)).findFirst()
                        .orElseThrow().currentQuantity,
                0.001);
        var history = reagentLotService.getUsageHistory(scenario.caseId);
        assertEquals(2, history.size());
        assertTrue(history.stream().allMatch(usage -> "CULTURE_SETUP".equals(usage.usageContext)));
        assertTrue(history.stream().allMatch(usage -> usage.actionId != null));
    }

    @Test
    public void finalReleaseAppearsInTheStandardPatientReportWithReviewedSirResults() throws Exception {
        String performedBy = fixtures.defaultUserId();
        MicrobiologyUatScenarioForm scenario = uatScenarioService.provision(scenarioRequest("MVP"), performedBy);
        fixtures.prepareAstCase(cases.getCase(scenario.caseId), performedBy);
        MicroIsolate isolate = isolateService.createIsolate(scenario.caseId, "ISO-1", "Gram negative rods",
                "Lactose fermenting colonies", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, performedBy);
        isolateService.updateIdentification(isolate.getId(), scenario.organismId, "Escherichia coli",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                "MALDI_TOF", new BigDecimal("99.5"), performedBy);
        MicroBreakpointStandard standard = breakpointService.getActiveStandard("CLSI", "2026");
        String panelId = referenceService.getActiveAstPanels(null).stream()
                .filter(panel -> "Gram negative AST panel (UAT)".equals(panel.getName())).findFirst().orElseThrow()
                .getId();
        MicroAntibiotic ciprofloxacin = uatAntibiotic("CIPUAT");
        MicroAntibiotic gentamicin = uatAntibiotic("GENUAT");
        MicroAstRun run = astService.startRun(isolate.getId(), panelId, standard.getId(), performedBy);
        MicroAstReading ciprofloxacinReading = astService.recordReading(run.getId(), ciprofloxacin.getId(),
                MicroAstMethod.MIC, new BigDecimal("4"), performedBy);
        astService.recordReading(run.getId(), gentamicin.getId(), MicroAstMethod.MIC, new BigDecimal("4"), performedBy);
        astService.overrideReading(ciprofloxacinReading.getId(), MicroAstInterpretation.RESISTANT,
                "UAT verification override", performedBy);
        astService.reviewRun(run.getId(), performedBy);
        List<MicroAstRun> persistedRuns = astService.getRunsForIsolate(isolate.getId());
        assertEquals(1, persistedRuns.size());
        assertEquals(MicroAstRunStatus.REVIEWED.name(), persistedRuns.get(0).getStatus());
        assertEquals(2, astService.getReadingsForRun(run.getId()).size());

        fixtures.prepareFinalCase(cases.getCase(scenario.caseId), performedBy);
        reportReleaseService.releaseFinal(scenario.caseId, performedBy);
        Analysis analysis = analysisService.get(scenario.analysisId);
        assertTrue("AST reporting must not manufacture an ordinary clinical Result",
                resultService.getResultsByAnalysis(analysis).isEmpty());
        var form = new org.openelisglobal.reports.form.ReportForm();
        form.setSelPatient(scenario.patientId);
        var report = new org.openelisglobal.reports.action.implementation.PatientCILNSPClinical_vreduit();
        report.setSystemUserId(performedBy);
        report.initializeReport(form);
        String text = org.openelisglobal.testsupport.PdfText.of(report.runReport()).replaceAll("\\s+", " ");
        assertTrue(text, text.contains("ISO-1: Escherichia coli (UAT)"));
        assertTrue(text, text.contains("Ciprofloxacin (UAT) 4 ug/mL Resistant CLSI 2026"));
        assertTrue(text, text.contains("Gentamicin (UAT) 4 ug/mL Susceptible CLSI 2026"));
    }

    private MicroAntibiotic uatAntibiotic(String whonetCode) {
        return referenceService.getActiveAntibiotics().stream()
                .filter(antibiotic -> whonetCode.equals(antibiotic.getWhonetCode())).findFirst().orElseThrow();
    }

    private MicrobiologyUatScenarioRequestForm scenarioRequest(String scenario) {
        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = scenario;
        request.scenarioKey = UUID.randomUUID().toString();
        return request;
    }
}
