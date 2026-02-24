package org.openelisglobal.qaevent.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.qaevent.form.DeltaCheckAlertResponseForm;
import org.openelisglobal.qaevent.form.DismissAlertRequestForm;
import org.openelisglobal.qaevent.form.EscalateAlertRequestForm;
import org.openelisglobal.qaevent.form.InlineNCERequestForm;
import org.openelisglobal.qaevent.form.NCEBadgeResponseForm;
import org.openelisglobal.qaevent.form.NCEResponseForm;
import org.openelisglobal.qaevent.form.PromptDismissalRequestForm;
import org.openelisglobal.qaevent.service.DeltaCheckAlertService;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.service.NcePromptDismissalService;
import org.openelisglobal.qaevent.service.NceResultAssociationService;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceResultAssociation.AssociationType;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for NCE Results Entry Integration. Provides API endpoints for
 * inline NCE reporting, delta check alerts, and quality badges.
 */
@RestController
@RequestMapping("/rest")
public class NCEIntegrationRestController extends BaseRestController {

    private static final int DEFAULT_ALERT_DAYS = 90;
    private static final int MAX_ANALYSIS_IDS = 100;

    @Autowired
    private ResultService resultService;

    @Autowired
    private NCEventService ncEventService;

    @Autowired
    private NceResultAssociationService nceResultAssociationService;

    @Autowired
    private DeltaCheckAlertService deltaCheckAlertService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private NcePromptDismissalService promptDismissalService;

    /**
     * Create NCE from result inline form POST /rest/results/{resultId}/nce
     */
    @PostMapping(value = "/results/{resultId}/nce", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createNCEFromResult(@PathVariable String resultId,
            @Valid @RequestBody InlineNCERequestForm request, HttpServletRequest httpRequest) {
        try {
            // Validate result exists
            Result result = resultService.get(resultId);
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        createErrorResponse("Result not found", "Result with ID " + resultId + " does not exist"));
            }

            // Get current user
            SystemUser currentUser = systemUserService.getUserById(getSysUserId(httpRequest));
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Authentication required", "User must be authenticated"));
            }

            // Auto-populate context information (service handles lazy traversal within
            // transaction)
            Map<String, Object> contextInfo = nceResultAssociationService.buildResultContextInfo(resultId);

            // Create NCE (service handles entity population, NCE number generation)
            NcEvent ncEvent = ncEventService.createNCEFromInlineRequest(request, resultId, currentUser, contextInfo);

            // Create association between result and NCE
            nceResultAssociationService.createAssociation(resultId, ncEvent, AssociationType.RESULT_TRIGGERED_NCE,
                    getSysUserId(httpRequest));

            // Build response
            NCEResponseForm response = ncEventService.buildNCEResponse(ncEvent, List.of(resultId));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Invalid request", e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", "An unexpected error occurred"));
        }
    }

    /**
     * Get NCE badge information for result GET /rest/results/{resultId}/nce-badge
     */
    @GetMapping(value = "/results/{resultId}/nce-badge", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getResultNCEBadge(@PathVariable String resultId) {
        try {
            Result result = resultService.get(resultId);
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        createErrorResponse("Result not found", "Result with ID " + resultId + " does not exist"));
            }

            NCEBadgeResponseForm badgeResponse = nceResultAssociationService.buildBadgeResponseForResult(resultId);
            return ResponseEntity.ok(badgeResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
                    "Internal server error", "An error occurred while retrieving badge information"));
        }
    }

    /**
     * Get active delta check alerts GET /rest/delta-check/alerts
     */
    @GetMapping(value = "/delta-check/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getDeltaCheckAlerts(@RequestParam(required = false) List<String> analysisIds,
            @RequestParam(required = false) String status) {
        try {
            // Validate analysisIds size to prevent excessive queries
            if (analysisIds != null && analysisIds.size() > MAX_ANALYSIS_IDS) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        createErrorResponse("Too many analysis IDs", "Maximum " + MAX_ANALYSIS_IDS + " IDs allowed"));
            }

            DeltaCheckAlert.AlertStatus alertStatus = status != null ? DeltaCheckAlert.AlertStatus.valueOf(status)
                    : null;

            // Read comparison period from global configuration (defaults to
            // DEFAULT_ALERT_DAYS)
            int comparisonPeriod = DEFAULT_ALERT_DAYS;
            try {
                String configValue = ConfigurationProperties.getInstance()
                        .getPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_COMPARISON_PERIOD);
                if (configValue != null && !configValue.isBlank()) {
                    comparisonPeriod = Integer.parseInt(configValue);
                }
            } catch (NumberFormatException ignored) {
                // fall back to DEFAULT_ALERT_DAYS
            }

            // Delegate filtering to service layer (safe lazy traversal within transaction)
            List<DeltaCheckAlert> alerts = deltaCheckAlertService.getFilteredAlerts(alertStatus, analysisIds,
                    comparisonPeriod);

            List<DeltaCheckAlertResponseForm> response = alerts.stream().map(DeltaCheckAlertResponseForm::fromEntity)
                    .toList();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Invalid status", "Invalid alert status: " + status));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", "An error occurred while retrieving alerts"));
        }
    }

    /**
     * Dismiss delta check alert PUT /rest/delta-check/alerts/{alertId}/dismiss
     */
    @PutMapping(value = "/delta-check/alerts/{alertId}/dismiss", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> dismissDeltaCheckAlert(@PathVariable Integer alertId,
            @Valid @RequestBody DismissAlertRequestForm dismissRequest, HttpServletRequest httpRequest) {
        try {
            // Validate alert exists
            DeltaCheckAlert alert = deltaCheckAlertService.get(alertId);
            if (alert == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Alert not found", "Alert with ID " + alertId + " does not exist"));
            }

            if (!alert.isActive()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        createErrorResponse("Alert already resolved", "Alert has already been dismissed or escalated"));
            }

            // Get current user — require authentication
            SystemUser currentUser = systemUserService.getUserById(getSysUserId(httpRequest));
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Authentication required", "User must be authenticated"));
            }

            // Dismiss alert
            DeltaCheckAlert dismissedAlert = deltaCheckAlertService.dismissAlert(alertId, dismissRequest.getReason(),
                    currentUser.getLoginName());

            return ResponseEntity.ok(DeltaCheckAlertResponseForm.fromEntity(dismissedAlert));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", "An error occurred while dismissing alert"));
        }
    }

    /**
     * Escalate delta check alert to NCE POST
     * /rest/delta-check/alerts/{alertId}/escalate-nce
     */
    @PostMapping(value = "/delta-check/alerts/{alertId}/escalate-nce", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> escalateAlertToNCE(@PathVariable Integer alertId,
            @Valid @RequestBody EscalateAlertRequestForm escalateRequest, HttpServletRequest httpRequest) {
        try {
            DeltaCheckAlert alert = deltaCheckAlertService.get(alertId);
            if (alert == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Alert not found", "Alert with ID " + alertId + " does not exist"));
            }

            if (!alert.isActive()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        createErrorResponse("Alert already resolved", "Alert has already been dismissed or escalated"));
            }

            SystemUser currentUser = systemUserService.getUserById(getSysUserId(httpRequest));
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Authentication required", "User must be authenticated"));
            }

            // Build inline request from escalation form to reuse NCE creation logic
            InlineNCERequestForm nceRequest = new InlineNCERequestForm();
            nceRequest.setTitle(escalateRequest.getNceTitle());
            nceRequest.setDescription(escalateRequest.getNceDescription());
            nceRequest.setCategory(escalateRequest.getCategory());
            nceRequest.setSubcategory(escalateRequest.getSubcategory());
            nceRequest.setSeverity(escalateRequest.getSeverity());
            nceRequest.setImmediateAction(escalateRequest.getImmediateAction());
            nceRequest.setSampleAction(escalateRequest.getSampleAction() != null ? escalateRequest.getSampleAction()
                    : "Continue with flag");

            // Get context from the alert's result. Treat "0" and blank as absent to
            // guard against alerts that were created before a result was fully persisted.
            String rawResultId = alert.getResultId();
            String resultId = (rawResultId != null && !rawResultId.isBlank() && !"0".equals(rawResultId)) ? rawResultId
                    : null;
            Map<String, Object> contextInfo = resultId != null
                    ? nceResultAssociationService.buildResultContextInfo(resultId)
                    : new HashMap<>();

            // Create the NCE
            NcEvent ncEvent = ncEventService.createNCEFromInlineRequest(nceRequest, resultId, currentUser, contextInfo);

            // Escalate the alert (sets status to ESCALATED_NCE)
            deltaCheckAlertService.escalateAlertToNCE(alertId, ncEvent);

            // Create result-NCE association if result exists
            if (resultId != null) {
                nceResultAssociationService.createAssociation(resultId, ncEvent, AssociationType.DELTA_CHECK_ESCALATION,
                        getSysUserId(httpRequest));
            }

            NCEResponseForm response = ncEventService.buildNCEResponse(ncEvent,
                    resultId != null ? List.of(resultId) : List.of());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Invalid request", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", "An error occurred while escalating alert"));
        }
    }

    /**
     * Get delta check global configuration GET /rest/delta-check/configuration
     * Returns the four global NCE delta check settings stored in site_information.
     */
    @GetMapping(value = "/delta-check/configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getDeltaCheckConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_ENABLED));
        config.put("threshold", ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_THRESHOLD));
        config.put("absoluteThreshold", ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_ABSOLUTE_THRESHOLD));
        config.put("comparisonPeriod", ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_COMPARISON_PERIOD));
        return ResponseEntity.ok(config);
    }

    /**
     * Get NCE summary by lab order number (US4 Scenario 3 — FHIR screen badge) GET
     * /rest/nce-summary?labOrderNumber=...
     */
    @GetMapping(value = "/nce-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getNCESummaryByLabOrder(@RequestParam String labOrderNumber) {
        try {
            List<NcEvent> events = ncEventService.findByNCENumberOrLabOrderId(null, labOrderNumber);
            List<String> nceNumbers = events.stream().map(NcEvent::getNceNumber).toList();

            Map<String, Object> summary = new HashMap<>();
            summary.put("labOrderNumber", labOrderNumber);
            summary.put("nceCount", events.size());
            summary.put("nceNumbers", nceNumbers);
            summary.put("hasNCE", !events.isEmpty());
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    createErrorResponse("Internal server error", "An error occurred while retrieving NCE summary"));
        }
    }

    /**
     * Record a prompt dismissal for audit trail POST /rest/nce/prompt-dismissal
     */
    @PostMapping(value = "/nce/prompt-dismissal", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> recordPromptDismissal(@Valid @RequestBody PromptDismissalRequestForm request,
            HttpServletRequest httpRequest) {
        try {
            SystemUser currentUser = systemUserService.getUserById(getSysUserId(httpRequest));
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Authentication required", "User must be authenticated"));
            }

            var dismissal = promptDismissalService.recordDismissal(request, currentUser.getLoginName());

            Map<String, Object> response = new HashMap<>();
            response.put("id", dismissal.getId());
            response.put("timestamp", dismissal.getDismissedDate());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Invalid request", e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", "An error occurred while recording dismissal"));
        }
    }

    /**
     * Get delta check status for a specific result GET
     * /rest/results/{resultId}/delta-check
     */
    @GetMapping(value = "/results/{resultId}/delta-check", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getResultDeltaCheck(@PathVariable String resultId) {
        try {
            Result result = resultService.get(resultId);
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        createErrorResponse("Result not found", "Result with ID " + resultId + " does not exist"));
            }

            List<DeltaCheckAlert> alerts = deltaCheckAlertService.getActiveAlertsForResult(resultId);
            if (alerts.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("triggered", false);
                return ResponseEntity.ok(response);
            }

            DeltaCheckAlert latest = alerts.get(0);
            Map<String, Object> response = new HashMap<>();
            response.put("triggered", true);
            response.put("alertId", latest.getId());
            response.put("currentValue", latest.getCurrentValue());
            response.put("previousValue", latest.getPreviousValue());
            response.put("changePercent", latest.getChangePercent());
            response.put("threshold", latest.getThresholdPercent());
            response.put("status", latest.getStatus());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    createErrorResponse("Internal server error", "An error occurred while checking delta status"));
        }
    }

    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", DateUtil.formatDateAsText(new java.util.Date()));
        return errorResponse;
    }
}
