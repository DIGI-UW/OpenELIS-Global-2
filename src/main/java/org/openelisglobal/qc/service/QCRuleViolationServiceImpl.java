package org.openelisglobal.qc.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qc.dao.QCRuleViolationDAO;
import org.openelisglobal.qc.service.evaluator.RuleEvaluationResult;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for QC Rule Violation management (T107).
 *
 * Following Constitution IV.5: @Transactional in services ONLY (NOT
 * controllers)
 */
@Service
public class QCRuleViolationServiceImpl implements QCRuleViolationService {

    private static final String STATUS_UNRESOLVED = "UNRESOLVED";
    private static final String STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";
    private static final String STATUS_RESOLVED = "RESOLVED";

    @Autowired
    private QCRuleViolationDAO violationDAO;

    @Autowired
    private QCAlertService alertService;

    @Override
    @Transactional
    public QCRuleViolation createViolation(RuleEvaluationResult evalResult, QCResult qcResult) {
        if (evalResult == null || qcResult == null) {
            LogEvent.logWarn(this.getClass().getName(), "createViolation",
                    "Cannot create violation: null evalResult or qcResult");
            return null;
        }

        if (!evalResult.isViolated()) {
            LogEvent.logWarn(this.getClass().getName(), "createViolation",
                    "Cannot create violation: evalResult is not a violation");
            return null;
        }

        // Create the violation record
        QCRuleViolation violation = new QCRuleViolation();
        violation.setId(UUID.randomUUID().toString());
        violation.setTriggeringResultId(qcResult.getId());
        violation.setRuleCode(evalResult.getRuleCode());
        violation.setSeverity(evalResult.getSeverity());
        violation.setViolationDateTime(Timestamp.from(Instant.now()));
        violation.setInstrumentId(qcResult.getInstrumentId());
        violation.setTestId(qcResult.getTestId());
        violation.setResolutionStatus(STATUS_UNRESOLVED);

        // Store the evaluation message
        if (evalResult.getMessage() != null) {
            violation.setResolutionNotes("Detection: " + evalResult.getMessage());
        }

        // Persist the violation
        violationDAO.insert(violation);

        LogEvent.logInfo(this.getClass().getName(), "createViolation", "Created violation " + violation.getId()
                + " for rule " + evalResult.getRuleCode() + " with severity " + evalResult.getSeverity());

        // Trigger alert creation
        try {
            alertService.createAlertForViolation(violation);
        } catch (Exception e) {
            // Log but don't fail - violation is still created
            LogEvent.logError(this.getClass().getName(), "createViolation",
                    "Error creating alert for violation " + violation.getId() + ": " + e.getMessage());
        }

        return violation;
    }

    @Override
    @Transactional(readOnly = true)
    public QCRuleViolation getById(String id) {
        return violationDAO.get(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCRuleViolation> findByInstrument(Integer instrumentId) {
        return violationDAO.findByInstrument(instrumentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCRuleViolation> findUnresolved() {
        return violationDAO.findUnresolved();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCRuleViolation> findUnresolvedByInstrument(Integer instrumentId) {
        return violationDAO.findUnresolvedByInstrument(instrumentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCRuleViolation> findBySeverity(String severity) {
        return violationDAO.findBySeverity(severity);
    }

    @Override
    @Transactional
    public QCRuleViolation resolveViolation(String violationId, Integer userId, String notes) {
        QCRuleViolation violation = violationDAO.get(violationId).orElse(null);
        if (violation == null) {
            LogEvent.logWarn(this.getClass().getName(), "resolveViolation", "Violation not found: " + violationId);
            return null;
        }

        violation.setResolutionStatus(STATUS_RESOLVED);
        violation.setResolvedDateTime(Timestamp.from(Instant.now()));
        violation.setResolvedByUserId(userId);

        // Append resolution notes
        String existingNotes = violation.getResolutionNotes();
        if (existingNotes != null && !existingNotes.isEmpty()) {
            violation.setResolutionNotes(existingNotes + "\nResolution: " + notes);
        } else {
            violation.setResolutionNotes("Resolution: " + notes);
        }

        violationDAO.update(violation);

        LogEvent.logInfo(this.getClass().getName(), "resolveViolation",
                "Resolved violation " + violationId + " by user " + userId);

        return violation;
    }

    @Override
    @Transactional
    public QCRuleViolation acknowledgeViolation(String violationId, Integer userId) {
        QCRuleViolation violation = violationDAO.get(violationId).orElse(null);
        if (violation == null) {
            LogEvent.logWarn(this.getClass().getName(), "acknowledgeViolation", "Violation not found: " + violationId);
            return null;
        }

        // Only acknowledge if currently unresolved
        if (STATUS_UNRESOLVED.equals(violation.getResolutionStatus())) {
            violation.setResolutionStatus(STATUS_ACKNOWLEDGED);
            violation.setResolvedByUserId(userId);

            String existingNotes = violation.getResolutionNotes();
            String ackNote = "Acknowledged by user " + userId + " at " + Instant.now();
            if (existingNotes != null && !existingNotes.isEmpty()) {
                violation.setResolutionNotes(existingNotes + "\n" + ackNote);
            } else {
                violation.setResolutionNotes(ackNote);
            }

            violationDAO.update(violation);

            LogEvent.logInfo(this.getClass().getName(), "acknowledgeViolation",
                    "Acknowledged violation " + violationId + " by user " + userId);
        }

        return violation;
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnresolvedCountBySeverity(String severity) {
        List<QCRuleViolation> violations = violationDAO.findBySeverity(severity);
        return (int) violations.stream().filter(v -> STATUS_UNRESOLVED.equals(v.getResolutionStatus())).count();
    }
}
