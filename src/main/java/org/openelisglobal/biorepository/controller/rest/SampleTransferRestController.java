package org.openelisglobal.biorepository.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.biorepository.service.SampleTransferService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.biorepository.valueholder.SampleTransferItem;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest.TransferStatus;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Sample Transfer operations in the Biorepository module.
 *
 * Provides endpoints for: - Creating transfer requests from origin labs -
 * Querying pending/processed transfers - Accepting/rejecting individual items
 * or entire requests - Checking transfer status for samples
 */
@RestController
@RequestMapping(value = "/rest/biorepository/transfer")
public class SampleTransferRestController extends BaseRestController {

    @Autowired
    private SampleTransferService transferService;

    /**
     * Create a new transfer request. Called by origin labs to post samples to the
     * biorepository queue.
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createTransferRequest(@RequestBody TransferRequestCreate request,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            SampleTransferRequest transfer = transferService.createTransferRequest(request.getSourceLab(),
                    request.getSampleItemIds(), request.getRequestNotes(), sysUserId);

            return ResponseEntity.ok(Map.of("id", transfer.getId(), "status", transfer.getStatus().name(), "itemCount",
                    transfer.getTotalItemCount(), "sourceLab", transfer.getSourceLab()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create transfer request: " + e.getMessage()));
        }
    }

    /**
     * Get pending transfer requests for biorepository review.
     */
    @GetMapping(value = "/pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getPendingRequests(@RequestParam(defaultValue = "50") int limit) {

        List<SampleTransferRequest> requests = transferService.getPendingRequests(limit);
        List<Map<String, Object>> result = new ArrayList<>();

        for (SampleTransferRequest request : requests) {
            result.add(mapTransferRequest(request));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get a transfer request by ID.
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getTransferRequest(@PathVariable("id") Integer id) {
        SampleTransferRequest request = transferService.get(id);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapTransferRequestWithItems(request));
    }

    /**
     * Get transfer requests by status.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getByStatus(@RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) String sourceLab) {

        List<SampleTransferRequest> requests;
        if (status != null) {
            requests = transferService.getByStatus(status);
        } else if (sourceLab != null) {
            requests = transferService.getBySourceLab(sourceLab);
        } else {
            requests = transferService.getPendingRequests(100);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (SampleTransferRequest request : requests) {
            result.add(mapTransferRequest(request));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get transfer requests for a specific sample item. Allows origin labs to check
     * status of their transfers.
     */
    @GetMapping(value = "/by-sample-item/{sampleItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getBySampleItem(
            @PathVariable("sampleItemId") Integer sampleItemId) {

        List<SampleTransferRequest> requests = transferService.getBySampleItemId(sampleItemId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (SampleTransferRequest request : requests) {
            result.add(mapTransferRequest(request));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Check if a sample item has a pending transfer.
     */
    @GetMapping(value = "/has-pending/{sampleItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Boolean>> hasPendingTransfer(@PathVariable("sampleItemId") Integer sampleItemId) {

        boolean hasPending = transferService.hasPendingTransfer(sampleItemId);
        return ResponseEntity.ok(Map.of("hasPending", hasPending));
    }

    /**
     * Accept a single transfer item.
     */
    @PostMapping(value = "/item/{itemId}/accept", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> acceptItem(@PathVariable("itemId") Integer itemId,
            @RequestBody(required = false) BioSampleMetadata metadata, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            BioSample bioSample = createBioSampleFromMetadata(metadata);
            SampleTransferItem item = transferService.acceptItem(itemId, bioSample, sysUserId);

            return ResponseEntity.ok(Map.of("id", item.getId(), "status", item.getStatus().name(), "bioSampleId",
                    item.getBioSample() != null ? item.getBioSample().getId() : null));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to accept transfer item: " + e.getMessage()));
        }
    }

    /**
     * Reject a single transfer item.
     */
    @PostMapping(value = "/item/{itemId}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> rejectItem(@PathVariable("itemId") Integer itemId, @RequestBody RejectRequest rejectReq,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            SampleTransferItem item = transferService.rejectItem(itemId, rejectReq.getReason(), sysUserId);
            return ResponseEntity.ok(Map.of("id", item.getId(), "status", item.getStatus().name(), "rejectionReason",
                    item.getRejectionReason()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to reject transfer item: " + e.getMessage()));
        }
    }

    /**
     * Accept all pending items in a transfer request.
     */
    @PostMapping(value = "/{id}/accept-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> acceptAll(@PathVariable("id") Integer id,
            @RequestBody(required = false) BioSampleMetadata metadata, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            BioSample bioSampleTemplate = createBioSampleFromMetadata(metadata);
            SampleTransferRequest request = transferService.acceptAll(id, bioSampleTemplate, sysUserId);

            return ResponseEntity.ok(Map.of("id", request.getId(), "status", request.getStatus().name(),
                    "acceptedCount", request.getAcceptedItemCount()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to accept transfer request: " + e.getMessage()));
        }
    }

    /**
     * Reject all pending items in a transfer request.
     */
    @PostMapping(value = "/{id}/reject-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> rejectAll(@PathVariable("id") Integer id, @RequestBody RejectRequest rejectReq,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            SampleTransferRequest request = transferService.rejectAll(id, rejectReq.getReason(), sysUserId);

            return ResponseEntity.ok(Map.of("id", request.getId(), "status", request.getStatus().name(),
                    "rejectionReason", request.getRejectionReason()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to reject transfer request: " + e.getMessage()));
        }
    }

    /**
     * Cancel a transfer request (by origin lab).
     */
    @PostMapping(value = "/{id}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancelRequest(@PathVariable("id") Integer id, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            SampleTransferRequest request = transferService.cancelRequest(id, sysUserId);
            return ResponseEntity.ok(Map.of("id", request.getId(), "status", request.getStatus().name()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to cancel transfer request: " + e.getMessage()));
        }
    }

    /**
     * Get transfer statistics.
     */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", transferService.countByStatus(TransferStatus.PENDING));
        stats.put("accepted", transferService.countByStatus(TransferStatus.ACCEPTED));
        stats.put("partiallyAccepted", transferService.countByStatus(TransferStatus.PARTIALLY_ACCEPTED));
        stats.put("rejected", transferService.countByStatus(TransferStatus.REJECTED));
        stats.put("cancelled", transferService.countByStatus(TransferStatus.CANCELLED));
        stats.put("sourceLabs", transferService.getDistinctSourceLabs());
        return ResponseEntity.ok(stats);
    }

    private Map<String, Object> mapTransferRequest(SampleTransferRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", request.getId());
        map.put("sourceLab", request.getSourceLab());
        map.put("destinationLab", request.getDestinationLab());
        map.put("status", request.getStatus().name());
        map.put("requestedTimestamp", request.getRequestedTimestamp().toString());
        map.put("requestNotes", request.getRequestNotes());
        map.put("totalItemCount", request.getTotalItemCount());
        map.put("acceptedItemCount", request.getAcceptedItemCount());
        map.put("rejectedItemCount", request.getRejectedItemCount());
        if (request.getRequestedBy() != null) {
            map.put("requestedByName", request.getRequestedBy().getNameForDisplay());
        }
        if (request.getProcessedTimestamp() != null) {
            map.put("processedTimestamp", request.getProcessedTimestamp().toString());
        }
        if (request.getProcessedBy() != null) {
            map.put("processedByName", request.getProcessedBy().getNameForDisplay());
        }
        if (request.getRejectionReason() != null) {
            map.put("rejectionReason", request.getRejectionReason());
        }
        return map;
    }

    private Map<String, Object> mapTransferRequestWithItems(SampleTransferRequest request) {
        Map<String, Object> map = mapTransferRequest(request);

        List<Map<String, Object>> items = new ArrayList<>();
        for (SampleTransferItem item : request.getItems()) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", item.getId());
            itemMap.put("status", item.getStatus().name());
            itemMap.put("rejectionReason", item.getRejectionReason());

            SampleItem sampleItem = item.getSampleItem();
            if (sampleItem != null) {
                itemMap.put("sampleItemId", sampleItem.getId());
                itemMap.put("externalId", sampleItem.getExternalId());
                if (sampleItem.getTypeOfSample() != null) {
                    itemMap.put("sampleType", sampleItem.getTypeOfSample().getDescription());
                }
                if (sampleItem.getSample() != null) {
                    itemMap.put("accessionNumber", sampleItem.getSample().getAccessionNumber());
                }
            }

            if (item.getBioSample() != null) {
                itemMap.put("bioSampleId", item.getBioSample().getId());
            }

            items.add(itemMap);
        }
        map.put("items", items);

        return map;
    }

    private BioSample createBioSampleFromMetadata(BioSampleMetadata metadata) {
        BioSample bioSample = new BioSample();
        if (metadata != null) {
            if (metadata.getBiosafetyLevel() != null) {
                bioSample.setBiosafetyLevel(BiosafetyLevel.valueOf(metadata.getBiosafetyLevel()));
            } else {
                bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
            }
            bioSample.setEthicsApprovalRef(metadata.getEthicsApprovalRef());
            bioSample.setMtaReference(metadata.getMtaReference());
            bioSample.setSpecialHandling(metadata.getSpecialHandling());
            bioSample.setPrincipalInvestigator(metadata.getPrincipalInvestigator());
        } else {
            bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        }
        return bioSample;
    }

    /**
     * Request body for creating a transfer request.
     */
    public static class TransferRequestCreate {
        private String sourceLab;
        private List<Integer> sampleItemIds;
        private String requestNotes;

        public String getSourceLab() {
            return sourceLab;
        }

        public void setSourceLab(String sourceLab) {
            this.sourceLab = sourceLab;
        }

        public List<Integer> getSampleItemIds() {
            return sampleItemIds;
        }

        public void setSampleItemIds(List<Integer> sampleItemIds) {
            this.sampleItemIds = sampleItemIds;
        }

        public String getRequestNotes() {
            return requestNotes;
        }

        public void setRequestNotes(String requestNotes) {
            this.requestNotes = requestNotes;
        }
    }

    /**
     * BioSample metadata for acceptance.
     */
    public static class BioSampleMetadata {
        private String biosafetyLevel;
        private String ethicsApprovalRef;
        private String mtaReference;
        private String specialHandling;
        private String principalInvestigator;

        public String getBiosafetyLevel() {
            return biosafetyLevel;
        }

        public void setBiosafetyLevel(String biosafetyLevel) {
            this.biosafetyLevel = biosafetyLevel;
        }

        public String getEthicsApprovalRef() {
            return ethicsApprovalRef;
        }

        public void setEthicsApprovalRef(String ethicsApprovalRef) {
            this.ethicsApprovalRef = ethicsApprovalRef;
        }

        public String getMtaReference() {
            return mtaReference;
        }

        public void setMtaReference(String mtaReference) {
            this.mtaReference = mtaReference;
        }

        public String getSpecialHandling() {
            return specialHandling;
        }

        public void setSpecialHandling(String specialHandling) {
            this.specialHandling = specialHandling;
        }

        public String getPrincipalInvestigator() {
            return principalInvestigator;
        }

        public void setPrincipalInvestigator(String principalInvestigator) {
            this.principalInvestigator = principalInvestigator;
        }
    }

    /**
     * Request body for rejection.
     */
    public static class RejectRequest {
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
