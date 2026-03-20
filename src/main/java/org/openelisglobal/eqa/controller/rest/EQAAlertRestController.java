package org.openelisglobal.eqa.controller.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class EQAAlertRestController {

    @Autowired
    private AlertService alertService;

    @GetMapping(value = "/alerts/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAlertsDashboard(@RequestParam(required = false) String type,
            @RequestParam(required = false) String severity, @RequestParam(required = false) String status,
            @RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int pageSize) {

        List<Alert> allAlerts = alertService.getAlertsByEntity(null, null);
        if (allAlerts == null) {
            allAlerts = List.of();
        }

        List<Alert> filtered = allAlerts.stream()
                .filter(a -> type == null || type.isEmpty() || a.getAlertType().name().equals(type))
                .filter(a -> severity == null || severity.isEmpty() || a.getSeverity().name().equals(severity))
                .filter(a -> status == null || status.isEmpty() || a.getStatus().name().equals(status))
                .filter(a -> search == null || search.isEmpty()
                        || (a.getMessage() != null && a.getMessage().toLowerCase().contains(search.toLowerCase())))
                .collect(Collectors.toList());

        int totalCount = filtered.size();
        int fromIndex = Math.min(page * pageSize, totalCount);
        int toIndex = Math.min(fromIndex + pageSize, totalCount);
        List<Alert> pagedAlerts = filtered.subList(fromIndex, toIndex);

        Map<String, Object> response = new HashMap<>();
        response.put("alerts", pagedAlerts);
        response.put("totalCount", totalCount);
        response.put("page", page);
        response.put("pageSize", pageSize);

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/alerts/dashboard/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAlertsSummary() {
        List<Alert> allAlerts = alertService.getAlertsByEntity(null, null);
        if (allAlerts == null) {
            allAlerts = List.of();
        }

        long criticalCount = allAlerts.stream()
                .filter(a -> a.getSeverity() == AlertSeverity.CRITICAL && a.getStatus() == AlertStatus.OPEN).count();
        long eqaDeadlineCount = allAlerts.stream()
                .filter(a -> a.getAlertType() == AlertType.EQA_DEADLINE && a.getStatus() == AlertStatus.OPEN).count();
        long statOverdueCount = allAlerts.stream()
                .filter(a -> a.getAlertType() == AlertType.STAT_OVERDUE && a.getStatus() == AlertStatus.OPEN).count();
        long sampleExpirationCount = allAlerts.stream()
                .filter(a -> a.getAlertType() == AlertType.SAMPLE_EXPIRATION && a.getStatus() == AlertStatus.OPEN)
                .count();
        long totalOpen = allAlerts.stream().filter(a -> a.getStatus() == AlertStatus.OPEN).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("criticalAlerts", criticalCount);
        summary.put("eqaDeadlines", eqaDeadlineCount);
        summary.put("statOverdue", statOverdueCount);
        summary.put("sampleExpiration", sampleExpirationCount);
        summary.put("totalOpen", totalOpen);

        return ResponseEntity.ok(summary);
    }

    @PutMapping(value = "/alerts/{id}/acknowledge", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> acknowledgeAlert(@PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {

        List<Alert> alerts = alertService.getAlertsByEntity(null, null);
        Alert target = alerts != null ? alerts.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null)
                : null;

        if (target == null) {
            return ResponseEntity.notFound().build();
        }

        if (target.getSeverity() == AlertSeverity.CRITICAL) {
            if (body == null || body.get("comment") == null || body.get("comment").trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Resolution comment is required for critical alerts"));
            }
        }

        String comment = body != null ? body.get("comment") : null;
        if (target.getStatus() == AlertStatus.OPEN) {
            alertService.acknowledgeAlert(id, null);
        }
        if (comment != null && !comment.trim().isEmpty()) {
            alertService.resolveAlert(id, null, comment);
        }

        return ResponseEntity.ok(Map.of("status", "acknowledged"));
    }
}
