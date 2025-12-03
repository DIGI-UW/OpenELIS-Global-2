package org.openelisglobal.qc.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.qc.form.QCCorrectiveActionForm;
import org.openelisglobal.qc.service.QCCorrectiveActionService;
import org.openelisglobal.qc.valueholder.QCCorrectiveAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for QC Corrective Action management. Supports User Story 4:
 * Manage Corrective Actions.
 *
 * Following Constitution IV.5: @Transactional in services ONLY (NOT
 * controllers)
 */
@RestController
@RequestMapping("/rest/qc/corrective-actions")
public class QCCorrectiveActionRestController {

    @Autowired
    private QCCorrectiveActionService correctiveActionService;

    /**
     * Get all corrective actions with optional filtering. GET
     * /rest/qc/corrective-actions
     */
    @GetMapping
    public ResponseEntity<List<QCCorrectiveAction>> getCorrectiveActions(
            @RequestParam(required = false) String violationId, @RequestParam(required = false) Integer assignedUserId,
            @RequestParam(required = false) String status) {
        try {
            List<QCCorrectiveAction> actions;

            if (violationId != null && !violationId.isEmpty()) {
                actions = correctiveActionService.findByViolation(violationId);
            } else if (assignedUserId != null) {
                actions = correctiveActionService.findByAssignedUser(assignedUserId);
            } else if (status != null && !status.isEmpty()) {
                actions = correctiveActionService.findByStatus(status);
            } else {
                // Return all pending/assigned/in_progress by default
                actions = correctiveActionService.findByStatus(QCCorrectiveActionService.STATUS_PENDING);
                actions.addAll(correctiveActionService.findByStatus(QCCorrectiveActionService.STATUS_ASSIGNED));
                actions.addAll(correctiveActionService.findByStatus(QCCorrectiveActionService.STATUS_IN_PROGRESS));
            }

            return ResponseEntity.ok(actions);
        } catch (Exception e) {
            LogEvent.logError("QCCorrectiveActionRestController", "getCorrectiveActions", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific corrective action by ID. GET /rest/qc/corrective-actions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<QCCorrectiveAction> getCorrectiveAction(@PathVariable("id") String id) {
        try {
            QCCorrectiveAction action = correctiveActionService.get(id);
            if (action != null) {
                return ResponseEntity.ok(action);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LogEvent.logError("QCCorrectiveActionRestController", "getCorrectiveAction", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new corrective action. POST /rest/qc/corrective-actions
     */
    @PostMapping
    public ResponseEntity<Object> createCorrectiveAction(@RequestBody @Valid QCCorrectiveActionForm form,
            BindingResult result, HttpServletRequest request) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors());
        }

        try {
            Integer userId = getCurrentUserId(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            QCCorrectiveAction action = correctiveActionService.createAction(form.getViolationId(),
                    form.getActionType(), form.getDescription(), userId);

            // If assignedUserId is provided, assign immediately
            if (form.getAssignedUserId() != null) {
                action = correctiveActionService.assignAction(action.getId(), form.getAssignedUserId());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(action);
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn("QCCorrectiveActionRestController", "createCorrectiveAction", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError("QCCorrectiveActionRestController", "createCorrectiveAction", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating corrective action");
        }
    }

    /**
     * Assign a corrective action to a user. PUT
     * /rest/qc/corrective-actions/{id}/assign
     */
    @PutMapping("/{id}/assign")
    public ResponseEntity<Object> assignCorrectiveAction(@PathVariable("id") String id,
            @RequestBody @Valid QCCorrectiveActionForm.AssignRequest assignRequest, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors());
        }

        try {
            QCCorrectiveAction action = correctiveActionService.assignAction(id, assignRequest.getAssignedUserId());
            return ResponseEntity.ok(action);
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn("QCCorrectiveActionRestController", "assignCorrectiveAction", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            LogEvent.logWarn("QCCorrectiveActionRestController", "assignCorrectiveAction", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError("QCCorrectiveActionRestController", "assignCorrectiveAction", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Start working on a corrective action. PUT
     * /rest/qc/corrective-actions/{id}/start
     */
    @PutMapping("/{id}/start")
    public ResponseEntity<Object> startCorrectiveAction(@PathVariable("id") String id) {
        try {
            QCCorrectiveAction action = correctiveActionService.startAction(id);
            return ResponseEntity.ok(action);
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn("QCCorrectiveActionRestController", "startCorrectiveAction", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            LogEvent.logWarn("QCCorrectiveActionRestController", "startCorrectiveAction", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError("QCCorrectiveActionRestController", "startCorrectiveAction", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Complete a corrective action. PUT /rest/qc/corrective-actions/{id}/complete
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<Object> completeCorrectiveAction(@PathVariable("id") String id,
            @RequestBody @Valid QCCorrectiveActionForm.CompleteRequest completeRequest, BindingResult result,
            HttpServletRequest request) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors());
        }

        try {
            Integer userId = getCurrentUserId(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            QCCorrectiveAction action = correctiveActionService.completeAction(id, completeRequest.getResolutionNotes(),
                    userId);
            return ResponseEntity.ok(action);
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn("QCCorrectiveActionRestController", "completeCorrectiveAction", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            LogEvent.logWarn("QCCorrectiveActionRestController", "completeCorrectiveAction", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError("QCCorrectiveActionRestController", "completeCorrectiveAction", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get count of incomplete corrective actions. GET
     * /rest/qc/corrective-actions/count/incomplete
     */
    @GetMapping("/count/incomplete")
    public ResponseEntity<Integer> getIncompleteCount() {
        try {
            int count = correctiveActionService.getIncompleteCount();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            LogEvent.logError("QCCorrectiveActionRestController", "getIncompleteCount", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get corrective actions assigned to the current user. GET
     * /rest/qc/corrective-actions/my-tasks
     */
    @GetMapping("/my-tasks")
    public ResponseEntity<Object> getMyTasks(HttpServletRequest request) {
        try {
            Integer userId = getCurrentUserId(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            List<QCCorrectiveAction> actions = correctiveActionService.findPendingByAssignedUser(userId);
            return ResponseEntity.ok(actions);
        } catch (Exception e) {
            LogEvent.logError("QCCorrectiveActionRestController", "getMyTasks", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Integer getCurrentUserId(HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession()
                    .getAttribute(IActionConstants.USER_SESSION_DATA);
            if (usd != null) {
                return Integer.valueOf(usd.getSystemUserId());
            }
        } catch (Exception e) {
            LogEvent.logWarn("QCCorrectiveActionRestController", "getCurrentUserId",
                    "Could not get current user ID: " + e.getMessage());
        }
        return null;
    }
}
