package org.openelisglobal.qc.service;

import java.util.List;
import org.openelisglobal.qc.service.evaluator.RuleEvaluationResult;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;

/**
 * Service interface for QC Rule Violation management (T107).
 *
 * Handles creation, retrieval, and resolution of QC rule violations. Triggers
 * alerts via QCAlertService after violation creation.
 */
public interface QCRuleViolationService {

    /**
     * Create a violation from a rule evaluation result. Also triggers alert
     * creation via QCAlertService.
     *
     * @param evalResult The rule evaluation result
     * @param qcResult   The QC result that triggered the violation
     * @return The created violation
     */
    QCRuleViolation createViolation(RuleEvaluationResult evalResult, QCResult qcResult);

    /**
     * Get a violation by ID.
     *
     * @param id The violation ID
     * @return The violation, or null if not found
     */
    QCRuleViolation getById(String id);

    /**
     * Get all violations for a specific instrument.
     *
     * @param instrumentId The instrument ID
     * @return List of violations
     */
    List<QCRuleViolation> findByInstrument(Integer instrumentId);

    /**
     * Get all unresolved violations.
     *
     * @return List of unresolved violations
     */
    List<QCRuleViolation> findUnresolved();

    /**
     * Get unresolved violations for a specific instrument.
     *
     * @param instrumentId The instrument ID
     * @return List of unresolved violations
     */
    List<QCRuleViolation> findUnresolvedByInstrument(Integer instrumentId);

    /**
     * Get violations by severity.
     *
     * @param severity The severity (WARNING or REJECTION)
     * @return List of violations with the specified severity
     */
    List<QCRuleViolation> findBySeverity(String severity);

    /**
     * Resolve a violation.
     *
     * @param violationId The violation ID
     * @param userId      The user resolving the violation
     * @param notes       Resolution notes
     * @return The resolved violation
     */
    QCRuleViolation resolveViolation(String violationId, Integer userId, String notes);

    /**
     * Acknowledge a warning violation (does not fully resolve, but marks as seen).
     *
     * @param violationId The violation ID
     * @param userId      The user acknowledging
     * @return The acknowledged violation
     */
    QCRuleViolation acknowledgeViolation(String violationId, Integer userId);

    /**
     * Get count of unresolved violations by severity.
     *
     * @param severity The severity
     * @return Count of unresolved violations
     */
    int getUnresolvedCountBySeverity(String severity);
}
