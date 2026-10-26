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
    private MicroAstService ast;
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
    private org.openelisglobal.systemuser.service.SystemUserService users;
    @Autowired
    private org.openelisglobal.role.service.RoleService roles;
    @PersistenceContext
    private EntityManager em;
    private MicroCase c;
    private MicrobiologyTestFixtures.ReferenceData reference;
    private String writer, validator, outsider, isolate, drugB;
    private MicroAstPanelAntibiotic drugBPanelRow;
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
        drugBPanelRow = ordered;
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
        var i = new MicroIsolate();
        i.setCaseId(c.getId());
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
        drugBPanelRow.setReportBehavior("SUPPRESS_UNLESS_RESISTANT");
        panelDrugs.update(drugBPanelRow);
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
        reference.organism().setOrganismGroup(group);
        organisms.update(reference.organism());
        em.flush();
    }
}
