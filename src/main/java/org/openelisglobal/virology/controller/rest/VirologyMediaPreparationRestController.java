package org.openelisglobal.virology.controller.rest;

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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for virology media preparation logging.
 * Handles material traceability logging for media preparation in virology workflows.
 */
@RestController
@RequestMapping("/rest/virology/media-preparation")
public class VirologyMediaPreparationRestController extends BaseRestController {

    private static final Log log = LogFactory.getLog(VirologyMediaPreparationRestController.class);

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    /**
     * Save media preparation log with full material traceability.
     * Records media, reagents, equipment used with lot numbers and expiry dates.
     *
     * @param request Media preparation log request
     * @return ResponseEntity with success/failure status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveMediaPreparation(
            HttpServletRequest httpRequest,
            @Valid @RequestBody MediaPreparationRequest request) {

        try {
            log.info("Saving media preparation log for notebook page: " + request.getNotebookPageId());
            log.info("Sample IDs: " + (request.getSampleIds() != null ? request.getSampleIds() : "none"));
            log.info("Media: " + request.getMediaName() + " (ID: " + request.getMediaId() + ")");
            log.info("Reagents: " + (request.getReagents() != null ? request.getReagents().size() : 0));
            log.info("Equipment: " + request.getEquipmentName());

            // Build media preparation data to store on samples
            Map<String, Object> mediaData = new HashMap<>();
            mediaData.put("mediaId", request.getMediaId());
            mediaData.put("mediaName", request.getMediaName());
            mediaData.put("mediaLotNumber", request.getMediaLotNumber());
            mediaData.put("mediaExpiryDate", request.getMediaExpiryDate());
            mediaData.put("batchNumber", request.getBatchNumber());
            mediaData.put("preparationNotes", request.getPreparationNotes());
            mediaData.put("equipmentId", request.getEquipmentId());
            mediaData.put("equipmentName", request.getEquipmentName());
            mediaData.put("equipmentSerialNumber", request.getEquipmentSerialNumber());

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
                    reagentsList.add(reagentMap);
                }
                mediaData.put("reagents", reagentsList);
            }

            // Apply media preparation data to selected samples
            int updatedCount = 0;
            if (request.getSampleIds() != null && !request.getSampleIds().isEmpty() && request.getNotebookPageId() != null) {
                log.info("Attempting to update samples. PageId: " + request.getNotebookPageId() + ", SampleIds: " + request.getSampleIds());
                updatedCount = notebookPageSampleService.bulkApplyData(
                    request.getNotebookPageId().intValue(),
                    request.getSampleIds(),
                    mediaData,
                    getSysUserId(httpRequest)
                );
                log.info("Updated " + updatedCount + " samples with media preparation data");
            } else {
                log.warn("Cannot update samples - missing data. PageId: " + request.getNotebookPageId() +
                         ", SampleIds: " + (request.getSampleIds() != null ? request.getSampleIds().size() : "null"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Media preparation logged successfully");
            response.put("mediaPreparationId", System.currentTimeMillis());
            response.put("samplesUpdated", updatedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error saving media preparation", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to save media preparation: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * DTO for media preparation request from frontend
     */
    @Getter
    @Setter
    public static class MediaPreparationRequest {
        private Long notebookPageId;
        private List<Integer> sampleIds;
        private Long mediaId;
        private String mediaName;
        private String mediaLotNumber;
        private String mediaExpiryDate;
        private List<ReagentDTO> reagents;
        private Long equipmentId;
        private String equipmentName;
        private String equipmentSerialNumber;
        private String batchNumber;
        private String preparationNotes;
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
    }
}
