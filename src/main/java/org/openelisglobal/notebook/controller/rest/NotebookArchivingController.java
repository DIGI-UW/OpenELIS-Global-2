package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.service.ArchivingService;
import org.openelisglobal.notebook.service.ArchivingService.ArchivingProgress;
import org.openelisglobal.notebook.service.ArchivingService.TraceabilityReport;
import org.openelisglobal.notebook.service.ArchivingService.TraceabilityResult;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.SampleRouting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for notebook archiving operations (US8). Handles
 * biorepository transfers, traceability verification, and notebook
 * finalization.
 */
@RestController
@RequestMapping(value = "/rest/notebook")
public class NotebookArchivingController extends BaseRestController {

    @Autowired
    private ArchivingService archivingService;

    @Autowired
    private NoteBookService noteBookService;

    /**
     * T136a: Transfer samples to biorepository for permanent archival. POST
     * /notebook/{id}/archive/transfer
     */
    @PostMapping(value = "/{notebookId}/archive/transfer", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> transferToBiorepository(@PathVariable("notebookId") Integer notebookId,
            @RequestBody TransferRequest request, HttpServletRequest httpRequest) {

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String userId = getSysUserId(httpRequest);

            List<SampleRouting> routings = archivingService.transferToBiorepository(notebookId,
                    request.getSampleItemIds(), request.getLocationId(), request.getLocationType(), request.getNotes(),
                    userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transferredCount", routings.size());
            response.put("routings", routings.stream().map(r -> {
                Map<String, Object> routingInfo = new HashMap<>();
                routingInfo.put("id", r.getId());
                routingInfo.put("sampleItemId", r.getSampleItemId());
                routingInfo.put("destinationType", r.getDestinationType().name());
                return routingInfo;
            }).toList());

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "transferToBiorepository", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Failed to transfer samples: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * T136b: Verify traceability for all samples in a notebook. POST
     * /notebook/{id}/archive/verify-traceability
     */
    @PostMapping(value = "/{notebookId}/archive/verify-traceability", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TraceabilityResult> verifyTraceability(@PathVariable("notebookId") Integer notebookId) {

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            TraceabilityResult result = archivingService.verifyTraceability(notebookId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "verifyTraceability", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Simple archive endpoint - marks notebook as archived/complete. POST
     * /notebook/{id}/archive
     */
    @PostMapping(value = "/{notebookId}/archive", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> archiveNotebook(@PathVariable("notebookId") Integer notebookId,
            @RequestBody(required = false) ArchiveRequest request, HttpServletRequest httpRequest) {

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String userId = getSysUserId(httpRequest);
            NoteBook archived = archivingService.finalizeNotebook(notebookId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("notebookId", archived.getId());
            response.put("status", archived.getStatus().name());
            response.put("message", "Notebook has been archived successfully");
            if (request != null && request.getNotes() != null) {
                response.put("archiveNotes", request.getNotes());
            }

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "archiveNotebook", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Failed to archive notebook: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * T136c: Finalize notebook after archiving is complete. POST
     * /notebook/{id}/archive/finalize
     */
    @PostMapping(value = "/{notebookId}/archive/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> finalizeNotebook(@PathVariable("notebookId") Integer notebookId,
            HttpServletRequest httpRequest) {

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String userId = getSysUserId(httpRequest);
            NoteBook finalized = archivingService.finalizeNotebook(notebookId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("notebookId", finalized.getId());
            response.put("status", finalized.getStatus().name());
            response.put("message", "Notebook has been finalized successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "finalizeNotebook", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Failed to finalize notebook: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get archiving progress for a notebook. GET /notebook/{id}/archive/progress
     */
    @GetMapping(value = "/{notebookId}/archive/progress", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArchivingProgress> getArchivingProgress(@PathVariable("notebookId") Integer notebookId) {

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            ArchivingProgress progress = archivingService.getArchivingProgress(notebookId);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getArchivingProgress", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get archivable samples (parent and child) for a notebook. GET
     * /notebook/{id}/archive/samples
     */
    @GetMapping(value = "/{notebookId}/archive/samples", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, List<Integer>>> getArchivableSamples(
            @PathVariable("notebookId") Integer notebookId) {

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Map<String, List<Integer>> samples = archivingService.getArchivableSamples(notebookId);
            return ResponseEntity.ok(samples);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getArchivableSamples", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check if notebook can be finalized. GET /notebook/{id}/archive/can-finalize
     */
    @GetMapping(value = "/{notebookId}/archive/can-finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> canFinalize(@PathVariable("notebookId") Integer notebookId) {

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            boolean canFinalize = archivingService.canFinalize(notebookId);
            TraceabilityResult traceability = archivingService.verifyTraceability(notebookId);

            Map<String, Object> response = new HashMap<>();
            response.put("canFinalize", canFinalize);
            response.put("traceability", traceability);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "canFinalize", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate traceability report. GET /notebook/{id}/archive/traceability-report
     */
    @GetMapping(value = "/{notebookId}/archive/traceability-report", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TraceabilityReport> getTraceabilityReport(@PathVariable("notebookId") Integer notebookId) {

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            TraceabilityReport report = archivingService.generateTraceabilityReport(notebookId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getTraceabilityReport", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Request body for transfer operation.
     */
    public static class TransferRequest {
        private List<Integer> sampleItemIds;
        private String locationId;
        private String locationType;
        private String notes;

        public List<Integer> getSampleItemIds() {
            return sampleItemIds;
        }

        public void setSampleItemIds(List<Integer> sampleItemIds) {
            this.sampleItemIds = sampleItemIds;
        }

        public String getLocationId() {
            return locationId;
        }

        public void setLocationId(String locationId) {
            this.locationId = locationId;
        }

        public String getLocationType() {
            return locationType;
        }

        public void setLocationType(String locationType) {
            this.locationType = locationType;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * Request body for simple archive operation.
     */
    public static class ArchiveRequest {
        private Integer pageId;
        private String notes;
        private String archivedAt;

        public Integer getPageId() {
            return pageId;
        }

        public void setPageId(Integer pageId) {
            this.pageId = pageId;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getArchivedAt() {
            return archivedAt;
        }

        public void setArchivedAt(String archivedAt) {
            this.archivedAt = archivedAt;
        }
    }
}
