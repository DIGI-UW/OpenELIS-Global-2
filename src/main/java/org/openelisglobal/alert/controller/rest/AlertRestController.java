package org.openelisglobal.alert.controller.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.alert.form.AcknowledgeAlertRequest;
import org.openelisglobal.alert.form.AlertDTO;
import org.openelisglobal.alert.form.ResolveAlertRequest;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/alerts")
public class AlertRestController {

    @Autowired
    private AlertService alertService;

    @GetMapping
    public ResponseEntity<List<AlertDTO>> getAlerts(@RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId) {

        List<Alert> alerts;

        if (entityType != null && entityId != null) {
            alerts = alertService.getAlertsByEntity(entityType, entityId);
        } else {
            alerts = alertService.getAll();
        }

        List<AlertDTO> alertDTOs = alerts.stream().map(this::convertToDTO).collect(Collectors.toList());

        return ResponseEntity.ok(alertDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertDTO> getAlertById(@PathVariable Long id) {
        try {
            Alert alert = alertService.get(id);
            return ResponseEntity.ok(convertToDTO(alert));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<AlertDTO> acknowledgeAlert(@PathVariable Long id,
            @RequestBody AcknowledgeAlertRequest request) {

        Alert acknowledgedAlert = alertService.acknowledgeAlert(id, request.getUserId());
        return ResponseEntity.ok(convertToDTO(acknowledgedAlert));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<AlertDTO> resolveAlert(@PathVariable Long id, @RequestBody ResolveAlertRequest request) {

        Alert resolvedAlert = alertService.resolveAlert(id, request.getUserId(), request.getResolutionNotes());
        return ResponseEntity.ok(convertToDTO(resolvedAlert));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countActiveAlerts(@RequestParam String entityType,
            @RequestParam Long entityId) {

        Long count = alertService.countActiveAlertsForEntity(entityType, entityId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    private AlertDTO convertToDTO(Alert alert) {
        AlertDTO dto = new AlertDTO();
        dto.setId(alert.getId());
        dto.setAlertType(alert.getAlertType() != null ? alert.getAlertType().name() : null);
        dto.setAlertEntityType(alert.getAlertEntityType());
        dto.setAlertEntityId(alert.getAlertEntityId());
        dto.setSeverity(alert.getSeverity() != null ? alert.getSeverity().name() : null);
        dto.setStatus(alert.getStatus() != null ? alert.getStatus().name() : null);
        dto.setStartTime(alert.getStartTime());
        dto.setEndTime(alert.getEndTime());
        dto.setMessage(alert.getMessage());
        dto.setContextData(alert.getContextData());
        dto.setAcknowledgedAt(alert.getAcknowledgedAt());
        dto.setAcknowledgedBy(
                alert.getAcknowledgedBy() != null ? Integer.parseInt(alert.getAcknowledgedBy().getId()) : null);
        dto.setResolvedAt(alert.getResolvedAt());
        dto.setResolvedBy(alert.getResolvedBy() != null ? Integer.parseInt(alert.getResolvedBy().getId()) : null);
        dto.setResolutionNotes(alert.getResolutionNotes());
        dto.setDuplicateCount(alert.getDuplicateCount());
        dto.setLastDuplicateTime(alert.getLastDuplicateTime());
        return dto;
    }
}
