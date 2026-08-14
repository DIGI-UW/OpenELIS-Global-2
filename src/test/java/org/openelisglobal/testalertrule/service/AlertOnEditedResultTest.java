package org.openelisglobal.testalertrule.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.RuleResultScope;
import org.openelisglobal.notification.service.sender.AsyncNotificationDispatcher;
import org.openelisglobal.notifications.service.HeaderNotificationService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.testalertrule.valueholder.TestAlertRule;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * An alert rule fires on a corrected value, not only on a first one.
 *
 * <p>
 * A correction is exactly when an alert is wanted: the value that crosses into
 * the range is often the second one entered. Editing appeared not to raise
 * alerts at all, and the cause was in the comparison rather than in the wiring.
 * A numeric result is persisted to the test's significant digits, so entering
 * 200 on a two-digit test stores "200.00" — and the field the user edits shows
 * that stored form, so an edit posts "200.00" where a fresh entry posted "200".
 * A rule authored for 200 holds "200", and comparing the two as text said they
 * differ. Same measurement, same rule, two spellings of the number.
 *
 * <p>
 * A real {@link RuleResultScope} is used rather than a stubbed one, so the
 * three scope axes are genuinely evaluated here and not asserted from a stub.
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertOnEditedResultTest {

    private static final String TEST_ID = "6";
    private static final String PRIMARY = "c-albumin";
    private static final String SECONDARY = "c-albumin-b";
    private static final String DBS = "26";
    private static final String URINES = "1";
    private static final String USER = "1";

    @Mock
    private TestAlertRuleService alertRuleService;

    @Mock
    private AlertService alertService;

    @Mock
    private ResultLimitService resultLimitService;

    @Mock
    private ResultService resultService;

    @Mock
    private HeaderNotificationService headerNotificationService;

    @Mock
    private RoleService roleService;

    @Mock
    private SampleHumanService sampleHumanService;

    @Mock
    private AsyncNotificationDispatcher asyncNotificationDispatcher;

    @Mock
    private TestResultComponentService testResultComponentService;

    @InjectMocks
    private TestAlertEvaluationServiceImpl evaluation;

    private org.openelisglobal.test.valueholder.Test albumin;

    @Before
    public void setUp() {
        RuleResultScope scope = new RuleResultScope();
        ReflectionTestUtils.setField(scope, "testResultComponentService", testResultComponentService);
        ReflectionTestUtils.setField(evaluation, "ruleResultScope", scope);

        albumin = mock(org.openelisglobal.test.valueholder.Test.class);
        lenient().when(albumin.getId()).thenReturn(TEST_ID);
        lenient().when(albumin.getName()).thenReturn("Albumin");
        lenient().when(testResultComponentService.getActiveComponentsByTestId(TEST_ID))
                .thenReturn(Arrays.asList(component(PRIMARY, true), component(SECONDARY, false)));
        // No authored critical bounds, so the CRITICAL_RESULT path stays out of
        // the way of the SPECIFIC_VALUE rule under test.
        lenient().when(resultLimitService.getResultLimitForResult(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(null);
    }

    /** The rule the reported failure was configured with. */
    private void ruleForTwoHundredOnDbsPrimary() {
        TestAlertRule rule = new TestAlertRule();
        rule.setName("Albu-demoA");
        rule.setTestId(TEST_ID);
        rule.setEnabled(true);
        rule.setTriggerType("SPECIFIC_VALUE");
        rule.setTriggerValue("200");
        rule.setComponentId(PRIMARY);
        rule.setSampleTypeId(DBS);
        lenient().when(alertRuleService.getByTestId(TEST_ID)).thenReturn(Collections.singletonList(rule));
    }

    @Test
    public void firesOnANewlyEnteredValue() {
        ruleForTwoHundredOnDbsPrimary();

        evaluation.evaluateAndDispatch(result(PRIMARY, DBS, "200"), USER);

        verify(headerNotificationService).notifyUser(eq(USER), contains("Albu-demoA"));
    }

    @Test
    public void firesOnTheSameValueAfterAnEditReformattedIt() {
        // Entered as 201, corrected to 200: the edit posts what the field was
        // showing, which is the value at the test's significant digits.
        ruleForTwoHundredOnDbsPrimary();

        evaluation.evaluateAndDispatch(result(PRIMARY, DBS, "200.00"), USER);

        verify(headerNotificationService).notifyUser(eq(USER), contains("Albu-demoA"));
    }

    @Test
    public void doesNotFireOnAnEditedValueThatStillDoesNotMatch() {
        ruleForTwoHundredOnDbsPrimary();

        evaluation.evaluateAndDispatch(result(PRIMARY, DBS, "201.00"), USER);

        verify(headerNotificationService, never()).notifyUser(anyString(), anyString());
    }

    @Test
    public void doesNotFireOnTheRightValueRecordedOnAnotherSpecimen() {
        ruleForTwoHundredOnDbsPrimary();

        evaluation.evaluateAndDispatch(result(PRIMARY, URINES, "200.00"), USER);

        verify(headerNotificationService, never()).notifyUser(anyString(), anyString());
    }

    @Test
    public void doesNotFireOnTheRightValueRecordedOnAnotherComponent() {
        ruleForTwoHundredOnDbsPrimary();

        evaluation.evaluateAndDispatch(result(SECONDARY, DBS, "200.00"), USER);

        verify(headerNotificationService, never()).notifyUser(anyString(), anyString());
    }

    @Test
    public void doesNotTreatTrailingZeroesAsEqualForACodedResult() {
        // A coded value is a dictionary id, not a quantity; "1578" and "1578.0"
        // are different entries and must not be reconciled as numbers.
        TestAlertRule rule = new TestAlertRule();
        rule.setName("Coded");
        rule.setTestId(TEST_ID);
        rule.setEnabled(true);
        rule.setTriggerType("SPECIFIC_VALUE");
        rule.setTriggerValue("1578");
        rule.setComponentId(PRIMARY);
        lenient().when(alertRuleService.getByTestId(TEST_ID)).thenReturn(Collections.singletonList(rule));

        Result coded = result(PRIMARY, DBS, "1578.0");
        coded.setResultType("D");
        evaluation.evaluateAndDispatch(coded, USER);

        verify(headerNotificationService, never()).notifyUser(anyString(), anyString());

        Result exact = result(PRIMARY, DBS, "1578");
        exact.setResultType("D");
        evaluation.evaluateAndDispatch(exact, USER);

        verify(headerNotificationService, times(1)).notifyUser(eq(USER), contains("Coded"));
    }

    @Test
    public void aDisabledRuleNeverFires() {
        TestAlertRule rule = new TestAlertRule();
        rule.setName("Off");
        rule.setTestId(TEST_ID);
        rule.setEnabled(false);
        rule.setTriggerType("SPECIFIC_VALUE");
        rule.setTriggerValue("200");
        rule.setComponentId(PRIMARY);
        rule.setSampleTypeId(DBS);
        lenient().when(alertRuleService.getByTestId(TEST_ID)).thenReturn(Collections.singletonList(rule));

        evaluation.evaluateAndDispatch(result(PRIMARY, DBS, "200.00"), USER);

        verify(headerNotificationService, never()).notifyUser(anyString(), anyString());
    }

    /**
     * The enabled flag is read on every result, so switching a rule off stops the
     * next alert and switching it back on resumes it — no restart, no cache to
     * clear.
     */
    @Test
    public void disablingThenReenablingIsFollowedOnTheNextResult() {
        TestAlertRule rule = new TestAlertRule();
        rule.setName("Albu-demoA");
        rule.setTestId(TEST_ID);
        rule.setEnabled(true);
        rule.setTriggerType("SPECIFIC_VALUE");
        rule.setTriggerValue("200");
        rule.setComponentId(PRIMARY);
        rule.setSampleTypeId(DBS);
        lenient().when(alertRuleService.getByTestId(TEST_ID)).thenReturn(Collections.singletonList(rule));

        evaluation.evaluateAndDispatch(result(PRIMARY, DBS, "200.00"), USER);
        verify(headerNotificationService, times(1)).notifyUser(eq(USER), contains("Albu-demoA"));

        rule.setEnabled(false);
        evaluation.evaluateAndDispatch(result(PRIMARY, DBS, "200.00"), USER);
        verify(headerNotificationService, times(1)).notifyUser(eq(USER), contains("Albu-demoA"));

        rule.setEnabled(true);
        evaluation.evaluateAndDispatch(result(PRIMARY, DBS, "200.00"), USER);
        verify(headerNotificationService, times(2)).notifyUser(eq(USER), contains("Albu-demoA"));
    }

    @Test
    public void aRuleWithNoEnabledFlagSetIsTreatedAsOff() {
        TestAlertRule rule = new TestAlertRule();
        rule.setName("Unset");
        rule.setTestId(TEST_ID);
        rule.setEnabled(null);
        rule.setTriggerType("ALL");
        rule.setComponentId(PRIMARY);
        rule.setSampleTypeId(DBS);
        lenient().when(alertRuleService.getByTestId(TEST_ID)).thenReturn(Collections.singletonList(rule));

        evaluation.evaluateAndDispatch(result(PRIMARY, DBS, "200.00"), USER);

        verify(headerNotificationService, never()).notifyUser(anyString(), anyString());
    }

    private TestResultComponent component(String id, boolean primary) {
        TestResultComponent component = new TestResultComponent();
        component.setId(id);
        component.setTestId(TEST_ID);
        component.setIsPrimary(primary);
        component.setResultType("N");
        return component;
    }

    private Result result(String componentId, String sampleTypeId, String value) {
        TestResult testResult = new TestResult();
        testResult.setTest(albumin);
        testResult.setComponentId(componentId);

        SampleItem sampleItem = mock(SampleItem.class);
        lenient().when(sampleItem.getTypeOfSampleId()).thenReturn(sampleTypeId);
        Analysis analysis = mock(Analysis.class);
        lenient().when(analysis.getTest()).thenReturn(albumin);
        lenient().when(analysis.getSampleItem()).thenReturn(sampleItem);

        Result result = new Result();
        result.setTestResult(testResult);
        result.setAnalysis(analysis);
        result.setResultType("N");
        result.setValue(value);
        return result;
    }
}
