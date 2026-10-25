package org.openelisglobal.common.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
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
import org.openelisglobal.typeofsample.service.TypeOfSampleService;

/**
 * A condition on a coded test names the answer that fires the rule. The rule
 * editor sends the answer's dictionary id; a reflex-rules configuration file
 * names it by its dictionary entry. Both must bind that answer's option, and a
 * value that is neither must be refused: a rule bound to an arbitrary option
 * looks configured and fires on the wrong answer (release-test run 2026-10-19,
 * "RT Positive" bound to the "RT Invalid" option).
 */
@RunWith(MockitoJUnitRunner.class)
public class ReflexDictionaryConditionTest {

    private static final String TRIGGER_TEST = "392";
    private static final String ADDED_TEST = "391";
    private static final String SPECIMEN = "4";

    private static final String POSITIVE_ID = "1434";
    private static final String NEGATIVE_ID = "1435";
    private static final String INVALID_ID = "1436";

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
        testAnalyte.setId("295");
        lenient().when(testAnalyteService.save(any(TestAnalyte.class))).thenReturn(testAnalyte);

        lenient().when(testResultService.getActiveTestResultsByTest(TRIGGER_TEST)).thenReturn(new ArrayList<>(
                Arrays.asList(option("716", INVALID_ID), option("714", POSITIVE_ID), option("715", NEGATIVE_ID))));
        lenient().when(dictionaryService.getDataForId(INVALID_ID)).thenReturn(entry(INVALID_ID, "RT Invalid", "RTINV"));
        lenient().when(dictionaryService.getDataForId(POSITIVE_ID))
                .thenReturn(entry(POSITIVE_ID, "RT Positive", "RTPOS"));
        lenient().when(dictionaryService.getDataForId(NEGATIVE_ID))
                .thenReturn(entry(NEGATIVE_ID, "RT Negative", "RTNEG"));

        lenient().when(testResultComponentService.getActiveComponentsByTestId(TRIGGER_TEST))
                .thenReturn(Collections.emptyList());
        lenient().when(testService.getTestById(TRIGGER_TEST)).thenReturn(triggerTest);
        lenient().when(testService.getTestById(ADDED_TEST)).thenReturn(addedTest);
        lenient().when(testService.getResultType(triggerTest)).thenReturn("D");
        lenient().when(ruleResultScope.resultTypeForComponent(TRIGGER_TEST, null, "D")).thenReturn("D");
        lenient().when(typeOfSampleService.getActiveTestsBySampleTypeId(SPECIMEN, false))
                .thenReturn(Arrays.asList(triggerTest, addedTest));
        lenient().when(baseObjectDAO.getTestReflexsByTestAnalyteId(anyString())).thenReturn(new ArrayList<>());
        lenient().when(reflexService.save(any(TestReflex.class))).thenAnswer(invocation -> {
            TestReflex saved = invocation.getArgument(0);
            saved.setId("12");
            return saved;
        });
    }

    @Test
    public void anAnswerNamedByItsDictionaryEntryBindsThatAnswer() {
        TestReflex saved = save("RT Positive");

        assertEquals(POSITIVE_ID, saved.getTestResult().getValue());
    }

    @Test
    public void anAnswerNamedByItsLocalAbbreviationBindsThatAnswer() {
        TestReflex saved = save("rtneg");

        assertEquals(NEGATIVE_ID, saved.getTestResult().getValue());
    }

    @Test
    public void anAnswerGivenByItsDictionaryIdStillBindsThatAnswer() {
        TestReflex saved = save(NEGATIVE_ID);

        assertEquals(NEGATIVE_ID, saved.getTestResult().getValue());
    }

    @Test
    public void anAnswerThatMatchesNoOptionIsRefusedInsteadOfBindingTheFirstOption() {
        LIMSRuntimeException refused = assertThrows(LIMSRuntimeException.class, () -> save("RT Bogus"));

        assertEquals(true, refused.getMessage().contains("RT Bogus"));
        verify(reflexService, never()).save(any(TestReflex.class));
    }

    @Test
    public void aConditionNamingTheAnswerByItsEntryKeepsTheAnswersDictionaryId() {
        save("RT Positive");

        assertEquals(POSITIVE_ID, savedCondition.getValue());
    }

    @Test
    public void anOptionWhoseDictionaryEntryIsGoneIsSkipped() {
        lenient().when(testResultService.getActiveTestResultsByTest(TRIGGER_TEST))
                .thenReturn(new ArrayList<>(Arrays.asList(option("717", "9999"), option("714", POSITIVE_ID))));
        lenient().when(dictionaryService.get("9999")).thenThrow(new IllegalStateException("dictionary 9999 is gone"));
        lenient().when(dictionaryService.getDataForId("9999")).thenReturn(null);

        TestReflex saved = save("RT Positive");

        assertEquals(POSITIVE_ID, saved.getTestResult().getValue());
    }

    private ReflexRuleCondition savedCondition;

    private TestReflex save(String conditionValue) {
        ReflexRule rule = new ReflexRule();
        rule.setRuleName("RT Malaria Positive Reflex");
        rule.setOverall(OverallOptions.ANY);
        rule.setActive(true);

        ReflexRuleCondition condition = new ReflexRuleCondition();
        condition.setTestId(TRIGGER_TEST);
        condition.setSampleId(SPECIMEN);
        condition.setRelation(NumericRelationOptions.EQUALS);
        condition.setValue(conditionValue);
        savedCondition = condition;
        rule.setConditions(new LinkedHashSet<>(Collections.singletonList(condition)));

        ReflexRuleAction action = new ReflexRuleAction();
        action.setReflexTestId(ADDED_TEST);
        action.setSampleId(SPECIMEN);
        rule.setActions(new LinkedHashSet<>(Collections.singletonList(action)));

        service.saveOrUpdateReflexRule(rule);

        ArgumentCaptor<TestReflex> captor = ArgumentCaptor.forClass(TestReflex.class);
        verify(reflexService).save(captor.capture());
        return captor.getValue();
    }

    private TestResult option(String id, String dictionaryId) {
        TestResult option = new TestResult();
        option.setId(id);
        option.setTest(triggerTest);
        option.setValue(dictionaryId);
        option.setTestResultType("D");
        return option;
    }

    private Dictionary entry(String id, String dictEntry, String abbreviation) {
        Dictionary entry = new Dictionary();
        entry.setId(id);
        entry.setDictEntry(dictEntry);
        entry.setLocalAbbreviation(abbreviation);
        return entry;
    }

    private org.openelisglobal.test.valueholder.Test namedTest(String id) {
        org.openelisglobal.test.valueholder.Test test = mock(org.openelisglobal.test.valueholder.Test.class);
        lenient().when(test.getId()).thenReturn(id);
        return test;
    }
}
