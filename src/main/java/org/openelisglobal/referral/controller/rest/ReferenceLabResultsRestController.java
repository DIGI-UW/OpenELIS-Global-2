package org.openelisglobal.referral.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.referral.dto.ReferenceLabMetricsDTO;
import org.openelisglobal.referral.dto.ReferenceLabReferralDTO;
import org.openelisglobal.referral.service.ReferenceLabResultsService;
import org.openelisglobal.referral.service.ReferenceLabResultsService.DashboardView;
import org.openelisglobal.referral.service.ReferralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/reference-lab-results")
public class ReferenceLabResultsRestController extends BaseRestController {

    @Autowired
    private ReferenceLabResultsService referenceLabResultsService;

    @Autowired
    private ReferralService referralService;

    @GetMapping("/referrals")
    public ResponseEntity<List<ReferenceLabReferralDTO>> listReferrals(
            @RequestParam(name = "view", defaultValue = "outstanding") String view) {
        DashboardView parsed;
        try {
            parsed = parseView(view);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(referenceLabResultsService.getDashboardReferrals(parsed));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<ReferenceLabMetricsDTO> metrics() {
        try {
            return ResponseEntity.ok(referenceLabResultsService.getDashboardMetrics());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/referrals/{referralId}/mark-lost")
    public ResponseEntity<?> markReferralAsLost(@PathVariable String referralId,
            @Valid @RequestBody MarkLostRequest body, BindingResult result, HttpServletRequest request) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        try {
            referralService.markReferralAsLost(referralId, body.getReason(), getSysUserId(request));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private DashboardView parseView(String raw) {
        return switch (raw == null ? "" : raw.toLowerCase()) {
        case "outstanding" -> DashboardView.OUTSTANDING;
        case "returned" -> DashboardView.RETURNED;
        case "history" -> DashboardView.HISTORY;
        default -> throw new IllegalArgumentException("Unknown view: " + raw);
        };
    }

    public static class MarkLostRequest {
        @NotBlank
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
