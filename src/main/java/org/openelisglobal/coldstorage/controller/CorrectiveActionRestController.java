package org.openelisglobal.coldstorage.controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import org.openelisglobal.coldstorage.service.CorrectiveActionService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.CorrectiveAction;
import org.openelisglobal.coldstorage.valueholder.CorrectiveActionStatus;
import org.openelisglobal.coldstorage.valueholder.CorrectiveActionType;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/coldstorage/corrective-actions")
public class CorrectiveActionRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(CorrectiveActionRestController.class);

    @Autowired
    private CorrectiveActionService correctiveActionService;

    @Autowired
    private FreezerService freezerService;

    @PostMapping
    public ResponseEntity<CorrectiveActionDTO> createCorrectiveAction(
            @RequestBody CreateCorrectiveActionRequest request) {

        try {
            Optional<Freezer> freezerOpt = freezerService.findById(request.getFreezerId());
            if (freezerOpt.isEmpty()) {
                logger.error("Freezer not found with ID: {}", request.getFreezerId());
                return ResponseEntity.badRequest().build();
            }

            CorrectiveActionType actionType = CorrectiveActionType.valueOf(request.getActionType());
            CorrectiveAction createdAction = correctiveActionService.createCorrectiveAction(request.getFreezerId(),
                    actionType, request.getDescription(), request.getCreatedByUserId());

            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(createdAction));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid action type: {}", request.getActionType(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating corrective action", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<CorrectiveActionDTO>> getAllCorrectiveActions(
            @RequestParam(required = false) Long freezerId, @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {

        try {
            List<CorrectiveAction> actions;

            if (freezerId != null) {
                actions = correctiveActionService.getCorrectiveActionsByFreezerId(freezerId);
            } else if (status != null) {
                CorrectiveActionStatus statusEnum = CorrectiveActionStatus.valueOf(status);
                actions = correctiveActionService.getCorrectiveActionsByStatus(statusEnum);
            } else if (startDate != null && endDate != null) {
                OffsetDateTime start = OffsetDateTime.parse(startDate);
                OffsetDateTime end = OffsetDateTime.parse(endDate);
                actions = correctiveActionService.getCorrectiveActionsByDateRange(start, end);
            } else {
                actions = correctiveActionService.getAllCorrectiveActions();
            }

            List<CorrectiveActionDTO> actionDTOs = actions.stream().map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(actionDTOs);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid filter parameter", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error retrieving corrective actions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CorrectiveActionDTO> getCorrectiveActionById(@PathVariable Long id) {
        try {
            CorrectiveAction action = correctiveActionService.get(id);
            if (action == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(convertToDTO(action));
        } catch (Exception e) {
            logger.error("Error retrieving corrective action {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CorrectiveActionDTO> updateCorrectiveAction(@PathVariable Long id,
            @RequestBody UpdateCorrectiveActionRequest request) {

        try {
            CorrectiveAction action = correctiveActionService.get(id);
            if (action == null) {
                return ResponseEntity.notFound().build();
            }

            if (request.getUpdatedByUserId() == null) {
                logger.error("updatedByUserId is required");
                return ResponseEntity.badRequest().build();
            }

            if (request.getDescription() != null && !request.getDescription().isEmpty()) {
                action = correctiveActionService.updateCorrectiveActionDescription(id, request.getDescription(),
                        request.getUpdatedByUserId());
            }

            if (request.getStatus() != null && !request.getStatus().isEmpty()) {
                CorrectiveActionStatus newStatus = CorrectiveActionStatus.valueOf(request.getStatus());
                action = correctiveActionService.updateCorrectiveActionStatus(id, newStatus,
                        request.getUpdatedByUserId());
            }

            return ResponseEntity.ok(convertToDTO(action));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid status: {}", request.getStatus(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error updating corrective action {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<CorrectiveActionDTO> completeCorrectiveAction(@PathVariable Long id,
            @RequestBody UpdateCorrectiveActionRequest request) {

        try {
            if (request.getUpdatedByUserId() == null) {
                logger.error("updatedByUserId is required");
                return ResponseEntity.badRequest().build();
            }

            CorrectiveAction completedAction = correctiveActionService.completeCorrectiveAction(id,
                    request.getUpdatedByUserId(), request.getCompletionNotes());

            return ResponseEntity.ok(convertToDTO(completedAction));
        } catch (IllegalArgumentException e) {
            logger.error("Error completing corrective action {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error completing corrective action {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/retract")
    public ResponseEntity<CorrectiveActionDTO> retractCorrectiveAction(@PathVariable Long id,
            @RequestBody UpdateCorrectiveActionRequest request) {

        try {
            if (request.getUpdatedByUserId() == null) {
                return ResponseEntity.badRequest().build();
            }

            if (request.getRetractionReason() == null || request.getRetractionReason().isEmpty()) {
                logger.error("retractionReason is required");
                return ResponseEntity.badRequest().build();
            }

            CorrectiveAction retractedAction = correctiveActionService.retractCorrectiveAction(id,
                    request.getUpdatedByUserId(), request.getRetractionReason());

            return ResponseEntity.ok(convertToDTO(retractedAction));
        } catch (IllegalArgumentException e) {
            logger.error("Error retracting corrective action {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error retracting corrective action {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private CorrectiveActionDTO convertToDTO(CorrectiveAction action) {
        CorrectiveActionDTO dto = new CorrectiveActionDTO();
        dto.setId(action.getId());
        dto.setFreezerId(action.getFreezerId());
        dto.setActionType(action.getActionType() != null ? action.getActionType().name() : null);
        dto.setDescription(action.getDescription());
        dto.setStatus(action.getStatus() != null ? action.getStatus().name() : null);
        dto.setCreatedAt(action.getCreatedAt());
        dto.setCreatedBy(action.getCreatedBy() != null ? Integer.parseInt(action.getCreatedBy().getId()) : null);

        if (action.getCreatedBy() != null) {
            String firstName = action.getCreatedBy().getFirstName();
            String lastName = action.getCreatedBy().getLastName();
            if (firstName != null && lastName != null) {
                dto.setCreatedByName(firstName + " " + lastName);
            } else {
                dto.setCreatedByName(action.getCreatedBy().getLoginName());
            }
        }

        dto.setUpdatedAt(action.getUpdatedAt());

        if (action.getUpdatedBy() != null) {
            dto.setUpdatedBy(Integer.parseInt(action.getUpdatedBy().getId()));
            String firstName = action.getUpdatedBy().getFirstName();
            String lastName = action.getUpdatedBy().getLastName();
            if (firstName != null && lastName != null) {
                dto.setUpdatedByName(firstName + " " + lastName);
            } else {
                dto.setUpdatedByName(action.getUpdatedBy().getLoginName());
            }
        }

        dto.setCompletedAt(action.getCompletedAt());
        dto.setCompletionNotes(action.getCompletionNotes());

        dto.setRetractedAt(action.getRetractedAt());
        dto.setRetractionReason(action.getRetractionReason());
        dto.setIsEdited(action.getIsEdited());

        if (action.getFreezerId() != null) {
            try {
                Optional<Freezer> freezerOpt = freezerService.findById(action.getFreezerId());
                if (freezerOpt.isPresent()) {
                    dto.setFreezerName(freezerOpt.get().getName());
                } else {
                    dto.setFreezerName("Freezer " + action.getFreezerId());
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch freezer name for freezer {}", action.getFreezerId(), e);
                dto.setFreezerName("Freezer " + action.getFreezerId());
            }
        }

        return dto;
    }

    /**
     * DTO for corrective action responses. Represents maintenance/repair logs for
     * cold storage devices with computed fields.
     */
    @Data
    public static class CorrectiveActionDTO {
        private Long id;
        private Long freezerId;
        private String freezerName;
        private String actionType;
        private String description;
        private String status;
        private OffsetDateTime createdAt;
        private Integer createdBy;
        private String createdByName;
        private OffsetDateTime updatedAt;
        private Integer updatedBy;
        private String updatedByName;
        private OffsetDateTime completedAt;
        private String completionNotes;
        private OffsetDateTime retractedAt;
        private String retractionReason;
        private Boolean isEdited;
    }

    /**
     * Request DTO for creating a corrective action.
     */
    @Data
    public static class CreateCorrectiveActionRequest {
        private Long freezerId;
        private String actionType;
        private String description;
        private Integer createdByUserId;
    }

    /**
     * Request DTO for updating a corrective action.
     */
    @Data
    public static class UpdateCorrectiveActionRequest {
        private String description;
        private String status;
        private Integer updatedByUserId;
        private String completionNotes;
        private String retractionReason;
    }
}
