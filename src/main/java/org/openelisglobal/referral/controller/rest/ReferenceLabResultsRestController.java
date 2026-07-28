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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reference-lab referral tracking dashboard. There is no
 * {@code system_module_url} row for {@code /rest/reference-lab-results/**}, and
 * {@code ModuleAuthenticationInterceptor} fails open for unmapped {@code /rest}
 * paths, so authorization has to be declared here.
 *
 * <p>
 * The class-level expression covers the read-only dashboard queries; the state
 * transitions and the outbound notification narrow it further at method level.
 */
@RestController
@RequestMapping("/rest/reference-lab-results")
@PreAuthorize("hasAnyRole('RECEPTION', 'RESULTS', 'VALIDATION', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('RESULTS', 'VALIDATION', 'ADMIN')")
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

    @PutMapping("/referrals/{referralId}/accept")
    @PreAuthorize("hasAnyRole('RESULTS', 'VALIDATION', 'ADMIN')")
    public ResponseEntity<?> acceptReferral(@PathVariable String referralId, HttpServletRequest request) {
        try {
            referenceLabResultsService.acceptReferral(referralId, getSysUserId(request));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/referrals/{referralId}/reject")
    @PreAuthorize("hasAnyRole('RESULTS', 'VALIDATION', 'ADMIN')")
    public ResponseEntity<?> rejectReferral(@PathVariable String referralId, @Valid @RequestBody RejectRequest body,
            BindingResult result, HttpServletRequest request) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        try {
            referralService.markReferralRejected(referralId, body.getReasonCode(), body.getReasonText(),
                    getSysUserId(request));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/referrals/{referralId}/notify")
    @PreAuthorize("hasAnyRole('RESULTS', 'VALIDATION', 'ADMIN')")
    public ResponseEntity<?> notifyReferenceLab(@PathVariable String referralId,
            @RequestBody(required = false) NotifyRequest body, HttpServletRequest request) {
        try {
            String message = body == null ? null : body.getMessage();
            referralService.nudgeReferenceLab(referralId, message, getSysUserId(request));
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

    public static class NotifyRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class RejectRequest {
        private String reasonCode;

        @NotBlank
        private String reasonText;

        public String getReasonCode() {
            return reasonCode;
        }

        public void setReasonCode(String reasonCode) {
            this.reasonCode = reasonCode;
        }

        public String getReasonText() {
            return reasonText;
        }

        public void setReasonText(String reasonText) {
            this.reasonText = reasonText;
        }
    }
}
