package org.openelisglobal.virology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for logging virus culture data in virology workflow.
 * Handles POST requests to save virus strain, culture conditions, and culture notes.
 */
@Slf4j
@RestController
@RequestMapping("/rest/virology/virus-culture")
public class VirologyVirusCultureRestController extends BaseRestController {

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    /**
     * DTO for virus culture request payload
     */
    public static class VirusCultureRequest {
        private Integer notebookPageId;
        private List<Integer> sampleIds;
        private String virusStrain;
        private Double temperature;
        private Double co2Percentage;
        private Integer durationHours;
        private String notes;

        // Getters and setters
        public Integer getNotebookPageId() {
            return notebookPageId;
        }

        public void setNotebookPageId(Integer notebookPageId) {
            this.notebookPageId = notebookPageId;
        }

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public String getVirusStrain() {
            return virusStrain;
        }

        public void setVirusStrain(String virusStrain) {
            this.virusStrain = virusStrain;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Double getCo2Percentage() {
            return co2Percentage;
        }

        public void setCo2Percentage(Double co2Percentage) {
            this.co2Percentage = co2Percentage;
        }

        public Integer getDurationHours() {
            return durationHours;
        }

        public void setDurationHours(Integer durationHours) {
            this.durationHours = durationHours;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * Save virus culture data for selected samples.
     * Updates the JSONB data column for each sample with virus culture parameters.
     *
     * @param httpRequest HTTP servlet request
     * @param request Virus culture request containing sample IDs and culture data
     * @return ResponseEntity with success/failure status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveVirusCultureData(
            HttpServletRequest httpRequest,
            @RequestBody VirusCultureRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Received virus culture request for {} samples",
                    request.getSampleIds() != null ? request.getSampleIds().size() : 0);

            // Validation
            if (request.getNotebookPageId() == null) {
                response.put("success", false);
                response.put("error", "Notebook page ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
                response.put("success", false);
                response.put("error", "At least one sample ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getVirusStrain() == null || request.getVirusStrain().trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Virus strain is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getTemperature() != null && (request.getTemperature() < -80 || request.getTemperature() > 50)) {
                response.put("success", false);
                response.put("error", "Temperature must be between -80°C and 50°C");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getCo2Percentage() != null && (request.getCo2Percentage() < 0 || request.getCo2Percentage() > 100)) {
                response.put("success", false);
                response.put("error", "CO₂ percentage must be between 0 and 100");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getDurationHours() != null && request.getDurationHours() < 0) {
                response.put("success", false);
                response.put("error", "Duration must be a positive number");
                return ResponseEntity.badRequest().body(response);
            }

            // Build virus culture data map
            Map<String, Object> cultureData = new HashMap<>();
            cultureData.put("virusStrain", request.getVirusStrain());

            if (request.getTemperature() != null) {
                cultureData.put("temperature", request.getTemperature());
            }

            if (request.getCo2Percentage() != null) {
                cultureData.put("co2Percentage", request.getCo2Percentage());
            }

            if (request.getDurationHours() != null) {
                cultureData.put("durationHours", request.getDurationHours());
            }

            if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                cultureData.put("cultureNotes", request.getNotes());
            }

            log.info("Virus culture data to apply: {}", cultureData);

            // Apply data to all selected samples
            int updatedCount = notebookPageSampleService.bulkApplyData(
                    request.getNotebookPageId(),
                    request.getSampleIds(),
                    cultureData,
                    getSysUserId(httpRequest));

            log.info("Successfully updated {} samples with virus culture data", updatedCount);

            // Update sample status to IN_PROGRESS
            int statusUpdatedCount = notebookPageSampleService.bulkUpdateStatus(
                    request.getNotebookPageId(),
                    request.getSampleIds(),
                    org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.IN_PROGRESS,
                    getSysUserId(httpRequest));

            log.info("Updated status to IN_PROGRESS for {} samples", statusUpdatedCount);

            response.put("success", true);
            response.put("samplesUpdated", updatedCount);
            response.put("message", "Virus culture data saved successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error saving virus culture data", e);
            response.put("success", false);
            response.put("error", "An error occurred while saving virus culture data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
