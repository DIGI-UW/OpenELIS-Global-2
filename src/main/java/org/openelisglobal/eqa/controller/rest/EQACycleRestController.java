package org.openelisglobal.eqa.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.service.EQAInvalidTransitionException;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cycle read + transition API (T-10).
 *
 * <p>
 * No PUT or DELETE is mapped for the transitions path: audit rows are immutable
 * (FR-V2.1-21), and leaving those methods unmapped makes Spring answer 405.
 */
@RestController
@RequestMapping("/rest/eqa")
@PreAuthorize("hasAnyRole('RECEPTION', 'RESULTS')")
public class EQACycleRestController extends BaseRestController {

    private final EQACycleService cycleService;

    public EQACycleRestController(EQACycleService cycleService) {
        this.cycleService = cycleService;
    }

    /**
     * Cycles this lab takes part in. OpenELIS runs single-tenant, so every cycle in
     * the database belongs to this lab; passing a labEnrollmentId adds that
     * enrollment's derived participant state to each row.
     */
    @GetMapping(value = "/cycles/mine", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> myCycles(@RequestParam(required = false) Long labEnrollmentId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQACycle cycle : cycleService.getAll()) {
            Map<String, Object> dto = toCycleDto(cycle);
            if (labEnrollmentId != null) {
                dto.put("participantState", cycleService.deriveParticipantState(cycle, labEnrollmentId).name());
            }
            rows.add(dto);
        }
        return rows;
    }

    @GetMapping(value = "/cycles/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getCycle(@PathVariable Long id) {
        return toCycleDto(cycleService.get(id));
    }

    /** Computed, never stored (FR-V2.1-18). */
    @GetMapping(value = "/cycles/{id}/participant-state", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> participantState(@PathVariable Long id, @RequestParam Long labEnrollmentId) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("cycleId", id);
        dto.put("labEnrollmentId", labEnrollmentId);
        dto.put("participantState", cycleService.deriveParticipantState(id, labEnrollmentId).name());
        return dto;
    }

    @GetMapping(value = "/cycles/{id}/transitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> transitions(@PathVariable Long id) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQACycleStateTransition t : cycleService.getTransitions(id)) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", t.getId());
            dto.put("priorState", t.getPriorState());
            dto.put("newState", t.getNewState());
            dto.put("stateMachine", t.getStateMachine() == null ? null : t.getStateMachine().name());
            dto.put("triggerType", t.getTriggerType() == null ? null : t.getTriggerType().name());
            dto.put("triggerEvent", t.getTriggerEvent() == null ? null : t.getTriggerEvent().name());
            dto.put("triggeredBy", t.getTriggeredBy());
            dto.put("reason", t.getReason());
            dto.put("occurredAt", t.getOccurredAt() == null ? null : t.getOccurredAt().toString());
            rows.add(dto);
        }
        return rows;
    }

    /**
     * Advancing a cycle mutates lab-wide state and writes a permanent audit row, so
     * it needs a manage-level grant rather than the class-level read roles
     * (FR-V2.1-04).
     *
     * <p>
     * Provenance is never taken from the request body: an HTTP call is a person
     * acting, so it is always recorded as a MANUAL override attributed to the
     * session user (FR-V2.1-21).
     */
    @PatchMapping(value = "/cycles/{id}/transition", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.manage.eqa') or hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Map<String, Object>> transition(HttpServletRequest request, @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String newState = stringField(body, "newState");
        if (newState == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "newState is required"));
        }

        EQACycleStatus targetState;
        EQAStateMachine machine;
        try {
            targetState = EQACycleStatus.valueOf(newState.toUpperCase());
            String machineName = stringField(body, "stateMachine");
            machine = machineName == null ? EQAStateMachine.PARTICIPANT
                    : EQAStateMachine.valueOf(machineName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown state or state machine"));
        }

        String sysUserId = getSysUserId(request);
        EQACycle updated = cycleService.transition(id, targetState, machine, EQATriggerType.MANUAL,
                EQATriggerEvent.MANUAL_OVERRIDE, actingUserId(sysUserId), stringField(body, "reason"), sysUserId);

        return ResponseEntity.ok(toCycleDto(updated));
    }

    /** Tolerates non-string JSON values rather than throwing ClassCastException. */
    private String stringField(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * The session's user id as the audit actor. A manual transition with no
     * resolvable actor is refused rather than recorded anonymously.
     */
    private Long actingUserId(String sysUserId) {
        try {
            return Long.valueOf(sysUserId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot attribute this transition to an authenticated user");
        }
    }

    private Map<String, Object> toCycleDto(EQACycle cycle) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", cycle.getId());
        dto.put("cycleNumber", cycle.getCycleNumber());
        dto.put("cycleName", cycle.getCycleName());
        dto.put("status", cycle.getStatus() == null ? null : cycle.getStatus().name());
        dto.put("schemeId", cycle.getScheme() == null ? null : cycle.getScheme().getId());
        dto.put("plannedStartDate",
                cycle.getPlannedStartDate() == null ? null : cycle.getPlannedStartDate().toString());
        dto.put("plannedEndDate", cycle.getPlannedEndDate() == null ? null : cycle.getPlannedEndDate().toString());
        return dto;
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(ObjectNotFoundException e) {
        return Map.of("error", "EQA cycle not found");
    }

    /** An edge that is not in the machine is a conflict, not bad input. */
    @ExceptionHandler(EQAInvalidTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleInvalidTransition(EQAInvalidTransitionException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", e.getMessage());
        body.put("priorState", e.getPriorState() == null ? null : e.getPriorState().name());
        body.put("attemptedState", e.getAttemptedState() == null ? null : e.getAttemptedState().name());
        return body;
    }

    /**
     * A legal edge missing its required reason is unprocessable, not a conflict.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleBadInput(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
