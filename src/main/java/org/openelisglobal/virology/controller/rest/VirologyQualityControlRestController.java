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
 * REST controller for logging quality control data in virology workflow.
 * Handles POST requests to save viability percentage, sterility results, and QC
 * notes.
 */
@Slf4j
@RestController
@RequestMapping("/rest/virology/quality-control")
public class VirologyQualityControlRestController extends BaseRestController {

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    /**
     * DTO for quality control request payload
     */
    public static class QualityControlRequest {
        private Integer notebookPageId;
        private List<Integer> sampleIds;
        private Double viabilityPercentage;
        private String sterilityResult;
        private String testMethod;
        private String acceptanceCriteria;
        private String deviationNotes;
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

        public Double getViabilityPercentage() {
            return viabilityPercentage;
        }

        public void setViabilityPercentage(Double viabilityPercentage) {
            this.viabilityPercentage = viabilityPercentage;
        }

        public String getSterilityResult() {
            return sterilityResult;
        }

        public void setSterilityResult(String sterilityResult) {
            this.sterilityResult = sterilityResult;
        }

        public String getTestMethod() {
            return testMethod;
        }

        public void setTestMethod(String testMethod) {
            this.testMethod = testMethod;
        }

        public String getAcceptanceCriteria() {
            return acceptanceCriteria;
        }

        public void setAcceptanceCriteria(String acceptanceCriteria) {
            this.acceptanceCriteria = acceptanceCriteria;
        }

        public String getDeviationNotes() {
            return deviationNotes;
        }

        public void setDeviationNotes(String deviationNotes) {
            this.deviationNotes = deviationNotes;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * Save quality control data for selected samples. Updates the JSONB data column
     * for each sample with QC parameters.
     *
     * @param httpRequest HTTP servlet request
     * @param request     Quality control request containing sample IDs and QC data
     * @return ResponseEntity with success/failure status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveQualityControlData(HttpServletRequest httpRequest,
            @RequestBody QualityControlRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Received quality control request for {} samples",
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

            if (request.getViabilityPercentage() == null) {
                response.put("success", false);
                response.put("error", "Viability percentage is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getViabilityPercentage() < 0 || request.getViabilityPercentage() > 100) {
                response.put("success", false);
                response.put("error", "Viability percentage must be between 0 and 100");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getSterilityResult() == null || request.getSterilityResult().trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Sterility result is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (!request.getSterilityResult().equals("Pass") && !request.getSterilityResult().equals("Fail")) {
                response.put("success", false);
                response.put("error", "Sterility result must be 'Pass' or 'Fail'");
                return ResponseEntity.badRequest().body(response);
            }

            // Build quality control data map
            Map<String, Object> qcData = new HashMap<>();
            qcData.put("viabilityPercentage", request.getViabilityPercentage());
            qcData.put("sterilityResult", request.getSterilityResult());

            if (request.getTestMethod() != null && !request.getTestMethod().trim().isEmpty()) {
                qcData.put("testMethod", request.getTestMethod());
            }

            if (request.getAcceptanceCriteria() != null && !request.getAcceptanceCriteria().trim().isEmpty()) {
                qcData.put("acceptanceCriteria", request.getAcceptanceCriteria());
            }

            if (request.getDeviationNotes() != null && !request.getDeviationNotes().trim().isEmpty()) {
                qcData.put("deviationNotes", request.getDeviationNotes());
            }

            if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                qcData.put("qcNotes", request.getNotes());
            }

            log.info("Quality control data to apply: {}", qcData);

            // Apply data to all selected samples
            int updatedCount = notebookPageSampleService.bulkApplyData(request.getNotebookPageId(),
                    request.getSampleIds(), qcData, getSysUserId(httpRequest));

            log.info("Successfully updated {} samples with quality control data", updatedCount);

            response.put("success", true);
            response.put("samplesUpdated", updatedCount);
            response.put("message", "Quality control data saved successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error saving quality control data", e);
            response.put("success", false);
            response.put("error", "An error occurred while saving quality control data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
