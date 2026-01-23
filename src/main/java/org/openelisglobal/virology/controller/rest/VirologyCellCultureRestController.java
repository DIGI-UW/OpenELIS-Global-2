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
 * REST controller for logging cell culture data in virology workflow. Handles
 * POST requests to save cell line, passage number, and growth conditions.
 */
@Slf4j
@RestController
@RequestMapping("/rest/virology/cell-culture")
public class VirologyCellCultureRestController extends BaseRestController {

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    /**
     * DTO for cell culture request payload
     */
    public static class CellCultureRequest {
        private Integer notebookPageId;
        private List<Integer> sampleIds;
        private String cellLine;
        private Integer passageNumber;
        private String seedingDensity;
        private String flaskType;
        private Double mediaVolume;
        private Double incubationTemp;
        private Double co2Percentage;
        private Double humidityPercentage;
        private String seedingDate;
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

        public String getCellLine() {
            return cellLine;
        }

        public void setCellLine(String cellLine) {
            this.cellLine = cellLine;
        }

        public Integer getPassageNumber() {
            return passageNumber;
        }

        public void setPassageNumber(Integer passageNumber) {
            this.passageNumber = passageNumber;
        }

        public String getSeedingDensity() {
            return seedingDensity;
        }

        public void setSeedingDensity(String seedingDensity) {
            this.seedingDensity = seedingDensity;
        }

        public String getFlaskType() {
            return flaskType;
        }

        public void setFlaskType(String flaskType) {
            this.flaskType = flaskType;
        }

        public Double getMediaVolume() {
            return mediaVolume;
        }

        public void setMediaVolume(Double mediaVolume) {
            this.mediaVolume = mediaVolume;
        }

        public Double getIncubationTemp() {
            return incubationTemp;
        }

        public void setIncubationTemp(Double incubationTemp) {
            this.incubationTemp = incubationTemp;
        }

        public Double getCo2Percentage() {
            return co2Percentage;
        }

        public void setCo2Percentage(Double co2Percentage) {
            this.co2Percentage = co2Percentage;
        }

        public Double getHumidityPercentage() {
            return humidityPercentage;
        }

        public void setHumidityPercentage(Double humidityPercentage) {
            this.humidityPercentage = humidityPercentage;
        }

        public String getSeedingDate() {
            return seedingDate;
        }

        public void setSeedingDate(String seedingDate) {
            this.seedingDate = seedingDate;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * Save cell culture data for selected samples. Updates the JSONB data column
     * for each sample with cell culture parameters.
     *
     * @param httpRequest HTTP servlet request
     * @param request     Cell culture request containing sample IDs and culture
     *                    data
     * @return ResponseEntity with success/failure status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveCellCultureData(HttpServletRequest httpRequest,
            @RequestBody CellCultureRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Received cell culture request for {} samples",
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

            if (request.getCellLine() == null || request.getCellLine().trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Cell line is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getPassageNumber() == null) {
                response.put("success", false);
                response.put("error", "Passage number is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Build cell culture data map
            Map<String, Object> cellCultureData = new HashMap<>();
            cellCultureData.put("cellLine", request.getCellLine());
            cellCultureData.put("passageNumber", request.getPassageNumber());

            if (request.getSeedingDensity() != null && !request.getSeedingDensity().trim().isEmpty()) {
                cellCultureData.put("seedingDensity", request.getSeedingDensity());
            }

            if (request.getFlaskType() != null && !request.getFlaskType().trim().isEmpty()) {
                cellCultureData.put("flaskType", request.getFlaskType());
            }

            if (request.getMediaVolume() != null) {
                cellCultureData.put("mediaVolume", request.getMediaVolume());
            }

            if (request.getIncubationTemp() != null) {
                cellCultureData.put("incubationTemp", request.getIncubationTemp());
            }

            if (request.getCo2Percentage() != null) {
                cellCultureData.put("co2Percentage", request.getCo2Percentage());
            }

            if (request.getHumidityPercentage() != null) {
                cellCultureData.put("humidityPercentage", request.getHumidityPercentage());
            }

            if (request.getSeedingDate() != null && !request.getSeedingDate().trim().isEmpty()) {
                cellCultureData.put("seedingDate", request.getSeedingDate());
            }

            if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                cellCultureData.put("cellCultureNotes", request.getNotes());
            }

            log.info("Cell culture data to apply: {}", cellCultureData);

            // Apply data to all selected samples
            int updatedCount = notebookPageSampleService.bulkApplyData(request.getNotebookPageId(),
                    request.getSampleIds(), cellCultureData, getSysUserId(httpRequest));

            log.info("Successfully updated {} samples with cell culture data", updatedCount);

            response.put("success", true);
            response.put("samplesUpdated", updatedCount);
            response.put("message", "Cell culture data saved successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error saving cell culture data", e);
            response.put("success", false);
            response.put("error", "An error occurred while saving cell culture data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
