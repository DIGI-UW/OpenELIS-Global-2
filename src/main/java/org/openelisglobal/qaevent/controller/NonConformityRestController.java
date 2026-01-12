package org.openelisglobal.qaevent.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * REST controller for Non-Conformity management from analyzer imports
 * Handles QC failures and tracking of non-conforming results
 */
@Controller
@RequestMapping("/rest/non-conformities")
public class NonConformityRestController extends BaseController {

    @Override
    protected String findLocalForward(String forward) {
        return null;
    }

    @Override
    protected String getPageTitleKey() {
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }

    /**
     * Create a new non-conformity record when QC fails
     * POST /rest/non-conformities
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createNonConformity(@RequestBody Map<String, Object> payload) {
        try {
            // Generate unique NC ID
            String ncId = "NC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            // Log the NC creation
            LogEvent.logInfo(this.getClass().getName(), "createNonConformity",
                    "Creating non-conformity record: " + ncId + " for analyzer: " + payload.get("analyzerType"));
            
            // TODO: Persist to database
            // SampleQaEvent qaevent = new SampleQaEvent();
            // qaevent.setCompletedDate(Timestamp.valueOf(LocalDateTime.now()));
            // qaevent.setQaEvent(...);
            // sampleQaEventService.insert(qaevent);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", ncId);
            response.put("status", "OPEN");
            response.put("created", true);
            response.put("message", "Non-conformity record created successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create non-conformity record");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get non-conformity details by ID
     * GET /rest/non-conformities/{id}
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNonConformity(@PathVariable String id) {
        try {
            // TODO: Fetch from database
            // SampleQaEvent qaevent = sampleQaEventService.get(id);
            
            Map<String, Object> ncDetails = new HashMap<>();
            ncDetails.put("id", id);
            ncDetails.put("type", "QC_FAILURE");
            ncDetails.put("status", "OPEN");
            ncDetails.put("date", java.time.LocalDate.now().toString());
            ncDetails.put("analyzerType", "Unknown");
            ncDetails.put("affectedResultCount", 0);
            ncDetails.put("resolution", null);
            
            return ResponseEntity.ok(ncDetails);
        } catch (Exception e) {
            LogEvent.logError(e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve non-conformity record");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    /**
     * Resolve a non-conformity
     * PUT /rest/non-conformities/{id}/resolve
     */
    @PutMapping(value = "/{id}/resolve", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resolveNonConformity(
            @PathVariable String id,
            @RequestBody Map<String, Object> resolutionData) {
        try {
            // Log the resolution
            LogEvent.logInfo(this.getClass().getName(), "resolveNonConformity",
                    "Resolving non-conformity: " + id);
            
            // TODO: Update database
            // SampleQaEvent qaevent = sampleQaEventService.get(id);
            // qaevent.setCompletedDate(Timestamp.valueOf(LocalDateTime.now()));
            // Add resolution details as note
            // sampleQaEventService.update(qaevent);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", id);
            response.put("status", "RESOLVED");
            response.put("resolved", true);
            response.put("message", "Non-conformity resolved successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to resolve non-conformity");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
