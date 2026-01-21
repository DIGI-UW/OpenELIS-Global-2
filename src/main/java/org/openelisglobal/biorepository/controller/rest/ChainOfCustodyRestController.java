package org.openelisglobal.biorepository.controller.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.biorepository.service.ChainOfCustodyService;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Chain of Custody operations in the Biorepository module.
 *
 * Provides read-only endpoints for querying the immutable custody audit trail.
 * Used for: - Viewing custody history for samples - Tracing sample movements -
 * Generating chain-of-custody reports
 */
@RestController
@RequestMapping(value = "/rest/biorepository/custody")
public class ChainOfCustodyRestController extends BaseRestController {

    @Autowired
    private ChainOfCustodyService custodyService;

    /**
     * Get custody history for a sample item.
     */
    @GetMapping(value = "/sample-item/{sampleItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getBySampleItem(
            @PathVariable("sampleItemId") Integer sampleItemId) {

        List<ChainOfCustodyLog> logs = custodyService.getBySampleItemId(sampleItemId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ChainOfCustodyLog log : logs) {
            result.add(mapCustodyLog(log));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get custody history for a retrieval request.
     */
    @GetMapping(value = "/retrieval/{retrievalRequestId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getByRetrievalRequest(
            @PathVariable("retrievalRequestId") Integer retrievalRequestId) {

        List<ChainOfCustodyLog> logs = custodyService.getByRetrievalRequestId(retrievalRequestId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ChainOfCustodyLog log : logs) {
            result.add(mapCustodyLog(log));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get custody history for a transfer-in request.
     */
    @GetMapping(value = "/transfer-in/{transferInRequestId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getByTransferInRequest(
            @PathVariable("transferInRequestId") Integer transferInRequestId) {

        List<ChainOfCustodyLog> logs = custodyService.getByTransferInRequestId(transferInRequestId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ChainOfCustodyLog log : logs) {
            result.add(mapCustodyLog(log));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get custody logs by action type.
     */
    @GetMapping(value = "/by-action/{action}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getByAction(@PathVariable("action") String action) {

        try {
            CustodyAction custodyAction = CustodyAction.valueOf(action);
            List<ChainOfCustodyLog> logs = custodyService.getByAction(custodyAction);
            List<Map<String, Object>> result = new ArrayList<>();

            for (ChainOfCustodyLog log : logs) {
                result.add(mapCustodyLog(log));
            }

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid action type: " + action));
        }
    }

    /**
     * Get recent custody actions.
     */
    @GetMapping(value = "/recent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getRecentActions(@RequestParam(defaultValue = "50") int limit) {

        List<ChainOfCustodyLog> logs = custodyService.getRecentActions(limit);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ChainOfCustodyLog log : logs) {
            result.add(mapCustodyLog(log));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get full lifecycle for a sample (from transfer-in to retrieval).
     */
    @GetMapping(value = "/lifecycle/{sampleItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getFullLifecycle(
            @PathVariable("sampleItemId") Integer sampleItemId,
            @RequestParam(required = false) Integer transferInRequestId,
            @RequestParam(required = false) Integer retrievalRequestId) {

        List<ChainOfCustodyLog> logs = custodyService.getFullLifecycle(sampleItemId, transferInRequestId,
                retrievalRequestId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ChainOfCustodyLog log : logs) {
            result.add(mapCustodyLog(log));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get a single custody log entry.
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getCustodyLog(@PathVariable("id") Integer id) {

        ChainOfCustodyLog log = custodyService.get(id);
        if (log == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapCustodyLog(log));
    }

    /**
     * Get available custody action types.
     */
    @GetMapping(value = "/actions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, String>>> getActionTypes() {

        List<Map<String, String>> actions = new ArrayList<>();
        for (CustodyAction action : CustodyAction.values()) {
            Map<String, String> actionMap = new HashMap<>();
            actionMap.put("value", action.name());
            actionMap.put("label", formatActionLabel(action.name()));
            actions.add(actionMap);
        }
        return ResponseEntity.ok(actions);
    }

    /**
     * Get custody statistics.
     */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStats() {

        Map<String, Object> stats = new HashMap<>();
        stats.put("checkoutRequested", custodyService.countByAction(CustodyAction.CHECKOUT_REQUESTED));
        stats.put("checkoutApproved", custodyService.countByAction(CustodyAction.CHECKOUT_APPROVED));
        stats.put("checkoutRetrieved", custodyService.countByAction(CustodyAction.CHECKOUT_RETRIEVED));
        stats.put("checkoutReleased", custodyService.countByAction(CustodyAction.CHECKOUT_RELEASED));
        stats.put("returnStored", custodyService.countByAction(CustodyAction.RETURN_STORED));

        return ResponseEntity.ok(stats);
    }

    private Map<String, Object> mapCustodyLog(ChainOfCustodyLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("custodyAction", log.getCustodyAction().name());
        map.put("actionTimestamp", log.getActionTimestamp().toString());
        map.put("sampleItemId", log.getSampleItemId());

        if (log.getSampleItem() != null) {
            if (log.getSampleItem().getExternalId() != null) {
                map.put("sampleExternalId", log.getSampleItem().getExternalId());
            }
            if (log.getSampleItem().getSample() != null) {
                map.put("accessionNumber", log.getSampleItem().getSample().getAccessionNumber());
            }
        }

        if (log.getTransferInRequestId() != null) {
            map.put("transferInRequestId", log.getTransferInRequestId());
        }
        if (log.getRetrievalRequestId() != null) {
            map.put("retrievalRequestId", log.getRetrievalRequestId());
        }
        if (log.getFromLocation() != null) {
            map.put("fromLocation", log.getFromLocation());
        }
        if (log.getToLocation() != null) {
            map.put("toLocation", log.getToLocation());
        }
        if (log.getFromCustodian() != null) {
            map.put("fromCustodianName", log.getFromCustodian().getNameForDisplay());
        }
        if (log.getToCustodian() != null) {
            map.put("toCustodianName", log.getToCustodian().getNameForDisplay());
        }
        if (log.getStorageCoordinates() != null) {
            map.put("storageCoordinates", log.getStorageCoordinates());
        }
        if (log.getTemperature() != null) {
            map.put("temperature", log.getTemperature());
        }
        if (log.getNotes() != null) {
            map.put("notes", log.getNotes());
        }

        return map;
    }

    /**
     * Search custody logs with filters and pagination.
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> searchCustodyLogs(
            @RequestParam(required = false) String sampleExternalId, @RequestParam(required = false) String action,
            @RequestParam(required = false) Integer custodianId, @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize) {

        try {
            CustodyAction custodyAction = null;
            if (action != null && !action.trim().isEmpty() && !"ALL".equalsIgnoreCase(action)) {
                custodyAction = CustodyAction.valueOf(action);
            }

            java.sql.Timestamp startTimestamp = null;
            java.sql.Timestamp endTimestamp = null;

            if (startDate != null && !startDate.trim().isEmpty()) {
                startTimestamp = java.sql.Timestamp.valueOf(java.time.LocalDate
                        .parse(startDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay());
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                endTimestamp = java.sql.Timestamp.valueOf(java.time.LocalDate
                        .parse(endDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE).atTime(23, 59, 59));
            }

            List<ChainOfCustodyLog> logs = custodyService.searchCustodyLogs(sampleExternalId, custodyAction,
                    custodianId, startTimestamp, endTimestamp, page, pageSize);

            long totalCount = custodyService.countCustodyLogs(sampleExternalId, custodyAction, custodianId,
                    startTimestamp, endTimestamp);

            List<Map<String, Object>> result = new ArrayList<>();
            for (ChainOfCustodyLog log : logs) {
                result.add(mapCustodyLog(log));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("data", result);
            response.put("totalCount", totalCount);
            response.put("page", page);
            response.put("pageSize", pageSize);
            response.put("totalPages", (int) Math.ceil((double) totalCount / pageSize));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid action type: " + action));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private String formatActionLabel(String action) {
        // Convert CHECKOUT_REQUESTED to "Checkout Requested"
        String[] parts = action.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
