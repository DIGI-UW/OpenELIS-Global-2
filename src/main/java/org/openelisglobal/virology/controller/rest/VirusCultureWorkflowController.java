package org.openelisglobal.virology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.virology.service.VirusCultureWorkflowService;
import org.openelisglobal.virology.valueholder.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Virus Culture Workflow (Stage 2)
 */
@Controller
@RequestMapping("/rest/notebook/virology/culture")
public class VirusCultureWorkflowController extends BaseController {

    @Autowired
    private VirusCultureWorkflowService virusCultureWorkflowService;

    @Autowired
    private SystemUserService systemUserService;

    // ==========================================
    // VIRUS CULTURE BATCH ENDPOINTS
    // ==========================================

    /**
     * Create new virus culture batch from a notebook page sample
     */
    @PostMapping(value = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createBatch(@Valid @RequestBody CreateBatchRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get current user
            String systemUserId = getSysUserId(httpRequest);
            SystemUser currentUser = systemUserService.get(systemUserId);

            // Resolve NotebookPageSample if ID is provided
            NotebookPageSample notebookPageSample = null;
            if (request.getNotebookPageSampleId() != null) {
                // Note: You may want to add a service method to get NotebookPageSample by ID
                // For now, this handles the case where notebookPageSampleId might be null
                // notebookPageSample =
                // notebookPageSampleService.get(request.getNotebookPageSampleId());
            }

            // Validate batch creation
            Map<String, Object> validationResult = virusCultureWorkflowService
                    .validateBatchCreation(request.getNotebookPageSampleId());

            if (!(Boolean) validationResult.get("valid")) {
                response.put("success", false);
                response.put("message", "Validation failed");
                response.put("errors", validationResult.get("errors"));
                return ResponseEntity.badRequest().body(response);
            }

            // Create batch
            VirusCultureBatch batch = virusCultureWorkflowService.createVirusCultureBatch(notebookPageSample,
                    request.getVirusStrain(), request.getCellLine(), currentUser);

            response.put("success", true);
            response.put("message", "Virus culture batch created successfully");
            response.put("batchId", batch.getId());
            response.put("batchNumber", batch.getBatchId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create virus culture batch: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get virus culture batch details
     */
    @GetMapping(value = "/batch/{batchId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBatch(@PathVariable Integer batchId) {
        Map<String, Object> response = new HashMap<>();

        try {
            VirusCultureBatch batch = virusCultureWorkflowService.getVirusCultureBatch(batchId);

            if (batch == null) {
                response.put("success", false);
                response.put("message", "Virus culture batch not found");
                return ResponseEntity.notFound().build();
            }

            List<VirusCultureWorkflowStatus> workflowStatus = virusCultureWorkflowService.getWorkflowStatus(batchId);

            Map<String, Object> progressInfo = virusCultureWorkflowService.getWorkflowProgress(batchId);

            response.put("success", true);
            response.put("batch", batch);
            response.put("workflowStatus", workflowStatus);
            response.put("progress", progressInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get virus culture batch: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get virus culture batches for a sample
     */
    @GetMapping(value = "/batches/sample/{sampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBatchesBySample(@PathVariable Integer sampleId) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<VirusCultureBatch> batches = virusCultureWorkflowService.getVirusCultureBatchesBySample(sampleId);

            response.put("success", true);
            response.put("batches", batches);
            response.put("count", batches.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get virus culture batches: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get active virus culture batches
     */
    @GetMapping(value = "/batches/active", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getActiveBatches() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<VirusCultureBatch> activeBatches = virusCultureWorkflowService.getActiveVirusCultureBatches();

            response.put("success", true);
            response.put("batches", activeBatches);
            response.put("count", activeBatches.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get active batches: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==========================================
    // WORKFLOW STEP ENDPOINTS
    // ==========================================

    /**
     * Start a workflow step
     */
    @PostMapping(value = "/batch/{batchId}/step/{stepName}/start", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startStep(@PathVariable Integer batchId, @PathVariable String stepName,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate step can be started
            Map<String, Object> validationResult = virusCultureWorkflowService.validateStepCanStart(batchId, stepName);

            if (!(Boolean) validationResult.get("valid")) {
                response.put("success", false);
                response.put("message", "Step cannot be started");
                response.put("errors", validationResult.get("errors"));
                return ResponseEntity.badRequest().body(response);
            }

            // Get current user
            String systemUserId = getSysUserId(httpRequest);
            SystemUser currentUser = systemUserService.get(systemUserId);

            VirusCultureWorkflowStatus status = virusCultureWorkflowService.startWorkflowStep(batchId, stepName,
                    currentUser);

            response.put("success", true);
            response.put("message", "Step started successfully");
            response.put("workflowStatus", status);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to start step: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Complete a workflow step
     */
    @PostMapping(value = "/batch/{batchId}/step/{stepName}/complete", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> completeStep(@PathVariable Integer batchId,
            @PathVariable String stepName, @RequestBody CompleteStepRequest request, HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get current user
            String systemUserId = getSysUserId(httpRequest);
            SystemUser currentUser = systemUserService.get(systemUserId);

            VirusCultureWorkflowStatus.QualityCheckResult qcResult = request.getQualityResult() != null
                    ? VirusCultureWorkflowStatus.QualityCheckResult.valueOf(request.getQualityResult())
                    : VirusCultureWorkflowStatus.QualityCheckResult.NOT_APPLICABLE;

            VirusCultureWorkflowStatus status = virusCultureWorkflowService.completeWorkflowStep(batchId, stepName,
                    currentUser, qcResult, request.getNotes());

            // Auto-advance to next step if possible
            boolean advanced = virusCultureWorkflowService.autoAdvanceWorkflow(batchId);

            response.put("success", true);
            response.put("message", "Step completed successfully");
            response.put("workflowStatus", status);
            response.put("autoAdvanced", advanced);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to complete step: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==========================================
    // PROCESS STEP DATA ENDPOINTS
    // ==========================================

    /**
     * Record quality control data
     */
    @PostMapping(value = "/batch/{batchId}/quality-control", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordQualityControl(@PathVariable Integer batchId,
            @Valid @RequestBody VirusCultureQualityControl qualityControl, HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get current user
            String systemUserId = getSysUserId(httpRequest);
            SystemUser currentUser = systemUserService.get(systemUserId);

            qualityControl.setTestedBy(currentUser);

            VirusCultureQualityControl savedQc = virusCultureWorkflowService.recordQualityControl(batchId,
                    qualityControl);

            // Validate QC results
            Map<String, Object> validationResult = virusCultureWorkflowService.validateQualityControlResults(savedQc);

            response.put("success", true);
            response.put("message", "Quality control data recorded successfully");
            response.put("qualityControl", savedQc);
            response.put("validation", validationResult);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to record quality control data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Record virus inoculation data
     */
    @PostMapping(value = "/batch/{batchId}/virus-inoculation", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordVirusInoculation(@PathVariable Integer batchId,
            @Valid @RequestBody VirusCultureVirusInoculation virusInoculation, HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get current user
            String systemUserId = getSysUserId(httpRequest);
            SystemUser currentUser = systemUserService.get(systemUserId);

            virusInoculation.setPerformedBy(currentUser);

            VirusCultureVirusInoculation savedInoculation = virusCultureWorkflowService.recordVirusInoculation(batchId,
                    virusInoculation);

            response.put("success", true);
            response.put("message", "Virus inoculation data recorded successfully");
            response.put("virusInoculation", savedInoculation);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to record virus inoculation data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==========================================
    // DASHBOARD AND REPORTING ENDPOINTS
    // ==========================================

    /**
     * Get dashboard data for virus culture workflow
     */
    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> dashboardData = virusCultureWorkflowService.getDashboardData();

            response.put("success", true);
            response.putAll(dashboardData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get dashboard data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get workflow statistics
     */
    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> statistics = virusCultureWorkflowService.getWorkflowStatistics();

            response.put("success", true);
            response.put("statistics", statistics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==========================================
    // REQUEST CLASSES
    // ==========================================

    public static class CreateBatchRequest {
        private Integer notebookPageSampleId;
        private String virusStrain;
        private String cellLine;
        private String notes;

        // Getters and setters
        public Integer getNotebookPageSampleId() {
            return notebookPageSampleId;
        }

        public void setNotebookPageSampleId(Integer notebookPageSampleId) {
            this.notebookPageSampleId = notebookPageSampleId;
        }

        public String getVirusStrain() {
            return virusStrain;
        }

        public void setVirusStrain(String virusStrain) {
            this.virusStrain = virusStrain;
        }

        public String getCellLine() {
            return cellLine;
        }

        public void setCellLine(String cellLine) {
            this.cellLine = cellLine;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static class CompleteStepRequest {
        private String qualityResult;
        private String notes;

        public String getQualityResult() {
            return qualityResult;
        }

        public void setQualityResult(String qualityResult) {
            this.qualityResult = qualityResult;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    // Required implementations from BaseController
    @Override
    protected String findLocalForward(String forward) {
        return "virology/" + forward;
    }

    @Override
    protected String getPageTitleKey() {
        return "virology.culture.workflow.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "virology.culture.workflow.subtitle";
    }
}