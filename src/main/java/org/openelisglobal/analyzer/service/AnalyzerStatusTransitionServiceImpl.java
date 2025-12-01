package org.openelisglobal.analyzer.service;

import java.util.Date;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration.AnalyzerStatus;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of AnalyzerStatusTransitionService
 * 
 * Task Reference: T151d
 * 
 * Handles event-driven status transitions with: - Prerequisite validation -
 * Status update - Audit trail logging - Status change event publishing
 */
@Service
@Transactional
public class AnalyzerStatusTransitionServiceImpl implements AnalyzerStatusTransitionService {

    @Autowired
    private AnalyzerConfigurationService configurationService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public AnalyzerConfiguration transitionToValidation(String analyzerId) {
        AnalyzerConfiguration config = getConfigurationOrThrow(analyzerId);
        AnalyzerStatus currentStatus = config.getStatus();

        // Validate: must be in SETUP status
        if (currentStatus != AnalyzerStatus.SETUP) {
            throw new IllegalStateException("Cannot transition to VALIDATION: analyzer " + analyzerId + " is in "
                    + currentStatus + " status (expected SETUP)");
        }

        return updateStatus(config, AnalyzerStatus.VALIDATION, "First mapping created");
    }

    @Override
    public AnalyzerConfiguration transitionToActive(String analyzerId) {
        AnalyzerConfiguration config = getConfigurationOrThrow(analyzerId);
        AnalyzerStatus currentStatus = config.getStatus();

        // Validate: must be in VALIDATION status
        if (currentStatus != AnalyzerStatus.VALIDATION) {
            throw new IllegalStateException("Cannot transition to ACTIVE: analyzer " + analyzerId + " is in "
                    + currentStatus + " status (expected VALIDATION)");
        }

        // Record activation date
        config.setLastActivatedDate(new Date());

        return updateStatus(config, AnalyzerStatus.ACTIVE, "All required mappings activated");
    }

    @Override
    public AnalyzerConfiguration transitionToErrorPending(String analyzerId) {
        AnalyzerConfiguration config = getConfigurationOrThrow(analyzerId);
        AnalyzerStatus currentStatus = config.getStatus();

        // Validate: must be in ACTIVE status
        if (currentStatus != AnalyzerStatus.ACTIVE) {
            throw new IllegalStateException("Cannot transition to ERROR_PENDING: analyzer " + analyzerId + " is in "
                    + currentStatus + " status (expected ACTIVE)");
        }

        return updateStatus(config, AnalyzerStatus.ERROR_PENDING, "Unacknowledged error created");
    }

    @Override
    public AnalyzerConfiguration transitionToOffline(String analyzerId) {
        AnalyzerConfiguration config = getConfigurationOrThrow(analyzerId);
        AnalyzerStatus currentStatus = config.getStatus();

        // Validate: must be in ACTIVE or ERROR_PENDING status
        if (currentStatus != AnalyzerStatus.ACTIVE && currentStatus != AnalyzerStatus.ERROR_PENDING) {
            throw new IllegalStateException("Cannot transition to OFFLINE: analyzer " + analyzerId + " is in "
                    + currentStatus + " status (expected ACTIVE or ERROR_PENDING)");
        }

        return updateStatus(config, AnalyzerStatus.OFFLINE, "Connection test failed");
    }

    @Override
    public AnalyzerConfiguration transitionToActiveFromError(String analyzerId) {
        AnalyzerConfiguration config = getConfigurationOrThrow(analyzerId);
        AnalyzerStatus currentStatus = config.getStatus();

        // Validate: must be in ERROR_PENDING status
        if (currentStatus != AnalyzerStatus.ERROR_PENDING) {
            throw new IllegalStateException("Cannot transition to ACTIVE from ERROR_PENDING: analyzer " + analyzerId
                    + " is in " + currentStatus + " status (expected ERROR_PENDING)");
        }

        return updateStatus(config, AnalyzerStatus.ACTIVE, "All errors acknowledged");
    }

    @Override
    public AnalyzerConfiguration transitionToActiveFromOffline(String analyzerId) {
        AnalyzerConfiguration config = getConfigurationOrThrow(analyzerId);
        AnalyzerStatus currentStatus = config.getStatus();

        // Validate: must be in OFFLINE status
        if (currentStatus != AnalyzerStatus.OFFLINE) {
            throw new IllegalStateException("Cannot transition to ACTIVE from OFFLINE: analyzer " + analyzerId
                    + " is in " + currentStatus + " status (expected OFFLINE)");
        }

        return updateStatus(config, AnalyzerStatus.ACTIVE, "Connection restored");
    }

    /**
     * Get analyzer configuration or throw exception
     */
    private AnalyzerConfiguration getConfigurationOrThrow(String analyzerId) {
        Optional<AnalyzerConfiguration> configOpt = configurationService.getByAnalyzerId(analyzerId);
        if (configOpt.isEmpty()) {
            throw new IllegalArgumentException("AnalyzerConfiguration not found for analyzer: " + analyzerId);
        }
        return configOpt.get();
    }

    /**
     * Update status, log audit trail, and publish event
     */
    private AnalyzerConfiguration updateStatus(AnalyzerConfiguration config, AnalyzerStatus newStatus, String reason) {
        AnalyzerStatus oldStatus = config.getStatus();
        String analyzerId = config.getAnalyzer() != null ? config.getAnalyzer().getId() : "unknown";

        // Update status
        config.setStatus(newStatus);
        config.setSysUserId("SYSTEM"); // System-triggered transition
        config.setLastupdatedFields();

        configurationService.update(config);

        // Log audit trail
        LogEvent.logInfo(this.getClass().getSimpleName(), "updateStatus", "Analyzer " + analyzerId
                + " status transitioned from " + oldStatus + " to " + newStatus + ". Reason: " + reason);

        // Publish status change event for listeners
        eventPublisher.publishEvent(new AnalyzerStatusChangeEvent(this, analyzerId, oldStatus, newStatus, reason));

        return config;
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
