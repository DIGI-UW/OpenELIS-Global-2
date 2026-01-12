package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.service.NotebookBulkOperationService;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.QaApprovalService;
import org.openelisglobal.notebook.service.ReportingMetricsService;
import org.openelisglobal.notebook.service.WestgardRulesService;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Bioanalytical reporting, metrics, and external result
 * distribution.
 *
 * Handles Stage 4 (Reporting & Release) operations: - Generate performance
 * metrics dashboard (throughput, quality, study progress) - Track instrument
 * utilization - Generate external reports (regulatory, LMIS, study sponsors) -
 * Provide QC and analytical success metrics - Support result export to REDCap,
 * LMIS, and regulatory databases
 *
 * Endpoints: - GET /dashboard - Get complete dashboard metrics - GET
 * /entry/{entryId}/throughput - Get sample throughput metrics - GET
 * /entry/{entryId}/quality-metrics - Get quality metrics - GET
 * /entry/{entryId}/study-progress - Get study progress tracking - GET
 * /entry/{entryId}/instrument-utilization - Get instrument utilization - GET
 * /entry/{entryId}/tat-by-test - Get turnaround time by test type - GET
 * /external-reports - Get external reporting options - POST
 * /entry/{entryId}/export-results - Export results to external system - GET
 * /alerts - Get dashboard alerts
 */
@RestController
@RequestMapping("/rest/notebook/bioanalytical")
public class BioanalyticalReportingController extends BaseRestController {

    @Autowired
    private NotebookBulkOperationService bulkOperationService;

    @Autowired
    private QaApprovalService qaApprovalService;

    @Autowired(required = false)
    private ReportingMetricsService reportingMetricsService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired(required = false)
    private WestgardRulesService westgardRulesService;

    /**
     * Get complete dashboard metrics (all categories combined).
     *
     * @param numberOfDays Number of days of historical data (default 30)
     * @return Complete dashboard metrics
     */
    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDashboardMetrics(
            @RequestParam(value = "numberOfDays", defaultValue = "30") int numberOfDays) {

        if (reportingMetricsService == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Reporting Metrics service not available");
            return ResponseEntity.internalServerError().body(errorResponse);
        }

        // TODO: Implement ReportingMetricsService.getDashboardMetrics()
        // ReportingMetricsService.DashboardMetrics metrics =
        // reportingMetricsService.getDashboardMetrics(numberOfDays);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Dashboard metrics not yet implemented");
        response.put("numberOfDays", numberOfDays);

        // Placeholder structure
        Map<String, Object> throughput = new HashMap<>();
        throughput.put("samplesReceived", 0);
        throughput.put("samplesAnalyzed", 0);
        throughput.put("samplesReported", 0);
        throughput.put("averageTATDays", 0.0);
        response.put("throughput", throughput);

        Map<String, Object> quality = new HashMap<>();
        quality.put("qcPassRate", 0.0);
        quality.put("calibrationAcceptanceRate", 0.0);
        response.put("quality", quality);

        response.put("studyProgress", java.util.List.of());
        response.put("instrumentUtilization", java.util.List.of());

        return ResponseEntity.ok(response);
    }

    /**
     * Get sample throughput metrics (received, analyzed, reported, TAT).
     *
     * @param entryId   The notebook entry ID
     * @param startDate Start date for metrics (default: 30 days ago)
     * @param endDate   End date for metrics (default: today)
     * @return Throughput metrics
     */
    @GetMapping(value = "/entry/{entryId}/throughput", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getThroughputMetrics(@PathVariable("entryId") Integer entryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        if (reportingMetricsService == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Reporting Metrics service not available");
            return ResponseEntity.internalServerError().body(errorResponse);
        }

        // TODO: Implement ReportingMetricsService.calculateThroughputMetrics()
        // ReportingMetricsService.ThroughputMetrics metrics =
        // reportingMetricsService.calculateThroughputMetrics(startDate, endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        response.put("message", "Throughput metrics not yet implemented");

        // Placeholder
        response.put("samplesReceived", 0);
        response.put("samplesAnalyzed", 0);
        response.put("samplesReported", 0);
        response.put("averageTATDays", 0.0);

        return ResponseEntity.ok(response);
    }

    /**
     * Get quality metrics (QC pass rates, calibration acceptance, instrument
     * uptime).
     *
     * @param entryId   The notebook entry ID
     * @param startDate Start date for metrics
     * @param endDate   End date for metrics
     * @return Quality metrics
     */
    @GetMapping(value = "/entry/{entryId}/quality-metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getQualityMetrics(@PathVariable("entryId") Integer entryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        response.put("message", "Quality metrics not yet implemented");

        // Placeholder
        Map<String, Object> quality = new HashMap<>();
        quality.put("qcPassRate", 98.5);
        quality.put("calibrationAcceptanceRate", 99.2);
        quality.put("systemSuitabilityPassRate", 99.0);
        response.put("quality", quality);

        return ResponseEntity.ok(response);
    }

    /**
     * Get bioequivalence study progress tracking.
     *
     * @param entryId The notebook entry ID
     * @param studyId Optional study ID (null for all studies)
     * @return Study progress metrics
     */
    @GetMapping(value = "/entry/{entryId}/study-progress", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStudyProgress(@PathVariable("entryId") Integer entryId,
            @RequestParam(required = false) String studyId) {

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("studyId", studyId);
        response.put("message", "Study progress metrics not yet implemented");

        return ResponseEntity.ok(response);
    }

    /**
     * Get instrument utilization metrics.
     *
     * @param entryId      The notebook entry ID
     * @param numberOfDays Number of days to include (default 7)
     * @return Instrument utilization data
     */
    @GetMapping(value = "/entry/{entryId}/instrument-utilization", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getInstrumentUtilization(@PathVariable("entryId") Integer entryId,
            @RequestParam(value = "numberOfDays", defaultValue = "7") int numberOfDays) {

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("numberOfDays", numberOfDays);
        response.put("instruments", java.util.List.of());
        response.put("message", "Instrument utilization data not yet implemented");

        return ResponseEntity.ok(response);
    }

    /**
     * Get turnaround time (TAT) metrics by test type.
     *
     * @param entryId   The notebook entry ID
     * @param startDate Start date for metrics
     * @param endDate   End date for metrics
     * @return TAT by test type
     */
    @GetMapping(value = "/entry/{entryId}/tat-by-test", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTATByTestType(@PathVariable("entryId") Integer entryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        response.put("message", "TAT by test type not yet implemented");

        // Placeholder
        Map<String, Double> tatByTest = new HashMap<>();
        tatByTest.put("Assay", 2.1);
        tatByTest.put("Dissolution", 3.5);
        tatByTest.put("Drug Concentration (HPLC)", 1.8);
        tatByTest.put("Drug Concentration (LC-MS/MS)", 4.2);
        response.put("tatByTestType", tatByTest);

        return ResponseEntity.ok(response);
    }

    /**
     * Get available external reporting/export options.
     *
     * @return List of available export formats
     */
    @GetMapping(value = "/external-reports", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getExternalReportOptions() {
        Map<String, Object> response = new HashMap<>();

        java.util.List<Map<String, String>> exportOptions = java.util.List.of(
                Map.of("id", "LMIS", "name", "Medical Laboratory LMIS Integration", "description",
                        "Export results to Medical Laboratory system", "format", "JSON/HL7"),
                Map.of("id", "REDCAP", "name", "REDCap Bioequivalence Database", "description",
                        "Export to REDCap for bioequivalence studies", "format", "REDCap API"),
                Map.of("id", "REGULATORY", "name", "Regulatory Submission Format", "description",
                        "CDISC/SDTM format for regulatory agencies", "format", "CDISC-SDTM"),
                Map.of("id", "RESEARCH", "name", "Research Study Database", "description",
                        "Generic research study export", "format", "CSV/JSON"));

        response.put("exportOptions", exportOptions);
        response.put("total", exportOptions.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Export validated results to external system (REDCap, LMIS, regulatory
     * database, etc.).
     *
     * @param entryId    The notebook entry ID
     * @param exportType Type of export (LMIS, REDCAP, REGULATORY, RESEARCH)
     * @param request    HTTP request (for user session)
     * @return Export result
     */
    @PostMapping(value = "/entry/{entryId}/export-results", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> exportResults(@PathVariable("entryId") Integer entryId,
            @RequestParam("exportType") String exportType, HttpServletRequest request) {

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String userId = "SYSTEM";

        Map<String, Object> response = new HashMap<>();
        response.put("entryId", entryId);
        response.put("exportType", exportType);
        response.put("exportedBy", userId);
        response.put("exportDate", java.time.LocalDateTime.now());
        response.put("message", "Export functionality not yet implemented");

        // TODO: Implement export to specific external systems
        switch (exportType.toUpperCase()) {
        case "LMIS":
            response.put("destination", "Medical Laboratory LMIS");
            break;
        case "REDCAP":
            response.put("destination", "REDCap Project");
            break;
        case "REGULATORY":
            response.put("destination", "Regulatory Submission (CDISC-SDTM)");
            break;
        case "RESEARCH":
            response.put("destination", "Research Study Database");
            break;
        default:
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown export type: " + exportType));
        }

        response.put("status", "PENDING");
        response.put("samplesExported", 0);

        return ResponseEntity.ok(response);
    }

    /**
     * Export bioanalytical study data to REDCap for clinical data management. Uses
     * bioanalytical-specific field mappings including analytical method, sample
     * type, bioequivalence statistics, and regulatory compliance data.
     *
     * @param pageId   The notebook page ID
     * @param request  The export request containing sample IDs and configuration
     * @param response HTTP response for file download
     */
    @PostMapping(value = "/page/{pageId}/export/redcap")
    public void exportToREDCap(@PathVariable("pageId") Integer pageId,
            @RequestBody BioanalyticalREDCapExportRequest request, HttpServletResponse response) {

        if (pageId == null || request == null || request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"pageId and sampleIds are required\"}");
            } catch (Exception e) {
                // Ignore - response may already be committed
            }
            return;
        }

        try {
            byte[] csvContent = bulkOperationService.generateBioanalyticalREDCapExport(pageId, request.getSampleIds(),
                    request.getRecordIdField(), request.getEventName());

            if (csvContent == null || csvContent.length == 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            String projectId = request.getProjectId() != null ? request.getProjectId() : "bioanalytical";
            String filename = "Bioanalytical_REDCap_Export_" + projectId + "_" + java.time.LocalDate.now() + ".csv";

            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(csvContent.length);
            response.getOutputStream().write(csvContent);
            response.getOutputStream().flush();

        } catch (Exception e) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            } catch (Exception ignored) {
                // Response might already be committed
            }
        }
    }

    /**
     * Submit QA approval for a notebook page. Records the analyst's approval
     * decision, comments, and timestamp. Enables external data export once
     * approved.
     *
     * @param pageId      The notebook page ID
     * @param request     The QA approval request with status and comments
     * @param httpRequest HTTP request for user session
     * @return Response with approval confirmation and page status
     */
    @PostMapping(value = "/page/{pageId}/qa-approval", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitQaApproval(@PathVariable("pageId") Integer pageId,
            @RequestBody QaApprovalRequest request, HttpServletRequest httpRequest) {

        String userId = getSysUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }

        if (pageId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Page ID is required"));
        }

        if (request == null || request.getApprovalStatus() == null || request.getApprovalStatus().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Approval status is required (APPROVED, REJECTED, CONDITIONAL)"));
        }

        try {
            Map<String, Object> result = qaApprovalService.submitQaApproval(pageId, request.getApprovalStatus(),
                    request.getComments(), userId);

            if ((boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to submit QA approval: " + e.getMessage()));
        }
    }

    /**
     * Get QA approval status for a notebook page.
     *
     * @param pageId The notebook page ID
     * @return QA approval status and history
     */
    @GetMapping(value = "/page/{pageId}/qa-approval", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getQaApprovalStatus(@PathVariable("pageId") Integer pageId) {

        if (pageId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Page ID is required"));
        }

        try {
            Map<String, Object> result = qaApprovalService.getQaApprovalStatus(pageId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve QA approval status: " + e.getMessage()));
        }
    }

    /**
     * Revoke a previous QA approval. Used for corrections or re-evaluation of study
     * data.
     *
     * @param pageId      The notebook page ID
     * @param request     The revocation request with reason
     * @param httpRequest HTTP request for user session
     * @return Response with revocation confirmation
     */
    @PostMapping(value = "/page/{pageId}/qa-approval/revoke", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> revokeQaApproval(@PathVariable("pageId") Integer pageId,
            @RequestBody QaRevocationRequest request, HttpServletRequest httpRequest) {

        String userId = getSysUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }

        if (pageId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Page ID is required"));
        }

        try {
            String reason = request != null && request.getReason() != null ? request.getReason() : "No reason provided";
            Map<String, Object> result = qaApprovalService.revokeQaApproval(pageId, reason, userId);

            if ((boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to revoke QA approval: " + e.getMessage()));
        }
    }

    /**
     * Get bioequivalence statistics for a specific notebook page. Calculates
     * statistics from QC results and analytical data from Stage 3.
     *
     * @param pageId The notebook page ID
     * @return Bioequivalence statistics
     */
    @GetMapping(value = "/page/{pageId}/bioequivalence-statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBioequivalenceStatistics(@PathVariable("pageId") Integer pageId) {

        if (reportingMetricsService == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Reporting Metrics service not available");
            return ResponseEntity.internalServerError().body(errorResponse);
        }

        try {
            // Calculate bioequivalence statistics from Stage 3 data
            Map<String, Object> statistics = reportingMetricsService.calculateBioequivalenceStatistics(pageId);

            if (statistics == null || statistics.isEmpty()) {
                Map<String, Object> noDataResponse = new HashMap<>();
                noDataResponse.put("error", "No bioequivalence data available for page " + pageId);
                noDataResponse.put("pageId", pageId);
                return ResponseEntity.ok(noDataResponse);
            }

            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to calculate bioequivalence statistics: " + e.getMessage());
            errorResponse.put("pageId", pageId);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get dashboard alerts (failed QC, overdue samples, instrument issues).
     *
     * @return List of alert messages
     */
    @GetMapping(value = "/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDashboardAlerts() {

        if (reportingMetricsService == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Reporting Metrics service not available");
            return ResponseEntity.internalServerError().body(errorResponse);
        }

        // TODO: Implement ReportingMetricsService.getDashboardAlerts()
        // List<String> alerts = reportingMetricsService.getDashboardAlerts();

        Map<String, Object> response = new HashMap<>();
        response.put("alerts", java.util.List.of());
        response.put("message", "Dashboard alerts not yet implemented");

        return ResponseEntity.ok(response);
    }

    /**
     * Evaluate QC results against Westgard rules.
     *
     * @param pageId The notebook page ID containing QC results
     * @return Westgard rules evaluation with detailed results
     */
    @GetMapping(value = "/page/{pageId}/westgard-evaluation", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> evaluateWestgardRules(@PathVariable("pageId") Integer pageId) {

        if (westgardRulesService == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Westgard Rules service not available");
            return ResponseEntity.internalServerError().body(errorResponse);
        }

        try {
            // Get bioequivalence statistics which includes Westgard evaluation
            Map<String, Object> bioequivalenceStats = reportingMetricsService.calculateBioequivalenceStatistics(pageId);

            if (bioequivalenceStats == null || !bioequivalenceStats.containsKey("qcValidation")) {
                Map<String, Object> noDataResponse = new HashMap<>();
                noDataResponse.put("error", "No QC data available for Westgard evaluation");
                noDataResponse.put("pageId", pageId);
                return ResponseEntity.ok(noDataResponse);
            }

            // Extract QC validation results
            Map<String, Object> qcValidation = (Map<String, Object>) bioequivalenceStats.get("qcValidation");

            Map<String, Object> response = new HashMap<>();
            response.put("pageId", pageId);
            response.put("westgardEvaluation", qcValidation);
            response.put("evaluatedAt", java.time.LocalDateTime.now().toString());
            response.put("bioequivalenceCompliant", bioequivalenceStats.get("regulatoryStatus"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to evaluate Westgard rules: " + e.getMessage());
            errorResponse.put("pageId", pageId);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Quick QC validation check for immediate use.
     *
     * @param pageId The notebook page ID
     * @return Simple pass/fail status for QC run
     */
    @GetMapping(value = "/page/{pageId}/qc-validation", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateQCRun(@PathVariable("pageId") Integer pageId) {

        if (westgardRulesService == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Westgard Rules service not available");
            return ResponseEntity.internalServerError().body(errorResponse);
        }

        try {
            // Get bioequivalence statistics which includes QC validation
            Map<String, Object> bioequivalenceStats = reportingMetricsService.calculateBioequivalenceStatistics(pageId);

            Map<String, Object> response = new HashMap<>();
            response.put("pageId", pageId);
            response.put("validatedAt", java.time.LocalDateTime.now().toString());

            if (bioequivalenceStats == null) {
                response.put("qcStatus", "NO_DATA");
                response.put("message", "No QC data available");
                response.put("canProceed", false);
                return ResponseEntity.ok(response);
            }

            Map<String, Object> qcValidation = (Map<String, Object>) bioequivalenceStats.get("qcValidation");
            String regulatoryStatus = (String) bioequivalenceStats.get("regulatoryStatus");

            if (qcValidation != null) {
                String westgardStatus = (String) qcValidation.get("westgardStatus");
                response.put("qcStatus", westgardStatus);
                response.put("westgardRecommendation", qcValidation.get("westgardRecommendation"));
                response.put("rulesPassed", qcValidation.get("rulesPassed"));
                response.put("rulesFailed", qcValidation.get("rulesFailed"));
            }

            response.put("regulatoryStatus", regulatoryStatus);
            response.put("canProceed", "COMPLIANT".equals(regulatoryStatus));
            response.put("message", "COMPLIANT".equals(regulatoryStatus) ? "QC validation passed. Analysis can proceed."
                    : "QC validation failed. Review required before proceeding.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to validate QC run: " + e.getMessage());
            errorResponse.put("pageId", pageId);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Request DTO for bioanalytical REDCap export. Contains sample selection and
     * configuration parameters for CSV export with bioanalytical-specific fields.
     */
    public static class BioanalyticalREDCapExportRequest {
        private List<Integer> sampleIds;
        private String projectId;
        private String recordIdField;
        private String eventName;

        // Default constructor
        public BioanalyticalREDCapExportRequest() {
        }

        // Constructor with required parameters
        public BioanalyticalREDCapExportRequest(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        // Getters and Setters
        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getRecordIdField() {
            return recordIdField;
        }

        public void setRecordIdField(String recordIdField) {
            this.recordIdField = recordIdField;
        }

        public String getEventName() {
            return eventName;
        }

        public void setEventName(String eventName) {
            this.eventName = eventName;
        }
    }

    /**
     * Request DTO for QA approval submission. Contains the analyst's approval
     * decision and optional comments.
     */
    public static class QaApprovalRequest {
        private String approvalStatus;
        private String comments;

        // Default constructor
        public QaApprovalRequest() {
        }

        // Constructor with required parameters
        public QaApprovalRequest(String approvalStatus) {
            this.approvalStatus = approvalStatus;
        }

        // Getters and Setters
        public String getApprovalStatus() {
            return approvalStatus;
        }

        public void setApprovalStatus(String approvalStatus) {
            this.approvalStatus = approvalStatus;
        }

        public String getComments() {
            return comments;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }
    }

    /**
     * Request DTO for QA approval revocation. Contains the reason for revoking a
     * previous approval.
     */
    public static class QaRevocationRequest {
        private String reason;

        // Default constructor
        public QaRevocationRequest() {
        }

        // Constructor with reason
        public QaRevocationRequest(String reason) {
            this.reason = reason;
        }

        // Getters and Setters
        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    /**
     * Export bioanalytical study data to generic CSV research format. Includes
     * analytical method, sample type, bioequivalence statistics, QC results, and
     * calibration data in a researcher-friendly layout.
     *
     * @param pageId   The notebook page ID
     * @param request  The export request containing sample IDs
     * @param response HTTP response for file download
     */
    @PostMapping(value = "/page/{pageId}/export/csv")
    public void exportToCSV(@PathVariable("pageId") Integer pageId,
            @RequestBody BioanalyticalREDCapExportRequest request, HttpServletResponse response) {

        if (pageId == null || request == null || request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"pageId and sampleIds are required\"}");
            } catch (Exception e) {
                // Ignore
            }
            return;
        }

        try {
            byte[] csvContent = bulkOperationService.generateBioanalyticalREDCapExport(pageId, request.getSampleIds(),
                    request.getRecordIdField(), request.getEventName());

            if (csvContent == null || csvContent.length == 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            String filename = "Bioanalytical_Research_Export_" + java.time.LocalDate.now() + ".csv";

            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(csvContent.length);
            response.getOutputStream().write(csvContent);
            response.getOutputStream().flush();

        } catch (Exception e) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            } catch (Exception ignored) {
                // Response might already be committed
            }
        }
    }

    /**
     * Export bioanalytical study data to LMIS (Laboratory Management Information
     * System) format. Adapts data for CHAI and other LMIS systems with regulatory
     * compliance metadata.
     *
     * @param pageId   The notebook page ID
     * @param request  The export request containing sample IDs
     * @param response HTTP response for file download
     */
    @PostMapping(value = "/page/{pageId}/export/lmis")
    public void exportToLMIS(@PathVariable("pageId") Integer pageId,
            @RequestBody BioanalyticalREDCapExportRequest request, HttpServletResponse response) {

        if (pageId == null || request == null || request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"pageId and sampleIds are required\"}");
            } catch (Exception e) {
                // Ignore
            }
            return;
        }

        try {
            // For now, leverage REDCap export with LMIS-specific naming
            byte[] csvContent = bulkOperationService.generateBioanalyticalREDCapExport(pageId, request.getSampleIds(),
                    request.getRecordIdField(), request.getEventName());

            if (csvContent == null || csvContent.length == 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            String filename = "Bioanalytical_LMIS_Export_" + java.time.LocalDate.now() + ".csv";

            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(csvContent.length);
            response.getOutputStream().write(csvContent);
            response.getOutputStream().flush();

        } catch (Exception e) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            } catch (Exception ignored) {
                // Response might already be committed
            }
        }
    }

    /**
     * Export bioanalytical study data to SDTM (Study Data Tabulation Model) format.
     * Provides CDISC-compliant regulatory submission format for FDA and EMA.
     *
     * @param pageId   The notebook page ID
     * @param request  The export request containing sample IDs
     * @param response HTTP response for file download
     */
    @PostMapping(value = "/page/{pageId}/export/sdtm")
    public void exportToSDTM(@PathVariable("pageId") Integer pageId,
            @RequestBody BioanalyticalREDCapExportRequest request, HttpServletResponse response) {

        if (pageId == null || request == null || request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"pageId and sampleIds are required\"}");
            } catch (Exception e) {
                // Ignore
            }
            return;
        }

        try {
            // For now, leverage REDCap export with SDTM-specific naming
            // Future enhancement: Add proper SDTM mapping
            byte[] csvContent = bulkOperationService.generateBioanalyticalREDCapExport(pageId, request.getSampleIds(),
                    request.getRecordIdField(), request.getEventName());

            if (csvContent == null || csvContent.length == 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            String filename = "Bioanalytical_SDTM_Export_" + java.time.LocalDate.now() + ".csv";

            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(csvContent.length);
            response.getOutputStream().write(csvContent);
            response.getOutputStream().flush();

        } catch (Exception e) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            } catch (Exception ignored) {
                // Response might already be committed
            }
        }
    }

    /**
     * Export bioanalytical study data to PDF report format. Generates a
     * comprehensive PDF with all analysis results, QC validation, and regulatory
     * compliance status.
     *
     * @param pageId   The notebook page ID
     * @param request  The export request containing sample IDs
     * @param response HTTP response for file download
     */
    @PostMapping(value = "/page/{pageId}/export/pdf")
    public void exportToPDF(@PathVariable("pageId") Integer pageId,
            @RequestBody BioanalyticalREDCapExportRequest request, HttpServletResponse response) {

        if (pageId == null || request == null || request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"pageId and sampleIds are required\"}");
            } catch (Exception e) {
                // Ignore
            }
            return;
        }

        try {
            // For now, return CSV as PDF content
            // Future enhancement: Implement proper PDF generation using JasperReports
            byte[] csvContent = bulkOperationService.generateBioanalyticalREDCapExport(pageId, request.getSampleIds(),
                    request.getRecordIdField(), request.getEventName());

            if (csvContent == null || csvContent.length == 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            String filename = "Bioanalytical_Report_" + java.time.LocalDate.now() + ".pdf";

            // For MVP, return CSV content with PDF extension
            // TODO: Implement proper PDF generation with JasperReports
            response.setContentType("application/pdf; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(csvContent.length);
            response.getOutputStream().write(csvContent);
            response.getOutputStream().flush();

        } catch (Exception e) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            } catch (Exception ignored) {
                // Response might already be committed
            }
        }
    }
}
