package org.openelisglobal.testcatalog.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcalculated.service.TestCalculationService;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.Operation;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.action.bean.ReflexRuleAction;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReflexCalcViewServiceImpl implements ReflexCalcViewService {

    /** Result types whose stored value is a dictionary id, not the value itself. */
    private static final Set<String> DICTIONARY_TYPES = new HashSet<>(Arrays.asList("D", "M", "C"));

    @Autowired
    private TestReflexService testReflexService;

    @Autowired
    private TestCalculationService testCalculationService;

    @Autowired
    private TestService testService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TestResultComponentService testResultComponentService;

    /**
     * A test's reflex rules and calculations, each said the way the rule builder
     * that created it says it.
     *
     * <p>
     * A row is named by its Reflex Rules record, reached through the action that
     * points back at it. A reflex row also carries notes meant for the report, and
     * the summary was printing the internal one where the name belongs, so a rule
     * configured as "Covid-Reflex" read as whatever its author happened to note.
     */
    @Override
    @Transactional(readOnly = true)
    public ReflexCalcView getForTest(String testId) {
        ReflexCalcView view = new ReflexCalcView();

        Map<String, ReflexRule> ruleByTestReflexId = ruleByTestReflexId();
        for (TestReflex reflex : testReflexService.getTestReflexsByTestId(testId)) {
            ReflexRow row = new ReflexRow();
            row.id = reflex.getId();
            ReflexRule rule = ruleByTestReflexId.get(reflex.getId());
            row.ruleId = rule == null ? null : rule.getId();
            row.reflexTests = describeGeneratedTest(reflex);
            row.triggerCondition = describeTrigger(reflex);
            row.ruleName = rule != null && rule.getRuleName() != null && !rule.getRuleName().isBlank()
                    ? rule.getRuleName()
                    : row.reflexTests;
            view.reflexRules.add(row);
        }

        Integer tid = parseIntOrNull(testId);
        for (Calculation calc : testCalculationService.getAll()) {
            if (Boolean.FALSE.equals(calc.getActive())) {
                continue;
            }
            if (tid != null && tid.equals(calc.getTestId())) {
                view.calculatedBy.add(toCalcRow(calc));
            } else if (operationsReference(calc, testId)) {
                view.feedsInto.add(toCalcRow(calc));
            }
        }
        return view;
    }

    /**
     * test_reflex id → the Reflex Rules record that created it. A rule's actions
     * record the test_reflex row each one produced, which is the only link between
     * the legacy reflex table and the rules screen.
     */
    private Map<String, ReflexRule> ruleByTestReflexId() {
        Map<String, ReflexRule> byTestReflexId = new HashMap<>();
        for (ReflexRule rule : testReflexService.getAllReflexRules()) {
            if (rule.getId() == null || rule.getActions() == null) {
                continue;
            }
            for (ReflexRuleAction action : rule.getActions()) {
                if (action.getTestReflexId() != null) {
                    byTestReflexId.put(String.valueOf(action.getTestReflexId()), rule);
                }
            }
        }
        return byTestReflexId;
    }

    /**
     * What the rule fires on, said the way the rule builder says it: the coded
     * result by name, and the measurement it is read from where the trigger names
     * one.
     *
     * <p>
     * A coded test_result stores a dictionary id, so read out unresolved the
     * trigger said "EQUALS 1578" — a database key shown to a reader with no way to
     * know what it means.
     */
    private String describeTrigger(TestReflex reflex) {
        TestResult testResult = reflex.getTestResult();
        String value = null;
        if (testResult != null && testResult.getValue() != null && !testResult.getValue().isBlank()) {
            value = DICTIONARY_TYPES.contains(testResult.getTestResultType()) ? dictionaryValue(testResult.getValue())
                    : testResult.getValue();
        }
        if (value == null || value.isBlank()) {
            value = reflex.getNonDictionaryValue();
        }
        String relation = reflex.getRelation() != null ? reflex.getRelation().toString() + " " : "";
        String condition = value == null || value.isBlank() ? relation.trim() : (relation + value).trim();
        if (condition.isBlank()) {
            condition = "Any result";
        }
        String measurement = componentLabel(reflex.getTest(), reflex.getComponentId());
        return measurement == null ? condition : measurement + ": " + condition;
    }

    /** The test the rule adds, on the specimen it is configured to report on. */
    private String describeGeneratedTest(TestReflex reflex) {
        Test added = reflex.getAddedTest();
        if (added == null) {
            return null;
        }
        return withSpecimen(added, reflex.getAddedSampleTypeId());
    }

    /**
     * A dictionary id read out by name, resolved the way the rule builder's own
     * option list resolves it.
     */
    private String dictionaryValue(String dictionaryId) {
        if (dictionaryId == null || dictionaryId.isBlank()) {
            return dictionaryId;
        }
        Dictionary dictionary = dictionaryService.getDictionaryById(dictionaryId);
        return dictionary == null ? dictionaryId : dictionary.getLocalizedName();
    }

    /**
     * The label a test gives one of its components, or null where the caller names
     * no component or the test no longer reports it.
     */
    private String componentLabel(Test test, String componentId) {
        if (test == null || componentId == null || componentId.isBlank()) {
            return null;
        }
        return testResultComponentService.getActiveComponentsByTestId(test.getId()).stream()
                .filter(component -> componentId.equals(component.getId())).map(TestResultComponent::getLabel)
                .findFirst().orElse(null);
    }

    /**
     * A test named with the specimen the rule chose.
     *
     * <p>
     * getLocalizedTestNameWithType augments a name with every specimen the test is
     * configured for — "COVID-19 PCR(Dry Tube +1)" — which is the ambiguity this
     * summary has to resolve, not repeat. Where the rule names one specimen the
     * name carries that one; where it names none the configured list is still the
     * most the summary can honestly say.
     */
    private String withSpecimen(Test test, String sampleTypeId) {
        if (test == null) {
            return null;
        }
        if (sampleTypeId != null && !sampleTypeId.isBlank()) {
            TypeOfSample typeOfSample = typeOfSampleService.get(sampleTypeId);
            if (typeOfSample != null && typeOfSample.getLocalizedName() != null
                    && !typeOfSample.getLocalizedName().isBlank()) {
                return test.getLocalizedName() + "(" + typeOfSample.getLocalizedName() + ")";
            }
        }
        return TestServiceImpl.getLocalizedTestNameWithType(test);
    }

    /**
     * A calculation, named by where it writes. A test can run on several specimens
     * and report several components, so its name alone does not say which
     * measurement the calculation produces.
     */
    private CalcRow toCalcRow(Calculation calc) {
        CalcRow row = new CalcRow();
        row.id = calc.getId();
        row.name = calc.getName();
        row.formula = buildFormula(calc);
        if (calc.getTestId() != null) {
            row.outputTest = measurementLabel(String.valueOf(calc.getTestId()),
                    calc.getSampleId() == null ? null : String.valueOf(calc.getSampleId()), calc.getComponentId());
        }
        return row;
    }

    /**
     * The formula as the builder composes it: each parameter named by the
     * measurement it reads, not by the test id stored in its place.
     */
    private String buildFormula(Calculation calc) {
        List<Operation> operations = calc.getOperations();
        if (operations == null || operations.isEmpty()) {
            return dictionaryValue(calc.getResult());
        }
        StringBuilder sb = new StringBuilder();
        operations.stream().sorted().forEach(op -> {
            String token = describeOperation(op);
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(token);
        });
        return sb.toString();
    }

    /**
     * One term of a formula. A TEST_RESULT operation holds a test id in its value,
     * with the specimen and component beside it saying which of that test's
     * measurements is read.
     */
    private String describeOperation(Operation op) {
        if (op == null) {
            return "";
        }
        if (Operation.OperationType.TEST_RESULT.equals(op.getType())) {
            String label = measurementLabel(op.getValue(), op.getScopedSampleTypeId(), op.getComponentId());
            return label == null ? op.getValue() : "[" + label + "]";
        }
        if (op.getValue() != null && !op.getValue().isBlank()) {
            return op.getValue();
        }
        return op.getType() != null ? op.getType().toString() : "";
    }

    /**
     * "Test(Specimen) — Component": the same three things the rule builder makes
     * the author choose, so the summary names exactly the measurement the rule
     * does.
     */
    private String measurementLabel(String testId, String sampleTypeId, String componentId) {
        if (testId == null || testId.isBlank()) {
            return null;
        }
        Test test = testService.getTestById(testId);
        if (test == null) {
            return null;
        }
        String label = withSpecimen(test, sampleTypeId);
        String component = componentLabel(test, componentId);
        return component == null ? label : label + " — " + component;
    }

    private boolean operationsReference(Calculation calc, String testId) {
        List<Operation> operations = calc.getOperations();
        if (operations == null) {
            return false;
        }
        return operations.stream().anyMatch(op -> testId.equals(op.getValue()));
    }

    private Integer parseIntOrNull(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
