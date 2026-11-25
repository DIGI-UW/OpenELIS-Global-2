package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.MicroCaseResultRequestForm;
import org.openelisglobal.microbiology.service.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.testreflex.action.util.TestReflexUtil;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroReflexSuppressionIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private MicroCaseDAO cases;
    @Autowired
    private MicroCaseAnalysisDAO links;
    @Autowired
    private MicroIncomingResultService incoming;
    @Autowired
    private TestReflexUtil reflexes;
    @Autowired
    private SampleItemService specimens;
    @PersistenceContext
    private EntityManager em;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/system-user.xml");
        executeDataSetWithStateManagement("testdata/role.xml");
        fixtures.ensureRequiredWorkflowStatuses();
    }

    @Test
    public void waitingIncomingTestSuppressesReflexBeforeCreatingItsMissingSpecimen() {
        String actor = fixtures.defaultUserId();
        var unit = fixtures.createLabUnit();
        var source = fixtures.createSampleWithSampleItem("M7-REFLEX");
        source = em.find(SampleItem.class, source.getId());
        source.setTypeOfSample(fixtures.getOrCreateActiveSampleType());
        var triggerTest = fixtures.createCatalogTest();
        triggerTest = em.find(org.openelisglobal.test.valueholder.Test.class, triggerTest.getId());
        triggerTest.setTestSection(unit);
        var trigger = fixtures.createAnalysis(source, triggerTest);
        trigger = em.find(Analysis.class, trigger.getId());
        trigger.setTestSection(unit);
        var targetTest = fixtures.createCatalogTest();
        var targetType = fixtures.createTypeOfSample();
        var c = new MicroCase();
        c.setSampleId(source.getSample().getId());
        c.setSampleItemId(source.getId());
        c.setSampleTypeId(source.getTypeOfSampleId());
        c.setLabUnitId(unit.getId());
        c.setCreatedBy(actor);
        cases.insert(c);
        var link = new MicroCaseAnalysis();
        link.setCaseId(c.getId());
        link.setAnalysisId(trigger.getId());
        link.setSysUserId(actor);
        links.insert(link);
        var payload = new MicroCaseResultRequestForm();
        var component = new MicroCaseResultRequestForm.ResultComponent();
        component.componentId = "primary";
        component.value = "1";
        payload.components = List.of(component);
        assertEquals("WAITING", incoming.receive(c.getId(), source.getId(), targetTest.getId(), "ANALYZER",
                UUID.randomUUID().toString(), "1", payload, false, actor).state);
        var rule = new TestReflex();
        rule.setAddedTest(targetTest);
        rule.setAddedSampleTypeId(targetType.getId());
        var result = new Result();
        result.setAnalysis(trigger);
        int initialCount = specimens.getSampleItemsBySampleId(source.getSample().getId()).size();
        assertEquals(1, initialCount);
        for (String flag : Arrays.asList(null, "UC")) {
            rule.setFlags(flag);
            Optional<Analysis> added = ReflectionTestUtils.invokeMethod(reflexes, "addReflexTest", rule, result, null,
                    source.getSample(), true, false, flag == null ? null : "test_" + targetTest.getId(), true, actor);
            assertEquals(Optional.empty(), added);
            em.flush();
            assertEquals(initialCount, specimens.getSampleItemsBySampleId(source.getSample().getId()).size());
            assertEquals(0, specimens.getSampleItemsBySampleIdAndType(source.getSample().getId(), targetType).size());
        }
    }
}
