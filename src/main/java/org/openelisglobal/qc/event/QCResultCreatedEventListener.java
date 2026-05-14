package org.openelisglobal.qc.event;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.service.QCRuleViolationService;
import org.openelisglobal.qc.service.WestgardRuleEvaluationService;
import org.openelisglobal.qc.service.evaluator.RuleEvaluationResult;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for QCResultCreatedEvent (T098/T099/T109).
 *
 * Handles automatic Westgard rule evaluation when a new QC result is created.
 * Runs asynchronously to avoid blocking the main request thread. Uses
 * QCRuleViolationService to create violations and trigger alerts.
 */
@Component
public class QCResultCreatedEventListener {

    @Autowired
    private WestgardRuleEvaluationService ruleEvaluationService;

    @Autowired
    private QCRuleViolationService violationService;

    @Autowired
    private QCResultDAO resultDAO;

    /**
     * Handle QCResultCreatedEvent asynchronously.
     *
     * @param event The event containing the new QC result
     */
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleQCResultCreated(QCResultCreatedEvent event) {
        QCResult result = event.getResult();

        if (result == null) {
            LogEvent.logWarn(this.getClass().getName(), "handleQCResultCreated", "Event received with null result");
            return;
        }

        LogEvent.logInfo(this.getClass().getName(), "handleQCResultCreated",
                "Processing QC result: " + result.getId() + " for control lot: " + result.getControlLotId());

        try {
            // Evaluate all enabled Westgard rules
            List<RuleEvaluationResult> evaluationResults = ruleEvaluationService.evaluateAllRules(result.getId());

            // Process any violations using the violation service
            int violationsCreated = 0;
            for (RuleEvaluationResult evalResult : evaluationResults) {
                if (evalResult.isViolated()) {
                    QCRuleViolation violation = violationService.createViolation(evalResult, result);
                    if (violation != null) {
                        violationsCreated++;
                    }
                }
            }

            // Update result_status: REJECTED if any REJECTION-severity violation, ACCEPTED
            // otherwise
            boolean hasRejection = evaluationResults.stream()
                    .anyMatch(r -> r.isViolated() && "REJECTION".equals(r.getSeverity()));
            String newStatus = hasRejection ? "REJECTED" : "ACCEPTED";

            Optional<QCResult> freshResult = resultDAO.get(result.getId());
            if (freshResult.isPresent()) {
                QCResult toUpdate = freshResult.get();
                toUpdate.setResultStatus(newStatus);
                resultDAO.update(toUpdate);
            }

            if (violationsCreated > 0) {
                LogEvent.logInfo(this.getClass().getName(), "handleQCResultCreated", "Created " + violationsCreated
                        + " violation(s) for result: " + result.getId() + ", status → " + newStatus);
            } else {
                LogEvent.logInfo(this.getClass().getName(), "handleQCResultCreated",
                        "No violations detected for result: " + result.getId() + ", status → ACCEPTED");
            }

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "handleQCResultCreated",
                    "Error evaluating rules for result " + result.getId() + ": " + e.getMessage());
            // Don't rethrow - we don't want to fail the main transaction
        }
    }
}
