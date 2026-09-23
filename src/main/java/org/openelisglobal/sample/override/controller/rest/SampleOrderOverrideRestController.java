package org.openelisglobal.sample.override.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.sample.override.service.SampleOrderOverrideService;
import org.openelisglobal.sample.override.valueholder.OverrideReasonCode;
import org.openelisglobal.sample.override.valueholder.OverrideType;
import org.openelisglobal.sample.override.valueholder.SampleOrderOverride;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reading and recording the deliberate decisions taken on an order.
 *
 * Both overrides this serves — ordering without a patient, and continuing with
 * testing after a failed acceptance check — were previously unrecorded: the
 * user simply proceeded and nothing was kept.
 */
@RestController
@RequestMapping("/rest/order-override")
public class SampleOrderOverrideRestController extends BaseRestController {

    @Autowired
    private SampleOrderOverrideService sampleOrderOverrideService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private HttpServletRequest request;

    @GetMapping(value = "/{labNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getOverrides(@PathVariable String labNumber) {
        Sample sample = sampleService.getSampleByAccessionNumber(labNumber);
        if (sample == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Order not found"));
        }
        List<Map<String, Object>> body = new ArrayList<>();
        for (SampleOrderOverride override : sampleOrderOverrideService.findBySampleId(Long.valueOf(sample.getId()))) {
            body.add(toMap(override));
        }
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/{labNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> recordOverride(@PathVariable String labNumber,
            @RequestBody Map<String, Object> requestBody) {
        Sample sample = sampleService.getSampleByAccessionNumber(labNumber);
        if (sample == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Order not found"));
        }

        OverrideType overrideType;
        OverrideReasonCode reasonCode;
        try {
            overrideType = OverrideType.valueOf(String.valueOf(requestBody.get("overrideType")));
            reasonCode = OverrideReasonCode.valueOf(String.valueOf(requestBody.getOrDefault("reasonCode", "MANUAL")));
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown override type or reason code"));
        }

        String reason = requestBody.get("reason") == null ? null : String.valueOf(requestBody.get("reason"));
        // A manual decision without a stated reason records nothing useful; EQA
        // supplies its own reason, so only the manual path is held to this.
        if (reasonCode == OverrideReasonCode.MANUAL && (reason == null || reason.isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "A reason is required for a manual override"));
        }

        Long userId = null;
        try {
            String sysUserId = getSysUserId(request);
            if (sysUserId != null) {
                userId = Long.valueOf(sysUserId);
            }
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "recordOverride", "no user id on the request");
        }

        SampleOrderOverride saved = sampleOrderOverrideService.record(Long.valueOf(sample.getId()), overrideType,
                reasonCode, reason, userId);
        Map<String, Object> body = toMap(saved);
        body.put("success", true);
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> toMap(SampleOrderOverride override) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("id", override.getId());
        entry.put("overrideType", override.getOverrideType() == null ? null : override.getOverrideType().name());
        entry.put("reasonCode", override.getReasonCode() == null ? null : override.getReasonCode().name());
        entry.put("reason", override.getReason());
        entry.put("overrideUserId", override.getOverrideUserId());
        entry.put("recordedDate", override.getRecordedDate());
        return entry;
    }
}
