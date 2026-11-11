package org.openelisglobal.qc.event;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qaevent.service.QcViolationNceService;
import org.openelisglobal.qc.service.QCRuleViolationService;
import org.openelisglobal.qc.service.evaluator.RuleEvaluationResult;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerQualitativeControlFailedTest {

    @Mock
    private QCRuleViolationService violationService;

    @Mock
    private QcViolationNceService qcViolationNceService;

    @InjectMocks
    private BenchControlFailedEventListener listener;

    @Test
    public void anAnalyzerControlWithTheWrongAnswerBecomesARejectionViolation() {
        QCResult failing = new QCResult();
        failing.setId("qc-1");
        failing.setSource(QCSource.ASTM);
        failing.setQualitativeOutcome(QCQualitativeOutcome.FAIL);

        listener.handleBenchControlFailed(new BenchControlFailedEvent(this, failing));

        ArgumentCaptor<RuleEvaluationResult> violation = ArgumentCaptor.forClass(RuleEvaluationResult.class);
        verify(violationService).createViolation(violation.capture(), eq(failing));
        assertEquals(BenchControlFailedEventListener.QUALITATIVE_FAIL_RULE_CODE, violation.getValue().getRuleCode());
        assertEquals("REJECTION", violation.getValue().getSeverity());
        verify(qcViolationNceService, never()).createNceForFailedControl(any());
    }
}
