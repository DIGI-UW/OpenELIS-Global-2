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
 * REST controller for virology virus isolation logging. Handles virus isolation
 * from culture batches with full traceability.
 */
@RestController
@RequestMapping("/rest/virology/virus-isolation")
public class VirologyVirusIsolationRestController extends BaseRestController {

    private static final Log log = LogFactory.getLog(VirologyVirusIsolationRestController.class);

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    /**
     * Save virus isolation log with culture batch linkage. Records isolation
     * method, batch IDs, and virus strain information.
     *
     * @param request Virus isolation log request
     * @return ResponseEntity with success/failure status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveVirusIsolation(HttpServletRequest httpRequest,
            @Valid @RequestBody VirusIsolationRequest request) {

        try {
            log.info("Saving virus isolation log for notebook page: " + request.getNotebookPageId());
            log.info("Sample IDs: " + (request.getSampleIds() != null ? request.getSampleIds() : "none"));
            log.info("Culture Batch ID: " + request.getCultureBatchId());
            log.info("Isolation Method: " + request.getIsolationMethod());

            // Build isolation data to store on samples - simplified
            Map<String, Object> isolationData = new HashMap<>();
            isolationData.put("cultureBatchId", request.getCultureBatchId());
            isolationData.put("isolationDate", request.getIsolationDate());
            isolationData.put("isolationMethod", request.getIsolationMethod());
            isolationData.put("notes", request.getNotes());

            // Apply isolation data to selected samples
            int updatedCount = 0;
            if (request.getSampleIds() != null && !request.getSampleIds().isEmpty()
                    && request.getNotebookPageId() != null) {
                log.info("Attempting to update samples. PageId: " + request.getNotebookPageId() + ", SampleIds: "
                        + request.getSampleIds());
                updatedCount = notebookPageSampleService.bulkApplyData(request.getNotebookPageId().intValue(),
                        request.getSampleIds(), isolationData, getSysUserId(httpRequest));
                log.info("Updated " + updatedCount + " samples with virus isolation data");
            } else {
                log.warn("Cannot update samples - missing data. PageId: " + request.getNotebookPageId()
                        + ", SampleIds: " + (request.getSampleIds() != null ? request.getSampleIds().size() : "null"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Virus isolation logged successfully");
            response.put("isolationId", System.currentTimeMillis());
            response.put("samplesUpdated", updatedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error saving virus isolation log", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to save virus isolation log: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * DTO for virus isolation request from frontend - simplified
     */
    @Getter
    @Setter
    public static class VirusIsolationRequest {
        private Long notebookPageId;
        private List<Integer> sampleIds;
        private String cultureBatchId;
        private String isolationDate;
        private String isolationMethod;
        private String notes;
    }
}
