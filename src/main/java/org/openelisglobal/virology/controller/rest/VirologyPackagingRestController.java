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
 * REST controller for virology product packaging logging.
 * Handles final product packaging specifications for virology workflows.
 */
@RestController
@RequestMapping("/rest/virology/packaging")
public class VirologyPackagingRestController extends BaseRestController {

    private static final Log log = LogFactory.getLog(VirologyPackagingRestController.class);

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    /**
     * Save packaging log with product specifications.
     * Records batch ID, vial type, fill volume, labeling information.
     *
     * @param request Packaging log request
     * @return ResponseEntity with success/failure status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> savePackaging(
            HttpServletRequest httpRequest,
            @Valid @RequestBody PackagingRequest request) {

        try {
            log.info("Saving packaging log for notebook page: " + request.getNotebookPageId());
            log.info("Sample IDs: " + (request.getSampleIds() != null ? request.getSampleIds() : "none"));
            log.info("Batch ID: " + request.getBatchId());
            log.info("Vial Type: " + request.getVialType());
            log.info("Fill Volume: " + request.getFillVolume());

            // Build packaging data to store on samples
            Map<String, Object> packagingData = new HashMap<>();
            packagingData.put("batchId", request.getBatchId());
            packagingData.put("vialType", request.getVialType());
            packagingData.put("vialTypeName", request.getVialTypeName());
            packagingData.put("fillVolume", request.getFillVolume());
            packagingData.put("labelingInfo", request.getLabelingInfo());
            packagingData.put("packagingDate", request.getPackagingDate());
            packagingData.put("expiryDate", request.getExpiryDate());
            packagingData.put("storageConditions", request.getStorageConditions());
            packagingData.put("notes", request.getNotes());

            // Apply packaging data to selected samples
            int updatedCount = 0;
            if (request.getSampleIds() != null && !request.getSampleIds().isEmpty() && request.getNotebookPageId() != null) {
                log.info("Attempting to update samples. PageId: " + request.getNotebookPageId() + ", SampleIds: " + request.getSampleIds());
                updatedCount = notebookPageSampleService.bulkApplyData(
                    request.getNotebookPageId().intValue(),
                    request.getSampleIds(),
                    packagingData,
                    getSysUserId(httpRequest)
                );
                log.info("Updated " + updatedCount + " samples with packaging data");
            } else {
                log.warn("Cannot update samples - missing data. PageId: " + request.getNotebookPageId() +
                         ", SampleIds: " + (request.getSampleIds() != null ? request.getSampleIds().size() : "null"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Packaging logged successfully");
            response.put("packagingId", System.currentTimeMillis());
            response.put("samplesUpdated", updatedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error saving packaging log", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to save packaging log: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * DTO for packaging request from frontend
     */
    @Getter
    @Setter
    public static class PackagingRequest {
        private Long notebookPageId;
        private List<Integer> sampleIds;
        private String batchId;
        private String vialType;
        private String vialTypeName;
        private String fillVolume;
        private String labelingInfo;
        private String packagingDate;
        private String expiryDate;
        private String storageConditions;
        private String notes;
    }
}
