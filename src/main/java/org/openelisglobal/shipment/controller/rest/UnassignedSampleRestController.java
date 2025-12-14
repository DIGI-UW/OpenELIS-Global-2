package org.openelisglobal.shipment.controller.rest;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.shipment.service.UnassignedSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for unassigned referral sample management operations
 * Uses the existing referral table instead of a separate unassigned_sample table
 */
@RestController
@RequestMapping("/rest/unassigned-sample")
public class UnassignedSampleRestController extends BaseRestController {

    @Autowired
    private UnassignedSampleService unassignedSampleService;

    /**
     * Get all unassigned referral samples for dashboard
     * Returns samples that are not yet assigned to a box and not lost/canceled
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllUnassignedSamples() {
        try {
            List<Map<String, Object>> unassignedSamples = unassignedSampleService
                    .getUnassignedSamplesForDashboard();
            return ResponseEntity.ok(unassignedSamples);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get unassigned samples by destination facility
     */
    @GetMapping("/by-facility/{facilityId}")
    public ResponseEntity<List<Map<String, Object>>> getUnassignedSamplesByFacility(
            @PathVariable Integer facilityId) {
        try {
            List<Map<String, Object>> unassignedSamples = unassignedSampleService
                    .getUnassignedSamplesByDestinationFacility(facilityId);
            return ResponseEntity.ok(unassignedSamples);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Count unassigned samples by facility
     */
    @GetMapping("/count-by-facility/{facilityId}")
    public ResponseEntity<Map<String, Integer>> countUnassignedSamplesByFacility(
            @PathVariable Integer facilityId) {
        try {
            int count = unassignedSampleService.countUnassignedSamplesByFacility(facilityId);
            Map<String, Integer> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Assign a referral sample to a shipment box
     */
    @PutMapping("/{referralId}/assign-to-box")
    public ResponseEntity<?> assignSampleToBox(
            @PathVariable String referralId,
            @Valid @RequestBody AssignToBoxRequest requestBody,
            BindingResult result,
            jakarta.servlet.http.HttpServletRequest request) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        try {
            String currentUserId = getSysUserId(request);
            unassignedSampleService.assignSampleToBox(referralId, requestBody.getBoxId(), currentUserId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Sample assigned to box successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error assigning sample to box: " + e.getMessage());
        }
    }

    /**
     * Mark a referral sample as lost
     */
    @PutMapping("/{referralId}/mark-lost")
    public ResponseEntity<?> markSampleAsLost(
            @PathVariable String referralId,
            @Valid @RequestBody MarkLostRequest requestBody,
            BindingResult result,
            jakarta.servlet.http.HttpServletRequest request) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        try {
            String currentUserId = getSysUserId(request);
            unassignedSampleService.markSampleAsLost(referralId, requestBody.getReason(), currentUserId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Sample marked as lost");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error marking sample as lost: " + e.getMessage());
        }
    }

    /**
     * Cancel a referral
     */
    @PutMapping("/{referralId}/cancel")
    public ResponseEntity<?> cancelReferral(
            @PathVariable String referralId,
            @Valid @RequestBody CancelReferralRequest requestBody,
            BindingResult result,
            jakarta.servlet.http.HttpServletRequest request) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        try {
            String currentUserId = getSysUserId(request);
            unassignedSampleService.cancelReferral(referralId, requestBody.getReason(), currentUserId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Referral canceled");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error canceling referral: " + e.getMessage());
        }
    }

    // Request DTOs
    public static class AssignToBoxRequest {
        private String boxId;

        public String getBoxId() {
            return boxId;
        }

        public void setBoxId(String boxId) {
            this.boxId = boxId;
        }
    }

    public static class MarkLostRequest {
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class CancelReferralRequest {
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
