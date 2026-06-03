package org.openelisglobal.referral.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralSubcontract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/referrals")
@PreAuthorize("isAuthenticated()")
public class ReferralSubcontractDispatchRestController {

    @Autowired
    private ReferralService referralService;

    @PostMapping("/{referralId}/dispatch-subcontract")
    public ResponseEntity<Map<String, Object>> dispatchSubcontract(@PathVariable String referralId,
            @RequestBody(required = false) DispatchRequest body, HttpServletRequest request) {
        if (body == null || GenericValidator.isBlankOrNull(body.handoffDatetime)) {
            return ResponseEntity.badRequest().body(Map.of("error", "handoffDatetime is required"));
        }
        Timestamp handoff;
        try {
            handoff = DateUtil.convertStringDateToTimestamp(body.handoffDatetime);
            if (handoff == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "handoffDatetime could not be parsed"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "handoffDatetime could not be parsed"));
        }
        String actorUserId = ControllerUtills.getSysUserId(request);
        if (GenericValidator.isBlankOrNull(actorUserId)) {
            return ResponseEntity.status(401).body(Map.of("error", "no authenticated user"));
        }
        Referral referral;
        try {
            referralService.dispatchSubcontract(referralId, handoff, actorUserId, body.notes);
            referral = referralService.getReferralById(referralId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        ReferralSubcontract subcontract = referral.getSubcontract();
        Map<String, Object> resp = new HashMap<>();
        resp.put("referralId", referral.getId());
        resp.put("subcontractId", subcontract.getId());
        resp.put("subcontractStatus", subcontract.getSubcontractStatus().name());
        return ResponseEntity.ok(resp);
    }

    public static class DispatchRequest {
        public String handoffDatetime;
        public String notes;

        public String getHandoffDatetime() {
            return handoffDatetime;
        }

        public void setHandoffDatetime(String handoffDatetime) {
            this.handoffDatetime = handoffDatetime;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}
