package org.openelisglobal.biorepository.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.biorepository.service.SampleRetrievalService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.DestinationType;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.PriorityLevel;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.RequestStatus;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Sample Retrieval operations in the Biorepository module.
 *
 * Provides endpoints for: - Creating retrieval requests for samples -
 * Submitting requests for approval - Approving/rejecting requests (supervisor)
 * - Recording physical retrieval and return - Querying retrieval history
 */
@RestController
@RequestMapping(value = "/rest/biorepository/retrieval")
public class SampleRetrievalRestController extends BaseRestController {

    @Autowired
    private SampleRetrievalService retrievalService;

    @Autowired
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Autowired
    private StorageLocationService storageLocationService;

    /**
     * Create a new retrieval request (DRAFT status).
     */
    @PostMapping(value = "/requests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createRequest(@RequestBody RetrievalRequestCreate request,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            DestinationType destType = request.getDestinationType() != null
                    ? DestinationType.valueOf(request.getDestinationType())
                    : DestinationType.ANALYSIS_RETURN;

            PriorityLevel priority = request.getPriorityLevel() != null
                    ? PriorityLevel.valueOf(request.getPriorityLevel())
                    : PriorityLevel.NORMAL;

            SampleRetrievalRequest retrieval = retrievalService.createRequest(request.getRequestPurpose(),
                    request.getBioSampleIds(), request.getProjectId(), request.getEthicsApprovalRef(), destType,
                    request.getDestinationDetails(), priority, request.getRequiredByDate(), sysUserId);

            return ResponseEntity.ok(Map.of("id", retrieval.getId(), "requestNumber", retrieval.getRequestNumber(),
                    "status", retrieval.getStatus().name(), "itemCount", retrieval.getTotalItemCount()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create retrieval request: " + e.getMessage()));
        }
    }

    /**
     * Get retrieval requests with optional filters.
     *
     * @param status Comma-separated list of statuses (e.g., "APPROVED,IN_PROGRESS")
     */
    @GetMapping(value = "/requests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getRequests(@RequestParam(required = false) String status,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer requestedByUserId) {

        List<SampleRetrievalRequest> requests;
        if (status != null && !status.trim().isEmpty()) {
            // Handle comma-separated statuses
            String[] statusArray = status.split(",");
            List<SampleRetrievalRequest> allRequests = new ArrayList<>();
            for (String s : statusArray) {
                try {
                    RequestStatus requestStatus = RequestStatus.valueOf(s.trim());
                    allRequests.addAll(retrievalService.getByStatus(requestStatus));
                } catch (IllegalArgumentException e) {
                    // Skip invalid status
                }
            }
            requests = allRequests;
        } else if (projectId != null) {
            requests = retrievalService.getByProjectId(projectId);
        } else if (requestedByUserId != null) {
            requests = retrievalService.getByRequestedByUserId(requestedByUserId);
        } else {
            requests = retrievalService.getPendingApproval(100);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (SampleRetrievalRequest request : requests) {
            result.add(mapRetrievalRequest(request));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get a retrieval request by ID with items.
     */
    @GetMapping(value = "/requests/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRequest(@PathVariable("id") Integer id) {
        SampleRetrievalRequest request = retrievalService.get(id);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapRetrievalRequestWithItems(request));
    }

    /**
     * Get a retrieval request by request number.
     */
    @GetMapping(value = "/requests/by-number/{requestNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getByRequestNumber(@PathVariable("requestNumber") String requestNumber) {
        SampleRetrievalRequest request = retrievalService.getByRequestNumber(requestNumber);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapRetrievalRequestWithItems(request));
    }

    /**
     * Get pending approval requests.
     */
    @GetMapping(value = "/requests/pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getPendingApproval(@RequestParam(defaultValue = "50") int limit) {

        List<SampleRetrievalRequest> requests = retrievalService.getPendingApproval(limit);
        List<Map<String, Object>> result = new ArrayList<>();

        for (SampleRetrievalRequest request : requests) {
            result.add(mapRetrievalRequest(request));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Submit a draft request for approval.
     */
    @PostMapping(value = "/requests/{id}/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> submitForApproval(@PathVariable("id") Integer id, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            SampleRetrievalRequest request = retrievalService.submitForApproval(id, sysUserId);
            return ResponseEntity.ok(Map.of("id", request.getId(), "status", request.getStatus().name()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to submit request: " + e.getMessage()));
        }
    }

    /**
     * Approve a pending request (generates work order).
     */
    @PostMapping(value = "/requests/{id}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> approveRequest(@PathVariable("id") Integer id,
            @RequestBody(required = false) ApprovalRequest approvalReq, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            String notes = approvalReq != null ? approvalReq.getApprovalNotes() : null;
            SampleRetrievalRequest request = retrievalService.approveRequest(id, notes, sysUserId);

            return ResponseEntity.ok(Map.of("id", request.getId(), "status", request.getStatus().name(),
                    "workOrderNumber", request.getWorkOrderNumber()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to approve request: " + e.getMessage()));
        }
    }

    /**
     * Reject a pending request.
     */
    @PostMapping(value = "/requests/{id}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> rejectRequest(@PathVariable("id") Integer id, @RequestBody RejectRequest rejectReq,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            SampleRetrievalRequest request = retrievalService.rejectRequest(id, rejectReq.getReason(), sysUserId);
            return ResponseEntity.ok(Map.of("id", request.getId(), "status", request.getStatus().name(),
                    "rejectionReason", request.getRejectionReason()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to reject request: " + e.getMessage()));
        }
    }

    /**
     * Cancel a retrieval request.
     */
    @PostMapping(value = "/requests/{id}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> cancelRequest(@PathVariable("id") Integer id, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            SampleRetrievalRequest request = retrievalService.cancelRequest(id, sysUserId);
            return ResponseEntity.ok(Map.of("id", request.getId(), "status", request.getStatus().name()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to cancel request: " + e.getMessage()));
        }
    }

    /**
     * Mark a retrieval request as completed (administrative override).
     */
    @PostMapping(value = "/requests/{id}/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> completeRequest(@PathVariable("id") Integer id, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            SampleRetrievalRequest request = retrievalService.get(id);
            if (request == null) {
                return ResponseEntity.notFound().build();
            }

            request.setStatus(RequestStatus.COMPLETED);
            request.setSysUserId(sysUserId);
            retrievalService.update(request);

            return ResponseEntity.ok(Map.of("id", request.getId(), "status", request.getStatus().name()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to complete request: " + e.getMessage()));
        }
    }

    /**
     * Record physical retrieval of an item from storage.
     */
    @PostMapping(value = "/items/{itemId}/retrieve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> retrieveItem(@PathVariable("itemId") Integer itemId,
            @RequestBody(required = false) RetrievalDetails details, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            String condition = details != null ? details.getConditionAtRelease() : null;
            String notes = details != null ? details.getConditionNotes() : null;
            BigDecimal temp = details != null ? details.getTemperatureAtRetrieval() : null;

            SampleRetrievalItem item = retrievalService.retrieveItem(itemId, condition, notes, temp, sysUserId);

            return ResponseEntity.ok(Map.of("id", item.getId(), "status", item.getStatus().name(), "bioSampleId",
                    item.getBioSampleId()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve item: " + e.getMessage()));
        }
    }

    /**
     * Mark an item as released to the requester.
     */
    @PostMapping(value = "/items/{itemId}/release", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> releaseItem(@PathVariable("itemId") Integer itemId, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            SampleRetrievalItem item = retrievalService.releaseItem(itemId, sysUserId);
            return ResponseEntity.ok(Map.of("id", item.getId(), "status", item.getStatus().name()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to release item: " + e.getMessage()));
        }
    }

    /**
     * Process return of an item.
     */
    @PostMapping(value = "/items/{itemId}/return", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> returnItem(@PathVariable("itemId") Integer itemId, @RequestBody ReturnDetails details,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            SampleRetrievalItem item = retrievalService.returnItem(itemId, details.getReturnedCondition(),
                    details.getReturnNotes(), details.isFullyConsumed(), sysUserId);

            return ResponseEntity.ok(Map.of("id", item.getId(), "status", item.getStatus().name()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to return item: " + e.getMessage()));
        }
    }

    /**
     * Mark an item as unavailable.
     */
    @PostMapping(value = "/items/{itemId}/unavailable", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> markItemUnavailable(@PathVariable("itemId") Integer itemId,
            @RequestBody RejectRequest reasonReq, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            SampleRetrievalItem item = retrievalService.markItemUnavailable(itemId, reasonReq.getReason(), sysUserId);
            return ResponseEntity.ok(Map.of("id", item.getId(), "status", item.getStatus().name()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to mark item unavailable: " + e.getMessage()));
        }
    }

    /**
     * Update item condition notes.
     */
    @PutMapping(value = "/items/{itemId}/condition", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateItemCondition(@PathVariable("itemId") Integer itemId,
            @RequestBody ConditionUpdate update, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            SampleRetrievalItem item = retrievalService.getRetrievalItem(itemId);
            if (item == null) {
                return ResponseEntity.notFound().build();
            }

            item.setConditionNotes(update.getConditionNotes());
            item.setSysUserId(sysUserId);

            return ResponseEntity.ok(Map.of("id", item.getId(), "conditionNotes", item.getConditionNotes()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update condition: " + e.getMessage()));
        }
    }

    /**
     * Get retrieval requests for a specific BioSample.
     */
    @GetMapping(value = "/by-biosample/{bioSampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getByBioSample(@PathVariable("bioSampleId") Integer bioSampleId) {

        List<SampleRetrievalRequest> requests = retrievalService.getByBioSampleId(bioSampleId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (SampleRetrievalRequest request : requests) {
            result.add(mapRetrievalRequest(request));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Check if a BioSample has a pending/active retrieval.
     */
    @GetMapping(value = "/has-pending/{bioSampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Boolean>> hasPendingRetrieval(@PathVariable("bioSampleId") Integer bioSampleId) {

        boolean hasPending = retrievalService.hasActiveRetrieval(bioSampleId);
        return ResponseEntity.ok(Map.of("hasPending", hasPending));
    }

    /**
     * Get all items from active requests (APPROVED or IN_PROGRESS).
     */
    @GetMapping(value = "/items", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getActiveItems() {

        List<SampleRetrievalRequest> activeRequests = new ArrayList<>();
        activeRequests.addAll(retrievalService.getByStatus(RequestStatus.APPROVED));
        activeRequests.addAll(retrievalService.getByStatus(RequestStatus.IN_PROGRESS));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SampleRetrievalRequest request : activeRequests) {
            for (SampleRetrievalItem item : request.getItems()) {
                result.add(mapRetrievalItem(item));
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get currently checked out items.
     */
    @GetMapping(value = "/checked-out", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getCheckedOutItems() {

        List<SampleRetrievalItem> items = retrievalService.getCheckedOutItems();
        List<Map<String, Object>> result = new ArrayList<>();

        for (SampleRetrievalItem item : items) {
            result.add(mapRetrievalItem(item));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get overdue items.
     */
    @GetMapping(value = "/overdue", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getOverdueItems() {

        List<SampleRetrievalItem> items = retrievalService.getOverdueItems();
        List<Map<String, Object>> result = new ArrayList<>();

        for (SampleRetrievalItem item : items) {
            result.add(mapRetrievalItem(item));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get retrieval statistics.
     */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", retrievalService.countByStatus(RequestStatus.PENDING_APPROVAL));
        stats.put("approved", retrievalService.countByStatus(RequestStatus.APPROVED));
        stats.put("inProgress", retrievalService.countByStatus(RequestStatus.IN_PROGRESS));
        stats.put("completed", retrievalService.countByStatus(RequestStatus.COMPLETED));
        stats.put("rejected", retrievalService.countByStatus(RequestStatus.REJECTED));
        stats.put("checkedOutCount", retrievalService.getCheckedOutItems().size());
        stats.put("overdueCount", retrievalService.getOverdueItems().size());
        return ResponseEntity.ok(stats);
    }

    private Map<String, Object> mapRetrievalRequest(SampleRetrievalRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", request.getId());
        map.put("requestNumber", request.getRequestNumber());
        map.put("requestPurpose", request.getRequestPurpose());
        map.put("status", request.getStatus().name());
        map.put("priorityLevel", request.getPriorityLevel().name());
        map.put("destinationType", request.getDestinationType().name());
        map.put("destinationDetails", request.getDestinationDetails());
        map.put("requestedTimestamp", request.getRequestedTimestamp().toString());
        map.put("totalItemCount", request.getTotalItemCount());
        map.put("retrievedItemCount", request.getRetrievedItemCount());
        map.put("returnedItemCount", request.getReturnedItemCount());
        map.put("consumedItemCount", request.getConsumedItemCount());

        if (request.getRequestedBy() != null) {
            map.put("requestedByName", request.getRequestedBy().getNameForDisplay());
        }
        if (request.getProject() != null) {
            map.put("projectId", request.getProject().getId());
            map.put("projectName", request.getProject().getProjectName());
        }
        if (request.getEthicsApprovalRef() != null) {
            map.put("ethicsApprovalRef", request.getEthicsApprovalRef());
        }
        if (request.getRequiredByDate() != null) {
            map.put("requiredByDate", request.getRequiredByDate().toString());
        }
        if (request.getApprovedBy() != null) {
            map.put("approvedByName", request.getApprovedBy().getNameForDisplay());
        }
        if (request.getApprovedTimestamp() != null) {
            map.put("approvedTimestamp", request.getApprovedTimestamp().toString());
        }
        if (request.getWorkOrderNumber() != null) {
            map.put("workOrderNumber", request.getWorkOrderNumber());
        }
        if (request.getRejectionReason() != null) {
            map.put("rejectionReason", request.getRejectionReason());
        }
        if (request.getLastupdated() != null) {
            map.put("lastUpdated", request.getLastupdated().toString());
        }

        return map;
    }

    private Map<String, Object> mapRetrievalRequestWithItems(SampleRetrievalRequest request) {
        Map<String, Object> map = mapRetrievalRequest(request);

        List<Map<String, Object>> items = new ArrayList<>();
        for (SampleRetrievalItem item : request.getItems()) {
            items.add(mapRetrievalItem(item));
        }
        map.put("items", items);

        return map;
    }

    private Map<String, Object> mapRetrievalItem(SampleRetrievalItem item) {
        Map<String, Object> itemMap = new HashMap<>();
        itemMap.put("id", item.getId());
        itemMap.put("status", item.getStatus().name());
        itemMap.put("bioSampleId", item.getBioSampleId());
        itemMap.put("returnExpected", item.getReturnExpected());
        itemMap.put("isOverdue", item.isOverdue());

        BioSample bioSample = item.getBioSample();
        if (bioSample != null) {
            itemMap.put("workflowStatus", bioSample.getWorkflowStatus().name());
            itemMap.put("biosafetyLevel", bioSample.getBiosafetyLevel().name());

            SampleItem sampleItem = bioSample.getSampleItem();
            if (sampleItem != null) {
                itemMap.put("sampleItemId", sampleItem.getId());
                itemMap.put("externalId", sampleItem.getExternalId());
                if (sampleItem.getTypeOfSample() != null) {
                    itemMap.put("sampleType", sampleItem.getTypeOfSample().getDescription());
                }
                if (sampleItem.getSample() != null) {
                    itemMap.put("accessionNumber", sampleItem.getSample().getAccessionNumber());
                }

                // Fetch storage location data
                try {
                    SampleStorageAssignment storageAssignment = sampleStorageAssignmentDAO
                            .findBySampleItemId(sampleItem.getId());

                    if (storageAssignment != null) {
                        // Add well coordinate
                        if (storageAssignment.getPositionCoordinate() != null) {
                            itemMap.put("storageCoordinates", storageAssignment.getPositionCoordinate());
                        }

                        // Build storage location path
                        if (storageAssignment.getLocationId() != null && storageAssignment.getLocationType() != null) {
                            String storagePath = buildStoragePath(storageAssignment.getLocationId(),
                                    storageAssignment.getLocationType());
                            if (storagePath != null) {
                                itemMap.put("storageLocation", storagePath);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Log but don't fail if storage lookup fails
                }
            }

            if (bioSample.getRequiredTempMin() != null) {
                itemMap.put("requiredTempMin", bioSample.getRequiredTempMin());
            }
            if (bioSample.getRequiredTempMax() != null) {
                itemMap.put("requiredTempMax", bioSample.getRequiredTempMax());
            }
            if (bioSample.getSpecialHandling() != null) {
                itemMap.put("specialHandling", bioSample.getSpecialHandling());
            }
        }

        if (item.getQuantityRequested() != null) {
            itemMap.put("quantityRequested", item.getQuantityRequested());
        }
        if (item.getUnitOfMeasure() != null) {
            itemMap.put("unitOfMeasure", item.getUnitOfMeasure());
        }
        if (item.getConditionAtRelease() != null) {
            itemMap.put("conditionAtRelease", item.getConditionAtRelease());
        }
        if (item.getConditionNotes() != null) {
            itemMap.put("conditionNotes", item.getConditionNotes());
        }
        if (item.getExpectedReturnDate() != null) {
            itemMap.put("expectedReturnDate", item.getExpectedReturnDate().toString());
        }
        if (item.getRetrievedTimestamp() != null) {
            itemMap.put("retrievedTimestamp", item.getRetrievedTimestamp().toString());
        }
        if (item.getRetrievedBy() != null) {
            itemMap.put("retrievedByName", item.getRetrievedBy().getNameForDisplay());
        }
        if (item.getReturnedTimestamp() != null) {
            itemMap.put("returnedTimestamp", item.getReturnedTimestamp().toString());
        }
        if (item.getReturnedBy() != null) {
            itemMap.put("returnedByName", item.getReturnedBy().getNameForDisplay());
        }
        if (item.getReturnedCondition() != null) {
            itemMap.put("returnedCondition", item.getReturnedCondition());
        }
        if (item.getReturnNotes() != null) {
            itemMap.put("returnNotes", item.getReturnNotes());
        }

        return itemMap;
    }

    /**
     * Build storage location label from location ID and type. Returns the location
     * label (e.g., "Freezer-80C-01" or "Shelf-2"). For a full hierarchical path,
     * would need to traverse parent relationships.
     */
    private String buildStoragePath(Integer locationId, String locationType) {
        if (locationId == null || locationType == null) {
            return null;
        }

        try {
            // Get the storage location based on type and extract its label
            Object location = storageLocationService.get(locationId, getStorageClass(locationType));

            if (location == null) {
                return null;
            }

            // Extract label based on type
            String label = extractLabelFromLocation(location);
            return label;
        } catch (Exception e) {
            // Return null if lookup fails
            return null;
        }
    }

    /**
     * Get the appropriate storage class for the given location type.
     */
    private Class<?> getStorageClass(String locationType) {
        switch (locationType.toLowerCase()) {
        case "device":
            return org.openelisglobal.storage.valueholder.StorageDevice.class;
        case "shelf":
            return org.openelisglobal.storage.valueholder.StorageShelf.class;
        case "rack":
            return org.openelisglobal.storage.valueholder.StorageRack.class;
        case "box":
            return org.openelisglobal.storage.valueholder.StorageBox.class;
        default:
            return null;
        }
    }

    /**
     * Extract the label from a storage location entity.
     */
    private String extractLabelFromLocation(Object location) {
        if (location instanceof org.openelisglobal.storage.valueholder.StorageBox) {
            return ((org.openelisglobal.storage.valueholder.StorageBox) location).getLabel();
        } else if (location instanceof org.openelisglobal.storage.valueholder.StorageRack) {
            return ((org.openelisglobal.storage.valueholder.StorageRack) location).getLabel();
        } else if (location instanceof org.openelisglobal.storage.valueholder.StorageShelf) {
            return ((org.openelisglobal.storage.valueholder.StorageShelf) location).getLabel();
        } else if (location instanceof org.openelisglobal.storage.valueholder.StorageDevice) {
            return ((org.openelisglobal.storage.valueholder.StorageDevice) location).getName();
        }
        return null;
    }

    // ========== DTO CLASSES ==========

    public static class RetrievalRequestCreate {
        private String requestPurpose;
        private List<Integer> bioSampleIds;
        private Integer projectId;
        private String ethicsApprovalRef;
        private String destinationType;
        private String destinationDetails;
        private String priorityLevel;
        private LocalDate requiredByDate;

        public String getRequestPurpose() {
            return requestPurpose;
        }

        public void setRequestPurpose(String requestPurpose) {
            this.requestPurpose = requestPurpose;
        }

        public List<Integer> getBioSampleIds() {
            return bioSampleIds;
        }

        public void setBioSampleIds(List<Integer> bioSampleIds) {
            this.bioSampleIds = bioSampleIds;
        }

        public Integer getProjectId() {
            return projectId;
        }

        public void setProjectId(Integer projectId) {
            this.projectId = projectId;
        }

        public String getEthicsApprovalRef() {
            return ethicsApprovalRef;
        }

        public void setEthicsApprovalRef(String ethicsApprovalRef) {
            this.ethicsApprovalRef = ethicsApprovalRef;
        }

        public String getDestinationType() {
            return destinationType;
        }

        public void setDestinationType(String destinationType) {
            this.destinationType = destinationType;
        }

        public String getDestinationDetails() {
            return destinationDetails;
        }

        public void setDestinationDetails(String destinationDetails) {
            this.destinationDetails = destinationDetails;
        }

        public String getPriorityLevel() {
            return priorityLevel;
        }

        public void setPriorityLevel(String priorityLevel) {
            this.priorityLevel = priorityLevel;
        }

        public LocalDate getRequiredByDate() {
            return requiredByDate;
        }

        public void setRequiredByDate(LocalDate requiredByDate) {
            this.requiredByDate = requiredByDate;
        }
    }

    public static class ApprovalRequest {
        private String approvalNotes;

        public String getApprovalNotes() {
            return approvalNotes;
        }

        public void setApprovalNotes(String approvalNotes) {
            this.approvalNotes = approvalNotes;
        }
    }

    public static class RejectRequest {
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class RetrievalDetails {
        private String conditionAtRelease;
        private String conditionNotes;
        private BigDecimal temperatureAtRetrieval;

        public String getConditionAtRelease() {
            return conditionAtRelease;
        }

        public void setConditionAtRelease(String conditionAtRelease) {
            this.conditionAtRelease = conditionAtRelease;
        }

        public String getConditionNotes() {
            return conditionNotes;
        }

        public void setConditionNotes(String conditionNotes) {
            this.conditionNotes = conditionNotes;
        }

        public BigDecimal getTemperatureAtRetrieval() {
            return temperatureAtRetrieval;
        }

        public void setTemperatureAtRetrieval(BigDecimal temperatureAtRetrieval) {
            this.temperatureAtRetrieval = temperatureAtRetrieval;
        }
    }

    public static class ReturnDetails {
        private String returnedCondition;
        private String returnNotes;
        private boolean fullyConsumed;

        public String getReturnedCondition() {
            return returnedCondition;
        }

        public void setReturnedCondition(String returnedCondition) {
            this.returnedCondition = returnedCondition;
        }

        public String getReturnNotes() {
            return returnNotes;
        }

        public void setReturnNotes(String returnNotes) {
            this.returnNotes = returnNotes;
        }

        public boolean isFullyConsumed() {
            return fullyConsumed;
        }

        public void setFullyConsumed(boolean fullyConsumed) {
            this.fullyConsumed = fullyConsumed;
        }
    }

    public static class ConditionUpdate {
        private String conditionNotes;

        public String getConditionNotes() {
            return conditionNotes;
        }

        public void setConditionNotes(String conditionNotes) {
            this.conditionNotes = conditionNotes;
        }
    }
}
