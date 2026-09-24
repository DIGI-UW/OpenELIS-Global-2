package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerDeliveryIssue;
import org.openelisglobal.analyzer.service.AnalyzerDeliveryIssueService;
import org.openelisglobal.analyzer.service.BridgeAnalyzerConnectionException;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Results the Bridge received but has not delivered to OpenELIS. */
@RestController
@RequestMapping("/rest/analyzer/delivery-issues")
@PreAuthorize("hasAnyRole('ADMIN', 'ANALYSER_IMPORT')")
public class AnalyzerDeliveryIssuesRestController extends BaseRestController {

    private final AnalyzerDeliveryIssueService deliveryIssueService;

    public AnalyzerDeliveryIssuesRestController(AnalyzerDeliveryIssueService deliveryIssueService) {
        this.deliveryIssueService = deliveryIssueService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        try {
            List<AnalyzerDeliveryIssue> rows = deliveryIssueService.getOpenIssues();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", rows.size());
            data.put("rows", rows);
            return ResponseEntity.ok(Map.of("status", "success", "data", data));
        } catch (BridgeAnalyzerConnectionException exception) {
            return bridgeFailure(exception);
        }
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, Object>> retry(@PathVariable String id, HttpServletRequest request) {
        try {
            deliveryIssueService.retry(id, getSysUserId(request));
            return ResponseEntity.ok(Map.of("status", "success", "id", id));
        } catch (BridgeAnalyzerConnectionException exception) {
            return bridgeFailure(exception);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "messageKey", "analyzer.deliveryIssues.error.invalidId"));
        }
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<Map<String, Object>> dismiss(@PathVariable String id, HttpServletRequest request) {
        try {
            deliveryIssueService.dismiss(id, getSysUserId(request));
            return ResponseEntity.ok(Map.of("status", "success", "id", id));
        } catch (BridgeAnalyzerConnectionException exception) {
            return bridgeFailure(exception);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "messageKey", "analyzer.deliveryIssues.error.invalidId"));
        }
    }

    private static ResponseEntity<Map<String, Object>> bridgeFailure(BridgeAnalyzerConnectionException exception) {
        Object bridgeStatus = exception.messageArgs().get("status");
        HttpStatus status = bridgeStatus instanceof Integer code && (code == 404 || code == 409)
                ? HttpStatus.valueOf(code)
                : HttpStatus.BAD_GATEWAY;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "error");
        body.put("messageKey", exception.messageKey());
        body.put("messageArgs", exception.messageArgs());
        return ResponseEntity.status(status).body(body);
    }
}
