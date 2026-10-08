package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseRequestDAO;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.service.MicroCaseMembershipService;
import org.openelisglobal.microbiology.service.MicroOrderRoutingService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testreflex.action.util.ReflexAction;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroOrderRoutingIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private SampleService orders;
    @Autowired
    private SampleItemService samples;
    @Autowired
    private TestService tests;
    @Autowired
    private TestSectionService units;
    @Autowired
    private MicroOrderRoutingService routing;
    @Autowired
    private MicroCaseDAO cases;
    @Autowired
    private MicroCaseRequestDAO requests;
    @Autowired
    private MicroCaseMembershipService membership;
    @PersistenceContext
    private EntityManager em;
    @Autowired
    private AnalysisService analyses;
    @Autowired
    private MicroCaseAnalysisDAO analysisLinks;
    @Autowired
    private org.openelisglobal.program.service.ProgramService programs;
    @Autowired
    private org.openelisglobal.program.service.ProgramSampleService programSamples;
    private Sample order;
    private org.openelisglobal.test.valueholder.Test test;
    private TypeOfSample type;
    private String actor;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        actor = fixtures.defaultUserId();
        order = new Sample();
        order.setAccessionNumber("V2-" + UUID.randomUUID().toString().substring(0, 12));
        order.setEnteredDate(Date.valueOf("2026-01-02"));
        order.setStatusId(fixtures.ensureSampleEnteredStatus());
        order.setSysUserId(actor);
        orders.insert(order);
        type = fixtures.getOrCreateActiveSampleType();
        test = em.find(org.openelisglobal.test.valueholder.Test.class, fixtures.createCatalogTest().getId());
        test.setTestSection(fixtures.createLabUnit());
        test.setOpensMicrobiologyCase(true);
        test.setMicrobiologyCaseRole("DIRECT");
        em.flush();
    }

    private SampleTypeRequest request(String ids) {
        SampleTypeRequest r = new SampleTypeRequest();
        r.setSample(order);
        r.setTypeOfSample(type);
        r.setRequestedTests(ids);
        r.setCreatedDate(Timestamp.valueOf("2026-01-02 10:00:00"));
        if (test.isCollectedInSets())
            r.setCultureSetNumber(1);
        em.persist(r);
        em.flush();
        return r;
    }

    @Test
    public void newCaseInheritsEligibleOrderProgramAndReusedCaseKeepsIt() {
        var original = program(true);
        var association = new org.openelisglobal.program.valueholder.ProgramSample();
        association.setProgram(original);
        association.setSample(order);
        association.setSysUserId(actor);
        programSamples.insert(association);
        request(test.getId());
        routing.routeOrder(order, actor);
        var owner = cases.getByOrder(order.getId()).getFirst();
        assertEquals(original.getId(), owner.getProgramId());
        assertEquals(original.getId(),
                programSamples.getProgrammeSampleBySample(Integer.valueOf(order.getId()), null).getProgram().getId());

        var replacement = program(true);
        association.setProgram(replacement);
        programSamples.update(association);
        var additional = em.find(org.openelisglobal.test.valueholder.Test.class, fixtures.createCatalogTest().getId());
        additional.setTestSection(test.getTestSection());
        additional.setOpensMicrobiologyCase(true);
        additional.setMicrobiologyCaseRole("DIRECT");
        request(additional.getId());
        routing.routeOrder(order, actor);
        em.flush();
        em.clear();
        var owners = cases.getByOrder(order.getId());
        assertEquals(1, owners.size());
        assertEquals(owner.getId(), owners.getFirst().getId());
        assertEquals(original.getId(), owners.getFirst().getProgramId());
        assertEquals(replacement.getId(),
                programSamples.getProgrammeSampleBySample(Integer.valueOf(order.getId()), null).getProgram().getId());
    }

    @Test
    public void programWithoutMicrobiologyVisibilityDoesNotDefaultOntoCase() {
        var hidden = program(false);
        var association = new org.openelisglobal.program.valueholder.ProgramSample();
        association.setProgram(hidden);
        association.setSample(order);
        association.setSysUserId(actor);
        programSamples.insert(association);
        request(test.getId());
        routing.routeOrder(order, actor);
        assertNull(cases.getByOrder(order.getId()).getFirst().getProgramId());
        assertEquals(hidden.getId(), association.getProgram().getId());
    }

    private org.openelisglobal.program.valueholder.Program program(boolean visible) {
        var program = new org.openelisglobal.program.valueholder.Program();
        program.setCode("V2_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        program.setProgramName(program.getCode());
        program.setManuallyChanged(true);
        program.setShowOnMicroCase(visible);
        program.setSysUserId(actor);
        programs.insert(program);
        return program;
    }

    @Test
    public void savingRequestedWorkOpensOneCaseWithoutInventingSamplesOrReceipt() {
        SampleTypeRequest r = request(test.getId());
        routing.routeOrder(order, actor);
        routing.routeOrder(order, actor);
        em.flush();
        em.clear();
        assertEquals(1, cases.getByOrder(order.getId()).size());
        var owner = requests.getActiveByRequestAndTest(r.getId(), test.getId());
        assertNotNull(owner);
        assertNull(owner.getSampleItemId());
        assertTrue(samples.getSampleItemsBySampleId(order.getId()).isEmpty());
        assertNull(orders.get(order.getId()).getReceivedTimestamp());
        assertNull(orders.get(order.getId()).getCollectionDate());
    }

    @Test
    public void laterCollectionAttachesToTheRequestedCaseAndRetriesKeepOneOwner() {
        SampleTypeRequest r = request(test.getId());
        routing.routeOrder(order, actor);
        String caseId = requests.getActiveByRequestAndTest(r.getId(), test.getId()).getCaseId();
        SampleItem sample = new SampleItem();
        sample.setSample(order);
        sample.setTypeOfSample(type);
        sample.setSortOrder("1");
        sample.setStatusId(fixtures.ensureSampleEnteredStatus());
        sample.setSysUserId(actor);
        samples.insert(sample);
        var analysis = fixtures.createAnalysis(sample, test);
        r.setSampleItem(sample);
        r.setStatus(SampleTypeRequest.Status.COLLECTED);
        routing.routeOrder(order, actor);
        routing.routeOrder(order, actor);
        em.flush();
        em.clear();
        assertEquals(1, cases.getByOrder(order.getId()).size());
        var owner = requests.getActiveByRequestAndTest(r.getId(), test.getId());
        assertEquals(caseId, owner.getCaseId());
        assertEquals(analysis.getId(), owner.getAnalysisId());
        assertEquals(1, membership.getCaseSamples(caseId).size());
        assertEquals(sample.getId(), owner.getSampleItemId());
    }

    @Test
    public void anOrdinaryTestOpensNoCase() {
        test.setOpensMicrobiologyCase(false);
        em.flush();
        request(test.getId());
        routing.routeOrder(order, actor);
        assertTrue(cases.getByOrder(order.getId()).isEmpty());
    }

    @Test
    public void aCaseRoleOpensAnEmptyCaseWithoutAnAnalysis() {
        test.setMicrobiologyCaseRole("CASE");
        em.flush();
        SampleTypeRequest r = request(test.getId());
        routing.routeOrder(order, actor);
        var owner = requests.getActiveByRequestAndTest(r.getId(), test.getId());
        assertNotNull(owner);
        assertNull(owner.getAnalysisId());
        assertTrue(membership.getCaseSamples(owner.getCaseId()).isEmpty());
    }

    @Test
    public void setCulturesShareOneCaseAcrossRequestedSampleTypes() {
        test.setMicrobiologyCaseRole("CULTURE");
        test.setCollectedInSets(true);
        em.flush();
        var first = request(test.getId());
        var second = request(test.getId());
        second.setTypeOfSample(fixtures.createTypeOfSample());
        routing.routeOrder(order, actor);
        assertEquals(1, cases.getByOrder(order.getId()).size());
        assertEquals(requests.getActiveByRequestAndTest(first.getId(), test.getId()).getCaseId(),
                requests.getActiveByRequestAndTest(second.getId(), test.getId()).getCaseId());
    }

    @Test
    public void microReflexFilesInItsCatalogUnitAndOpensTheRelatedCase() {
        Analysis source = sourceAnalysis();
        var added = reflexTest(true, true);
        Analysis generated = reflex(source, added);
        assertNotNull(generated);
        assertEquals(added.getTestSection().getId(), generated.getTestSection().getId());
        generated.setSysUserId(actor);
        analyses.insert(generated);
        routing.routeAnalysis(generated, actor);
        routing.routeAnalysis(generated, actor);
        assertEquals(2, cases.getByOrder(order.getId()).size());
        var link = analysisLinks.getActiveByAnalysisId(generated.getId());
        assertEquals(added.getTestSection().getId(), cases.get(link.getCaseId()).orElseThrow().getLabUnitId());
        assertNotEquals(analysisLinks.getActiveByAnalysisId(source.getId()).getCaseId(), link.getCaseId());
        assertEquals(source.getSampleItem().getId(), generated.getSampleItem().getId());
    }

    @Test
    public void inactiveMicroTargetUnitBlocksTheReflexEvenWhenParentIsActive() {
        Analysis source = sourceAnalysis();
        var added = reflexTest(true, false);
        assertNull(reflex(source, added));
        assertEquals(1, cases.getByOrder(order.getId()).size());
    }

    @Test
    public void activeMicroTargetUnitCanReceiveAReflexFromAnInactiveParentUnit() {
        Analysis source = sourceAnalysis();
        var added = reflexTest(true, true);
        source.getTestSection().setIsActive("N");
        em.flush();
        Analysis generated = reflex(source, added);
        assertNotNull(generated);
        assertEquals(added.getTestSection().getId(), generated.getTestSection().getId());
    }

    @Test
    public void ordinaryReflexKeepsTheActiveParentUnitAndCase() {
        Analysis source = sourceAnalysis();
        var added = reflexTest(false, false);
        Analysis generated = reflex(source, added);
        assertNotNull(generated);
        assertEquals(source.getTestSection().getId(), generated.getTestSection().getId());
        generated.setSysUserId(actor);
        analyses.insert(generated);
        routing.routeAnalysis(generated, actor);
        assertEquals(1, cases.getByOrder(order.getId()).size());
        assertEquals(analysisLinks.getActiveByAnalysisId(source.getId()).getCaseId(),
                analysisLinks.getActiveByAnalysisId(generated.getId()).getCaseId());
    }

    @Test
    public void ordinaryReflexStillCannotLandInAnInactiveParentUnit() {
        Analysis source = sourceAnalysis();
        var added = reflexTest(false, true);
        source.getTestSection().setIsActive("N");
        em.flush();
        assertNull(reflex(source, added));
    }

    @Test
    public void analysisCreationRoutesBeforeCommitAndAFailureRollsBackAllClinicalWrites() {
        SampleItem sample = new SampleItem();
        sample.setSample(order);
        sample.setTypeOfSample(type);
        sample.setSortOrder("1");
        sample.setStatusId(fixtures.ensureSampleEnteredStatus());
        sample.setSysUserId(actor);
        samples.insert(sample);
        Analysis created = fixtures.createAnalysis(sample, test);
        String orderId = order.getId();
        String analysisId = created.getId();
        assertNull(analysisLinks.getActiveByAnalysisId(analysisId));
        var routed = new java.util.concurrent.atomic.AtomicBoolean();
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void beforeCommit(boolean readOnly) {
                        assertNotNull(analysisLinks.getActiveByAnalysisId(analysisId));
                        assertEquals(1, cases.getByOrder(orderId).size());
                        routed.set(true);
                        throw new IllegalStateException("Simulated failure after case routing");
                    }
                });
        org.springframework.test.context.transaction.TestTransaction.flagForCommit();
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                org.springframework.test.context.transaction.TestTransaction::end);
        assertEquals("Simulated failure after case routing", failure.getMessage());
        assertTrue(routed.get());
        org.springframework.test.context.transaction.TestTransaction.start();
        em.clear();
        assertNull(analysisLinks.getActiveByAnalysisId(analysisId));
        assertTrue(cases.getByOrder(orderId).isEmpty());
        assertTrue(samples.getSampleItemsBySampleId(orderId).isEmpty());
        assertNull(em.find(Sample.class, orderId));
    }

    private Analysis sourceAnalysis() {
        SampleItem sample = new SampleItem();
        sample.setSample(order);
        sample.setTypeOfSample(type);
        sample.setSortOrder("1");
        sample.setStatusId(fixtures.ensureSampleEnteredStatus());
        sample.setSysUserId(actor);
        samples.insert(sample);
        Analysis source = fixtures.createAnalysis(sample, test);
        source.setTestSection(test.getTestSection());
        routing.routeAnalysis(source, actor);
        em.flush();
        return source;
    }

    private org.openelisglobal.test.valueholder.Test reflexTest(boolean micro, boolean activeUnit) {
        TestSection unit = new TestSection();
        unit.setTestSectionName("Reflex unit " + UUID.randomUUID().toString().substring(0, 8));
        unit.setDescription(unit.getTestSectionName());
        unit.setIsActive(activeUnit ? "Y" : "N");
        unit.setSysUserId(actor);
        var name = new org.openelisglobal.localization.valueholder.Localization();
        name.setDescription("test section name");
        name.setLocalizedValue("en", unit.getTestSectionName());
        em.persist(name);
        unit.setLocalization(name);
        units.insert(unit);
        var added = em.find(org.openelisglobal.test.valueholder.Test.class, fixtures.createCatalogTest().getId());
        added.setTestSection(unit);
        added.setOpensMicrobiologyCase(micro);
        added.setMicrobiologyCaseRole("DIRECT");
        em.flush();
        return added;
    }

    private Analysis reflex(Analysis source, org.openelisglobal.test.valueholder.Test added) {
        Result trigger = new Result();
        trigger.setAnalysis(source);
        trigger.setSysUserId(actor);
        trigger.setResultType("N");
        trigger.setValue("1");
        trigger.setIsReportable("Y");
        em.persist(trigger);
        em.flush();
        TestReflex rule = new TestReflex();
        rule.setAddedTest(added);
        rule.setAddedSampleTypeId(type.getId());
        ReflexAction action = new ReflexAction() {
            @Override
            protected void handleScriptletAction(Scriptlet scriptlet) {
            }
        };
        action.handleReflex(rule, trigger, null);
        return action.getNewAnalysis();
    }
}
