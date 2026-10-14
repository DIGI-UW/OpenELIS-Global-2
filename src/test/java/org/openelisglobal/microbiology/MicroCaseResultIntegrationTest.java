package org.openelisglobal.microbiology;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.resultvalidation.service.ResultSelfValidationPolicy;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Transactional
public class MicroCaseResultIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private MicroCaseResultService workspace;
    @Autowired
    private MicroCaseTimelineService timeline;
    @Autowired
    private MicroCaseDAO cases;
    @Autowired
    private MicroCaseAnalysisDAO links;
    @Autowired
    private MicroCaseMembershipService membership;
    @Autowired
    private AnalysisService analyses;
    @Autowired
    private ResultService results;
    @Autowired
    private TestResultComponentService components;
    @Autowired
    private SystemUserService users;
    @Autowired
    private RoleService roles;
    @Autowired
    private IStatusService statuses;
    @Autowired
    private ResultSelfValidationPolicy policy;
    @PersistenceContext
    private EntityManager em;
    private MicroCase microCase;
    private Analysis analysis;
    private String actor, validator, outsider, oldBlock;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/analysis.xml");
        executeDataSetWithStateManagement("testdata/role.xml");
        executeDataSetWithStateManagement("testdata/system-user.xml");
        fixtures.ensureRequiredWorkflowStatuses();
        if ("-1".equals(statuses.getStatusID(AnalysisStatus.TechnicalAcceptance))) {
            var status = new org.openelisglobal.statusofsample.valueholder.StatusOfSample();
            status.setStatusOfSampleName("Technical Acceptance");
            status.setDescription("Awaiting validation");
            status.setCode("95");
            status.setStatusType("ANALYSIS");
            status.setIsActive("Y");
            status.setSysUserId("1");
            em.persist(status);
            em.flush();
            statuses.refreshCache();
        }
        for (String name : List.of("Testing Started", "Testing finished")) {
            if (em.createQuery("from StatusOfSample s where s.statusType = 'ORDER' and s.statusOfSampleName = :name")
                    .setParameter("name", name).getResultList().isEmpty()) {
                var status = new org.openelisglobal.statusofsample.valueholder.StatusOfSample();
                status.setStatusOfSampleName(name);
                status.setDescription(name);
                status.setStatusType("ORDER");
                status.setCode("Testing Started".equals(name) ? "96" : "97");
                status.setIsActive("Y");
                status.setSysUserId("1");
                em.persist(status);
            }
        }
        em.flush();
        statuses.refreshCache();
        oldBlock = ConfigurationProperties.getInstance().getPropertyValue(Property.BLOCK_SELF_VALIDATION);
        ConfigurationProperties.getInstance().setPropertyValue(Property.BLOCK_SELF_VALIDATION, "true");
        var unit = fixtures.createLabUnit();
        actor = user("Writer", unit.getId(), Constants.ROLE_RESULTS, Constants.ROLE_VALIDATION);
        validator = user("Validator", unit.getId(), Constants.ROLE_VALIDATION);
        outsider = user("Outside", fixtures.createLabUnit().getId(), Constants.ROLE_RESULTS);
        var test = fixtures.createCatalogTest();
        test.setTestSection(unit);
        em.merge(test);
        components.saveSampleResults(test.getId(),
                List.of(component("PRIMARY", "MTB", true), component("RIF", "Rifampicin", false)), null, null, actor);
        var specimen = fixtures.createSampleWithSampleItem("M3");
        specimen.setTypeOfSample(fixtures.getOrCreateActiveSampleType());
        em.merge(specimen);
        analysis = fixtures.createAnalysis(specimen, test);
        analysis.setStatusId(statuses.getStatusID(AnalysisStatus.NotStarted));
        em.merge(analysis);
        microCase = new MicroCase();
        microCase.setSampleId(specimen.getSample().getId());
        microCase.setSampleItemId(specimen.getId());
        microCase.setSampleTypeId(specimen.getTypeOfSample().getId());
        microCase.setLabUnitId(unit.getId());
        microCase.setCreatedBy(actor);
        cases.insert(microCase);
        membership.addSample(microCase.getId(), specimen.getId(), actor);
        var link = new MicroCaseAnalysis();
        link.setCaseId(microCase.getId());
        link.setAnalysisId(analysis.getId());
        link.setPlacement("INITIAL");
        links.insert(link);
        em.flush();
    }

    @After
    public void restoreSetting() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.BLOCK_SELF_VALIDATION, oldBlock);
    }

    private TestResultComponent component(String code, String label, boolean primary) {
        var c = new TestResultComponent();
        c.setCode(code);
        c.setLabel(label);
        c.setIsPrimary(primary);
        c.setResultType("N");
        c.setSignificantDigits(0);
        c.setDisplayOrder(primary ? 0 : 1);
        return c;
    }

    private String user(String name, String unit, String... rights) {
        var u = new SystemUser();
        u.setLoginName("M3-" + UUID.randomUUID().toString().substring(0, 12));
        u.setFirstName(name);
        u.setLastName("M3");
        u.setIsActive("Y");
        u.setIsEmployee("Y");
        u.setSysUserId("1");
        var id = users.insert(u);
        var assignment = new org.openelisglobal.userrole.valueholder.UserLabUnitRoles();
        assignment.setId(Integer.valueOf(id));
        var map = new org.openelisglobal.userrole.valueholder.LabUnitRoleMap();
        map.setLabUnit(unit);
        Set<String> roleIds = new HashSet<>();
        for (var right : rights)
            roleIds.add(roles.getRoleByName(right).getId());
        map.setRoles(roleIds);
        assignment.setLabUnitRoleMap(Set.of(map));
        em.persist(assignment);
        return id;
    }

    private UserSessionData session(String actor) {
        var s = new UserSessionData();
        s.setSytemUserId(Integer.parseInt(actor));
        return s;
    }

    private MockHttpServletRequest request(String actor) {
        var r = new MockHttpServletRequest();
        r.setAttribute(IActionConstants.USER_SESSION_DATA, session(actor));
        return r;
    }

    private MicroCaseResultRequestForm payload() {
        var f = workspace.getTests(microCase.getId(), actor).get(0);
        var p = new MicroCaseResultRequestForm();
        p.version = f.version;
        p.components = new ArrayList<>();
        for (var row : f.components) {
            var c = new MicroCaseResultRequestForm.ResultComponent();
            c.componentId = row.getTestResultComponentId();
            c.value = p.components.isEmpty() ? "12" : "34";
            p.components.add(c);
        }
        return p;
    }

    @Test
    public void savesEveryComponentAndEditsWithoutDuplicates() {
        var p = payload();
        assertEquals(2, p.components.size());
        var saved = workspace.saveResults(microCase.getId(), analysis.getId(), p, actor, request(actor)).get(0);
        assertEquals("TechnicalAcceptance", saved.status);
        assertEquals(actor, saved.enteredBy);
        assertTrue(saved.selfValidationBlocked);
        assertFalse(saved.canValidate);
        assertEquals(Set.of("12", "34"), results.getResultsByAnalysis(analysis).stream().map(r -> r.getValue())
                .collect(java.util.stream.Collectors.toSet()));
        var edit = payload();
        edit.components.get(0).value = "56";
        workspace.saveResults(microCase.getId(), analysis.getId(), edit, actor, request(actor));
        em.flush();
        em.clear();
        var stored = results.getResultsByAnalysis(analyses.get(analysis.getId()));
        assertEquals(2, stored.size());
        assertEquals(Set.of("56", "34"),
                stored.stream().map(r -> r.getValue()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(2L, stored.stream().map(r -> r.getTestResult().getComponentId()).distinct().count());
    }

    private void configureSelection(String type) {
        var configured = components.getActiveComponentsByTestId(analysis.getTest().getId());
        var selection = configured.stream().filter(c -> !c.getIsPrimary()).findFirst().orElseThrow();
        selection.setResultType(type);
        var option = new org.openelisglobal.testresult.valueholder.TestResult();
        option.setTestResultType(type);
        option.setValue("M3 selected option");
        option.setSortOrder("1");
        option.setIsActive(true);
        components.saveSampleResults(analysis.getTest().getId(), configured, null,
                Map.of(selection.getCode(), List.of(option)), actor);
        em.flush();
    }

    private void rejectEmptySelection(String type) {
        configureSelection(type);
        var submission = payload();
        for (var input : submission.components) {
            input.value = "";
            input.multiSelectResultValues = "{}";
        }
        var failure = assertThrows(IllegalArgumentException.class,
                () -> workspace.saveResults(microCase.getId(), analysis.getId(), submission, actor, request(actor)));
        assertEquals("MICROBIOLOGY_RESULT_REQUIRED", failure.getMessage());
        assertEquals(0, results.getResultsByAnalysis(analysis).size());
        assertEquals(AnalysisStatus.NotStarted, statuses.getAnalysisStatusForID(analysis.getStatusId()));
    }

    @Test
    public void rejectsEmptyMultiselectWithoutAdvancingAnalysis() {
        rejectEmptySelection("M");
    }

    @Test
    public void rejectsEmptyCascadeWithoutAdvancingAnalysis() {
        rejectEmptySelection("C");
    }

    @Test
    public void emptySelectionCannotDeleteExistingSelections() {
        configureSelection("M");
        var loaded = workspace.getTests(microCase.getId(), actor).get(0);
        var selection = loaded.components.stream().filter(r -> "M".equals(r.getResultType())).findFirst().orElseThrow();
        var submission = payload();
        for (var input : submission.components) {
            input.value = "";
            input.multiSelectResultValues = selection.getTestResultComponentId().equals(input.componentId)
                    ? "{\"1\":\"" + selection.getDictionaryResults().get(0).getId() + "\"}"
                    : null;
        }
        workspace.saveResults(microCase.getId(), analysis.getId(), submission, actor, request(actor));
        var before = results.getResultsByAnalysis(analysis).stream().map(r -> r.getId() + ":" + r.getValue()).toList();
        assertEquals(1, before.size());
        var edit = payload();
        edit.components.forEach(input -> {
            input.value = "";
            input.multiSelectResultValues = "{\"1\":\" , \"}";
        });
        assertThrows(IllegalArgumentException.class,
                () -> workspace.saveResults(microCase.getId(), analysis.getId(), edit, actor, request(actor)));
        assertEquals(before,
                results.getResultsByAnalysis(analysis).stream().map(r -> r.getId() + ":" + r.getValue()).toList());
    }

    @Test
    public void clearingSelectionPreservesSubmittedNumericSibling() {
        configureSelection("M");
        var selection = workspace.getTests(microCase.getId(), actor).get(0).components.stream()
                .filter(r -> "M".equals(r.getResultType())).findFirst().orElseThrow();
        var submission = payload();
        var selected = submission.components.stream()
                .filter(c -> selection.getTestResultComponentId().equals(c.componentId)).findFirst().orElseThrow();
        selected.value = "";
        selected.multiSelectResultValues = "{\"1\":\"" + selection.getDictionaryResults().get(0).getId() + "\"}";
        workspace.saveResults(microCase.getId(), analysis.getId(), submission, actor, request(actor));
        assertEquals(2, results.getResultsByAnalysis(analysis).size());
        var edit = payload();
        edit.components.stream().filter(c -> selection.getTestResultComponentId().equals(c.componentId)).forEach(c -> {
            c.value = "";
            c.multiSelectResultValues = "{}";
        });
        workspace.saveResults(microCase.getId(), analysis.getId(), edit, actor, request(actor));
        var saved = results.getResultsByAnalysis(analysis);
        assertEquals(1, saved.size());
        assertEquals("12", saved.get(0).getValue());
    }

    @Test
    public void missingJsonSelectionDocumentIsAValidationError() {
        configureSelection("M");
        var submission = payload();
        submission.components.forEach(c -> {
            c.value = "";
            c.multiSelectResultValues = " ";
        });
        assertEquals("MICROBIOLOGY_INVALID_RESULT_OPTION", assertThrows(IllegalArgumentException.class,
                () -> workspace.saveResults(microCase.getId(), analysis.getId(), submission, actor, request(actor)))
                .getMessage());
        assertTrue(results.getResultsByAnalysis(analysis).isEmpty());
    }

    private void configurePrecision(int places) {
        var configured = components.getActiveComponentsByTestId(analysis.getTest().getId());
        configured.forEach(c -> c.setSignificantDigits(places));
        components.saveSampleResults(analysis.getTest().getId(), configured, null, null, actor);
        em.flush();
    }

    @Test
    public void acceptsNegativeExponentUsingMantissaPrecision() {
        configurePrecision(2);
        var submission = payload();
        submission.components.get(0).value = "1.5e-7";
        var saved = workspace.saveResults(microCase.getId(), analysis.getId(), submission, actor, request(actor));
        assertEquals("TechnicalAcceptance", saved.get(0).status);
        assertTrue(results.getResultsByAnalysis(analysis).stream().anyMatch(r -> "1.5e-7".equals(r.getValue())));
    }

    @Test
    public void rejectsExcessMantissaPrecisionEvenWithPositiveExponent() {
        configurePrecision(2);
        var submission = payload();
        submission.components.get(0).value = "1.555e7";
        assertEquals("MICROBIOLOGY_INVALID_PRECISION", assertThrows(IllegalArgumentException.class,
                () -> workspace.saveResults(microCase.getId(), analysis.getId(), submission, actor, request(actor)))
                .getMessage());
        assertTrue(results.getResultsByAnalysis(analysis).isEmpty());
    }

    @Test
    public void wholeNumberPrecisionDoesNotConstrainScientificMantissa() {
        var submission = payload();
        submission.components.get(0).value = "1.555e7";
        assertEquals("TechnicalAcceptance", workspace
                .saveResults(microCase.getId(), analysis.getId(), submission, actor, request(actor)).get(0).status);
    }

    @Test
    public void rejectsStaleAndInvalidComponentsWithoutWriting() {
        var p = payload();
        p.components.get(0).componentId = "unrelated";
        assertThrows(IllegalArgumentException.class,
                () -> workspace.saveResults(microCase.getId(), analysis.getId(), p, actor, request(actor)));
        assertEquals(0, results.getResultsByAnalysis(analysis).size());
        var stale = payload();
        stale.version = "stale";
        assertEquals(org.springframework.http.HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        () -> workspace.saveResults(microCase.getId(), analysis.getId(), stale, actor, request(actor)))
                        .getStatusCode());
    }

    @Test
    public void directReadIsReadOnlyAndWritesEnforceCurrentOwner() {
        assertFalse(workspace.getTests(microCase.getId(), outsider).get(0).canEdit);
        assertThrows(AccessDeniedException.class, () -> workspace.saveResults(microCase.getId(), analysis.getId(),
                payload(), outsider, request(outsider)));
        microCase.setLabUnitId(fixtures.createLabUnit().getId());
        cases.update(microCase);
        em.flush();
        assertThrows(AccessDeniedException.class,
                () -> workspace.saveResults(microCase.getId(), analysis.getId(), payload(), actor, request(actor)));
    }

    @Test
    public void cannotSubmitAnotherCasesAnalysis() {
        var other = new MicroCase();
        other.setSampleId(microCase.getSampleId());
        other.setLabUnitId(microCase.getLabUnitId());
        other.setCreatedBy(actor);
        cases.insert(other);
        assertThrows(ResponseStatusException.class,
                () -> workspace.saveResults(other.getId(), analysis.getId(), payload(), actor, request(actor)));
        assertEquals(0, results.getResultsByAnalysis(analysis).size());
    }

    @Test
    public void validatorMustDifferAndAcceptedResultsAreLocked() {
        var saved = workspace.saveResults(microCase.getId(), analysis.getId(), payload(), actor, request(actor)).get(0);
        assertThrows(AccessDeniedException.class,
                () -> workspace.validateResult(microCase.getId(), analysis.getId(), saved.version, actor));
        assertTrue(workspace.getTests(microCase.getId(), validator).get(0).canValidate);
        var validated = workspace.validateResult(microCase.getId(), analysis.getId(), saved.version, validator).get(0);
        assertEquals("Finalized", validated.status);
        assertFalse(validated.canEdit);
        assertThrows(ResponseStatusException.class,
                () -> workspace.saveResults(microCase.getId(), analysis.getId(), payload(), actor, request(actor)));
    }

    @Test
    public void testedElsewhereRequiresCatalogPerformerAndDate() {
        var p = new MicroCaseTestedElsewhereRequestForm();
        p.version = payload().version;
        p.testedElsewhere = true;
        assertThrows(IllegalArgumentException.class,
                () -> workspace.setTestedElsewhere(microCase.getId(), analysis.getId(), p, actor));
        p.performingUserId = validator;
        p.performedAt = "2026-10-01";
        var saved = workspace.setTestedElsewhere(microCase.getId(), analysis.getId(), p, actor).get(0);
        assertTrue(saved.testedElsewhere);
        assertEquals(validator, saved.performingUserId);
        assertEquals("2026-10-01", saved.performedAt);
        assertEquals("Validator M3", saved.performedByDisplay);
    }

    @Test
    public void caseNotesAreAttributedAndCannotWriteOutOfUnitOrAfterFinal() {
        var note = timeline.addNote(microCase.getId(), "  Bench note  ", actor);
        assertEquals("Bench note", note.note);
        assertEquals(actor, note.performedBy);
        assertEquals(List.of("Bench note"), timeline.getTimeline(microCase.getId()).stream()
                .filter(a -> "MANUAL_NOTE".equals(a.activityType)).map(a -> a.note).toList());
        assertThrows(AccessDeniedException.class, () -> timeline.addNote(microCase.getId(), "denied", outsider));
        microCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        cases.update(microCase);
        assertThrows(MicroCaseLockedException.class, () -> timeline.addNote(microCase.getId(), "locked", actor));
    }

    @Test
    public void httpAcceptsContractAndRejectsCrossUnitSubmission() throws Exception {
        var json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload());
        mockMvc.perform(
                post("/rest/microbiology/cases/" + microCase.getId() + "/analyses/" + analysis.getId() + "/results")
                        .requestAttr(IActionConstants.USER_SESSION_DATA, session(outsider))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                post("/rest/microbiology/cases/" + microCase.getId() + "/analyses/" + analysis.getId() + "/results")
                        .requestAttr(IActionConstants.USER_SESSION_DATA, session(actor))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("TechnicalAcceptance"))
                .andExpect(jsonPath("$[0].components.length()").value(2));
    }

    @Test
    public void clearingOneComponentPreservesItsSibling() {
        workspace.saveResults(microCase.getId(), analysis.getId(), payload(), actor, request(actor));
        var edit = payload();
        edit.components.get(1).value = "";
        workspace.saveResults(microCase.getId(), analysis.getId(), edit, actor, request(actor));
        em.flush();
        em.clear();
        var saved = results.getResultsByAnalysis(analyses.get(analysis.getId()));
        assertEquals(1, saved.size());
        assertEquals("12", saved.get(0).getValue());
    }

    @Test
    public void additionalSelectionUsesTheCatalogAndIsIdempotent() {
        var follow = fixtures.createCatalogTest();
        follow.setTestSection(analysis.getTestSection());
        em.merge(follow);
        components.saveSampleResults(follow.getId(), List.of(component("PRIMARY", "Follow-up", true)), null, null,
                actor);
        var mapping = new org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest();
        mapping.setTypeOfSampleId(microCase.getSampleTypeId());
        mapping.setTestId(follow.getId());
        mapping.setSysUserId(actor);
        em.persist(mapping);
        var selection = new MicroCaseAddTestsForm();
        selection.sampleItemId = microCase.getSampleItemId();
        selection.placement = "ADDITIONAL";
        selection.testIds = List.of(follow.getId());
        var saved = workspace.addTests(microCase.getId(), selection, actor);
        assertEquals(2, saved.size());
        assertEquals("ADDITIONAL",
                saved.stream().filter(t -> follow.getId().equals(t.testId)).findFirst().get().placement);
        assertEquals(2, workspace.addTests(microCase.getId(), selection, actor).size());
        assertThrows(AccessDeniedException.class, () -> workspace.addTests(microCase.getId(), selection, outsider));
    }

    @Test
    public void ordinaryValidationCannotBypassTheSameAuthorRule() {
        workspace.saveResults(microCase.getId(), analysis.getId(), payload(), actor, request(actor));
        assertThrows(AccessDeniedException.class, () -> policy.requireAnotherValidator(analysis, actor));
        policy.requireAnotherValidator(analysis, validator);
        var row = new org.openelisglobal.resultvalidation.bean.AnalysisItem();
        row.setAnalysisId(analysis.getId());
        row.setClear(true);
        policy.decorateRows(List.of(row), actor);
        assertTrue(row.isSelfValidationBlocked());
        assertFalse(row.isClear());
        policy.decorateRows(List.of(row), validator);
        assertFalse(row.isSelfValidationBlocked());
    }

}
