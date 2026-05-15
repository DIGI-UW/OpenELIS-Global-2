package org.openelisglobal.alert.controller.rest;

import java.util.Map;
import org.openelisglobal.alert.service.AlertNotificationConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/alert-notification-config")
@SuppressWarnings("unused")
@PreAuthorize("hasRole('ADMIN')")
public class AlertNotificationConfigRestController {

    private static final String INVALID_ESCALATION_DELAY_MESSAGE = "Invalid escalationDelayMinutes: must be an integer";

    @Autowired
    private AlertNotificationConfigService alertNotificationConfigService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAlertNotificationConfig() {
        try {
            Map<String, Object> config = alertNotificationConfigService.getAlertNotificationConfig();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> saveAlertNotificationConfig(@RequestBody Map<String, Object> config) {
        try {
            alertNotificationConfigService.saveAlertNotificationConfig(config);
            return ResponseEntity.ok(Map.of("message", "Alert notification configuration saved successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", INVALID_ESCALATION_DELAY_MESSAGE));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to save configuration: " + e.getMessage()));
        }
    }
}
