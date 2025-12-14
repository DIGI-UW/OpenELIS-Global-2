package org.openelisglobal.pharmaceutical.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.pharmaceutical.service.EnvironmentalExcursionService;
import org.openelisglobal.pharmaceutical.valueholder.EnvironmentalExcursionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/pharmaceutical/excursions")
public class EnvironmentalExcursionRestController extends BaseRestController {

    @Autowired
    private EnvironmentalExcursionService environmentalExcursionService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EnvironmentalExcursionEvent>> getAll() {
        try {
            List<EnvironmentalExcursionEvent> events = environmentalExcursionService.getAll();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EnvironmentalExcursionEvent> getById(@PathVariable Integer id) {
        try {
            EnvironmentalExcursionEvent event = environmentalExcursionService.get(id);
            if (event == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/device/{deviceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EnvironmentalExcursionEvent>> getByDeviceId(@PathVariable Integer deviceId) {
        try {
            List<EnvironmentalExcursionEvent> events = environmentalExcursionService.findByDeviceId(deviceId);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EnvironmentalExcursionEvent>> getByStatus(
            @PathVariable EnvironmentalExcursionEvent.ExcursionStatus status) {
        try {
            List<EnvironmentalExcursionEvent> events = environmentalExcursionService.findByStatus(status);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EnvironmentalExcursionEvent>> getActiveExcursions() {
        try {
            List<EnvironmentalExcursionEvent> events = environmentalExcursionService.findActiveExcursions();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/unacknowledged", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EnvironmentalExcursionEvent>> getUnacknowledgedExcursions() {
        try {
            List<EnvironmentalExcursionEvent> events = environmentalExcursionService.getUnacknowledgedExcursions();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/alert-type/{alertType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EnvironmentalExcursionEvent>> getByAlertType(
            @PathVariable EnvironmentalExcursionEvent.AlertType alertType) {
        try {
            List<EnvironmentalExcursionEvent> events = environmentalExcursionService.findByAlertType(alertType);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/device/{deviceId}/has-active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ActiveExcursionResponse> hasActiveExcursions(@PathVariable Integer deviceId) {
        try {
            boolean hasActive = environmentalExcursionService.hasActiveExcursions(deviceId);
            return ResponseEntity.ok(new ActiveExcursionResponse(hasActive));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/record", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EnvironmentalExcursionEvent> recordExcursion(
            @RequestBody ExcursionRecordRequest recordRequest,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            EnvironmentalExcursionEvent event = environmentalExcursionService.recordExcursion(
                    recordRequest.getDeviceId(),
                    recordRequest.getAlertType(),
                    recordRequest.getRecordedValue(),
                    recordRequest.getThresholdValue(),
                    recordRequest.getDeviceLocation(),
                    userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(event);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{id}/acknowledge", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EnvironmentalExcursionEvent> acknowledge(
            @PathVariable Integer id,
            @RequestParam(required = false) String notes,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            EnvironmentalExcursionEvent acknowledgedEvent = environmentalExcursionService.acknowledgeExcursion(
                    id, notes, userId);
            return ResponseEntity.ok(acknowledgedEvent);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{id}/resolve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EnvironmentalExcursionEvent> resolve(
            @PathVariable Integer id,
            @RequestParam String resolutionNotes,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            EnvironmentalExcursionEvent resolvedEvent = environmentalExcursionService.resolveExcursion(
                    id, resolutionNotes, userId);
            return ResponseEntity.ok(resolvedEvent);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{id}/escalate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EnvironmentalExcursionEvent> escalate(
            @PathVariable Integer id,
            @RequestParam String escalationReason,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            EnvironmentalExcursionEvent escalatedEvent = environmentalExcursionService.escalateExcursion(
                    id, escalationReason, userId);
            return ResponseEntity.ok(escalatedEvent);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        try {
            environmentalExcursionService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}/affected-samples", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getAffectedSamples(@PathVariable Integer id) {
        try {
            List<Map<String, Object>> affectedSamples = environmentalExcursionService.getAffectedSamples(id);
            return ResponseEntity.ok(affectedSamples);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Setter
    @Getter
    public static class ExcursionRecordRequest {
        private Integer deviceId;
        private EnvironmentalExcursionEvent.AlertType alertType;
        private Double recordedValue;
        private Double thresholdValue;
        private String deviceLocation;
    }

    @Getter
    public static class ActiveExcursionResponse {
        private Boolean hasActive;

        public ActiveExcursionResponse(Boolean hasActive) {
            this.hasActive = hasActive;
        }
    }
}
