package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerPendingCodeService;
import org.openelisglobal.analyzer.service.AnalyzerPluginConfigService;
import org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/analyzer")
public class AnalyzerPluginConfigRestController extends BaseRestController {

    @Autowired
    private AnalyzerPluginConfigService analyzerPluginConfigService;

    @Autowired
    private AnalyzerPendingCodeService analyzerPendingCodeService;

    @GetMapping("/analyzers/{analyzerId}/plugin-config")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Map<String, Object>> getPluginConfig(@PathVariable String analyzerId,
            HttpServletRequest request) {
        try {
            analyzerPluginConfigService.getOrCreate(analyzerId, getSysUserId(request));
            return ResponseEntity.ok(analyzerPluginConfigService.getConfigAsMap(analyzerId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError("Failed to load plugin config: " + e.getMessage()));
        }
    }

    @PutMapping("/analyzers/{analyzerId}/plugin-config")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Map<String, Object>> updatePluginConfig(@PathVariable String analyzerId,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            analyzerPluginConfigService.upsert(analyzerId, body, getSysUserId(request));
            return ResponseEntity.ok(analyzerPluginConfigService.getConfigAsMap(analyzerId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AnalyzerControllerHelper.wrapError("Failed to update plugin config: " + e.getMessage()));
        }
    }

    @GetMapping("/analyzers/{analyzerId}/pending-codes")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getPendingCodes(@PathVariable String analyzerId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AnalyzerPendingCode code : analyzerPendingCodeService.findByAnalyzerId(analyzerId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", code.getId());
            row.put("analyzerId", code.getAnalyzerId());
            row.put("analyzerTestName", code.getAnalyzerTestName());
            row.put("firstSeenAt", code.getFirstSeenAt());
            row.put("lastSeenAt", code.getLastSeenAt());
            row.put("seenCount", code.getSeenCount());
            row.put("samplePayload", code.getSamplePayload());
            row.put("status", code.getStatus().name());
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/analyzers/{analyzerId}/pending-codes/{pendingCodeId}/status")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Map<String, Object>> updatePendingCodeStatus(@PathVariable String analyzerId,
            @PathVariable String pendingCodeId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            String requested = String.valueOf(body.get("status"));
            AnalyzerPendingCode.Status status = AnalyzerPendingCode.Status.valueOf(requested);
            AnalyzerPendingCode updated = analyzerPendingCodeService.updateStatus(pendingCodeId, status,
                    getSysUserId(request));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", updated.getId());
            response.put("analyzerId", updated.getAnalyzerId());
            response.put("status", updated.getStatus().name());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AnalyzerControllerHelper.wrapError("Invalid pending code status"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError("Failed to update pending code: " + e.getMessage()));
        }
    }
}
