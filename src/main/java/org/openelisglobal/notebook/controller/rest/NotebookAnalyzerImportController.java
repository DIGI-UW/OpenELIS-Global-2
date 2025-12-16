package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.form.AnalyzerImportForm;
import org.openelisglobal.notebook.service.AnalyzerResultImportService;
import org.openelisglobal.notebook.service.AnalyzerResultImportService.FileFormat;
import org.openelisglobal.notebook.service.AnalyzerResultImportService.ImportPreview;
import org.openelisglobal.notebook.service.AnalyzerResultImportService.ImportResult;
import org.openelisglobal.notebook.service.AnalyzerResultImportService.ImportSummary;
import org.openelisglobal.notebook.service.AnalyzerResultImportService.ParseResult;
import org.openelisglobal.notebook.service.NoteBookPageService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.valueholder.AnalyzerResultImport;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for analyzer result import operations. Handles file upload,
 * column mapping, preview, and import execution.
 */
@RestController
@RequestMapping("/rest/notebook/bulk/page")
public class NotebookAnalyzerImportController extends BaseRestController {

    @Autowired
    private AnalyzerResultImportService analyzerResultImportService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    /**
     * Parse an uploaded analyzer file and return headers/sample data. Step 1 of the
     * import wizard.
     *
     * <p>
     * POST /rest/notebook/bulk/page/{pageId}/analyzer-import/parse
     */
    @PostMapping(value = "/{pageId}/analyzer-import/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> parseFile(@PathVariable Integer pageId,
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate page exists
            NoteBookPage page = noteBookPageService.get(pageId);
            if (page == null) {
                response.put("error", "Page not found: " + pageId);
                return ResponseEntity.badRequest().body(response);
            }

            // Detect file format
            String fileName = file.getOriginalFilename();
            FileFormat format = analyzerResultImportService.detectFileFormat(fileName);

            // Parse the file
            ParseResult parseResult = analyzerResultImportService.parseAnalyzerFile(file.getInputStream(), fileName);

            // Build response
            response.put("success", !parseResult.hasErrors());
            response.put("fileName", fileName);
            response.put("format", format.name());
            response.put("headers", parseResult.headers());
            response.put("totalRows", parseResult.totalRows());
            response.put("sampleData",
                    parseResult.rows().size() > 5 ? parseResult.rows().subList(0, 5) : parseResult.rows()); // First 5
            // rows for
            // preview
            if (parseResult.hasErrors()) {
                response.put("parseErrors", parseResult.parseErrors());
            }

            // Store parse result in session for later use (preview/import)
            // For now, return as temporary token that frontend can use
            // In production, consider using session or cache
            response.put("parseToken", String.valueOf(System.currentTimeMillis()));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getName(), "parseFile", "Error reading file: " + e.getMessage());
            response.put("error", "Failed to read file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Preview import by matching rows to samples. Step 2 of the import wizard -
     * shows matched/unmatched samples.
     *
     * <p>
     * POST /rest/notebook/bulk/page/{pageId}/analyzer-import/preview
     */
    @PostMapping(value = "/{pageId}/analyzer-import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> previewImport(@PathVariable Integer pageId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "wellCoordinate", required = false) String wellCoordinate,
            @RequestParam(value = "externalId", required = false) String externalId,
            @RequestParam(value = "result", required = false) String result) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate page exists
            NoteBookPage page = noteBookPageService.get(pageId);
            if (page == null) {
                response.put("error", "Page not found: " + pageId);
                return ResponseEntity.badRequest().body(response);
            }

            Integer notebookId = page.getNotebook().getId();
            String fileName = file.getOriginalFilename();

            // Parse the file
            ParseResult parseResult = analyzerResultImportService.parseAnalyzerFile(file.getInputStream(), fileName);
            if (parseResult.hasErrors()) {
                response.put("error", "File parsing failed");
                response.put("parseErrors", parseResult.parseErrors());
                return ResponseEntity.badRequest().body(response);
            }

            // Build column mapping from individual parameters
            Map<String, String> columnMapping = new HashMap<>();
            if (wellCoordinate != null && !wellCoordinate.isBlank()) {
                columnMapping.put("wellCoordinate", wellCoordinate);
            }
            if (externalId != null && !externalId.isBlank()) {
                columnMapping.put("externalId", externalId);
            }
            if (result != null && !result.isBlank()) {
                columnMapping.put("result", result);
            }

            // Generate preview
            ImportPreview preview = analyzerResultImportService.previewImport(notebookId, pageId, parseResult,
                    columnMapping);

            response.put("success", true);
            response.put("matchedCount", preview.matchedCount());
            response.put("unmatchedCount", preview.unmatchedCount());
            response.put("warnings", preview.warnings());
            response.put("rows", preview.rows());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getName(), "previewImport", "Error reading file: " + e.getMessage());
            response.put("error", "Failed to read file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Execute the analyzer result import. Step 3 of the import wizard - stores
     * results in NotebookPageSample.data.
     *
     * <p>
     * POST /rest/notebook/bulk/page/{pageId}/analyzer-import
     */
    @PostMapping(value = "/{pageId}/analyzer-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> executeImport(@PathVariable Integer pageId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "wellCoordinate", required = false) String wellCoordinate,
            @RequestParam(value = "externalId", required = false) String externalId,
            @RequestParam(value = "result", required = false) String result,
            @RequestParam(value = "assayRunId", required = false) String assayRunId,
            @RequestParam(value = "operatorId", required = false) String operatorId, HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();
        String userId = getSysUserId(request);

        try {
            // Validate page exists
            NoteBookPage page = noteBookPageService.get(pageId);
            if (page == null) {
                response.put("error", "Page not found: " + pageId);
                return ResponseEntity.badRequest().body(response);
            }

            String fileName = file.getOriginalFilename();

            // Parse the file
            ParseResult parseResult = analyzerResultImportService.parseAnalyzerFile(file.getInputStream(), fileName);
            if (parseResult.hasErrors()) {
                response.put("error", "File parsing failed");
                response.put("parseErrors", parseResult.parseErrors());
                return ResponseEntity.badRequest().body(response);
            }

            // Build column mapping from individual parameters
            Map<String, String> columnMapping = new HashMap<>();
            if (wellCoordinate != null && !wellCoordinate.isBlank()) {
                columnMapping.put("wellCoordinate", wellCoordinate);
            }
            if (externalId != null && !externalId.isBlank()) {
                columnMapping.put("externalId", externalId);
            }
            if (result != null && !result.isBlank()) {
                columnMapping.put("result", result);
            }

            // Execute import (machine parameters and reagent lots not needed for basic
            // import)
            ImportResult importResult = analyzerResultImportService.executeImport(pageId, parseResult, columnMapping,
                    assayRunId, operatorId, null, null, userId);

            response.put("success", importResult.isFullySuccessful());
            response.put("importId", importResult.importId());
            response.put("totalRows", importResult.totalRows());
            response.put("successfulRows", importResult.successfulRows());
            response.put("failedRows", importResult.failedRows());
            if (!importResult.isFullySuccessful()) {
                response.put("errors", importResult.errors());
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getName(), "executeImport", "Error reading file: " + e.getMessage());
            response.put("error", "Failed to read file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Execute import with form data instead of multipart. Alternative endpoint for
     * JSON-based import requests.
     *
     * <p>
     * POST /rest/notebook/bulk/page/{pageId}/analyzer-import/json
     */
    @PostMapping(value = "/{pageId}/analyzer-import/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> executeImportJson(@PathVariable Integer pageId,
            @RequestBody AnalyzerImportForm form) {

        Map<String, Object> response = new HashMap<>();
        response.put("error",
                "JSON import not supported. Use multipart/form-data with file upload to /analyzer-import endpoint.");
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Get import record by ID.
     *
     * <p>
     * GET /rest/notebook/bulk/page/{pageId}/analyzer-import/{importId}
     */
    @GetMapping("/{pageId}/analyzer-import/{importId}")
    public ResponseEntity<Map<String, Object>> getImport(@PathVariable Integer pageId, @PathVariable Integer importId) {

        Map<String, Object> response = new HashMap<>();

        AnalyzerResultImport importRecord = analyzerResultImportService.getImportById(importId);
        if (importRecord == null) {
            response.put("error", "Import not found: " + importId);
            return ResponseEntity.notFound().build();
        }

        // Verify import belongs to this page
        if (!pageId.equals(importRecord.getNotebookPageId())) {
            response.put("error", "Import does not belong to this page");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("id", importRecord.getId());
        response.put("fileName", importRecord.getFileName());
        response.put("fileFormat", importRecord.getFileFormat());
        response.put("importDate", importRecord.getImportDate());
        response.put("totalRows", importRecord.getTotalRows());
        response.put("successfulRows", importRecord.getSuccessfulRows());
        response.put("failedRows", importRecord.getFailedRows());
        response.put("successRate", importRecord.getSuccessRate());
        response.put("columnMapping", importRecord.getColumnMapping());
        response.put("errorDetails", importRecord.getErrorDetails());
        response.put("assayRunId", importRecord.getAssayRunId());
        response.put("operatorId", importRecord.getOperatorId());
        response.put("machineParameters", importRecord.getMachineParameters());
        response.put("reagentLots", importRecord.getReagentLots());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all import records for a page.
     *
     * <p>
     * GET /rest/notebook/bulk/page/{pageId}/analyzer-import
     */
    @GetMapping("/{pageId}/analyzer-import")
    public ResponseEntity<Map<String, Object>> getImports(@PathVariable Integer pageId) {

        Map<String, Object> response = new HashMap<>();

        // Validate page exists
        NoteBookPage page = noteBookPageService.get(pageId);
        if (page == null) {
            response.put("error", "Page not found: " + pageId);
            return ResponseEntity.badRequest().body(response);
        }

        List<AnalyzerResultImport> imports = analyzerResultImportService.getByNotebookPageId(pageId);
        ImportSummary summary = analyzerResultImportService.getImportSummary(pageId);

        response.put("imports", imports.stream().map(imp -> {
            Map<String, Object> importData = new HashMap<>();
            importData.put("id", imp.getId());
            importData.put("fileName", imp.getFileName());
            importData.put("fileFormat", imp.getFileFormat());
            importData.put("importDate", imp.getImportDate());
            importData.put("totalRows", imp.getTotalRows());
            importData.put("successfulRows", imp.getSuccessfulRows());
            importData.put("failedRows", imp.getFailedRows());
            importData.put("successRate", imp.getSuccessRate());
            importData.put("assayRunId", imp.getAssayRunId());
            importData.put("operatorId", imp.getOperatorId());
            return importData;
        }).toList());

        response.put("summary",
                Map.of("importCount", summary.importCount(), "totalRows", summary.totalRows(), "successfulRows",
                        summary.successfulRows(), "failedRows", summary.failedRows(), "overallSuccessRate",
                        summary.overallSuccessRate(), "lastImportDate", summary.lastImportDate()));

        return ResponseEntity.ok(response);
    }

    /**
     * Get import summary for a page.
     *
     * <p>
     * GET /rest/notebook/bulk/page/{pageId}/analyzer-import/summary
     */
    @GetMapping("/{pageId}/analyzer-import/summary")
    public ResponseEntity<Map<String, Object>> getImportSummary(@PathVariable Integer pageId) {

        Map<String, Object> response = new HashMap<>();

        // Validate page exists
        NoteBookPage page = noteBookPageService.get(pageId);
        if (page == null) {
            response.put("error", "Page not found: " + pageId);
            return ResponseEntity.badRequest().body(response);
        }

        ImportSummary summary = analyzerResultImportService.getImportSummary(pageId);

        response.put("importCount", summary.importCount());
        response.put("totalRows", summary.totalRows());
        response.put("successfulRows", summary.successfulRows());
        response.put("failedRows", summary.failedRows());
        response.put("overallSuccessRate", summary.overallSuccessRate());
        response.put("lastImportDate", summary.lastImportDate());
        response.put("hasFailedImports", analyzerResultImportService.hasFailedImports(pageId));

        return ResponseEntity.ok(response);
    }

    /**
     * Add manual results to selected samples. POST
     * /rest/notebook/bulk/page/{pageId}/manual-results
     *
     * <p>
     * Allows technicians to manually enter results for selected samples when
     * automated import is not available or applicable.
     */
    @PostMapping(value = "/{pageId}/manual-results", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> addManualResults(@PathVariable Integer pageId,
            @RequestBody ManualResultRequest request, HttpServletRequest httpRequest) {

        Map<String, Object> response = new HashMap<>();
        String userId = getSysUserId(httpRequest);

        try {
            // Validate page exists
            NoteBookPage page = noteBookPageService.get(pageId);
            if (page == null) {
                response.put("error", "Page not found: " + pageId);
                return ResponseEntity.badRequest().body(response);
            }

            // Validate request
            if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
                response.put("error", "No sample IDs provided");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getResult() == null || request.getResult().isBlank()) {
                response.put("error", "Result value is required");
                return ResponseEntity.badRequest().body(response);
            }

            int updatedCount = 0;
            java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

            for (Integer sampleId : request.getSampleIds()) {
                try {
                    // Get or create the NotebookPageSample record
                    NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                    if (nps == null) {
                        // Auto-create if doesn't exist
                        notebookPageSampleService.createPageSampleForPage(pageId, sampleId,
                                NotebookPageSample.Status.IN_PROGRESS);
                        nps = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleId);
                    }

                    if (nps != null) {
                        // Update status to IN_PROGRESS if PENDING
                        if (nps.getStatus() == NotebookPageSample.Status.PENDING) {
                            nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                        }

                        // Store result in data field
                        Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData())
                                : new HashMap<>();

                        // Create analyzer result structure (same format as import)
                        Map<String, Object> analyzerResult = new HashMap<>();
                        analyzerResult.put("result", request.getResult());
                        analyzerResult.put("entryType", "MANUAL");
                        analyzerResult.put("importDate", now.toString());
                        if (request.getAssayRunId() != null && !request.getAssayRunId().isBlank()) {
                            analyzerResult.put("assayRunId", request.getAssayRunId());
                        }
                        if (request.getNotes() != null && !request.getNotes().isBlank()) {
                            analyzerResult.put("notes", request.getNotes());
                        }
                        if (userId != null) {
                            analyzerResult.put("enteredBy", userId);
                        }

                        data.put("analyzerResult", analyzerResult);
                        nps.setData(data);
                        notebookPageSampleService.update(nps);
                        updatedCount++;
                    }
                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getName(), "addManualResults",
                            "Error adding result for sample " + sampleId + ": " + e.getMessage());
                }
            }

            response.put("success", updatedCount > 0);
            response.put("updatedCount", updatedCount);
            response.put("requestedCount", request.getSampleIds().size());
            response.put("pageId", pageId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "addManualResults", "Error: " + e.getMessage());
            response.put("error", "Failed to add manual results: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /** Request body for manual result entry. */
    public static class ManualResultRequest {
        private List<Integer> sampleIds;
        private String result;
        private String assayRunId;
        private String notes;
        private String entryType;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getAssayRunId() {
            return assayRunId;
        }

        public void setAssayRunId(String assayRunId) {
            this.assayRunId = assayRunId;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getEntryType() {
            return entryType;
        }

        public void setEntryType(String entryType) {
            this.entryType = entryType;
        }
    }
}
