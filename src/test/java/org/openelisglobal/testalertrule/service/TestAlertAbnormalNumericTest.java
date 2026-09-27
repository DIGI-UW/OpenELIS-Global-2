package org.openelisglobal.testalertrule.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
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
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.testalertrule.valueholder.TestAlertRule;

/**
 * An ABNORMAL alert rule fires on the results Results Entry and Validation flag
 * abnormal. It matched only coded results, so a rule on a numeric component
 * never fired for a value outside the patient's reference range.
 */
@RunWith(MockitoJUnitRunner.class)
public class TestAlertAbnormalNumericTest {

    private static final String USER = "1";

    @Mock
    private TestAlertRuleService alertRuleService;
    @Mock
    private RuleResultScope ruleResultScope;
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

    @InjectMocks
    private TestAlertEvaluationServiceImpl service;

    private Analysis analysis;

    @Before
    public void setUp() {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("301");
        test.setName("COVID-19 PCR");
        SampleItem item = new SampleItem();
        item.setSample(new Sample());
        analysis = new Analysis();
        analysis.setTest(test);
        analysis.setSampleItem(item);

        TestAlertRule rule = new TestAlertRule();
        rule.setName("Sputum N2 abnormal");
        rule.setTriggerType("ABNORMAL");
        rule.setEnabled(true);
        lenient().when(alertRuleService.getByTestId("301")).thenReturn(List.of(rule));
        lenient().when(ruleResultScope.matches(any(), any(), any())).thenReturn(true);
        lenient().when(sampleHumanService.getPatientForSample(any())).thenReturn(new Patient());

        ResultLimit limit = new ResultLimit();
        limit.setId("7");
        limit.setLowNormal(10);
        limit.setHighNormal(12);
        limit.setLowValid(0);
        limit.setHighValid(60);
        limit.setLowCritical(Double.POSITIVE_INFINITY);
        limit.setHighCritical(Double.POSITIVE_INFINITY);
        lenient().when(resultLimitService.getResultLimitForResult(any(), any(), any())).thenReturn(limit);
    }

    private Result numeric(String value) {
        Result result = new Result();
        result.setAnalysis(analysis);
        result.setResultType("N");
        result.setValue(value);
        return result;
    }

    @Test
    public void aNumericValueAboveTheRangeFiresTheAbnormalRule() {
        service.evaluateAndDispatch(numeric("20"), USER);
        verify(headerNotificationService).notifyUser(eq(USER), anyString());
    }

    @Test
    public void aNumericValueBelowTheRangeFiresTheAbnormalRule() {
        service.evaluateAndDispatch(numeric("5"), USER);
        verify(headerNotificationService).notifyUser(eq(USER), anyString());
    }

    @Test
    public void aNumericValueInsideTheRangeDoesNotFire() {
        service.evaluateAndDispatch(numeric("11"), USER);
        verify(headerNotificationService, never()).notifyUser(anyString(), anyString());
    }

    @Test
    public void aValueOutsideTheValidRangeIsAnEntryErrorNotAFinding() {
        service.evaluateAndDispatch(numeric("75"), USER);
        verify(headerNotificationService, never()).notifyUser(anyString(), anyString());
    }
}
