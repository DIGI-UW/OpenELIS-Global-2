package org.openelisglobal.medlab.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.medlab.service.MedLabPatientOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for MedLab patient order operations. Handles patient-order
 * creation and management for the MedLab workflow.
 */
@RestController
@RequestMapping(value = "/rest/medlab")
public class MedLabPatientOrderRestController extends BaseRestController {

    @Autowired
    private MedLabPatientOrderService medLabPatientOrderService;

    /**
     * Creates a new patient order for the MedLab workflow.
     *
     * @param body    request body containing order details
     * @param request HTTP request for user session
     * @return response with created order information
     */
    @PostMapping(value = "/patient-order", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createPatientOrder(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            String patientId = (String) body.get("patientId");
            String labNo = (String) body.get("labNo");
            String requestDate = (String) body.get("requestDate");
            String receivedDate = (String) body.get("receivedDate");
            String priority = (String) body.get("priority");

            @SuppressWarnings("unchecked")
            List<String> testIds = (List<String>) body.get("testIds");

            Integer notebookEntryId = null;
            if (body.get("notebookEntryId") != null) {
                try {
                    notebookEntryId = Integer.valueOf(body.get("notebookEntryId").toString());
                } catch (NumberFormatException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "createPatientOrder",
                            "Invalid notebookEntryId format: " + body.get("notebookEntryId"));
                }
            }
            Integer notebookPageId = null;
            if (body.get("notebookPageId") != null) {
                try {
                    notebookPageId = Integer.valueOf(body.get("notebookPageId").toString());
                } catch (NumberFormatException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "createPatientOrder",
                            "Invalid notebookPageId format: " + body.get("notebookPageId"));
                }
            }

            // Validate required fields
            if (patientId == null || patientId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Patient ID is required"));
            }
            if (labNo == null || labNo.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Lab number is required"));
            }
            if (testIds == null || testIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "At least one test is required"));
            }

            Map<String, Object> result = medLabPatientOrderService.createPatientOrder(patientId, labNo, requestDate,
                    receivedDate, priority, testIds, notebookEntryId, notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error creating order: " + e.getMessage()));
        }
    }

    /**
     * Gets orders for a notebook page.
     *
     * @param pageId the notebook page ID
     * @return list of orders for the page
     */
    @GetMapping(value = "/page/{pageId}/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getOrdersForPage(@PathVariable("pageId") Integer pageId) {

        List<Map<String, Object>> orders = medLabPatientOrderService.getOrdersForPage(pageId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Gets order details by lab number.
     *
     * @param labNo the lab accession number
     * @return order details
     */
    @GetMapping(value = "/order/{labNo}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderByLabNo(@PathVariable("labNo") String labNo) {

        Map<String, Object> result = medLabPatientOrderService.getOrderByLabNo(labNo);

        if (result.containsKey("error")) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Gets orders pending sample collection for a notebook page.
     *
     * @param pageId the notebook page ID
     * @return list of orders with collection status
     */
    @GetMapping(value = "/page/{pageId}/pending-collections", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getPendingCollections(@PathVariable("pageId") Integer pageId) {

        List<Map<String, Object>> orders = medLabPatientOrderService.getOrdersForPage(pageId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Gets orders for a notebook entry (across all pages).
     *
     * @param entryId the notebook entry ID
     * @return list of orders with collection status
     */
    @GetMapping(value = "/entry/{entryId}/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getOrdersForEntry(@PathVariable("entryId") Integer entryId) {

        List<Map<String, Object>> orders = medLabPatientOrderService.getOrdersForEntry(entryId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Records sample collection for an order.
     *
     * @param body    request body containing collection details
     * @param request HTTP request for user session
     * @return response with collection result
     */
    @PostMapping(value = "/sample-collection", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordSampleCollection(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            String labNo = (String) body.get("labNo");
            String sampleTypeId = (String) body.get("sampleTypeId");
            String containerType = (String) body.get("containerType");
            String collectionTime = (String) body.get("collectionTime");
            String collectionDate = (String) body.get("collectionDate");
            String collectorId = (String) body.get("collectorId");
            String volume = (String) body.get("volume");
            String notes = (String) body.get("notes");

            Integer notebookPageId = null;
            if (body.get("notebookPageId") != null) {
                try {
                    notebookPageId = Integer.valueOf(body.get("notebookPageId").toString());
                } catch (NumberFormatException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "recordSampleCollection",
                            "Invalid notebookPageId format: " + body.get("notebookPageId"));
                }
            }

            // Validate required fields
            if (labNo == null || labNo.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Lab number is required"));
            }
            if (sampleTypeId == null || sampleTypeId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sample type is required"));
            }
            if (containerType == null || containerType.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Container type is required"));
            }

            Map<String, Object> result = medLabPatientOrderService.recordSampleCollection(labNo, sampleTypeId,
                    containerType, collectionTime, collectionDate, collectorId, volume, notes, notebookPageId,
                    sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error recording sample collection: " + e.getMessage()));
        }
    }

    /**
     * Gets samples ready for quality check for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of samples ready for QC
     */
    @GetMapping(value = "/entry/{entryId}/samples-for-qc", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSamplesForQC(@PathVariable("entryId") Integer entryId) {

        List<Map<String, Object>> samples = medLabPatientOrderService.getSamplesForQC(entryId);
        return ResponseEntity.ok(samples);
    }

    /**
     * Gets collected samples ready for transport packaging verification. Only
     * returns samples that have been collected (completed Sample Collection page).
     *
     * @param entryId the notebook entry ID
     * @return list of collected samples ready for transport packaging
     */
    @GetMapping(value = "/entry/{entryId}/samples-for-transport", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSamplesForTransport(@PathVariable("entryId") Integer entryId) {

        List<Map<String, Object>> samples = medLabPatientOrderService.getSamplesForTransport(entryId);
        return ResponseEntity.ok(samples);
    }

    /**
     * Records a QC decision (accept or reject) for a sample.
     *
     * @param body    request body containing QC decision details
     * @param request HTTP request for user session
     * @return response with QC decision result
     */
    @PostMapping(value = "/qc-decision", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordQCDecision(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            String labNo = (String) body.get("labNo");
            Boolean accepted = (Boolean) body.get("accepted");
            String rejectionReason = (String) body.get("rejectionReason");

            Integer notebookPageId = null;
            if (body.get("notebookPageId") != null) {
                try {
                    notebookPageId = Integer.valueOf(body.get("notebookPageId").toString());
                } catch (NumberFormatException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "recordQCDecision",
                            "Invalid notebookPageId format: " + body.get("notebookPageId"));
                }
            }

            // Validate required fields
            if (labNo == null || labNo.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Lab number is required"));
            }
            if (accepted == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Accepted flag is required"));
            }

            Map<String, Object> result = medLabPatientOrderService.recordQCDecision(labNo, accepted, rejectionReason,
                    notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error recording QC decision: " + e.getMessage()));
        }
    }

    /**
     * Records bulk QC decisions (accept or reject) for multiple samples.
     *
     * @param body    request body containing bulk QC decision details
     * @param request HTTP request for user session
     * @return response with bulk QC decision result
     */
    @PostMapping(value = "/bulk-qc-decision", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordBulkQCDecision(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<String> labNumbers = (List<String>) body.get("labNumbers");
            Boolean accepted = (Boolean) body.get("accepted");
            String rejectionReason = (String) body.get("rejectionReason");

            Integer notebookPageId = null;
            if (body.get("notebookPageId") != null) {
                try {
                    notebookPageId = Integer.valueOf(body.get("notebookPageId").toString());
                } catch (NumberFormatException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "recordBulkQCDecision",
                            "Invalid notebookPageId format: " + body.get("notebookPageId"));
                }
            }

            // Validate required fields
            if (labNumbers == null || labNumbers.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "At least one lab number is required"));
            }
            if (accepted == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Accepted flag is required"));
            }

            Map<String, Object> result = medLabPatientOrderService.recordBulkQCDecision(labNumbers, accepted,
                    rejectionReason, notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error recording bulk QC decision: " + e.getMessage()));
        }
    }

    // ==================== Sample Routing Endpoints ====================

    /**
     * Gets QC-accepted samples ready for routing decision for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of samples ready for routing
     */
    @GetMapping(value = "/entry/{entryId}/samples-for-routing", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSamplesForRouting(@PathVariable("entryId") Integer entryId) {
        List<Map<String, Object>> samples = medLabPatientOrderService.getSamplesForRouting(entryId);
        return ResponseEntity.ok(samples);
    }

    /**
     * Gets routing summary statistics for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return summary with counts by destination type
     */
    @GetMapping(value = "/entry/{entryId}/routing-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoutingSummary(@PathVariable("entryId") Integer entryId) {
        Map<String, Object> summary = medLabPatientOrderService.getRoutingSummary(entryId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Routes samples to a destination (INTERNAL_ANALYSIS, EXTERNAL_LAB, or
     * STORAGE).
     *
     * @param body    request body containing routing details
     * @param request HTTP request for user session
     * @return response with routing result
     */
    @PostMapping(value = "/route-samples", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> routeSamples(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<Integer> sampleIds = (List<Integer>) body.get("sampleIds");
            String destinationType = (String) body.get("destinationType");
            Integer notebookPageId = body.get("pageId") != null ? ((Number) body.get("pageId")).intValue() : null;

            // Extract metadata (external lab name, storage box, etc.)
            Map<String, Object> metadata = new java.util.HashMap<>();
            if (body.get("externalLabName") != null) {
                metadata.put("externalLabName", body.get("externalLabName"));
            }
            if (body.get("shipmentDate") != null) {
                metadata.put("shipmentDate", body.get("shipmentDate"));
            }
            if (body.get("assayPlate") != null) {
                metadata.put("assayPlate", body.get("assayPlate"));
            }
            // Storage-specific metadata
            if (body.get("storageBoxId") != null) {
                metadata.put("storageBoxId", body.get("storageBoxId"));
            }
            if (body.get("positionCoordinate") != null) {
                metadata.put("positionCoordinate", body.get("positionCoordinate"));
            }
            if (body.get("locationType") != null) {
                metadata.put("locationType", body.get("locationType"));
            }
            if (body.get("storageNotes") != null) {
                metadata.put("storageNotes", body.get("storageNotes"));
            }
            // Well assignments map (sampleId -> wellPosition)
            if (body.get("storageWellAssignments") != null) {
                metadata.put("storageWellAssignments", body.get("storageWellAssignments"));
            }

            if (sampleIds == null || sampleIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sample IDs are required"));
            }

            if (destinationType == null || destinationType.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Destination type is required"));
            }

            Map<String, Object> result = medLabPatientOrderService.routeSamples(sampleIds, destinationType,
                    notebookPageId, metadata, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error routing samples: " + e.getMessage()));
        }
    }

    // ==================== Result Entry Endpoints ====================

    /**
     * Gets samples ready for result entry for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of samples with their tests ready for result entry
     */
    @GetMapping(value = "/entry/{entryId}/samples-for-results", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSamplesForResultEntry(
            @PathVariable("entryId") Integer entryId) {

        List<Map<String, Object>> samples = medLabPatientOrderService.getSamplesForResultEntry(entryId);
        return ResponseEntity.ok(samples);
    }

    /**
     * Saves a test result for a sample with enhanced data.
     *
     * @param body    request body containing result details
     * @param request HTTP request for user session
     * @return response with save result including flag
     */
    @PostMapping(value = "/result-entry", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveTestResult(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            String labNo = (String) body.get("labNo");
            String testId = (String) body.get("testId");
            String resultValue = (String) body.get("resultValue");
            String unit = (String) body.get("unit");
            String entryType = (String) body.get("entryType");
            String notes = (String) body.get("notes");

            Integer notebookPageId = null;
            if (body.get("notebookPageId") != null) {
                try {
                    notebookPageId = Integer.valueOf(body.get("notebookPageId").toString());
                } catch (NumberFormatException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "saveTestResult",
                            "Invalid notebookPageId format: " + body.get("notebookPageId"));
                }
            }

            // Validate required fields
            if (labNo == null || labNo.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Lab number is required"));
            }
            if (testId == null || testId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Test ID is required"));
            }
            if (resultValue == null || resultValue.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Result value is required"));
            }

            // Default entry type if not provided
            if (entryType == null || entryType.isEmpty()) {
                entryType = "MANUAL";
            }

            Map<String, Object> result = medLabPatientOrderService.saveTestResult(labNo, testId, resultValue, unit,
                    entryType, notes, notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error saving result: " + e.getMessage()));
        }
    }

    /**
     * Saves multiple test results in bulk (for analyzer import).
     *
     * @param body    request body containing bulk results
     * @param request HTTP request for user session
     * @return response with import counts
     */
    @PostMapping(value = "/bulk-result-entry", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveBulkResults(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
            String entryType = (String) body.get("entryType");
            String analyzerName = (String) body.get("analyzerName");
            String runId = (String) body.get("runId");

            Integer notebookPageId = null;
            if (body.get("notebookPageId") != null) {
                try {
                    notebookPageId = Integer.valueOf(body.get("notebookPageId").toString());
                } catch (NumberFormatException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "saveBulkResults",
                            "Invalid notebookPageId format: " + body.get("notebookPageId"));
                }
            }

            if (results == null || results.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No results provided"));
            }

            // Default entry type if not provided
            if (entryType == null || entryType.isEmpty()) {
                entryType = "AUTO_IMPORT";
            }

            Map<String, Object> result = medLabPatientOrderService.saveBulkResults(results, entryType, analyzerName,
                    runId, notebookPageId, sysUserId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error importing results: " + e.getMessage()));
        }
    }

    /**
     * Marks samples as complete on the result entry page.
     *
     * @param body    request body containing sample IDs
     * @param request HTTP request for user session
     * @return response with update count
     */
    @PostMapping(value = "/result-entry/mark-complete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markResultEntryComplete(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<Integer> sampleIds = (List<Integer>) body.get("sampleIds");

            Integer notebookPageId = null;
            if (body.get("notebookPageId") != null) {
                try {
                    notebookPageId = Integer.valueOf(body.get("notebookPageId").toString());
                } catch (NumberFormatException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "markResultEntryComplete",
                            "Invalid notebookPageId format: " + body.get("notebookPageId"));
                }
            }

            if (sampleIds == null || sampleIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No sample IDs provided"));
            }

            Map<String, Object> result = medLabPatientOrderService.markResultEntryComplete(sampleIds, notebookPageId,
                    sysUserId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error marking samples complete: " + e.getMessage()));
        }
    }

    // ==================== Result Verification Endpoints ====================

    /**
     * Gets results pending verification for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of results pending verification
     */
    @GetMapping(value = "/entry/{entryId}/results-for-verification", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getResultsForVerification(
            @PathVariable("entryId") Integer entryId) {

        List<Map<String, Object>> results = medLabPatientOrderService.getResultsForVerification(entryId);
        return ResponseEntity.ok(results);
    }

    /**
     * Verifies (approves or rejects) a test result.
     *
     * @param body    request body containing verification details
     * @param request HTTP request for user session
     * @return response with verification result
     */
    @PostMapping(value = "/verify-result", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyResult(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            String labNo = (String) body.get("labNo");
            String testId = (String) body.get("testId");
            Boolean approved = (Boolean) body.get("approved");
            String comments = (String) body.get("comments");

            Integer notebookPageId = null;
            if (body.get("notebookPageId") != null) {
                try {
                    notebookPageId = Integer.valueOf(body.get("notebookPageId").toString());
                } catch (NumberFormatException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "verifyResult",
                            "Invalid notebookPageId format: " + body.get("notebookPageId"));
                }
            }

            // Validate required fields
            if (labNo == null || labNo.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Lab number is required"));
            }
            if (testId == null || testId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Test ID is required"));
            }
            if (approved == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Approved flag is required"));
            }

            Map<String, Object> result = medLabPatientOrderService.verifyResult(labNo, testId, approved, comments,
                    notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error verifying result: " + e.getMessage()));
        }
    }

    // ==================== Reporting Endpoints ====================

    /**
     * Gets results ready for reporting for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of results ready for reporting
     */
    @GetMapping(value = "/entry/{entryId}/results-for-reporting", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getResultsForReporting(@PathVariable("entryId") Integer entryId) {

        List<Map<String, Object>> results = medLabPatientOrderService.getResultsForReporting(entryId);
        return ResponseEntity.ok(results);
    }

    /**
     * Marks a result as reported/delivered.
     *
     * @param body    request body containing reporting details
     * @param request HTTP request for user session
     * @return response with reporting result
     */
    @PostMapping(value = "/mark-reported", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markResultReported(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            String labNo = (String) body.get("labNo");
            String deliveryMethod = (String) body.get("deliveryMethod");
            String recipient = (String) body.get("recipient");

            Integer notebookPageId = null;
            if (body.get("notebookPageId") != null) {
                try {
                    notebookPageId = Integer.valueOf(body.get("notebookPageId").toString());
                } catch (NumberFormatException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "markResultReported",
                            "Invalid notebookPageId format: " + body.get("notebookPageId"));
                }
            }

            // Validate required fields
            if (labNo == null || labNo.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Lab number is required"));
            }

            Map<String, Object> result = medLabPatientOrderService.markResultReported(labNo, deliveryMethod, recipient,
                    notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error marking as reported: " + e.getMessage()));
        }
    }

    // ==================== Storage Endpoints ====================

    /**
     * Gets samples ready for storage assignment for a notebook entry. Returns
     * samples that have passed QC.
     *
     * @param entryId the notebook entry ID
     * @return list of samples ready for storage
     */
    @GetMapping(value = "/entry/{entryId}/samples-for-storage", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSamplesForStorage(@PathVariable("entryId") Integer entryId) {
        List<Map<String, Object>> samples = medLabPatientOrderService.getSamplesForStorage(entryId);
        return ResponseEntity.ok(samples);
    }

    /**
     * Assigns samples to storage locations.
     *
     * @param entryId the notebook entry ID
     * @param body    request body containing storage assignment details
     * @param request HTTP request for user session
     * @return response with assignment result
     */
    @PostMapping(value = "/entry/{entryId}/assign-storage", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignSamplesToStorage(@PathVariable("entryId") Integer entryId,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<Integer> sampleIds = (List<Integer>) body.get("sampleIds");
            Integer boxId = body.get("boxId") != null ? ((Number) body.get("boxId")).intValue() : null;
            String boxLabel = (String) body.get("boxLabel");

            @SuppressWarnings("unchecked")
            Map<String, Object> wellAssignmentsRaw = (Map<String, Object>) body.get("wellAssignments");
            Map<Integer, String> wellAssignments = new java.util.HashMap<>();
            if (wellAssignmentsRaw != null) {
                wellAssignmentsRaw.forEach((key, value) -> {
                    wellAssignments.put(Integer.valueOf(key.toString()), (String) value);
                });
            }

            String condition = (String) body.get("condition");
            Integer retentionYears = body.get("retentionYears") != null
                    ? ((Number) body.get("retentionYears")).intValue()
                    : 5;
            String cryovialId = (String) body.get("cryovialId");

            Integer notebookPageId = null;
            if (body.get("notebookPageId") != null) {
                notebookPageId = Integer.valueOf(body.get("notebookPageId").toString());
            }

            if (sampleIds == null || sampleIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sample IDs are required"));
            }

            Map<String, Object> result = medLabPatientOrderService.assignSamplesToStorage(sampleIds, boxId, boxLabel,
                    wellAssignments, condition, retentionYears, cryovialId, notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error assigning samples to storage: " + e.getMessage()));
        }
    }

    /**
     * Records an environmental temperature reading.
     *
     * @param entryId the notebook entry ID
     * @param body    request body containing reading details
     * @param request HTTP request for user session
     * @return response with save result
     */
    @PostMapping(value = "/entry/{entryId}/environmental-reading", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordEnvironmentalReading(@PathVariable("entryId") Integer entryId,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            Integer deviceId = body.get("deviceId") != null ? ((Number) body.get("deviceId")).intValue() : null;
            Double temperatureReading = body.get("temperatureReading") != null
                    ? ((Number) body.get("temperatureReading")).doubleValue()
                    : null;
            String readingTime = (String) body.get("readingTime");
            String readingPeriod = (String) body.get("readingPeriod");
            String recordedBy = (String) body.get("recordedBy");
            Boolean alarmTriggered = (Boolean) body.get("alarmTriggered");
            String notes = (String) body.get("notes");

            if (temperatureReading == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Temperature reading is required"));
            }

            Map<String, Object> result = medLabPatientOrderService.recordEnvironmentalReading(entryId, deviceId,
                    temperatureReading, readingTime, readingPeriod, recordedBy, alarmTriggered, notes, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error recording environmental reading: " + e.getMessage()));
        }
    }

    /**
     * Gets environmental readings for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of environmental readings
     */
    @GetMapping(value = "/entry/{entryId}/environmental-readings", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getEnvironmentalReadings(
            @PathVariable("entryId") Integer entryId) {
        List<Map<String, Object>> readings = medLabPatientOrderService.getEnvironmentalReadings(entryId);
        return ResponseEntity.ok(readings);
    }

    // ==================== Sample Processing Endpoints ====================

    /**
     * Gets samples ready for processing (QC-accepted samples).
     *
     * @param entryId the notebook entry ID
     * @return list of samples ready for processing
     */
    @GetMapping(value = "/entry/{entryId}/samples-for-processing", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSamplesForProcessing(@PathVariable("entryId") Integer entryId) {
        List<Map<String, Object>> samples = medLabPatientOrderService.getSamplesForProcessing(entryId);
        return ResponseEntity.ok(samples);
    }

    /**
     * Records sample processing details.
     *
     * @param entryId the notebook entry ID
     * @param body    request body containing processing details
     * @param request HTTP request for user session
     * @return response with processing result
     */
    @PostMapping(value = "/entry/{entryId}/record-processing", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordProcessing(@PathVariable("entryId") Integer entryId,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<Integer> sampleIds = (List<Integer>) body.get("sampleIds");
            String processingType = (String) body.get("processingType");
            String derivedMaterial = (String) body.get("derivedMaterial");
            String notes = (String) body.get("notes");
            Boolean isBioequivalence = (Boolean) body.get("isBioequivalence");
            Boolean transferToBioanalytical = (Boolean) body.get("transferToBioanalytical");
            Integer notebookPageId = body.get("notebookPageId") != null
                    ? ((Number) body.get("notebookPageId")).intValue()
                    : null;

            if (sampleIds == null || sampleIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sample IDs are required"));
            }

            if (processingType == null || processingType.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Processing type is required"));
            }

            Map<String, Object> result = medLabPatientOrderService.recordProcessing(sampleIds, processingType,
                    derivedMaterial, notes, isBioequivalence, transferToBioanalytical, notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error recording processing: " + e.getMessage()));
        }
    }

    /**
     * Creates aliquots (child samples) from parent samples.
     *
     * @param entryId the notebook entry ID
     * @param body    request body containing aliquot creation details
     * @param request HTTP request for user session
     * @return response with created aliquots
     */
    @PostMapping(value = "/entry/{entryId}/create-aliquots", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createAliquots(@PathVariable("entryId") Integer entryId,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            // Convert parentSampleIds - may be Strings or Numbers from JSON
            List<?> rawIds = (List<?>) body.get("parentSampleIds");
            List<Integer> parentSampleIds = null;
            if (rawIds != null) {
                parentSampleIds = rawIds.stream()
                        .map(id -> id instanceof Number ? ((Number) id).intValue() : Integer.parseInt(id.toString()))
                        .collect(java.util.stream.Collectors.toList());
            }
            int childCountPerParent = body.get("childCountPerParent") != null
                    ? (body.get("childCountPerParent") instanceof Number
                            ? ((Number) body.get("childCountPerParent")).intValue()
                            : Integer.parseInt(body.get("childCountPerParent").toString()))
                    : 1;
            String externalIdPrefix = (String) body.get("externalIdPrefix");
            String containerType = (String) body.get("containerType");
            Integer notebookPageId = null;
            if (body.get("notebookPageId") != null) {
                Object pageIdObj = body.get("notebookPageId");
                notebookPageId = pageIdObj instanceof Number ? ((Number) pageIdObj).intValue()
                        : Integer.parseInt(pageIdObj.toString());
            }

            if (parentSampleIds == null || parentSampleIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Parent sample IDs are required"));
            }

            Map<String, Object> result = medLabPatientOrderService.createAliquots(parentSampleIds, childCountPerParent,
                    externalIdPrefix, containerType, notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error creating aliquots: " + e.getMessage()));
        }
    }

    // ==================== Testing & Analyzer Endpoints ====================

    /**
     * Gets samples ready for testing (samples that passed QC).
     *
     * @param entryId the notebook entry ID
     * @return list of samples ready for testing
     */
    @GetMapping(value = "/entry/{entryId}/samples-for-testing", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSamplesForTesting(@PathVariable("entryId") Integer entryId) {
        List<Map<String, Object>> samples = medLabPatientOrderService.getSamplesForTesting(entryId);
        return ResponseEntity.ok(samples);
    }

    /**
     * Executes tests on selected samples.
     *
     * @param entryId the notebook entry ID
     * @param body    request body containing test execution details
     * @param request HTTP request for user session
     * @return response with test execution result
     */
    @PostMapping(value = "/entry/{entryId}/execute-tests", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> executeTests(@PathVariable("entryId") Integer entryId,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<Integer> sampleIds = (List<Integer>) body.get("sampleIds");
            String analyzerId = (String) body.get("analyzerId");
            String analyzerName = (String) body.get("analyzerName");
            Boolean worklistGenerated = (Boolean) body.get("worklistGenerated");
            Boolean isManualTest = (Boolean) body.get("isManualTest");
            String technologyUsed = (String) body.get("technologyUsed");
            Integer notebookPageId = body.get("notebookPageId") != null
                    ? ((Number) body.get("notebookPageId")).intValue()
                    : null;

            if (sampleIds == null || sampleIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sample IDs are required"));
            }

            Map<String, Object> result = medLabPatientOrderService.executeTests(sampleIds, analyzerId, analyzerName,
                    worklistGenerated, isManualTest, technologyUsed, notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error executing tests: " + e.getMessage()));
        }
    }

    /**
     * Assigns tests to samples that don't have Analysis records.
     *
     * @param entryId the notebook entry ID
     * @param body    request body containing sampleItemIds and testIds
     * @param request HTTP request for user session
     * @return response with assignment result
     */
    @PostMapping(value = "/entry/{entryId}/assign-tests", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignTestsToSamples(@PathVariable("entryId") Integer entryId,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<Integer> sampleItemIds = (List<Integer>) body.get("sampleItemIds");
            @SuppressWarnings("unchecked")
            List<String> testIds = (List<String>) body.get("testIds");
            Integer notebookPageId = body.get("notebookPageId") != null
                    ? ((Number) body.get("notebookPageId")).intValue()
                    : null;

            if (sampleItemIds == null || sampleItemIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sample IDs are required"));
            }

            if (testIds == null || testIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Test IDs are required"));
            }

            Map<String, Object> result = medLabPatientOrderService.assignTestsToSamples(sampleItemIds, testIds,
                    notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error assigning tests: " + e.getMessage()));
        }
    }

    /**
     * Records a QC result.
     *
     * @param entryId the notebook entry ID
     * @param body    request body containing QC details
     * @param request HTTP request for user session
     * @return response with QC recording result
     */
    @PostMapping(value = "/entry/{entryId}/record-qc", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordQc(@PathVariable("entryId") Integer entryId,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            String analyzerId = (String) body.get("analyzerId");
            String analyzerName = (String) body.get("analyzerName");
            String qcType = (String) body.get("qcType");
            String qcLevel = (String) body.get("qcLevel");
            String qcResult = (String) body.get("qcResult");
            String calibrationStatus = (String) body.get("calibrationStatus");
            String notes = (String) body.get("notes");
            Integer notebookPageId = body.get("notebookPageId") != null
                    ? ((Number) body.get("notebookPageId")).intValue()
                    : null;

            if (qcType == null || qcType.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "QC type is required"));
            }

            if (qcResult == null || qcResult.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "QC result is required"));
            }

            Map<String, Object> result = medLabPatientOrderService.recordQc(analyzerId, analyzerName, qcType, qcLevel,
                    qcResult, calibrationStatus, notes, notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error recording QC: " + e.getMessage()));
        }
    }

    /**
     * Gets QC records for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of QC records
     */
    @GetMapping(value = "/entry/{entryId}/qc-records", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getQcRecords(@PathVariable("entryId") Integer entryId) {
        List<Map<String, Object>> qcRecords = medLabPatientOrderService.getQcRecords(entryId);
        return ResponseEntity.ok(qcRecords);
    }

    /**
     * Records a deviation/error.
     *
     * @param entryId the notebook entry ID
     * @param body    request body containing deviation details
     * @param request HTTP request for user session
     * @return response with deviation recording result
     */
    @PostMapping(value = "/entry/{entryId}/record-deviation", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordDeviation(@PathVariable("entryId") Integer entryId,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<Integer> sampleIds = (List<Integer>) body.get("sampleIds");
            String deviationType = (String) body.get("deviationType");
            String actionTaken = (String) body.get("actionTaken");
            String rootCauseAnalysis = (String) body.get("rootCauseAnalysis");
            String notes = (String) body.get("notes");
            Integer notebookPageId = body.get("notebookPageId") != null
                    ? ((Number) body.get("notebookPageId")).intValue()
                    : null;

            if (deviationType == null || deviationType.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Deviation type is required"));
            }

            Map<String, Object> result = medLabPatientOrderService.recordDeviation(sampleIds, deviationType,
                    actionTaken, rootCauseAnalysis, notes, notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error recording deviation: " + e.getMessage()));
        }
    }

    /**
     * Gets deviations for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of deviation records
     */
    @GetMapping(value = "/entry/{entryId}/deviations", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getDeviations(@PathVariable("entryId") Integer entryId) {
        List<Map<String, Object>> deviations = medLabPatientOrderService.getDeviations(entryId);
        return ResponseEntity.ok(deviations);
    }

    // ==================== Performance Monitoring Endpoints ====================

    /**
     * Gets performance dashboard data for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return dashboard data with metrics
     */
    @GetMapping(value = "/entry/{entryId}/performance-dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPerformanceDashboard(@PathVariable("entryId") Integer entryId) {
        Map<String, Object> dashboard = medLabPatientOrderService.getPerformanceDashboard(entryId);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Gets turnaround time statistics for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return TAT statistics
     */
    @GetMapping(value = "/entry/{entryId}/tat-stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTurnaroundTimeStats(@PathVariable("entryId") Integer entryId) {
        Map<String, Object> stats = medLabPatientOrderService.getTurnaroundTimeStats(entryId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Gets sample acceptance statistics for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return acceptance rate statistics
     */
    @GetMapping(value = "/entry/{entryId}/acceptance-stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSampleAcceptanceStats(@PathVariable("entryId") Integer entryId) {
        Map<String, Object> stats = medLabPatientOrderService.getSampleAcceptanceStats(entryId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Gets QC performance trends for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return QC trends by analyzer
     */
    @GetMapping(value = "/entry/{entryId}/qc-trends", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getQcPerformanceTrends(@PathVariable("entryId") Integer entryId) {
        List<Map<String, Object>> trends = medLabPatientOrderService.getQcPerformanceTrends(entryId);
        return ResponseEntity.ok(trends);
    }

    /**
     * Gets equipment usage statistics for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return equipment usage stats
     */
    @GetMapping(value = "/entry/{entryId}/equipment-stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getEquipmentUsageStats(@PathVariable("entryId") Integer entryId) {
        List<Map<String, Object>> stats = medLabPatientOrderService.getEquipmentUsageStats(entryId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Gets sample utilization report for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return sample utilization data
     */
    @GetMapping(value = "/entry/{entryId}/utilization-report", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSampleUtilizationReport(@PathVariable("entryId") Integer entryId) {
        Map<String, Object> report = medLabPatientOrderService.getSampleUtilizationReport(entryId);
        return ResponseEntity.ok(report);
    }

    /**
     * Gets corrective actions log for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of corrective actions
     */
    @GetMapping(value = "/entry/{entryId}/corrective-actions", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getCorrectiveActionsLog(@PathVariable("entryId") Integer entryId) {
        List<Map<String, Object>> actions = medLabPatientOrderService.getCorrectiveActionsLog(entryId);
        return ResponseEntity.ok(actions);
    }

    /**
     * Modifies reference range for a test (supervisor action).
     *
     * @param request HTTP request
     * @param body    request body with reference range data
     * @return result of the operation
     */
    @PostMapping(value = "/modify-reference-range", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> modifyReferenceRange(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {

        try {
            String sysUserId = getSysUserId(request);

            String testId = (String) body.get("testId");
            Double lowNormal = body.get("lowNormal") != null ? ((Number) body.get("lowNormal")).doubleValue() : null;
            Double highNormal = body.get("highNormal") != null ? ((Number) body.get("highNormal")).doubleValue() : null;
            Double lowCritical = body.get("lowCritical") != null ? ((Number) body.get("lowCritical")).doubleValue()
                    : null;
            Double highCritical = body.get("highCritical") != null ? ((Number) body.get("highCritical")).doubleValue()
                    : null;
            Integer notebookPageId = body.get("notebookPageId") != null
                    ? ((Number) body.get("notebookPageId")).intValue()
                    : null;

            if (testId == null || testId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Test ID is required"));
            }

            Map<String, Object> result = medLabPatientOrderService.modifyReferenceRange(testId, lowNormal, highNormal,
                    lowCritical, highCritical, notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error modifying reference range: " + e.getMessage()));
        }
    }

    /**
     * Marks verification complete for samples.
     *
     * @param request HTTP request
     * @param body    request body with sample IDs
     * @return result of the operation
     */
    @PostMapping(value = "/verification/mark-complete", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markVerificationComplete(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {

        try {
            String sysUserId = getSysUserId(request);

            @SuppressWarnings("unchecked")
            List<Integer> sampleIds = (List<Integer>) body.get("sampleIds");
            Integer notebookPageId = body.get("notebookPageId") != null
                    ? ((Number) body.get("notebookPageId")).intValue()
                    : null;

            if (sampleIds == null || sampleIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sample IDs are required"));
            }

            Map<String, Object> result = medLabPatientOrderService.markVerificationComplete(sampleIds, notebookPageId,
                    sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error marking verification complete: " + e.getMessage()));
        }
    }

    // ==================== Disposal & Archiving Endpoints ====================

    /**
     * Gets samples available for disposal.
     *
     * @param entryId the notebook entry ID
     * @return list of samples that can be disposed
     */
    @GetMapping(value = "/entry/{entryId}/samples-for-disposal", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSamplesForDisposal(@PathVariable("entryId") Integer entryId) {
        List<Map<String, Object>> samples = medLabPatientOrderService.getSamplesForDisposal(entryId);
        return ResponseEntity.ok(samples);
    }

    /**
     * Records disposal of samples.
     *
     * @param body    request body containing disposal details
     * @param request HTTP request for user session
     * @return response with disposal result
     */
    @PostMapping(value = "/record-disposal", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordDisposal(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<Integer> sampleIds = (List<Integer>) body.get("sampleIds");
            String disposalReason = (String) body.get("disposalReason");
            String disposalMethod = (String) body.get("disposalMethod");
            String disposalDate = (String) body.get("disposalDate");
            String responsiblePerson = (String) body.get("responsiblePerson");
            String facilityDetails = (String) body.get("facilityDetails");
            String notes = (String) body.get("notes");
            Integer notebookPageId = body.get("notebookPageId") != null
                    ? ((Number) body.get("notebookPageId")).intValue()
                    : null;

            if (sampleIds == null || sampleIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sample IDs are required"));
            }

            Map<String, Object> result = medLabPatientOrderService.recordDisposal(sampleIds, disposalReason,
                    disposalMethod, disposalDate, responsiblePerson, facilityDetails, notes, notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error recording disposal: " + e.getMessage()));
        }
    }

    /**
     * Gets disposal records for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of disposal records
     */
    @GetMapping(value = "/entry/{entryId}/disposal-records", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getDisposalRecords(@PathVariable("entryId") Integer entryId) {
        List<Map<String, Object>> records = medLabPatientOrderService.getDisposalRecords(entryId);
        return ResponseEntity.ok(records);
    }

    /**
     * Records archiving of samples.
     *
     * @param body    request body containing archiving details
     * @param request HTTP request for user session
     * @return response with archiving result
     */
    @PostMapping(value = "/record-archiving", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordArchiving(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<Integer> sampleIds = (List<Integer>) body.get("sampleIds");
            Integer retentionYears = body.get("retentionYears") != null
                    ? ((Number) body.get("retentionYears")).intValue()
                    : 2;
            String storageCondition = (String) body.get("storageCondition");
            Boolean transferToBiobank = (Boolean) body.get("transferToBiobank");
            String biobankDetails = (String) body.get("biobankDetails");
            String notes = (String) body.get("notes");
            Integer notebookPageId = body.get("notebookPageId") != null
                    ? ((Number) body.get("notebookPageId")).intValue()
                    : null;

            if (sampleIds == null || sampleIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sample IDs are required"));
            }

            Map<String, Object> result = medLabPatientOrderService.recordArchiving(sampleIds, retentionYears,
                    storageCondition, transferToBiobank, biobankDetails, notes, notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error recording archiving: " + e.getMessage()));
        }
    }

    /**
     * Gets archiving records for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of archiving records
     */
    @GetMapping(value = "/entry/{entryId}/archiving-records", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getArchivingRecords(@PathVariable("entryId") Integer entryId) {
        List<Map<String, Object>> records = medLabPatientOrderService.getArchivingRecords(entryId);
        return ResponseEntity.ok(records);
    }

    /**
     * Gets disposal and archiving summary for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return summary data for disposal/archiving dashboard
     */
    @GetMapping(value = "/entry/{entryId}/disposal-archiving-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDisposalArchivingSummary(@PathVariable("entryId") Integer entryId) {
        Map<String, Object> summary = medLabPatientOrderService.getDisposalArchivingSummary(entryId);
        return ResponseEntity.ok(summary);
    }

    // ==================== Accreditation Support Endpoints ====================

    /**
     * Gets audit trail for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of audit trail records
     */
    @GetMapping(value = "/entry/{entryId}/audit-trail", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAuditTrail(@PathVariable("entryId") Integer entryId) {
        List<Map<String, Object>> auditTrail = medLabPatientOrderService.getAuditTrail(entryId);
        return ResponseEntity.ok(auditTrail);
    }

    /**
     * Gets SOP compliance status for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return SOP compliance status data
     */
    @GetMapping(value = "/entry/{entryId}/sop-compliance", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSopComplianceStatus(@PathVariable("entryId") Integer entryId) {
        Map<String, Object> status = medLabPatientOrderService.getSopComplianceStatus(entryId);
        return ResponseEntity.ok(status);
    }

    /**
     * Records SOP completion.
     *
     * @param body    request body containing SOP completion details
     * @param request HTTP request for user session
     * @return response with recording result
     */
    @PostMapping(value = "/record-sop-completion", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordSopCompletion(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            String sopId = (String) body.get("sopId");
            String completedBy = (String) body.get("completedBy");
            String completionDate = (String) body.get("completionDate");
            String notes = (String) body.get("notes");
            Integer notebookPageId = body.get("notebookPageId") != null
                    ? ((Number) body.get("notebookPageId")).intValue()
                    : null;

            if (sopId == null || sopId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "SOP ID is required"));
            }

            Map<String, Object> result = medLabPatientOrderService.recordSopCompletion(sopId, completedBy,
                    completionDate, notes, notebookPageId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error recording SOP completion: " + e.getMessage()));
        }
    }

    /**
     * Finalizes a notebook entry (prevents further modifications).
     *
     * @param entryId the notebook entry ID
     * @param request HTTP request for user session
     * @return response with finalization result
     */
    @PostMapping(value = "/entry/{entryId}/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> finalizeNotebookEntry(@PathVariable("entryId") Integer entryId,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            Map<String, Object> result = medLabPatientOrderService.finalizeNotebookEntry(entryId, sysUserId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error finalizing notebook entry: " + e.getMessage()));
        }
    }

    @Override
    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        if (usd == null) {
            return null;
        }
        return String.valueOf(usd.getSystemUserId());
    }
}
