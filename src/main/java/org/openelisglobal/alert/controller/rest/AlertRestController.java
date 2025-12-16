package org.openelisglobal.alert.controller.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.alert.form.AcknowledgeAlertRequest;
import org.openelisglobal.alert.form.AlertDTO;
import org.openelisglobal.alert.form.FreezerDTO;
import org.openelisglobal.alert.form.ResolveAlertRequest;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
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

    @Autowired
    private FreezerService freezerService;

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
        try {
            Alert acknowledgedAlert = alertService.acknowledgeAlert(id, request.getUserId());
            return ResponseEntity.ok(convertToDTO(acknowledgedAlert));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            // Handle optimistic locking failures and other exceptions
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<AlertDTO> resolveAlert(@PathVariable Long id, @RequestBody ResolveAlertRequest request) {
        try {
            Alert resolvedAlert = alertService.resolveAlert(id, request.getUserId(), request.getResolutionNotes());
            return ResponseEntity.ok(convertToDTO(resolvedAlert));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            // Handle optimistic locking failures and other exceptions
            return ResponseEntity.status(500).build();
        }
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

        if ("Freezer".equals(alert.getAlertEntityType()) && alert.getAlertEntityId() != null) {
            try {
                Freezer freezer = freezerService.findById(alert.getAlertEntityId()).orElse(null);
                if (freezer != null) {
                    FreezerDTO freezerDTO = new FreezerDTO();
                    freezerDTO.setId(freezer.getId());
                    freezerDTO.setName(freezer.getName());
                    if (freezer.getStorageDevice() != null) {
                        freezerDTO.setCode(freezer.getStorageDevice().getCode());
                    }
                    dto.setFreezer(freezerDTO);
                }
            } catch (Exception e) {
                // Log error but don't fail the entire request
                // The frontend will fall back to showing "Unknown"
            }
        }

        return dto;
    }
}
