package org.openelisglobal.qaevent.criticalcallback.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.qaevent.criticalcallback.service.CriticalCallbackService;
import org.openelisglobal.qaevent.criticalcallback.valueholder.CriticalCallback;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * OGC-714 [QA-C.4] — logs a manual critical-result callback (TJC NPSG.02.03.01
 * / CLSI GP47 read-back documentation).
 *
 * <p>
 * Write-only in this PR; the compliance read endpoints (summary/detail) grow on
 * this controller in the follow-up. Gated on {@code qa.view.qi} — the QI pillar
 * permission held by exactly the roles that make callbacks (Reception/
 * Results/Validation, plus QA Officer and Global Admin); a dedicated
 * manage-callback scope would be over-engineering for a manual documentation
 * form.
 *
 * <p>
 * The body carries only {@code resultId}, {@code recipientName} and
 * {@code status}: a callback is logged against a PERSISTED result (per the C.4
 * outline §5 — the call is made and documented after the value is in the
 * record). Caller identity, time, the owning analysis, and the communicated
 * value ({@code result.value} snapshot) are all stamped server-side, never
 * client-supplied. The saved value must actually be critical (outside-band vs
 * the resolved ResultLimit) or the request is rejected — documentation cannot
 * be created for a value the record does not support. Repeat POSTs for the same
 * result are additional attempt rows by design.
 */
@RestController
@RequestMapping("/rest/critical-callback")
@PreAuthorize("hasAuthority('qa.view.qi') or hasRole('GLOBAL_ADMIN')")
public class CriticalCallbackRestController extends BaseRestController {

    private static final Set<String> STATUSES = Set.of("CONFIRMED", "REACHED_NO_READBACK", "UNABLE_TO_REACH");

    private static final int RECIPIENT_NAME_MAX = 255;

    private final CriticalCallbackService callbackService;

    private final ResultService resultService;

    private final ResultLimitService resultLimitService;

    public CriticalCallbackRestController(CriticalCallbackService callbackService, ResultService resultService,
            ResultLimitService resultLimitService) {
        this.callbackService = callbackService;
        this.resultService = resultService;
        this.resultLimitService = resultLimitService;
    }

    /** Create payload for a callback log entry. */
    public static class CallbackRequest {
        public String resultId;
        public String recipientName;
        public String status;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CriticalCallback> create(@RequestBody CallbackRequest body, HttpServletRequest request) {
        Result result = validate(body);

        CriticalCallback callback = new CriticalCallback();
        callback.setResultId(result.getId());
        callback.setAnalysisId(result.getAnalysis().getId());
        callback.setResultValue(result.getValue());
        callback.setRecipientName(body.recipientName.trim());
        callback.setStatus(body.status);
        callback.setLoggedAt(Timestamp.from(Instant.now()));
        callback.setLoggedBy(ControllerUtills.getSysUserId(request));
        callback.setSysUserId(ControllerUtills.getSysUserId(request));
        callbackService.insert(callback);
        return ResponseEntity.status(HttpStatus.CREATED).body(callback);
    }

    private Result validate(CallbackRequest body) {
        if (body.resultId == null || body.resultId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resultId is required");
        }
        Result result = resultService.getResultById(body.resultId);
        if (result == null || result.getAnalysis() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resultId must reference an existing result");
        }
        if (!isCritical(result)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "result is not critical: the saved value is not at or beyond the configured critical bounds");
        }
        if (body.recipientName == null || body.recipientName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipientName is required");
        }
        if (body.recipientName.trim().length() > RECIPIENT_NAME_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipientName must be at most 255 characters");
        }
        if (body.status == null || !STATUSES.contains(body.status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "status must be one of CONFIRMED, REACHED_NO_READBACK, UNABLE_TO_REACH");
        }
        return result;
    }

    /**
     * Server-side criticality check, mirroring the Results-Entry rule: numeric
     * result at or beyond a configured critical bound (outside band). Per-bound
     * finiteness guards handle both unconfigured conventions (-Infinity/Infinity
     * from the DB; POSITIVE_INFINITY/POSITIVE_INFINITY entity defaults collapse via
     * the low==high guard). This is the same predicate the compliance compute
     * (OGC-714 read side) applies to the denominator.
     */
    private boolean isCritical(Result result) {
        if (!"N".equals(result.getResultType()) || result.getValue() == null) {
            return false;
        }
        ResultLimit limit;
        try {
            limit = resultLimitService.getResultLimitForAnalysis(result.getAnalysis());
        } catch (RuntimeException e) {
            // Degenerate analysis chain (no sample item/sample): criticality
            // cannot be established, so the callback is rejected as
            // not-critical (400) rather than surfacing a 500.
            return false;
        }
        if (limit == null) {
            return false;
        }
        double low = limit.getLowCritical();
        double high = limit.getHighCritical();
        if (low == high) {
            return false;
        }
        double value;
        try {
            value = Double.parseDouble(result.getValue().replaceAll("[<>]", "").trim());
        } catch (NumberFormatException e) {
            return false;
        }
        return (Double.isFinite(low) && value <= low) || (Double.isFinite(high) && value >= high);
    }
}
