package org.openelisglobal.shipment.controller.rest;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.shipment.dto.SampleItemDTO;
import org.openelisglobal.shipment.form.BoxSampleItemForm;
import org.openelisglobal.shipment.service.BoxSampleItemService;
import org.openelisglobal.shipment.valueholder.BoxSampleItem;
import org.openelisglobal.shipment.valueholder.ReceptionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for box sample management operations.
 *
 * Now uses SampleItem (not Sample) as the correct granularity for shipment
 * operations.
 */
@RestController
@RequestMapping("/rest/box-sample")
public class BoxSampleRestController extends BaseRestController {

    @Autowired
    private BoxSampleItemService boxSampleItemService;

    /**
     * Get box samples by shipping box ID
     * 
     * @deprecated since 3.3.x - Use GET /items/by-box/{shippingBoxId} instead
     *             (SampleItem-based API)
     */
    @Deprecated(since = "3.3.x", forRemoval = true)
    @GetMapping("/by-box/{shippingBoxId}")
    public ResponseEntity<List<Map<String, Object>>> getBoxSamplesByShippingBox(@PathVariable Integer shippingBoxId) {
        // This endpoint is deprecated - table box_sample has been replaced by
        // box_sample_item
        // Use /rest/box-sample/items/by-box/{shippingBoxId} instead
        LogEvent.logWarn(this.getClass().getSimpleName(), "getBoxSamplesByShippingBox",
                "Deprecated endpoint called - use /items/by-box/" + shippingBoxId + " instead");
        return ResponseEntity.status(HttpStatus.GONE).body(java.util.Collections.emptyList());
    }

    /**
     * Get box sample by ID
     * 
     * @deprecated since 3.3.x - table box_sample no longer exists
     */
    @Deprecated(since = "3.3.x", forRemoval = true)
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBoxSampleById(@PathVariable Integer id) {
        LogEvent.logWarn(this.getClass().getSimpleName(), "getBoxSampleById",
                "Deprecated endpoint called - box_sample table no longer exists");
        return ResponseEntity.status(HttpStatus.GONE).build();
    }

    /**
     * Get box sample by sample ID
     * 
     * @deprecated since 3.3.x - table box_sample no longer exists
     */
    @Deprecated(since = "3.3.x", forRemoval = true)
    @GetMapping("/by-sample/{sampleId}")
    public ResponseEntity<Map<String, Object>> getBoxSampleBySampleId(@PathVariable Integer sampleId) {
        LogEvent.logWarn(this.getClass().getSimpleName(), "getBoxSampleBySampleId",
                "Deprecated endpoint called - box_sample table no longer exists");
        return ResponseEntity.status(HttpStatus.GONE).build();
    }

    /**
     * Get box samples by reception status
     * 
     * @deprecated since 3.3.x - table box_sample no longer exists
     */
    @Deprecated(since = "3.3.x", forRemoval = true)
    @GetMapping("/by-box/{shippingBoxId}/status/{status}")
    public ResponseEntity<List<Map<String, Object>>> getBoxSamplesByReceptionStatus(@PathVariable Integer shippingBoxId,
            @PathVariable String status) {
        LogEvent.logWarn(this.getClass().getSimpleName(), "getBoxSamplesByReceptionStatus",
                "Deprecated endpoint called - box_sample table no longer exists");
        return ResponseEntity.status(HttpStatus.GONE).body(java.util.Collections.emptyList());
    }

    /**
     * Add sample to box
     *
     * @deprecated Use POST /items endpoint instead
     */
    @Deprecated(since = "3.3.x", forRemoval = true)
    @PostMapping
    public ResponseEntity<?> addSampleToBox(@RequestBody Map<String, Object> form,
            jakarta.servlet.http.HttpServletRequest request) {
        LogEvent.logWarn(this.getClass().getSimpleName(), "addSampleToBox",
                "Deprecated endpoint called - use POST /items instead");
        return ResponseEntity.status(HttpStatus.GONE).body(java.util.Collections.singletonMap("error",
                "This endpoint is deprecated. Use POST /rest/box-sample/items instead"));
    }

    /**
     * Remove sample from box (POST alternative to DELETE for CSRF compatibility)
     *
     * @deprecated Use POST /items/{id}/remove endpoint instead
     */
    @Deprecated(since = "3.3.x", forRemoval = true)
    @PostMapping("/{id}/remove")
    public ResponseEntity<?> removeSampleFromBoxPost(@PathVariable Integer id) {
        LogEvent.logWarn(this.getClass().getSimpleName(), "removeSampleFromBoxPost",
                "Deprecated endpoint called - use POST /items/{id}/remove instead");
        return ResponseEntity.status(HttpStatus.GONE).body(java.util.Collections.singletonMap("error",
                "This endpoint is deprecated. Use POST /rest/box-sample/items/{id}/remove instead"));
    }

    /**
     * Remove sample from box
     *
     * @deprecated Use DELETE /items/{id} endpoint instead
     */
    @Deprecated(since = "3.3.x", forRemoval = true)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeSampleFromBox(@PathVariable Integer id) {
        LogEvent.logWarn(this.getClass().getSimpleName(), "removeSampleFromBox",
                "Deprecated endpoint called - use DELETE /items/{id} instead");
        return ResponseEntity.status(HttpStatus.GONE).body(java.util.Collections.singletonMap("error",
                "This endpoint is deprecated. Use DELETE /rest/box-sample/items/{id} instead"));
    }

    /**
     * Update reception status
     *
     * @deprecated Use PUT /items/{id}/reception-status endpoint instead
     */
    @Deprecated(since = "3.3.x", forRemoval = true)
    @PutMapping("/{id}/reception-status")
    public ResponseEntity<?> updateReceptionStatus(@PathVariable Integer id, @RequestParam String status,
            @RequestParam(required = false) String notes) {
        LogEvent.logWarn(this.getClass().getSimpleName(), "updateReceptionStatus",
                "Deprecated endpoint called - use PUT /items/{id}/reception-status instead");
        return ResponseEntity.status(HttpStatus.GONE).body(java.util.Collections.singletonMap("error",
                "This endpoint is deprecated. Use PUT /rest/box-sample/items/{id}/reception-status instead"));
    }

    /**
     * Check if sample is in a box
     *
     * @deprecated Use /items/check-sample-item/{sampleItemId} endpoint instead
     */
    @Deprecated(since = "3.3.x", forRemoval = true)
    @GetMapping("/check-sample/{sampleId}")
    public ResponseEntity<Boolean> isSampleInBox(@PathVariable Integer sampleId) {
        LogEvent.logWarn(this.getClass().getSimpleName(), "isSampleInBox",
                "Deprecated endpoint called - box_sample table no longer exists");
        return ResponseEntity.status(HttpStatus.GONE).build();
    }

    /**
     * Count samples in box
     *
     * @deprecated Use /items/count-by-box/{shippingBoxId} endpoint instead
     */
    @Deprecated(since = "3.3.x", forRemoval = true)
    @GetMapping("/count-by-box/{shippingBoxId}")
    public ResponseEntity<Integer> countSamplesInBox(@PathVariable Integer shippingBoxId) {
        LogEvent.logWarn(this.getClass().getSimpleName(), "countSamplesInBox",
                "Deprecated endpoint called - box_sample table no longer exists");
        return ResponseEntity.status(HttpStatus.GONE).build();
    }

    // ========== NEW SAMPLEITEM-BASED ENDPOINTS ==========

    /**
     * Get box sample items by shipping box ID (NEW API using SampleItem). Returns
     * full DTOs with typeOfSample and referralTests.
     */
    @GetMapping("/items/by-box/{shippingBoxId}")
    public ResponseEntity<List<SampleItemDTO>> getBoxSampleItemsByShippingBox(@PathVariable Integer shippingBoxId) {
        try {
            List<SampleItemDTO> sampleItems = boxSampleItemService.getBoxSampleItemDTOsByShippingBoxId(shippingBoxId);
            return ResponseEntity.ok(sampleItems);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Add sample item to box (NEW API using SampleItem).
     */
    @PostMapping("/items")
    public ResponseEntity<?> addSampleItemToBox(@Valid @RequestBody BoxSampleItemForm form, BindingResult result,
            jakarta.servlet.http.HttpServletRequest request) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        try {
            // Get system user ID for audit trail
            String userIdString = getSysUserId(request);
            Integer systemUserId = userIdString != null ? Integer.parseInt(userIdString) : null;

            BoxSampleItem boxSampleItem = boxSampleItemService.addSampleItemToBox(form.getShippingBoxId(),
                    form.getSampleItemId(), systemUserId);

            // Convert to DTO for response
            SampleItemDTO responseDTO = convertBoxSampleItemToDTO(boxSampleItem);

            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    java.util.Collections.singletonMap("error", "Error adding sample item to box: " + e.getMessage()));
        }
    }

    /**
     * Remove sample item from box (POST alternative to DELETE for CSRF
     * compatibility).
     */
    @PostMapping("/items/{id}/remove")
    public ResponseEntity<?> removeSampleItemFromBoxPost(@PathVariable Integer id,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String userIdString = getSysUserId(request);
            Integer systemUserId = userIdString != null ? Integer.parseInt(userIdString) : null;
            boxSampleItemService.removeSampleItemFromBox(id, systemUserId);
            return ResponseEntity.ok(java.util.Collections.singletonMap("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Collections
                    .singletonMap("error", "Error removing sample item from box: " + e.getMessage()));
        }
    }

    /**
     * Remove sample item from box (DELETE method).
     */
    @DeleteMapping("/items/{id}")
    public ResponseEntity<?> removeSampleItemFromBox(@PathVariable Integer id,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String userIdString = getSysUserId(request);
            Integer systemUserId = userIdString != null ? Integer.parseInt(userIdString) : null;
            boxSampleItemService.removeSampleItemFromBox(id, systemUserId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Collections
                    .singletonMap("error", "Error removing sample item from box: " + e.getMessage()));
        }
    }

    /**
     * Update reception status for a sample item.
     */
    @PutMapping("/items/{id}/reception-status")
    public ResponseEntity<?> updateSampleItemReceptionStatus(@PathVariable Integer id, @RequestParam String status,
            @RequestParam(required = false) String notes, jakarta.servlet.http.HttpServletRequest request) {
        try {
            ReceptionStatus receptionStatus = ReceptionStatus.valueOf(status.toUpperCase());
            String userIdString = getSysUserId(request);
            Integer systemUserId = userIdString != null ? Integer.parseInt(userIdString) : null;
            BoxSampleItem boxSampleItem = boxSampleItemService.updateReceptionStatus(id, receptionStatus, notes,
                    systemUserId);

            // Convert to DTO for response
            SampleItemDTO responseDTO = convertBoxSampleItemToDTO(boxSampleItem);

            return ResponseEntity.ok(responseDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Collections.singletonMap("error", "Invalid reception status: " + status));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    java.util.Collections.singletonMap("error", "Error updating reception status: " + e.getMessage()));
        }
    }

    /**
     * Check if sample item is in a box.
     */
    @GetMapping("/items/check/{sampleItemId}")
    public ResponseEntity<Boolean> isSampleItemInBox(@PathVariable String sampleItemId) {
        try {
            boolean inBox = boxSampleItemService.isSampleItemInBox(sampleItemId);
            return ResponseEntity.ok(inBox);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Count sample items in box.
     */
    @GetMapping("/items/count-by-box/{shippingBoxId}")
    public ResponseEntity<Integer> countSampleItemsInBox(@PathVariable Integer shippingBoxId) {
        try {
            int count = boxSampleItemService.countSampleItemsInBox(shippingBoxId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Convert BoxSampleItem to SampleItemDTO
     */
    private SampleItemDTO convertBoxSampleItemToDTO(BoxSampleItem boxSampleItem) {
        SampleItemDTO dto = new SampleItemDTO();

        if (boxSampleItem.getSampleItem() != null) {
            dto.setSampleItemId(boxSampleItem.getSampleItem().getId());

            if (boxSampleItem.getSampleItem().getSample() != null) {
                dto.setAccessionNumber(boxSampleItem.getSampleItem().getSample().getAccessionNumber());
                dto.setCollectionDate(boxSampleItem.getSampleItem().getSample().getCollectionDate());
            }

            if (boxSampleItem.getSampleItem().getTypeOfSample() != null) {
                dto.setTypeOfSample(boxSampleItem.getSampleItem().getTypeOfSample().getDescription());
                dto.setTypeOfSampleId(boxSampleItem.getSampleItem().getTypeOfSample().getId());
            }
        }

        if (boxSampleItem.getShippingBox() != null) {
            dto.setAssignedBoxId(boxSampleItem.getShippingBox().getId());
            dto.setAssignedBoxName(boxSampleItem.getShippingBox().getBoxId());
        }

        // BoxSampleItem ID for reception updates
        dto.setBoxSampleItemId(boxSampleItem.getId());

        // Reception data
        if (boxSampleItem.getReceptionStatus() != null) {
            dto.setReceptionStatus(boxSampleItem.getReceptionStatus().name());
        }
        dto.setReceptionNotes(boxSampleItem.getReceptionNotes());

        return dto;
    }

}
