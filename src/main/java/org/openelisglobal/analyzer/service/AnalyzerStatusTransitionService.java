package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.Analyzer;

/**
 * Service for event-driven analyzer status transitions
 *
 *
 * Handles automatic status transitions triggered by system events: - SETUP →
 * VALIDATION: First mapping created - VALIDATION → ACTIVE: All required
 * mappings activated - ACTIVE → ERROR_PENDING: Unacknowledged error created -
 * ACTIVE → OFFLINE: Connection test failed - ERROR_PENDING → ACTIVE: All errors
 * acknowledged - OFFLINE → ACTIVE: Connection test succeeded
 *
 * Each method validates prerequisites, updates status, logs audit trail, and
 * publishes status change event.
 */
public interface AnalyzerStatusTransitionService {

    /**
     * Transition analyzer to VALIDATION status Triggered when first field mapping
     * is created
     *
     * @param analyzerId The analyzer ID
     * @return Updated Analyzer
     * @throws IllegalStateException if analyzer is not in SETUP status
     */
    Analyzer transitionToValidation(String analyzerId);

    /**
     * Transition analyzer to ACTIVE status Triggered when all required field
     * mappings are activated
     *
     * @param analyzerId The analyzer ID
     * @return Updated Analyzer
     * @throws IllegalStateException if analyzer is not in VALIDATION status
     */
    Analyzer transitionToActive(String analyzerId);

    /**
     * Transition analyzer to ERROR_PENDING status Triggered when an unacknowledged
     * error is created
     *
     * @param analyzerId The analyzer ID
     * @return Updated Analyzer
     * @throws IllegalStateException if analyzer is not in ACTIVE status
     */
    Analyzer transitionToErrorPending(String analyzerId);

    /**
     * Transition analyzer to OFFLINE status Triggered when connection test fails
     *
     * @param analyzerId The analyzer ID
     * @return Updated Analyzer
     * @throws IllegalStateException if analyzer is not in ACTIVE or ERROR_PENDING
     *                               status
     */
    Analyzer transitionToOffline(String analyzerId);

    /**
     * Transition analyzer from ERROR_PENDING to ACTIVE status Triggered when all
     * errors are acknowledged
     *
     * @param analyzerId The analyzer ID
     * @return Updated Analyzer
     * @throws IllegalStateException if analyzer is not in ERROR_PENDING status
     */
    Analyzer transitionToActiveFromError(String analyzerId);

    /**
     * Transition analyzer from OFFLINE to ACTIVE status Triggered when connection
     * test succeeds after being offline
     *
     * @param analyzerId The analyzer ID
     * @return Updated Analyzer
     * @throws IllegalStateException if analyzer is not in OFFLINE status
     */
    Analyzer transitionToActiveFromOffline(String analyzerId);
}
