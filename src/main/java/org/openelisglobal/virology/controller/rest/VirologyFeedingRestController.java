package org.openelisglobal.virology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
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
 * REST controller for virology culture feeding logging. Handles feeding
 * schedule and reagent traceability logging for culture maintenance in virology
 * workflows.
 */
@RestController
@RequestMapping("/rest/virology/feeding")
public class VirologyFeedingRestController extends BaseRestController {

    private static final Log log = LogFactory.getLog(VirologyFeedingRestController.class);

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    /**
     * Save feeding log with full material traceability. Records feeding schedule,
     * reagents used with lot numbers and expiry dates.
     *
     * @param request Feeding log request
     * @return ResponseEntity with success/failure status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveFeeding(HttpServletRequest httpRequest,
            @Valid @RequestBody FeedingRequest request) {

        try {
            log.info("Saving feeding log for notebook page: " + request.getNotebookPageId());
            log.info("Sample IDs: " + (request.getSampleIds() != null ? request.getSampleIds() : "none"));
            log.info("Feeding Date: " + request.getFeedingDate());
            log.info("Feeding Schedule: " + request.getFeedingSchedule());
            log.info("Reagents: " + (request.getReagents() != null ? request.getReagents().size() : 0));

            // Build feeding event to append to feeding history
            Map<String, Object> feedingEvent = new HashMap<>();
            feedingEvent.put("feedingDate", request.getFeedingDate());
            feedingEvent.put("feedingTime", request.getFeedingTime());
            feedingEvent.put("feedingSchedule", request.getFeedingSchedule());
            feedingEvent.put("batchNumber", request.getBatchNumber());
            feedingEvent.put("volumeUsed", request.getVolumeUsed());
            feedingEvent.put("notes", request.getNotes());
            feedingEvent.put("timestamp", System.currentTimeMillis()); // Add timestamp for ordering

            // Convert reagents to a list of maps for JSONB storage
            if (request.getReagents() != null && !request.getReagents().isEmpty()) {
                List<Map<String, Object>> reagentsList = new ArrayList<>();
                for (ReagentDTO reagent : request.getReagents()) {
                    Map<String, Object> reagentMap = new HashMap<>();
                    reagentMap.put("reagentId", reagent.getReagentId());
                    reagentMap.put("name", reagent.getName());
                    reagentMap.put("category", reagent.getCategory());
                    reagentMap.put("lotNumber", reagent.getLotNumber());
                    reagentMap.put("expiryDate", reagent.getExpiryDate());
                    reagentMap.put("volumeUsed", reagent.getVolumeUsed());
                    reagentsList.add(reagentMap);
                }
                feedingEvent.put("reagents", reagentsList);
            }

            // Append feeding event to history (instead of overwriting)
            int updatedCount = 0;
            if (request.getSampleIds() != null && !request.getSampleIds().isEmpty()
                    && request.getNotebookPageId() != null) {
                log.info("Attempting to update samples. PageId: " + request.getNotebookPageId() + ", SampleIds: "
                        + request.getSampleIds());
                updatedCount = notebookPageSampleService.bulkAppendToArray(request.getNotebookPageId().intValue(),
                        request.getSampleIds(), "feedingHistory", // Array field name
                        feedingEvent, getSysUserId(httpRequest));
                log.info("Appended feeding event to " + updatedCount + " samples");
            } else {
                log.warn("Cannot update samples - missing data. PageId: " + request.getNotebookPageId()
                        + ", SampleIds: " + (request.getSampleIds() != null ? request.getSampleIds().size() : "null"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Feeding logged successfully");
            response.put("feedingId", System.currentTimeMillis());
            response.put("samplesUpdated", updatedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error saving feeding log", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to save feeding log: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * DTO for feeding request from frontend
     */
    @Getter
    @Setter
    public static class FeedingRequest {
        private Long notebookPageId;
        private List<Integer> sampleIds;
        private String feedingDate;
        private String feedingTime;
        private String feedingSchedule;
        private String batchNumber;
        private String volumeUsed;
        private List<ReagentDTO> reagents;
        private String notes;
    }

    /**
     * DTO for reagent information
     */
    @Getter
    @Setter
    public static class ReagentDTO {
        private Long reagentId;
        private String name;
        private String category;
        private String lotNumber;
        private String expiryDate;
        private String volumeUsed;
    }
}
