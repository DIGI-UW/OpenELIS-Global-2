package org.openelisglobal.virology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for virology sterilization logging. Handles sterilization
 * parameter logging (autoclaving, filtration) for virology workflows.
 */
@RestController
@RequestMapping("/rest/virology/sterilization")
public class VirologySterilizationRestController extends BaseRestController {

    private static final Log log = LogFactory.getLog(VirologySterilizationRestController.class);

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    /**
     * Save sterilization log with parameters. Records sterilization method,
     * temperature, time, and pressure.
     *
     * @param request Sterilization log request
     * @return ResponseEntity with success/failure status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveSterilization(HttpServletRequest httpRequest,
            @Valid @RequestBody SterilizationRequest request) {

        try {
            log.info("Saving sterilization log for notebook page: " + request.getNotebookPageId());
            log.info("Sample IDs: " + (request.getSampleIds() != null ? request.getSampleIds() : "none"));
            log.info("Sterilization type: " + request.getSterilizationType());
            log.info("Parameters - Temp: " + request.getTemperature() + "°C, Time: " + request.getTime()
                    + " min, Pressure: " + request.getPressure() + " psi");

            // Build sterilization data to store on samples
            Map<String, Object> sterilizationData = new HashMap<>();
            Map<String, Object> sterilization = new HashMap<>();
            sterilization.put("type", request.getSterilizationType());
            sterilization.put("method", request.getSterilizationMethod());
            sterilization.put("temperature", request.getTemperature());
            sterilization.put("time", request.getTime());
            sterilization.put("pressure", request.getPressure());
            sterilization.put("notes", request.getNotes());
            sterilization.put("timestamp", System.currentTimeMillis());

            sterilizationData.put("sterilization", sterilization);

            // Apply sterilization data to selected samples
            int updatedCount = 0;
            if (request.getSampleIds() != null && !request.getSampleIds().isEmpty()
                    && request.getNotebookPageId() != null) {
                log.info("Attempting to update samples. PageId: " + request.getNotebookPageId() + ", SampleIds: "
                        + request.getSampleIds());
                log.info("Sterilization data to apply: " + sterilizationData);
                updatedCount = notebookPageSampleService.bulkApplyData(request.getNotebookPageId().intValue(),
                        request.getSampleIds(), sterilizationData, getSysUserId(httpRequest));
                log.info("Updated " + updatedCount + " samples with sterilization data");

                // Verify the data was saved by reading it back
                for (Integer sampleId : request.getSampleIds()) {
                    var savedSample = notebookPageSampleService
                            .getByPageIdAndSampleItemId(request.getNotebookPageId().intValue(), sampleId);
                    if (savedSample != null) {
                        log.info("Verified sample " + sampleId + " data after save: " + savedSample.getData());
                    } else {
                        log.warn("Sample " + sampleId + " not found after save!");
                    }
                }
            } else {
                log.warn("Cannot update samples - missing data. PageId: " + request.getNotebookPageId()
                        + ", SampleIds: " + (request.getSampleIds() != null ? request.getSampleIds().size() : "null"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sterilization logged successfully");
            response.put("samplesUpdated", updatedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error saving sterilization", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to save sterilization: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * DTO for sterilization request from frontend
     */
    @Getter
    @Setter
    public static class SterilizationRequest {
        private Long notebookPageId;
        private List<Integer> sampleIds;
        private String sterilizationType; // "autoclaving" or "filtration"
        private String sterilizationMethod; // Display name
        private String temperature; // in °C
        private String time; // in minutes
        private String pressure; // in psi (for autoclaving)
        private String notes;
    }
}
