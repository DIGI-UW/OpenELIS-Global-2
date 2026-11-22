package org.openelisglobal.common.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.action.bean.ReflexRuleAction;
import org.openelisglobal.testreflex.action.bean.ReflexRuleCondition;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptions.NumericRelationOptions;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptions.OverallOptions;
import org.openelisglobal.testreflex.dao.ReflexRuleDAO;
import org.openelisglobal.testreflex.dao.TestReflexDAO;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.testreflex.service.TestReflexServiceImpl;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;

/**
 * A reflex rule names two specimens, and they are not the same specimen.
 *
 * <p>
 * The condition names the specimen whose result fires the rule; the action
 * names the specimen the generated test is reported on. The rule builder has
 * collected both since the editor was written, but the executed row had one
 * column for them, so the target had nowhere to live: the generated test fell
 * back to guessing from the added test's own configuration and, where that was
 * ambiguous, landed on whichever specimen happened to trigger the rule — "read
 * Respiratory Swab, report on DBS" reported on Respiratory Swab.
 *
 * <p>
 * These assertions pin the two axes apart on the row the executor reads.
 * Sibling of {@link GeneratedSpecimenScopeTest}, which covers what the executor
 * then does with the target.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReflexTriggerAndTargetScopeTest {

    private static final String TRIGGER_TEST = "300";
    private static final String ADDED_TEST = "6";
    private static final String TRIGGER_SPECIMEN = "30";
    private static final String TARGET_SPECIMEN = "26";
    private static final String TRIGGER_COMPONENT = "c-pcr";

    @Mock
    private TestReflexDAO baseObjectDAO;

    @Mock
    private ReflexRuleDAO reflexRuleDAO;

    @Mock
    private TestReflexService reflexService;

    @Mock
    private TestService testService;

    @Mock
    private TestResultService testResultService;

    @Mock
    private DictionaryService dictionaryService;

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private AnalyteService analyteService;

    @Mock
    private TestAnalyteService testAnalyteService;

    @Mock
    private RuleResultScope ruleResultScope;

    @Mock
    private TestResultComponentService testResultComponentService;

    @InjectMocks
    private TestReflexServiceImpl service;

    private org.openelisglobal.test.valueholder.Test triggerTest;
    private org.openelisglobal.test.valueholder.Test addedTest;

    @Before
    public void setUp() {
        triggerTest = namedTest(TRIGGER_TEST);
        addedTest = namedTest(ADDED_TEST);

        Analyte analyte = new Analyte();
        analyte.setId("278");
        lenient().when(analyteService.save(any(Analyte.class))).thenReturn(analyte);

        TestAnalyte testAnalyte = new TestAnalyte();
        testAnalyte.setId("294");
        lenient().when(testAnalyteService.save(any(TestAnalyte.class))).thenReturn(testAnalyte);

        TestResult row = new TestResult();
        row.setId("655");
        row.setTest(triggerTest);
        row.setComponentId(TRIGGER_COMPONENT);
        lenient().when(testResultService.getActiveTestResultsByTest(TRIGGER_TEST))
                .thenReturn(new ArrayList<>(Collections.singletonList(row)));

        lenient().when(testResultComponentService.getActiveComponentsByTestId(TRIGGER_TEST))
                .thenReturn(Collections.singletonList(component(TRIGGER_COMPONENT)));

        lenient().when(testService.getTestById(TRIGGER_TEST)).thenReturn(triggerTest);
        lenient().when(testService.getTestById(ADDED_TEST)).thenReturn(addedTest);
        lenient().when(testService.getResultType(triggerTest)).thenReturn("N");
        lenient().when(ruleResultScope.resultTypeForComponent(TRIGGER_TEST, TRIGGER_COMPONENT, "N")).thenReturn("N");

        // The trigger test runs on the specimen its condition names, and the added
        // test on the specimen its action names — what testAndSampleMatches checks
        // before either is written.
        lenient().when(typeOfSampleService.getActiveTestsBySampleTypeId(TRIGGER_SPECIMEN, false))
                .thenReturn(Collections.singletonList(triggerTest));
        lenient().when(typeOfSampleService.getActiveTestsBySampleTypeId(TARGET_SPECIMEN, false))
                .thenReturn(Collections.singletonList(addedTest));

        lenient().when(reflexService.save(any(TestReflex.class))).thenAnswer(invocation -> {
            TestReflex saved = invocation.getArgument(0);
            saved.setId("13");
            return saved;
        });
    }

    @Test
    public void theTriggerSpecimenAndTheTargetSpecimenLandInDifferentColumns() {
        TestReflex saved = save(TRIGGER_SPECIMEN, TARGET_SPECIMEN);

        assertEquals("the rule fires on the specimen the condition names", TRIGGER_SPECIMEN, saved.getSampleTypeId());
        assertEquals("the generated test is reported on the specimen the action names", TARGET_SPECIMEN,
                saved.getAddedSampleTypeId());
        assertNotEquals("reading one column for both is what made the target follow the trigger",
                saved.getSampleTypeId(), saved.getAddedSampleTypeId());
    }

    @Test
    public void theTriggerComponentAndTestStillScopeExecution() {
        TestReflex saved = save(TRIGGER_SPECIMEN, TARGET_SPECIMEN);

        assertEquals(TRIGGER_COMPONENT, saved.getComponentId());
        assertEquals(TRIGGER_TEST, saved.getTest().getId());
        assertEquals(ADDED_TEST, saved.getAddedTest().getId());
    }

    @Test
    public void aRuleThatNamesNoTargetSpecimenLeavesTheColumnUnset() {
        // "" is what an unset picker posts; NULL is what "the added test's own
        // configuration decides" means to the executor.
        lenient().when(typeOfSampleService.getActiveTestsBySampleTypeId("", false))
                .thenReturn(Collections.singletonList(addedTest));

        TestReflex saved = save(TRIGGER_SPECIMEN, "");

        assertNull(saved.getAddedSampleTypeId());
        assertEquals("and the trigger scope is untouched by that", TRIGGER_SPECIMEN, saved.getSampleTypeId());
    }

    @Test
    public void aTargetSpecimenTheAddedTestDoesNotRunOnIsNotAccepted() {
        // The pairing is rejected, so neither the added test nor its specimen is
        // written: a rule that could never produce a valid result must not look
        // configured.
        lenient().when(typeOfSampleService.getActiveTestsBySampleTypeId("99", false))
                .thenReturn(Collections.<org.openelisglobal.test.valueholder.Test>emptyList());

        TestReflex saved = save(TRIGGER_SPECIMEN, "99");

        assertNull(saved.getAddedSampleTypeId());
        assertNull(saved.getAddedTest());
    }

    @Test
    public void anUnscopedTriggerDoesNotBorrowTheTargetSpecimen() {
        lenient().when(typeOfSampleService.getActiveTestsBySampleTypeId("", false))
                .thenReturn(Collections.singletonList(triggerTest));

        TestReflex saved = save("", TARGET_SPECIMEN);

        assertNull("an unscoped trigger fires on every specimen, not on the target's", saved.getSampleTypeId());
        assertEquals(TARGET_SPECIMEN, saved.getAddedSampleTypeId());
    }

    /**
     * Saves a one-condition, one-action rule and returns the row that was written.
     */
    private TestReflex save(String conditionSampleId, String actionSampleId) {
        ReflexRule rule = new ReflexRule();
        rule.setRuleName("Covid-Reflex");
        rule.setOverall(OverallOptions.ANY);
        rule.setActive(true);

        ReflexRuleCondition condition = new ReflexRuleCondition();
        condition.setTestId(TRIGGER_TEST);
        condition.setSampleId(conditionSampleId);
        condition.setComponentId(TRIGGER_COMPONENT);
        condition.setRelation(NumericRelationOptions.EQUALS);
        condition.setValue("110");
        condition.setValue2("0");
        rule.setConditions(new LinkedHashSet<>(Collections.singletonList(condition)));

        ReflexRuleAction action = new ReflexRuleAction();
        action.setReflexTestId(ADDED_TEST);
        action.setSampleId(actionSampleId);
        rule.setActions(new LinkedHashSet<>(Collections.singletonList(action)));

        service.saveOrUpdateReflexRule(rule);

        ArgumentCaptor<TestReflex> captor = ArgumentCaptor.forClass(TestReflex.class);
        org.mockito.Mockito.verify(reflexService).save(captor.capture());
        return captor.getValue();
    }

    private TestResultComponent component(String id) {
        TestResultComponent component = new TestResultComponent();
        component.setId(id);
        component.setTestId(TRIGGER_TEST);
        component.setIsPrimary(true);
        component.setResultType("N");
        return component;
    }

    private org.openelisglobal.test.valueholder.Test namedTest(String id) {
        org.openelisglobal.test.valueholder.Test test = mock(org.openelisglobal.test.valueholder.Test.class);
        lenient().when(test.getId()).thenReturn(id);
        return test;
    }

    /** Guards the fixture: nothing is cleared when the rule has no prior rows. */
    @Test
    public void savesWithoutTouchingRowsThatDoNotExist() {
        lenient().when(baseObjectDAO.getTestReflexsByTestAnalyteId(anyString())).thenReturn(new ArrayList<>());

        List<TestReflex> written = Collections.singletonList(save(TRIGGER_SPECIMEN, TARGET_SPECIMEN));

        assertEquals(1, written.size());
    }
}
