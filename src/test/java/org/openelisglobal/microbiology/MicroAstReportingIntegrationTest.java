package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import org.junit.*;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.MicroAstWorkspaceForm;
import org.openelisglobal.microbiology.form.MicroDstForm;
import org.openelisglobal.microbiology.service.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

@org.springframework.transaction.annotation.Transactional
public class MicroAstReportingIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private MicroBreakpointAdminService breakpointAdminService;
    @Autowired
    private MicroCaseDAO cases;
    @Autowired
    private MicroIsolateDAO isolates;
    @Autowired
    private MicroAstPanelDAO panels;
    @Autowired
    private MicroAntibioticDAO antibiotics;
    @Autowired
    private MicroOrganismDAO organisms;
    @Autowired
    private MicroCaseTimelineService timeline;
    @Autowired
    private MicroAstPanelAntibioticDAO panelDrugs;
    @Autowired
    private MicroBreakpointRuleDAO rules;
    @Autowired
    private MicroAstReadingDAO readings;
    @Autowired
    private MicroAstRunDAO runs;
    @Autowired
    private MicroAstRunAntibioticDAO runAntibiotics;
    @Autowired
    private MicroReportingRuleService reportingRules;
    @Autowired
    private MicroAstResponseService astResponses;
    @Autowired
    private MicroInterpretationRuleService interpretationRules;
    @Autowired
    private MicroAstService ast;
    @Autowired
    private org.openelisglobal.analyzer.service.AnalyzerService analyzers;
    @Autowired
    private MicroIsolateService isolateService;
    @Autowired
    private MicroAstPanelAssignmentService assignments;
    @Autowired
    private MicroAstReportChoiceService choices;
    @Autowired
    private MicroPatientReportService reports;
    @Autowired
    private MicroReportVersionService versions;
    @Autowired
    private MicroReportProjectionService projection;
    @Autowired
    private MicroOrderRoutingService routing;
    @Autowired
    private MicroCaseAnalysisDAO analysisLinks;
    @Autowired
    private MicroMolecularDstBindingDAO molecularBindings;
    @Autowired
    private MicroDstService dst;
    @Autowired
    private MicroCaseReadinessService readiness;
    @Autowired
    private MicroAstWorkspaceService workspace;
    @Autowired
    private MicroCaseActivityDAO activities;
    @Autowired
    private org.openelisglobal.systemuser.service.SystemUserService users;
    @Autowired
    private org.openelisglobal.role.service.RoleService roles;
    @Autowired
    private org.openelisglobal.microbiology.service.MicrobiologyReferenceAdminService referenceAdmin;
    @PersistenceContext
    private EntityManager em;
    private MicroCase c;
    private MicrobiologyTestFixtures.ReferenceData reference;
    private String writer, validator, outsider, isolate, drugB;
    private String sampleTypeId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        fixtures.ensureCultureAtmosphere();
        executeDataSetWithStateManagement("testdata/system-user.xml");
        executeDataSetWithStateManagement("testdata/role.xml");
        em.clear();
        var unit = fixtures.createLabUnit();
        writer = actor(unit.getId(), Constants.ROLE_RESULTS);
        validator = actor(unit.getId(), Constants.ROLE_VALIDATION);
        outsider = actor(fixtures.createLabUnit().getId(), Constants.ROLE_VALIDATION);
        var source = fixtures.createSampleWithSampleItem("M6");
        source.setTypeOfSample(fixtures.getOrCreateActiveSampleType());
        sampleTypeId = source.getTypeOfSample().getId();
        c = new MicroCase();
        c.setSampleId(source.getSample().getId());
        c.setSampleItemId(source.getId());
        c.setLabUnitId(unit.getId());
        c.setCreatedBy(writer);
        cases.insert(c);
        reference = fixtures.createReferenceData(fixtures.createMethodId());
        reference.panel().setPublishedAt(new Timestamp(System.currentTimeMillis()));
        reference.panel().setPublishedBy(writer);
        panels.update(reference.panel());
        var b = new MicroAntibiotic();
        b.setDisplayName("Second agent");
        b.setWhonetCode("B" + UUID.randomUUID().toString().substring(0, 7));
        antibiotics.insert(b);
        drugB = b.getId();
        var ordered = new MicroAstPanelAntibiotic();
        ordered.setPanelId(reference.panel().getId());
        ordered.setAntibioticId(drugB);
        ordered.setDisplayOrder(2);
        panelDrugs.insert(ordered);
        var rule = new MicroBreakpointRule();
        rule.setStandardId(reference.standard().getId());
        rule.setOrganismId(reference.organism().getId());
        rule.setAntibioticId(drugB);
        rule.setMethod("MIC");
        rule.setBreakpointType("MIC");
        rule.setSusceptibleValue(new BigDecimal("8"));
        rule.setResistantValue(new BigDecimal("32"));
        rules.insert(rule);
        breakpointAdminService.activate(reference.standard().getId(), new java.sql.Date(System.currentTimeMillis()),
                fixtures.defaultUserId());
        isolate = isolate("ISO-1");
        em.flush();
    }

    @Test
    public void issuedAstReportIncludesPendingAnchorOnceAndUsesCaseOwningUnitForAccess() {
        var source = em.find(org.openelisglobal.sampleitem.valueholder.SampleItem.class, c.getSampleItemId());
        var test = fixtures.createCatalogTest();
        for (int n = 0; n < 2; n++) {
            var analysis = fixtures.createAnalysis(source, test);
            var link = new MicroCaseAnalysis();
            link.setCaseId(c.getId());
            link.setAnalysisId(analysis.getId());
            link.setPlacement("INITIAL");
            link.setCaseRole(MicroCaseRole.CULTURE);
            link.setSelectedForReport(false);
            em.persist(link);
        }
        var run = start(isolate);
        enter(run, "4");
        ast.reviewRun(run.getId(), validator);
        versions.recordPreliminary(c.getId(), projection.releasePreliminary(c.getId(), writer), writer);
        em.flush();
        assertEquals(2, reports.issuedAnalysesForSample(c.getSampleId()).size());
        String reporter = actor(c.getLabUnitId(), Constants.ROLE_REPORTS);
        var harness = new PrintedReportHarness();
        var printed = harness.render(source.getSample(), reporter);
        assertEquals(1, printed.size());
        assertFalse(harness.complete());
        assertEquals(reports.preview(c.getId()).labUnitName, printed.get(0).getTestSection());
        assertTrue(printed.get(0).getMicrobiologyRows().stream()
                .anyMatch(r -> reference.antibiotic().getDisplayName().equals(r.getAgent())));
        assertTrue(new PrintedReportHarness().render(source.getSample(), outsider).isEmpty());
    }

    private static class PrintedReportHarness
            extends org.openelisglobal.reports.action.implementation.PatientCILNSPClinical_vreduit {
        List<org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData> render(
                org.openelisglobal.sample.valueholder.Sample sample, String actor) {
            initializeReportItems();
            currentSample = sample;
            setSystemUserId(actor);
            sampleCompleteMap = new HashMap<>();
            sampleCompleteMap.put(convertToAlphaNumericDisplay(sample), true);
            analysisService = org.mockito.Mockito.mock(org.openelisglobal.analysis.service.AnalysisService.class);
            org.mockito.Mockito
                    .when(analysisService.getAnalysesBySampleIdAndStatusId(org.mockito.ArgumentMatchers.anyString(),
                            org.mockito.ArgumentMatchers.any()))
                    .thenReturn(List.of());
            createReportItems();
            return reportItems;
        }

        boolean complete() {
            return sampleCompleteMap.get(convertToAlphaNumericDisplay(currentSample));
        }

        @Override
        protected org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData buildClinicalPatientData(
                boolean parent) {
            sampleCompleteMap.put(convertToAlphaNumericDisplay(currentSample), false);
            return new org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData();
        }

        @Override
        protected void setCollectionTime(Set<org.openelisglobal.sampleitem.valueholder.SampleItem> samples,
                List<org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData> rows,
                boolean ignored) {
        }
    }

    @Autowired
    private MicrobiologyUatScenarioService scenarioService;

    @Test
    public void browserScenarioCreatesItsOwnCompleteClinicalContext() {
        var request = new org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm();
        request.scenario = "AST_REPORTING";
        request.scenarioKey = "m6-integration-" + UUID.randomUUID();
        var adminRole = new org.openelisglobal.userrole.valueholder.UserRole();
        adminRole.setSystemUserId(writer);
        adminRole.setRoleId(roles.getRoleByName(Constants.ROLE_GLOBAL_ADMIN).getId());
        em.persist(adminRole);
        em.flush();
        var scenario = scenarioService.provision(request, writer);
        em.flush();
        em.clear();
        assertNotNull(scenario.isolateId);
        assertEquals(1, assignments.getAssignments(scenario.isolateId).size());
        assertEquals("GROWTH", reports.preview(scenario.caseId).cultures.get(0).outcome());
        assertTrue(em.find(org.openelisglobal.microbiology.valueholder.MicroAstPanel.class, scenario.astPanelId)
                .getPublishedAt() != null);
    }

    private String actor(String unit, String role) {
        var user = new org.openelisglobal.systemuser.valueholder.SystemUser();
        user.setLoginName("M6" + UUID.randomUUID().toString().substring(0, 12));
        user.setFirstName("M6" + UUID.randomUUID().toString().substring(0, 8));
        user.setLastName(role);
        user.setIsActive("Y");
        user.setIsEmployee("Y");
        user.setSysUserId("1");
        String id = users.insert(user);
        var assignment = new org.openelisglobal.userrole.valueholder.UserLabUnitRoles();
        assignment.setId(Integer.valueOf(id));
        var map = new org.openelisglobal.userrole.valueholder.LabUnitRoleMap();
        map.setLabUnit(unit);
        map.setRoles(Set.of(roles.getRoleByName(role).getId()));
        assignment.setLabUnitRoleMap(Set.of(map));
        em.persist(assignment);
        return id;
    }

    private String isolate(String label) {
        return isolate(c.getId(), label);
    }

    private String isolate(String caseId, String label) {
        var i = new MicroIsolate();
        i.setCaseId(caseId);
        i.setIsolateLabel(label);
        i.setCreatedBy(writer);
        i.setOrganismId(reference.organism().getId());
        i.setIdentificationStatus("CONFIRMED");
        isolates.insert(i);
        assignments.assignDefault(i, writer);
        return i.getId();
    }

    private MicroAstRun start(String id) {
        return ast.startRun(id, null, reference.standard().getId(), null, MicroAstTechnique.BROTH_MICRODILUTION,
                List.of(), writer);
    }

    private List<MicroAstReading> enter(MicroAstRun run, String value) {
        return List.of(ast.recordReading(run.getId(), reference.antibiotic().getId(), new BigDecimal(value), writer),
                ast.recordReading(run.getId(), drugB, new BigDecimal(value), writer));
    }

    private Map<String, String> selected(String id) {
        em.flush();
        em.clear();
        return choices.getSelectedReadings(id).stream()
                .collect(java.util.stream.Collectors.toMap(MicroAstReading::getAntibioticId, MicroAstReading::getId));
    }

    @Test
    public void choosingOneRepeatedDrugPreservesOtherDrugAndOtherIsolateAcrossReload() {
        var first = start(isolate);
        var original = enter(first, "4");
        ast.reviewRun(first.getId(), validator);
        var other = isolate("ISO-2");
        var otherRun = start(other);
        var otherReadings = enter(otherRun, "4");
        ast.reviewRun(otherRun.getId(), validator);
        var repeat = ast.startRepeatRun(first.getId(), MicroAstAttemptType.RETEST, "Confirm first agent",
                MicroAstTechnique.BROTH_MICRODILUTION, writer);
        var repeated = enter(repeat, "64");
        ast.reviewRun(repeat.getId(), validator);
        assertEquals(original.get(0).getId(), selected(isolate).get(reference.antibiotic().getId()));
        choices.select(isolate, reference.antibiotic().getId(), repeated.get(0).getId(), "Confirmed resistance",
                validator);
        assertEquals(Map.of(reference.antibiotic().getId(), repeated.get(0).getId(), drugB, original.get(1).getId()),
                selected(isolate));
        assertEquals(Map.of(reference.antibiotic().getId(), otherReadings.get(0).getId(), drugB,
                otherReadings.get(1).getId()), selected(other));
        var report = reports.preview(c.getId());
        assertEquals(4, report.validatedResults);
        assertEquals(2, report.isolates.size());
        var row = report.isolates.stream().filter(i -> isolate.equals(i.id())).findFirst().orElseThrow();
        assertEquals(2, row.susceptibilities().size());
        assertEquals(Set.of(repeated.get(0).getId(), original.get(1).getId()),
                readings.getSelectedByRunIds(List.of(first.getId(), repeat.getId())).stream()
                        .map(MicroAstReading::getId).collect(java.util.stream.Collectors.toSet()));
        var byDrug = row.susceptibilities().stream()
                .collect(java.util.stream.Collectors.toMap(r -> r.antibioticId(), r -> r));
        assertEquals("RESISTANT", byDrug.get(reference.antibiotic().getId()).interpretation());
        assertEquals("SUSCEPTIBLE", byDrug.get(drugB).interpretation());
        assertEquals("2026", byDrug.get(drugB).version());
        assertEquals(new BigDecimal("4.0000"), readings.get(original.get(0).getId()).orElseThrow().getRawValue());
    }

    @Test
    public void xpertOrderedAtEntryWithRifResistanceAgainstSusceptiblePhenotypeBlocksRelease() {
        var tb = tbCaseWithXpert("RIF_DETECTED");
        var run = start(tb.isolateId());
        enter(run, "4");
        ast.reviewRun(run.getId(), validator);

        var rif = rifampicin(tb.caseId());
        assertEquals("DISCORDANT", rif.interpretation());
        assertEquals(Set.of("MOLECULAR:RESISTANT", "PHENOTYPIC:SUSCEPTIBLE"), sources(rif));
        assertTrue(readiness.getReadiness(tb.caseId()).blockers.contains("TB_DISCORDANCE_UNRESOLVED"));
    }

    @Test
    public void resolvingTheXpertDiscordanceClearsTheReleaseBlocker() {
        var tb = tbCaseWithXpert("RIF_DETECTED");
        var run = start(tb.isolateId());
        enter(run, "4");
        ast.reviewRun(run.getId(), validator);
        var request = new MicroDstForm.ResolutionRequest();
        request.antibioticId = reference.antibiotic().getId();
        request.evidenceFingerprint = rifampicin(tb.caseId()).evidenceFingerprint();
        request.interpretation = "RESISTANT";
        request.reason = "Molecular result confirmed by the reference laboratory";

        dst.resolve(tb.caseId(), request, validator);

        var rif = rifampicin(tb.caseId());
        assertEquals("RESISTANT", rif.interpretation());
        assertEquals(request.reason, rif.resolutionReason());
        assertFalse(readiness.getReadiness(tb.caseId()).blockers.contains("TB_DISCORDANCE_UNRESOLVED"));
    }

    @Test
    public void unreviewedRunDoesNotChangeTheTbClassification() {
        var tb = tbCaseWithXpert("RIF_NOT_DETECTED");
        var run = start(tb.isolateId());
        enter(run, "4");
        ast.reviewRun(run.getId(), validator);
        String before = dst.evaluate(tb.caseId()).classification;

        var repeat = ast.startRepeatRun(run.getId(), MicroAstAttemptType.RETEST, "Confirm rifampicin",
                MicroAstTechnique.BROTH_MICRODILUTION, writer);
        enter(repeat, "64");

        assertEquals(before, dst.evaluate(tb.caseId()).classification);
        var rif = rifampicin(tb.caseId());
        assertEquals("SUSCEPTIBLE", rif.interpretation());
        assertFalse(rif.discordant());
    }

    @Test
    public void originalRunAndItsSelectedRepeatAreNotDiscordant() {
        var tb = tbCaseWithXpert("RIF_DETECTED");
        var run = start(tb.isolateId());
        enter(run, "4");
        ast.reviewRun(run.getId(), validator);
        var repeat = ast.startRepeatRun(run.getId(), MicroAstAttemptType.RETEST, "Confirm rifampicin",
                MicroAstTechnique.BROTH_MICRODILUTION, writer);
        var repeated = enter(repeat, "64");
        ast.reviewRun(repeat.getId(), validator);
        choices.select(tb.isolateId(), reference.antibiotic().getId(), repeated.get(0).getId(), "Confirmed resistance",
                validator);

        var rif = rifampicin(tb.caseId());
        assertEquals("RESISTANT", rif.interpretation());
        assertFalse(rif.discordant());
        assertEquals(Set.of("MOLECULAR:RESISTANT", "PHENOTYPIC:RESISTANT"), sources(rif));
    }

    @Test
    public void untickedFinalizedMolecularResultIsStillTbEvidence() {
        var tb = tbCaseWithXpert("RIF_DETECTED");
        tb.xpert().setPlacement("INITIAL");
        analysisLinks.update(tb.xpert());
        var run = start(tb.isolateId());
        enter(run, "4");
        ast.reviewRun(run.getId(), validator);

        reports.selectAnalysis(tb.caseId(), tb.xpert().getAnalysisId(), false, validator);

        var rif = rifampicin(tb.caseId());
        assertTrue(rif.discordant());
        assertEquals(Set.of("MOLECULAR:RESISTANT", "PHENOTYPIC:SUSCEPTIBLE"), sources(rif));
    }

    private record TbCase(String caseId, String isolateId, MicroCaseAnalysis xpert) {
    }

    private TbCase tbCaseWithXpert(String xpertValue) {
        var organism = organisms.get(reference.organism().getId()).orElseThrow();
        organism.setSusceptibilityProtocol("TB");
        organisms.update(organism);
        reference.panel().setSusceptibilityProtocol("TB");
        panels.update(reference.panel());
        var drug = antibiotics.get(reference.antibiotic().getId()).orElseThrow();
        drug.setResistanceRole("RIFAMPICIN");
        antibiotics.update(drug);

        var xpert = em.find(org.openelisglobal.test.valueholder.Test.class, fixtures.createCatalogTest().getId());
        xpert.setTestSection(em.find(org.openelisglobal.test.valueholder.TestSection.class, c.getLabUnitId()));
        xpert.setOpensMicrobiologyCase(true);
        xpert.setMicrobiologyCaseRole("DIRECT");
        var specimen = fixtures.createSampleWithSampleItem("M6TB");
        specimen.setTypeOfSample(fixtures.createTypeOfSample());
        var analysis = fixtures.createAnalysis(specimen, xpert);
        analysis.setStatusId(fixtures.ensureAnalysisFinalizedStatus());
        var request = new org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest();
        request.setSample(specimen.getSample());
        request.setTypeOfSample(specimen.getTypeOfSample());
        request.setSampleItem(specimen);
        request.setRequestedTests(xpert.getId());
        request.setStatus(org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest.Status.COLLECTED);
        request.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        em.persist(request);
        em.flush();
        routing.routeOrder(specimen.getSample(), writer);
        var link = analysisLinks.getActiveByAnalysisId(analysis.getId());
        assertEquals("INITIAL_TESTING", link.getPlacement());

        var component = new org.openelisglobal.testresultcomponent.valueholder.TestResultComponent();
        component.setTestId(xpert.getId());
        component.setCode("RIF");
        component.setLabel("Rifampicin resistance");
        component.setResultType("D");
        em.persist(component);
        var option = new org.openelisglobal.testresult.valueholder.TestResult();
        option.setTest(xpert);
        option.setTestResultType("D");
        option.setComponentId(component.getId());
        option.setSortOrder("1");
        em.persist(option);
        var result = new org.openelisglobal.result.valueholder.Result();
        result.setAnalysis(analysis);
        result.setTestResult(option);
        result.setValue(xpertValue);
        result.setResultType("D");
        result.setSysUserId(writer);
        em.persist(result);
        var binding = new MicroMolecularDstBinding();
        binding.setTestId(xpert.getId());
        binding.setComponentId(option.getComponentId());
        binding.setAntibioticId(reference.antibiotic().getId());
        binding.setResistantValue("RIF_DETECTED");
        binding.setSusceptibleValue("RIF_NOT_DETECTED");
        molecularBindings.insert(binding);
        em.flush();
        return new TbCase(link.getCaseId(), isolate(link.getCaseId(), "ISO-TB"), link);
    }

    private org.openelisglobal.microbiology.form.MicroDstForm.Drug rifampicin(String caseId) {
        em.flush();
        em.clear();
        return dst.evaluate(caseId).drugs.stream().filter(d -> d.antibioticId().equals(reference.antibiotic().getId()))
                .findFirst().orElseThrow();
    }

    private static Set<String> sources(org.openelisglobal.microbiology.form.MicroDstForm.Drug drug) {
        return drug.evidence().stream().map(e -> e.source() + ":" + e.interpretation())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    public void databaseAppendSequenceWinsOverTimestampOrder() {
        var run = start(isolate);
        var original = enter(run, "4");
        var later = ast.recordReading(run.getId(), reference.antibiotic().getId(), new BigDecimal("64"), writer);
        var sameTime = new Timestamp(1000000);
        original.get(0).setCreatedAt(sameTime);
        later.setCreatedAt(new Timestamp(999999));
        readings.update(original.get(0));
        readings.update(later);
        em.flush();
        em.clear();
        ast.reviewRun(run.getId(), validator);
        assertEquals(later.getId(), selected(isolate).get(reference.antibiotic().getId()));
        assertEquals(3, readings.getByRunId(run.getId()).size());
        assertTrue(readings.get(later.getId()).orElseThrow().getEntrySequence() > readings.get(original.get(0).getId())
                .orElseThrow().getEntrySequence());
    }

    @Test
    public void twoPartialVersionsRemainImmutableAfterNewSelectionAndCatalogRename() {
        var first = start(isolate);
        enter(first, "4");
        ast.reviewRun(first.getId(), validator);
        var v1 = versions.recordPreliminary(c.getId(), projection.releasePreliminary(c.getId(), writer), writer);
        String issued = v1.getStructuredContent();
        var repeat = ast.startRepeatRun(first.getId(), MicroAstAttemptType.REPEAT, "Confirm resistance",
                MicroAstTechnique.BROTH_MICRODILUTION, writer);
        var repeated = enter(repeat, "64");
        ast.reviewRun(repeat.getId(), validator);
        choices.select(isolate, reference.antibiotic().getId(), repeated.get(0).getId(), "Confirmed", validator);
        versions.recordPreliminary(c.getId(), projection.releasePreliminary(c.getId(), writer), writer);
        var drug = antibiotics.get(reference.antibiotic().getId()).orElseThrow();
        drug.setDisplayName("Renamed after issue");
        antibiotics.update(drug);
        em.flush();
        em.clear();
        var history = reports.versions(c.getId());
        assertEquals(List.of(1, 2), history.stream().map(v -> v.number()).toList());
        assertEquals(issued, versions.getVersions(c.getId()).get(0).getStructuredContent());
        assertEquals("SUSCEPTIBLE", history.get(0).report().isolates.get(0).susceptibilities().stream()
                .filter(r -> r.antibioticId().equals(drug.getId())).findFirst().orElseThrow().interpretation());
        assertEquals("RESISTANT", history.get(1).report().isolates.get(0).susceptibilities().stream()
                .filter(r -> r.antibioticId().equals(drug.getId())).findFirst().orElseThrow().interpretation());
    }

    @Test
    public void enabledSelfValidationPolicyRejectsTheResultAuthorEvenWithBothRoles() {
        var assignment = em.find(org.openelisglobal.userrole.valueholder.UserLabUnitRoles.class,
                Integer.valueOf(writer));
        assignment.getLabUnitRoleMap().iterator().next()
                .setRoles(Set.of(roles.getRoleByName(Constants.ROLE_RESULTS).getId(),
                        roles.getRoleByName(Constants.ROLE_VALIDATION).getId()));
        em.flush();
        var run = start(isolate);
        enter(run, "4");
        var config = org.openelisglobal.common.util.ConfigurationProperties.getInstance();
        var property = org.openelisglobal.common.util.ConfigurationProperties.Property.BLOCK_SELF_VALIDATION;
        String original = config.getPropertyValue(property);
        try {
            config.setPropertyValue(property, "true");
            assertEquals("MICROBIOLOGY_SELF_VALIDATION_BLOCKED",
                    assertThrows(AccessDeniedException.class, () -> ast.reviewRun(run.getId(), writer)).getMessage());
        } finally {
            config.setPropertyValue(property, original);
        }
    }

    @Test
    public void ntmIdentificationDoesNotAssignTheConfiguredGeneralPanel() {
        var organism = organisms.get(reference.organism().getId()).orElseThrow();
        organism.setSusceptibilityProtocol("NTM");
        organisms.update(organism);
        String ntm = isolate("ISO-NTM");
        em.flush();
        em.clear();
        assertEquals(List.of(), assignments.getAssignments(ntm));
        assertEquals("AST_PANEL_ORGANISM_MISMATCH", assertThrows(IllegalArgumentException.class,
                () -> assignments.assign(ntm, reference.panel().getId(), writer)).getMessage());
    }

    @Test
    public void reportContainsOnlyExplicitlyExternalNotes() {
        timeline.addNote(c.getId(), "Internal bench observation", false, writer);
        timeline.addNote(c.getId(), "Communicate to clinician", true, writer);
        em.flush();
        em.clear();
        assertEquals(List.of("Communicate to clinician"),
                reports.preview(c.getId()).notes.stream().map(n -> n.text()).toList());
    }

    @Test
    public void wrongUnitCannotSelectReading() {
        var run = start(isolate);
        var values = enter(run, "4");
        ast.reviewRun(run.getId(), validator);
        assertThrows(AccessDeniedException.class, () -> choices.select(isolate, reference.antibiotic().getId(),
                values.get(0).getId(), "Wrong unit", outsider));
    }

    @Test
    public void finalCaseRejectsRawResultMutation() {
        var run = start(isolate);
        c.setFinalReleaseState("FINAL_RELEASED");
        cases.update(c);
        em.flush();
        assertThrows(MicroCaseLockedException.class,
                () -> ast.recordReading(run.getId(), drugB, new BigDecimal("4"), writer));
    }

    @Test
    public void resolvingADrugWithoutDstEvidenceIsAConflict() {
        var request = new MicroDstForm.ResolutionRequest();
        request.antibioticId = drugB;
        request.evidenceFingerprint = "none";
        request.interpretation = "RESISTANT";
        request.reason = "Reference laboratory result";
        assertEquals("DST_EVIDENCE_CHANGED",
                assertThrows(MicroAstConflictException.class, () -> dst.resolve(c.getId(), request, validator))
                        .getMessage());
    }

    @Test
    public void anOpenAstRunBlocksReleaseUntilCancelledAndStaysInTheHistory() {
        var first = start(isolate);
        enter(first, "4");
        ast.reviewRun(first.getId(), validator);
        var mistake = ast.startRepeatRun(first.getId(), MicroAstAttemptType.RETEST, "Second attempt",
                MicroAstTechnique.BROTH_MICRODILUTION, writer);
        em.flush();
        assertTrue(readiness.getReadiness(c.getId()).blockers.contains("AST_RUN_OPEN"));

        var cancel = new MicroAstWorkspaceForm.Action();
        cancel.isolateId = isolate;
        cancel.runId = mistake.getId();
        cancel.action = "cancel";
        cancel.reason = " ";
        assertThrows(IllegalArgumentException.class, () -> workspace.act(c.getId(), cancel, writer, false));
        cancel.reason = "Started on the wrong isolate";
        var form = workspace.act(c.getId(), cancel, writer, false);
        em.flush();
        assertFalse(readiness.getReadiness(c.getId()).blockers.contains("AST_RUN_OPEN"));
        assertEquals("CANCELLED", form.isolates.stream().flatMap(i -> i.runs().stream())
                .filter(r -> r.id.equals(mistake.getId())).findFirst().orElseThrow().status);
        assertTrue(
                activities.getByCaseId(c.getId()).stream().anyMatch(a -> "AST_RUN_CANCELLED".equals(a.getActivityType())
                        && "Started on the wrong isolate".equals(a.getNote())));

        var withReading = ast.startRepeatRun(first.getId(), MicroAstAttemptType.RETEST, "Third attempt",
                MicroAstTechnique.BROTH_MICRODILUTION, writer);
        ast.recordReading(withReading.getId(), drugB, new BigDecimal("4"), writer);
        cancel.runId = withReading.getId();
        assertEquals("AST_RUN_CANNOT_BE_CANCELLED",
                assertThrows(MicroAstConflictException.class, () -> workspace.act(c.getId(), cancel, writer, false))
                        .getMessage());
    }

    @Test
    public void aCaseWithNothingToReportCannotBeReleased() {
        var source = fixtures.createSampleWithSampleItem("M6");
        var empty = new MicroCase();
        empty.setSampleId(source.getSample().getId());
        empty.setSampleItemId(source.getId());
        empty.setLabUnitId(c.getLabUnitId());
        empty.setCreatedBy(writer);
        cases.insert(empty);
        em.flush();
        var blocked = readiness.getReadiness(empty.getId());
        assertFalse(blocked.finalReleaseReady);
        assertTrue(blocked.blockers.contains("REPORTABLE_WORK_REQUIRED"));
    }

    @Test
    public void untickedAgentLeavesThePatientReportButStaysSelectedForWhonet() {
        var run = start(isolate);
        var entered = enter(run, "4");
        ast.reviewRun(run.getId(), validator);
        ast.setReported(run.getId(), drugB, false, validator);
        em.flush();
        em.clear();
        var row = reports.preview(c.getId()).isolates.get(0);
        assertEquals(2, row.tested());
        assertEquals(List.of(reference.antibiotic().getId()),
                row.susceptibilities().stream().map(r -> r.antibioticId()).toList());
        assertEquals(Set.of(entered.get(0).getId(), entered.get(1).getId()),
                readings.getSelectedByRunIds(List.of(run.getId())).stream().map(MicroAstReading::getId)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    public void reportOnlyIfResistantStartsUntickedAndTicksOnAResistantReading() {
        reportingRule(reference.organism().getId(), null, List.of(), "SUPPRESS_UNLESS_RESISTANT");
        var susceptible = start(isolate);
        assertFalse(agent(susceptible, drugB).isReported());
        enter(susceptible, "4");
        assertFalse(agent(susceptible, drugB).isReported());
        var resistant = start(isolate("ISO-2"));
        enter(resistant, "64");
        assertTrue(agent(resistant, drugB).isReported());
        assertTrue(agent(resistant, reference.antibiotic().getId()).isReported());
    }

    @Test
    public void aDrugNoRuleMatchesIsReportedOnAPanelSavedFromTheEditor() {
        var drug = new org.openelisglobal.microbiology.form.MicroAstPanelAntibioticAdminForm();
        drug.antibioticId = drugB;
        var form = new org.openelisglobal.microbiology.form.MicroAstPanelAdminForm();
        form.name = "Editor panel " + UUID.randomUUID();
        form.antibiotics = List.of(drug);
        var panel = referenceAdmin.createPanel(form, writer);
        var run = ast.startRun(isolate, panel.id, reference.standard().getId(), "Editor panel", List.of(), writer);
        assertTrue(agent(run, drugB).isReported());
        assertEquals("ALWAYS", agent(run, drugB).getReportBehavior());
    }

    @Test
    public void manualUntickSurvivesALaterReadingAndResetRestoresTheDefault() {
        var run = start(isolate);
        ast.setReported(run.getId(), drugB, false, writer);
        enter(run, "64");
        assertFalse(agent(run, drugB).isReported());
        assertTrue(agent(run, drugB).isReportManual());
        assertTrue(
                timeline.getTimeline(c.getId()).stream().anyMatch(a -> "REPORT_CHOICE_CHANGED".equals(a.activityType)));
        ast.resetReportDefaults(isolate, writer);
        assertTrue(agent(run, drugB).isReported());
        assertFalse(agent(run, drugB).isReportManual());
    }

    @Test
    public void repeatRunKeepsAManualTick() {
        var first = start(isolate);
        enter(first, "4");
        ast.setReported(first.getId(), drugB, false, writer);
        ast.reviewRun(first.getId(), validator);
        var repeat = ast.startRepeatRun(first.getId(), MicroAstAttemptType.RETEST, "Confirm second agent",
                MicroAstTechnique.BROTH_MICRODILUTION, writer);
        assertFalse(agent(repeat, drugB).isReported());
        assertTrue(agent(repeat, drugB).isReportManual());
    }

    @Test
    public void reportTickNeedsRightsInTheCaseLabUnit() {
        var run = start(isolate);
        assertThrows(AccessDeniedException.class, () -> ast.setReported(run.getId(), drugB, false, outsider));
    }

    @Test
    public void finalCaseRejectsReportTickChange() {
        var run = start(isolate);
        c.setFinalReleaseState("FINAL_RELEASED");
        cases.update(c);
        em.flush();
        assertThrows(MicroCaseLockedException.class, () -> ast.setReported(run.getId(), drugB, false, writer));
    }

    private MicroAstRunAntibiotic agent(MicroAstRun run, String antibioticId) {
        em.flush();
        em.clear();
        return runAntibiotics.getByRunIdAndAntibioticId(run.getId(), antibioticId).orElseThrow();
    }

    @Test
    public void dontReportRuleLeavesTheDrugUntickedButKeepsItForWhonet() {
        var dontReport = reportingRule(reference.organism().getId(), null, List.of(), "DONT_REPORT");
        var run = start(isolate);
        enter(run, "4");
        assertFalse(agent(run, drugB).isReported());
        assertEquals(dontReport.getId(), agent(run, drugB).getReportRuleId());
        assertEquals("DONT_REPORT", agent(run, drugB).getReportBehavior());
        ast.reviewRun(run.getId(), validator);
        em.flush();
        em.clear();
        assertEquals(List.of(reference.antibiotic().getId()), reports.preview(c.getId()).isolates.get(0)
                .susceptibilities().stream().map(r -> r.antibioticId()).toList());
        assertTrue(readings.getSelectedByRunIds(List.of(run.getId())).stream()
                .anyMatch(r -> drugB.equals(r.getAntibioticId())));
    }

    @Test
    public void sampleTypeRuleBeatsAnySampleTypeRule() {
        groupOrganism("Enterobacterales");
        reportingRule(null, "Enterobacterales", List.of(), "DONT_REPORT");
        reportingRule(null, "Enterobacterales", List.of(sampleTypeId), "ALWAYS");
        assertTrue(agent(start(isolate), drugB).isReported());
    }

    @Test
    public void ruleForAnotherSampleTypeDoesNotApply() {
        groupOrganism("Enterobacterales");
        reportingRule(null, "Enterobacterales", List.of(), "DONT_REPORT");
        reportingRule(null, "Enterobacterales", List.of(fixtures.createTypeOfSample().getId()), "ALWAYS");
        assertFalse(agent(start(isolate), drugB).isReported());
    }

    @Test
    public void organismRuleBeatsGroupRule() {
        groupOrganism("Enterobacterales");
        reportingRule(null, "Enterobacterales", List.of(), "DONT_REPORT");
        reportingRule(reference.organism().getId(), null, List.of(), "ALWAYS");
        assertTrue(agent(start(isolate), drugB).isReported());
    }

    @Test
    public void indicationRuleAppliesOnlyToRunsWithThatIndication() {
        var form = reportingRuleForm(reference.organism().getId(), null, List.of(), "DONT_REPORT");
        form.clinicalContext = "NON_MENINGITIS";
        reportingRules.save(form, writer);
        assertTrue(agent(start(isolate), drugB).isReported());
    }

    @Test
    public void editingARuleUpdatesUnreleasedRunsButNotHandChangedTicksAndKeepsHistory() {
        var saved = reportingRule(reference.organism().getId(), null, List.of(), "ALWAYS");
        var run = start(isolate);
        var other = start(isolate("ISO-2"));
        ast.setReported(other.getId(), drugB, true, writer);
        var edit = reportingRuleForm(reference.organism().getId(), null, List.of(), "DONT_REPORT");
        edit.id = saved.getId();
        reportingRules.save(edit, validator);
        assertFalse(agent(run, drugB).isReported());
        assertTrue(agent(other, drugB).isReported());
        var history = reportingRules.getHistory(saved.getId());
        assertEquals(2, history.size());
        assertEquals(validator, history.get(0).changedBy);
        assertTrue(history.get(0).before.contains("ALWAYS"));
        assertTrue(history.get(0).after.contains("DONT_REPORT"));
        assertNull(history.get(1).before);
    }

    @Test
    public void reportingRuleTargetsAnOrganismOrAGroupNotBoth() {
        assertThrows(IllegalArgumentException.class,
                () -> reportingRule(reference.organism().getId(), "Enterobacterales", List.of(), "ALWAYS"));
        assertThrows(IllegalArgumentException.class,
                () -> reportingRule(reference.organism().getId(), null, List.of(), "SOMETIMES"));
    }

    @Test
    public void reportingRulesRecordWhichStandardTheyWereWrittenFor() {
        assertEquals("EUCAST", reportingRules.saveSettings("EUCAST", writer).writtenFor);
        em.flush();
        em.clear();
        assertEquals("EUCAST", reportingRules.getSettings().writtenFor);
        assertThrows(IllegalArgumentException.class, () -> reportingRules.saveSettings("WHONET", writer));
    }

    @Test
    public void listedRulesCarryDisplayNames() {
        reportingRule(reference.organism().getId(), null, List.of(sampleTypeId), "ALWAYS");
        var listed = reportingRules.getRules().get(0);
        assertEquals("Second agent", listed.antibioticName);
        assertEquals(reference.organism().getDisplayName(), listed.organismName);
        assertEquals(1, listed.sampleTypeNames.size());
    }

    @Test
    public void ruleHistoryNamesWhoChangedIt() {
        var saved = reportingRule(reference.organism().getId(), null, List.of(), "ALWAYS");
        assertTrue(reportingRules.getHistory(saved.getId()).get(0).changedByName.contains(Constants.ROLE_RESULTS));
    }

    @Test
    public void resetAfterARuleChangeUsesTheNewRule() {
        var saved = reportingRule(reference.organism().getId(), null, List.of(), "ALWAYS");
        var run = start(isolate);
        ast.setReported(run.getId(), drugB, true, writer);
        var edit = reportingRuleForm(reference.organism().getId(), null, List.of(), "DONT_REPORT");
        edit.id = saved.getId();
        reportingRules.save(edit, writer);
        assertTrue(agent(run, drugB).isReported());
        assertEquals("DONT_REPORT", agent(run, drugB).getReportBehavior());
        ast.resetReportDefaults(isolate, writer);
        assertFalse(agent(run, drugB).isReported());
    }

    @Test
    public void blankReportDefaultIsRejectedNotACrash() {
        assertThrows(IllegalArgumentException.class,
                () -> reportingRule(reference.organism().getId(), null, List.of(), ""));
        assertThrows(IllegalArgumentException.class,
                () -> reportingRule(reference.organism().getId(), null, List.of(), null));
    }

    @Test
    public void blankWrittenForClearsTheSetting() {
        reportingRules.saveSettings("EUCAST", writer);
        assertNull(reportingRules.saveSettings("", writer).writtenFor);
        em.flush();
        em.clear();
        assertNull(reportingRules.getSettings().writtenFor);
    }

    @Test
    public void savingAnUnchangedRuleAddsNoHistory() {
        var saved = reportingRule(reference.organism().getId(), null, List.of(), "ALWAYS");
        var same = reportingRuleForm(reference.organism().getId(), null, List.of(), "ALWAYS");
        same.id = saved.getId();
        reportingRules.save(same, writer);
        assertEquals(1, reportingRules.getHistory(saved.getId()).size());
    }

    @Test
    public void leavingCascadeDropsItsCondition() {
        var form = reportingRuleForm(reference.organism().getId(), null, List.of(), "CASCADE");
        form.cascadeCondition = "Report if ceftriaxone is resistant";
        form.addAutomatically = true;
        var saved = reportingRules.save(form, writer);
        form.id = saved.getId();
        form.reportBehavior = "ALWAYS";
        var report = reportingRules.save(form, writer);
        assertNull(report.getCascadeCondition());
        assertFalse(report.isAddAutomatically());
    }

    @Test
    public void equallySpecificRulesResolveTheSameWayEveryTime() {
        var later = ruleWithId("ffffffff-ffff-ffff-ffff-ffffffffffff", "DONT_REPORT");
        var earlier = ruleWithId("00000000-0000-0000-0000-000000000000", "ALWAYS");
        var run = start(isolate);
        assertEquals(earlier.getId(), agent(run, drugB).getReportRuleId());
        assertTrue(agent(run, drugB).isReported());
        assertNotEquals(later.getId(), agent(run, drugB).getReportRuleId());
    }

    @Test
    public void aRuleEditThatChangesTicksRecordsOneActivityPerCase() {
        var saved = reportingRule(reference.organism().getId(), null, List.of(), "ALWAYS");
        start(isolate);
        start(isolate("ISO-2"));
        var edit = reportingRuleForm(reference.organism().getId(), null, List.of(), "DONT_REPORT");
        edit.id = saved.getId();
        reportingRules.save(edit, validator);
        em.flush();
        assertEquals(1, activities.getByCaseId(c.getId()).stream()
                .filter(a -> MicroCaseActivityType.REPORT_CHOICE_CHANGED.name().equals(a.getActivityType())).count());
    }

    @Test
    public void reidentifyingTheIsolateReappliesTheReportingRules() {
        var acinetobacter = fixtures.createOrganism("Acinetobacter " + UUID.randomUUID(), "ACB", null);
        reportingRule(acinetobacter.getId(), null, List.of(), "DONT_REPORT");
        var run = start(isolate);
        assertTrue(agent(run, drugB).isReported());
        isolateService.updateIdentification(isolate, acinetobacter.getId(), null,
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                fixtures.createMethodId(), null, "Corrected identification",
                new Timestamp(System.currentTimeMillis() - 1000), validator);
        assertFalse(agent(run, drugB).isReported());
        assertEquals("DONT_REPORT", agent(run, drugB).getReportBehavior());
    }

    private MicroReportingRule ruleWithId(String id, String reportBehavior) {
        var rule = new MicroReportingRule();
        rule.setId(id);
        rule.setAntibioticId(drugB);
        rule.setOrganismId(reference.organism().getId());
        rule.setReportBehavior(reportBehavior);
        em.persist(rule);
        em.flush();
        return rule;
    }

    @Test
    public void ruleNoteWhenResistantPrintsUnderAResistantDrugOnly() {
        var form = reportingRuleForm(reference.organism().getId(), null, List.of(), "ALWAYS");
        form.note = "Consider meropenem for this resistant isolate";
        form.noteWhen = "WHEN_R";
        reportingRules.save(form, writer);
        var resistant = start(isolate);
        var resistantReadings = enter(resistant, "64");
        ast.reviewRun(resistant.getId(), validator);
        var susceptible = start(isolate("ISO-2"));
        enter(susceptible, "4");
        ast.reviewRun(susceptible.getId(), validator);
        em.flush();
        em.clear();
        var notes = reports.preview(c.getId()).notes.stream().filter(n -> n.fromRule()).toList();
        assertEquals(1, notes.size());
        assertEquals("Consider meropenem for this resistant isolate", notes.get(0).text());
        assertEquals("DRUG", notes.get(0).targetType());
        assertEquals(resistantReadings.get(1).getId(), notes.get(0).targetId());
    }

    @Test
    public void ruleNoteIsNotPrintedForAnUntickedDrug() {
        var form = reportingRuleForm(reference.organism().getId(), null, List.of(), "DONT_REPORT");
        form.note = "Not printed";
        form.noteWhen = "ALWAYS";
        reportingRules.save(form, writer);
        var run = start(isolate);
        enter(run, "64");
        ast.reviewRun(run.getId(), validator);
        em.flush();
        em.clear();
        assertTrue(reports.preview(c.getId()).notes.stream().noneMatch(n -> n.fromRule()));
    }

    @Test
    public void ruleNoteFollowsItsRuleAndShowsOnTheRunDrug() {
        var form = reportingRuleForm(reference.organism().getId(), null, List.of(), "ALWAYS");
        form.note = "First wording";
        form.noteWhen = "ALWAYS";
        var saved = reportingRules.save(form, writer);
        var run = start(isolate);
        enter(run, "4");
        form.id = saved.getId();
        form.note = "Second wording";
        reportingRules.save(form, writer);
        em.flush();
        em.clear();
        var drug = astResponses.toRunFormWithReadings(runs.get(run.getId()).orElseThrow()).orderedAntibiotics.stream()
                .filter(a -> drugB.equals(a.antibioticId)).findFirst().orElseThrow();
        assertEquals("Second wording", drug.ruleNote);
    }

    @Test
    public void ruleNoteNeedsAKnownCondition() {
        var form = reportingRuleForm(reference.organism().getId(), null, List.of(), "ALWAYS");
        form.note = "Sometimes";
        form.noteWhen = "SOMETIMES";
        assertEquals("REPORTING_RULE_NOTE_WHEN_INVALID",
                assertThrows(IllegalArgumentException.class, () -> reportingRules.save(form, writer)).getMessage());
        form.noteWhen = null;
        assertEquals("REPORTING_RULE_NOTE_WHEN_INVALID",
                assertThrows(IllegalArgumentException.class, () -> reportingRules.save(form, writer)).getMessage());
    }

    @Test
    public void releasedCaseShowsNoCurrentRuleNote() {
        var form = reportingRuleForm(reference.organism().getId(), null, List.of(), "ALWAYS");
        form.note = "Current wording";
        form.noteWhen = "ALWAYS";
        reportingRules.save(form, writer);
        var run = start(isolate);
        enter(run, "4");
        c.setFinalReleaseState("FINAL_RELEASED");
        cases.update(c);
        em.flush();
        em.clear();
        var drug = astResponses.toRunFormWithReadings(runs.get(run.getId()).orElseThrow()).orderedAntibiotics.stream()
                .filter(a -> drugB.equals(a.antibioticId)).findFirst().orElseThrow();
        assertNull(drug.ruleNote);
    }

    @Test
    public void ruleNotePrintsWithoutAnAuthor() {
        var form = reportingRuleForm(reference.organism().getId(), null, List.of(), "ALWAYS");
        form.note = "Interpret with the clinical picture";
        form.noteWhen = "ALWAYS";
        reportingRules.save(form, writer);
        var run = start(isolate);
        enter(run, "4");
        ast.reviewRun(run.getId(), validator);
        em.flush();
        em.clear();
        assertTrue(MicroPatientReportRows.of(reports.preview(c.getId())).stream()
                .anyMatch(row -> "Interpret with the clinical picture".equals(row.getText())));
    }

    @Test
    public void ruleNoteLongerThanAThousandCharactersIsRejected() {
        var form = reportingRuleForm(reference.organism().getId(), null, List.of(), "ALWAYS");
        form.note = "x".repeat(1001);
        form.noteWhen = "ALWAYS";
        var rejected = assertThrows(IllegalArgumentException.class, () -> reportingRules.save(form, writer));
        assertEquals("REPORTING_RULE_NOTE_TOO_LONG", rejected.getMessage());
    }

    @Test
    public void ruleTurnedOffAfterReleaseShowsNoNoteOnAnAmendment() {
        var form = reportingRuleForm(reference.organism().getId(), null, List.of(), "ALWAYS");
        form.note = "Old advice";
        form.noteWhen = "ALWAYS";
        var saved = reportingRules.save(form, writer);
        var run = start(isolate);
        enter(run, "4");
        ast.reviewRun(run.getId(), validator);
        c.setFinalReleaseState("FINAL_RELEASED");
        cases.update(c);
        em.flush();
        form.id = saved.getId();
        form.active = false;
        reportingRules.save(form, writer);
        c.setFinalReleaseState("AMENDMENT_IN_PROGRESS");
        cases.update(c);
        em.flush();
        em.clear();
        assertTrue(reports.preview(c.getId()).notes.stream().noneMatch(n -> n.fromRule()));
        var drug = astResponses.toRunFormWithReadings(runs.get(run.getId()).orElseThrow()).orderedAntibiotics.stream()
                .filter(a -> drugB.equals(a.antibioticId)).findFirst().orElseThrow();
        assertNull(drug.ruleNote);
    }

    @Test
    public void alwaysResistantRuleOverridesAMeasuredSusceptibleReadingAndBlocksReviewUntilChecked() {
        var rule = alwaysResistant(reference.organism().getId(), null);
        var run = start(isolate);
        var entered = enter(run, "4");
        var reading = readings.get(entered.get(1).getId()).orElseThrow();
        assertEquals("SUSCEPTIBLE", reading.getInterpretation());
        assertEquals("RESISTANT", reading.getOverrideInterpretation());
        assertEquals(rule.getId(), reading.getOverrideRuleId());
        var blocked = assertThrows(MicroAstConflictException.class, () -> ast.reviewRun(run.getId(), validator));
        assertEquals("AST_INTRINSIC_RESISTANCE_UNVERIFIED", blocked.getMessage());
        ast.acknowledgeAnalyzerFlags(run.getId(), "Identification confirmed", validator);
        ast.reviewRun(run.getId(), validator);
        em.flush();
        em.clear();
        var printed = reports.preview(c.getId()).isolates.get(0).susceptibilities().stream()
                .filter(r -> drugB.equals(r.antibioticId())).findFirst().orElseThrow();
        assertEquals("RESISTANT", printed.interpretation());
    }

    @Test
    public void ruleOverrideIsRecordedAsAnExpertRuleInTheOverrideHistory() {
        var rule = alwaysResistant(reference.organism().getId(), null);
        var run = start(isolate);
        enter(run, "4");
        var history = ast.getOverrideHistoryForRun(run.getId());
        assertEquals(1, history.size());
        assertEquals(rule.getId(), history.get(0).ruleId);
    }

    @Test
    public void validatorCanRevertAnExpertRuleOverride() {
        alwaysResistant(reference.organism().getId(), null);
        var run = start(isolate);
        var entered = enter(run, "4");
        ast.revertOverride(entered.get(1).getId(), "Confirmed susceptible by the reference laboratory", validator);
        em.flush();
        em.clear();
        var reading = readings.get(entered.get(1).getId()).orElseThrow();
        assertNull(reading.getOverrideInterpretation());
        assertNull(reading.getOverrideRuleId());
    }

    @Test
    public void groupRuleOverridesASusceptibleReadingButLeavesAResistantOneAlone() {
        groupOrganism("Enterobacterales");
        alwaysResistant(null, "Enterobacterales");
        var susceptible = enter(start(isolate), "4");
        assertEquals("RESISTANT", readings.get(susceptible.get(1).getId()).orElseThrow().getOverrideInterpretation());
        var resistantRun = start(isolate("ISO-2"));
        var resistant = enter(resistantRun, "64");
        assertNull(readings.get(resistant.get(1).getId()).orElseThrow().getOverrideInterpretation());
        ast.reviewRun(resistantRun.getId(), validator);
    }

    @Test
    public void anOrganismInTwoLabGroupsGetsTheRulesOfBoth() {
        groupOrganism("Klebsiella spp.");
        groupOrganism("Enterobacterales non-ESCPPM");
        var cascade = cascadeFor("Enterobacterales non-ESCPPM");
        alwaysResistantOn(reference.antibiotic().getId(), "Klebsiella spp.");

        var result = previewFor(reference.organism().getId());

        assertEquals(cascade.getId(), row(result, drugB).reportRuleId);
        assertEquals("CASCADE", row(result, drugB).reportBehavior);
        assertEquals("RESISTANT", row(result, reference.antibiotic().getId()).interpretation);
    }

    @Test
    public void anOrganismOnlyInTheSharedGroupDoesNotGetTheOtherGroupsAlwaysResistantRule() {
        groupOrganism("Klebsiella spp.");
        groupOrganism("Enterobacterales non-ESCPPM");
        var coli = new MicroOrganism();
        coli.setDisplayName("Escherichia coli " + UUID.randomUUID().toString().substring(0, 8));
        coli.setWhonetCode("E" + UUID.randomUUID().toString().substring(0, 7));
        coli.getLabGroups().add("Enterobacterales non-ESCPPM");
        organisms.insert(coli);
        var cascade = cascadeFor("Enterobacterales non-ESCPPM");
        alwaysResistantOn(reference.antibiotic().getId(), "Klebsiella spp.");

        var result = previewFor(coli.getId());

        assertEquals(cascade.getId(), row(result, drugB).reportRuleId);
        assertEquals("SUSCEPTIBLE", row(result, reference.antibiotic().getId()).interpretation);
        assertNull(row(result, reference.antibiotic().getId()).expertRule);
    }

    @Test
    public void aRuleForThePackagesOrganismGroupAloneNoLongerApplies() {
        reference.organism().setOrganismGroup("Enterobacterales");
        organisms.update(reference.organism());
        em.flush();
        reportingRule(null, "Enterobacterales", List.of(), "DONT_REPORT");
        alwaysResistant(null, "Enterobacterales");

        assertTrue(agent(start(isolate), drugB).isReported());
        var result = previewFor(reference.organism().getId());
        assertNull(row(result, drugB).reportRuleId);
        assertEquals("SUSCEPTIBLE", row(result, drugB).interpretation);
    }

    @Test
    public void interpretationRuleTargetsAnOrganismOrAGroup() {
        assertThrows(IllegalArgumentException.class, () -> alwaysResistant(null, null));
        assertThrows(IllegalArgumentException.class,
                () -> alwaysResistant(reference.organism().getId(), "Enterobacterales"));
    }

    @Test
    public void expertRuleOverrideIsOnTheCaseTimeline() {
        alwaysResistant(reference.organism().getId(), null);
        enter(start(isolate), "4");
        assertTrue(
                timeline.getTimeline(c.getId()).stream().anyMatch(a -> "AST_READING_OVERRIDDEN".equals(a.activityType)
                        && a.note != null && a.note.startsWith("Expert rule")));
    }

    @Test
    public void aValidatorOverrideReplacesTheExpertRuleOverride() {
        alwaysResistant(reference.organism().getId(), null);
        var run = start(isolate);
        var entered = enter(run, "4");
        ast.overrideReading(entered.get(1).getId(), MicroAstInterpretation.SUSCEPTIBLE,
                "Reference laboratory confirmed susceptible", validator);
        em.flush();
        em.clear();
        assertNull(readings.get(entered.get(1).getId()).orElseThrow().getOverrideRuleId());
        ast.reviewRun(run.getId(), validator);
    }

    @Test
    public void aContradictionAfterTheIdentificationWasConfirmedBlocksReviewAgain() {
        var run = start(isolate);
        enter(run, "4");
        ast.acknowledgeAnalyzerFlags(run.getId(), "Identification confirmed", validator);
        alwaysResistant(reference.organism().getId(), null);
        ast.recordReading(run.getId(), drugB, new BigDecimal("4"), writer);
        var blocked = assertThrows(MicroAstConflictException.class, () -> ast.reviewRun(run.getId(), validator));
        assertEquals("AST_INTRINSIC_RESISTANCE_UNVERIFIED", blocked.getMessage());
    }

    @Test
    public void reidentifyingRetiresTheRunAndTheRepeatGetsTheNewOrganismsRules() {
        var klebsiella = fixtures.createOrganism("Klebsiella " + UUID.randomUUID(), "Enterobacterales",
                reference.panel().getId());
        alwaysResistant(klebsiella.getId(), null);
        var first = start(isolate);
        var entered = enter(first, "4");
        reidentify(klebsiella.getId());
        assertEquals("RERUN_REQUIRED", runs.get(first.getId()).orElseThrow().getStatus());
        assertNull(readings.get(entered.get(1).getId()).orElseThrow().getOverrideInterpretation());
        var repeat = enter(start(isolate), "4");
        assertEquals("RESISTANT", readings.get(repeat.get(1).getId()).orElseThrow().getOverrideInterpretation());
    }

    @Test
    public void reidentifyingAwayFromTheRulesOrganismLeavesTheRetiredRunsOverride() {
        var escherichia = fixtures.createOrganism("Escherichia " + UUID.randomUUID(), "Enterobacterales",
                reference.panel().getId());
        var rule = alwaysResistant(reference.organism().getId(), null);
        var first = start(isolate);
        var entered = enter(first, "4");
        reidentify(escherichia.getId());
        assertEquals("RERUN_REQUIRED", runs.get(first.getId()).orElseThrow().getStatus());
        var retired = readings.get(entered.get(1).getId()).orElseThrow();
        assertEquals("RESISTANT", retired.getOverrideInterpretation());
        assertEquals(rule.getId(), retired.getOverrideRuleId());
        var repeat = readings.get(enter(start(isolate), "4").get(1).getId()).orElseThrow();
        assertNull(repeat.getOverrideInterpretation());
        assertNull(repeat.getOverrideRuleId());
    }

    @Test
    public void identifyingAnIsolateWithNoOrganismAppliesTheRulesToItsOpenRun() {
        var klebsiella = fixtures.createOrganism("Klebsiella " + UUID.randomUUID(), "Enterobacterales",
                reference.panel().getId());
        alwaysResistant(klebsiella.getId(), null);
        var run = start(isolate);
        var entered = enter(run, "4");
        // An amendment revert can restore a null organism while the run survives.
        em.createQuery("update MicroIsolate i set i.organismId = null where i.id = :id").setParameter("id", isolate)
                .executeUpdate();
        em.flush();
        em.clear();
        var status = runs.get(run.getId()).orElseThrow().getStatus();
        reidentify(klebsiella.getId());
        assertEquals(status, runs.get(run.getId()).orElseThrow().getStatus());
        assertEquals("RESISTANT", readings.get(entered.get(1).getId()).orElseThrow().getOverrideInterpretation());
    }

    private void reidentify(String organismId) {
        reidentify(isolate, organismId);
    }

    private void reidentify(String isolateId, String organismId) {
        isolateService.updateIdentification(isolateId, organismId, null,
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                fixtures.createMethodId(), null, "Corrected identification",
                new Timestamp(System.currentTimeMillis() - 1000), validator);
        em.flush();
        em.clear();
    }

    private MicroInterpretationRule alwaysResistant(String organismId, String organismGroup) {
        var form = new org.openelisglobal.microbiology.form.MicroInterpretationRuleForm();
        form.antibioticId = drugB;
        form.organismId = organismId;
        form.organismGroup = organismGroup;
        form.interpretation = "RESISTANT";
        form.source = "EUCAST expected phenotypes";
        var saved = interpretationRules.save(form, writer);
        em.flush();
        return saved;
    }

    @Test
    public void previewShowsHowEachDrugWouldBeInterpretedAndReported() {
        alwaysResistant(reference.organism().getId(), null);
        var form = reportingRuleForm(reference.organism().getId(), null, List.of(), "SUPPRESS_UNLESS_RESISTANT");
        form.note = "Consider meropenem";
        form.noteWhen = "WHEN_R";
        var rule = reportingRules.save(form, writer);
        var request = new org.openelisglobal.microbiology.form.MicroReportingRulePreviewForm();
        request.organismId = reference.organism().getId();
        request.sampleTypeId = sampleTypeId;
        request.readings = List.of(previewReading(drugB, "SUSCEPTIBLE"),
                previewReading(reference.antibiotic().getId(), "SUSCEPTIBLE"));
        var result = ast.previewReportingRules(request);
        assertEquals(reference.panel().getName(), result.defaultPanelName);
        var expert = result.rows.stream().filter(r -> drugB.equals(r.antibioticId)).findFirst().orElseThrow();
        assertEquals("SUSCEPTIBLE", expert.measured);
        assertEquals("RESISTANT", expert.interpretation);
        assertEquals("Expert rule: " + reference.organism().getDisplayName() + ", Second agent always R",
                expert.expertRule);
        assertEquals(rule.getId(), expert.reportRuleId);
        assertTrue(expert.reported);
        assertEquals("Consider meropenem", expert.note);
        var plain = result.rows.stream().filter(r -> reference.antibiotic().getId().equals(r.antibioticId)).findFirst()
                .orElseThrow();
        assertEquals("SUSCEPTIBLE", plain.interpretation);
        assertNull(plain.expertRule);
        assertNull(plain.reportRuleId);
        assertTrue(plain.reported);
        assertNull(plain.note);
    }

    @Test
    public void previewIgnoresTheDefaultPanelForAnNtmOrganismAsRealRunsDo() {
        reference.organism().setSusceptibilityProtocol("NTM");
        organisms.update(reference.organism());
        em.flush();
        var result = ast.previewReportingRules(previewOf(thirdPanelDrug()));
        assertNull(result.defaultPanelName);
        assertEquals("ALWAYS", result.rows.get(0).reportBehavior);
        assertTrue(result.rows.get(0).reported);
    }

    private String thirdPanelDrug() {
        var drug = new MicroAntibiotic();
        drug.setDisplayName("Third agent");
        drug.setWhonetCode("C" + UUID.randomUUID().toString().substring(0, 7));
        antibiotics.insert(drug);
        var row = new MicroAstPanelAntibiotic();
        row.setPanelId(reference.panel().getId());
        row.setAntibioticId(drug.getId());
        row.setDisplayOrder(3);
        panelDrugs.insert(row);
        em.flush();
        return drug.getId();
    }

    private org.openelisglobal.microbiology.form.MicroReportingRulePreviewForm previewOf(String antibioticId) {
        var request = new org.openelisglobal.microbiology.form.MicroReportingRulePreviewForm();
        request.organismId = reference.organism().getId();
        request.sampleTypeId = sampleTypeId;
        request.readings = List.of(previewReading(antibioticId, "SUSCEPTIBLE"));
        return request;
    }

    private org.openelisglobal.microbiology.form.MicroReportingRulePreviewForm.Reading previewReading(
            String antibioticId, String interpretation) {
        var reading = new org.openelisglobal.microbiology.form.MicroReportingRulePreviewForm.Reading();
        reading.antibioticId = antibioticId;
        reading.interpretation = interpretation;
        return reading;
    }

    private org.openelisglobal.microbiology.form.MicroReportingRuleForm reportingRuleForm(String organismId,
            String organismGroup, List<String> sampleTypeIds, String reportBehavior) {
        var form = new org.openelisglobal.microbiology.form.MicroReportingRuleForm();
        form.antibioticId = drugB;
        form.organismId = organismId;
        form.organismGroup = organismGroup;
        form.sampleTypeIds = sampleTypeIds;
        form.reportBehavior = reportBehavior;
        return form;
    }

    private MicroReportingRule reportingRule(String organismId, String organismGroup, List<String> sampleTypeIds,
            String reportBehavior) {
        var saved = reportingRules.save(reportingRuleForm(organismId, organismGroup, sampleTypeIds, reportBehavior),
                writer);
        em.flush();
        return saved;
    }

    private void groupOrganism(String group) {
        reference.organism().getLabGroups().add(group);
        organisms.update(reference.organism());
        em.flush();
    }

    private MicroInterpretationRule alwaysResistantOn(String antibioticId, String organismGroup) {
        var form = new org.openelisglobal.microbiology.form.MicroInterpretationRuleForm();
        form.antibioticId = antibioticId;
        form.organismGroup = organismGroup;
        form.interpretation = "RESISTANT";
        form.source = "EUCAST expected phenotypes";
        var saved = interpretationRules.save(form, writer);
        em.flush();
        return saved;
    }

    private MicroReportingRule cascadeFor(String organismGroup) {
        var form = reportingRuleForm(null, organismGroup, List.of(), "CASCADE");
        form.cascadeCondition = "ANY_R CRO";
        var saved = reportingRules.save(form, writer);
        em.flush();
        return saved;
    }

    private org.openelisglobal.microbiology.form.MicroReportingRulePreviewResultForm previewFor(String organismId) {
        var request = new org.openelisglobal.microbiology.form.MicroReportingRulePreviewForm();
        request.organismId = organismId;
        request.sampleTypeId = sampleTypeId;
        request.readings = List.of(previewReading(drugB, "SUSCEPTIBLE"),
                previewReading(reference.antibiotic().getId(), "SUSCEPTIBLE"));
        return ast.previewReportingRules(request);
    }

    private org.openelisglobal.microbiology.form.MicroReportingRulePreviewResultForm.Row row(
            org.openelisglobal.microbiology.form.MicroReportingRulePreviewResultForm result, String antibioticId) {
        return result.rows.stream().filter(r -> antibioticId.equals(r.antibioticId)).findFirst().orElseThrow();
    }

    @Test
    public void aCaseWhoseOnlyAnalysisIsNotReportableHasNothingToReport() {
        var source = fixtures.createSampleWithSampleItem("M6");
        var onlyInternal = new MicroCase();
        onlyInternal.setSampleId(source.getSample().getId());
        onlyInternal.setSampleItemId(source.getId());
        onlyInternal.setLabUnitId(c.getLabUnitId());
        onlyInternal.setCreatedBy(writer);
        cases.insert(onlyInternal);
        var analysis = fixtures.createAnalysis(source, fixtures.createCatalogTest());
        var link = new MicroCaseAnalysis();
        link.setCaseId(onlyInternal.getId());
        link.setAnalysisId(analysis.getId());
        link.setPlacement("INITIAL");
        link.setCaseRole(MicroCaseRole.DIRECT);
        em.persist(link);
        em.find(org.openelisglobal.analysis.valueholder.Analysis.class, analysis.getId()).setIsReportable("N");
        em.flush();
        assertTrue(readiness.getReadiness(onlyInternal.getId()).blockers.contains("REPORTABLE_WORK_REQUIRED"));

        em.find(org.openelisglobal.analysis.valueholder.Analysis.class, analysis.getId()).setIsReportable("Y");
        em.flush();
        assertFalse(readiness.getReadiness(onlyInternal.getId()).blockers.contains("REPORTABLE_WORK_REQUIRED"));
    }

    @Test
    public void reidentifyingOutOfThePanelsGroupRetiresItsAssignment() {
        var klebsiella = fixtures.createOrganism("Klebsiella " + UUID.randomUUID(), "Enterobacterales",
                reference.panel().getId());
        reidentify(klebsiella.getId());
        assertEquals(List.of(reference.panel().getId()),
                assignments.getAssignments(isolate).stream().map(MicroAstPanelAssignment::getPanelId).toList());

        var staphylococcus = fixtures.createOrganism("Staphylococcus " + UUID.randomUUID(), "Staphylococci", null);
        reidentify(staphylococcus.getId());
        assertEquals(List.of(), assignments.getAssignments(isolate));
    }

    @Test
    public void aDefaultAssignmentFollowsTheNewOrganismsDefaultEvenWithoutAPanelGroup() {
        var groupless = panel(null);
        var manual = panel(null);
        reference.organism().setDefaultAstPanelId(groupless.getId());
        organisms.update(reference.organism());
        String id = isolate("ISO-G");
        assignments.assign(id, manual.getId(), writer);
        var klebsiella = fixtures.createOrganism("Klebsiella " + UUID.randomUUID(), "Enterobacterales",
                reference.panel().getId());
        reidentify(id, klebsiella.getId());
        assertEquals(Set.of(reference.panel().getId(), manual.getId()), assignments.getAssignments(id).stream()
                .map(MicroAstPanelAssignment::getPanelId).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    public void reconfirmingTheSameOrganismLeavesItsPanelsAlone() {
        reference.organism().setDefaultAstPanelId(panel("Enterobacterales").getId());
        organisms.update(reference.organism());
        reidentify(reference.organism().getId());
        assertEquals(List.of(reference.panel().getId()),
                assignments.getAssignments(isolate).stream().map(MicroAstPanelAssignment::getPanelId).toList());
    }

    @Test
    public void theWorkspaceNamesTheOrganismsCurrentDefaultPanel() {
        var current = panel("Enterobacterales");
        reference.organism().setDefaultAstPanelId(current.getId());
        organisms.update(reference.organism());
        em.flush();
        em.clear();
        var shown = workspace.get(c.getId(), writer).isolates.stream().filter(i -> i.id().equals(isolate)).findFirst()
                .orElseThrow();
        assertEquals(current.getId(), shown.defaultPanelId());
        assertEquals(List.of(reference.panel().getId()),
                shown.assignments().stream().map(MicroAstWorkspaceForm.Assignment::panelId).toList());
    }

    private MicroAstPanel panel(String organismGroup) {
        var p = new MicroAstPanel();
        p.setName("Panel " + UUID.randomUUID());
        p.setOrganismGroup(organismGroup);
        p.setPublishedAt(new Timestamp(System.currentTimeMillis()));
        p.setPublishedBy(writer);
        panels.insert(p);
        return p;
    }

    @Test
    public void aCancelledRunsAgentsCannotBeTickedForTheReport() {
        var run = start(isolate);
        cancel(run);
        boolean before = agent(run, drugB).isReported();
        assertEquals("AST_RUN_NOT_EDITABLE", assertThrows(MicroAstConflictException.class,
                () -> ast.setReported(run.getId(), drugB, !before, validator)).getMessage());
        assertEquals(before, agent(run, drugB).isReported());
    }

    @Test
    public void aCancelledAnalyzerRunRefusesLateInstrumentResults() {
        var vitek = new org.openelisglobal.analyzer.valueholder.Analyzer();
        vitek.ensureFhirUuid();
        vitek.setName("VITEK " + UUID.randomUUID());
        vitek.setStatus(org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus.SETUP);
        vitek.setSysUserId("1");
        String analyzerId = analyzers.insert(vitek);
        var run = ast.startRun(new MicroAstRunSetupCommand(isolate, null, reference.standard().getId(), null,
                MicroAstTechnique.VITEK_2, List.of(), null, true, analyzerId, "card-1"), writer);
        cancel(run);
        var late = new MicroAstAnalyzerResultBatch(run.getId(), "late-event", analyzerId, "card-1", null, null, null,
                null, List.of(), null, true, null, null, List.of(),
                List.of(new MicroAstAnalyzerReading(drugB, new BigDecimal("4"), "mg/L", null, null)));
        assertEquals("AST_RUN_NOT_AWAITING_ANALYZER_RESULTS",
                assertThrows(MicroAstConflictException.class, () -> ast.applyAnalyzerResults(late, writer))
                        .getMessage());
        em.flush();
        em.clear();
        assertEquals("CANCELLED", runs.get(run.getId()).orElseThrow().getStatus());
        assertEquals(List.of(), ast.getReadingsForRun(run.getId()));
    }

    @Test
    public void runsAndAssignmentsNameTheirPanelVersion() {
        start(isolate);
        em.flush();
        em.clear();
        var shown = workspace.get(c.getId(), writer).isolates.stream().filter(i -> i.id().equals(isolate)).findFirst()
                .orElseThrow();
        String expected = reference.panel().getName() + " v" + reference.panel().getVersionNumber();
        assertEquals(List.of(expected),
                shown.assignments().stream().map(MicroAstWorkspaceForm.Assignment::label).toList());
        assertEquals(expected, shown.runs().getFirst().panelLabel);
    }

    private void cancel(MicroAstRun run) {
        var cancel = new MicroAstWorkspaceForm.Action();
        cancel.isolateId = isolate;
        cancel.runId = run.getId();
        cancel.action = "cancel";
        cancel.reason = "Started on the wrong isolate";
        workspace.act(c.getId(), cancel, writer, false);
        em.flush();
    }
}
