package org.openelisglobal.analyzer.service;

import java.util.Date;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of AnalyzerStatusTransitionService
 * 
 * 
 * Handles event-driven status transitions with: - Prerequisite validation -
 * Status update - Audit trail logging - Status change event publishing
 */
@Service
@Transactional
public class AnalyzerStatusTransitionServiceImpl implements AnalyzerStatusTransitionService {

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public Analyzer transitionToValidation(String analyzerId) {
        Analyzer analyzer = getAnalyzerOrThrow(analyzerId);
        AnalyzerStatus currentStatus = analyzer.getStatus();

        if (currentStatus != AnalyzerStatus.SETUP) {
            throw new IllegalStateException("Cannot transition to VALIDATION: analyzer " + analyzerId + " is in "
                    + currentStatus + " status (expected SETUP)");
        }

        return updateStatus(analyzer, AnalyzerStatus.VALIDATION, "First mapping created");
    }

    @Override
    public Analyzer transitionToActive(String analyzerId) {
        Analyzer analyzer = getAnalyzerOrThrow(analyzerId);
        AnalyzerStatus currentStatus = analyzer.getStatus();

        if (currentStatus != AnalyzerStatus.VALIDATION) {
            throw new IllegalStateException("Cannot transition to ACTIVE: analyzer " + analyzerId + " is in "
                    + currentStatus + " status (expected VALIDATION)");
        }

        analyzer.setLastActivatedDate(new Date());

        return updateStatus(analyzer, AnalyzerStatus.ACTIVE, "All required mappings activated");
    }

    @Override
    public Analyzer transitionToErrorPending(String analyzerId) {
        Analyzer analyzer = getAnalyzerOrThrow(analyzerId);
        AnalyzerStatus currentStatus = analyzer.getStatus();

        if (currentStatus != AnalyzerStatus.ACTIVE) {
            throw new IllegalStateException("Cannot transition to ERROR_PENDING: analyzer " + analyzerId + " is in "
                    + currentStatus + " status (expected ACTIVE)");
        }

        return updateStatus(analyzer, AnalyzerStatus.ERROR_PENDING, "Unacknowledged error created");
    }

    @Override
    public Analyzer transitionToOffline(String analyzerId) {
        Analyzer analyzer = getAnalyzerOrThrow(analyzerId);
        AnalyzerStatus currentStatus = analyzer.getStatus();

        if (currentStatus != AnalyzerStatus.ACTIVE && currentStatus != AnalyzerStatus.ERROR_PENDING) {
            throw new IllegalStateException("Cannot transition to OFFLINE: analyzer " + analyzerId + " is in "
                    + currentStatus + " status (expected ACTIVE or ERROR_PENDING)");
        }

        return updateStatus(analyzer, AnalyzerStatus.OFFLINE, "Connection test failed");
    }

    @Override
    public Analyzer transitionToActiveFromError(String analyzerId) {
        Analyzer analyzer = getAnalyzerOrThrow(analyzerId);
        AnalyzerStatus currentStatus = analyzer.getStatus();

        if (currentStatus != AnalyzerStatus.ERROR_PENDING) {
            throw new IllegalStateException("Cannot transition to ACTIVE from ERROR_PENDING: analyzer " + analyzerId
                    + " is in " + currentStatus + " status (expected ERROR_PENDING)");
        }

        return updateStatus(analyzer, AnalyzerStatus.ACTIVE, "All errors acknowledged");
    }

    @Override
    public Analyzer transitionToActiveFromOffline(String analyzerId) {
        Analyzer analyzer = getAnalyzerOrThrow(analyzerId);
        AnalyzerStatus currentStatus = analyzer.getStatus();

        if (currentStatus != AnalyzerStatus.OFFLINE) {
            throw new IllegalStateException("Cannot transition to ACTIVE from OFFLINE: analyzer " + analyzerId
                    + " is in " + currentStatus + " status (expected OFFLINE)");
        }

        return updateStatus(analyzer, AnalyzerStatus.ACTIVE, "Connection restored");
    }

    /**
     * Get analyzer or throw exception
     */
    private Analyzer getAnalyzerOrThrow(String analyzerId) {
        Analyzer analyzer = analyzerService.get(analyzerId);
        if (analyzer == null) {
            throw new IllegalArgumentException("Analyzer not found: " + analyzerId);
        }
        return analyzer;
    }

    /**
     * Update status, log audit trail, and publish event
     */
    private Analyzer updateStatus(Analyzer analyzer, AnalyzerStatus newStatus, String reason) {
        AnalyzerStatus oldStatus = analyzer.getStatus();
        String analyzerId = analyzer.getId() != null ? analyzer.getId() : "unknown";

        analyzer.setStatus(newStatus);
        analyzer.setSysUserId("SYSTEM"); // System-triggered transition
        analyzer.setLastupdatedFields();

        analyzerService.update(analyzer);

        LogEvent.logInfo(this.getClass().getSimpleName(), "updateStatus", "Analyzer " + analyzerId
                + " status transitioned from " + oldStatus + " to " + newStatus + ". Reason: " + reason);

        eventPublisher.publishEvent(new AnalyzerStatusChangeEvent(this, analyzerId, oldStatus, newStatus, reason));

        return analyzer;
    }

    /**
     * Event object for analyzer status changes
     */
    public static class AnalyzerStatusChangeEvent extends org.springframework.context.ApplicationEvent {

        private static final long serialVersionUID = 1L;

        private final String analyzerId;
        private final AnalyzerStatus oldStatus;
        private final AnalyzerStatus newStatus;
        private final String reason;

        public AnalyzerStatusChangeEvent(Object source, String analyzerId, AnalyzerStatus oldStatus,
                AnalyzerStatus newStatus, String reason) {
            super(source);
            this.analyzerId = analyzerId;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
            this.reason = reason;
        }

        public String getAnalyzerId() {
            return analyzerId;
        }

        public AnalyzerStatus getOldStatus() {
            return oldStatus;
        }

        public AnalyzerStatus getNewStatus() {
            return newStatus;
        }

        public String getReason() {
            return reason;
        }
    }
}
