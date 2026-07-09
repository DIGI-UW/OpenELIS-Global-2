package org.openelisglobal.qc.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.qc.service.QCAlertService;
import org.openelisglobal.qc.valueholder.QCAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for QC Alert management. Supports the Alerts tab on the QC
 * Dashboard.
 *
 * Following Constitution IV.5: @Transactional in services ONLY (NOT
 * controllers)
 */
@RestController
@RequestMapping("/rest/qc/alerts")
public class QCAlertRestController {

    @Autowired
    private QCAlertService alertService;

    /**
     * Get alerts for the current user. GET /rest/qc/alerts?unreadOnly=false
     */
    @GetMapping
    public ResponseEntity<Object> getAlerts(@RequestParam(defaultValue = "false") boolean unreadOnly,
            HttpServletRequest request) {
        try {
            Integer userId = getCurrentUserId(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            List<QCAlert> alerts;
            if (unreadOnly) {
                alerts = alertService.getUnreadAlertsForUser(userId);
            } else {
                alerts = alertService.getAlertsForUser(userId);
            }
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            LogEvent.logError("QCAlertRestController", "getAlerts", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get unread alert count for the current user. GET /rest/qc/alerts/count/unread
     */
    @GetMapping("/count/unread")
    public ResponseEntity<Object> getUnreadCount(HttpServletRequest request) {
        try {
            Integer userId = getCurrentUserId(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            int count = alertService.getUnreadAlertCount(userId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            LogEvent.logError("QCAlertRestController", "getUnreadCount", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mark a single alert as read. PUT /rest/qc/alerts/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Object> markAsRead(@PathVariable("id") String id) {
        try {
            QCAlert alert = alertService.markAsRead(id);
            if (alert != null) {
                return ResponseEntity.ok(alert);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LogEvent.logError("QCAlertRestController", "markAsRead", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mark multiple alerts as read. PUT /rest/qc/alerts/read
     */
    @PutMapping("/read")
    public ResponseEntity<Object> markMultipleAsRead(@RequestBody List<String> alertIds) {
        try {
            int count = alertService.markMultipleAsRead(alertIds);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            LogEvent.logError("QCAlertRestController", "markMultipleAsRead", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Integer getCurrentUserId(HttpServletRequest request) {
        String sysUserId = ControllerUtills.getSysUserId(request);
        return sysUserId != null ? Integer.valueOf(sysUserId) : null;
    }
}