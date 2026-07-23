package org.openelisglobal.qc.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qc.dao.QCRuleViolationDAO;
import org.openelisglobal.qc.form.QCViolationForm;
import org.openelisglobal.qc.service.evaluator.RuleEvaluationResult;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
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

    /** System user ID used for automated violation creation (no user session). */
    private static final int SYSTEM_AUTOMATION_USER_ID = 1;

    private static final String STATUS_UNRESOLVED = "UNRESOLVED";
    private static final String STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";
    private static final String STATUS_RESOLVED = "RESOLVED";

    @Autowired
    private QCRuleViolationDAO violationDAO;

    @Autowired
    private QCAlertService alertService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private TestService testService;

    @Autowired
    private SystemUserService systemUserService;

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

        // Set system user ID for audit trail (automated — no user session)
        violation.setSysUserId(String.valueOf(SYSTEM_AUTOMATION_USER_ID));
        violation.setSystemUserId(SYSTEM_AUTOMATION_USER_ID);

        // Persist the violation
        violationDAO.insert(violation);

        LogEvent.logInfo(this.getClass().getName(), "createViolation", "Created violation " + violation.getId()
                + " for rule " + evalResult.getRuleCode() + " with severity " + evalResult.getSeverity());

        // Trigger alert creation
        try {
            alertService.createAlertsForViolation(violation);
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
    public List<QCRuleViolation> findAll() {
        return violationDAO.getAllOrdered("violationDateTime", true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCRuleViolation> findByInstrument(String instrumentId) {
        return violationDAO.findByInstrument(instrumentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCRuleViolation> findUnresolved() {
        return violationDAO.findUnresolved();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCRuleViolation> findUnresolvedByInstrument(String instrumentId) {
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
            violation.setResolvedDateTime(Timestamp.from(Instant.now()));

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

    @Override
    @Transactional(readOnly = true)
    public QCViolationForm toForm(QCRuleViolation violation) {
        QCViolationForm form = new QCViolationForm();
        form.setId(violation.getId());
        form.setTriggeringResultId(violation.getTriggeringResultId());
        form.setRuleCode(violation.getRuleCode());
        form.setViolationDateTime(violation.getViolationDateTime());
        form.setSeverity(violation.getSeverity());
        form.setInstrumentId(violation.getInstrumentId());
        form.setTestId(violation.getTestId());
        form.setResolutionStatus(violation.getResolutionStatus());
        form.setResolvedDateTime(violation.getResolvedDateTime());
        form.setResolvedByUserId(violation.getResolvedByUserId());
        form.setResolutionNotes(violation.getResolutionNotes());

        form.setRuleDescription(getRuleDescription(violation.getRuleCode()));

        if (violation.getInstrumentId() != null) {
            try {
                Analyzer analyzer = analyzerService.get(String.valueOf(violation.getInstrumentId()));
                if (analyzer != null) {
                    form.setInstrumentName(analyzer.getName());
                }
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getName(), "toForm",
                        "Could not resolve analyzer name for ID " + violation.getInstrumentId());
            }
        }

        if (violation.getTestId() != null) {
            try {
                Test test = testService.get(String.valueOf(violation.getTestId()));
                if (test != null) {
                    form.setTestName(test.getName());
                }
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getName(), "toForm",
                        "Could not resolve test name for ID " + violation.getTestId());
            }
        }

        if (violation.getResolvedByUserId() != null) {
            try {
                SystemUser user = systemUserService.getUserById(String.valueOf(violation.getResolvedByUserId()));
                if (user != null) {
                    form.setResolvedByUserName(user.getDisplayName());
                }
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getName(), "toForm",
                        "Could not resolve user name for ID " + violation.getResolvedByUserId());
            }
        }

        if (STATUS_ACKNOWLEDGED.equals(violation.getResolutionStatus())) {
            form.setAcknowledgedDate(violation.getResolvedDateTime());
        }

        return form;
    }

    private String getRuleDescription(String ruleCode) {
        switch (ruleCode) {
        case "1₂ₛ":
            return "Single result exceeds \u00b12SD (Warning)";
        case "1₃ₛ":
            return "Single result exceeds \u00b13SD (Rejection)";
        case "2₂ₛ":
            return "Two consecutive results exceed same \u00b12SD limit";
        case "R₄ₛ":
            return "Range between consecutive results exceeds 4SD";
        case "4₁ₛ":
            return "Four consecutive results exceed same \u00b11SD limit";
        case "10ₓ":
            return "Ten consecutive results on same side of mean";
        case "3₁ₛ":
            return "Three consecutive results exceed same \u00b11SD limit (Warning)";
        case "7ₜ":
            return "Seven consecutive results showing consistent trend";
        default:
            return ruleCode;
        }
    }
}
