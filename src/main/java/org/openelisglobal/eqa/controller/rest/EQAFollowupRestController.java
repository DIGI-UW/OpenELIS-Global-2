package org.openelisglobal.eqa.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.eqa.service.EQAParticipantFollowupService;
import org.openelisglobal.eqa.service.EQAShipmentService;
import org.openelisglobal.eqa.valueholder.EQADismissalCategory;
import org.openelisglobal.eqa.valueholder.EQAFollowupStatus;
import org.openelisglobal.eqa.valueholder.EQAParticipantFollowup;
import org.openelisglobal.qaevent.service.EqaScoreNceService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Follow-Up Queue triage API (OGC-611, FR-V2.3-02): the queue this lab owes a
 * corrective review on, plus its escalate and dismiss actions.
 */
@RestController
@RequestMapping("/rest/eqa")
@PreAuthorize(EQAGuards.READ)
public class EQAFollowupRestController extends BaseRestController {

    private final EQAParticipantFollowupService followupService;
    private final EqaScoreNceService eqaScoreNceService;
    private final EQAShipmentService shipmentService;

    public EQAFollowupRestController(EQAParticipantFollowupService followupService,
            EqaScoreNceService eqaScoreNceService, EQAShipmentService shipmentService) {
        this.followupService = followupService;
        this.eqaScoreNceService = eqaScoreNceService;
        this.shipmentService = shipmentService;
    }

    @GetMapping(value = "/followups", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> queue() {
        return followupService.getQueueRows();
    }

    // ---- T-27: the provider-side register (FR-V2.5-05..08) ----

    @GetMapping(value = "/provider/followups", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> register() {
        return followupService.getProviderRegisterRows();
    }

    /** Triage: {"target": "UNDER_INVESTIGATION", "notes": "..."}. */
    @PostMapping(value = "/provider/followups/{followupId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.PROVIDER)
    public Map<String, Object> triage(HttpServletRequest request, @PathVariable Long followupId,
            @RequestBody Map<String, Object> body) {
        String target = stringField(body, "target");
        if (target == null) {
            throw new IllegalArgumentException("A triage action needs a target status");
        }
        EQAFollowupStatus status;
        try {
            status = EQAFollowupStatus.valueOf(target.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown follow-up status " + target);
        }
        EQAParticipantFollowup moved = followupService.transitionStatus(followupId, status, stringField(body, "notes"),
                getSysUserId(request));
        return Map.of("followupId", moved.getId(), "followupStatus", moved.getFollowupStatus().name());
    }

    @PostMapping(value = "/provider/followups/{followupId}/notify", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.PROVIDER)
    public Map<String, Object> notifyParticipant(HttpServletRequest request, @PathVariable Long followupId) {
        return followupService.notifyParticipant(followupId, getSysUserId(request));
    }

    /**
     * Flag for repeat (FR-V2.5-06 → FR-V2.5-15): reprovisioning is the shipment
     * service's job, and the register row keeps its own status — the repeat is an
     * action taken during triage, not a state of the follow-up.
     */
    @PostMapping(value = "/provider/followups/{followupId}/repeat", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.PROVIDER)
    public Map<String, Object> repeat(HttpServletRequest request, @PathVariable Long followupId,
            @RequestBody(required = false) Map<String, Object> body) {
        EQAParticipantFollowup followup = followupService.get(followupId);
        if (followup.getCycle() == null) {
            throw new IllegalArgumentException("This follow-up names no cycle, so nothing can be reprovisioned");
        }
        return shipmentService.sendRepeat(followup.getCycle().getId(), followup.getParticipantOrgId(),
                body == null ? null : stringField(body, "overrideNote"), getSysUserId(request));
    }

    @PostMapping(value = "/followups/{followupId}/escalate", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.MANAGE)
    public ResponseEntity<Map<String, Object>> escalate(HttpServletRequest request, @PathVariable Long followupId) {
        NcEvent nce = eqaScoreNceService.escalateFollowup(followupId, getSysUserId(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("followupId", followupId, "nceId", nce.getId(), "nceNumber", nce.getNceNumber()));
    }

    @PostMapping(value = "/followups/{followupId}/dismiss", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.MANAGE)
    public Map<String, Object> dismiss(HttpServletRequest request, @PathVariable Long followupId,
            @RequestBody Map<String, Object> body) {
        Object rawCategory = body.get("category");
        if (rawCategory == null) {
            throw new IllegalArgumentException("A dismissal needs a category");
        }
        EQADismissalCategory category;
        try {
            category = EQADismissalCategory.valueOf(String.valueOf(rawCategory).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown dismissal category " + rawCategory);
        }
        String notes = body.get("notes") == null ? null : String.valueOf(body.get("notes"));
        EQAParticipantFollowup dismissed = followupService.dismiss(followupId, category, notes, getSysUserId(request));
        return Map.of("followupId", dismissed.getId(), "followupStatus", dismissed.getFollowupStatus().name(),
                "dismissalCategory", category.name());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> onBadRequest(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> onConflict(IllegalStateException e) {
        return Map.of("error", e.getMessage());
    }
}
