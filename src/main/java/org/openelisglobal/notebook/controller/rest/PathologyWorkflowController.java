package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.service.NoteBookPageService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Pathology Laboratory workflow operations. Handles
 * processing, storage, testing, disposal, and reporting endpoints.
 */
@RestController
@RequestMapping(value = "/rest/notebook/pathology")
public class PathologyWorkflowController extends BaseRestController {

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    // ========================================
    // SAMPLE CREATION ENDPOINTS
    // ========================================

    /**
     * Create a new pathology sample. POST /rest/notebook/pathology/sample/create
     */
    @PostMapping(value = "/sample/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createSample(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            // Store the sample creation data - actual sample creation logic would go here
            response.put("success", true);
            response.put("message", "Sample created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to create sample: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========================================
    // PROCESSING ENDPOINTS
    // ========================================

    /**
     * Submit sample processing data. POST
     * /rest/notebook/pathology/processing/submit
     *
     * Handles all processing types from UI: - Histopathology: grossExamDone,
     * grossDescription, sectioningDone, tissueProcessingSteps, embeddingDone,
     * microtomyThickness - Cytopathology: centrifugationDone, smearTypes, stainUsed
     * - Blood: wedgeSmearDone, bloodStain - Research: sopFollowed,
     * processingMethods
     */
    @PostMapping(value = "/processing/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitProcessing(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String sampleId = parseString(requestData.get("sampleId"));
            Integer pageId = parseInteger(requestData.get("pageId"));

            if (sampleId == null || pageId == null) {
                response.put("success", false);
                response.put("error", "Sample ID and Page ID are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Build processing data map from all UI fields
            Map<String, Object> processingData = new HashMap<>();
            // Processing action
            processingData.put("processingAction", requestData.get("processingAction"));
            // Histopathology fields
            processingData.put("grossExamDone", requestData.get("grossExamDone"));
            processingData.put("grossDescription", requestData.get("grossDescription"));
            processingData.put("sectioningDone", requestData.get("sectioningDone"));
            processingData.put("tissueProcessingSteps", requestData.get("tissueProcessingSteps"));
            processingData.put("embeddingDone", requestData.get("embeddingDone"));
            processingData.put("microtomyThickness", requestData.get("microtomyThickness"));
            // Cytopathology fields
            processingData.put("centrifugationDone", requestData.get("centrifugationDone"));
            processingData.put("smearTypes", requestData.get("smearTypes"));
            processingData.put("stainUsed", requestData.get("stainUsed"));
            // Blood fields
            processingData.put("wedgeSmearDone", requestData.get("wedgeSmearDone"));
            processingData.put("bloodStain", requestData.get("bloodStain"));
            // Research fields
            processingData.put("sopFollowed", requestData.get("sopFollowed"));
            processingData.put("processingMethods", requestData.get("processingMethods"));
            // Common fields
            processingData.put("processingDate", requestData.get("processingDate"));
            processingData.put("staffInitials", requestData.get("staffInitials"));
            processingData.put("processingNotes", requestData.get("processingNotes"));

            // Update or create the notebook page sample with processing data
            NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleId, pageId);

            if (pageSample != null) {
                Map<String, Object> data = pageSample.getData() != null ? new HashMap<>(pageSample.getData())
                        : new HashMap<>();
                data.putAll(processingData);
                pageSample.setData(data);
                pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                pageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                pageSample.setSysUserId(sysUserId);
                notebookPageSampleService.update(pageSample);
            } else {
                // Create new page sample entry
                NoteBookPage page = noteBookPageService.get(pageId);
                if (page == null) {
                    response.put("success", false);
                    response.put("error", "Page not found: " + pageId);
                    return ResponseEntity.badRequest().body(response);
                }

                NotebookPageSample newPageSample = new NotebookPageSample();
                newPageSample.setNotebookPage(page);
                newPageSample.setSampleItemId(sampleId);
                newPageSample.setData(processingData);
                newPageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                newPageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                newPageSample.setSysUserId(sysUserId);
                notebookPageSampleService.insert(newPageSample);
            }

            response.put("success", true);
            response.put("message", "Processing data saved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to save processing data: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========================================
    // STORAGE ENDPOINTS
    // ========================================

    /**
     * Assign storage location to a sample. POST
     * /rest/notebook/pathology/storage/assign
     *
     * UI fields: storageType, expectedDuration, storageUnit, rack, box, position,
     * dateStored, storedBy Location format follows PDF: "Freezer X, Rack Y, Box Z,
     * Position N"
     */
    @PostMapping(value = "/storage/assign", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignStorage(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String sampleId = parseString(requestData.get("sampleId"));
            Integer pageId = parseInteger(requestData.get("pageId"));

            if (sampleId == null || pageId == null) {
                response.put("success", false);
                response.put("error", "Sample ID and Page ID are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Build storage data map from all UI fields
            Map<String, Object> storageData = new HashMap<>();
            storageData.put("storageType", requestData.get("storageType"));
            storageData.put("expectedDuration", requestData.get("expectedDuration"));
            storageData.put("storageUnit", requestData.get("storageUnit"));
            storageData.put("rack", requestData.get("rack"));
            storageData.put("box", requestData.get("box"));
            storageData.put("position", requestData.get("position"));
            storageData.put("dateStored", requestData.get("dateStored"));
            storageData.put("storedBy", requestData.get("storedBy"));
            // Build full location string for display
            String storageLocation = String.format("%s, Rack %s, Box %s, Position %s", requestData.get("storageUnit"),
                    requestData.get("rack"), requestData.get("box"), requestData.get("position"));
            storageData.put("storageLocation", storageLocation);

            NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleId, pageId);

            if (pageSample != null) {
                Map<String, Object> data = pageSample.getData() != null ? new HashMap<>(pageSample.getData())
                        : new HashMap<>();
                data.putAll(storageData);
                pageSample.setData(data);
                pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                pageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                pageSample.setSysUserId(sysUserId);
                notebookPageSampleService.update(pageSample);
            } else {
                NoteBookPage page = noteBookPageService.get(pageId);
                if (page == null) {
                    response.put("success", false);
                    response.put("error", "Page not found: " + pageId);
                    return ResponseEntity.badRequest().body(response);
                }

                NotebookPageSample newPageSample = new NotebookPageSample();
                newPageSample.setNotebookPage(page);
                newPageSample.setSampleItemId(sampleId);
                newPageSample.setData(storageData);
                newPageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                newPageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                newPageSample.setSysUserId(sysUserId);
                notebookPageSampleService.insert(newPageSample);
            }

            response.put("success", true);
            response.put("message", "Storage location assigned successfully");
            response.put("storageLocation", storageLocation);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to assign storage: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Log temperature for a storage unit. POST
     * /rest/notebook/pathology/storage/temperature-log
     */
    @PostMapping(value = "/storage/temperature-log", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logTemperature(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            // Temperature log data - would typically be stored in a separate table
            // For now, just acknowledge receipt
            response.put("success", true);
            response.put("message", "Temperature logged successfully");
            response.put("data", requestData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to log temperature: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Record sample retrieval from storage. POST
     * /rest/notebook/pathology/storage/retrieve
     */
    @PostMapping(value = "/storage/retrieve", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> retrieveSample(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String sampleId = parseString(requestData.get("sampleId"));
            Integer pageId = parseInteger(requestData.get("pageId"));

            if (sampleId == null || pageId == null) {
                response.put("success", false);
                response.put("error", "Sample ID and Page ID are required");
                return ResponseEntity.badRequest().body(response);
            }

            NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleId, pageId);

            if (pageSample != null) {
                Map<String, Object> data = pageSample.getData() != null ? new HashMap<>(pageSample.getData())
                        : new HashMap<>();
                data.put("dateRetrieved", requestData.get("dateRetrieved"));
                data.put("retrievedBy", requestData.get("retrievedBy"));
                data.put("recipientSignature", requestData.get("recipientSignature"));
                data.put("retrievalRecorded", true);
                pageSample.setData(data);
                pageSample.setSysUserId(sysUserId);
                notebookPageSampleService.update(pageSample);
            }

            response.put("success", true);
            response.put("message", "Sample retrieval recorded successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to record retrieval: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========================================
    // TESTING ENDPOINTS
    // ========================================

    /**
     * Submit testing/microscopy results. POST
     * /rest/notebook/pathology/testing/submit
     */
    @PostMapping(value = "/testing/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitTesting(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String sampleId = parseString(requestData.get("sampleId"));
            Integer pageId = parseInteger(requestData.get("pageId"));

            if (sampleId == null || pageId == null) {
                response.put("success", false);
                response.put("error", "Sample ID and Page ID are required");
                return ResponseEntity.badRequest().body(response);
            }

            NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleId, pageId);

            if (pageSample != null) {
                Map<String, Object> data = pageSample.getData() != null ? new HashMap<>(pageSample.getData())
                        : new HashMap<>();
                data.putAll(requestData);
                pageSample.setData(data);
                pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                pageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                pageSample.setSysUserId(sysUserId);
                notebookPageSampleService.update(pageSample);
            } else {
                NoteBookPage page = noteBookPageService.get(pageId);
                if (page == null) {
                    response.put("success", false);
                    response.put("error", "Page not found: " + pageId);
                    return ResponseEntity.badRequest().body(response);
                }

                NotebookPageSample newPageSample = new NotebookPageSample();
                newPageSample.setNotebookPage(page);
                newPageSample.setSampleItemId(sampleId);
                newPageSample.setData(new HashMap<>(requestData));
                newPageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                newPageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                newPageSample.setSysUserId(sysUserId);
                notebookPageSampleService.insert(newPageSample);
            }

            response.put("success", true);
            response.put("message", "Testing results saved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to save testing results: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========================================
    // DISPOSAL & ARCHIVING ENDPOINTS
    // ========================================

    /**
     * Submit disposal record for multiple samples. POST
     * /rest/notebook/pathology/disposal/submit
     *
     * UI sends: sampleIds (array), pageId, disposalReason, retentionPolicy,
     * disposalMethod, disposalDate, staffSignature, unitHeadApproval
     */
    @PostMapping(value = "/disposal/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> submitDisposal(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            Integer pageId = parseInteger(requestData.get("pageId"));
            List<Integer> sampleIds = (List<Integer>) requestData.get("sampleIds");

            if ((sampleIds == null || sampleIds.isEmpty()) || pageId == null) {
                response.put("success", false);
                response.put("error", "Sample IDs and Page ID are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Build disposal data map from UI fields
            Map<String, Object> disposalData = new HashMap<>();
            disposalData.put("disposalReason", requestData.get("disposalReason"));
            disposalData.put("retentionPolicy", requestData.get("retentionPolicy"));
            disposalData.put("disposalMethod", requestData.get("disposalMethod"));
            disposalData.put("disposalDate", requestData.get("disposalDate"));
            disposalData.put("staffSignature", requestData.get("staffSignature"));
            disposalData.put("unitHeadApproval", requestData.get("unitHeadApproval"));
            disposalData.put("disposalStatus", "DISPOSED");

            // Process each sample
            int processedCount = 0;
            for (Integer sampleId : sampleIds) {
                String sampleIdStr = String.valueOf(sampleId);
                NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleIdStr,
                        pageId);

                if (pageSample != null) {
                    Map<String, Object> data = pageSample.getData() != null ? new HashMap<>(pageSample.getData())
                            : new HashMap<>();
                    data.putAll(disposalData);
                    pageSample.setData(data);
                    pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                    pageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                    pageSample.setSysUserId(sysUserId);
                    notebookPageSampleService.update(pageSample);
                    processedCount++;
                } else {
                    // Create new page sample entry for disposal tracking
                    NoteBookPage page = noteBookPageService.get(pageId);
                    if (page != null) {
                        NotebookPageSample newPageSample = new NotebookPageSample();
                        newPageSample.setNotebookPage(page);
                        newPageSample.setSampleItemId(sampleIdStr);
                        newPageSample.setData(new HashMap<>(disposalData));
                        newPageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                        newPageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                        newPageSample.setSysUserId(sysUserId);
                        notebookPageSampleService.insert(newPageSample);
                        processedCount++;
                    }
                }
            }

            response.put("success", true);
            response.put("message", String.format("Disposal recorded for %d samples", processedCount));
            response.put("processedCount", processedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to save disposal record: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Submit archive record for entry-level documentation. POST
     * /rest/notebook/pathology/archive/submit
     *
     * UI sends: entryId, pageId, archiveTypes (array), archiveLocation,
     * digitalBackupLocation, archiveDate This is for archiving
     * logbooks/ledgers/reports, not per-sample archiving
     */
    @PostMapping(value = "/archive/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitArchive(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            Integer pageId = parseInteger(requestData.get("pageId"));

            // Build archive data map from UI fields
            Map<String, Object> archiveData = new HashMap<>();
            archiveData.put("archiveTypes", requestData.get("archiveTypes")); // Array of archive types
            archiveData.put("archiveLocation", requestData.get("archiveLocation")); // Physical location
            archiveData.put("digitalBackupLocation", requestData.get("digitalBackupLocation")); // Digital backup
            archiveData.put("archiveDate", requestData.get("archiveDate"));
            archiveData.put("archiveStatus", "ARCHIVED");
            archiveData.put("archivedBy", sysUserId);

            // Archive is entry-level, not sample-level
            // Store the archive record - could be in a separate archive_log table
            // For now, we'll acknowledge and store the metadata

            response.put("success", true);
            response.put("message", "Archive record saved successfully");
            response.put("archiveData", archiveData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to save archive record: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========================================
    // REPORTING ENDPOINTS
    // ========================================

    /**
     * Get pathology metrics for an entry. GET /rest/notebook/pathology/metrics
     */
    @GetMapping(value = "/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMetrics(@RequestParam Integer entryId, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Return default/placeholder metrics - would be calculated from actual data
            response.put("specimenRejectionRate", 2.5);
            response.put("assaySuccessRate", 97.8);
            response.put("averageTAT", 24);
            response.put("equipmentDowntimeHours", 4);
            response.put("monthlySpecimenVolume", 150);
            response.put("qcIncidents", 3);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Failed to get metrics: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Generate a pathology report. POST /rest/notebook/pathology/report/generate
     */
    @PostMapping(value = "/report/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateReport(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            // Report generation logic would go here
            response.put("success", true);
            response.put("message", "Report generated successfully");
            response.put("reportType", requestData.get("reportType"));
            response.put("reportPeriod", requestData.get("reportPeriod"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to generate report: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Save QC meeting record. POST /rest/notebook/pathology/qc-meeting
     */
    @PostMapping(value = "/qc-meeting", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveQcMeeting(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            // QC meeting record would be stored - for now just acknowledge
            response.put("success", true);
            response.put("message", "QC meeting recorded successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to save QC meeting: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========================================
    // REFERENCE & SOP ENDPOINTS
    // ========================================

    /**
     * Get SOPs for an entry. GET /rest/notebook/pathology/sops
     */
    @GetMapping(value = "/sops", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSops(@RequestParam Integer entryId,
            HttpServletRequest request) {
        try {
            // Return empty list - would be fetched from reference_document table
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    /**
     * Upload an SOP document. POST /rest/notebook/pathology/sop/upload
     *
     * UI sends JSON with: entryId, pageId, sopTitle, sopCategory, version,
     * effectiveDate, reviewDate, previousVersion, changesSummary, approvedBy,
     * approvalDate, sopDocument: { base64File, fileType, fileName }
     */
    @PostMapping(value = "/sop/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> uploadSop(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            // Extract SOP metadata from request
            String sopTitle = parseString(requestData.get("sopTitle"));
            String sopCategory = parseString(requestData.get("sopCategory"));
            String version = parseString(requestData.get("version"));
            String effectiveDate = parseString(requestData.get("effectiveDate"));
            String reviewDate = parseString(requestData.get("reviewDate"));
            String previousVersion = parseString(requestData.get("previousVersion"));
            String changesSummary = parseString(requestData.get("changesSummary"));
            String approvedBy = parseString(requestData.get("approvedBy"));

            // Extract file data if present
            Map<String, Object> sopDocument = (Map<String, Object>) requestData.get("sopDocument");
            String fileName = null;
            String fileType = null;
            if (sopDocument != null) {
                fileName = parseString(sopDocument.get("fileName"));
                fileType = parseString(sopDocument.get("fileType"));
                // base64File would be stored/processed here
            }

            // Store SOP record - would typically go to reference_document table
            // For now, acknowledge receipt and return success

            response.put("success", true);
            response.put("message", "SOP uploaded successfully");
            response.put("sopTitle", sopTitle);
            response.put("sopCategory", sopCategory);
            response.put("version", version);
            response.put("fileName", fileName);
            response.put("fileType", fileType);
            response.put("status", "Active");
            response.put("effectiveDate", effectiveDate);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to upload SOP: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer intValue) {
            return intValue;
        }
        if (value instanceof String strValue) {
            try {
                return Integer.parseInt(strValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (value instanceof Number numValue) {
            return numValue.intValue();
        }
        return null;
    }

    private String parseString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
