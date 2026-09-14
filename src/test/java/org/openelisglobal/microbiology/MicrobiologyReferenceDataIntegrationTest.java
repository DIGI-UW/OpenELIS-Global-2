package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.login.valueholder.UserSessionData;
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
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.report.ReportingData;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
        fixtures.ensureSampleEnteredStatus();
        fixtures.ensureAnalysisNotStartedStatus();
        fixtures.ensureAnalysisFinalizedStatus();
        methodId = fixtures.createMethodId();
        referenceData = fixtures.createReferenceData(methodId);
    }

    @Test
    public void activeReferenceLookupsReturnOnlyTheRequestedWorkflow() {
        assertEquals(referenceData.organism().getDisplayName(),
                referenceService.getActiveOrganisms().stream()
                        .filter(organism -> organism.getId().equals(referenceData.organism().getId())).findFirst()
                        .orElseThrow().getDisplayName());
        assertEquals(referenceData.antibiotic().getDisplayName(),
                referenceService.getActiveAntibiotics().stream()
                        .filter(antibiotic -> antibiotic.getId().equals(referenceData.antibiotic().getId())).findFirst()
                        .orElseThrow().getDisplayName());
        assertEquals(referenceData.panel().getId(),
                referenceService.getActiveAstPanels(MicroWorkflowType.BACTERIOLOGY).stream()
                        .filter(panel -> panel.getId().equals(referenceData.panel().getId())).findFirst().orElseThrow()
                        .getId());
        assertEquals(0, referenceService.getActiveAstPanels(MicroWorkflowType.MYCOLOGY).size());
        assertEquals(referenceData.cultureSetup().getId(),
                referenceService.getActiveCultureSetupForMethod(methodId, MicroWorkflowType.BACTERIOLOGY).getId());
    }

    @Test
    public void referenceFixtureReusesCultureSetupForTheSameMethodAndWorkflow() {
        ReferenceData repeated = fixtures.createReferenceData(methodId);

        assertEquals(referenceData.cultureSetup().getId(), repeated.cultureSetup().getId());
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
        assertEquals(1L, referenceService.getActiveAstPanels(MicroWorkflowType.BACTERIOLOGY).stream()
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
        MicroIsolate isolate = isolateService.createIsolate(scenario.caseId, "ISO-1", "Gram negative rods",
                "Lactose fermenting colonies", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, performedBy);
        isolateService.updateIdentification(isolate.getId(), scenario.organismId, "Escherichia coli",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                "MALDI_TOF", new BigDecimal("99.5"), performedBy);
        MicroBreakpointStandard standard = breakpointService.getActiveStandard("CLSI", "2026");
        String panelId = referenceService.getActiveAstPanels(MicroWorkflowType.BACTERIOLOGY).stream()
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

        reportReleaseService.releaseFinal(scenario.caseId, performedBy);
        Analysis analysis = analysisService.get(scenario.analysisId);
        assertNotNull("UAT analysis must have a test section", analysis.getTestSection());
        List<String> projectedValues = resultService.getResultsByAnalysis(analysis).stream().map(Result::getValue)
                .toList();
        assertTrue("Projected Result is missing: " + projectedValues,
                projectedValues.stream().anyMatch(value -> value.contains("ISO-1: Escherichia coli")));
        assertTrue("Projected Result is missing ciprofloxacin interpretation: " + projectedValues,
                projectedValues.stream().anyMatch(value -> value.contains("Ciprofloxacin (UAT) R")));
        assertTrue("Projected Result is missing gentamicin interpretation: " + projectedValues,
                projectedValues.stream().anyMatch(value -> value.contains("Gentamicin (UAT) S")));
        assertTrue("UAT test is not discoverable from its test section",
                testService.getTestsByTestSectionIds(List.of(analysis.getTestSection().getId())).stream()
                        .anyMatch(test -> analysis.getTest().getId().equals(test.getId())));
        assertTrue("Patient is not linked back to the UAT sample",
                sampleHumanService.getSamplesForPatient(scenario.patientId).stream()
                        .anyMatch(sample -> scenario.sampleId.equals(sample.getId())));
        assertTrue("Projected Result is missing from the standard report aggregation",
                rawPatientReportResults(scenario.patientId).stream().anyMatch(item -> item.getResult() != null
                        && item.getResult().getValue().contains("ISO-1: Escherichia coli")));
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
        grantResultsAccess(performedBy, analysis.getTestSection());
        List<IdValuePair> visibleSections = reportingUserTestSections(performedBy);
        assertTrue(
                "Reporting user cannot see UAT test section " + analysis.getTestSection().getId() + "; visible="
                        + visibleSections.stream().map(IdValuePair::getId).toList() + "; mappings="
                        + userRoleService.getUserLabUnitRoles(performedBy).getLabUnitRoleMap().stream()
                                .map(mapping -> mapping.getLabUnit() + "=" + mapping.getRoles()).toList(),
                visibleSections.stream()
                        .anyMatch(section -> analysis.getTestSection().getId().equals(section.getId())));
        ReportingData patientReport = buildPatientReport(scenario.patientId, performedBy);
        List<String> reportValues = patientReport.getRows().stream()
                .map(row -> String.valueOf(row.getDataMap().get("resultValue"))).toList();

        assertTrue("Expected microbiology content in patient report values: " + reportValues,
                reportValues.stream().anyMatch(value -> value.contains("ISO-1: Escherichia coli")));
        assertTrue("Expected ciprofloxacin interpretation in patient report values: " + reportValues,
                reportValues.stream().anyMatch(value -> value.contains("Ciprofloxacin (UAT) R")));
        assertTrue("Expected gentamicin interpretation in patient report values: " + reportValues,
                reportValues.stream().anyMatch(value -> value.contains("Gentamicin (UAT) S")));
    }

    private MicroAntibiotic uatAntibiotic(String whonetCode) {
        return referenceService.getActiveAntibiotics().stream()
                .filter(antibiotic -> whonetCode.equals(antibiotic.getWhonetCode())).findFirst().orElseThrow();
    }

    private ReportingData buildPatientReport(String patientId, String performedBy) throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/reports/patient-results").queryParam("patientId", patientId)
                .with(reportingUser(performedBy)).accept(MediaType.APPLICATION_JSON)).andReturn();

        assertEquals("Patient report request failed: " + result.getResponse().getContentAsString(), 200,
                result.getResponse().getStatus());
        return mapFromJson(result.getResponse().getContentAsString(), ReportingData.class);
    }

    private List<TestResultItem> rawPatientReportResults(String patientId) {
        ResultsLoadUtility resultsUtility = SpringContext.getBean(ResultsLoadUtility.class);
        return resultsUtility.getGroupedTestsForPatient(patientService.getData(patientId));
    }

    private List<IdValuePair> reportingUserTestSections(String performedBy) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        reportingUser(performedBy).postProcessRequest(request);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            return userService.getUserTestSections(performedBy,
                    roleService.getRoleByName(Constants.ROLE_RESULTS).getId());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private RequestPostProcessor reportingUser(String performedBy) {
        return request -> {
            UserDetails user = User.withUsername("admin").password("N/A")
                    .authorities("ROLE_ADMIN", "ROLE_RESULTS", "ROLE_GLOBAL_ADMIN").build();
            SecurityContext context = new SecurityContextImpl();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(user, "N/A", user.getAuthorities()));
            request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context);
            UserSessionData sessionData = new UserSessionData();
            sessionData.setSytemUserId(Integer.parseInt(performedBy));
            request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
            return request;
        };
    }

    private void grantResultsAccess(String systemUserId, TestSection reportSection) {
        String resultsRoleId = roleService.getRoleByName(Constants.ROLE_RESULTS).getId();
        UserLabUnitRoles labUnitRoles = userRoleService.getUserLabUnitRoles(systemUserId);
        if (labUnitRoles == null) {
            labUnitRoles = new UserLabUnitRoles();
            labUnitRoles.setId(Integer.valueOf(systemUserId));
            labUnitRoles.setLabUnitRoleMap(new HashSet<>());
        }
        Set<LabUnitRoleMap> mappings = labUnitRoles.getLabUnitRoleMap();
        if (mappings == null) {
            mappings = new HashSet<>();
            labUnitRoles.setLabUnitRoleMap(mappings);
        }
        LabUnitRoleMap mapping = null;
        for (LabUnitRoleMap candidate : mappings) {
            if (reportSection.getId().equals(candidate.getLabUnit())) {
                mapping = candidate;
                break;
            }
        }
        if (mapping == null) {
            mapping = new LabUnitRoleMap();
            mapping.setLabUnit(reportSection.getId());
            mapping.setRoles(new HashSet<>());
            mappings.add(mapping);
        }
        mapping.getRoles().add(resultsRoleId);
        userRoleService.saveOrUpdateUserLabUnitRoles(labUnitRoles);
    }

    private MicrobiologyUatScenarioRequestForm scenarioRequest(String scenario) {
        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = scenario;
        request.scenarioKey = UUID.randomUUID().toString();
        return request;
    }
}
