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
 * REST controller for logging formulation data in virology workflow. Handles
 * POST requests to save formulation details (stabilizers, preservatives,
 * concentrations).
 */
@Slf4j
@RestController
@RequestMapping("/rest/virology/formulation")
public class VirologyFormulationRestController extends BaseRestController {

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    /**
     * DTO for formulation request payload
     */
    public static class FormulationRequest {
        private Integer notebookPageId;
        private List<Integer> sampleIds;
        private String batchNumber;
        private String stabilizers;
        private String preservatives;
        private String virusConcentration;
        private String bufferComposition;
        private String formulationNotes;

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

        public String getBatchNumber() {
            return batchNumber;
        }

        public void setBatchNumber(String batchNumber) {
            this.batchNumber = batchNumber;
        }

        public String getStabilizers() {
            return stabilizers;
        }

        public void setStabilizers(String stabilizers) {
            this.stabilizers = stabilizers;
        }

        public String getPreservatives() {
            return preservatives;
        }

        public void setPreservatives(String preservatives) {
            this.preservatives = preservatives;
        }

        public String getVirusConcentration() {
            return virusConcentration;
        }

        public void setVirusConcentration(String virusConcentration) {
            this.virusConcentration = virusConcentration;
        }

        public String getBufferComposition() {
            return bufferComposition;
        }

        public void setBufferComposition(String bufferComposition) {
            this.bufferComposition = bufferComposition;
        }

        public String getFormulationNotes() {
            return formulationNotes;
        }

        public void setFormulationNotes(String formulationNotes) {
            this.formulationNotes = formulationNotes;
        }
    }

    /**
     * Save formulation data for selected samples. Updates the JSONB data column for
     * each sample with formulation parameters.
     *
     * @param httpRequest HTTP servlet request
     * @param request     Formulation request containing sample IDs and formulation
     *                    data
     * @return ResponseEntity with success/failure status
     */
    @PostMapping(value = "/save", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveFormulationData(HttpServletRequest httpRequest,
            @RequestBody FormulationRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Received formulation request for {} samples",
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

            // Build formulation data map
            Map<String, Object> formulationData = new HashMap<>();

            if (request.getBatchNumber() != null && !request.getBatchNumber().trim().isEmpty()) {
                formulationData.put("batchNumber", request.getBatchNumber().trim());
            }

            if (request.getStabilizers() != null && !request.getStabilizers().trim().isEmpty()) {
                formulationData.put("stabilizers", request.getStabilizers().trim());
            }

            if (request.getPreservatives() != null && !request.getPreservatives().trim().isEmpty()) {
                formulationData.put("preservatives", request.getPreservatives().trim());
            }

            if (request.getVirusConcentration() != null && !request.getVirusConcentration().trim().isEmpty()) {
                formulationData.put("virusConcentration", request.getVirusConcentration().trim());
            }

            if (request.getBufferComposition() != null && !request.getBufferComposition().trim().isEmpty()) {
                formulationData.put("bufferComposition", request.getBufferComposition().trim());
            }

            if (request.getFormulationNotes() != null && !request.getFormulationNotes().trim().isEmpty()) {
                formulationData.put("formulationNotes", request.getFormulationNotes().trim());
            }

            log.info("Formulation data to apply: {}", formulationData);

            // Apply data to all selected samples
            int updatedCount = notebookPageSampleService.bulkApplyData(request.getNotebookPageId(),
                    request.getSampleIds(), formulationData, getSysUserId(httpRequest));

            log.info("Successfully updated {} samples with formulation data", updatedCount);

            // Update sample status to IN_PROGRESS
            int statusUpdatedCount = notebookPageSampleService.bulkUpdateStatus(request.getNotebookPageId(),
                    request.getSampleIds(),
                    org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.IN_PROGRESS,
                    getSysUserId(httpRequest));

            log.info("Updated status to IN_PROGRESS for {} samples", statusUpdatedCount);

            response.put("success", true);
            response.put("samplesUpdated", updatedCount);
            response.put("message", "Formulation data saved successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error saving formulation data", e);
            response.put("success", false);
            response.put("error", "An error occurred while saving formulation data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Complete formulation for selected samples. Updates the sample status to
     * COMPLETED.
     *
     * @param httpRequest HTTP servlet request
     * @param request     Formulation request containing sample IDs
     * @return ResponseEntity with success/failure status
     */
    @PostMapping(value = "/complete", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> completeFormulation(HttpServletRequest httpRequest,
            @RequestBody FormulationRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Received complete formulation request for {} samples",
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

            // Update sample status to COMPLETED
            int statusUpdatedCount = notebookPageSampleService.bulkUpdateStatus(request.getNotebookPageId(),
                    request.getSampleIds(), org.openelisglobal.notebook.valueholder.NotebookPageSample.Status.COMPLETED,
                    getSysUserId(httpRequest));

            log.info("Updated status to COMPLETED for {} samples", statusUpdatedCount);

            response.put("success", true);
            response.put("samplesUpdated", statusUpdatedCount);
            response.put("message", "Formulation completed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error completing formulation", e);
            response.put("success", false);
            response.put("error", "An error occurred while completing formulation: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
