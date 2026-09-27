package org.openelisglobal.analyzer.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerEventPersistenceService;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Failed microbiology analyzer events; held results use the analyzer results
 * worklist.
 */
@RestController
@RequestMapping("/rest/analyzer")
@PreAuthorize("hasAnyRole('ADMIN', 'ANALYSER_IMPORT')")
public class ImportIssuesRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(ImportIssuesRestController.class);

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    @Autowired
    private AnalyzerEventPersistenceService analyzerEventPersistenceService;

    @GetMapping("/import-issues")
    public ResponseEntity<Map<String, Object>> getImportIssues(
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        try {
            int safeLimit = Math.max(1, Math.min(limit == null ? DEFAULT_LIMIT : limit, MAX_LIMIT));
            List<Map<String, Object>> events = analyzerEventPersistenceService.getFailed(safeLimit).stream()
                    .map(this::eventToMap).toList();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", events.size());
            data.put("limit", safeLimit);
            data.put("eventRows", events);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving import issues", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    private Map<String, Object> eventToMap(AnalyzerEvent event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", event.getId());
        row.put("externalEventId", event.getExternalEventId());
        row.put("eventType", event.getEventType());
        row.put("analyzerId", event.getAnalyzerId());
        row.put("sourceId", event.getSourceId());
        row.put("targetReference", event.getTargetReference());
        row.put("failureReason", event.getFailureReason());
        row.put("receivedAt", event.getReceivedAt());
        row.put("reconciliationUrl", "/AnalyzerResults?view=import-issues");
        return row;
    }
}
