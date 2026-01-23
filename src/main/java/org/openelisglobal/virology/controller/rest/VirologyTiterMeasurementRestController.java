package org.openelisglobal.virology.controller.rest;

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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for virology titer measurement logging.
 * Handles viral load quantification (TCID50, PFU/mL, etc.) with array-based history.
 */
@RestController
@RequestMapping("/rest/virology/titer-measurement")
public class VirologyTiterMeasurementRestController extends BaseRestController {

    private static final Log log = LogFactory.getLog(VirologyTiterMeasurementRestController.class);

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    /**
     * Save titer measurement log with assay method and titer values.
     * Records multiple measurements per sample (appends to titerHistory array).
     *
     * @param request Titer measurement log request
     * @return ResponseEntity with success/failure status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveTiterMeasurement(
            HttpServletRequest httpRequest,
            @Valid @RequestBody TiterMeasurementRequest request) {

        try {
            log.info("Saving titer measurement log for notebook page: " + request.getNotebookPageId());
            log.info("Sample IDs: " + (request.getSampleIds() != null ? request.getSampleIds() : "none"));
            log.info("Titer Value: " + request.getTiterValue());
            log.info("Titer Unit: " + request.getTiterUnit());

            // Build titer measurement event to append to titerHistory - simplified
            Map<String, Object> measurementEvent = new HashMap<>();
            measurementEvent.put("measurementDate", request.getMeasurementDate());
            measurementEvent.put("titerValue", request.getTiterValue());
            measurementEvent.put("titerUnit", request.getTiterUnit());
            measurementEvent.put("notes", request.getNotes());
            measurementEvent.put("timestamp", System.currentTimeMillis()); // Add timestamp for ordering

            // Append titer measurement event to history (instead of overwriting)
            int updatedCount = 0;
            if (request.getSampleIds() != null && !request.getSampleIds().isEmpty() && request.getNotebookPageId() != null) {
                log.info("Attempting to update samples. PageId: " + request.getNotebookPageId() + ", SampleIds: " + request.getSampleIds());
                updatedCount = notebookPageSampleService.bulkAppendToArray(
                    request.getNotebookPageId().intValue(),
                    request.getSampleIds(),
                    "titerHistory", // Array field name
                    measurementEvent,
                    getSysUserId(httpRequest)
                );
                log.info("Appended titer measurement to " + updatedCount + " samples");
            } else {
                log.warn("Cannot update samples - missing data. PageId: " + request.getNotebookPageId() +
                         ", SampleIds: " + (request.getSampleIds() != null ? request.getSampleIds().size() : "null"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Titer measurement logged successfully");
            response.put("measurementId", System.currentTimeMillis());
            response.put("samplesUpdated", updatedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error saving titer measurement log", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to save titer measurement log: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * DTO for titer measurement request from frontend - simplified
     */
    @Getter
    @Setter
    public static class TiterMeasurementRequest {
        private Long notebookPageId;
        private List<Integer> sampleIds;
        private String measurementDate;
        private String titerValue;
        private String titerUnit;
        private String notes;
    }
}
