package org.openelisglobal.testcalculated.action.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.RuleResultScope;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.Operation;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;

/**
 * Which recorded result runs a calculation.
 *
 * <p>
 * A calculation runs because one of its parameters was measured, and a
 * parameter is a test AND a specimen AND a component. The executor asked its
 * operands that question when binding a result to a parameter, but decided
 * whether to run at all by asking only which test the calculation mentions — so
 * a COVID-19 PCR result recorded on Dry Tube re-ran a calculation whose operand
 * names the numeric Ct Value on Respiratory Swab, and the calculated result was
 * rewritten from a measurement it never reads.
 *
 * <p>
 * The matrix below is the contract: identical on all three axes runs, different
 * on any one axis does not.
 */
@RunWith(MockitoJUnitRunner.class)
public class CalculationTriggerScopeTest {

    private static final String PCR_TEST = "300";
    private static final String OTHER_TEST = "301";
    private static final String PRIMARY = "c-pcr";
    private static final String CT = "c-ct";
    private static final String SWAB = "30";
    private static final String DRY_TUBE = "24";

    @Mock
    private TestResultComponentService testResultComponentService;

    @InjectMocks
    private RuleResultScope scope;

    @Before
    public void setUp() {
        lenient().when(testResultComponentService.getActiveComponentsByTestId(PCR_TEST)).thenReturn(
                Arrays.asList(component(PCR_TEST, PRIMARY, true, "D"), component(PCR_TEST, CT, false, "N")));
        lenient().when(testResultComponentService.getActiveComponentsByTestId(OTHER_TEST))
                .thenReturn(new ArrayList<>());
    }

    /** The calculation the reported failure was configured with. */
    private Calculation ctOnSwab() {
        return calculation(operand(PCR_TEST, CT, SWAB), mathFunction("-"), integer("3"));
    }

    @Test
    public void runsOnTheMeasurementItsOperandNames() {
        assertTrue(TestCalculatedUtil.isTriggeredBy(scope, ctOnSwab(), result(PCR_TEST, CT, SWAB)));
    }

    @Test
    public void doesNotRunOnAnotherSpecimenOfTheSameTest() {
        assertFalse("Dry Tube is not the Respiratory Swab the operand reads",
                TestCalculatedUtil.isTriggeredBy(scope, ctOnSwab(), result(PCR_TEST, CT, DRY_TUBE)));
    }

    @Test
    public void doesNotRunOnAnotherComponentOfTheSameTest() {
        assertFalse("the coded PCR result is not the numeric Ct Value",
                TestCalculatedUtil.isTriggeredBy(scope, ctOnSwab(), result(PCR_TEST, PRIMARY, SWAB)));
    }

    @Test
    public void doesNotRunOnAnotherTest() {
        assertFalse(TestCalculatedUtil.isTriggeredBy(scope, ctOnSwab(), result(OTHER_TEST, CT, SWAB)));
    }

    /**
     * The exact shape that reached production: the result is on the trigger test,
     * but on the wrong component and the wrong specimen at once. Matching by test
     * alone accepted it.
     */
    @Test
    public void doesNotRunOnTheTestsPrimaryComponentRecordedOnAnUnrelatedSpecimen() {
        assertFalse(TestCalculatedUtil.isTriggeredBy(scope, ctOnSwab(), result(PCR_TEST, PRIMARY, DRY_TUBE)));
    }

    @Test
    public void runsWhenAnyOneOfSeveralOperandsReadsTheResult() {
        // A two-parameter calculation is run by either parameter being measured;
        // it is the completeness check downstream that decides it can compute.
        Calculation twoParams = calculation(operand(PCR_TEST, CT, SWAB), mathFunction("+"),
                operand(OTHER_TEST, null, DRY_TUBE));

        assertTrue(TestCalculatedUtil.isTriggeredBy(scope, twoParams, result(PCR_TEST, CT, SWAB)));
        assertTrue(TestCalculatedUtil.isTriggeredBy(scope, twoParams, result(OTHER_TEST, null, DRY_TUBE)));
        assertFalse(TestCalculatedUtil.isTriggeredBy(scope, twoParams, result(OTHER_TEST, null, SWAB)));
    }

    @Test
    public void readsTheSpecimenTheBuilderStoredOnTheOperand() {
        // The form writes the operand's specimen to sample_id, never to
        // sample_type_id, so a scope read only from the latter left every operand
        // matching every specimen of its test.
        Operation authoredByTheForm = new Operation();
        authoredByTheForm.setType(Operation.OperationType.TEST_RESULT);
        authoredByTheForm.setValue(PCR_TEST);
        authoredByTheForm.setComponentId(CT);
        authoredByTheForm.setSampleId(Integer.valueOf(SWAB));

        Calculation calculation = calculation(authoredByTheForm);

        assertTrue(TestCalculatedUtil.isTriggeredBy(scope, calculation, result(PCR_TEST, CT, SWAB)));
        assertFalse(TestCalculatedUtil.isTriggeredBy(scope, calculation, result(PCR_TEST, CT, DRY_TUBE)));
    }

    @Test
    public void anUnscopedOperandStillReadsItsTest() {
        // What every operand authored before the scope columns existed means.
        Operation legacy = new Operation();
        legacy.setType(Operation.OperationType.TEST_RESULT);
        legacy.setValue(PCR_TEST);

        Calculation calculation = calculation(legacy);

        assertTrue(TestCalculatedUtil.isTriggeredBy(scope, calculation, result(PCR_TEST, CT, DRY_TUBE)));
        assertFalse(TestCalculatedUtil.isTriggeredBy(scope, calculation, result(OTHER_TEST, CT, DRY_TUBE)));
    }

    @Test
    public void operandsThatAreNotTestResultsNeverTrigger() {
        assertFalse(TestCalculatedUtil.isTriggeredBy(scope, calculation(mathFunction("+"), integer("3")),
                result(PCR_TEST, CT, SWAB)));
    }

    @Test
    public void survivesACalculationWithNothingConfigured() {
        assertFalse(TestCalculatedUtil.isTriggeredBy(scope, null, result(PCR_TEST, CT, SWAB)));
        assertFalse(TestCalculatedUtil.isTriggeredBy(scope, calculation(), result(PCR_TEST, CT, SWAB)));
        assertFalse(TestCalculatedUtil.isTriggeredBy(scope, ctOnSwab(), null));
    }

    /**
     * A deactivated calculation calculates nothing.
     *
     * <p>
     * Deactivating flips the flag and leaves everything else standing, including
     * the result_calculation rows that bind a patient's results to the parameters.
     * The recomputation pass reads those rows, and it read them without asking
     * whether the calculation was still switched on — so a deactivated rule went on
     * producing results for every patient it had already run for, which is every
     * patient the switch was meant to stop it for.
     */
    @Test
    public void aDeactivatedCalculationDoesNotRunOnAMatchingResult() {
        Result matching = result(PCR_TEST, CT, SWAB);

        assertTrue("switched on, it runs", TestCalculatedUtil.runsFor(scope, active(ctOnSwab()), matching));
        assertFalse("switched off, the same result runs nothing",
                TestCalculatedUtil.runsFor(scope, deactivated(ctOnSwab()), matching));
    }

    @Test
    public void deactivatingThenReactivatingIsFollowedOnTheNextResult() {
        Result matching = result(PCR_TEST, CT, SWAB);
        Calculation calculation = active(ctOnSwab());

        assertTrue(TestCalculatedUtil.runsFor(scope, calculation, matching));

        calculation.setActive(false);
        assertFalse("a rule switched off after it last ran stops",
                TestCalculatedUtil.runsFor(scope, calculation, matching));

        calculation.setActive(true);
        assertTrue("and resumes when it is switched back on", TestCalculatedUtil.runsFor(scope, calculation, matching));
    }

    @Test
    public void beingSwitchedOnIsNotEnoughOnItsOwn() {
        // The state check is additional to the scope check, never a substitute:
        // an active calculation still only runs on the measurement it names.
        assertFalse(TestCalculatedUtil.runsFor(scope, active(ctOnSwab()), result(PCR_TEST, CT, DRY_TUBE)));
        assertFalse(TestCalculatedUtil.runsFor(scope, active(ctOnSwab()), result(PCR_TEST, PRIMARY, SWAB)));
        assertFalse(TestCalculatedUtil.runsFor(scope, active(ctOnSwab()), result(OTHER_TEST, CT, SWAB)));
    }

    @Test
    public void aCalculationWithNoFlagSetIsTreatedAsOff() {
        Calculation unset = ctOnSwab();
        unset.setActive(null);

        assertFalse(TestCalculatedUtil.runsFor(scope, unset, result(PCR_TEST, CT, SWAB)));
        assertFalse(TestCalculatedUtil.runsFor(scope, null, result(PCR_TEST, CT, SWAB)));
    }

    private Calculation active(Calculation calculation) {
        calculation.setActive(true);
        return calculation;
    }

    private Calculation deactivated(Calculation calculation) {
        calculation.setActive(false);
        return calculation;
    }

    private Calculation calculation(Operation... operations) {
        Calculation calculation = new Calculation();
        List<Operation> list = new ArrayList<>(Arrays.asList(operations));
        for (int order = 0; order < list.size(); order++) {
            list.get(order).setId(order);
            list.get(order).setOrder(order);
        }
        calculation.setOperations(list);
        return calculation;
    }

    private Operation operand(String testId, String componentId, String sampleTypeId) {
        Operation operation = new Operation();
        operation.setType(Operation.OperationType.TEST_RESULT);
        operation.setValue(testId);
        operation.setComponentId(componentId);
        operation.setSampleTypeId(sampleTypeId == null ? null : Integer.valueOf(sampleTypeId));
        return operation;
    }

    private Operation mathFunction(String value) {
        Operation operation = new Operation();
        operation.setType(Operation.OperationType.MATH_FUNCTION);
        operation.setValue(value);
        return operation;
    }

    private Operation integer(String value) {
        Operation operation = new Operation();
        operation.setType(Operation.OperationType.INTEGER);
        operation.setValue(value);
        return operation;
    }

    private TestResultComponent component(String testId, String id, boolean primary, String resultType) {
        TestResultComponent component = new TestResultComponent();
        component.setId(id);
        component.setTestId(testId);
        component.setIsPrimary(primary);
        component.setResultType(resultType);
        return component;
    }

    private Result result(String testId, String componentId, String sampleTypeId) {
        org.openelisglobal.test.valueholder.Test test = mock(org.openelisglobal.test.valueholder.Test.class);
        lenient().when(test.getId()).thenReturn(testId);

        TestResult testResult = new TestResult();
        testResult.setTest(test);
        testResult.setComponentId(componentId);

        SampleItem sampleItem = mock(SampleItem.class);
        lenient().when(sampleItem.getTypeOfSampleId()).thenReturn(sampleTypeId);
        Analysis analysis = mock(Analysis.class);
        lenient().when(analysis.getSampleItem()).thenReturn(sampleItem);

        Result result = new Result();
        result.setTestResult(testResult);
        result.setAnalysis(analysis);
        return result;
    }
}
