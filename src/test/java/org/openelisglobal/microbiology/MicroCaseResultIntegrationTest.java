package org.openelisglobal.microbiology;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.Instant;
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
    private MicroCaseWorkspaceService caseWorkspace;
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
    @Autowired
    private MicroIncomingResultService incoming;
    @Autowired
    private MicroIncomingItemDAO incomingItems;
    @Autowired
    private org.openelisglobal.analyzerresults.service.AnalyzerResultsService analyzerStaging;
    @Autowired
    private org.openelisglobal.analyzerresults.service.AnalyzerResultsAcceptService analyzerAcceptance;
    private MicroCase microCase;
    private Analysis analysis;
    private String actor, validator, outsider, oldBlock;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/analysis.xml");
        executeDataSetWithStateManagement("testdata/system-user.xml");
        executeDataSetWithStateManagement("testdata/role.xml");
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
    public void analyzerReviewAcceptanceTransfersComponentsOnlyAfterTheUserAccepts() {
        var patient = fixtures.createPatient("M7PAT");
        var human = new org.openelisglobal.samplehuman.valueholder.SampleHuman();
        human.setSampleId(analysis.getSampleItem().getSample().getId());
        human.setPatientId(patient.getId());
        human.setSysUserId(actor);
        em.persist(human);
        var analyzer = new org.openelisglobal.analyzer.valueholder.Analyzer();
        analyzer.setName("M7 acceptance " + UUID.randomUUID());
        analyzer.setActive(true);
        analyzer.setSysUserId(actor);
        em.persist(analyzer);
        var stagedRows = new ArrayList<org.openelisglobal.analyzerresults.valueholder.AnalyzerResults>();
        for (var component : payload().components) {
            var row = new org.openelisglobal.analyzerresults.valueholder.AnalyzerResults();
            row.setAnalyzerId(analyzer.getId());
            row.setAccessionNumber(analysis.getSampleItem().getSample().getAccessionNumber());
            row.setInstrumentSpecimenId(row.getAccessionNumber());
            row.setInstrumentPatientId(patient.getExternalId());
            row.setTestId(analysis.getTest().getId());
            row.setTestName(analysis.getTest().getName());
            row.setComponentId(component.componentId);
            row.setResult(component.value);
            row.setRawResultValue(component.value);
            row.setResultType("N");
            row.setCompleteDate(Timestamp.from(Instant.now()));
            row.setSourceConnectionId("M7-review-connection");
            row.setSourceMessageId("M7-review-delivery");
            stagedRows.add(row);
        }
        analyzerStaging.insertAnalyzerResults(stagedRows, "1");
        em.flush();
        var decisions = stagedRows.stream().map(row -> {
            var item = new org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem();
            item.setId(row.getId());
            item.setTestId(row.getTestId());
            item.setAccessionNumber(row.getAccessionNumber());
            item.setInstrumentSpecimenId(row.getInstrumentSpecimenId());
            item.setResult(row.getResult());
            return item;
        }).toList();
        analyzerAcceptance.acceptAndPersist(decisions, actor);
        assertTrue(results.getResultsByAnalysis(analysis).isEmpty());
        assertTrue(incomingItems.forCase(microCase.getId()).isEmpty());
        stagedRows.forEach(row -> assertNotNull(analyzerStaging.get(row.getId())));
        decisions.forEach(item -> item.setIsAccepted(true));
        analyzerAcceptance.acceptAndPersist(decisions, actor);
        em.flush();
        assertEquals(2, results.getResultsByAnalysis(analysis).size());
        assertEquals("TechnicalAcceptance", workspace.getTests(microCase.getId(), actor).getFirst().status);
        var received = incomingItems.forCase(microCase.getId());
        assertEquals(1, received.size());
        assertEquals("PLACED", received.getFirst().getState());
        assertEquals(actor, received.getFirst().getPlacedBy());
        assertEquals(Long.valueOf(0),
                em.createQuery("select count(r) from AnalyzerResults r where r.id in :ids", Long.class)
                        .setParameter("ids", stagedRows.stream().map(row -> row.getId()).toList()).getSingleResult());
    }

    @Test
    public void incomingSingleComponentKeepsItsCatalogIdentityDuringPlacement() {
        var follow = fixtures.createCatalogTest();
        follow.setTestSection(analysis.getTestSection());
        em.merge(follow);
        var configured = components.saveSampleResults(follow.getId(), List.of(component("PRIMARY", "Follow-up", true)),
                null, null, actor);
        var mapping = new org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest();
        mapping.setTypeOfSampleId(microCase.getSampleTypeId());
        mapping.setTestId(follow.getId());
        mapping.setSysUserId(actor);
        em.persist(mapping);
        var p = new MicroCaseResultRequestForm();
        var value = new MicroCaseResultRequestForm.ResultComponent();
        value.componentId = configured.get(0).getId();
        value.value = "12";
        p.components = List.of(value);
        var queued = incoming.receive(microCase.getId(), microCase.getSampleItemId(), follow.getId(), "ANALYZER",
                "single-field", "12", p, false, actor);
        var placement = new org.openelisglobal.microbiology.form.MicroIncomingPlacementForm();
        placement.sampleItemId = microCase.getSampleItemId();
        var placed = incoming.place(microCase.getId(), queued.id, placement, actor, request(actor));
        assertEquals("PLACED", placed.state);
        var saved = workspace.getTests(microCase.getId(), actor).stream().filter(t -> follow.getId().equals(t.testId))
                .findFirst().orElseThrow();
        assertEquals("TechnicalAcceptance", saved.status);
        assertEquals("12", saved.components.get(0).getResultValue());
        assertEquals(configured.get(0).getId(), saved.components.get(0).getTestResultComponentId());
    }

    @Test
    public void placementAcknowledgementCannotReplaceTheStagedResult() {
        String instrumentId = analyzer();
        var mapping = new org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest();
        mapping.setTypeOfSampleId(microCase.getSampleTypeId());
        mapping.setTestId(analysis.getTest().getId());
        mapping.setSysUserId(actor);
        em.persist(mapping);
        var original = payload();
        original.note = "Original analyzer note";
        var queued = incoming.receive(microCase.getId(), microCase.getSampleItemId(), analysis.getTest().getId(),
                "ANALYZER", "acknowledgement-integrity", "12 / 34", original, false, actor, instrumentId);
        var placement = new org.openelisglobal.microbiology.form.MicroIncomingPlacementForm();
        placement.sampleItemId = microCase.getSampleItemId();
        placement.result = payload();
        placement.result.note = "Replacement client note";
        placement.result.components.get(0).value = "99";
        placement.result.components.get(0).criticalAcknowledged = true;
        placement.result.components.remove(1);
        var unknown = new MicroCaseResultRequestForm.ResultComponent();
        unknown.componentId = "unknown";
        unknown.value = "999";
        placement.result.components.add(unknown);
        var placed = incoming.place(microCase.getId(), queued.id, placement, actor, request(actor));
        assertEquals("PLACED", placed.state);
        assertEquals(Set.of("12", "34"), results.getResultsByAnalysis(analysis).stream().map(r -> r.getValue())
                .collect(java.util.stream.Collectors.toSet()));
        var notes = em
                .createQuery("from Note n where n.referenceId = :id and n.text = :text",
                        org.openelisglobal.note.valueholder.Note.class)
                .setParameter("id", analysis.getId()).setParameter("text", "Original analyzer note").getResultList();
        assertEquals(1, notes.size());
        assertEquals(Long.valueOf(0),
                em.createQuery("select count(n) from Note n where n.referenceId = :id and n.text = :text", Long.class)
                        .setParameter("id", analysis.getId()).setParameter("text", "Replacement client note")
                        .getSingleResult());
        em.flush();
        em.clear();
        assertEquals(instrumentId, analyses.get(analysis.getId()).getAnalyzerId());
        assertEquals("Original analyzer note", placed.result.note);
        assertEquals("12", placed.result.components.getFirst().value);
    }

    @Test
    public void analyzerArrivalAttachesComponentsOnceAndNeverFinalizes() {
        String instrumentId = analyzer();
        var p = payload();
        var arrived = incoming.receive(microCase.getId(), analysis.getSampleItem().getId(), analysis.getTest().getId(),
                "ANALYZER", "owned-run-1", "12 / 34", p, true, actor, instrumentId);
        assertEquals("PLACED", arrived.state);
        assertEquals(analysis.getId(), arrived.placementTarget);
        assertEquals("TechnicalAcceptance", workspace.getTests(microCase.getId(), actor).get(0).status);
        assertEquals(Set.of("12", "34"), results.getResultsByAnalysis(analysis).stream().map(r -> r.getValue())
                .collect(java.util.stream.Collectors.toSet()));
        var retry = incoming.receive(microCase.getId(), analysis.getSampleItem().getId(), analysis.getTest().getId(),
                "ANALYZER", "owned-run-1", "12 / 34", p, true, actor, instrumentId);
        assertEquals(arrived.id, retry.id);
        assertEquals(1, incomingItems.forCase(microCase.getId()).size());
        assertEquals(2, results.getResultsByAnalysis(analysis).size());
        em.flush();
        em.clear();
        assertEquals(instrumentId, analyses.get(analysis.getId()).getAnalyzerId());
    }

    private String analyzer() {
        var instrument = new org.openelisglobal.analyzer.valueholder.Analyzer();
        instrument.setName("AMR provenance " + UUID.randomUUID());
        instrument.setActive(true);
        instrument.setFhirUuid(UUID.randomUUID());
        instrument.setSysUserId(actor);
        em.persist(instrument);
        em.flush();
        return instrument.getId();
    }

    @Test
    public void laterInstrumentRunCannotOverwriteReviewPendingValues() {
        incoming.receive(microCase.getId(), microCase.getSampleItemId(), analysis.getTest().getId(), "ANALYZER",
                "first-run", "12", payload(), true, actor);
        var replacement = payload();
        replacement.components.get(0).value = "99";
        assertThrows(IllegalArgumentException.class,
                () -> incoming.receive(microCase.getId(), microCase.getSampleItemId(), analysis.getTest().getId(),
                        "ANALYZER", "second-run", "99", replacement, true, actor));
        assertEquals("12", workspace.getTests(microCase.getId(), actor).get(0).components.get(0).getResultValue());
    }

    @Test
    public void finalizedCaseQueuesLateEvidenceWithoutChangingResults() {
        var p = payload();
        microCase.setFinalReleaseState("FINAL_RELEASED");
        cases.update(microCase);
        var arrived = incoming.receive(microCase.getId(), analysis.getSampleItem().getId(), analysis.getTest().getId(),
                "ANALYZER", "owned-late-run", "12 / 34", p, true, actor);
        assertEquals("NEEDS_AMENDMENT", arrived.state);
        assertEquals(0, results.getResultsByAnalysis(analysis).size());
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
    public void malformedSelectionGroupingIsRejectedBeforePersistence() {
        configureSelection("M");
        var selection = workspace.getTests(microCase.getId(), actor).get(0).components.stream()
                .filter(r -> "M".equals(r.getResultType())).findFirst().orElseThrow();
        for (String key : List.of("x", "2147483648")) {
            var submission = payload();
            var selected = submission.components.stream()
                    .filter(c -> selection.getTestResultComponentId().equals(c.componentId)).findFirst().orElseThrow();
            selected.value = "";
            selected.multiSelectResultValues = "{\"" + key + "\":\"" + selection.getDictionaryResults().get(0).getId()
                    + "\"}";
            var failure = assertThrows(IllegalArgumentException.class, () -> workspace.saveResults(microCase.getId(),
                    analysis.getId(), submission, actor, request(actor)));
            assertEquals("MICROBIOLOGY_INVALID_RESULT_OPTION", failure.getMessage());
            assertTrue(results.getResultsByAnalysis(analysis).isEmpty());
        }
    }

    @Test
    public void qualifiedScientificFormsUseTheSharedNumericNormalization() {
        for (String value : List.of("<1.5×10⁵", ">10⁻³")) {
            var submission = payload();
            submission.components.get(0).value = value;
            var saved = workspace.saveResults(microCase.getId(), analysis.getId(), submission, actor, request(actor))
                    .get(0);
            assertEquals("TechnicalAcceptance", saved.status);
            assertEquals(2, results.getResultsByAnalysis(analysis).size());
        }
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
        assertEquals(statuses.getStatusID(org.openelisglobal.common.services.StatusService.OrderStatus.Finished),
                em.createQuery("select s.statusId from Sample s where s.id = :id", String.class)
                        .setParameter("id", microCase.getSampleId()).getSingleResult());
        assertThrows(ResponseStatusException.class,
                () -> workspace.saveResults(microCase.getId(), analysis.getId(), payload(), actor, request(actor)));
    }

    @Test
    public void caseSpecimenSummaryPreservesRejectionState() {
        analysis.getSampleItem().setRejected(true);
        em.merge(analysis.getSampleItem());
        em.flush();
        var specimen = caseWorkspace.get(microCase.getId(), actor).samples.stream()
                .filter(s -> analysis.getSampleItem().getId().equals(s.sampleItemId)).findFirst().orElseThrow();
        assertTrue(specimen.sampleRejected);
    }

    @Test
    public void validationDoesNotFinishSampleWithOutstandingAnalysis() {
        var sibling = fixtures.createAnalysis(analysis.getSampleItem(), fixtures.createCatalogTest());
        sibling.setStatusId(statuses.getStatusID(AnalysisStatus.NotStarted));
        em.merge(sibling);
        var saved = workspace.saveResults(microCase.getId(), analysis.getId(), payload(), actor, request(actor)).get(0);
        workspace.validateResult(microCase.getId(), analysis.getId(), saved.version, validator);
        assertNotEquals(statuses.getStatusID(org.openelisglobal.common.services.StatusService.OrderStatus.Finished),
                em.createQuery("select s.statusId from Sample s where s.id = :id", String.class)
                        .setParameter("id", microCase.getSampleId()).getSingleResult());
    }

    @Test
    public void clearingExternalProvenanceLeavesPerformerUnsetUntilLocalResult() {
        var p = new MicroCaseTestedElsewhereRequestForm();
        p.version = payload().version;
        p.testedElsewhere = true;
        p.performingUserId = validator;
        p.performedAt = "2026-10-01";
        var external = workspace.setTestedElsewhere(microCase.getId(), analysis.getId(), p, actor).get(0);
        p.version = external.version;
        p.testedElsewhere = false;
        var local = workspace.setTestedElsewhere(microCase.getId(), analysis.getId(), p, actor).get(0);
        assertFalse(local.testedElsewhere);
        assertNull(local.performingUserId);
        assertNull(local.performingLabId);
        assertNull(local.performedAt);
        var saved = workspace.saveResults(microCase.getId(), analysis.getId(), payload(), actor, request(actor)).get(0);
        assertEquals(actor, saved.performingUserId);
        assertNotNull(saved.performedAt);
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
    public void clearingOneComponentPreservesItsSiblingAndCompilesCancellationBeforeDeletion() throws Exception {
        assertClearComponent(null);
    }

    @Test
    public void clearingWithANoteDoesNotReloadTheDeletedResult() throws Exception {
        assertClearComponent("Corrected the optional component");
    }

    private void assertClearComponent(String note) throws Exception {
        var target = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(workspace);
        var original = org.springframework.test.util.ReflectionTestUtils.getField(target, "fhir");
        var publisher = org.mockito.Mockito
                .mock(org.openelisglobal.dataexchange.fhir.service.FhirTransformService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(target, "fhir", publisher);
        try {
            workspace.saveResults(microCase.getId(), analysis.getId(), payload(), actor, request(actor));
            var edit = payload();
            String clearedComponent = edit.components.get(1).componentId;
            var removed = results.getResultsByAnalysis(analysis).stream()
                    .filter(r -> clearedComponent.equals(r.getTestResult().getComponentId())).findFirst().orElseThrow();
            String removedId = removed.getId(), removedUuid = removed.getFhirUuidAsString();
            edit.components.get(1).value = "";
            edit.note = note;
            workspace.saveResults(microCase.getId(), analysis.getId(), edit, actor, request(actor));
            em.flush();
            em.clear();
            var saved = results.getResultsByAnalysis(analyses.get(analysis.getId()));
            assertEquals(1, saved.size());
            assertEquals("12", saved.get(0).getValue());
            assertFalse(saved.stream().anyMatch(r -> removedId.equals(r.getId())));
            org.mockito.Mockito.verifyZeroInteractions(publisher);
            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().getLast()
                    .afterCommit();
            var captured = org.mockito.ArgumentCaptor.forClass(List.class);
            var savedData = org.mockito.ArgumentCaptor
                    .forClass(org.openelisglobal.result.action.util.ResultsUpdateDataSet.class);
            org.mockito.Mockito.verify(publisher).transformPersistResultsEntryFhirObjects(savedData.capture(),
                    captured.capture());
            assertFalse(savedData.getValue().getModifiedResults().stream()
                    .anyMatch(r -> removedId.equals(r.result.getId())));
            if (note != null) {
                var notes = em
                        .createQuery("from Note n where n.referenceId = :id and n.text = :text",
                                org.openelisglobal.note.valueholder.Note.class)
                        .setParameter("id", analysis.getId()).setParameter("text", note).getResultList();
                assertEquals(1, notes.size());
                assertNull(notes.get(0).getTestResultComponentId());
            }
            assertEquals(1, captured.getValue().size());
            var snapshot = (org.hl7.fhir.r4.model.Observation) captured.getValue().get(0);
            assertEquals(removedUuid, snapshot.getIdElement().getIdPart());
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(target, "fhir", original);
        }
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

    private org.openelisglobal.test.valueholder.Test additionalTest() {
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
        em.flush();
        return em.find(org.openelisglobal.test.valueholder.Test.class, follow.getId());
    }

    private MicroCaseAddTestsForm selection() {
        var selection = new MicroCaseAddTestsForm();
        selection.sampleItemId = microCase.getSampleItemId();
        selection.placement = "ADDITIONAL";
        return selection;
    }

    @Test
    public void panelSelectionRetainsItsPanelOnTheAnalysis() {
        var follow = additionalTest();
        var panel = panel(follow);
        var selection = selection();
        selection.panelIds = List.of(panel.getId());
        workspace.addTests(microCase.getId(), selection, actor);
        em.flush();
        em.clear();
        var added = links.getAnalyses(microCase.getId()).stream()
                .filter(a -> follow.getId().equals(a.getTest().getId())).findFirst().orElseThrow();
        assertEquals(panel.getId(), added.getPanel().getId());
        assertEquals(2, workspace.addTests(microCase.getId(), selection, actor).size());
    }

    @Test
    public void resultsCatalogWorksWithoutReceptionAndRejectsOtherUnits() throws Exception {
        var follow = additionalTest();
        var panel = panel(follow);
        assertTrue(workspace.getCatalog(microCase.getId(), microCase.getSampleTypeId(), actor).tests.stream()
                .anyMatch(t -> follow.getId().equals(t.id())));
        mockMvc.perform(get("/rest/microbiology/cases/" + microCase.getId() + "/analyses/catalog")
                .param("sampleType", microCase.getSampleTypeId())
                .requestAttr(IActionConstants.USER_SESSION_DATA, session(actor))).andExpect(status().isOk())
                .andExpect(jsonPath("$.panels[0].id").value(panel.getId()));
        assertThrows(AccessDeniedException.class,
                () -> workspace.getCatalog(microCase.getId(), microCase.getSampleTypeId(), outsider));
        assertThrows(IllegalArgumentException.class, () -> workspace.getCatalog(microCase.getId(), "-12345", actor));
    }

    @Test
    public void deactivatedPanelCannotBeSubmittedFromAStaleCatalog() {
        var panel = panel(additionalTest());
        assertTrue(workspace.getCatalog(microCase.getId(), microCase.getSampleTypeId(), actor).panels.stream()
                .anyMatch(p -> panel.getId().equals(p.id())));
        panel.setIsActive("N");
        em.merge(panel);
        var selection = selection();
        selection.panelIds = List.of(panel.getId());
        var error = assertThrows(IllegalArgumentException.class,
                () -> workspace.addTests(microCase.getId(), selection, actor));
        assertEquals("MICROBIOLOGY_INVALID_PANEL", error.getMessage());
        assertTrue(workspace.getCatalog(microCase.getId(), microCase.getSampleTypeId(), actor).panels.isEmpty());
    }

    private org.openelisglobal.panel.valueholder.Panel panel(org.openelisglobal.test.valueholder.Test... members) {
        var panel = new org.openelisglobal.panel.valueholder.Panel();
        var localized = new org.openelisglobal.localization.valueholder.Localization();
        localized.setDescription("Culture panel");
        localized.setEnglish("Culture panel");
        localized.setSysUserId(actor);
        em.persist(localized);
        panel.setLocalization(localized);
        panel.setPanelName("M3 culture panel");
        panel.setDescription("M3 culture panel");
        panel.setIsActive("Y");
        panel.setSysUserId(actor);
        em.persist(panel);
        var samplePanel = new org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel();
        samplePanel.setPanelId(panel.getId());
        samplePanel.setTypeOfSampleId(microCase.getSampleTypeId());
        samplePanel.setSysUserId(actor);
        em.persist(samplePanel);
        for (var test : members) {
            var item = new org.openelisglobal.panelitem.valueholder.PanelItem();
            item.setPanel(panel);
            item.setTest(test);
            item.setSysUserId(actor);
            em.persist(item);
        }
        em.flush();
        return panel;
    }

    @Test
    public void panelSelectionFiltersIneligibleMembersAndAssociatesExistingStandaloneTest() {
        var follow = additionalTest();
        var otherSpecimenTest = fixtures.createCatalogTest();
        var inactive = additionalTest();
        inactive.setIsActive("N");
        em.merge(inactive);
        var nonOrderable = additionalTest();
        nonOrderable.setOrderable(false);
        em.merge(nonOrderable);
        var panel = panel(follow, otherSpecimenTest, inactive, nonOrderable);
        var selection = selection();
        selection.testIds = List.of(follow.getId());
        workspace.addTests(microCase.getId(), selection, actor);
        String existingId = links.getAnalyses(microCase.getId()).stream()
                .filter(a -> follow.getId().equals(a.getTest().getId())).findFirst().orElseThrow().getId();
        selection.testIds = List.of();
        selection.panelIds = List.of(panel.getId());
        workspace.addTests(microCase.getId(), selection, actor);
        em.flush();
        em.clear();
        var saved = links.getAnalyses(microCase.getId());
        assertEquals(2, saved.size());
        var associated = saved.stream().filter(a -> existingId.equals(a.getId())).findFirst().orElseThrow();
        assertEquals(panel.getId(), associated.getPanel().getId());
        assertEquals(2, workspace.addTests(microCase.getId(), selection, actor).size());
    }

    @Test
    public void addedTestsPublishTheirStableIdsOnlyAfterCommit() throws Exception {
        var target = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(workspace);
        var original = org.springframework.test.util.ReflectionTestUtils.getField(target, "fhir");
        var publisher = org.mockito.Mockito
                .mock(org.openelisglobal.dataexchange.fhir.service.FhirTransformService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(target, "fhir", publisher);
        try {
            var follow = additionalTest();
            var selection = selection();
            selection.testIds = List.of(follow.getId());
            workspace.addTests(microCase.getId(), selection, actor);
            var added = links.getAnalyses(microCase.getId()).stream()
                    .filter(a -> follow.getId().equals(a.getTest().getId())).findFirst().orElseThrow();
            org.mockito.Mockito.verifyZeroInteractions(publisher);
            var callbacks = org.springframework.transaction.support.TransactionSynchronizationManager
                    .getSynchronizations();
            callbacks.getLast().afterCommit();
            org.mockito.Mockito.verify(publisher).transformAnalysisByIds(List.of(added.getId()));
            int count = callbacks.size();
            workspace.addTests(microCase.getId(), selection, actor);
            assertEquals(count, org.springframework.transaction.support.TransactionSynchronizationManager
                    .getSynchronizations().size());
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(target, "fhir", original);
        }
    }

    @Test
    public void testsWithNullOrderabilityCannotBeAdded() {
        var follow = additionalTest();
        follow.setOrderable(null);
        em.merge(follow);
        em.flush();
        em.clear();
        var selection = selection();
        selection.testIds = List.of(follow.getId());
        var failure = assertThrows(IllegalArgumentException.class,
                () -> workspace.addTests(microCase.getId(), selection, actor));
        assertEquals("MICROBIOLOGY_TEST_SAMPLE_MISMATCH", failure.getMessage());
        assertEquals(1, links.getAnalyses(microCase.getId()).size());
    }

    @Test
    public void changingOnlyProvenanceRejectsThePreviouslyIssuedVersion() {
        var payload = new MicroCaseTestedElsewhereRequestForm();
        payload.version = payload().version;
        payload.testedElsewhere = true;
        payload.performingUserId = validator;
        payload.performedAt = "2026-10-01";
        var staleVersion = payload.version;
        var saved = workspace.setTestedElsewhere(microCase.getId(), analysis.getId(), payload, actor).get(0);
        em.flush();
        em.clear();
        assertNotEquals(staleVersion, saved.version);
        payload.performingUserId = actor;
        var failure = assertThrows(ResponseStatusException.class,
                () -> workspace.setTestedElsewhere(microCase.getId(), analysis.getId(), payload, actor));
        assertEquals(409, failure.getStatusCode().value());
        assertEquals(validator, links.getByCaseAndAnalysis(microCase.getId(), analysis.getId()).getPerformingUserId());
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
