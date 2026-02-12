package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listeners for automatic analyzer status transitions
 * 
 * 
 * Listens for domain events and triggers appropriate status transitions: -
 * MappingCreatedEvent → SETUP → VALIDATION - AllMappingsActivatedEvent →
 * VALIDATION → ACTIVE - UnacknowledgedErrorCreatedEvent → ACTIVE →
 * ERROR_PENDING - ConnectionTestFailedEvent → ACTIVE/ERROR_PENDING → OFFLINE -
 * AllErrorsAcknowledgedEvent → ERROR_PENDING → ACTIVE -
 * ConnectionTestSucceededEvent → OFFLINE → ACTIVE
 */
@Component
public class AnalyzerStatusEventListeners {

    @Autowired
    private AnalyzerStatusTransitionService transitionService;

    @Autowired
    private AnalyzerService analyzerService;

    /**
     * Listener: First field mapping created for an analyzer Triggers: SETUP →
     * VALIDATION transition
     */
    @EventListener
    public void onMappingCreated(MappingCreatedEvent event) {
        String analyzerId = event.getAnalyzerId();
        LogEvent.logInfo(this.getClass().getSimpleName(), "onMappingCreated",
                "Received MappingCreatedEvent for analyzer " + analyzerId);

        try {
            // Only transition if analyzer is in SETUP status
            Analyzer analyzer = analyzerService.get(analyzerId);
            if (analyzer != null && analyzer.getStatus() == AnalyzerStatus.SETUP) {
                transitionService.transitionToValidation(analyzerId);
                LogEvent.logInfo(this.getClass().getSimpleName(), "onMappingCreated",
                        "Triggered SETUP → VALIDATION transition for analyzer " + analyzerId);
            }
        } catch (Exception e) {
            LogEvent.logError("Failed to transition analyzer " + analyzerId + " to VALIDATION on mapping created: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Listener: All required mappings have been activated Triggers: VALIDATION →
     * ACTIVE transition
     */
    @EventListener
    public void onAllMappingsActivated(AllMappingsActivatedEvent event) {
        String analyzerId = event.getAnalyzerId();
        LogEvent.logInfo(this.getClass().getSimpleName(), "onAllMappingsActivated",
                "Received AllMappingsActivatedEvent for analyzer " + analyzerId);

        try {
            // Only transition if analyzer is in VALIDATION status
            Analyzer analyzer = analyzerService.get(analyzerId);
            if (analyzer != null && analyzer.getStatus() == AnalyzerStatus.VALIDATION) {
                transitionService.transitionToActive(analyzerId);
                LogEvent.logInfo(this.getClass().getSimpleName(), "onAllMappingsActivated",
                        "Triggered VALIDATION → ACTIVE transition for analyzer " + analyzerId);
            }
        } catch (Exception e) {
            LogEvent.logError("Failed to transition analyzer " + analyzerId + " to ACTIVE on all mappings activated: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Listener: Unacknowledged error was created for an analyzer Triggers: ACTIVE →
     * ERROR_PENDING transition
     */
    @EventListener
    public void onUnacknowledgedErrorCreated(UnacknowledgedErrorCreatedEvent event) {
        String analyzerId = event.getAnalyzerId();
        LogEvent.logInfo(this.getClass().getSimpleName(), "onUnacknowledgedErrorCreated",
                "Received UnacknowledgedErrorCreatedEvent for analyzer " + analyzerId);

        try {
            // Only transition if analyzer is in ACTIVE status
            Analyzer analyzer = analyzerService.get(analyzerId);
            if (analyzer != null && analyzer.getStatus() == AnalyzerStatus.ACTIVE) {
                transitionService.transitionToErrorPending(analyzerId);
                LogEvent.logInfo(this.getClass().getSimpleName(), "onUnacknowledgedErrorCreated",
                        "Triggered ACTIVE → ERROR_PENDING transition for analyzer " + analyzerId);
            }
        } catch (Exception e) {
            LogEvent.logError("Failed to transition analyzer " + analyzerId + " to ERROR_PENDING on error created: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Listener: Connection test failed for an analyzer Triggers:
     * ACTIVE/ERROR_PENDING → OFFLINE transition
     */
    @EventListener
    public void onConnectionTestFailed(ConnectionTestFailedEvent event) {
        String analyzerId = event.getAnalyzerId();
        LogEvent.logInfo(this.getClass().getSimpleName(), "onConnectionTestFailed",
                "Received ConnectionTestFailedEvent for analyzer " + analyzerId);

        try {
            // Only transition if analyzer is in ACTIVE or ERROR_PENDING status
            Analyzer analyzer = analyzerService.get(analyzerId);
            if (analyzer != null) {
                AnalyzerStatus status = analyzer.getStatus();
                if (status == AnalyzerStatus.ACTIVE || status == AnalyzerStatus.ERROR_PENDING) {
                    transitionService.transitionToOffline(analyzerId);
                    LogEvent.logInfo(this.getClass().getSimpleName(), "onConnectionTestFailed",
                            "Triggered " + status + " → OFFLINE transition for analyzer " + analyzerId);
                }
            }
        } catch (Exception e) {
            LogEvent.logError("Failed to transition analyzer " + analyzerId + " to OFFLINE on connection test failed: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Listener: All errors have been acknowledged for an analyzer Triggers:
     * ERROR_PENDING → ACTIVE transition
     */
    @EventListener
    public void onAllErrorsAcknowledged(AllErrorsAcknowledgedEvent event) {
        String analyzerId = event.getAnalyzerId();
        LogEvent.logInfo(this.getClass().getSimpleName(), "onAllErrorsAcknowledged",
                "Received AllErrorsAcknowledgedEvent for analyzer " + analyzerId);

        try {
            // Only transition if analyzer is in ERROR_PENDING status
            Analyzer analyzer = analyzerService.get(analyzerId);
            if (analyzer != null && analyzer.getStatus() == AnalyzerStatus.ERROR_PENDING) {
                transitionService.transitionToActiveFromError(analyzerId);
                LogEvent.logInfo(this.getClass().getSimpleName(), "onAllErrorsAcknowledged",
                        "Triggered ERROR_PENDING → ACTIVE transition for analyzer " + analyzerId);
            }
        } catch (Exception e) {
            LogEvent.logError("Failed to transition analyzer " + analyzerId + " to ACTIVE on errors acknowledged: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Listener: Connection test succeeded for an offline analyzer Triggers: OFFLINE
     * → ACTIVE transition
     */
    @EventListener
    public void onConnectionTestSucceeded(ConnectionTestSucceededEvent event) {
        String analyzerId = event.getAnalyzerId();
        LogEvent.logInfo(this.getClass().getSimpleName(), "onConnectionTestSucceeded",
                "Received ConnectionTestSucceededEvent for analyzer " + analyzerId);

        try {
            // Only transition if analyzer is in OFFLINE status
            Analyzer analyzer = analyzerService.get(analyzerId);
            if (analyzer != null && analyzer.getStatus() == AnalyzerStatus.OFFLINE) {
                transitionService.transitionToActiveFromOffline(analyzerId);
                LogEvent.logInfo(this.getClass().getSimpleName(), "onConnectionTestSucceeded",
                        "Triggered OFFLINE → ACTIVE transition for analyzer " + analyzerId);
            }
        } catch (Exception e) {
            LogEvent.logError("Failed to transition analyzer " + analyzerId
                    + " to ACTIVE on connection test succeeded: " + e.getMessage(), e);
        }
    }

    // === Event Classes ===

    /**
     * Event: First field mapping created for an analyzer
     */
    public static class MappingCreatedEvent extends org.springframework.context.ApplicationEvent {
        private static final long serialVersionUID = 1L;
        private final String analyzerId;

        public MappingCreatedEvent(Object source, String analyzerId) {
            super(source);
            this.analyzerId = analyzerId;
        }

        public String getAnalyzerId() {
            return analyzerId;
        }
    }

    /**
     * Event: All required mappings have been activated
     */
    public static class AllMappingsActivatedEvent extends org.springframework.context.ApplicationEvent {
        private static final long serialVersionUID = 1L;
        private final String analyzerId;

        public AllMappingsActivatedEvent(Object source, String analyzerId) {
            super(source);
            this.analyzerId = analyzerId;
        }

        public String getAnalyzerId() {
            return analyzerId;
        }
    }

    /**
     * Event: Unacknowledged error was created
     */
    public static class UnacknowledgedErrorCreatedEvent extends org.springframework.context.ApplicationEvent {
        private static final long serialVersionUID = 1L;
        private final String analyzerId;
        private final String errorId;

        public UnacknowledgedErrorCreatedEvent(Object source, String analyzerId, String errorId) {
            super(source);
            this.analyzerId = analyzerId;
            this.errorId = errorId;
        }

        public String getAnalyzerId() {
            return analyzerId;
        }

        public String getErrorId() {
            return errorId;
        }
    }

    /**
     * Event: Connection test failed
     */
    public static class ConnectionTestFailedEvent extends org.springframework.context.ApplicationEvent {
        private static final long serialVersionUID = 1L;
        private final String analyzerId;
        private final String reason;

        public ConnectionTestFailedEvent(Object source, String analyzerId, String reason) {
            super(source);
            this.analyzerId = analyzerId;
            this.reason = reason;
        }

        public String getAnalyzerId() {
            return analyzerId;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * Event: All errors have been acknowledged
     */
    public static class AllErrorsAcknowledgedEvent extends org.springframework.context.ApplicationEvent {
        private static final long serialVersionUID = 1L;
        private final String analyzerId;

        public AllErrorsAcknowledgedEvent(Object source, String analyzerId) {
            super(source);
            this.analyzerId = analyzerId;
        }

        public String getAnalyzerId() {
            return analyzerId;
        }
    }

    /**
     * Event: Connection test succeeded (for offline analyzer)
     */
    public static class ConnectionTestSucceededEvent extends org.springframework.context.ApplicationEvent {
        private static final long serialVersionUID = 1L;
        private final String analyzerId;

        public ConnectionTestSucceededEvent(Object source, String analyzerId) {
            super(source);
            this.analyzerId = analyzerId;
        }

        public String getAnalyzerId() {
            return analyzerId;
        }
    }
}
