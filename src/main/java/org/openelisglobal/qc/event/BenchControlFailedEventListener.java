package org.openelisglobal.qc.event;

import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qaevent.service.QcViolationNceService;
import org.openelisglobal.qc.service.QCRuleViolationService;
import org.openelisglobal.qc.service.evaluator.RuleEvaluationResult;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Raises the QC-fail signal for a failing bench control, after the control
 * itself has committed (OGC-1147).
 *
 * <p>
 * Both routes end in the same place — an NCE with the affected patient analyses
 * linked — but the two routes are deliberately different. A MANUAL Fail is a
 * real quality failure on a measurement, so it becomes a
 * {@code qc_rule_violation} and enters the corrective-action workflow exactly
 * as an analyzer rule hit does. An RDT Invalid does not: a missing control line
 * is not a statistical rule hit, and recording it as one would corrupt
 * violation counts and sigma metrics.
 *
 * <p>
 * Mirrors {@link QCResultCreatedEventListener}'s annotations deliberately —
 * {@code REQUIRES_NEW} after commit, exceptions swallowed and logged. Here the
 * swallow is genuinely safe, unlike an inline catch: the capture transaction
 * has already committed, so a failure to raise the signal cannot take the
 * control result down with it.
 */
@Component
public class BenchControlFailedEventListener {

    /**
     * Rule code for a failing manual control. Deliberately not one of the Westgard
     * codes — those are Unicode subscripts (1₃ₛ, 2₂ₛ …) produced by the evaluators
     * over a window of historical points. This one records the technician's own
     * measured-vs-expected judgement, and reads as such in the violation list.
     */
    public static final String MANUAL_FAIL_RULE_CODE = "MANUAL_FAIL";

    @Autowired
    private QCRuleViolationService violationService;

    @Autowired
    private QcViolationNceService qcViolationNceService;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBenchControlFailed(BenchControlFailedEvent event) {
        QCResult result = event.getResult();
        if (result == null) {
            LogEvent.logWarn(this.getClass().getName(), "handleBenchControlFailed", "Event received with null result");
            return;
        }

        try {
            if (result.getSource() == QCSource.MANUAL) {
                violationService.createViolation(
                        RuleEvaluationResult.violation(MANUAL_FAIL_RULE_CODE, "REJECTION", List.of(result.getId()),
                                "Manual control outside the expected value and uncertainty entered at capture"),
                        result);
            } else {
                qcViolationNceService.createNceForFailedControl(result);
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "handleBenchControlFailed", "QC result " + result.getId()
                    + " is recorded, but the QC-fail signal was not raised: " + e.getMessage());
        }
    }
}
