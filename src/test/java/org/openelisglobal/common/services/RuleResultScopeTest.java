package org.openelisglobal.common.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;

/**
 * A rule names a measurement — test AND specimen AND component. These are the
 * decisions every rule engine used to make by test alone, which is how a rule
 * authored for a numeric Ct Value came to be evaluated against the coded PCR
 * Result recorded beside it.
 */
@RunWith(MockitoJUnitRunner.class)
public class RuleResultScopeTest {

    private static final String TEST_ID = "300";
    private static final String PRIMARY = "c-pcr";
    private static final String CT = "c-ct";
    private static final String VIRAL_LOAD = "c-vl";

    @Mock
    private TestResultComponentService testResultComponentService;

    @InjectMocks
    private RuleResultScope scope;

    private org.openelisglobal.test.valueholder.Test covidTest;

    @Before
    public void setUp() {
        covidTest = mock(org.openelisglobal.test.valueholder.Test.class);
        lenient().when(covidTest.getId()).thenReturn(TEST_ID);
        lenient().when(testResultComponentService.getActiveComponentsByTestId(TEST_ID)).thenReturn(Arrays
                .asList(component(PRIMARY, true, "D"), component(CT, false, "N"), component(VIRAL_LOAD, false, "N")));
    }

    @Test
    public void matches_shouldAcceptOnlyTheComponentTheRuleNames() {
        Result ctResult = result(CT, "1");

        assertTrue(scope.matches(ctResult, CT, null));
        assertFalse("a Ct rule must not be evaluated against the coded PCR result",
                scope.matches(result(PRIMARY, "1"), CT, null));
        assertFalse("nor against the other numeric component", scope.matches(result(VIRAL_LOAD, "1"), CT, null));
    }

    @Test
    public void matches_shouldTreatAnUnscopedRuleAsEveryComponent() {
        // What a rule authored before components existed meant, and what it has
        // to keep meaning after the migration.
        assertTrue(scope.matches(result(PRIMARY, "1"), null, null));
        assertTrue(scope.matches(result(CT, "1"), null, null));
        assertTrue(scope.matches(result(VIRAL_LOAD, "1"), "", null));
    }

    @Test
    public void matches_shouldIsolateSpecimens() {
        Result swab = result(CT, "30");
        Result saliva = result(CT, "31");

        assertTrue(scope.matches(swab, CT, "30"));
        assertFalse("the same component on another specimen is another measurement", scope.matches(saliva, CT, "30"));
        assertTrue("an unscoped rule runs on every specimen", scope.matches(saliva, CT, null));
    }

    @Test
    public void matches_shouldRequireBothAxesTogether() {
        assertFalse("right specimen, wrong component", scope.matches(result(PRIMARY, "30"), CT, "30"));
        assertFalse("right component, wrong specimen", scope.matches(result(CT, "31"), CT, "30"));
        assertTrue(scope.matches(result(CT, "30"), CT, "30"));
    }

    /**
     * The regression matrix every engine is held to.
     *
     * <p>
     * A rule's trigger names a test AND a specimen AND a component, and all three
     * have to be the recorded result's for the rule to run. Each row below is a
     * measurement that differs from the rule on exactly one axis, and every one of
     * them used to satisfy a rule that matched by test alone.
     */
    @Test
    public void matchesTrigger_shouldRequireTestSpecimenAndComponentTogether() {
        assertTrue("same test, same specimen, same component",
                scope.matchesTrigger(result(CT, "30"), TEST_ID, CT, "30"));
        assertFalse("same test, different specimen, same component",
                scope.matchesTrigger(result(CT, "31"), TEST_ID, CT, "30"));
        assertFalse("same test, same specimen, different component",
                scope.matchesTrigger(result(PRIMARY, "30"), TEST_ID, CT, "30"));
        assertFalse("different test", scope.matchesTrigger(otherTestResult(CT, "30"), TEST_ID, CT, "30"));
    }

    @Test
    public void matchesTrigger_shouldNotInferAnAxisTheRuleStates() {
        // The specimen of another result in the same save, or the one the test is
        // configured for, is not this result's specimen. Only what the result
        // itself carries counts.
        Result onSaliva = result(CT, "31");

        assertFalse(scope.matchesTrigger(onSaliva, TEST_ID, CT, "30"));
        assertTrue("and it still matches the specimen it is actually on",
                scope.matchesTrigger(onSaliva, TEST_ID, CT, "31"));
    }

    @Test
    public void matchesTrigger_shouldTreatABlankAxisAsUnscoped() {
        // A rule authored before either axis existed names neither, and has to
        // keep running everywhere it ran before.
        assertTrue(scope.matchesTrigger(result(PRIMARY, "30"), null, null, null));
        assertTrue(scope.matchesTrigger(result(CT, "31"), "", "", ""));
        assertTrue("a test-only rule is still a test-only rule",
                scope.matchesTrigger(result(CT, "31"), TEST_ID, null, null));
        assertFalse("but not on another test's result",
                scope.matchesTrigger(otherTestResult(CT, "31"), TEST_ID, null, null));
    }

    @Test
    public void matchesTrigger_shouldRejectAResultThatCarriesNoTest() {
        assertFalse(scope.matchesTrigger(null, TEST_ID, CT, "30"));
        assertFalse(scope.matchesTrigger(new Result(), TEST_ID, CT, "30"));
    }

    @Test
    public void componentIdOf_shouldReadALegacyResultAsThePrimaryComponent() {
        // A result written before components carries no component on its
        // test_result row; it belongs to the primary, the same reading Results
        // Entry applies.
        Result legacy = result(null, "30");

        assertEquals(PRIMARY, scope.componentIdOf(legacy));
        assertTrue("so a migrated primary-component rule still matches it", scope.matches(legacy, PRIMARY, null));
    }

    @Test
    public void resultTypeForComponent_shouldComeFromTheComponentNotTheParentTest() {
        // The parent test reports the coded type of its primary component; a
        // numeric condition on Ct Value must not be validated against that.
        assertEquals("N", scope.resultTypeForComponent(TEST_ID, CT, "D"));
        assertEquals("N", scope.resultTypeForComponent(TEST_ID, VIRAL_LOAD, "D"));
        assertEquals("D", scope.resultTypeForComponent(TEST_ID, PRIMARY, "D"));
    }

    @Test
    public void resultTypeForComponent_shouldFallBackToTheTestWhenUnscoped() {
        assertEquals("D", scope.resultTypeForComponent(TEST_ID, null, "D"));
        when(testResultComponentService.getActiveComponentsByTestId("99")).thenReturn(new ArrayList<>());
        assertEquals("N", scope.resultTypeForComponent("99", "missing", "N"));
    }

    @Test
    public void primaryComponentId_shouldPickTheFlaggedPrimary() {
        assertEquals(PRIMARY, scope.primaryComponentId(TEST_ID));
    }

    @Test
    public void primaryComponentId_shouldFallBackToTheOnlyComponentWhenNoneIsFlagged() {
        when(testResultComponentService.getActiveComponentsByTestId("77"))
                .thenReturn(Collections.singletonList(component("c-only", false, "N")));

        assertEquals("c-only", scope.primaryComponentId("77"));
    }

    @Test
    public void primaryComponentId_shouldBeNullForATestWithNoComponents() {
        when(testResultComponentService.getActiveComponentsByTestId("88")).thenReturn(new ArrayList<>());

        assertNull(scope.primaryComponentId("88"));
        assertNull(scope.primaryComponentId(null));
    }

    private TestResultComponent component(String id, boolean primary, String resultType) {
        TestResultComponent component = new TestResultComponent();
        component.setId(id);
        component.setTestId(TEST_ID);
        component.setIsPrimary(primary);
        component.setResultType(resultType);
        return component;
    }

    private Result result(String componentId, String sampleTypeId) {
        return resultOfTest(covidTest, componentId, sampleTypeId);
    }

    /** The same measurement recorded under a different test entirely. */
    private Result otherTestResult(String componentId, String sampleTypeId) {
        org.openelisglobal.test.valueholder.Test other = mock(org.openelisglobal.test.valueholder.Test.class);
        lenient().when(other.getId()).thenReturn("999");
        return resultOfTest(other, componentId, sampleTypeId);
    }

    private Result resultOfTest(org.openelisglobal.test.valueholder.Test test, String componentId,
            String sampleTypeId) {
        TestResult testResult = new TestResult();
        testResult.setComponentId(componentId);
        testResult.setTest(test);

        SampleItem sampleItem = mock(SampleItem.class);
        lenient().when(sampleItem.getTypeOfSampleId()).thenReturn(sampleTypeId);
        Analysis analysis = mock(Analysis.class);
        lenient().when(analysis.getSampleItem()).thenReturn(sampleItem);

        Result result = new Result();
        result.setTestResult(testResult);
        result.setAnalysis(analysis);
        return result;
    }

    /** Guards the list-shape assumption the fallbacks above rely on. */
    @Test
    public void componentIdOf_shouldBeNullWithoutATestResult() {
        List<TestResultComponent> none = new ArrayList<>();
        lenient().when(testResultComponentService.getActiveComponentsByTestId("0")).thenReturn(none);

        assertNull(scope.componentIdOf(null));
        assertNull(scope.componentIdOf(new Result()));
    }
}
