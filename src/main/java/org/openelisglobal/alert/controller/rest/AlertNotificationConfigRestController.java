package org.openelisglobal.alert.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.openelisglobal.alert.service.AlertNotificationConfigService;
import org.openelisglobal.common.util.ControllerUtills;
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
    public ResponseEntity<Map<String, String>> saveAlertNotificationConfig(@RequestBody Map<String, Object> config,
            HttpServletRequest request) {
        String sysUserId = ControllerUtills.getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Authenticated session required to save alert configuration"));
        }
        try {
            alertNotificationConfigService.saveAlertNotificationConfig(config, sysUserId);
            return ResponseEntity.ok(Map.of("message", "Alert notification configuration saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to save configuration: " + e.getMessage()));
        }
    }
}
