package org.openelisglobal.virology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Virology Preclinical and Clinical Trials page.
 * Handles saving trial data for both preclinical (animal) and clinical (human) trials.
 * Uses trialsHistory array pattern to allow multiple trials of both types per sample.
 */
@RestController
@RequestMapping("/rest/virology")
public class VirologyTrialsRestController extends BaseRestController {

    private static final Log log = LogFactory.getLog(VirologyTrialsRestController.class);

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    public static class TrialsRequest {
        public Long notebookPageId;
        public List<Integer> sampleIds;
        public String trialType; // "PRECLINICAL" or "CLINICAL"

        // Common fields
        public String trialInitiationDate;
        public String safetyOutcome;
        public String adverseEvents;
        public String notes;

        // Preclinical-specific fields
        public String animalSpecies;
        public String numberOfAnimals;
        public String studyDesign;
        public String immunogenicityOutcome;

        // Clinical-specific fields
        public String trialPhase;
        public String numberOfParticipants;
        public String primaryEndpoint;
        public String efficacyOutcome;
        public String regulatorySubmission;
        public String regulatoryStatus;
    }

    @PostMapping(value = "/trials", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveTrialData(
            HttpServletRequest httpRequest,
            @RequestBody TrialsRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Saving trial data for notebook page: " + request.notebookPageId);
            log.info("Trial Type: " + request.trialType);
            log.info("Sample IDs: " + (request.sampleIds != null ? request.sampleIds : "none"));

            if (request.notebookPageId == null || request.sampleIds == null || request.sampleIds.isEmpty()) {
                response.put("success", false);
                response.put("error", "Missing required fields: notebookPageId or sampleIds");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.trialType == null || request.trialType.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Trial type is required (PRECLINICAL or CLINICAL)");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.trialInitiationDate == null || request.trialInitiationDate.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Trial initiation date is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Build trial event to append to trialsHistory array
            Map<String, Object> trialEvent = new HashMap<>();
            trialEvent.put("trialType", request.trialType);
            trialEvent.put("trialInitiationDate", request.trialInitiationDate);
            trialEvent.put("safetyOutcome", request.safetyOutcome);
            trialEvent.put("adverseEvents", request.adverseEvents);
            trialEvent.put("notes", request.notes);
            trialEvent.put("timestamp", System.currentTimeMillis()); // Add timestamp for ordering

            if ("PRECLINICAL".equalsIgnoreCase(request.trialType)) {
                // Preclinical (animal) trial data
                if (request.animalSpecies == null || request.animalSpecies.trim().isEmpty()) {
                    response.put("success", false);
                    response.put("error", "Animal species is required for preclinical trials");
                    return ResponseEntity.badRequest().body(response);
                }
                trialEvent.put("animalSpecies", request.animalSpecies);
                trialEvent.put("numberOfAnimals", request.numberOfAnimals);
                trialEvent.put("studyDesign", request.studyDesign);
                trialEvent.put("immunogenicityOutcome", request.immunogenicityOutcome);
            } else if ("CLINICAL".equalsIgnoreCase(request.trialType)) {
                // Clinical (human) trial data
                if (request.trialPhase == null || request.trialPhase.trim().isEmpty()) {
                    response.put("success", false);
                    response.put("error", "Trial phase is required for clinical trials");
                    return ResponseEntity.badRequest().body(response);
                }
                trialEvent.put("trialPhase", request.trialPhase);
                trialEvent.put("numberOfParticipants", request.numberOfParticipants);
                trialEvent.put("primaryEndpoint", request.primaryEndpoint);
                trialEvent.put("efficacyOutcome", request.efficacyOutcome);
                trialEvent.put("regulatorySubmission", request.regulatorySubmission);
                trialEvent.put("regulatoryStatus", request.regulatoryStatus);
            } else {
                response.put("success", false);
                response.put("error", "Invalid trial type. Must be PRECLINICAL or CLINICAL");
                return ResponseEntity.badRequest().body(response);
            }

            // Append trial event to trialsHistory array (allows multiple trials per sample)
            int updatedCount = notebookPageSampleService.bulkAppendToArray(
                request.notebookPageId.intValue(),
                request.sampleIds,
                "trialsHistory", // Array field name
                trialEvent,
                getSysUserId(httpRequest)
            );

            log.info("Appended trial event to " + updatedCount + " samples");

            response.put("success", true);
            response.put("samplesUpdated", updatedCount);
            response.put("message", request.trialType + " trial data saved successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error saving trial data", e);
            response.put("success", false);
            response.put("error", "Failed to save trial data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
