package org.openelisglobal.biorepository.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.openelisglobal.biorepository.service.DocumentationVerificationService;
import org.openelisglobal.biorepository.valueholder.DocumentationVerification;
import org.openelisglobal.biorepository.valueholder.DocumentationVerification.OverallStatus;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for DocumentationVerification operations in the Biorepository
 * module. Documentation verification is now at shipment level (one verification
 * per shipment).
 */
@RestController
@RequestMapping(value = "/rest/biorepository/verification")
public class DocumentationVerificationRestController extends BaseRestController {

    @Autowired
    private DocumentationVerificationService verificationService;

    /**
     * Get verifications by status.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DocumentationVerification>> getVerifications(
            @RequestParam(required = false) OverallStatus status,
            @RequestParam(required = false, defaultValue = "50") int limit) {

        List<DocumentationVerification> verifications;
        if (status != null) {
            verifications = verificationService.getByOverallStatus(status);
        } else {
            verifications = verificationService.getPendingVerifications(limit);
        }

        return ResponseEntity.ok(verifications);
    }

    /**
     * Get a verification by ID.
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentationVerification> getVerification(@PathVariable("id") Integer id) {
        DocumentationVerification verification = verificationService.get(id);
        if (verification == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(verification);
    }

    /**
     * Get verification for a shipment.
     */
    @GetMapping(value = "/by-shipment/{shipmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentationVerification> getVerificationByShipment(
            @PathVariable("shipmentId") Integer shipmentId) {

        DocumentationVerification verification = verificationService.getByShipmentId(shipmentId);
        if (verification == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(verification);
    }

    /**
     * Create a verification record for a shipment.
     */
    @PostMapping(value = "/create-for-shipment/{shipmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createVerificationForShipment(@PathVariable("shipmentId") Integer shipmentId,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);

        try {
            DocumentationVerification verification = verificationService.createForShipment(shipmentId, sysUserId);

            return ResponseEntity.ok(Map.of("id", verification.getId(), "shipmentId", shipmentId, "status",
                    verification.getOverallStatus().name()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create verification: " + e.getMessage()));
        }
    }

    /**
     * Update a verification item.
     */
    @PutMapping(value = "/{id}/item", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateVerificationItem(@PathVariable("id") Integer id,
            @RequestBody VerificationItemUpdate update, HttpServletRequest request) {

        try {
            DocumentationVerification verification = verificationService.updateVerificationItem(id,
                    update.getItemName(), update.isVerified(), update.isNotApplicable(), update.getNaJustification());

            return ResponseEntity.ok(Map.of("id", verification.getId(), "status",
                    verification.getOverallStatus().name(), "completedCount", verification.getCompletedCount(),
                    "isComplete", verification.isComplete()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update verification item: " + e.getMessage()));
        }
    }

    /**
     * Complete verification for a shipment.
     */
    @PostMapping(value = "/{id}/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> completeVerification(@PathVariable("id") Integer id, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);

        try {
            Integer verifierUserId = sysUserId != null ? Integer.parseInt(sysUserId) : null;
            DocumentationVerification verification = verificationService.completeVerification(id, verifierUserId);

            return ResponseEntity
                    .ok(Map.of("id", verification.getId(), "status", verification.getOverallStatus().name(),
                            "verifiedTimestamp", verification.getVerifiedTimestamp().toString()));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to complete verification: " + e.getMessage()));
        }
    }

    /**
     * Quarantine a shipment due to failed verification.
     */
    @PostMapping(value = "/{id}/quarantine", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> quarantineShipment(@PathVariable("id") Integer id,
            @RequestBody QuarantineRequest quarantineRequest, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);

        try {
            Integer verifierUserId = sysUserId != null ? Integer.parseInt(sysUserId) : null;
            DocumentationVerification verification = verificationService.quarantineShipment(id, verifierUserId,
                    quarantineRequest.getReason());

            return ResponseEntity.ok(Map.of("id", verification.getId(), "status",
                    verification.getOverallStatus().name(), "quarantineReason", quarantineRequest.getReason()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to quarantine shipment: " + e.getMessage()));
        }
    }

    /**
     * Get pending verifications count.
     */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> getVerificationStats() {
        Map<String, Long> stats = Map.of("pending", verificationService.countByOverallStatus(OverallStatus.PENDING),
                "verified", verificationService.countByOverallStatus(OverallStatus.VERIFIED), "quarantine",
                verificationService.countByOverallStatus(OverallStatus.QUARANTINE));

        return ResponseEntity.ok(stats);
    }

    /**
     * Request body for updating a verification item.
     */
    public static class VerificationItemUpdate {
        private String itemName;
        private boolean verified;
        private boolean notApplicable;
        private String naJustification;

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public boolean isVerified() {
            return verified;
        }

        public void setVerified(boolean verified) {
            this.verified = verified;
        }

        public boolean isNotApplicable() {
            return notApplicable;
        }

        public void setNotApplicable(boolean notApplicable) {
            this.notApplicable = notApplicable;
        }

        public String getNaJustification() {
            return naJustification;
        }

        public void setNaJustification(String naJustification) {
            this.naJustification = naJustification;
        }
    }

    /**
     * Request body for quarantine action.
     */
    public static class QuarantineRequest {
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
